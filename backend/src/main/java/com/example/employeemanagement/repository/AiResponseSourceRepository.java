package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.AiResponseSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiResponseSourceRepository extends JpaRepository<AiResponseSource, Long> {

    List<AiResponseSource> findByMessageIdIn(List<Long> messageIds);
}
