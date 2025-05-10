package com.swe573.dto;

import lombok.Data;

@Data
public class EdgeUpdateDTO {
    private String label;
    private String type;
    private Integer weight;
    private String color;
} 