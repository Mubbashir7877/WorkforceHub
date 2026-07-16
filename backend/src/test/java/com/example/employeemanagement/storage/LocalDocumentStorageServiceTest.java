package com.example.employeemanagement.storage;

import com.example.employeemanagement.exception.DocumentUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalDocumentStorageService(tempDir.toString(), 1024L);
    }

    @Test
    void store_validPlainTextFile_savesAndReturnsMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello world".getBytes());

        StoredDocument stored = storageService.store(1L, file);

        assertThat(stored.originalFileName()).isEqualTo("notes.txt");
        assertThat(stored.contentType()).isEqualTo("text/plain");
        assertThat(tempDir.resolve(stored.storageLocation())).exists();
    }

    @Test
    void store_validPdfMagicBytes_succeeds() {
        byte[] pdfBytes = "%PDF-1.4 fake pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", pdfBytes);

        StoredDocument stored = storageService.store(2L, file);

        assertThat(stored.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void store_pdfExtensionWithoutPdfMagicBytes_throwsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes());

        assertThatThrownBy(() -> storageService.store(3L, file))
                .isInstanceOf(DocumentUploadException.class)
                .satisfies(ex -> assertThat(((DocumentUploadException) ex).getErrorCode())
                        .isEqualTo("UNSUPPORTED_DOCUMENT_TYPE"));
    }

    @Test
    void store_disallowedExtension_throwsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", "MZ".getBytes());

        assertThatThrownBy(() -> storageService.store(4L, file))
                .isInstanceOf(DocumentUploadException.class)
                .satisfies(ex -> assertThat(((DocumentUploadException) ex).getErrorCode())
                        .isEqualTo("UNSUPPORTED_DOCUMENT_TYPE"));
    }

    @Test
    void store_pathTraversalFilename_isRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> storageService.store(5L, file))
                .isInstanceOf(DocumentUploadException.class);
    }

    @Test
    void store_oversizedFile_throwsTooLarge() {
        byte[] big = new byte[2048];
        MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", big);

        assertThatThrownBy(() -> storageService.store(6L, file))
                .isInstanceOf(DocumentUploadException.class)
                .satisfies(ex -> assertThat(((DocumentUploadException) ex).getErrorCode())
                        .isEqualTo("DOCUMENT_TOO_LARGE"));
    }

    @Test
    void store_emptyFile_throwsUploadFailed() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storageService.store(7L, file))
                .isInstanceOf(DocumentUploadException.class);
    }

    @Test
    void load_existingFile_returnsReadableResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "readme.md", "text/markdown", "# Title".getBytes());
        StoredDocument stored = storageService.store(8L, file);

        Resource resource = storageService.load(stored.storageLocation());

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getInputStream().readAllBytes()).isEqualTo("# Title".getBytes());
    }

    @Test
    void delete_existingFile_removesItFromDisk() {
        MockMultipartFile file = new MockMultipartFile("file", "temp.md", "text/markdown", "content".getBytes());
        StoredDocument stored = storageService.store(9L, file);

        storageService.delete(stored.storageLocation());

        assertThat(tempDir.resolve(stored.storageLocation())).doesNotExist();
    }
}
