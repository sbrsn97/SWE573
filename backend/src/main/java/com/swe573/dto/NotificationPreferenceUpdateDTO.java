package com.swe573.dto;

import com.swe573.models.enums.NotificationType;
import lombok.Data;

@Data
public class NotificationPreferenceUpdateDTO {
    private NotificationType type;
    private boolean enabled;
    private Long referenceId;
    private String referenceType;
    private boolean isGlobal;
} 