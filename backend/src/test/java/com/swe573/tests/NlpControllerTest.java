package com.swe573.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe573.controllers.NlpController;
import com.swe573.services.NlpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NlpController.class)
public class NlpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NlpService nlpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCheckProfanity_Clean() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("text", "This is a clean text");

        when(nlpService.containsProfanity(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/nlp/profanity/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testCheckProfanity_Profane() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("text", "This contains bad words");

        when(nlpService.containsProfanity(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/nlp/profanity/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testCheckProfanity_EmptyText() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("text", "");

        mockMvc.perform(post("/api/nlp/profanity/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    public void testGetProfanityWords() throws Exception {
        Set<String> mockWords = new HashSet<>(Arrays.asList("word1", "word2", "word3"));
        when(nlpService.getAllProfanityWords()).thenReturn(mockWords);

        mockMvc.perform(get("/api/nlp/profanity/words")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }
    
    @Test
    public void testAddProfanityWord_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("word", "badword");
        request.put("language", "en");

        when(nlpService.addProfanityWord(eq("badword"), eq("en"))).thenReturn(true);

        mockMvc.perform(post("/api/nlp/profanity/words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
    
    @Test
    public void testAddProfanityWord_EmptyWord() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("word", "");
        request.put("language", "en");

        mockMvc.perform(post("/api/nlp/profanity/words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    public void testAddProfanityWord_DefaultLanguage() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("word", "badword");
        // No language specified, should default to "en"

        when(nlpService.addProfanityWord(eq("badword"), eq("en"))).thenReturn(true);

        mockMvc.perform(post("/api/nlp/profanity/words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
    
    @Test
    public void testRemoveProfanityWord_Success() throws Exception {
        when(nlpService.removeProfanityWord(eq("badword"))).thenReturn(true);

        mockMvc.perform(delete("/api/nlp/profanity/words/badword")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
    
    @Test
    public void testReloadProfanityWords() throws Exception {
        when(nlpService.reloadProfanityWords()).thenReturn(42);

        mockMvc.perform(post("/api/nlp/profanity/reload")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42));
    }

    @Test
    public void testExtractKeywords() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("text", "Java Spring Boot application");

        when(nlpService.extractKeywords(anyString())).thenReturn(Arrays.asList("java", "spring", "boot", "application"));

        mockMvc.perform(post("/api/nlp/keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    public void testAnalyzeText() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("text", "Microsoft develops Windows operating system");

        when(nlpService.extractKeywords(anyString())).thenReturn(Arrays.asList("microsoft", "develops", "windows", "operating", "system"));
        when(nlpService.extractNamedEntities(anyString())).thenReturn(Arrays.asList("Microsoft", "Windows"));
        when(nlpService.analyzeTopics(anyString())).thenReturn(Arrays.asList("technology", "software"));
        when(nlpService.containsProfanity(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/nlp/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.entities").isArray())
                .andExpect(jsonPath("$.data.topics").isArray())
                .andExpect(jsonPath("$.data.containsProfanity").value(false));
    }
} 