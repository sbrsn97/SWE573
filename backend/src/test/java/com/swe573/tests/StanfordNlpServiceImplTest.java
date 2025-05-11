package com.swe573.tests;

import com.swe573.services.NlpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StanfordNlpServiceImplTest {

    @Autowired
    private NlpService nlpService;
    
    private Set<String> originalProfanityWords;
    
    @BeforeEach
    void setUp() {
        // Store original profanity words to restore after tests
        originalProfanityWords = nlpService.getAllProfanityWords();
    }
    
    @AfterEach
    void tearDown() {
        // Reload original profanity words
        nlpService.reloadProfanityWords();
    }

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

    @Test
    void containsProfanity_WithCleanText() {
        String text = "This is a clean and appropriate message.";
        assertFalse(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithProfaneText() {
        String text = "This shit is unacceptable.";
        assertTrue(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithObfuscatedProfanity() {
        String text = "This sh*t is unacceptable.";
        assertTrue(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithProfanityInWord() {
        String text = "I'm talking about assessment in education.";
        // The word "assessment" contains "ass" but should not trigger the filter
        assertFalse(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithEmptyText() {
        String text = "";
        assertFalse(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithNullText() {
        assertFalse(nlpService.containsProfanity(null));
    }
    
    @Test
    void containsProfanity_WithTurkishProfanity() {
        String text = "Bu orospu çocuğu beni rahatsız ediyor.";
        assertTrue(nlpService.containsProfanity(text));
    }
    
    @Test
    void containsProfanity_WithObfuscatedTurkishProfanity() {
        String text = "Bu or*spu çocuğu beni rahatsız ediyor.";
        assertTrue(nlpService.containsProfanity(text));
    }
    
    @Test
    void addProfanityWord_ShouldDetectNewWordAfterAddition() {
        // Add a new profanity word
        String newWord = "testprofane" + System.currentTimeMillis();
        boolean added = nlpService.addProfanityWord(newWord, "en");
        assertTrue(added);
        
        // Test that it's detected
        String text = "This " + newWord + " word should be detected.";
        assertTrue(nlpService.containsProfanity(text));
        
        // Remove the test word
        nlpService.removeProfanityWord(newWord);
    }
    
    @Test
    void addProfanityWord_Turkish_ShouldDetectNewWordAfterAddition() {
        // Add a new Turkish profanity word
        String newWord = "testküfür" + System.currentTimeMillis();
        boolean added = nlpService.addProfanityWord(newWord, "tr");
        assertTrue(added);
        
        // Test that it's detected
        String text = "Bu " + newWord + " kelime tespit edilmelidir.";
        assertTrue(nlpService.containsProfanity(text));
        
        // Remove the test word
        nlpService.removeProfanityWord(newWord);
    }
    
    @Test
    void removeProfanityWord_ShouldNotDetectWordAfterRemoval() {
        // First ensure the default word exists and is detected
        String word = "shit";
        String text = "This " + word + " word should be detected.";
        assertTrue(nlpService.containsProfanity(text));
        
        // Remove the word
        boolean removed = nlpService.removeProfanityWord(word);
        assertTrue(removed);
        
        // Test that it's no longer detected
        assertFalse(nlpService.containsProfanity(text));
        
        // Add the word back for other tests
        nlpService.addProfanityWord(word, "en");
    }
    
    @Test
    void getAllProfanityWords_ShouldReturnNonEmptySet() {
        Set<String> words = nlpService.getAllProfanityWords();
        assertNotNull(words);
        assertFalse(words.isEmpty());
    }
    
    @Test
    void reloadProfanityWords_ShouldReturnPositiveCount() {
        int count = nlpService.reloadProfanityWords();
        assertTrue(count > 0);
    }
} 