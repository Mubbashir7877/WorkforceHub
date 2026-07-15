package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.TimeClockEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TimeClockEventLogRepository extends JpaRepository<TimeClockEventLog, Long> {

    boolean existsByEventId(String eventId);

    @Query("SELECT e FROM TimeClockEventLog e " +
           "WHERE (:employeeId IS NULL OR e.employeeId = :employeeId) " +
           "AND   (:eventType  IS NULL OR e.eventType  = :eventType)  " +
           "AND   (:startDate  IS NULL OR e.eventTime >= :startDate)  " +
           "AND   (:endDate    IS NULL OR e.eventTime <= :endDate)    " +
           "AND   (:userEmail  IS NULL OR e.userEmail  = :userEmail)  " +
           "ORDER BY e.eventTime DESC")
    Page<TimeClockEventLog> findFiltered(
            @Param("employeeId") Long employeeId,
            @Param("eventType")  String eventType,
            @Param("startDate")  Instant startDate,
            @Param("endDate")    Instant endDate,
            @Param("userEmail")  String userEmail,
            Pageable pageable
    );
}
