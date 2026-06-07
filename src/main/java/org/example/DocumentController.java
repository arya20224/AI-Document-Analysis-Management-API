package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentProcessingService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        // 1. Validate the file
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".pdf")) {
            response.put("error", "Invalid file. Please upload a valid PDF document.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 2. Send the file to your AI service
            DocumentRecord savedRecord = documentService.processAndSaveDocument(file);

            // 3. Return the exact AI analysis to Postman
            response.put("status", "success");
            response.put("fileName", savedRecord.getFileName());
            response.put("aiAnalysis", savedRecord.getSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to process document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}