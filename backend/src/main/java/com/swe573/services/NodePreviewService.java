package com.swe573.services;

import com.swe573.dto.NodePreviewDTO;

public interface NodePreviewService {
    /**
     * Generates a preview for a node based on label and description text
     * @param label The node label
     * @param description The node description
     * @return A NodePreviewDTO containing keywords and Wikidata suggestions
     */
    NodePreviewDTO generatePreview(String label, String description);
} 