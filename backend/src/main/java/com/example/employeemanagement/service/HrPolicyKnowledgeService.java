package com.example.employeemanagement.service;

import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.exception.DocumentProcessingException;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the RAG knowledge base: chunking, embedding, and querying the vector
 * store. Deliberately depends on HrPolicyDocumentRepository (not
 * HrPolicyDocumentService) so HrPolicyDocumentService can safely depend on this
 * class for indexing without creating a circular dependency.
 *
 * All vector-store beans are conditional on AI being enabled/configured (see
 * config/ai/AiClientConfig), so they are injected via ObjectProvider and every
 * public method fails with a structured AiUnavailableException rather than a
 * NullPointerException when AI is off.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPolicyKnowledgeService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final DocumentChunker documentChunker;
    private final HrPolicyDocumentRepository documentRepository;

    public boolean isAvailable() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    /**
     * Chunks + embeds the extracted text and (re)indexes it, replacing any chunks
     * left over from a previous processing run. Returns the new chunk count.
     */
    public int indexDocument(HrPolicyDocument document, ExtractedDocument extracted) {
        VectorStore vectorStore = requireVectorStore();

        List<DocumentChunker.Chunk> chunks = documentChunker.chunk(extracted);
        if (chunks.isEmpty()) {
            throw new DocumentProcessingException("DOCUMENT_TEXT_EMPTY",
                    "No chunks could be produced from the extracted text.");
        }

        removeChunks(document, vectorStore);

        List<org.springframework.ai.document.Document> aiDocuments = new ArrayList<>(chunks.size());
        for (DocumentChunker.Chunk chunk : chunks) {
            aiDocuments.add(org.springframework.ai.document.Document.builder()
                    .id(chunkId(document.getId(), chunk.chunkIndex()))
                    .text(chunk.text())
                    .metadata(chunkMetadata(document, chunk))
                    .build());
        }

        vectorStore.add(aiDocuments);
        return chunks.size();
    }

    /** Removes all currently-indexed chunks for a document (e.g. on deactivation). */
    public void removeChunks(HrPolicyDocument document) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return; // AI was never enabled — nothing could have been indexed
        }
        removeChunks(document, vectorStore);
    }

    public List<RetrievedChunk> retrieveRelevant(String question, int topK, double similarityThreshold) {
        VectorStore vectorStore = requireVectorStore();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<RetrievedChunk> retrieved = new ArrayList<>();
        for (org.springframework.ai.document.Document result : vectorStore.similaritySearch(request)) {
            Map<String, Object> metadata = result.getMetadata();
            Long documentId = asLong(metadata.get("documentId"));
            if (documentId == null) {
                continue;
            }

            // Defense-in-depth: never surface a chunk whose owning document has since
            // been deactivated or isn't fully processed, even if the vector store
            // still holds stale entries for it.
            HrPolicyDocument doc = documentRepository.findById(documentId).orElse(null);
            if (doc == null || !doc.isActive() || doc.getProcessingStatus() != PolicyProcessingStatus.READY) {
                continue;
            }

            retrieved.add(new RetrievedChunk(
                    documentId,
                    (String) metadata.get("documentTitle"),
                    (String) metadata.get("category"),
                    (String) metadata.get("version"),
                    asInteger(metadata.get("pageNumber")),
                    asInteger(metadata.get("chunkIndex")) != null ? asInteger(metadata.get("chunkIndex")) : 0,
                    (String) metadata.get("effectiveDate"),
                    result.getText(),
                    result.getScore() != null ? result.getScore() : 0.0
            ));
        }
        return retrieved;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void removeChunks(HrPolicyDocument document, VectorStore vectorStore) {
        if (document.getChunkCount() <= 0) {
            return;
        }
        List<String> ids = new ArrayList<>(document.getChunkCount());
        for (int i = 0; i < document.getChunkCount(); i++) {
            ids.add(chunkId(document.getId(), i));
        }
        vectorStore.delete(ids);
    }

    private static Map<String, Object> chunkMetadata(HrPolicyDocument document, DocumentChunker.Chunk chunk) {
        // Document rejects null metadata values, so only non-null fields are added.
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "documentId", document.getId());
        putIfNotNull(metadata, "documentTitle", document.getTitle());
        putIfNotNull(metadata, "category", document.getCategory() != null ? document.getCategory().name() : null);
        putIfNotNull(metadata, "version", document.getVersion());
        putIfNotNull(metadata, "pageNumber", chunk.pageNumber());
        putIfNotNull(metadata, "sectionName", document.getTitle());
        putIfNotNull(metadata, "chunkIndex", chunk.chunkIndex());
        putIfNotNull(metadata, "effectiveDate",
                document.getEffectiveDate() != null ? document.getEffectiveDate().toString() : null);
        return metadata;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static String chunkId(Long documentId, int chunkIndex) {
        return "doc-" + documentId + "-chunk-" + chunkIndex;
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private VectorStore requireVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new AiUnavailableException("KNOWLEDGE_BASE_UNAVAILABLE",
                    "The HR knowledge base is not available right now.");
        }
        return vectorStore;
    }
}
