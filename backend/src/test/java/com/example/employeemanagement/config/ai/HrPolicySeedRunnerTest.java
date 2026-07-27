package com.example.employeemanagement.config.ai;

import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.extraction.DocumentTextExtractor;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import com.example.employeemanagement.service.HrPolicyKnowledgeService;
import com.example.employeemanagement.storage.DocumentStorageService;
import com.example.employeemanagement.storage.StoredDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrPolicySeedRunnerTest {

    @Mock
    private HrPolicyDocumentRepository documentRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private HrPolicyKnowledgeService knowledgeService;

    @Mock
    private DocumentTextExtractor textExtractor;

    private final AtomicLong nextId = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        lenient().when(documentRepository.save(any(HrPolicyDocument.class))).thenAnswer(invocation -> {
            HrPolicyDocument doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(nextId.getAndIncrement());
            }
            return doc;
        });
    }

    private HrPolicySeedRunner runner(boolean seedDefaults) {
        return new HrPolicySeedRunner(
                documentRepository, documentStorageService, knowledgeService, List.of(textExtractor), seedDefaults);
    }

    @Test
    void doesNothing_whenSeedingIsDisabled() throws Exception {
        runner(false).run(null);

        verifyNoInteractions(documentRepository, documentStorageService, knowledgeService, textExtractor);
    }

    @Test
    void doesNothing_whenKnowledgeBaseIsUnavailable() throws Exception {
        when(knowledgeService.isAvailable()).thenReturn(false);

        runner(true).run(null);

        verify(knowledgeService, never()).indexDocument(any(), any());
        verifyNoInteractions(documentRepository, documentStorageService);
    }

    @Test
    void doesNothing_whenPolicyDocumentsAlreadyExist() throws Exception {
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentRepository.count()).thenReturn(3L);

        runner(true).run(null);

        verify(documentRepository, never()).save(any());
        verify(knowledgeService, never()).indexDocument(any(), any());
        verifyNoInteractions(documentStorageService, textExtractor);
    }

    @Test
    void seedsAndActivatesEveryDefaultPolicy_whenLibraryIsEmptyAndAiIsAvailable() throws Exception {
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentRepository.count()).thenReturn(0L);
        when(textExtractor.supports(anyString(), anyString())).thenReturn(true);
        when(documentStorageService.store(anyLong(), any())).thenAnswer(invocation ->
                new StoredDocument("stored-" + invocation.getArgument(0) + ".md", "seed.md", "text/markdown", 100));
        when(documentStorageService.load(anyString())).thenReturn(new ByteArrayResource("content".getBytes()));
        when(textExtractor.extract(any(), anyString()))
                .thenReturn(new ExtractedDocument("content", List.of(), Map.of()));
        when(knowledgeService.indexDocument(any(), any())).thenReturn(3);

        runner(true).run(null);

        int policyCount = DefaultPolicies.ALL.size();
        verify(knowledgeService, times(policyCount)).indexDocument(any(), any());
        verify(documentStorageService, times(policyCount)).store(anyLong(), any());

        ArgumentCaptor<HrPolicyDocument> savedCaptor = ArgumentCaptor.forClass(HrPolicyDocument.class);
        verify(documentRepository, atLeast(policyCount)).save(savedCaptor.capture());

        long readyAndActive = savedCaptor.getAllValues().stream()
                .filter(doc -> doc.isActive() && doc.getProcessingStatus() == PolicyProcessingStatus.READY)
                .map(HrPolicyDocument::getId)
                .distinct()
                .count();
        assertThat(readyAndActive).isEqualTo(policyCount);
    }

    @Test
    void doesNotAbortRemainingPolicies_whenOneFailsToIndex() throws Exception {
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentRepository.count()).thenReturn(0L);
        when(textExtractor.supports(anyString(), anyString())).thenReturn(true);
        when(documentStorageService.store(anyLong(), any())).thenAnswer(invocation ->
                new StoredDocument("stored-" + invocation.getArgument(0) + ".md", "seed.md", "text/markdown", 100));
        when(documentStorageService.load(anyString())).thenReturn(new ByteArrayResource("content".getBytes()));
        when(textExtractor.extract(any(), anyString()))
                .thenReturn(new ExtractedDocument("content", List.of(), Map.of()));
        when(knowledgeService.indexDocument(any(), any()))
                .thenThrow(new RuntimeException("boom"))
                .thenReturn(3);

        runner(true).run(null);

        int policyCount = DefaultPolicies.ALL.size();
        verify(knowledgeService, times(policyCount)).indexDocument(any(), any());
    }
}
