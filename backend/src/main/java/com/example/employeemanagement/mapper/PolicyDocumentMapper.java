package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.PolicyDocumentResponse;
import com.example.employeemanagement.entity.HrPolicyDocument;

public final class PolicyDocumentMapper {

    private PolicyDocumentMapper() {}

    public static PolicyDocumentResponse toResponse(HrPolicyDocument doc) {
        return new PolicyDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getCategory() != null ? doc.getCategory().name() : null,
                doc.getVersion(),
                doc.getEffectiveDate(),
                doc.isActive(),
                doc.getProcessingStatus() != null ? doc.getProcessingStatus().name() : null,
                doc.getChunkCount(),
                doc.getOriginalFileName(),
                doc.getContentType(),
                doc.getUploadedByEmail(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
