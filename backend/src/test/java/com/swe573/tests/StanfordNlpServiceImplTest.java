package com.swe573.tests;

import com.swe573.services.NlpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StanfordNlpServiceImplTest {

    @Autowired
    private NlpService nlpService;

    @Test
    void extractKeywords_WithSimpleText() {
        String text = "The quick brown fox jumps over the lazy dog.";
        List<String> keywords = nlpService.extractKeywords(text);
        
        assertNotNull(keywords);
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("quick"));
        assertTrue(keywords.contains("brown"));
        assertTrue(keywords.contains("fox"));
        assertTrue(keywords.contains("jumps"));
        assertTrue(keywords.contains("lazy"));
        assertTrue(keywords.contains("dog"));
    }

    @Test
    void extractKeywords_WithTechnicalContent() {
        String text = "Java Spring Boot application with RESTful APIs and database integration.";
        List<String> keywords = nlpService.extractKeywords(text);
        
        assertNotNull(keywords);
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("java"));
        assertTrue(keywords.contains("spring"));
        assertTrue(keywords.contains("boot"));
        assertTrue(keywords.contains("application"));
        assertTrue(keywords.contains("restful"));
        assertTrue(keywords.contains("apis"));
        assertTrue(keywords.contains("database"));
        assertTrue(keywords.contains("integration"));
    }

    @Test
    void extractNamedEntities_WithNamesAndPlaces() {
        String text = "John Smith works at Google in New York.";
        List<String> entities = nlpService.extractNamedEntities(text);
        
        assertNotNull(entities);
        assertFalse(entities.isEmpty());
        assertTrue(entities.contains("John Smith"));
        assertTrue(entities.contains("Google"));
        assertTrue(entities.contains("New York"));
    }

    @Test
    void extractNamedEntities_WithTechnicalTerms() {
        String text = "The project uses Microsoft and Amazon Web Services for cloud computing.";
        List<String> entities = nlpService.extractNamedEntities(text);
        
        assertNotNull(entities);
        assertFalse(entities.isEmpty());
        assertTrue(entities.contains("Microsoft"));
        assertTrue(entities.contains("Amazon"));
    }

    @Test
    void analyzeTopics_WithMixedContent() {
        String text = "The new iPhone 13 features advanced AI capabilities and improved camera system.";
        List<String> topics = nlpService.analyzeTopics(text);
        
        assertNotNull(topics);
        assertFalse(topics.isEmpty());
        assertTrue(topics.contains("iphone"));
        assertTrue(topics.contains("ai"));
        assertTrue(topics.contains("camera"));
        assertTrue(topics.contains("system"));
    }

    @Test
    void analyzeTopics_WithEmptyText() {
        String text = "";
        List<String> topics = nlpService.analyzeTopics(text);
        
        assertNotNull(topics);
        assertTrue(topics.isEmpty());
    }

    @Test
    void extractKeywords_WithEmptyText() {
        String text = "";
        List<String> keywords = nlpService.extractKeywords(text);
        
        assertNotNull(keywords);
        assertTrue(keywords.isEmpty());
    }

    @Test
    void extractNamedEntities_WithEmptyText() {
        String text = "";
        List<String> entities = nlpService.extractNamedEntities(text);
        
        assertNotNull(entities);
        assertTrue(entities.isEmpty());
    }
} 