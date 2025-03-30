package com.swe573.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for tag information")
public class TagDTO {
    @Schema(description = "Unique identifier of the tag")
    private Long id;
    
    @Schema(description = "Wikidata entity ID associated with the tag")
    private String wikidataEntityId;
    
    @NotBlank(message = "Label is required")
    @Schema(description = "Display label of the tag", example = "Java")
    private String label;
    
    @Schema(description = "Description of the tag", example = "Java programming language")
    private String description;
    
    @Schema(description = "Color code for the tag in hex format", example = "#FF0000")
    private String colorCodeString;
} 