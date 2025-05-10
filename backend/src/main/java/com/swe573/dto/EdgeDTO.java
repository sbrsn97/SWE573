package com.swe573.dto;

import com.swe573.models.Edge;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeDTO {
    private Long id;
    private Long sourceNodeId;
    private Long targetNodeId;
    private String label;
    private String type;
    private Integer weight;
    private String color;
    private String wikidataPropertyId;
    private Long threadId;
    
    public static EdgeDTO fromEntity(Edge edge) {
        if (edge == null) {
            return null;
        }
        
        EdgeDTO dto = new EdgeDTO();
        dto.setId(edge.getId());
        dto.setSourceNodeId(edge.getSourceNode() != null ? edge.getSourceNode().getId() : null);
        dto.setTargetNodeId(edge.getTargetNode() != null ? edge.getTargetNode().getId() : null);
        dto.setLabel(edge.getLabel());
        dto.setType(edge.getType());
        dto.setWeight(edge.getWeight());
        dto.setColor(edge.getColor());
        dto.setWikidataPropertyId(edge.getWikidataPropertyId());
        dto.setThreadId(edge.getThread() != null ? edge.getThread().getId() : null);
        
        return dto;
    }
} 