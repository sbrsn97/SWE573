package com.swe573.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
@Schema(description = "Data transfer object for creating a new comment")
public class CreateCommentDTO {
    @NotBlank(message = "Comment content is required")
    @Schema(description = "Content of the comment", example = "This is a great point!")
    private String content;
    
    @NotNull(message = "Thread ID is required")
    @Schema(description = "ID of the thread this comment belongs to")
    private Long threadId;
    
    @Schema(description = "IDs of nodes referenced in this comment")
    private Set<Long> referencedNodeIds;
} 