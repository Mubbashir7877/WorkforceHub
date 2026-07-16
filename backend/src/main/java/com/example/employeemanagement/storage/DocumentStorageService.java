package com.example.employeemanagement.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over where policy document originals physically live. The only
 * implementation today is local-disk (see LocalDocumentStorageService); Amazon S3
 * is the planned production replacement and can be swapped in behind this
 * interface without touching callers.
 */
public interface DocumentStorageService {

    StoredDocument store(Long documentId, MultipartFile file);

    Resource load(String storageLocation);

    void delete(String storageLocation);
}
