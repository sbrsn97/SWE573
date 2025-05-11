package com.swe573.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Utility to convert plain text profanity files to encrypted format.
 * Run this class as a standalone Java application to convert files.
 */
public class ProfanityFileConverter {
    
    private static final String ENCRYPTION_KEY = "SWE573ProfanityFilterSecretKey123";
    private static final String FILE_WARNING = "#this file is full of disgusting words. decrypt at your own risk";
    private static final String ENGLISH_PROFANITY_FILE = "profanity/english_profanity.txt";
    private static final String TURKISH_PROFANITY_FILE = "profanity/turkish_profanity.txt";
    
    public static void main(String[] args) {
        try {
            // Create directory if it doesn't exist
            Path dirPath = Paths.get("profanity");
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Created profanity directory");
            }
            
            // Convert English file if it exists
            Path englishPath = Paths.get(ENGLISH_PROFANITY_FILE);
            if (Files.exists(englishPath)) {
                convertFile(englishPath, "English");
            } else {
                System.out.println("English profanity file not found, skipping conversion");
            }
            
            // Convert Turkish file if it exists
            Path turkishPath = Paths.get(TURKISH_PROFANITY_FILE);
            if (Files.exists(turkishPath)) {
                convertFile(turkishPath, "Turkish");
            } else {
                System.out.println("Turkish profanity file not found, skipping conversion");
            }
            
            System.out.println("Conversion complete");
        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void convertFile(Path filePath, String language) throws Exception {
        System.out.println("Converting " + language + " profanity file");
        
        // Read the plaintext file
        List<String> lines = Files.readAllLines(filePath);
        
        // Skip comment lines
        List<String> wordLines = lines.stream()
            .filter(line -> !line.trim().startsWith("#") && !line.trim().isEmpty())
            .toList();
        
        // Create content with warning
        StringBuilder content = new StringBuilder();
        content.append(FILE_WARNING).append("\n");
        
        // Add each word
        for (String word : wordLines) {
            content.append(word.trim().toLowerCase()).append("\n");
        }
        
        // Encrypt the content
        byte[] encryptedContent = encryptContent(content.toString());
        
        // Backup the original file
        Path backupPath = Paths.get(filePath.toString() + ".bak");
        Files.copy(filePath, backupPath);
        System.out.println("Original file backed up to " + backupPath);
        
        // Write the encrypted content
        Files.write(filePath, encryptedContent);
        System.out.println("Encrypted file written to " + filePath);
        
        // Verify we can decrypt it
        byte[] fileBytes = Files.readAllBytes(filePath);
        String decrypted = decryptContent(fileBytes);
        int wordCount = decrypted.split("\n").length - 1; // -1 for the warning line
        System.out.println("Verified decryption: found " + wordCount + " words");
    }
    
    /**
     * Encrypts a string using AES encryption
     */
    private static byte[] encryptContent(String content) throws Exception {
        javax.crypto.Cipher cipher = getEncryptionCipher(javax.crypto.Cipher.ENCRYPT_MODE);
        return cipher.doFinal(content.getBytes());
    }
    
    /**
     * Decrypts bytes to a string using AES decryption
     */
    private static String decryptContent(byte[] encryptedContent) throws Exception {
        javax.crypto.Cipher cipher = getEncryptionCipher(javax.crypto.Cipher.DECRYPT_MODE);
        byte[] decryptedBytes = cipher.doFinal(encryptedContent);
        return new String(decryptedBytes);
    }
    
    /**
     * Creates an encryption cipher for the given mode
     */
    private static javax.crypto.Cipher getEncryptionCipher(int mode) throws Exception {
        // Convert the key string to a fixed-length key
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(ENCRYPTION_KEY.getBytes());
        keyBytes = Arrays.copyOf(keyBytes, 16); // Use first 128 bits for AES
        
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
        cipher.init(mode, secretKey);
        
        return cipher;
    }
} 