package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
