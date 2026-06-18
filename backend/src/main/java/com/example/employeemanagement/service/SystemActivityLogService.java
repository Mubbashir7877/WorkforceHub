package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.SystemActivityLogResponse;
import com.example.employeemanagement.entity.SystemActivityLog;
import com.example.employeemanagement.kafka.EmployeeActivityEvent;
import com.example.employeemanagement.repository.SystemActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemActivityLogService {

    private final SystemActivityLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(EmployeeActivityEvent event, String sourceTopic) {
        if (repository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate eventId {} received — skipping insert", event.getEventId());
            return;
        }

        SystemActivityLog log = new SystemActivityLog();
        log.setEventId(event.getEventId());
        log.setEventType(event.getEventType().name());
        log.setEntityType(event.getEntityType());
        log.setEntityId(event.getEmployeeId());
        log.setActorUserId(event.getActorUserId());
        log.setActorEmail(event.getActorEmail());
        log.setActorRoles(event.getActorRoles() == null ? null
                : String.join(",", event.getActorRoles()));
        log.setMessage(event.getMessage());
        log.setOccurredAt(event.getOccurredAt());
        log.setConsumedAt(Instant.now());
        log.setSourceTopic(sourceTopic);

        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            try {
                log.setMetadataJson(objectMapper.writeValueAsString(event.getMetadata()));
            } catch (JsonProcessingException ex) {
                SystemActivityLogService.log.warn("Could not serialize metadata for event {}", event.getEventId());
            }
        }

        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<SystemActivityLogResponse> getLogs(int page, int size, String eventType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        String filter = (eventType == null || eventType.isBlank()) ? null : eventType.toUpperCase();
        return repository.findFiltered(filter, pageable).map(this::toResponse);
    }

    private SystemActivityLogResponse toResponse(SystemActivityLog entity) {
        return new SystemActivityLogResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getActorUserId(),
                entity.getActorEmail(),
                entity.getActorRoles(),
                entity.getMessage(),
                entity.getOccurredAt(),
                entity.getConsumedAt(),
                entity.getSourceTopic()
        );
    }
}
