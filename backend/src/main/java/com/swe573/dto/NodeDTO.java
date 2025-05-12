package com.swe573.dto;

import com.swe573.models.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {
    private Long id;
    private String label;
    private Long threadId;
    
    @JsonProperty("xPosition")
    private Double xPosition;
    
    @JsonProperty("yPosition")
    private Double yPosition;
    
    private String color;
    private String shape;
    private Integer size;
    private Integer version;
    private Long detailsId;
    private NodeDetailsDTO details;
    
    public static NodeDTO fromEntity(Node node) {
        if (node == null) {
            return null;
        }
        
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setLabel(node.getLabel());
        dto.setThreadId(node.getThread() != null ? node.getThread().getId() : null);
        dto.setXPosition(node.getXPosition());
        dto.setYPosition(node.getYPosition());
        dto.setColor(node.getColor());
        dto.setShape(node.getShape());
        dto.setSize(node.getSize());
        dto.setVersion(node.getVersion());
        
        if (node.getDetails() != null) {
            dto.setDetailsId(node.getDetails().getId());
            
            NodeDetailsDTO detailsDTO = new NodeDetailsDTO();
            detailsDTO.setId(node.getDetails().getId());
            detailsDTO.setNodeId(node.getId());
            detailsDTO.setLabel(node.getDetails().getLabel());
            detailsDTO.setDescription(node.getDetails().getDescription());
            detailsDTO.setWikidataEntityId(node.getDetails().getWikidataEntityId());
            
            dto.setDetails(detailsDTO);
        } else {
            dto.setDetailsId(null);
            dto.setDetails(null);
        }
        
        return dto;
    }
} 