package com.swe573.services.impl;

import com.swe573.dto.ThreadDTO;
import com.swe573.models.Thread;
import com.swe573.models.Tag;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.repositories.ThreadRepository;
import com.swe573.repositories.TagRepository;
import com.swe573.repositories.UserRepository;
import com.swe573.services.ThreadService;
import com.swe573.services.VoteService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ThreadServiceImpl implements ThreadService {

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private VoteService voteService;

    @Override
    @Transactional
    public Thread createThread(ThreadDTO threadDTO) {
        User author = userRepository.findById(threadDTO.getAuthorId())
            .orElseThrow(() -> new EntityNotFoundException("Author not found"));

        Thread thread = new Thread();
        thread.setTitle(threadDTO.getTitle());
        thread.setDescription(threadDTO.getDescription());
        thread.setAuthor(author);

        // Handle tags
        Set<Tag> tags = new HashSet<>();
        if (threadDTO.getTags() != null) {
            for (String tagLabel : threadDTO.getTags()) {
                Tag tag = tagRepository.findByLabel(tagLabel)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setLabel(tagLabel);
                        return tagRepository.save(newTag);
                    });
                tags.add(tag);
            }
        }
        thread.setTags(tags);

        return threadRepository.save(thread);
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
    public List<Thread> searchThreads(String keyword) {
        return threadRepository.searchThreads(keyword);
    }

    @Override
    public List<Thread> getThreadsFollowedByUser(Long userId) {
        return threadRepository.findThreadsFollowedByUser(userId);
    }

    @Override
    @Transactional
    public Thread updateThread(Long id, ThreadDTO threadDTO) {
        Thread thread = threadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));

        thread.setTitle(threadDTO.getTitle());
        thread.setDescription(threadDTO.getDescription());

        // Update tags
        if (threadDTO.getTags() != null) {
            Set<Tag> tags = new HashSet<>();
            for (String tagLabel : threadDTO.getTags()) {
                Tag tag = tagRepository.findByLabel(tagLabel)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setLabel(tagLabel);
                        return tagRepository.save(newTag);
                    });
                tags.add(tag);
            }
            thread.setTags(tags);
        }

        return threadRepository.save(thread);
    }

    @Override
    @Transactional
    public void deleteThread(Long id) {
        Thread thread = threadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        threadRepository.delete(thread);
    }

    @Override
    @Transactional
    public Thread followThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        thread.getThreadFollowers().add(user);
        return threadRepository.save(thread);
    }

    @Override
    @Transactional
    public Thread unfollowThread(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        thread.getThreadFollowers().remove(user);
        return threadRepository.save(thread);
    }

    @Override
    @Transactional
    public Thread voteThread(Long threadId, Long userId, boolean isUpvote) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        Vote vote = voteService.createThreadVote(userId, threadId, voteType);
        thread.getVotes().add(vote);
        
        return threadRepository.save(thread);
    }

    @Override
    @Transactional
    public Thread removeVote(Long threadId, Long userId) {
        Thread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
        
        voteService.deleteVoteByUserAndThread(userId, threadId);
        
        return threadRepository.findById(threadId)
            .orElseThrow(() -> new EntityNotFoundException("Thread not found"));
    }
} 