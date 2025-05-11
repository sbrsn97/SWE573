package com.swe573.services;

import com.swe573.dto.ThreadHistoryDTO;
import com.swe573.models.Thread;
import com.swe573.models.ThreadHistoryEntry;
import com.swe573.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ThreadHistoryService {
    
    // Log thread-related actions
    ThreadHistoryEntry logThreadCreation(Thread thread, User user);
    ThreadHistoryEntry logThreadUpdate(Thread thread, User user, String beforeState, String afterState);
    ThreadHistoryEntry logThreadDeletion(Thread thread, User user, String beforeState);
    
    // Log thread interactions
    ThreadHistoryEntry logThreadFollow(Thread thread, User user);
    ThreadHistoryEntry logThreadUnfollow(Thread thread, User user);
    ThreadHistoryEntry logThreadVote(Thread thread, User user, boolean isUpvote);
    ThreadHistoryEntry logThreadRemoveVote(Thread thread, User user);
    
    // Log node operations
    ThreadHistoryEntry logNodeCreation(Thread thread, User user, Long nodeId, String nodeDetails);
    ThreadHistoryEntry logNodeUpdate(Thread thread, User user, Long nodeId, String beforeState, String afterState);
    ThreadHistoryEntry logNodeDeletion(Thread thread, User user, Long nodeId, String beforeState);
    
    // Log edge operations
    ThreadHistoryEntry logEdgeCreation(Thread thread, User user, Long edgeId, String edgeDetails);
    ThreadHistoryEntry logEdgeUpdate(Thread thread, User user, Long edgeId, String beforeState, String afterState);
    ThreadHistoryEntry logEdgeDeletion(Thread thread, User user, Long edgeId, String beforeState);
    
    // Log tag operations
    ThreadHistoryEntry logTagAddition(Thread thread, User user, Long tagId, String tagLabel);
    ThreadHistoryEntry logTagRemoval(Thread thread, User user, Long tagId, String tagLabel);
    
    // Log comment operations
    ThreadHistoryEntry logCommentCreation(Thread thread, User user, Long commentId, String commentContent);
    ThreadHistoryEntry logCommentUpdate(Thread thread, User user, Long commentId, String beforeContent, String afterContent);
    ThreadHistoryEntry logCommentDeletion(Thread thread, User user, Long commentId, String commentContent);
    ThreadHistoryEntry logCommentVote(Thread thread, User user, Long commentId, boolean isUpvote);
    ThreadHistoryEntry logCommentRemoveVote(Thread thread, User user, Long commentId);
    
    // Retrieve history
    List<ThreadHistoryDTO> getThreadHistory(Long threadId);
    Page<ThreadHistoryDTO> getThreadHistoryPaginated(Long threadId, Pageable pageable);
    List<ThreadHistoryDTO> getThreadEntityHistory(Long threadId, ThreadHistoryEntry.EntityType entityType, Long entityId);
    List<ThreadHistoryDTO> getUserActionHistory(Long userId);
} 