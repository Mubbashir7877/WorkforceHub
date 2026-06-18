package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.SystemActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemActivityLogRepository extends JpaRepository<SystemActivityLog, Long> {

    boolean existsByEventId(String eventId);

    @Query("SELECT s FROM SystemActivityLog s " +
           "WHERE (:eventType IS NULL OR s.eventType = :eventType) " +
           "ORDER BY s.occurredAt DESC")
    Page<SystemActivityLog> findFiltered(@Param("eventType") String eventType, Pageable pageable);
}
