package com.example.employeemanagement.kafka;

import com.example.employeemanagement.service.SystemActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmployeeActivityEventConsumer}. The listener method is
 * called directly — no real Kafka broker or embedded broker is required.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeActivityEventConsumerTest {

    @Mock
    private SystemActivityLogService systemActivityLogService;

    @InjectMocks
    private EmployeeActivityEventConsumer consumer;

    private EmployeeActivityEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = EmployeeActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EmployeeEventType.EMPLOYEE_CREATED)
                .entityType("EMPLOYEE")
                .employeeId(1L)
                .employeeEmail("john@example.com")
                .employeeFullName("John Doe")
                .actorUserId(10L)
                .actorEmail("admin@example.com")
                .actorRoles(List.of("HR_ADMIN"))
                .message("HR admin admin@example.com created employee John Doe.")
                .occurredAt(Instant.now())
                .build();
    }

    @Test
    void consume_validEvent_delegatesToService() {
        consumer.consume(validEvent);

        verify(systemActivityLogService).save(eq(validEvent), any());
    }

    @Test
    void consume_nullEvent_skipsServiceCall() {
        consumer.consume(null);

        verify(systemActivityLogService, never()).save(any(), any());
    }

    @Test
    void consume_eventWithNullEventId_skipsServiceCall() {
        validEvent.setEventId(null);
        consumer.consume(validEvent);

        verify(systemActivityLogService, never()).save(any(), any());
    }

    @Test
    void consume_serviceThrowsException_doesNotPropagateError() {
        doThrow(new RuntimeException("DB error")).when(systemActivityLogService).save(any(), any());

        // should not throw — consumer handles the exception gracefully
        consumer.consume(validEvent);
    }
}
