package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.PolicyConflictReviewResponse;
import com.example.employeemanagement.dto.PolicyDocumentCreateRequest;
import com.example.employeemanagement.dto.PolicyDocumentResponse;
import com.example.employeemanagement.dto.PolicyDocumentUpdateRequest;
import com.example.employeemanagement.service.HrPolicyDocumentService;
import com.example.employeemanagement.service.PolicyConflictReviewService;
import com.example.employeemanagement.service.PolicyDocumentDownload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * HR policy document administration. Restricted to HR_ADMIN and SYSTEM_ADMIN —
 * employees interact with policy content only indirectly, through the HR
 * assistant chat (see HrAssistantController).
 */
@RestController
@RequestMapping("/api/v1/hr/policies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
public class HrPolicyController {

    private final HrPolicyDocumentService documentService;
    private final PolicyConflictReviewService conflictReviewService;

    @GetMapping
    public ResponseEntity<Page<PolicyDocumentResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.list(category, active, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyDocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PolicyDocumentResponse> create(@Valid @RequestBody PolicyDocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyDocumentResponse> update(
            @PathVariable Long id, @Valid @RequestBody PolicyDocumentUpdateRequest request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<PolicyDocumentResponse> upload(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadFile(id, file));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<PolicyDocumentResponse> process(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.process(id));
    }

    @PostMapping("/{id}/review-conflicts")
    public ResponseEntity<PolicyConflictReviewResponse> reviewConflicts(@PathVariable Long id) {
        return ResponseEntity.ok(conflictReviewService.reviewForConflicts(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PolicyDocumentResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<PolicyDocumentResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.deactivate(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        PolicyDocumentDownload download = documentService.download(id);
        MediaType mediaType = download.contentType() != null
                ? MediaType.parseMediaType(download.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.filename()).build().toString())
                .body(download.resource());
    }
}
