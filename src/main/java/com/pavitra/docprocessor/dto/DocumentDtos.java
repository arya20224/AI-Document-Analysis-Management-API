package com.pavitra.docprocessor.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentDtos {

    // What the API actually returns to the client - structured, typed,
    // and decoupled from the Document entity/persistence model.
    public record DocumentResponse(
            Long id,
            String fileName,
            String documentType,
            List<String> keyTopics,
            String summary,
            LocalDateTime processedAt
    ) {}

    // Internal shape used only to hold the parsed Groq JSON response
    // before it gets mapped onto a Document entity. Never sent to the client.
    public record GroqAnalysis(
            String documentType,
            List<String> keyTopics,
            String summary
    ) {}
}
