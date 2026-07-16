package com.example.employeemanagement.service;

import com.example.employeemanagement.extraction.ExtractedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted document text into overlapping, roughly-fixed-size chunks
 * suitable for embedding. Chunks are split per-page when page boundaries are
 * known (PDF), so retrieved chunks keep an accurate pageNumber; otherwise the
 * whole fullText is split directly and pageNumber is left null.
 */
@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 150;

    public List<Chunk> chunk(ExtractedDocument extracted) {
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;

        if (extracted.pages() != null && !extracted.pages().isEmpty()) {
            for (ExtractedDocument.PageText page : extracted.pages()) {
                for (String piece : splitText(page.text())) {
                    chunks.add(new Chunk(piece, page.pageNumber(), index++));
                }
            }
        } else {
            for (String piece : splitText(extracted.fullText())) {
                chunks.add(new Chunk(piece, null, index++));
            }
        }
        return chunks;
    }

    private List<String> splitText(String text) {
        List<String> pieces = new ArrayList<>();
        if (text == null) {
            return pieces;
        }
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return pieces;
        }

        int length = normalized.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            if (end < length) {
                int lastSpace = normalized.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }
            if (end >= length) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return pieces;
    }

    public record Chunk(String text, Integer pageNumber, int chunkIndex) {}
}
