package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentProcessingApplication.class, args);
        System.out.println("=================================================");
        System.out.println("🚀 ENTERPRISE DOCUMENT ENGINE IS ONLINE ON PORT 8081");
        System.out.println("=================================================");
    }
}