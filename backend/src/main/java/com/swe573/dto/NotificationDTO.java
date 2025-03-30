package com.swe573.dto;

import com.swe573.models.enums.NotificationType;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Data
@Schema(description = "Data transfer object for notification information")
public class NotificationDTO {
    @Schema(description = "Unique identifier of the notification")
    private Long id;
    
    @Schema(description = "Type of notification", example = "THREAD_VOTE")
    private NotificationType type;
    
    @Schema(description = "ID of the user who received the notification")
    private Long userId;
    
    @Schema(description = "ID of the thread associated with the notification")
    private Long threadId;
    
    @Schema(description = "ID of the comment associated with the notification")
    private Long commentId;
    
    @Schema(description = "Timestamp when the notification was created")
    private LocalDateTime createdAt;
    
    @Schema(description = "Whether the notification has been read")
    private boolean read;
} 