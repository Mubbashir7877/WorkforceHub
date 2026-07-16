package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * DOCX has no reliable page-boundary metadata without a full layout engine, so
 * the whole document is treated as a single logical section (pages is empty and
 * chunking falls back to splitting fullText directly).
 */
@Component
public class DocxDocumentExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase();
        return name.endsWith(".docx")
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType);
    }

    @Override
    public ExtractedDocument extract(Resource resource, String filename) {
        String text;
        try (InputStream in = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            text = extractor.getText();
        } catch (IOException e) {
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "Could not parse DOCX file " + filename + ": " + e.getMessage());
        } catch (RuntimeException e) {
            // POI throws unchecked exceptions (e.g. OLE2/POIXMLException) for corrupt files.
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "File does not appear to be a valid DOCX: " + filename);
        }

        if (text == null || text.isBlank()) {
            throw new DocumentProcessingException("DOCUMENT_TEXT_EMPTY",
                    "No meaningful text could be extracted from " + filename);
        }

        return new ExtractedDocument(text, List.of(), Map.of("format", "docx"));
    }
}
