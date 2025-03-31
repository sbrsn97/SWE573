package com.swe573.services;

import java.util.List;

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
} 