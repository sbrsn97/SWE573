package com.swe573.services.impl;

import com.swe573.dto.ThreadDTO;
import com.swe573.dto.TagDTO;
import com.swe573.models.Thread;
import com.swe573.models.Tag;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.models.enums.ThreadStyle;
import com.swe573.models.enums.Role;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.TagRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.ThreadService;
import com.swe573.services.VoteService;
import com.swe573.services.NlpService;
import com.swe573.services.ThreadHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ThreadServiceImpl implements ThreadService {

    @Autowired
    private ThreadRepository threadRepository;

    @Override
    public Thread save(Thread thread) {
        return threadRepository.save(thread);
    }

    @Override 
    public Optional<Thread> findById(Long id) {
        return threadRepository.findById(id);
    }

    @Override
    @Transactional
    public void delete(Thread thread) {
        // Additional safety check to ensure all collections are cleared before deletion
        if (thread.getVotes() != null && !thread.getVotes().isEmpty()) {
            for (Vote vote : new HashSet<>(thread.getVotes())) {
                vote.setThread(null);
                voteService.deleteVote(vote.getId());
            }
            thread.getVotes().clear();
        }
        
        // Perform the actual deletion
        threadRepository.delete(thread);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private VoteService voteService;

    @Autowired
    private NlpService nlpService;

    @Autowired
    private ThreadHistoryService threadHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @Override
    @Transactional
    public Thread createThread(ThreadDTO threadDTO) {
        // Check for profanity in title or description
        boolean hasProfanityInTitle = nlpService.containsProfanity(threadDTO.getTitle());
        boolean hasProfanityInDesc = threadDTO.getDescription() != null && 
                                    nlpService.containsProfanity(threadDTO.getDescription());
        
        if (hasProfanityInTitle || hasProfanityInDesc) {
            throw new IllegalArgumentException("Thread contains inappropriate language and cannot be created.");
        }
        
        User author = userRepository.findById(threadDTO.getAuthorId())
            .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        Thread thread = new Thread();
        thread.setTitle(threadDTO.getTitle());
        thread.setDescription(threadDTO.getDescription());
        thread.setAuthor(author);
        
        // Set thread style if provided, otherwise default to PUBLIC
        if (threadDTO.getThreadStyle() != null) {
            thread.setThreadStyle(threadDTO.getThreadStyle());
        }

        // Handle tags
        Set<Tag> tags = new HashSet<>();
        if (threadDTO.getTags() != null) {
            for (TagDTO tagDTO : threadDTO.getTags()) {
                Tag tag = tagRepository.findByLabel(tagDTO.getLabel())
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setLabel(tagDTO.getLabel());
                        newTag.setDescription(tagDTO.getDescription());
                        newTag.setColorCodeString(tagDTO.getColorCodeString());
                        newTag.setWikidataEntityId(tagDTO.getWikidataEntityId());
                        return tagRepository.save(newTag);
                    });
                tags.add(tag);
            }
        }
        thread.setTags(tags);

        Thread savedThread = threadRepository.save(thread);
        
        // Log thread creation to history
        threadHistoryService.logThreadCreation(savedThread, author);
        
        return savedThread;
    }

    @Override
    public Thread getThread(Long id) {
        return threadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
    }

    @Override
    public List<Thread> getAllThreads() {
        return threadRepository.findAll();
    }

    @Override
    public List<Thread> getThreadsByAuthor(Long authorId) {
        return threadRepository.findByAuthorId(authorId);
    }

    @Override
    public List<Thread> getThreadsByTag(String tagLabel) {
        return threadRepository.findByTagLabel(tagLabel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Thread> searchThreads(String keyword) {
        List<Thread> threads = threadRepository.searchThreads(keyword);
        // Initialize tags collection
        var thread = threads.get(0);
        for(var t : thread.getTags()) {
            var tag = t;
        }
        //threads.forEach(thread -> thread.getTags().size());
        return threads;
    }

    @Override
    public List<Thread> getThreadsFollowedByUser(Long userId) {
        return threadRepository.findThreadsFollowedByUser(userId);
    }

    @Override
    @Transactional
    public Thread updateThread(Long id, ThreadDTO threadDTO) {
        Thread existingThread = threadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User currentUser = userRepository.findById(threadDTO.getAuthorId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Check for profanity in title or description
        boolean hasProfanityInTitle = nlpService.containsProfanity(threadDTO.getTitle());
        boolean hasProfanityInDesc = threadDTO.getDescription() != null && 
                                    nlpService.containsProfanity(threadDTO.getDescription());
        
        if (hasProfanityInTitle || hasProfanityInDesc) {
            throw new IllegalArgumentException("Thread contains inappropriate language and cannot be updated.");
        }
        
        // Capture before state for history
        String beforeState = toJson(existingThread);
        
        // Update thread fields
        existingThread.setTitle(threadDTO.getTitle());
        existingThread.setDescription(threadDTO.getDescription());
        
        // Update thread style if provided
        if (threadDTO.getThreadStyle() != null) {
            existingThread.setThreadStyle(threadDTO.getThreadStyle());
        }
        
        // Handle tags
        Set<Tag> oldTags = new HashSet<>(existingThread.getTags());
        Set<Tag> newTags = new HashSet<>();
        
        if (threadDTO.getTags() != null) {
            for (TagDTO tagDTO : threadDTO.getTags()) {
                Tag tag = tagRepository.findByLabel(tagDTO.getLabel())
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setLabel(tagDTO.getLabel());
                        newTag.setDescription(tagDTO.getDescription());
                        newTag.setColorCodeString(tagDTO.getColorCodeString());
                        newTag.setWikidataEntityId(tagDTO.getWikidataEntityId());
                        return tagRepository.save(newTag);
                    });
                newTags.add(tag);
            }
        }
        
        // Find removed tags
        Set<Tag> removedTags = new HashSet<>(oldTags);
        removedTags.removeAll(newTags);
        
        // Find added tags
        Set<Tag> addedTags = new HashSet<>(newTags);
        addedTags.removeAll(oldTags);
        
        // Update thread tags
        existingThread.setTags(newTags);
        
        // Save the thread
        Thread updatedThread = threadRepository.save(existingThread);
        
        // Log thread update to history
        threadHistoryService.logThreadUpdate(
            updatedThread, 
            currentUser, 
            beforeState, 
            toJson(updatedThread)
        );
        
        // Log tag changes
        for (Tag tag : addedTags) {
            threadHistoryService.logTagAddition(
                updatedThread, 
                currentUser, 
                tag.getId(), 
                tag.getLabel()
            );
        }
        
        for (Tag tag : removedTags) {
            threadHistoryService.logTagRemoval(
                updatedThread, 
                currentUser, 
                tag.getId(), 
                tag.getLabel()
            );
        }
        
        return updatedThread;
    }

    @Override
    @Transactional
    public void deleteThread(Long id) {
        Thread thread = threadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User currentUser = thread.getAuthor(); // Simplified - normally would get from authentication
        
        // Log thread deletion to history
        threadHistoryService.logThreadDeletion(
            thread, 
            currentUser, 
            toJson(thread)
        );
        
        thread.setActive(false);
        threadRepository.save(thread);
    }

    @Override
    @Transactional
    public Thread followThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        if (thread.getThreadFollowers().contains(user)) {
            return thread; // User already follows this thread
        }
        
        thread.getThreadFollowers().add(user);
        Thread updatedThread = threadRepository.save(thread);
        
        // Log thread follow to history
        threadHistoryService.logThreadFollow(updatedThread, user);
        
        return updatedThread;
    }

    @Override
    @Transactional
    public Thread unfollowThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        if (!thread.getThreadFollowers().contains(user)) {
            return thread; // User doesn't follow this thread
        }
        
        thread.getThreadFollowers().remove(user);
        Thread updatedThread = threadRepository.save(thread);
        
        // Log thread unfollow to history
        threadHistoryService.logThreadUnfollow(updatedThread, user);
        
        return updatedThread;
    }

    @Override
    public boolean canUserInteractWithThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
        // Admins can interact with any thread
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        
        // Author can always interact with their own thread
        if (thread.getAuthor().getId().equals(userId)) {
            return true;
        }
        
        // For FOLLOW_TO_INTERACT threads, check if user is a follower
        if (thread.getThreadStyle() == ThreadStyle.FOLLOW_TO_INTERACT) {
            return thread.getThreadFollowers().stream()
                .anyMatch(follower -> follower.getId().equals(userId));
        }
        
        // For PRIVATE threads (not fully implemented), only explicit permissions would allow
        if (thread.getThreadStyle() == ThreadStyle.PRIVATE) {
            // For now, only author and admin can interact with private threads
            return false;
        }
        
        // PUBLIC threads are open to all
        return thread.getThreadStyle() == ThreadStyle.PUBLIC;
    }
    
    @Override
    public boolean canUserViewThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        // If thread is inactive, only author and admins can view it
        if (!thread.isActive()) {
            if (userId == null) return false;
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
            return thread.getAuthor().getId().equals(userId) || user.getRole() == Role.ADMIN;
        }
        
        // For PUBLIC and FOLLOW_TO_INTERACT threads, anyone can view
        if (thread.getThreadStyle() == ThreadStyle.PUBLIC || 
            thread.getThreadStyle() == ThreadStyle.FOLLOW_TO_INTERACT) {
            return true;
        }
        
        // For PRIVATE threads, only author, followers, and admins can view
        if (thread.getThreadStyle() == ThreadStyle.PRIVATE) {
            // If no userId, anonymous users cannot access private threads
            if (userId == null) return false;
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
            // Author can always view
            if (thread.getAuthor().getId().equals(userId)) {
                return true;
            }
            
            // Admin can always view
            if (user.getRole() == Role.ADMIN) {
                return true;
            }
            
            // For now, followers cannot view private threads (we'll implement explicit permissions later)
            return false;
        }
        
        return false;
    }
    
    @Override
    @Transactional
    public Thread voteThread(Long threadId, Long userId, boolean isUpvote) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
        // Check if the user can interact with this thread
        if (!canUserInteractWithThread(threadId, userId)) {
            if (thread.getThreadStyle() == ThreadStyle.FOLLOW_TO_INTERACT) {
                throw new IllegalStateException("You must follow this thread to vote on it");
            } else {
                throw new IllegalStateException("You do not have permission to vote on this thread");
            }
        }
        
        // First remove any existing votes
        if (voteService.hasUserVotedOnThread(userId, threadId)) {
            voteService.deleteVoteByUserAndThread(userId, threadId);
        }
        
        // Create the new vote with the appropriate VoteType
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        voteService.createThreadVote(userId, threadId, voteType);
        
        // Refresh the thread to ensure vote counts are updated
        return threadRepository.findById(threadId).orElseThrow(() -> 
            new EntityNotFoundException("Thread not found after voting"));
    }

    @Override
    @Transactional
    public Thread removeVote(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Let VoteService handle removing the vote using the deleteVoteByUserAndThread method
        voteService.deleteVoteByUserAndThread(userId, threadId);
        
        // Re-fetch the thread to get updated vote counts
        Thread updatedThread = threadRepository.findById(threadId).orElseThrow();
        
        // Log thread vote removal to history
        threadHistoryService.logThreadRemoveVote(updatedThread, user);
        
        return updatedThread;
    }
} 