package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentProcessingService {

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    public DocumentRecord processAndSaveDocument(MultipartFile file) throws Exception {
        // 1. Extract text from the uploaded PDF using PDFBox 3.0
        byte[] fileBytes = file.getBytes();
        String extractedText = "";

        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        }

        // 2. Send the extracted text to Groq AI
        String aiJsonResponse = callGroqApi(extractedText);

        // 3. Save the metadata and AI results to MySQL
        DocumentRecord record = new DocumentRecord();
        record.setFileName(file.getOriginalFilename());
        record.setSummary(aiJsonResponse);
        record.setKeywords("AI Processed");

        return documentRepository.save(record);
    }

    private String callGroqApi(String text) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // Prevent overloading the free API token limit
        String safeText = text.length() > 3000 ? text.substring(0, 3000) : text;

        // Force the AI to return clean JSON
        String systemPrompt = "You are a professional document analyzer. Read the provided text and return ONLY a valid JSON object with three keys: 'documentType' (string), 'keyTopics' (array of strings), and 'summary' (1-sentence string). Do not include any other text, markdown formatting, or explanations.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", safeText);

        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(groqApiUrl, HttpMethod.POST, entity, String.class);

        // Parse the response to grab just the AI's actual JSON content
        JsonNode root = mapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}