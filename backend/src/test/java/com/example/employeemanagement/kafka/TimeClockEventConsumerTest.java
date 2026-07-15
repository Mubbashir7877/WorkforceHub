package com.example.employeemanagement.kafka;

import com.example.employeemanagement.service.TimeClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TimeClockEventConsumer}. The listener method is called
 * directly — no real or embedded Kafka broker is required.
 */
@ExtendWith(MockitoExtension.class)
class TimeClockEventConsumerTest {

    @Mock
    private TimeClockService timeClockService;

    @InjectMocks
    private TimeClockEventConsumer consumer;

    private TimeClockEvent validClockInEvent;
    private TimeClockEvent validClockOutEvent;

    @BeforeEach
    void setUp() {
        validClockInEvent = TimeClockEvent.builder()
                .eventId(UUID.randomUUID().toString())
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

        validClockOutEvent = TimeClockEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(TimeClockEventType.EMPLOYEE_CLOCKED_OUT)
                .employeeId(1L)
                .employeeEmail("jane@example.com")
                .employeeFullName("Jane Doe")
                .userId(10L)
                .userEmail("jane@example.com")
                .sessionId(5L)
                .eventTime(Instant.now())
                .message("Jane Doe clocked out.")
                .build();
    }

    @Test
    void consume_validClockInEvent_delegatesToService() {
        consumer.consume(validClockInEvent);

        verify(timeClockService).saveEventLog(eq(validClockInEvent), any());
    }

    @Test
    void consume_validClockOutEvent_delegatesToService() {
        consumer.consume(validClockOutEvent);

        verify(timeClockService).saveEventLog(eq(validClockOutEvent), any());
    }

    @Test
    void consume_nullEvent_skipsServiceCall() {
        consumer.consume(null);

        verify(timeClockService, never()).saveEventLog(any(), any());
    }

    @Test
    void consume_eventWithNullEventId_skipsServiceCall() {
        validClockInEvent.setEventId(null);
        consumer.consume(validClockInEvent);

        verify(timeClockService, never()).saveEventLog(any(), any());
    }

    @Test
    void consume_eventWithNullEventType_skipsServiceCall() {
        validClockInEvent.setEventType(null);
        consumer.consume(validClockInEvent);

        verify(timeClockService, never()).saveEventLog(any(), any());
    }

    @Test
    void consume_serviceThrowsException_doesNotPropagateError() {
        doThrow(new RuntimeException("DB error")).when(timeClockService).saveEventLog(any(), any());

        // should not throw — consumer handles the exception gracefully
        consumer.consume(validClockInEvent);
    }
}
