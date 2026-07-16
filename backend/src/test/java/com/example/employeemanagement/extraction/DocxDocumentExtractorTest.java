package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxDocumentExtractorTest {

    private final DocxDocumentExtractor extractor = new DocxDocumentExtractor();

    @Test
    void supports_docxFiles() {
        assertThat(extractor.supports(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "policy.docx"))
                .isTrue();
        assertThat(extractor.supports(null, "policy.pdf")).isFalse();
    }

    @Test
    void extract_validDocx_returnsFullTextWithNoPages() throws IOException {
        byte[] docxBytes = buildDocx("Remote work is permitted up to three days per week.");
        ByteArrayResource resource = new ByteArrayResource(docxBytes);

        ExtractedDocument extracted = extractor.extract(resource, "remote-work.docx");

        assertThat(extracted.fullText()).contains("Remote work is permitted");
        assertThat(extracted.pages()).isEmpty();
    }

    @Test
    void extract_corruptDocxBytes_throwsProcessingFailed() {
        ByteArrayResource resource = new ByteArrayResource("not a real docx file".getBytes());

        assertThatThrownBy(() -> extractor.extract(resource, "broken.docx"))
                .isInstanceOf(DocumentProcessingException.class);
    }

    private static byte[] buildDocx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
