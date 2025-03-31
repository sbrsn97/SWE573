package com.swe573.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for Wikidata property information")
public class WikidataPropertyDTO {
    @Schema(description = "Wikidata property ID (e.g., P123)")
    private String id;
    
    @Schema(description = "Display label of the property")
    private String label;
    
    @Schema(description = "Description of the property")
    private String description;
    
    @Schema(description = "URL to the property on Wikidata")
    private String url;
    
    @Schema(description = "Type of values this property can have")
    private String valueType;
    
    @Schema(description = "Example value for the property")
    private String exampleValue;
} 