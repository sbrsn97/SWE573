package com.swe573.models.enums;

public enum NotificationType {
    // Vote related
    THREAD_UPVOTE,
    THREAD_DOWNVOTE,
    COMMENT_UPVOTE,
    COMMENT_DOWNVOTE,
    NEW_COMMENT,
    VOTE_MILESTONE,
    VOTE_REMOVED,
    
    // Thread related
    NEW_THREAD_FOLLOWED,
    THREAD_UPDATED,
    THREAD_DELETED,
    
    // Comment related
    NEW_COMMENT_ON_THREAD,
    NEW_COMMENT_ON_FOLLOWED_THREAD,
    COMMENT_REPLY,
    COMMENT_DELETED,
    
    // User related
    USER_FOLLOWED,
    USER_UNFOLLOWED,
    USER_MENTIONED,
    
    // System related
    SYSTEM_NOTIFICATION
} 