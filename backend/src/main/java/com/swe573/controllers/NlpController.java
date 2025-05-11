package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.services.NlpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@RestController
@RequestMapping("/api/nlp")
@Tag(name = "NLP", description = "Natural Language Processing APIs")
@CrossOrigin(origins = "*")
public class NlpController {

    @Autowired
    private NlpService nlpService;

    @Operation(summary = "Check text for profanity", description = "Determines if input text contains profane language")
    @PostMapping("/profanity/check")
    public ResponseEntity<ApiResponse<Boolean>> checkProfanity(
            @Parameter(description = "Text content to analyze", required = true) 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Text parameter is required")
            );
        }
        
        boolean containsProfanity = nlpService.containsProfanity(text);
        return ResponseEntity.ok(
            ApiResponse.success(containsProfanity)
        );
    }
    
    @Operation(summary = "Get all profanity words", description = "Returns all words currently in the profanity filter")
    @GetMapping("/profanity/words")
    public ResponseEntity<ApiResponse<Set<String>>> getProfanityWords() {
        Set<String> words = nlpService.getAllProfanityWords();
        return ResponseEntity.ok(
            ApiResponse.success(words)
        );
    }
    
    @Operation(summary = "Add a profanity word", description = "Adds a new word to the profanity filter")
    @PostMapping("/profanity/words")
    public ResponseEntity<ApiResponse<Boolean>> addProfanityWord(
            @Parameter(description = "Word data", required = true) 
            @RequestBody Map<String, String> request) {
        
        String word = request.get("word");
        String language = request.get("language");
        
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Word parameter is required")
            );
        }
        
        if (language == null || language.trim().isEmpty()) {
            language = "en"; // Default to English
        }
        
        boolean success = nlpService.addProfanityWord(word, language);
        return ResponseEntity.ok(
            ApiResponse.success(success)
        );
    }
    
    @Operation(summary = "Remove a profanity word", description = "Removes a word from the profanity filter")
    @DeleteMapping("/profanity/words/{word}")
    public ResponseEntity<ApiResponse<Boolean>> removeProfanityWord(
            @Parameter(description = "Word to remove", required = true) 
            @PathVariable String word) {
        
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Word parameter is required")
            );
        }
        
        boolean success = nlpService.removeProfanityWord(word);
        return ResponseEntity.ok(
            ApiResponse.success(success)
        );
    }
    
    @Operation(summary = "Reload profanity words", description = "Reloads profanity words from source files")
    @PostMapping("/profanity/reload")
    public ResponseEntity<ApiResponse<Integer>> reloadProfanityWords() {
        int count = nlpService.reloadProfanityWords();
        return ResponseEntity.ok(
            ApiResponse.success(count)
        );
    }

    @Operation(summary = "Extract keywords", description = "Extracts key terms from input text")
    @PostMapping("/keywords")
    public ResponseEntity<ApiResponse<List<String>>> extractKeywords(
            @Parameter(description = "Text content to analyze", required = true) 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Text parameter is required")
            );
        }
        
        List<String> keywords = nlpService.extractKeywords(text);
        return ResponseEntity.ok(
            ApiResponse.success(keywords)
        );
    }

    @Operation(summary = "Extract named entities", description = "Identifies named entities like people, organizations, locations in text")
    @PostMapping("/entities")
    public ResponseEntity<ApiResponse<List<String>>> extractEntities(
            @Parameter(description = "Text content to analyze", required = true) 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Text parameter is required")
            );
        }
        
        List<String> entities = nlpService.extractNamedEntities(text);
        return ResponseEntity.ok(
            ApiResponse.success(entities)
        );
    }

    @Operation(summary = "Analyze topics", description = "Identifies topics and themes in the text")
    @PostMapping("/topics")
    public ResponseEntity<ApiResponse<List<String>>> analyzeTopics(
            @Parameter(description = "Text content to analyze", required = true) 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Text parameter is required")
            );
        }
        
        List<String> topics = nlpService.analyzeTopics(text);
        return ResponseEntity.ok(
            ApiResponse.success(topics)
        );
    }

    @Operation(summary = "Analyze text", description = "Performs comprehensive analysis including keywords, entities, topics, and profanity check")
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeText(
            @Parameter(description = "Text content to analyze", required = true) 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Text parameter is required")
            );
        }
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("keywords", nlpService.extractKeywords(text));
        analysis.put("entities", nlpService.extractNamedEntities(text));
        analysis.put("topics", nlpService.analyzeTopics(text));
        analysis.put("containsProfanity", nlpService.containsProfanity(text));
        
        return ResponseEntity.ok(
            ApiResponse.success("Text analysis complete", analysis)
        );
    }
} 