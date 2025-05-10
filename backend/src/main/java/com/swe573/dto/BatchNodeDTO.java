package com.swe573.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class BatchNodeDTO {
    private String label;
    
    @JsonProperty("xPosition")
    private Double xPosition;
    
    @JsonProperty("yPosition")
    private Double yPosition;
    
    private String color;
    private String shape;
    private Integer size;
    private String wikidataEntityId;
    private String description;
} 