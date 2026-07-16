package com.example.employeemanagement.service;

import com.example.employeemanagement.extraction.ExtractedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void chunk_shortText_producesSingleChunkWithNullPage() {
        ExtractedDocument extracted = new ExtractedDocument("Short policy text.", List.of(), Map.of());

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extracted);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Short policy text.");
        assertThat(chunks.get(0).pageNumber()).isNull();
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(0);
    }

    @Test
    void chunk_longTextWithoutPages_splitsIntoOverlappingChunks() {
        String longText = "word ".repeat(500); // 2500 chars, well over the 1000-char chunk size
        ExtractedDocument extracted = new ExtractedDocument(longText, List.of(), Map.of());

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extracted);

        assertThat(chunks.size()).isGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
            assertThat(chunks.get(i).pageNumber()).isNull();
            assertThat(chunks.get(i).text()).isNotBlank();
        }
    }

    @Test
    void chunk_pdfWithPages_preservesPageNumberPerChunk() {
        ExtractedDocument extracted = new ExtractedDocument(
                "page one text\npage two text",
                List.of(
                        new ExtractedDocument.PageText(1, "Leave accrues at 1.25 days per month."),
                        new ExtractedDocument.PageText(2, "Unused leave carries over up to 5 days.")
                ),
                Map.of());

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extracted);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(1).pageNumber()).isEqualTo(2);
    }

    @Test
    void chunk_blankText_producesNoChunks() {
        ExtractedDocument extracted = new ExtractedDocument("   ", List.of(), Map.of());

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extracted);

        assertThat(chunks).isEmpty();
    }
}
