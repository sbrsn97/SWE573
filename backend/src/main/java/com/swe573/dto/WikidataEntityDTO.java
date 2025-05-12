package com.swe573.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Data
@Schema(description = "Data transfer object for Wikidata entity information")
public class WikidataEntityDTO {
    @Schema(description = "Wikidata entity ID (e.g., Q123456)")
    private String id;
    
    @Schema(description = "Display label of the entity")
    private String label;
    
    @Schema(description = "Description of the entity")
    private String description;
    
    @Schema(description = "Map of property labels to their values")
    private Map<String, String> properties;
    
    @Schema(description = "Map of property labels to their descriptions")
    private Map<String, String> propertyDescriptions;
    
    @Schema(description = "URL to the entity on Wikidata")
    private String url;
    
    @Schema(description = "Type of the entity (e.g., topic, person, organization)")
    private String type;
} 