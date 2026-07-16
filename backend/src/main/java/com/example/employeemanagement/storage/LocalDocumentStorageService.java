package com.example.employeemanagement.storage;

import com.example.employeemanagement.exception.DocumentUploadException;
import com.example.employeemanagement.exception.PolicyDocumentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores policy document originals on local disk, outside any Git-tracked
 * directory (see app.hr-documents.storage-path / HR_DOCUMENT_STORAGE_PATH).
 * Stored filenames are always server-generated (never the client-supplied name),
 * which rules out path traversal and unsafe/duplicate filenames by construction.
 */
@Slf4j
@Component
public class LocalDocumentStorageService implements DocumentStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt", "md", "docx");

    private final Path storageRoot;
    private final long maxUploadBytes;

    public LocalDocumentStorageService(
            @Value("${app.hr-documents.storage-path}") String storagePath,
            @Value("${app.hr-documents.max-upload-bytes}") long maxUploadBytes) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes;
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create HR document storage directory: " + storageRoot, e);
        }
    }

    @Override
    public StoredDocument store(Long documentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Uploaded file is empty.");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new DocumentUploadException("DOCUMENT_TOO_LARGE",
                    "File exceeds the maximum allowed size of " + maxUploadBytes + " bytes.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalFilename.isBlank() || originalFilename.contains("..")
                || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Filename is not valid.");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new DocumentUploadException("UNSUPPORTED_DOCUMENT_TYPE",
                    "Unsupported file type '" + extension + "'. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Could not read uploaded file.");
        }

        // Client-supplied Content-Type is never trusted for validation — only the
        // resolved (server-side) extension and, for binary formats, magic bytes are.
        validateMagicBytes(extension, content, originalFilename);

        String storedFileName = "doc-" + documentId + "-" + UUID.randomUUID() + "." + extension;
        Path target = storageRoot.resolve(storedFileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Invalid storage path.");
        }

        try {
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store policy document file for document {}", documentId, e);
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Could not store uploaded file.");
        }

        return new StoredDocument(storedFileName, originalFilename, resolveContentType(extension), content.length);
    }

    @Override
    public Resource load(String storageLocation) {
        Path target = resolveExisting(storageLocation);
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageLocation) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageLocation));
        } catch (IOException e) {
            log.warn("Failed to delete stored policy document file {}: {}", storageLocation, e.getMessage());
        }
    }

    private Path resolveExisting(String storageLocation) {
        Path target = resolveWithinRoot(storageLocation);
        if (!Files.exists(target)) {
            throw new PolicyDocumentNotFoundException("Stored file not found: " + storageLocation);
        }
        return target;
    }

    private Path resolveWithinRoot(String storageLocation) {
        Path target = storageRoot.resolve(storageLocation).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new DocumentUploadException("POLICY_UPLOAD_FAILED", "Invalid storage location.");
        }
        return target;
    }

    private static String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    private static void validateMagicBytes(String extension, byte[] content, String originalFilename) {
        switch (extension) {
            case "pdf" -> {
                if (content.length < 5 || !"%PDF-".equals(new String(content, 0, 5, StandardCharsets.US_ASCII))) {
                    throw new DocumentUploadException("UNSUPPORTED_DOCUMENT_TYPE",
                            "File content does not look like a valid PDF: " + originalFilename);
                }
            }
            case "docx" -> {
                boolean looksLikeZip = content.length >= 4
                        && content[0] == 0x50 && content[1] == 0x4B
                        && content[2] == 0x03 && content[3] == 0x04;
                if (!looksLikeZip) {
                    throw new DocumentUploadException("UNSUPPORTED_DOCUMENT_TYPE",
                            "File content does not look like a valid DOCX: " + originalFilename);
                }
            }
            default -> {
                // txt/md: plain text has no reliable magic-byte signature to check.
            }
        }
    }

    private static String resolveContentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "md" -> "text/markdown";
            default -> "text/plain";
        };
    }
}
