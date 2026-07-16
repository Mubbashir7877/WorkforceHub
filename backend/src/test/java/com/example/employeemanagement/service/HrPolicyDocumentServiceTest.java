package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.PolicyDocumentCreateRequest;
import com.example.employeemanagement.dto.PolicyDocumentResponse;
import com.example.employeemanagement.dto.PolicyDocumentUpdateRequest;
import com.example.employeemanagement.entity.*;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.exception.DocumentProcessingException;
import com.example.employeemanagement.exception.InvalidPolicyCategoryException;
import com.example.employeemanagement.exception.PolicyDocumentNotFoundException;
import com.example.employeemanagement.exception.PolicyDocumentStateException;
import com.example.employeemanagement.extraction.DocumentTextExtractor;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.storage.DocumentStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrPolicyDocumentServiceTest {

    @Mock
    private HrPolicyDocumentRepository documentRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private HrPolicyKnowledgeService knowledgeService;

    @Mock
    private DocumentTextExtractor textExtractor;

    private HrPolicyDocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new HrPolicyDocumentService(
                documentRepository, documentStorageService, knowledgeService, List.of(textExtractor));

        User user = new User("hr.admin@example.com", "hashed");
        user.setId(42L);
        user.setRoles(Set.of(new Role(RoleName.HR_ADMIN)));

        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_validRequest_savesDraftWithUploaderInfo() {
        PolicyDocumentCreateRequest request = new PolicyDocumentCreateRequest();
        request.setTitle("Leave Policy");
        request.setDescription("Annual leave rules");
        request.setCategory("leave");
        request.setVersion("1.0");
        request.setEffectiveDate(LocalDate.of(2026, 1, 1));

        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> {
            HrPolicyDocument doc = inv.getArgument(0);
            doc.setId(1L);
            return doc;
        });

        PolicyDocumentResponse response = documentService.create(request);

        assertThat(response.title()).isEqualTo("Leave Policy");
        assertThat(response.category()).isEqualTo("LEAVE");
        assertThat(response.active()).isFalse();
        assertThat(response.processingStatus()).isEqualTo("DRAFT");
        assertThat(response.uploadedByEmail()).isEqualTo("hr.admin@example.com");

        ArgumentCaptor<HrPolicyDocument> captor = ArgumentCaptor.forClass(HrPolicyDocument.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getUploadedByUserId()).isEqualTo(42L);
    }

    @Test
    void create_unknownCategory_throwsInvalidPolicyCategoryException() {
        PolicyDocumentCreateRequest request = new PolicyDocumentCreateRequest();
        request.setTitle("Mystery Policy");
        request.setCategory("NOT_A_REAL_CATEGORY");

        assertThatThrownBy(() -> documentService.create(request))
                .isInstanceOf(InvalidPolicyCategoryException.class);

        verify(documentRepository, never()).save(any());
    }

    @Test
    void update_existingDocument_updatesEditableFields() {
        HrPolicyDocument existing = new HrPolicyDocument();
        existing.setId(5L);
        existing.setTitle("Old Title");
        existing.setCategory(PolicyCategory.GENERAL);
        existing.setActive(true);
        existing.setProcessingStatus(PolicyProcessingStatus.READY);

        when(documentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyDocumentUpdateRequest request = new PolicyDocumentUpdateRequest();
        request.setTitle("New Title");
        request.setCategory("benefits");
        request.setVersion("2.0");

        PolicyDocumentResponse response = documentService.update(5L, request);

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.category()).isEqualTo("BENEFITS");
        // Editing metadata does not implicitly change processing/active state.
        assertThat(response.active()).isTrue();
        assertThat(response.processingStatus()).isEqualTo("READY");
    }

    @Test
    void update_nonExistentDocument_throwsNotFound() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        PolicyDocumentUpdateRequest request = new PolicyDocumentUpdateRequest();
        request.setTitle("Title");
        request.setCategory("general");

        assertThatThrownBy(() -> documentService.update(999L, request))
                .isInstanceOf(PolicyDocumentNotFoundException.class);
    }

    @Test
    void getById_existingDocument_returnsResponse() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(7L);
        doc.setTitle("Conduct Policy");
        doc.setCategory(PolicyCategory.CONDUCT);
        when(documentRepository.findById(7L)).thenReturn(Optional.of(doc));

        PolicyDocumentResponse response = documentService.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("Conduct Policy");
    }

    @Test
    void getById_missingDocument_throwsNotFound() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(404L))
                .isInstanceOf(PolicyDocumentNotFoundException.class);
    }

    @Test
    void list_delegatesFilteringToRepository() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(1L);
        doc.setCategory(PolicyCategory.SECURITY);
        Page<HrPolicyDocument> page = new PageImpl<>(java.util.List.of(doc));

        when(documentRepository.findFiltered(eq(PolicyCategory.SECURITY), eq(true), any()))
                .thenReturn(page);

        Page<PolicyDocumentResponse> result = documentService.list("security", true, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).category()).isEqualTo("SECURITY");
    }

    // ── uploadFile ───────────────────────────────────────────────────────

    @Test
    void uploadFile_storesFileAndMarksUploaded() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(3L);
        doc.setCategory(PolicyCategory.GENERAL);
        doc.setProcessingStatus(PolicyProcessingStatus.DRAFT);
        when(documentRepository.findById(3L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "policy.pdf", "application/pdf", "content".getBytes());

        com.example.employeemanagement.storage.StoredDocument stored =
                new com.example.employeemanagement.storage.StoredDocument(
                        "doc-3-abc.pdf", "policy.pdf", "application/pdf", 7L);
        when(documentStorageService.store(eq(3L), any())).thenReturn(stored);

        PolicyDocumentResponse response = documentService.uploadFile(3L, file);

        assertThat(response.processingStatus()).isEqualTo("UPLOADED");
        assertThat(response.originalFileName()).isEqualTo("policy.pdf");
        verify(documentStorageService, never()).delete(any());
    }

    @Test
    void uploadFile_replacingExistingFile_deletesOldBlob() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(3L);
        doc.setCategory(PolicyCategory.GENERAL);
        doc.setStorageLocation("doc-3-old.pdf");
        when(documentRepository.findById(3L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "policy-v2.pdf", "application/pdf", "content".getBytes());

        com.example.employeemanagement.storage.StoredDocument stored =
                new com.example.employeemanagement.storage.StoredDocument(
                        "doc-3-new.pdf", "policy-v2.pdf", "application/pdf", 7L);
        when(documentStorageService.store(eq(3L), any())).thenReturn(stored);

        documentService.uploadFile(3L, file);

        verify(documentStorageService).delete("doc-3-old.pdf");
    }

    // ── download ─────────────────────────────────────────────────────────

    @Test
    void download_documentWithoutUploadedFile_throwsNotFound() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(9L);
        when(documentRepository.findById(9L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.download(9L))
                .isInstanceOf(PolicyDocumentNotFoundException.class);
    }

    // ── process ──────────────────────────────────────────────────────────

    @Test
    void process_withoutUploadedFile_throwsProcessingFailed() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(10L);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.process(10L))
                .isInstanceOf(DocumentProcessingException.class)
                .satisfies(ex -> assertThat(((DocumentProcessingException) ex).getErrorCode())
                        .isEqualTo("POLICY_PROCESSING_FAILED"));
    }

    @Test
    void process_success_marksReadyWithChunkCount() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(11L);
        doc.setStorageLocation("doc-11-abc.txt");
        doc.setContentType("text/plain");
        doc.setOriginalFileName("policy.txt");
        when(documentRepository.findById(11L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentStorageService.load("doc-11-abc.txt")).thenReturn(new ByteArrayResource("content".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);

        ExtractedDocument extracted = new ExtractedDocument("content", List.of(), Map.of());
        when(textExtractor.extract(any(), eq("policy.txt"))).thenReturn(extracted);
        when(knowledgeService.indexDocument(eq(doc), eq(extracted))).thenReturn(3);

        PolicyDocumentResponse response = documentService.process(11L);

        assertThat(response.processingStatus()).isEqualTo("READY");
        assertThat(response.chunkCount()).isEqualTo(3);
    }

    @Test
    void process_extractionFailure_marksFailedAndRethrows() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(12L);
        doc.setStorageLocation("doc-12-abc.txt");
        doc.setContentType("text/plain");
        doc.setOriginalFileName("policy.txt");
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentStorageService.load("doc-12-abc.txt")).thenReturn(new ByteArrayResource("content".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenThrow(new DocumentProcessingException("DOCUMENT_TEXT_EMPTY", "empty"));

        assertThatThrownBy(() -> documentService.process(12L))
                .isInstanceOf(DocumentProcessingException.class);

        assertThat(doc.getProcessingStatus()).isEqualTo(PolicyProcessingStatus.FAILED);
    }

    @Test
    void process_aiUnavailable_marksFailedAndRethrows() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(13L);
        doc.setStorageLocation("doc-13-abc.txt");
        doc.setContentType("text/plain");
        doc.setOriginalFileName("policy.txt");
        when(documentRepository.findById(13L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentStorageService.load("doc-13-abc.txt")).thenReturn(new ByteArrayResource("content".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        ExtractedDocument extracted = new ExtractedDocument("content", List.of(), Map.of());
        when(textExtractor.extract(any(), eq("policy.txt"))).thenReturn(extracted);
        when(knowledgeService.indexDocument(any(), any()))
                .thenThrow(new AiUnavailableException("KNOWLEDGE_BASE_UNAVAILABLE", "off"));

        assertThatThrownBy(() -> documentService.process(13L))
                .isInstanceOf(AiUnavailableException.class);

        assertThat(doc.getProcessingStatus()).isEqualTo(PolicyProcessingStatus.FAILED);
    }

    // ── activate / deactivate ────────────────────────────────────────────

    @Test
    void activate_notYetProcessed_throwsNotReady() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(14L);
        doc.setProcessingStatus(PolicyProcessingStatus.UPLOADED);
        when(documentRepository.findById(14L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.activate(14L))
                .isInstanceOf(PolicyDocumentStateException.class)
                .satisfies(ex -> assertThat(((PolicyDocumentStateException) ex).getErrorCode())
                        .isEqualTo("POLICY_DOCUMENT_NOT_READY"));

        verify(documentRepository, never()).save(any());
    }

    @Test
    void activate_readyDocument_setsActiveTrue() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(15L);
        doc.setProcessingStatus(PolicyProcessingStatus.READY);
        when(documentRepository.findById(15L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyDocumentResponse response = documentService.activate(15L);

        assertThat(response.active()).isTrue();
    }

    @Test
    void deactivate_activeDocument_removesChunksAndMarksInactive() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(16L);
        doc.setActive(true);
        doc.setProcessingStatus(PolicyProcessingStatus.READY);
        doc.setChunkCount(4);
        when(documentRepository.findById(16L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyDocumentResponse response = documentService.deactivate(16L);

        assertThat(response.active()).isFalse();
        assertThat(response.processingStatus()).isEqualTo("INACTIVE");
        assertThat(response.chunkCount()).isEqualTo(0);
        verify(knowledgeService).removeChunks(doc);
    }
}
