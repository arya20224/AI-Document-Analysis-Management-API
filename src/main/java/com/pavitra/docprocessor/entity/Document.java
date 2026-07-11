package com.pavitra.docprocessor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String documentType;

    // Stored as a comma-separated string at the DB level; the DTO layer
    // (DocumentResponse) converts this back into a proper List<String>
    // before it ever reaches the client.
    @Column(columnDefinition = "TEXT")
    private String keyTopics;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
