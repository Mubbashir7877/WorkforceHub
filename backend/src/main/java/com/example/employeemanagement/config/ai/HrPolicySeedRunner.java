package com.example.employeemanagement.config.ai;

import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyCategory;
import com.example.employeemanagement.entity.PolicyProcessingStatus;
import com.example.employeemanagement.entity.StorageType;
import com.example.employeemanagement.extraction.DocumentTextExtractor;
import com.example.employeemanagement.extraction.ExtractedDocument;
import com.example.employeemanagement.repository.HrPolicyDocumentRepository;
import com.example.employeemanagement.service.HrPolicyKnowledgeService;
import com.example.employeemanagement.storage.DocumentStorageService;
import com.example.employeemanagement.storage.StoredDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a starter set of general workplace policy documents the first time the
 * policy library is empty, so the HR Assistant is grounded in something useful
 * out of the box instead of an empty vector store. Runs the same
 * upload -> process -> activate lifecycle as a real HR_ADMIN upload
 * (see HrPolicyDocumentService), just driven directly against the repositories
 * instead of an authenticated HTTP request, since there is no logged-in user at
 * startup.
 *
 * A no-op whenever AI/the knowledge base isn't available (nothing to embed
 * into), seeding is disabled, or any policy document already exists — safe to
 * leave enabled permanently, matching AdminBootstrapRunner's idiom.
 */
@Component
public class HrPolicySeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HrPolicySeedRunner.class);
    private static final String SEED_UPLOADER_EMAIL = "system@hr-policy-seed";

    private final HrPolicyDocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final HrPolicyKnowledgeService knowledgeService;
    private final List<DocumentTextExtractor> textExtractors;
    private final boolean seedDefaults;

    public HrPolicySeedRunner(
            HrPolicyDocumentRepository documentRepository,
            DocumentStorageService documentStorageService,
            HrPolicyKnowledgeService knowledgeService,
            List<DocumentTextExtractor> textExtractors,
            @Value("${app.hr-documents.seed-defaults:true}") boolean seedDefaults) {
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.knowledgeService = knowledgeService;
        this.textExtractors = textExtractors;
        this.seedDefaults = seedDefaults;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDefaults) {
            return;
        }
        if (!knowledgeService.isAvailable()) {
            log.info("Skipping HR policy seeding: AI/knowledge base is not enabled.");
            return;
        }
        if (documentRepository.count() > 0) {
            return;
        }

        DocumentTextExtractor extractor = textExtractors.stream()
                .filter(e -> e.supports("text/markdown", "policy.md"))
                .findFirst()
                .orElse(null);
        if (extractor == null) {
            log.warn("Skipping HR policy seeding: no text extractor available for Markdown content.");
            return;
        }

        int seeded = 0;
        for (DefaultPolicy policy : DefaultPolicies.ALL) {
            try {
                seedOne(policy, extractor);
                seeded++;
            } catch (Exception ex) {
                log.warn("Failed to seed default HR policy document '{}': {}", policy.title(), ex.getMessage(), ex);
            }
        }
        log.info("Seeded {} default HR policy document(s).", seeded);
    }

    void seedOne(DefaultPolicy policy, DocumentTextExtractor extractor) {
        HrPolicyDocument doc = new HrPolicyDocument();
        doc.setTitle(policy.title());
        doc.setDescription(policy.description());
        doc.setCategory(policy.category());
        doc.setVersion("1.0");
        doc.setEffectiveDate(LocalDate.now());
        doc.setActive(false);
        doc.setProcessingStatus(PolicyProcessingStatus.DRAFT);
        doc.setUploadedByUserId(null);
        doc.setUploadedByEmail(SEED_UPLOADER_EMAIL);
        doc = documentRepository.save(doc);

        String fileName = "seed-policy-" + doc.getId() + ".md";
        MultipartFile file = new InMemoryMarkdownFile(fileName, policy.content());
        StoredDocument stored = documentStorageService.store(doc.getId(), file);

        doc.setOriginalFileName(stored.originalFileName());
        doc.setContentType(stored.contentType());
        doc.setStorageType(StorageType.LOCAL);
        doc.setStorageLocation(stored.storageLocation());
        doc.setProcessingStatus(PolicyProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            Resource resource = documentStorageService.load(stored.storageLocation());
            ExtractedDocument extracted = extractor.extract(resource, stored.originalFileName());
            int chunkCount = knowledgeService.indexDocument(doc, extracted);

            doc.setChunkCount(chunkCount);
            doc.setProcessingStatus(PolicyProcessingStatus.READY);
            doc.setActive(true);
            documentRepository.save(doc);
        } catch (Exception ex) {
            doc.setProcessingStatus(PolicyProcessingStatus.FAILED);
            documentRepository.save(doc);
            throw ex;
        }
    }

    record DefaultPolicy(String title, String description, PolicyCategory category, String content) {}

    /** Minimal in-memory MultipartFile so seeding doesn't need spring-test on the main classpath. */
    private static final class InMemoryMarkdownFile implements MultipartFile {
        private final String filename;
        private final byte[] bytes;

        InMemoryMarkdownFile(String filename, String content) {
            this.filename = filename;
            this.bytes = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "text/markdown";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (var out = new java.io.FileOutputStream(dest)) {
                out.write(bytes);
            }
        }
    }
}
