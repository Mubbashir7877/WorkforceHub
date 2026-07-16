package com.example.employeemanagement.service;

import com.example.employeemanagement.config.ai.AiProperties;
import com.example.employeemanagement.dto.AiStatusResponse;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Backs GET /api/v1/system/ai/status — safe, secret-free operational status only. */
@Service
@RequiredArgsConstructor
public class AiOperationalStatusService {

    private final AiProperties aiProperties;
    private final HrPolicyKnowledgeService knowledgeService;
    private final HrPolicyDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public AiStatusResponse getStatus() {
        return new AiStatusResponse(
                aiProperties.isEnabled(),
                aiProperties.isConfigured(),
                aiProperties.getModel(),
                knowledgeService.isAvailable(),
                documentRepository.countByActiveTrue()
        );
    }
}
