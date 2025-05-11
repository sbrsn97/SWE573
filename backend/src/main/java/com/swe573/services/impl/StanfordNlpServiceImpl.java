package com.swe573.services.impl;

import com.swe573.services.NlpService;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import java.util.*;

@Service
public class StanfordNlpServiceImpl implements NlpService {

    private final StanfordCoreNLP pipeline;
    private final Set<String> stopWords;
    private static final int MAX_TEXT_LENGTH = 10000; // Maximum text length to process
    private Set<String> profanityWords;
    private static final String ENGLISH_PROFANITY_FILE = "profanity/english_profanity.txt";
    private static final String TURKISH_PROFANITY_FILE = "profanity/turkish_profanity.txt";
    private static final String ENCRYPTION_KEY = "SWE573ProfanityFilterSecretKey123";
    private static final String FILE_WARNING = "#this file is full of disgusting words. decrypt at your own risk";
    
    @Autowired
    private CacheManager cacheManager;

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
        
        // Initialize profanity words set
        profanityWords = loadProfanityWords();
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
    
    private Set<String> getProfanityWords() {
        return profanityWords;
    }
    
    @Override
    @Cacheable(value = "profanityCheck", key = "#text")
    public boolean containsProfanity(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        
        // Trim and convert to lowercase
        String normalizedText = text.toLowerCase().trim();
        
        // Get the profanity list
        Set<String> profanityWordsList;
        synchronized (profanityWords) {
            profanityWordsList = new HashSet<>(profanityWords);
        }
        
        // Simple word-based check
        String[] words = normalizedText.split("\\s+");
        for (String word : words) {
            // Remove common punctuation
            word = word.replaceAll("[.,!?;:\\-\\(\\)\\[\\]{}'\"]", "");
            
            // Check for exact matches
            if (profanityWordsList.contains(word)) {
                return true;
            }
            
            // Check for obfuscated words (e.g., "f*ck", "s**t")
            for (String profanity : profanityWordsList) {
                if (word.length() >= 2 && 
                    word.charAt(0) == profanity.charAt(0) && 
                    word.charAt(word.length() - 1) == profanity.charAt(profanity.length() - 1)) {
                    
                    // Check if the word has wildcards or special characters in the middle
                    String middle = word.substring(1, word.length() - 1);
                    if (middle.matches("[\\*$#@\\-\\.]+") && 
                        middle.length() == profanity.length() - 2) {
                        return true;
                    }
                }
            }
        }
        
        // Additional check for combined words or words without spaces
        for (String profanity : profanityWordsList) {
            // Use word boundary to catch standalone occurrences
            String regex = "\\b" + profanity + "\\b";
            if (normalizedText.matches(".*" + regex + ".*")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Loads profanity words from both English and Turkish files
     * Creates resource directories and files if they don't exist
     * @return Combined set of profanity words
     */
    private Set<String> loadProfanityWords() {
        Set<String> words = new HashSet<>();
        
        try {
            // Create resource directories if they don't exist
            createResourceDirectoriesIfNeeded();
            
            // Try to load from English file
            try {
                words.addAll(loadWordsFromFile(ENGLISH_PROFANITY_FILE));
            } catch (Exception e) {
                System.out.println("Could not load English profanity file. Using default list: " + e.getMessage());
                // Add default English profanity words if file can't be loaded
                words.addAll(getDefaultEnglishProfanityWords());
                // Try to create the file with default words for future use
                createProfanityFile(ENGLISH_PROFANITY_FILE, getDefaultEnglishProfanityWords());
            }
            
            // Try to load from Turkish file
            try {
                words.addAll(loadWordsFromFile(TURKISH_PROFANITY_FILE));
            } catch (Exception e) {
                System.out.println("Could not load Turkish profanity file. Using default list: " + e.getMessage());
                // Add default Turkish profanity words if file can't be loaded
                words.addAll(getDefaultTurkishProfanityWords());
                // Try to create the file with default words for future use
                createProfanityFile(TURKISH_PROFANITY_FILE, getDefaultTurkishProfanityWords());
            }
        } catch (Exception e) {
            System.out.println("Error loading profanity files: " + e.getMessage());
            e.printStackTrace();
            // Fallback to hardcoded lists
            words.addAll(getDefaultEnglishProfanityWords());
            words.addAll(getDefaultTurkishProfanityWords());
        }
        
        return words;
    }

    /**
     * Creates resource directories for profanity files if they don't exist
     */
    private void createResourceDirectoriesIfNeeded() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("profanity");
            if (!java.nio.file.Files.exists(path)) {
                java.nio.file.Files.createDirectories(path);
                System.out.println("Created profanity directory");
            }
        } catch (Exception e) {
            System.out.println("Failed to create profanity directory: " + e.getMessage());
        }
    }

    /**
     * Loads words from a file, one word per line
     * @param filename The path to the file
     * @return Set of words from the file
     */
    private Set<String> loadWordsFromFile(String filename) throws Exception {
        Set<String> words = new HashSet<>();
        java.nio.file.Path path = java.nio.file.Paths.get(filename);
        
        if (java.nio.file.Files.exists(path)) {
            // Read encrypted content
            byte[] encryptedContent = java.nio.file.Files.readAllBytes(path);
            
            // Skip the first line if it's a warning comment
            List<String> lines = new ArrayList<>();
            try {
                // Try to decrypt the file
                String content = decryptContent(encryptedContent);
                lines = Arrays.asList(content.split("\\n"));
                
                // If the first line is the warning comment, skip it
                if (lines.size() > 0 && lines.get(0).startsWith("#")) {
                    lines = lines.subList(1, lines.size());
                }
            } catch (Exception e) {
                // If decryption fails, try to read as plain text (for backward compatibility)
                lines = java.nio.file.Files.readAllLines(path);
                if (lines.size() > 0 && lines.get(0).startsWith("#")) {
                    lines = lines.subList(1, lines.size());
                }
            }
            
            for (String line : lines) {
                String trimmed = line.trim().toLowerCase();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    words.add(trimmed);
                }
            }
            System.out.println("Loaded " + words.size() + " words from " + filename);
        } else {
            throw new java.io.FileNotFoundException("Profanity file not found: " + filename);
        }
        
        return words;
    }

    /**
     * Creates a profanity file with the given words
     * @param filename The path to create
     * @param words The words to write to the file
     */
    private void createProfanityFile(String filename, Set<String> words) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filename);
            
            // Create content with warning comment
            StringBuilder content = new StringBuilder();
            content.append(FILE_WARNING).append("\n");
            
            // Add each word on a new line
            for (String word : words) {
                content.append(word).append("\n");
            }
            
            // Encrypt the content
            byte[] encryptedContent = encryptContent(content.toString());
            
            // Write the encrypted content to the file
            java.nio.file.Files.write(path, encryptedContent);
            
            System.out.println("Created encrypted profanity file: " + filename);
        } catch (Exception e) {
            System.out.println("Failed to create profanity file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encrypts a string using AES encryption
     * @param content The string to encrypt
     * @return Encrypted bytes
     */
    private byte[] encryptContent(String content) throws Exception {
        try {
            javax.crypto.Cipher cipher = getEncryptionCipher(javax.crypto.Cipher.ENCRYPT_MODE);
            return cipher.doFinal(content.getBytes());
        } catch (Exception e) {
            System.out.println("Encryption error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Decrypts bytes to a string using AES decryption
     * @param encryptedContent The bytes to decrypt
     * @return Decrypted string
     */
    private String decryptContent(byte[] encryptedContent) throws Exception {
        try {
            javax.crypto.Cipher cipher = getEncryptionCipher(javax.crypto.Cipher.DECRYPT_MODE);
            byte[] decryptedBytes = cipher.doFinal(encryptedContent);
            return new String(decryptedBytes);
        } catch (Exception e) {
            System.out.println("Decryption error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates an encryption cipher for the given mode
     * @param mode The cipher mode (encrypt or decrypt)
     * @return The configured cipher
     */
    private javax.crypto.Cipher getEncryptionCipher(int mode) throws Exception {
        // Convert the key string to a fixed-length key
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(ENCRYPTION_KEY.getBytes());
        keyBytes = Arrays.copyOf(keyBytes, 16); // Use first 128 bits for AES
        
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
        cipher.init(mode, secretKey);
        
        return cipher;
    }

    /**
     * Adds a new profanity word to both memory and files
     * @param word The word to add
     * @param language The language ("en" for English, "tr" for Turkish)
     * @return True if added successfully, false otherwise
     */
    public boolean addProfanityWord(String word, String language) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        word = word.trim().toLowerCase();
        
        // Add to memory
        synchronized (profanityWords) {
            profanityWords.add(word);
        }
        
        // Add to appropriate file
        try {
            String file = "tr".equalsIgnoreCase(language) ? TURKISH_PROFANITY_FILE : ENGLISH_PROFANITY_FILE;
            java.nio.file.Path path = java.nio.file.Paths.get(file);
            
            if (!java.nio.file.Files.exists(path)) {
                createResourceDirectoriesIfNeeded();
                // Create new file with just the warning and the new word
                Set<String> initialWords = new HashSet<>();
                initialWords.add(word);
                createProfanityFile(file, initialWords);
                
                // Clear the profanity check cache
                clearProfanityCache();
                
                return true;
            }
            
            // Read existing file
            Set<String> existingWords = loadWordsFromFile(file);
            existingWords.add(word);
            
            // Write updated content
            createProfanityFile(file, existingWords);
            
            // Clear the profanity check cache
            clearProfanityCache();
            
            return true;
        } catch (Exception e) {
            System.out.println("Error adding word to profanity file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a profanity word from both memory and files
     * @param word The word to remove
     * @return True if removed successfully, false otherwise
     */
    public boolean removeProfanityWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        word = word.trim().toLowerCase();
        
        // Remove from memory
        synchronized (profanityWords) {
            profanityWords.remove(word);
        }
        
        // Remove from both files
        boolean success = true;
        
        try {
            updateProfanityFile(ENGLISH_PROFANITY_FILE, word);
        } catch (Exception e) {
            System.out.println("Error removing word from English profanity file: " + e.getMessage());
            success = false;
        }
        
        try {
            updateProfanityFile(TURKISH_PROFANITY_FILE, word);
        } catch (Exception e) {
            System.out.println("Error removing word from Turkish profanity file: " + e.getMessage());
            success = false;
        }
        
        // Clear the profanity check cache
        clearProfanityCache();
        
        return success;
    }

    /**
     * Updates a profanity file by removing a word
     * @param filename The file to update
     * @param wordToRemove The word to remove
     */
    private void updateProfanityFile(String filename, String wordToRemove) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(filename);
        
        if (java.nio.file.Files.exists(path)) {
            // Load existing words
            Set<String> words = loadWordsFromFile(filename);
            
            // Remove the word
            words.remove(wordToRemove);
            
            // Write the updated content back to the file
            createProfanityFile(filename, words);
        }
    }

    /**
     * @return Get all currently loaded profanity words
     */
    public Set<String> getAllProfanityWords() {
        synchronized (profanityWords) {
            return new HashSet<>(profanityWords);
        }
    }

    /**
     * Reload profanity words from files
     * @return The number of words loaded
     */
    public int reloadProfanityWords() {
        synchronized (profanityWords) {
            profanityWords = loadProfanityWords();
            
            // Clear the profanity check cache
            clearProfanityCache();
            
            return profanityWords.size();
        }
    }

    private Set<String> getDefaultEnglishProfanityWords() {
        // This is a limited sample list for demonstration purposes.
        return new HashSet<>(Arrays.asList(
            "fuck", "shit", "ass", "bitch", "damn", "cunt", "dick",
            "bastard", "asshole", "piss", "whore", "slut"
        ));
    }

    private Set<String> getDefaultTurkishProfanityWords() {
        // This is a limited sample list for demonstration purposes.
        return new HashSet<>(Arrays.asList(
            "siktir", "amına", "göt", "orospu", "piç", "yarak", "amcık",
            "ibne", "ananı", "sikeyim", "pezevenk", "gavat"
        ));
    }

    /**
     * Clears the profanity check cache
     */
    private void clearProfanityCache() {
        try {
            if (cacheManager != null) {
                cacheManager.getCache("profanityCheck").clear();
                System.out.println("Profanity check cache cleared");
            }
        } catch (Exception e) {
            System.out.println("Error clearing cache: " + e.getMessage());
        }
    }
} 