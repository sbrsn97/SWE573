package com.swe573.dto;

import lombok.Data;

@Data
public class BatchEdgeDTO {
    private Long sourceNodeId;
    private Long targetNodeId;
    private String label;
    private String type;
    private Integer weight;
    private String color;
    private String wikidataPropertyId;
} 