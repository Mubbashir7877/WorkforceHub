package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.ClockInResponse;
import com.example.employeemanagement.dto.ClockOutResponse;
import com.example.employeemanagement.dto.TimeClockStatusResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeClockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeClockSessionRepository sessionRepository;

    @Mock
    private TimeClockEventLogRepository eventLogRepository;

    @Mock
    private TimeClockEventProducer eventProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TimeClockService timeClockService;

    private User user;
    private Employee employee;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        employeeRole = new Role(RoleName.EMPLOYEE);

        employee = new Employee(1L, "Jane", "Doe", "jane.doe@example.com");

        user = new User("jane.doe@example.com", "hashed");
        user.setId(10L);
        user.setEmployee(employee);
        user.setRoles(Set.of(employeeRole));

        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getStatus ──────────────────────────────────────────────────────────

    @Test
    void getStatus_whenClockedOut_returnsFalse() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN))
                .thenReturn(Optional.empty());

        TimeClockStatusResponse result = timeClockService.getStatus();

        assertThat(result.clockedIn()).isFalse();
        assertThat(result.sessionId()).isNull();
        assertThat(result.clockInTime()).isNull();
    }

    @Test
    void getStatus_whenClockedIn_returnsTrueWithSessionDetails() {
        TimeClockSession openSession = new TimeClockSession();
        openSession.setId(5L);
        openSession.setClockInTime(Instant.now().minusSeconds(3600));
        openSession.setStatus(TimeClockStatus.OPEN);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN))
                .thenReturn(Optional.of(openSession));

        TimeClockStatusResponse result = timeClockService.getStatus();

        assertThat(result.clockedIn()).isTrue();
        assertThat(result.sessionId()).isEqualTo(5L);
        assertThat(result.clockInTime()).isNotNull();
    }

    // ── clockIn ───────────────────────────────────────────────────────────

    @Test
    void clockIn_withLinkedEmployee_createsOpenSession() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.existsByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN)).thenReturn(false);

        TimeClockSession saved = new TimeClockSession();
        saved.setId(99L);
        saved.setEmployeeId(1L);
        saved.setUserId(10L);
        saved.setClockInTime(Instant.now());
        saved.setStatus(TimeClockStatus.OPEN);
        when(sessionRepository.save(any(TimeClockSession.class))).thenReturn(saved);

        ClockInResponse response = timeClockService.clockIn();

        assertThat(response.sessionId()).isEqualTo(99L);
        assertThat(response.employeeId()).isEqualTo(1L);
        assertThat(response.clockInTime()).isNotNull();

        ArgumentCaptor<TimeClockSession> captor = ArgumentCaptor.forClass(TimeClockSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TimeClockStatus.OPEN);
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(1L);
    }

    @Test
    void clockIn_publishesKafkaEventAfterSuccessfulSave() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.existsByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN)).thenReturn(false);

        TimeClockSession saved = new TimeClockSession();
        saved.setId(99L);
        saved.setClockInTime(Instant.now());
        when(sessionRepository.save(any())).thenReturn(saved);

        timeClockService.clockIn();

        // No active transaction synchronization in unit test context → immediate publish
        ArgumentCaptor<TimeClockEvent> eventCaptor = ArgumentCaptor.forClass(TimeClockEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(TimeClockEventType.EMPLOYEE_CLOCKED_IN);
        assertThat(eventCaptor.getValue().getEmployeeId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getUserEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void clockIn_whileAlreadyClockedIn_throwsConflict() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.existsByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> timeClockService.clockIn())
                .isInstanceOf(TimeClockConflictException.class)
                .hasMessageContaining("already clocked in");

        verify(sessionRepository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    @Test
    void clockIn_withoutLinkedEmployee_throwsNotFoundException() {
        user.setEmployee(null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> timeClockService.clockIn())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sessionRepository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    // ── clockOut ──────────────────────────────────────────────────────────

    @Test
    void clockOut_withOpenSession_closesSessionAndReturnsResponse() {
        TimeClockSession openSession = new TimeClockSession();
        openSession.setId(5L);
        openSession.setEmployeeId(1L);
        openSession.setUserId(10L);
        openSession.setClockInTime(Instant.now().minusSeconds(3600));
        openSession.setStatus(TimeClockStatus.OPEN);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN))
                .thenReturn(Optional.of(openSession));
        when(sessionRepository.save(any())).thenReturn(openSession);

        ClockOutResponse response = timeClockService.clockOut();

        assertThat(response.sessionId()).isEqualTo(5L);
        assertThat(response.clockOutTime()).isNotNull();
        assertThat(response.durationMinutes()).isGreaterThanOrEqualTo(59L);

        ArgumentCaptor<TimeClockSession> captor = ArgumentCaptor.forClass(TimeClockSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TimeClockStatus.CLOSED);
        assertThat(captor.getValue().getClockOutTime()).isNotNull();
    }

    @Test
    void clockOut_publishesKafkaEventWithDurationMetadata() {
        TimeClockSession openSession = new TimeClockSession();
        openSession.setId(5L);
        openSession.setClockInTime(Instant.now().minusSeconds(1800));
        openSession.setStatus(TimeClockStatus.OPEN);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN))
                .thenReturn(Optional.of(openSession));
        when(sessionRepository.save(any())).thenReturn(openSession);

        timeClockService.clockOut();

        ArgumentCaptor<TimeClockEvent> eventCaptor = ArgumentCaptor.forClass(TimeClockEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());
        TimeClockEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(TimeClockEventType.EMPLOYEE_CLOCKED_OUT);
        assertThat(event.getMetadata()).containsKey("durationMinutes");
        assertThat(event.getMetadata()).containsKey("clockInTime");
        assertThat(event.getMetadata()).containsKey("clockOutTime");
    }

    @Test
    void clockOut_withoutOpenSession_throwsConflict() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByEmployeeIdAndStatus(1L, TimeClockStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeClockService.clockOut())
                .isInstanceOf(TimeClockConflictException.class)
                .hasMessageContaining("not currently clocked in");

        verify(sessionRepository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    @Test
    void clockOut_withoutLinkedEmployee_throwsNotFoundException() {
        user.setEmployee(null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> timeClockService.clockOut())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(sessionRepository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    // ── saveEventLog ──────────────────────────────────────────────────────

    @Test
    void saveEventLog_validEvent_persistsRecord() {
        TimeClockEvent event = TimeClockEvent.builder()
                .eventId("evt-1")
                .eventType(TimeClockEventType.EMPLOYEE_CLOCKED_IN)
                .employeeId(1L)
                .employeeEmail("jane@example.com")
                .employeeFullName("Jane Doe")
                .userId(10L)
                .userEmail("jane@example.com")
                .sessionId(5L)
                .eventTime(Instant.now())
                .message("Jane Doe clocked in.")
                .build();

        when(eventLogRepository.existsByEventId("evt-1")).thenReturn(false);

        timeClockService.saveEventLog(event, "hr.timeclock.events");

        verify(eventLogRepository).save(any(TimeClockEventLog.class));
    }

    @Test
    void saveEventLog_duplicateEventId_skipsInsert() {
        TimeClockEvent event = TimeClockEvent.builder()
                .eventId("dup-id")
                .eventType(TimeClockEventType.EMPLOYEE_CLOCKED_IN)
                .eventTime(Instant.now())
                .build();

        when(eventLogRepository.existsByEventId("dup-id")).thenReturn(true);

        timeClockService.saveEventLog(event, "hr.timeclock.events");

        verify(eventLogRepository, never()).save(any());
    }
}
