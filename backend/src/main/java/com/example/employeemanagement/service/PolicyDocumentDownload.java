package com.example.employeemanagement.service;

import org.springframework.core.io.Resource;

public record PolicyDocumentDownload(Resource resource, String filename, String contentType) {}
