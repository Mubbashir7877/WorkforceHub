package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.HrPolicyDocument;
import com.example.employeemanagement.entity.PolicyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HrPolicyDocumentRepository extends JpaRepository<HrPolicyDocument, Long> {

    @Query("SELECT d FROM HrPolicyDocument d " +
           "WHERE (:category IS NULL OR d.category = :category) " +
           "AND   (:active   IS NULL OR d.active   = :active) " +
           "ORDER BY d.title ASC")
    Page<HrPolicyDocument> findFiltered(
            @Param("category") PolicyCategory category,
            @Param("active") Boolean active,
            Pageable pageable
    );

    long countByActiveTrue();
}
