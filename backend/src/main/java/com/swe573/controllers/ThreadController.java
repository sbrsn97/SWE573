package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.ThreadDTO;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.services.ThreadService;
import com.swe573.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.UnauthorizedException;
import com.swe573.services.ThreadPreviewService;
import com.swe573.dto.ThreadPreviewDTO;
import com.swe573.dto.TagDTO;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;

@Tag(name = "Threads", description = "APIs for managing discussion threads")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/threads")
public class ThreadController {

    @Autowired
    private ThreadService threadService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ThreadPreviewService threadPreviewService;

    @Operation(summary = "Create thread", description = "Creates a new discussion thread")
    @PostMapping
    public ResponseEntity<ApiResponse<ThreadDTO>> createThread(
            @Parameter(description = "Thread data", required = true) @RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.createThread(threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread created successfully", convertToDTO(thread)));
    }

    @Operation(summary = "Get thread by ID", description = "Retrieves a specific thread by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreadDTO>> getThread(
            @Parameter(description = "ID of the thread to retrieve", required = true) @PathVariable Long id) {
        Thread thread = threadService.getThread(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(thread)));
    }

    @Operation(summary = "Get all threads", description = "Retrieves a list of all threads")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getAllThreads() {
        List<Thread> threads = threadService.getAllThreads();
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get threads by author", description = "Retrieves all threads created by a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getThreadsByAuthor(
            @Parameter(description = "ID of the author", required = true) @PathVariable Long userId) {
        List<Thread> threads = threadService.getThreadsByAuthor(userId);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get threads by tag", description = "Retrieves all threads with a specific tag")
    @GetMapping("/tag/{tagLabel}")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getThreadsByTag(
            @Parameter(description = "Label of the tag", required = true) @PathVariable String tagLabel) {
        List<Thread> threads = threadService.getThreadsByTag(tagLabel);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Search threads", description = "Searches threads by keyword")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> searchThreads(
            @Parameter(description = "Search keyword", required = true) @RequestParam String keyword) {
        List<Thread> threads = threadService.searchThreads(keyword.toLowerCase());
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get followed threads", description = "Retrieves all threads that the current user follows")
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getFollowedThreads() {
        User currentUser = authenticationService.getCurrentUser();
        List<Thread> threads = threadService.getThreadsFollowedByUser(currentUser.getId());
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Update thread", description = "Updates an existing thread")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreadDTO>> updateThread(
            @Parameter(description = "ID of the thread to update", required = true) @PathVariable Long id,
            @Parameter(description = "Updated thread data", required = true) @RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.updateThread(id, threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread updated successfully", convertToDTO(thread)));
    }

    @Operation(summary = "Delete thread", description = "Deletes a thread")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(@PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        if (thread.getAuthor().getId().equals(currentUser.getId())) {
            // Author deletion
            thread.softDeleteByUser();
        } else if (authenticationService.isAdmin()) {
            // Admin deletion
            thread.softDeleteByAdmin();
        } else {
            throw new UnauthorizedException("You don't have permission to delete this thread");
        }

        threadService.save(thread);
        return ResponseEntity.ok(ApiResponse.success("Thread deactivated successfully", null));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateThread(@PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));

        if (!thread.canBeReactivatedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to reactivate this thread");
        }

        thread.reactivate();
        threadService.save(thread);
        return ResponseEntity.ok(ApiResponse.success("Thread reactivated successfully", null));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteThread(@PathVariable Long id) {
        Thread thread = threadService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        
        thread.hardDelete();
        threadService.delete(thread);
        return ResponseEntity.ok(ApiResponse.success("Thread permanently deleted", null));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<ThreadDTO>> followThread(
            @PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.followThread(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Thread followed successfully", convertToDTO(thread)));
    }

    @PostMapping("/{id}/unfollow")
    public ResponseEntity<ApiResponse<ThreadDTO>> unfollowThread(
            @PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.unfollowThread(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Thread unfollowed successfully", convertToDTO(thread)));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<ThreadDTO>> voteThread(
            @PathVariable Long id,
            @RequestParam boolean isUpvote) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.voteThread(id, currentUser.getId(), isUpvote);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", convertToDTO(thread)));
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<ThreadDTO>> removeVote(
            @PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Thread thread = threadService.removeVote(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", convertToDTO(thread)));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<ThreadPreviewDTO>> previewThread(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content) {
        ThreadPreviewDTO preview = threadPreviewService.generatePreview(title, content);
        return ResponseEntity.ok(ApiResponse.success(preview));
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
        dto.setTags(thread.getTags() != null ? thread.getTags().stream()
                .map(tag -> {
                    TagDTO tagDTO = new TagDTO();
                    tagDTO.setId(tag.getId());
                    tagDTO.setLabel(tag.getLabel());
                    tagDTO.setDescription(tag.getDescription());
                    tagDTO.setColorCodeString(tag.getColorCodeString());
                    tagDTO.setWikidataEntityId(tag.getWikidataEntityId());
                    return tagDTO;
                })
                .collect(Collectors.toSet()) : new HashSet<>());
        dto.setFollowerIds(thread.getThreadFollowers() != null ? thread.getThreadFollowers().stream()
                .map(user -> user.getId())
                .collect(Collectors.toSet()) : new HashSet<>());
        return dto;
    }
} 