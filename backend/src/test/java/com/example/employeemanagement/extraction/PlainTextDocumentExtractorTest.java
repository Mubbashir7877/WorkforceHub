package com.example.employeemanagement.extraction;

import com.example.employeemanagement.exception.DocumentProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainTextDocumentExtractorTest {

    private final PlainTextDocumentExtractor extractor = new PlainTextDocumentExtractor();

    @Test
    void supports_txtAndMdFiles() {
        assertThat(extractor.supports("text/plain", "policy.txt")).isTrue();
        assertThat(extractor.supports(null, "policy.md")).isTrue();
        assertThat(extractor.supports("application/pdf", "policy.pdf")).isFalse();
    }

    @Test
    void extract_validText_returnsFullTextWithNoPages() {
        ByteArrayResource resource = new ByteArrayResource(
                "Employees accrue 15 days of leave per year.".getBytes(StandardCharsets.UTF_8));

        ExtractedDocument extracted = extractor.extract(resource, "leave.txt");

        assertThat(extracted.fullText()).contains("15 days of leave");
        assertThat(extracted.pages()).isEmpty();
    }

    @Test
    void extract_blankContent_throwsDocumentTextEmpty() {
        ByteArrayResource resource = new ByteArrayResource("   \n  ".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> extractor.extract(resource, "empty.txt"))
                .isInstanceOf(DocumentProcessingException.class)
                .satisfies(ex -> assertThat(((DocumentProcessingException) ex).getErrorCode())
                        .isEqualTo("DOCUMENT_TEXT_EMPTY"));
    }
}
