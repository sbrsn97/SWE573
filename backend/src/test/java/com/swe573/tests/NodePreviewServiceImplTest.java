package com.swe573.tests;

import com.swe573.dto.NodePreviewDTO;
import com.swe573.dto.PaginatedResponse;
import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.services.NlpService;
import com.swe573.services.WikidataService;
import com.swe573.services.impl.NodePreviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class NodePreviewServiceImplTest {

    @Mock
    private WikidataService wikidataService;

    @Mock
    private NlpService nlpService;

    @InjectMocks
    private NodePreviewServiceImpl nodePreviewService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGeneratePreviewWithEmptyInputs() {
        NodePreviewDTO result = nodePreviewService.generatePreview("", "");
        
        assertNotNull(result);
        assertTrue(result.getKeywords().isEmpty());
        assertTrue(result.getSuggestedEntities().isEmpty());
        assertTrue(result.getSuggestedProperties().isEmpty());
    }

    @Test
    public void testGeneratePreviewWithValidInputs() {
        // Setup NLP service mock
        List<String> keywords = Arrays.asList("software", "engineering", "project");
        when(nlpService.extractKeywords(anyString())).thenReturn(keywords);
        
        // Setup Wikidata service mocks
        List<WikidataEntityDTO> entities = Arrays.asList(
            createEntity("Q7397", "software"),
            createEntity("Q11635", "engineering")
        );
        List<WikidataPropertyDTO> properties = Arrays.asList(
            createProperty("P31", "instance of"),
            createProperty("P361", "part of")
        );
        
        PaginatedResponse<WikidataEntityDTO> entityResponse = new PaginatedResponse<>();
        entityResponse.setItems(entities);
        
        PaginatedResponse<WikidataPropertyDTO> propertyResponse = new PaginatedResponse<>();
        propertyResponse.setItems(properties);
        
        when(wikidataService.searchEntities(anyString(), anyInt(), anyInt())).thenReturn(entityResponse);
        when(wikidataService.searchProperties(anyString(), anyInt(), anyInt())).thenReturn(propertyResponse);
        
        // Execute
        NodePreviewDTO result = nodePreviewService.generatePreview("Software Engineering", "A course project");
        
        // Verify
        assertNotNull(result);
        assertEquals(3, result.getKeywords().size());
        assertTrue(result.getKeywords().contains("software"));
        
        assertNotNull(result.getSuggestedEntities());
        assertEquals(2, result.getSuggestedEntities().size());
        assertEquals("Q7397", result.getSuggestedEntities().get(0).getId());
        
        assertNotNull(result.getSuggestedProperties());
        assertEquals(2, result.getSuggestedProperties().size());
        assertEquals("P31", result.getSuggestedProperties().get(0).getId());
    }
    
    @Test
    public void testGeneratePreviewWithExceptions() {
        // Setup NLP service mock
        List<String> keywords = Arrays.asList("software", "engineering");
        when(nlpService.extractKeywords(anyString())).thenReturn(keywords);
        
        // Setup Wikidata service to throw exception
        when(wikidataService.searchEntities(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("API error"));
        when(wikidataService.searchProperties(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("API error"));
        
        // Execute
        NodePreviewDTO result = nodePreviewService.generatePreview("Software", "Engineering");
        
        // Verify - should still return empty lists but not fail
        assertNotNull(result);
        assertEquals(2, result.getKeywords().size());
        assertTrue(result.getSuggestedEntities().isEmpty());
        assertTrue(result.getSuggestedProperties().isEmpty());
    }
    
    private WikidataEntityDTO createEntity(String id, String label) {
        WikidataEntityDTO entity = new WikidataEntityDTO();
        entity.setId(id);
        entity.setLabel(label);
        entity.setDescription("A test entity");
        return entity;
    }
    
    private WikidataPropertyDTO createProperty(String id, String label) {
        WikidataPropertyDTO property = new WikidataPropertyDTO();
        property.setId(id);
        property.setLabel(label);
        property.setDescription("A test property");
        return property;
    }
} 