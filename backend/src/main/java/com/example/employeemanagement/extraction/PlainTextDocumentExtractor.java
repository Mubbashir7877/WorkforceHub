package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class PlainTextDocumentExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md")
                || "text/plain".equalsIgnoreCase(contentType)
                || "text/markdown".equalsIgnoreCase(contentType);
    }

    @Override
    public ExtractedDocument extract(Resource resource, String filename) {
        String text;
        try {
            text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "Could not read text file: " + filename);
        }

        if (text.isBlank()) {
            throw new DocumentProcessingException("DOCUMENT_TEXT_EMPTY",
                    "No meaningful text could be extracted from " + filename);
        }

        return new ExtractedDocument(text, List.of(), Map.of("format", "text"));
    }
}
