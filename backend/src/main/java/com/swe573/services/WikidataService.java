package com.swe573.services;

import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.dto.PaginatedResponse;

public interface WikidataService {
    // Topic methods
    WikidataEntityDTO getTopicDetails(String id);
    PaginatedResponse<WikidataEntityDTO> searchTopics(String query, int page, int size);

    // Entity operations
    PaginatedResponse<WikidataEntityDTO> getAllEntities(int page, int size);
    PaginatedResponse<WikidataEntityDTO> searchEntities(String query, int page, int size);
    WikidataEntityDTO getEntityById(String id);
    
    // Property operations
    PaginatedResponse<WikidataPropertyDTO> getAllProperties(int page, int size);
    PaginatedResponse<WikidataPropertyDTO> searchProperties(String query, int page, int size);
    WikidataPropertyDTO getPropertyById(String id);
} 