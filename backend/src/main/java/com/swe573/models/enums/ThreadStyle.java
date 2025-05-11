package com.swe573.models.enums;

/**
 * Enumeration for different thread styles that control access and interaction permissions.
 */
public enum ThreadStyle {
    /**
     * Public thread - Anyone can view and interact with it.
     */
    PUBLIC,
    
    /**
     * Private thread - Only the creator and explicitly invited users can view and interact with it.
     */
    PRIVATE,
    
    /**
     * Follow-to-interact thread - Anyone can view, but only followers can comment or vote.
     */
    FOLLOW_TO_INTERACT
} 