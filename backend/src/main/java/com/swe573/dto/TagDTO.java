package com.swe573.dto;

public class TagDTO {
    private Long id;
    private String wikidataEntityId;
    private String label;
    private String description;
    private String colorCodeString;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWikidataEntityId() {
        return wikidataEntityId;
    }

    public void setWikidataEntityId(String wikidataEntityId) {
        this.wikidataEntityId = wikidataEntityId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColorCodeString() {
        return colorCodeString;
    }

    public void setColorCodeString(String colorCodeString) {
        this.colorCodeString = colorCodeString;
    }
} 