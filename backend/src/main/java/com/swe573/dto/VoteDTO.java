package com.swe573.dto;

import com.swe573.models.enums.VoteType;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Data
@Schema(description = "Data transfer object for vote information")
public class VoteDTO {
    @Schema(description = "Unique identifier of the vote")
    private Long id;
    
    @Schema(description = "Type of vote (UPVOTE or DOWNVOTE)", example = "UPVOTE")
    private VoteType type;
    
    @Schema(description = "ID of the user who cast the vote")
    private Long userId;
    
    @Schema(description = "ID of the thread that was voted on")
    private Long threadId;
    
    @Schema(description = "ID of the comment that was voted on")
    private Long commentId;
    
    @Schema(description = "Timestamp when the vote was cast")
    private LocalDateTime createdAt;
} 