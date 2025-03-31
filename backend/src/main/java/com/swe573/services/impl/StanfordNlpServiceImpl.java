package com.swe573.services.impl;

import com.swe573.services.NlpService;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;
import java.util.*;

@Service
public class StanfordNlpServiceImpl implements NlpService {

    private final StanfordCoreNLP pipeline;
    private final Set<String> stopWords;
    private static final int MAX_TEXT_LENGTH = 10000; // Maximum text length to process

    public StanfordNlpServiceImpl() {
        Properties props = new Properties();
        // Reduce annotators to only what's needed for keyword extraction
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");
        props.setProperty("ner.buildEntityMentions", "false");
        props.setProperty("ner.combinationMode", "HIGH_RECALL");
        props.setProperty("ner.applyFineGrained", "true");
        props.setProperty("ner.additional.regexner.mapping", "edu/stanford/nlp/models/kbp/english/gazetteers/regexner_caseless.tab");
        
        // Add custom rules for product names and technical terms
        props.setProperty("ner.fine.regexner.ignorecase", "true");
        props.setProperty("ner.fine.regexner.noDefaultOverwriteLabels", "PRODUCT,TECH");
        
        pipeline = new StanfordCoreNLP(props);
        stopWords = getStopWords();
    }

    @Override
    @Cacheable(value = "keywords", key = "#text", unless = "#result == null || #result.isEmpty()")
    public List<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<String> keywords = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class).toLowerCase();
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                
                // Include nouns, verbs, and adjectives that aren't stop words
                if ((pos.startsWith("NN") || pos.startsWith("VB") || pos.startsWith("JJ")) 
                    && !stopWords.contains(word)
                    && word.length() > 2
                    && !seen.contains(word)) {
                    keywords.add(word);
                    seen.add(word);
                    
                    // Also add lemma if it's different and meets criteria
                    if (!lemma.equals(word) && !stopWords.contains(lemma) && lemma.length() > 2) {
                        keywords.add(lemma);
                        seen.add(lemma);
                    }
                }
                
                // Add product names and technical terms
                if (word.matches("(?i)iphone|android|ios|ai|camera|system")) {
                    keywords.add(word);
                    seen.add(word);
                }
            }
        }

        return keywords;
    }

    @Override
    @Cacheable(value = "entities", key = "#text")
    public List<String> extractNamedEntities(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }

        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<String> entities = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            StringBuilder currentEntity = new StringBuilder();
            String currentPos = null;
            
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                
                // Consider proper nouns (NNP, NNPS) and capitalized words as entities
                boolean isEntity = pos.equals("NNP") || pos.equals("NNPS") || 
                                 (word.length() > 1 && Character.isUpperCase(word.charAt(0)));
                
                // Also consider product names and technical terms
                if (!isEntity && word.matches("(?i)iphone|android|ios|ai|camera|system")) {
                    isEntity = true;
                    pos = "PRODUCT";
                }
                
                if (isEntity) {
                    // If this is the start of a new entity or a different type of entity
                    if (currentPos == null || !currentPos.equals(pos)) {
                        // If we have a previous entity, add it
                        if (currentEntity.length() > 0) {
                            String entity = currentEntity.toString().trim();
                            if (!seen.contains(entity.toLowerCase()) && entity.length() > 2) {
                                entities.add(entity);
                                seen.add(entity.toLowerCase());
                            }
                        }
                        // Start a new entity
                        currentEntity = new StringBuilder(word);
                        currentPos = pos;
                    } else {
                        // Continue the current entity
                        currentEntity.append(" ").append(word);
                    }
                } else if (currentEntity.length() > 0) {
                    // If we have a previous entity, add it
                    String entity = currentEntity.toString().trim();
                    if (!seen.contains(entity.toLowerCase()) && entity.length() > 2) {
                        entities.add(entity);
                        seen.add(entity.toLowerCase());
                    }
                    currentEntity = new StringBuilder();
                    currentPos = null;
                }
            }
            
            // Add any remaining entity
            if (currentEntity.length() > 0) {
                String entity = currentEntity.toString().trim();
                if (!seen.contains(entity.toLowerCase()) && entity.length() > 2) {
                    entities.add(entity);
                    seen.add(entity.toLowerCase());
                }
            }
        }

        return entities;
    }

    @Override
    @Cacheable(value = "topics", key = "#text")
    public List<String> analyzeTopics(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        // Get keywords and named entities
        List<String> keywords = extractKeywords(text);
        List<String> entities = extractNamedEntities(text);
        
        // Convert everything to lowercase for topic analysis
        Set<String> topics = new HashSet<>();
        topics.addAll(keywords);
        topics.addAll(entities.stream().map(String::toLowerCase).toList());
        
        // Add technical terms and product names
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                
                // Convert to lowercase for comparison
                String wordLower = word.toLowerCase();
                String lemmaLower = lemma.toLowerCase();
                
                // Check for product names (e.g., "iPhone")
                if (pos.startsWith("NNP") || pos.startsWith("NNPS")) {
                    // Look for next token to handle multi-word product names
                    if (i + 1 < tokens.size()) {
                        CoreLabel nextToken = tokens.get(i + 1);
                        String nextWord = nextToken.get(CoreAnnotations.TextAnnotation.class);
                        String nextPos = nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                        
                        // If next token is a number or proper noun, combine them
                        if (nextWord.matches("\\d+") || nextPos.startsWith("NNP") || nextPos.startsWith("NNPS")) {
                            String combinedTerm = (word + nextWord).toLowerCase();
                            if (!stopWords.contains(combinedTerm) && combinedTerm.length() > 2) {
                                topics.add(combinedTerm);
                            }
                        }
                    }
                    
                    // Add single word if it's not a stop word
                    if (!stopWords.contains(wordLower) && wordLower.length() > 2) {
                        topics.add(wordLower);
                    }
                }
                
                // Add technical terms and acronyms
                if ((pos.equals("NN") || pos.equals("NNP") || word.matches("[A-Z]{2,}")) 
                    && !stopWords.contains(wordLower) 
                    && wordLower.length() > 2) {
                    topics.add(wordLower);
                    // Also add the lemma if it's different
                    if (!lemmaLower.equals(wordLower) && !stopWords.contains(lemmaLower) && lemmaLower.length() > 2) {
                        topics.add(lemmaLower);
                    }
                }
                
                // Add product names and technical terms
                if (wordLower.matches("iphone|android|ios|ai|camera|system|ux|ui")) {
                    topics.add(wordLower);
                }
            }
        }
        
        return new ArrayList<>(topics);
    }

    private Set<String> getStopWords() {
        return new HashSet<>(Arrays.asList(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their",
            "what", "so", "up", "out", "if", "about", "who", "get", "which", "go",
            "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
            "take", "people", "into", "year", "your", "good", "some", "could", "them",
            "see", "other", "than", "then", "now", "look", "only", "come", "its",
            "over", "think", "also", "back", "after", "use", "two", "how", "our",
            "work", "first", "well", "way", "even", "new", "want", "because", "any",
            "these", "give", "day", "most", "us"
        ));
    }
} 