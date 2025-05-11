package com.swe573.services;

import java.util.List;
import java.util.Set;

public interface NlpService {
    /**
     * Extract keywords from text
     * @param text The input text to analyze
     * @return List of extracted keywords
     */
    List<String> extractKeywords(String text);

    /**
     * Extract named entities from text
     * @param text The input text to analyze
     * @return List of extracted named entities
     */
    List<String> extractNamedEntities(String text);

    /**
     * Analyze text and return relevant topics
     * @param text The input text to analyze
     * @return List of relevant topics
     */
    List<String> analyzeTopics(String text);

    /**
     * Check if text contains profanity
     * @param text The input text to analyze
     * @return True if profanity is detected, false otherwise
     */
    boolean containsProfanity(String text);
    
    /**
     * Add a new profanity word to the filter
     * @param word The word to add
     * @param language The language code ("en" for English, "tr" for Turkish)
     * @return True if added successfully
     */
    boolean addProfanityWord(String word, String language);
    
    /**
     * Remove a profanity word from the filter
     * @param word The word to remove
     * @return True if removed successfully
     */
    boolean removeProfanityWord(String word);
    
    /**
     * Get all profanity words currently in the filter
     * @return Set of all profanity words
     */
    Set<String> getAllProfanityWords();
    
    /**
     * Reload profanity words from source files
     * @return Number of words loaded
     */
    int reloadProfanityWords();
} 