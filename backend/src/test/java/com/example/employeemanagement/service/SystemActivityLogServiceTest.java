package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.SystemActivityLogResponse;
import com.example.employeemanagement.entity.SystemActivityLog;
import com.example.employeemanagement.kafka.EmployeeActivityEvent;
import com.example.employeemanagement.kafka.EmployeeEventType;
import com.example.employeemanagement.repository.SystemActivityLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemActivityLogServiceTest {

    @Mock
    private SystemActivityLogRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SystemActivityLogService service;

    private EmployeeActivityEvent event;

    @BeforeEach
    void setUp() {
        event = EmployeeActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EmployeeEventType.EMPLOYEE_CREATED)
                .entityType("EMPLOYEE")
                .employeeId(5L)
                .employeeEmail("jane@example.com")
                .employeeFullName("Jane Smith")
                .actorUserId(1L)
                .actorEmail("hr@example.com")
                .actorRoles(List.of("HR_ADMIN"))
                .message("HR admin hr@example.com created employee Jane Smith.")
                .occurredAt(Instant.now())
                .build();
    }

    @Test
    void save_newEvent_persistsActivityLog() {
        when(repository.existsByEventId(event.getEventId())).thenReturn(false);
        when(repository.save(any(SystemActivityLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(event, "hr.employee.events");

        ArgumentCaptor<SystemActivityLog> captor = ArgumentCaptor.forClass(SystemActivityLog.class);
        verify(repository).save(captor.capture());
        SystemActivityLog saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getEventType()).isEqualTo("EMPLOYEE_CREATED");
        assertThat(saved.getEntityId()).isEqualTo(5L);
        assertThat(saved.getActorEmail()).isEqualTo("hr@example.com");
        assertThat(saved.getActorRoles()).isEqualTo("HR_ADMIN");
        assertThat(saved.getSourceTopic()).isEqualTo("hr.employee.events");
        assertThat(saved.getConsumedAt()).isNotNull();
    }

    @Test
    void save_duplicateEventId_skipsInsert() {
        when(repository.existsByEventId(event.getEventId())).thenReturn(true);

        service.save(event, "hr.employee.events");

        verify(repository, never()).save(any());
    }

    @Test
    void getLogs_returnsPageNewestFirst() {
        SystemActivityLog log = new SystemActivityLog();
        log.setEventId(event.getEventId());
        log.setEventType("EMPLOYEE_CREATED");
        log.setEntityType("EMPLOYEE");
        log.setEntityId(5L);
        log.setActorEmail("hr@example.com");
        log.setOccurredAt(Instant.now());
        log.setConsumedAt(Instant.now());

        when(repository.findFiltered(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        var page = service.getLogs(0, 20, null);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0)).isInstanceOf(SystemActivityLogResponse.class);
    }
}
