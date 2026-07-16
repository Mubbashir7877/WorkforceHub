package com.example.employeemanagement.entity;

/**
 * Where a policy document's original file lives. Only LOCAL is implemented in
 * this phase; S3 is the planned future replacement (see DocumentStorageService).
 */
public enum StorageType {
    LOCAL,
    S3
}
