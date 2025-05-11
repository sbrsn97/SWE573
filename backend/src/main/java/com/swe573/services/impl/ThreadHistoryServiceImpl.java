package com.swe573.services.impl;

import com.swe573.dto.ThreadHistoryDTO;
import com.swe573.models.Thread;
import com.swe573.models.ThreadHistoryEntry;
import com.swe573.models.User;
import com.swe573.repositories.ThreadHistoryRepository;
import com.swe573.services.ThreadHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadHistoryServiceImpl implements ThreadHistoryService {

    @Autowired
    private ThreadHistoryRepository threadHistoryRepository;

    private ThreadHistoryEntry createHistoryEntry(Thread thread, User user, 
                                               ThreadHistoryEntry.ActionType actionType,
                                               ThreadHistoryEntry.EntityType entityType,
                                               Long entityId, String beforeState, 
                                               String afterState, String description) {
        ThreadHistoryEntry entry = ThreadHistoryEntry.builder()
            .thread(thread)
            .user(user)
            .actionType(actionType)
            .entityType(entityType)
            .entityId(entityId)
            .beforeState(beforeState)
            .afterState(afterState)
            .description(description)
            .build();
        
        return threadHistoryRepository.save(entry);
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadCreation(Thread thread, User user) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.CREATE, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            null, 
            thread.getTitle(), 
            "Thread created"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadUpdate(Thread thread, User user, String beforeState, String afterState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.UPDATE, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            beforeState, 
            afterState, 
            "Thread updated"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadDeletion(Thread thread, User user, String beforeState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.DELETE, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            beforeState, 
            null, 
            "Thread deleted"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadFollow(Thread thread, User user) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.FOLLOW, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            null, 
            null, 
            "User followed the thread"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadUnfollow(Thread thread, User user) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.UNFOLLOW, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            null, 
            null, 
            "User unfollowed the thread"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadVote(Thread thread, User user, boolean isUpvote) {
        ThreadHistoryEntry.ActionType actionType = isUpvote ? 
            ThreadHistoryEntry.ActionType.UPVOTE : ThreadHistoryEntry.ActionType.DOWNVOTE;
        String description = isUpvote ? "User upvoted the thread" : "User downvoted the thread";
        
        return createHistoryEntry(
            thread, 
            user, 
            actionType, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            null, 
            null, 
            description
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logThreadRemoveVote(Thread thread, User user) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.REMOVE_VOTE, 
            ThreadHistoryEntry.EntityType.THREAD, 
            thread.getId(), 
            null, 
            null, 
            "User removed vote from the thread"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logNodeCreation(Thread thread, User user, Long nodeId, String nodeDetails) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.CREATE, 
            ThreadHistoryEntry.EntityType.NODE, 
            nodeId, 
            null, 
            nodeDetails, 
            "Node created"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logNodeUpdate(Thread thread, User user, Long nodeId, String beforeState, String afterState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.UPDATE, 
            ThreadHistoryEntry.EntityType.NODE, 
            nodeId, 
            beforeState, 
            afterState, 
            "Node updated"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logNodeDeletion(Thread thread, User user, Long nodeId, String beforeState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.DELETE, 
            ThreadHistoryEntry.EntityType.NODE, 
            nodeId, 
            beforeState, 
            null, 
            "Node deleted"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logEdgeCreation(Thread thread, User user, Long edgeId, String edgeDetails) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.CREATE, 
            ThreadHistoryEntry.EntityType.EDGE, 
            edgeId, 
            null, 
            edgeDetails, 
            "Edge created"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logEdgeUpdate(Thread thread, User user, Long edgeId, String beforeState, String afterState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.UPDATE, 
            ThreadHistoryEntry.EntityType.EDGE, 
            edgeId, 
            beforeState, 
            afterState, 
            "Edge updated"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logEdgeDeletion(Thread thread, User user, Long edgeId, String beforeState) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.DELETE, 
            ThreadHistoryEntry.EntityType.EDGE, 
            edgeId, 
            beforeState, 
            null, 
            "Edge deleted"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logTagAddition(Thread thread, User user, Long tagId, String tagLabel) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.ADD_TAG, 
            ThreadHistoryEntry.EntityType.TAG, 
            tagId, 
            null, 
            tagLabel, 
            "Tag added to thread"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logTagRemoval(Thread thread, User user, Long tagId, String tagLabel) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.REMOVE_TAG, 
            ThreadHistoryEntry.EntityType.TAG, 
            tagId, 
            tagLabel, 
            null, 
            "Tag removed from thread"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logCommentCreation(Thread thread, User user, Long commentId, String commentContent) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.CREATE, 
            ThreadHistoryEntry.EntityType.COMMENT, 
            commentId, 
            null, 
            commentContent, 
            "Comment created"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logCommentUpdate(Thread thread, User user, Long commentId, String beforeContent, String afterContent) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.UPDATE, 
            ThreadHistoryEntry.EntityType.COMMENT, 
            commentId, 
            beforeContent, 
            afterContent, 
            "Comment updated"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logCommentDeletion(Thread thread, User user, Long commentId, String commentContent) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.DELETE, 
            ThreadHistoryEntry.EntityType.COMMENT, 
            commentId, 
            commentContent, 
            null, 
            "Comment deleted"
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logCommentVote(Thread thread, User user, Long commentId, boolean isUpvote) {
        ThreadHistoryEntry.ActionType actionType = isUpvote ? 
            ThreadHistoryEntry.ActionType.UPVOTE : ThreadHistoryEntry.ActionType.DOWNVOTE;
        String description = isUpvote ? "User upvoted a comment" : "User downvoted a comment";
        
        return createHistoryEntry(
            thread, 
            user, 
            actionType, 
            ThreadHistoryEntry.EntityType.COMMENT, 
            commentId, 
            null, 
            null, 
            description
        );
    }

    @Override
    @Transactional
    public ThreadHistoryEntry logCommentRemoveVote(Thread thread, User user, Long commentId) {
        return createHistoryEntry(
            thread, 
            user, 
            ThreadHistoryEntry.ActionType.REMOVE_VOTE, 
            ThreadHistoryEntry.EntityType.COMMENT, 
            commentId, 
            null, 
            null, 
            "User removed vote from a comment"
        );
    }

    private ThreadHistoryDTO convertToDTO(ThreadHistoryEntry entry) {
        return ThreadHistoryDTO.builder()
            .id(entry.getId())
            .threadId(entry.getThread().getId())
            .threadTitle(entry.getThread().getTitle())
            .user(ThreadHistoryDTO.UserDTO.builder()
                .id(entry.getUser().getId())
                .username(entry.getUser().getUsername())
                .build())
            .actionType(entry.getActionType())
            .entityType(entry.getEntityType())
            .entityId(entry.getEntityId())
            .beforeState(entry.getBeforeState())
            .afterState(entry.getAfterState())
            .description(entry.getDescription())
            .createdAt(entry.getCreatedAt())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThreadHistoryDTO> getThreadHistory(Long threadId) {
        return threadHistoryRepository.findByThreadIdOrderByCreatedAtDesc(threadId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ThreadHistoryDTO> getThreadHistoryPaginated(Long threadId, Pageable pageable) {
        return threadHistoryRepository.findByThreadIdOrderByCreatedAtDesc(threadId, pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThreadHistoryDTO> getThreadEntityHistory(Long threadId, ThreadHistoryEntry.EntityType entityType, Long entityId) {
        return threadHistoryRepository.findByThreadIdAndEntityIdAndEntityTypeOrderByCreatedAtDesc(threadId, entityId, entityType)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThreadHistoryDTO> getUserActionHistory(Long userId) {
        return threadHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
} 