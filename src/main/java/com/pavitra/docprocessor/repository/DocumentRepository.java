package com.pavitra.docprocessor.repository;

import com.pavitra.docprocessor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
