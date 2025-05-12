package com.swe573.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDetailsDTO {
    private Long id;
    private Long nodeId;
    private String label;
    private String description;
    private String wikidataEntityId;
} 