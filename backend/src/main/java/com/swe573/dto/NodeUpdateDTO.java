package com.swe573.dto;

import lombok.Data;

@Data
public class NodeUpdateDTO {
    private String label;
    private Double xPosition;
    private Double yPosition;
    private String color;
    private String shape;
    private Integer size;
    private String wikidataEntityId;
    private String description;
} 