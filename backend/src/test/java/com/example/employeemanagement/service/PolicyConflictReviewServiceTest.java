package com.example.employeemanagement.service;

import com.example.employeemanagement.config.ai.AiProperties;
import com.example.employeemanagement.dto.PolicyConflictReviewResponse;
import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.entity.StorageType;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.exception.PolicyDocumentStateException;
import com.example.employeemanagement.extraction.DocumentTextExtractor;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.storage.DocumentStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyConflictReviewServiceTest {

    @Mock private HrPolicyDocumentService documentService;
    @Mock private HrPolicyKnowledgeService knowledgeService;
    @Mock private DocumentStorageService documentStorageService;
    @Mock private DocumentTextExtractor textExtractor;
    @Mock private ObjectProvider<ChatClient> chatClientProvider;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    private AiProperties aiProperties;
    private PolicyConflictReviewService reviewService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setConflictReviewSimilarityThreshold(0.55);
        aiProperties.setConflictReviewMaxChunks(20);
        aiProperties.setConflictReviewMaxCandidatePairs(15);

        reviewService = new PolicyConflictReviewService(
                documentService, knowledgeService, documentStorageService,
                List.of(textExtractor), new DocumentChunker(), chatClientProvider,
                new ObjectMapper(), aiProperties);
    }

    private HrPolicyDocument readyDocument() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(1L);
        doc.setTitle("New Leave Policy");
        doc.setProcessingStatus(PolicyProcessingStatus.READY);
        doc.setStorageType(StorageType.LOCAL);
        doc.setStorageLocation("stored.txt");
        doc.setContentType("text/plain");
        doc.setOriginalFileName("policy.txt");
        return doc;
    }

    private void mockChatClientChain(String answer) {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(answer);
    }

    @Test
    void reviewForConflicts_documentNotReady_throwsStateException() {
        HrPolicyDocument doc = readyDocument();
        doc.setProcessingStatus(PolicyProcessingStatus.UPLOADED);
        when(documentService.findEntity(1L)).thenReturn(doc);

        assertThatThrownBy(() -> reviewService.reviewForConflicts(1L))
                .isInstanceOf(PolicyDocumentStateException.class)
                .satisfies(ex -> assertThat(((PolicyDocumentStateException) ex).getErrorCode())
                        .isEqualTo("POLICY_DOCUMENT_NOT_READY"));

        verifyNoInteractions(knowledgeService, chatClientProvider);
    }

    @Test
    void reviewForConflicts_knowledgeBaseUnavailable_throwsAiUnavailable() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> reviewService.reviewForConflicts(1L))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_DISABLED"));

        verifyNoInteractions(chatClientProvider);
    }

    @Test
    void reviewForConflicts_chatClientUnavailable_throwsAiUnavailable() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> reviewService.reviewForConflicts(1L))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_DISABLED"));
    }

    @Test
    void reviewForConflicts_noCandidatesFound_skipsModelCallAndReturnsNoConflicts() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(documentStorageService.load("stored.txt"))
                .thenReturn(new ByteArrayResource("Some new policy text.".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenReturn(new ExtractedDocument("Some new policy text.", List.of(), Map.of()));
        when(knowledgeService.retrieveRelevant(anyString(), eq(3), eq(0.55))).thenReturn(List.of());

        PolicyConflictReviewResponse response = reviewService.reviewForConflicts(1L);

        assertThat(response.hasConflicts()).isFalse();
        assertThat(response.conflicts()).isEmpty();
        verify(chatClient, never()).prompt();
    }

    @Test
    void reviewForConflicts_modelFindsConflict_returnsMappedConflictItem() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentStorageService.load("stored.txt"))
                .thenReturn(new ByteArrayResource("Employees get 10 vacation days per year.".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenReturn(new ExtractedDocument("Employees get 10 vacation days per year.", List.of(), Map.of()));

        RetrievedChunk existingChunk = new RetrievedChunk(
                9L, "Old Leave Policy", "LEAVE", "1.0", null, 0, null,
                "Employees get 15 vacation days per year.", 0.9);
        when(knowledgeService.retrieveRelevant(anyString(), eq(3), eq(0.55))).thenReturn(List.of(existingChunk));

        mockChatClientChain(
                "[{\"pair\": 1, \"explanation\": \"The new document says 10 days but the existing policy says 15 days.\"}]");

        PolicyConflictReviewResponse response = reviewService.reviewForConflicts(1L);

        assertThat(response.hasConflicts()).isTrue();
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().get(0).conflictingDocumentId()).isEqualTo(9L);
        assertThat(response.conflicts().get(0).conflictingDocumentTitle()).isEqualTo("Old Leave Policy");
        assertThat(response.conflicts().get(0).explanation()).contains("10 days");
    }

    @Test
    void reviewForConflicts_excludesChunksFromSameDocument() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(documentStorageService.load("stored.txt"))
                .thenReturn(new ByteArrayResource("Some new policy text.".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenReturn(new ExtractedDocument("Some new policy text.", List.of(), Map.of()));

        RetrievedChunk selfChunk = new RetrievedChunk(
                1L, "New Leave Policy", "LEAVE", "1.0", null, 0, null, "Some new policy text.", 1.0);
        when(knowledgeService.retrieveRelevant(anyString(), eq(3), eq(0.55))).thenReturn(List.of(selfChunk));

        PolicyConflictReviewResponse response = reviewService.reviewForConflicts(1L);

        assertThat(response.hasConflicts()).isFalse();
        verify(chatClient, never()).prompt();
    }

    @Test
    void reviewForConflicts_providerThrows_marksAiRequestFailed() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentStorageService.load("stored.txt"))
                .thenReturn(new ByteArrayResource("Some new policy text.".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenReturn(new ExtractedDocument("Some new policy text.", List.of(), Map.of()));
        RetrievedChunk existingChunk = new RetrievedChunk(9L, "Other Policy", "LEAVE", "1.0", null, 0, null, "text", 0.9);
        when(knowledgeService.retrieveRelevant(anyString(), eq(3), eq(0.55))).thenReturn(List.of(existingChunk));

        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> reviewService.reviewForConflicts(1L))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_REQUEST_FAILED"));
    }

    @Test
    void reviewForConflicts_modelReturnsInvalidJson_throwsAiResponseInvalid() {
        when(documentService.findEntity(1L)).thenReturn(readyDocument());
        when(knowledgeService.isAvailable()).thenReturn(true);
        when(documentStorageService.load("stored.txt"))
                .thenReturn(new ByteArrayResource("Some new policy text.".getBytes()));
        when(textExtractor.supports("text/plain", "policy.txt")).thenReturn(true);
        when(textExtractor.extract(any(), eq("policy.txt")))
                .thenReturn(new ExtractedDocument("Some new policy text.", List.of(), Map.of()));
        RetrievedChunk existingChunk = new RetrievedChunk(9L, "Other Policy", "LEAVE", "1.0", null, 0, null, "text", 0.9);
        when(knowledgeService.retrieveRelevant(anyString(), eq(3), eq(0.55))).thenReturn(List.of(existingChunk));

        mockChatClientChain("not valid json at all");

        assertThatThrownBy(() -> reviewService.reviewForConflicts(1L))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_RESPONSE_INVALID"));
    }
}
