package com.swe573.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodePreviewDTO {
    private List<String> keywords;
    private List<WikidataEntityDTO> suggestedEntities;
    private List<WikidataPropertyDTO> suggestedProperties;
} 