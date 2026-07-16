package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.PolicyDocumentCreateRequest;
import com.example.employeemanagement.dto.PolicyDocumentResponse;
import com.example.employeemanagement.dto.PolicyDocumentUpdateRequest;
import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyCategory;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.entity.StorageType;
import com.example.employeemanagement.exception.DocumentProcessingException;
import com.example.employeemanagement.exception.InvalidPolicyCategoryException;
import com.example.employeemanagement.exception.PolicyDocumentNotFoundException;
import com.example.employeemanagement.exception.PolicyDocumentStateException;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.extraction.DocumentTextExtractor;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.mapper.PolicyDocumentMapper;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.storage.DocumentStorageService;
import com.example.employeemanagement.storage.StoredDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrPolicyDocumentService {

    private final HrPolicyDocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final HrPolicyKnowledgeService knowledgeService;
    private final List<DocumentTextExtractor> textExtractors;

    @Transactional
    public PolicyDocumentResponse create(PolicyDocumentCreateRequest request) {
        UserPrincipal principal = resolveCurrentPrincipal();

        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setTitle(request.getTitle());
        doc.setDescription(request.getDescription());
        doc.setCategory(parseCategory(request.getCategory()));
        doc.setVersion(request.getVersion());
        doc.setEffectiveDate(request.getEffectiveDate());
        doc.setActive(false);
        doc.setProcessingStatus(PolicyProcessingStatus.DRAFT);
        doc.setUploadedByUserId(principal.getId());
        doc.setUploadedByEmail(principal.getUsername());

        HrPolicyDocument saved = documentRepository.save(doc);
        log.info("HR policy document {} created by {}", saved.getId(), principal.getUsername());
        return PolicyDocumentMapper.toResponse(saved);
    }

    @Transactional
    public PolicyDocumentResponse update(Long id, PolicyDocumentUpdateRequest request) {
        HrPolicyDocument doc = findEntity(id);
        doc.setTitle(request.getTitle());
        doc.setDescription(request.getDescription());
        doc.setCategory(parseCategory(request.getCategory()));
        doc.setVersion(request.getVersion());
        doc.setEffectiveDate(request.getEffectiveDate());
        return PolicyDocumentMapper.toResponse(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public PolicyDocumentResponse getById(Long id) {
        return PolicyDocumentMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<PolicyDocumentResponse> list(String category, Boolean active, int page, int size) {
        PolicyCategory categoryFilter = (category == null || category.isBlank()) ? null : parseCategory(category);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"));
        return documentRepository.findFiltered(categoryFilter, active, pageable)
                .map(PolicyDocumentMapper::toResponse);
    }

    @Transactional
    public PolicyDocumentResponse uploadFile(Long id, MultipartFile file) {
        HrPolicyDocument doc = findEntity(id);

        // Replacing a previously uploaded file — remove the old blob so re-uploads
        // don't leak orphaned files on disk. The document itself is never deleted.
        String previousLocation = doc.getStorageLocation();

        StoredDocument stored = documentStorageService.store(id, file);

        doc.setOriginalFileName(stored.originalFileName());
        doc.setContentType(stored.contentType());
        doc.setStorageType(StorageType.LOCAL);
        doc.setStorageLocation(stored.storageLocation());
        doc.setProcessingStatus(PolicyProcessingStatus.UPLOADED);
        doc.setChunkCount(0);
        HrPolicyDocument saved = documentRepository.save(doc);

        if (previousLocation != null && !previousLocation.equals(stored.storageLocation())) {
            documentStorageService.delete(previousLocation);
        }

        log.info("File uploaded for HR policy document {}: {}", id, stored.originalFileName());
        return PolicyDocumentMapper.toResponse(saved);
    }

    /**
     * Extracts, chunks, and embeds the uploaded file, indexing it in the vector
     * store. Does not make the document active/searchable by itself — see
     * activate(). FAILED status is deliberately allowed to persist even though
     * this method throws, so admins can see the failure on the next GET.
     */
    @Transactional(noRollbackFor = {DocumentProcessingException.class, AiUnavailableException.class})
    public PolicyDocumentResponse process(Long id) {
        HrPolicyDocument doc = findEntity(id);
        if (doc.getStorageLocation() == null) {
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "No file has been uploaded for this document yet.");
        }

        doc.setProcessingStatus(PolicyProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            Resource resource = documentStorageService.load(doc.getStorageLocation());
            DocumentTextExtractor extractor = textExtractors.stream()
                    .filter(e -> e.supports(doc.getContentType(), doc.getOriginalFileName()))
                    .findFirst()
                    .orElseThrow(() -> new DocumentProcessingException("UNSUPPORTED_DOCUMENT_TYPE",
                            "No text extractor available for this document's file type."));

            ExtractedDocument extracted = extractor.extract(resource, doc.getOriginalFileName());
            int chunkCount = knowledgeService.indexDocument(doc, extracted);

            doc.setChunkCount(chunkCount);
            doc.setProcessingStatus(PolicyProcessingStatus.READY);
            HrPolicyDocument saved = documentRepository.save(doc);
            log.info("Processed HR policy document {} into {} chunks", id, chunkCount);
            return PolicyDocumentMapper.toResponse(saved);
        } catch (DocumentProcessingException | AiUnavailableException ex) {
            doc.setProcessingStatus(PolicyProcessingStatus.FAILED);
            documentRepository.save(doc);
            throw ex;
        }
    }

    @Transactional
    public PolicyDocumentResponse activate(Long id) {
        HrPolicyDocument doc = findEntity(id);
        if (doc.getProcessingStatus() != PolicyProcessingStatus.READY) {
            throw new PolicyDocumentStateException("POLICY_DOCUMENT_NOT_READY",
                    "This document must be successfully processed before it can be activated.");
        }
        doc.setActive(true);
        HrPolicyDocument saved = documentRepository.save(doc);
        log.info("HR policy document {} activated", id);
        return PolicyDocumentMapper.toResponse(saved);
    }

    @Transactional
    public PolicyDocumentResponse deactivate(Long id) {
        HrPolicyDocument doc = findEntity(id);
        doc.setActive(false);
        doc.setProcessingStatus(PolicyProcessingStatus.INACTIVE);
        knowledgeService.removeChunks(doc);
        doc.setChunkCount(0);
        HrPolicyDocument saved = documentRepository.save(doc);
        log.info("HR policy document {} deactivated", id);
        return PolicyDocumentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PolicyDocumentDownload download(Long id) {
        HrPolicyDocument doc = findEntity(id);
        if (doc.getStorageLocation() == null) {
            throw new PolicyDocumentNotFoundException("No file has been uploaded for document " + id);
        }
        return new PolicyDocumentDownload(
                documentStorageService.load(doc.getStorageLocation()),
                doc.getOriginalFileName(),
                doc.getContentType());
    }

    // ── package-private helpers used by other AI services ──────────────────────

    HrPolicyDocument findEntity(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new PolicyDocumentNotFoundException("No policy document found with id " + id));
    }

    static PolicyCategory parseCategory(String raw) {
        try {
            return PolicyCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidPolicyCategoryException("Unknown policy category: " + raw);
        }
    }

    private UserPrincipal resolveCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new PolicyDocumentNotFoundException("No authenticated user found.");
        }
        return principal;
    }
}
