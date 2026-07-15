package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.TimeClockSession;
import com.example.employeemanagement.entity.TimeClockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeClockSessionRepository extends JpaRepository<TimeClockSession, Long> {

    Optional<TimeClockSession> findByEmployeeIdAndStatus(Long employeeId, TimeClockStatus status);

    Page<TimeClockSession> findByUserIdOrderByClockInTimeDesc(Long userId, Pageable pageable);

    boolean existsByEmployeeIdAndStatus(Long employeeId, TimeClockStatus status);
}
