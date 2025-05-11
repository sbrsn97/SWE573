package com.swe573.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.swe573.models.enums.ThreadStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for thread information")
public class ThreadDTO {
    @Schema(description = "Unique identifier of the thread")
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Schema(description = "Title of the thread", example = "How to use Spring Boot?")
    private String title;
    
    @NotBlank(message = "Description is required")
    @Schema(description = "Description of the thread", example = "I need help understanding Spring Boot basics")
    private String description;
    
    @NotNull(message = "Author ID is required")
    @Schema(description = "ID of the thread author")
    private Long authorId;
    
    @Schema(description = "Style of the thread (PUBLIC, PRIVATE, FOLLOW_TO_INTERACT)", example = "PUBLIC")
    private ThreadStyle threadStyle = ThreadStyle.PUBLIC;
    
    @Schema(description = "Set of tags associated with the thread")
    private Set<TagDTO> tags;
    
    @Schema(description = "Set of user IDs following this thread")
    private Set<Long> followerIds;
    
    @Schema(description = "Number of upvotes on the thread")
    private int upvoteCount;
    
    @Schema(description = "Number of downvotes on the thread")
    private int downvoteCount;
    
    @Schema(description = "Timestamp when the thread was created")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the thread was last updated")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Whether the thread is active or soft-deleted")
    private boolean active = true;
    
    @Schema(description = "Role of the user who deactivated the thread (USER or ADMIN)")
    private String deactivatedByRole;
} 