package com.example.employeemanagement.service;

import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyCategory;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.exception.DocumentProcessingException;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrPolicyKnowledgeServiceTest {

    @Mock
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private HrPolicyDocumentRepository documentRepository;

    private HrPolicyKnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new HrPolicyKnowledgeService(vectorStoreProvider, new DocumentChunker(), documentRepository);
    }

    private HrPolicyDocument sampleDocument() {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setId(1L);
        doc.setTitle("Leave Policy");
        doc.setCategory(PolicyCategory.LEAVE);
        doc.setVersion("1.0");
        return doc;
    }

    @Test
    void isAvailable_whenVectorStoreBeanAbsent_returnsFalse() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        assertThat(knowledgeService.isAvailable()).isFalse();
    }

    @Test
    void indexDocument_whenAiDisabled_throwsKnowledgeBaseUnavailable() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        ExtractedDocument extracted = new ExtractedDocument("Some policy text.", List.of(), Map.of());

        assertThatThrownBy(() -> knowledgeService.indexDocument(sampleDocument(), extracted))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode())
                        .isEqualTo("KNOWLEDGE_BASE_UNAVAILABLE"));
    }

    @Test
    void indexDocument_emptyExtractedText_throwsDocumentTextEmpty() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);
        ExtractedDocument extracted = new ExtractedDocument("   ", List.of(), Map.of());

        assertThatThrownBy(() -> knowledgeService.indexDocument(sampleDocument(), extracted))
                .isInstanceOf(DocumentProcessingException.class)
                .satisfies(ex -> assertThat(((DocumentProcessingException) ex).getErrorCode())
                        .isEqualTo("DOCUMENT_TEXT_EMPTY"));

        verify(vectorStore, never()).add(any());
    }

    @Test
    void indexDocument_validText_addsChunksWithDeterministicIdsAndMetadata() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);
        HrPolicyDocument doc = sampleDocument();
        ExtractedDocument extracted = new ExtractedDocument("Employees accrue fifteen days of annual leave.", List.of(), Map.of());

        int chunkCount = knowledgeService.indexDocument(doc, extracted);

        assertThat(chunkCount).isEqualTo(1);
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document added = captor.getValue().get(0);
        assertThat(added.getId()).isEqualTo("doc-1-chunk-0");
        assertThat(added.getMetadata()).containsEntry("documentId", 1L);
        assertThat(added.getMetadata()).containsEntry("documentTitle", "Leave Policy");
        assertThat(added.getMetadata()).containsEntry("category", "LEAVE");
    }

    @Test
    void indexDocument_reprocessing_deletesPreviousChunksBeforeAdding() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);
        HrPolicyDocument doc = sampleDocument();
        doc.setChunkCount(2); // simulate a prior processing run with 2 chunks
        ExtractedDocument extracted = new ExtractedDocument("New replacement text for the policy.", List.of(), Map.of());

        knowledgeService.indexDocument(doc, extracted);

        verify(vectorStore).delete(List.of("doc-1-chunk-0", "doc-1-chunk-1"));
        verify(vectorStore).add(any());
    }

    @Test
    void removeChunks_withNoPriorChunks_doesNotCallDelete() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);
        HrPolicyDocument doc = sampleDocument();
        doc.setChunkCount(0);

        knowledgeService.removeChunks(doc);

        verify(vectorStore, never()).delete(anyList());
    }

    @Test
    void removeChunks_whenAiNeverEnabled_isNoOp() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        HrPolicyDocument doc = sampleDocument();
        doc.setChunkCount(3);

        knowledgeService.removeChunks(doc); // should not throw
    }

    @Test
    void retrieveRelevant_filtersOutInactiveOrNotReadyDocuments() {
        when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);

        Map<String, Object> activeMetadata = new HashMap<>();
        activeMetadata.put("documentId", 1L);
        activeMetadata.put("documentTitle", "Leave Policy");
        activeMetadata.put("category", "LEAVE");
        Document activeChunk = Document.builder().id("doc-1-chunk-0").text("active chunk").metadata(activeMetadata).build();

        Map<String, Object> inactiveMetadata = new HashMap<>();
        inactiveMetadata.put("documentId", 2L);
        Document inactiveChunk = Document.builder().id("doc-2-chunk-0").text("inactive chunk").metadata(inactiveMetadata).build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(activeChunk, inactiveChunk));

        HrPolicyDocument activeDoc = sampleDocument();
        activeDoc.setActive(true);
        activeDoc.setProcessingStatus(PolicyProcessingStatus.READY);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(activeDoc));

        HrPolicyDocument inactiveDoc = new HrPolicyDocument();
        inactiveDoc.setId(2L);
        inactiveDoc.setActive(false);
        inactiveDoc.setProcessingStatus(PolicyProcessingStatus.INACTIVE);
        when(documentRepository.findById(2L)).thenReturn(Optional.of(inactiveDoc));

        List<RetrievedChunk> result = knowledgeService.retrieveRelevant("How many leave days?", 5, 0.5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).documentId()).isEqualTo(1L);
        assertThat(result.get(0).content()).isEqualTo("active chunk");
    }
}
