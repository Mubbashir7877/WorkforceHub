package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PdfDocumentExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(String contentType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase();
        return name.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType);
    }

    @Override
    public ExtractedDocument extract(Resource resource, String filename) {
        byte[] bytes;
        try {
            bytes = resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "Could not read PDF file: " + filename);
        }

        List<ExtractedDocument.PageText> pages = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = stripper.getText(document);
                pages.add(new ExtractedDocument.PageText(pageNumber, pageText));
                fullText.append(pageText).append('\n');
            }
        } catch (IOException e) {
            throw new DocumentProcessingException("POLICY_PROCESSING_FAILED",
                    "Could not parse PDF file " + filename + ": " + e.getMessage());
        }

        if (fullText.toString().isBlank()) {
            throw new DocumentProcessingException("DOCUMENT_TEXT_EMPTY",
                    "No extractable text found in PDF " + filename
                            + " (scanned/image-only PDFs are not supported — OCR is out of scope).");
        }

        return new ExtractedDocument(fullText.toString(), pages, Map.of("format", "pdf", "pageCount", pages.size()));
    }
}
