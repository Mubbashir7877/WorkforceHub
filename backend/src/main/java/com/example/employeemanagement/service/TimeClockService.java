package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.*;
import com.example.employeemanagement.entity.*;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.exception.TimeClockConflictException;
import com.example.employeemanagement.kafka.TimeClockEvent;
import com.example.employeemanagement.kafka.TimeClockEventProducer;
import com.example.employeemanagement.kafka.TimeClockEventType;
import com.example.employeemanagement.repository.TimeClockEventLogRepository;
import com.example.employeemanagement.repository.TimeClockSessionRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeClockService {

    private final UserRepository userRepository;
    private final TimeClockSessionRepository sessionRepository;
    private final TimeClockEventLogRepository eventLogRepository;
    private final TimeClockEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TimeClockStatusResponse getStatus() {
        UserContext ctx = resolveCurrentUser();
        Optional<TimeClockSession> open = sessionRepository
                .findByEmployeeIdAndStatus(ctx.employee().getId(), TimeClockStatus.OPEN);
        if (open.isPresent()) {
            TimeClockSession session = open.get();
            return new TimeClockStatusResponse(true, session.getId(), session.getClockInTime());
        }
        return new TimeClockStatusResponse(false, null, null);
    }

    @Transactional
    public ClockInResponse clockIn() {
        UserContext ctx = resolveCurrentUser();

        if (sessionRepository.existsByEmployeeIdAndStatus(ctx.employee().getId(), TimeClockStatus.OPEN)) {
            throw new TimeClockConflictException("ALREADY_CLOCKED_IN",
                    "You are already clocked in. Please clock out before clocking in again.");
        }

        Instant now = Instant.now();
        TimeClockSession session = new TimeClockSession();
        session.setEmployeeId(ctx.employee().getId());
        session.setUserId(ctx.user().getId());
        session.setClockInTime(now);
        session.setStatus(TimeClockStatus.OPEN);
        TimeClockSession saved = sessionRepository.save(session);

        String employeeFullName = ctx.employee().getFirstName() + " " + ctx.employee().getLastName();
        String message = employeeFullName + " clocked in.";

        TimeClockEvent event = TimeClockEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(TimeClockEventType.EMPLOYEE_CLOCKED_IN)
                .employeeId(ctx.employee().getId())
                .employeeEmail(ctx.employee().getEmail())
                .employeeFullName(employeeFullName)
                .userId(ctx.user().getId())
                .userEmail(ctx.user().getEmail())
                .sessionId(saved.getId())
                .eventTime(now)
                .message(message)
                .build();

        scheduleEvent(event);

        return new ClockInResponse(saved.getId(), ctx.employee().getId(), now, message);
    }

    @Transactional
    public ClockOutResponse clockOut() {
        UserContext ctx = resolveCurrentUser();

        TimeClockSession session = sessionRepository
                .findByEmployeeIdAndStatus(ctx.employee().getId(), TimeClockStatus.OPEN)
                .orElseThrow(() -> new TimeClockConflictException("NOT_CLOCKED_IN",
                        "You are not currently clocked in. Please clock in first."));

        Instant now = Instant.now();
        session.setClockOutTime(now);
        session.setStatus(TimeClockStatus.CLOSED);
        sessionRepository.save(session);

        long durationMinutes = ChronoUnit.MINUTES.between(session.getClockInTime(), now);
        String employeeFullName = ctx.employee().getFirstName() + " " + ctx.employee().getLastName();
        String message = employeeFullName + " clocked out after " + durationMinutes + " minute(s).";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("clockInTime", session.getClockInTime().toString());
        metadata.put("clockOutTime", now.toString());
        metadata.put("durationMinutes", String.valueOf(durationMinutes));

        TimeClockEvent event = TimeClockEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(TimeClockEventType.EMPLOYEE_CLOCKED_OUT)
                .employeeId(ctx.employee().getId())
                .employeeEmail(ctx.employee().getEmail())
                .employeeFullName(employeeFullName)
                .userId(ctx.user().getId())
                .userEmail(ctx.user().getEmail())
                .sessionId(session.getId())
                .eventTime(now)
                .message(message)
                .metadata(metadata)
                .build();

        scheduleEvent(event);

        return new ClockOutResponse(session.getId(), ctx.employee().getId(),
                session.getClockInTime(), now, durationMinutes, message);
    }

    @Transactional(readOnly = true)
    public Page<TimeClockSessionResponse> getMySessions(int page, int size) {
        UserContext ctx = resolveCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "clockInTime"));
        return sessionRepository
                .findByUserIdOrderByClockInTimeDesc(ctx.user().getId(), pageable)
                .map(this::toSessionResponse);
    }

    @Transactional(readOnly = true)
    public Page<TimeClockEventLogResponse> getEventLogs(
            int page, int size,
            Long employeeId, String eventType,
            Instant startDate, Instant endDate,
            String userEmail) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTime"));
        String eventTypeFilter = (eventType == null || eventType.isBlank()) ? null : eventType.toUpperCase();
        String userEmailFilter = (userEmail == null || userEmail.isBlank()) ? null : userEmail.toLowerCase();

        return eventLogRepository
                .findFiltered(employeeId, eventTypeFilter, startDate, endDate, userEmailFilter, pageable)
                .map(this::toEventLogResponse);
    }

    @Transactional
    public void saveEventLog(TimeClockEvent event, String sourceTopic) {
        if (eventLogRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate time clock eventId {} — skipping insert", event.getEventId());
            return;
        }

        TimeClockEventLog record = new TimeClockEventLog();
        record.setEventId(event.getEventId());
        record.setEventType(event.getEventType().name());
        record.setEmployeeId(event.getEmployeeId());
        record.setEmployeeEmail(event.getEmployeeEmail());
        record.setEmployeeFullName(event.getEmployeeFullName());
        record.setUserId(event.getUserId());
        record.setUserEmail(event.getUserEmail());
        record.setSessionId(event.getSessionId());
        record.setEventTime(event.getEventTime());
        record.setConsumedAt(Instant.now());
        record.setSourceTopic(sourceTopic);
        record.setMessage(event.getMessage());

        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            try {
                record.setMetadataJson(objectMapper.writeValueAsString(event.getMetadata()));
            } catch (JsonProcessingException ex) {
                log.warn("Could not serialize metadata for time clock event {}", event.getEventId());
            }
        }

        eventLogRepository.save(record);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserContext resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResourceNotFoundException("No authenticated user found.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new ResourceNotFoundException(
                    "No employee profile is linked to this account. " +
                    "Please ask an administrator to link your employee record.");
        }

        return new UserContext(user, employee);
    }

    private void scheduleEvent(TimeClockEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventProducer.publish(event);
                }
            });
        } else {
            eventProducer.publish(event);
        }
    }

    private TimeClockSessionResponse toSessionResponse(TimeClockSession s) {
        return new TimeClockSessionResponse(
                s.getId(), s.getEmployeeId(), s.getUserId(),
                s.getClockInTime(), s.getClockOutTime(),
                s.getStatus().name(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private TimeClockEventLogResponse toEventLogResponse(TimeClockEventLog e) {
        return new TimeClockEventLogResponse(
                e.getId(), e.getEventId(), e.getEventType(),
                e.getEmployeeId(), e.getEmployeeEmail(), e.getEmployeeFullName(),
                e.getUserId(), e.getUserEmail(), e.getSessionId(),
                e.getEventTime(), e.getConsumedAt(), e.getSourceTopic(),
                e.getMessage(), e.getMetadataJson());
    }

    private record UserContext(User user, Employee employee) {}
}
