package com.swe573.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Schema(description = "Data transfer object for comment information")
public class CommentDTO {
    @Schema(description = "Unique identifier of the comment")
    private Long id;
    
    @Schema(description = "Content of the comment")
    private String content;
    
    @Schema(description = "ID of the user who authored the comment")
    private Long authorId;
    
    @Schema(description = "Username of the author")
    private String authorUsername;
    
    @Schema(description = "ID of the thread this comment belongs to")
    private Long threadId;
    
    @Schema(description = "ID of the parent comment, null for top-level comments")
    private Long parentId;
    
    @Schema(description = "IDs of nodes referenced in this comment")
    private Set<Long> referencedNodeIds;
    
    @Schema(description = "Number of upvotes")
    private int upvoteCount;
    
    @Schema(description = "Number of downvotes")
    private int downvoteCount;
    
    @Schema(description = "Whether the comment is active")
    private boolean active;
    
    @Schema(description = "Timestamp when the comment was created")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the comment was last updated")
    private LocalDateTime updatedAt;
} 