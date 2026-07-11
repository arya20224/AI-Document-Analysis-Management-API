package com.pavitra.docprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate is registered as a bean here (instead of "new RestTemplate()"
 * inline inside the service) so it can be injected via the constructor,
 * same pattern used across the Parking Slot Booking API. This also makes
 * the service class actually testable - a mock RestTemplate can be swapped
 * in during unit tests instead of always hitting the real Groq API.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
