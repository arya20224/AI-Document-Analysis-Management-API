package com.pavitra.docprocessor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavitra.docprocessor.dto.DocumentDtos.*;
import com.pavitra.docprocessor.entity.Document;
import com.pavitra.docprocessor.exception.DocumentNotFoundException;
import com.pavitra.docprocessor.exception.InvalidFileException;
import com.pavitra.docprocessor.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    private static final int MAX_CHARS_SENT_TO_LLM = 3000;

    public DocumentResponse processAndSaveDocument(MultipartFile file) {
        validateFile(file);

        String extractedText = extractText(file);
        GroqAnalysis analysis = analyzeWithGroq(extractedText);

        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .documentType(analysis.documentType())
                .keyTopics(String.join(", ", analysis.keyTopics()))
                .summary(analysis.summary())
                .build();

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DocumentResponse getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));
        return toResponse(document);
    }

    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new DocumentNotFoundException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF files are supported");
        }
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new InvalidFileException("Could not read PDF content: " + e.getMessage());
        }
    }

    /**
     * Sends extracted text to Groq and actually parses the structured JSON
     * it returns into documentType / keyTopics / summary - instead of the
     * old version, which just shoved the entire raw JSON string into one
     * "summary" field and called it done.
     */
    @SuppressWarnings("unchecked")
    private GroqAnalysis analyzeWithGroq(String extractedText) {
        String safeText = extractedText.length() > MAX_CHARS_SENT_TO_LLM
                ? extractedText.substring(0, MAX_CHARS_SENT_TO_LLM)
                : extractedText;

        String systemPrompt = "You are a professional document analyzer. Read the provided text and " +
                "return ONLY a valid JSON object with three keys: 'documentType' (string), " +
                "'keyTopics' (array of strings), and 'summary' (1-sentence string). " +
                "Do not include any other text, markdown formatting, or explanations.";

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", safeText)
                ),
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(groqApiUrl, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode parsed = objectMapper.readTree(content);

            List<String> topics = new ArrayList<>();
            if (parsed.has("keyTopics") && parsed.get("keyTopics").isArray()) {
                parsed.get("keyTopics").forEach(node -> topics.add(node.asText()));
            }

            return new GroqAnalysis(
                    parsed.path("documentType").asText("Unknown"),
                    topics,
                    parsed.path("summary").asText("")
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze document with Groq API: " + e.getMessage(), e);
        }
    }

    private DocumentResponse toResponse(Document d) {
        List<String> topics = (d.getKeyTopics() == null || d.getKeyTopics().isBlank())
                ? List.of()
                : Arrays.stream(d.getKeyTopics().split(",\\s*")).collect(Collectors.toList());

        return new DocumentResponse(
                d.getId(),
                d.getFileName(),
                d.getDocumentType(),
                topics,
                d.getSummary(),
                d.getProcessedAt()
        );
    }
}
