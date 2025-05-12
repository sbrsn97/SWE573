# Backend Fix for Notification Preferences Infinite Recursion

There appears to be an infinite recursion issue in the backend when serializing `NotificationPreference` objects. This is causing extremely large JSON responses that are failing to parse in the frontend.

## Problem

The `NotificationPreference` entity has a bidirectional relationship with `User` that creates a circular reference during JSON serialization.

## Solution

### Option 1: Create a Dedicated DTO for NotificationPreference

Add a new DTO class to properly serialize notification preferences without the circular reference:

```java
// In package com.swe573.dto
@Data
public class NotificationPreferenceDTO {
    private Long id;
    private NotificationType type;
    private boolean enabled;
    private Long referenceId;
    private String referenceType;
    private boolean isGlobal;
    private Long userId; // Instead of the full User object
}
```

Then modify the NotificationController to use this DTO:

```java
// In NotificationController.java
@GetMapping("/user/{userId}/preferences")
public ResponseEntity<ApiResponse<List<NotificationPreferenceDTO>>> getUserPreferences(
        @PathVariable Long userId) {
    List<NotificationPreference> preferences = notificationService.getUserPreferences(userId);
    List<NotificationPreferenceDTO> dtoList = preferences.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(dtoList));
}

private NotificationPreferenceDTO convertToDTO(NotificationPreference preference) {
    NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
    dto.setId(preference.getId());
    dto.setType(preference.getType());
    dto.setEnabled(preference.isEnabled());
    dto.setReferenceId(preference.getReferenceId());
    dto.setReferenceType(preference.getReferenceType());
    dto.setIsGlobal(preference.isGlobal());
    dto.setUserId(preference.getUser().getId());
    return dto;
}
```

### Option 2: Use Jackson's @JsonIgnore Annotation

Add `@JsonIgnore` to the User field in NotificationPreference:

```java
// In NotificationPreference.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@JsonIgnore
private User user;
```

### Option 3: Configure JSON Serialization Globally

In your Spring configuration, add a Jackson Module to handle bidirectional references:

```java
@Configuration
public class JacksonConfig {
    @Bean
    public Module springDataJacksonModule() {
        return new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public BeanSerializerBuilder updateBuilder(
                            SerializationConfig config, BeanDescription beanDesc, 
                            BeanSerializerBuilder builder) {
                        if (beanDesc.getBeanClass() == NotificationPreference.class) {
                            builder.setProperties(
                                builder.getProperties().stream()
                                .filter(p -> !p.getName().equals("user"))
                                .collect(Collectors.toList())
                            );
                        }
                        return builder;
                    }
                });
            }
        };
    }
}
```

## Recommendation

Option 1 (creating a dedicated DTO) is the recommended approach as it provides the cleanest separation of concerns and most explicit control over what data is sent to the client.

## Implementation Steps

1. Create the NotificationPreferenceDTO class
2. Modify NotificationController to use this DTO
3. Test the API to ensure it returns properly formatted data without circular references
4. Verify there are no unterminated string errors in the frontend 