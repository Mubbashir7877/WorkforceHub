package com.example.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit record of one retrieved chunk used to ground an assistant answer.
 * Stores only a short excerpt for auditing — never the full chunk/document text.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "ai_response_sources",
    indexes = {
        @Index(name = "idx_airs_message_id", columnList = "message_id")
    }
)
public class AiResponseSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_title", length = 255)
    private String documentTitle;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(length = 1000)
    private String excerpt;

    @Column(name = "similarity_score")
    private Double similarityScore;
}
