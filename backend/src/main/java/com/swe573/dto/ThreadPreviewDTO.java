package com.swe573.dto;

import lombok.Data;
import java.util.List;

@Data
public class ThreadPreviewDTO {
    private List<String> keywords;
    private List<WikidataEntityDTO> suggestedEntities;
    private List<WikidataPropertyDTO> suggestedProperties;
} 