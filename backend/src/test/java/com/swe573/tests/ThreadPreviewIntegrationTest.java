package com.swe573.tests;

import com.swe573.dto.ThreadPreviewDTO;
import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.services.NlpService;
import com.swe573.services.WikidataService;
import com.swe573.services.ThreadPreviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ThreadPreviewIntegrationTest {

    @Autowired
    private ThreadPreviewService threadPreviewService;

    @Autowired
    private NlpService nlpService;

    @Autowired
    private WikidataService wikidataService;

    @Test
    void generatePreview_WithRealWikidataData() {
        // Given
        String title = "Java Programming Language";
        String content = "Java is a high-level, class-based, object-oriented programming language " +
                        "designed to have as few implementation dependencies as possible. " +
                        "It is a general-purpose programming language intended to let programmers " +
                        "write once, run anywhere (WORA), meaning that compiled Java code can run " +
                        "on all platforms that support Java without the need to recompile.";

        // When
        ThreadPreviewDTO preview = threadPreviewService.generatePreview(title, content);

        // Then
        assertNotNull(preview);
        assertNotNull(preview.getKeywords());
        assertNotNull(preview.getSuggestedEntities());
        assertNotNull(preview.getSuggestedProperties());

        // Verify keywords
        List<String> keywords = preview.getKeywords();
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("java"));
        assertTrue(keywords.contains("programming"));
        assertTrue(keywords.contains("language"));

        // Verify entities
        List<WikidataEntityDTO> entities = preview.getSuggestedEntities();
        assertFalse(entities.isEmpty());
        assertTrue(entities.stream().anyMatch(e -> 
            e.getLabel().toLowerCase().contains("java") ||
            e.getLabel().toLowerCase().contains("programming") ||
            e.getLabel().toLowerCase().contains("language")
        ));

        // Verify properties
        List<WikidataPropertyDTO> properties = preview.getSuggestedProperties();
        assertFalse(properties.isEmpty());
        assertTrue(properties.stream().anyMatch(p -> 
            p.getLabel().toLowerCase().contains("programming") ||
            p.getLabel().toLowerCase().contains("language") ||
            p.getLabel().toLowerCase().contains("implementation")
        ));
    }

    @Test
    void generatePreview_WithTechnicalStack() {
        // Given
        String title = "Spring Boot Microservices";
        String content = "Building microservices architecture using Spring Boot, " +
                        "Docker containers, and Kubernetes orchestration. " +
                        "The application uses RESTful APIs and follows cloud-native principles.";

        // When
        ThreadPreviewDTO preview = threadPreviewService.generatePreview(title, content);

        // Then
        assertNotNull(preview);
        assertNotNull(preview.getKeywords());
        assertNotNull(preview.getSuggestedEntities());
        assertNotNull(preview.getSuggestedProperties());

        // Verify keywords
        List<String> keywords = preview.getKeywords();
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("spring"));
        assertTrue(keywords.contains("boot"));
        assertTrue(keywords.contains("microservices"));

        // Verify entities
        List<WikidataEntityDTO> entities = preview.getSuggestedEntities();
        assertFalse(entities.isEmpty());
        assertTrue(entities.stream().anyMatch(e -> 
            e.getLabel().toLowerCase().contains("spring") ||
            e.getLabel().toLowerCase().contains("docker") ||
            e.getLabel().toLowerCase().contains("kubernetes")
        ));

        // Verify properties
        List<WikidataPropertyDTO> properties = preview.getSuggestedProperties();
        assertFalse(properties.isEmpty());
        assertTrue(properties.stream().anyMatch(p -> 
            p.getLabel().toLowerCase().contains("software") ||
            p.getLabel().toLowerCase().contains("framework") ||
            p.getLabel().toLowerCase().contains("architecture")
        ));
    }

    @Test
    void generatePreview_WithNonTechnicalContent() {
        // Given
        String title = "Mojito Cocktail Recipe";
        String content = "A classic Cuban cocktail made with white rum, fresh mint leaves, " +
                        "lime juice, sugar, and soda water. The drink is known for its " +
                        "refreshing taste and is popular in tropical regions.";

        // When
        ThreadPreviewDTO preview = threadPreviewService.generatePreview(title, content);

        // Then
        assertNotNull(preview);
        assertNotNull(preview.getKeywords());
        assertNotNull(preview.getSuggestedEntities());
        assertNotNull(preview.getSuggestedProperties());

        // Verify keywords
        List<String> keywords = preview.getKeywords();
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("mojito"));
        assertTrue(keywords.contains("cocktail"));
        assertTrue(keywords.contains("rum"));

        // Verify entities
        List<WikidataEntityDTO> entities = preview.getSuggestedEntities();
        assertFalse(entities.isEmpty());
        assertTrue(entities.stream().anyMatch(e -> 
            e.getLabel().toLowerCase().contains("mojito") ||
            e.getLabel().toLowerCase().contains("cuba") ||
            e.getLabel().toLowerCase().contains("rum")
        ));

        // Verify properties
        List<WikidataPropertyDTO> properties = preview.getSuggestedProperties();
        assertFalse(properties.isEmpty());
        assertTrue(properties.stream().anyMatch(p -> 
            p.getLabel().toLowerCase().contains("beverage") ||
            p.getLabel().toLowerCase().contains("alcohol") ||
            p.getLabel().toLowerCase().contains("ingredient")
        ));
    }
} 