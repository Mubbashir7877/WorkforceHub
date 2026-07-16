package com.example.employeemanagement.extraction;

import java.util.List;
import java.util.Map;

/**
 * fullText is always populated. pages is populated only when the source format has
 * a natural page boundary (PDF); it is empty for plain text/Markdown/DOCX, in which
 * case chunking falls back to splitting fullText directly (no page numbers).
 */
public record ExtractedDocument(
        String fullText,
        List<PageText> pages,
        Map<String, Object> metadata
) {
    public record PageText(int pageNumber, String text) {}
}
