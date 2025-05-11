package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.ThreadDTO;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.services.VoteAnalyticsService;
import com.swe573.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Analytics", description = "APIs for thread analytics and recommendations")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private VoteAnalyticsService voteAnalyticsService;
    
    @Autowired
    private AuthenticationService authenticationService;

    @Operation(summary = "Get hot threads", description = "Retrieves threads with recent activity")
    @GetMapping("/hot-threads")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getHotThreads(
            @Parameter(description = "Number of days to look back for activity", required = false) 
            @RequestParam(defaultValue = "7") int daysBack,
            @Parameter(description = "Maximum number of threads to return", required = false) 
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Thread> hotThreads = voteAnalyticsService.getHotThreads(daysBack, limit);
        List<ThreadDTO> hotThreadDTOs = hotThreads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(ApiResponse.success(hotThreadDTOs));
    }
    
    @Operation(summary = "Get most voted threads", description = "Retrieves threads with the most votes")
    @GetMapping("/most-voted")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getMostVotedThreads(
            @Parameter(description = "Maximum number of threads to return", required = false) 
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Thread> mostVotedThreads = voteAnalyticsService.getMostVotedThreads(limit);
        List<ThreadDTO> threadDTOs = mostVotedThreads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(ApiResponse.success(threadDTOs));
    }
    
    @Operation(summary = "Get recommended threads for current user", 
              description = "Retrieves threads recommended based on user interests and followed threads")
    @GetMapping("/recommended-threads")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getRecommendedThreads(
            @Parameter(description = "Maximum number of threads to return", required = false) 
            @RequestParam(defaultValue = "10") int limit) {
        
        User currentUser = authenticationService.getCurrentUser();
        List<Thread> recommendedThreads = voteAnalyticsService.getRecommendedThreadsForUser(currentUser.getId(), limit);
        List<ThreadDTO> threadDTOs = recommendedThreads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(ApiResponse.success(threadDTOs));
    }
    
    @Operation(summary = "Get similar threads", 
              description = "Retrieves threads similar to a specific thread based on tag overlap")
    @GetMapping("/similar-threads/{threadId}")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getSimilarThreads(
            @Parameter(description = "ID of the thread to find similar threads for", required = true) 
            @PathVariable Long threadId,
            @Parameter(description = "Maximum number of similar threads to return", required = false) 
            @RequestParam(defaultValue = "5") int limit) {
        
        List<Thread> similarThreads = voteAnalyticsService.getSimilarThreads(threadId, limit);
        List<ThreadDTO> threadDTOs = similarThreads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(ApiResponse.success(threadDTOs));
    }
    
    private ThreadDTO convertToDTO(Thread thread) {
        ThreadDTO dto = new ThreadDTO();
        dto.setId(thread.getId());
        dto.setTitle(thread.getTitle());
        dto.setDescription(thread.getDescription());
        dto.setAuthorId(thread.getAuthor().getId());
        dto.setCreatedAt(thread.getCreatedAt());
        dto.setUpdatedAt(thread.getUpdatedAt());
        dto.setUpvoteCount(thread.getUpvoteCount());
        dto.setDownvoteCount(thread.getDownvoteCount());
        
        // Convert tags
        if (thread.getTags() != null) {
            dto.setTags(thread.getTags().stream()
                .map(tag -> {
                    com.swe573.dto.TagDTO tagDTO = new com.swe573.dto.TagDTO();
                    tagDTO.setId(tag.getId());
                    tagDTO.setLabel(tag.getLabel());
                    tagDTO.setDescription(tag.getDescription());
                    tagDTO.setColorCodeString(tag.getColorCodeString());
                    tagDTO.setWikidataEntityId(tag.getWikidataEntityId());
                    return tagDTO;
                })
                .collect(Collectors.toSet()));
        } else {
            dto.setTags(new HashSet<>());
        }
        
        // Convert follower IDs
        if (thread.getThreadFollowers() != null) {
            dto.setFollowerIds(thread.getThreadFollowers().stream()
                .map(user -> user.getId())
                .collect(Collectors.toSet()));
        } else {
            dto.setFollowerIds(new HashSet<>());
        }
        
        return dto;
    }
} 