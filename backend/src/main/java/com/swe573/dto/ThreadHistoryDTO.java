package com.swe573.dto;

import com.swe573.models.ThreadHistoryEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadHistoryDTO {

    private Long id;
    private Long threadId;
    private String threadTitle;
    private UserDTO user;
    private ThreadHistoryEntry.ActionType actionType;
    private ThreadHistoryEntry.EntityType entityType;
    private Long entityId;
    private String beforeState;
    private String afterState;
    private String description;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDTO {
        private Long id;
        private String username;
    }
} 