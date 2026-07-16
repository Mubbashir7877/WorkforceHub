package com.example.employeemanagement.extraction;

import org.springframework.core.io.Resource;

public interface DocumentTextExtractor {

    boolean supports(String contentType, String filename);

    ExtractedDocument extract(Resource resource, String filename);
}
