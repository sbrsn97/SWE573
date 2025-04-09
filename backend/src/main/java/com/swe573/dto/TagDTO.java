package com.swe573.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Data transfer object for tag information")
public class TagDTO {
    @Schema(description = "Unique identifier of the tag")
    private Long id;
    
    @Schema(description = "Label of the tag")
    private String label;
    
    @Schema(description = "Description of the tag")
    private String description;
    
    @Schema(description = "Color code for the tag")
    private String colorCodeString;
    
    @Schema(description = "Wikidata entity ID if available")
    private String wikidataEntityId;
} 