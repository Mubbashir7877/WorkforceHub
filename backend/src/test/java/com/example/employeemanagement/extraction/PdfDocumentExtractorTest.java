package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentExtractorTest {

    private final PdfDocumentExtractor extractor = new PdfDocumentExtractor();

    @Test
    void supports_pdfFiles() {
        assertThat(extractor.supports("application/pdf", "policy.pdf")).isTrue();
        assertThat(extractor.supports(null, "policy.txt")).isFalse();
    }

    @Test
    void extract_twoPagePdf_returnsPerPageText() throws IOException {
        byte[] pdfBytes = buildTwoPagePdf("Page one leave policy text.", "Page two benefits policy text.");
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        ExtractedDocument extracted = extractor.extract(resource, "policy.pdf");

        assertThat(extracted.pages()).hasSize(2);
        assertThat(extracted.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(extracted.pages().get(0).text()).contains("Page one leave policy");
        assertThat(extracted.pages().get(1).pageNumber()).isEqualTo(2);
        assertThat(extracted.pages().get(1).text()).contains("Page two benefits policy");
        assertThat(extracted.fullText()).contains("Page one").contains("Page two");
    }

    @Test
    void extract_corruptPdfBytes_throwsProcessingFailed() {
        ByteArrayResource resource = new ByteArrayResource("%PDF-1.4 not really a pdf".getBytes());

        assertThatThrownBy(() -> extractor.extract(resource, "broken.pdf"))
                .isInstanceOf(DocumentProcessingException.class);
    }

    private static byte[] buildTwoPagePdf(String page1Text, String page2Text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            addPage(document, page1Text);
            addPage(document, page2Text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void addPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText(text);
            stream.endText();
        }
    }
}
