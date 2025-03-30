package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.ThreadDTO;
import com.swe573.models.Thread;
import com.swe573.models.User;
import com.swe573.services.ThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/threads")
public class ThreadController {

    @Autowired
    private ThreadService threadService;

    @PostMapping
    public ResponseEntity<ApiResponse<ThreadDTO>> createThread(@RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.createThread(threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread created successfully", convertToDTO(thread)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreadDTO>> getThread(@PathVariable Long id) {
        Thread thread = threadService.getThread(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(thread)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getAllThreads() {
        List<Thread> threads = threadService.getAllThreads();
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getThreadsByAuthor(@PathVariable Long userId) {
        List<Thread> threads = threadService.getThreadsByAuthor(userId);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/tag/{tagLabel}")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getThreadsByTag(@PathVariable String tagLabel) {
        List<Thread> threads = threadService.getThreadsByTag(tagLabel);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> searchThreads(@RequestParam String keyword) {
        List<Thread> threads = threadService.searchThreads(keyword);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<ThreadDTO>>> getFollowedThreads(Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        List<Thread> threads = threadService.getThreadsFollowedByUser(userId);
        List<ThreadDTO> dtos = threads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreadDTO>> updateThread(
            @PathVariable Long id,
            @RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.updateThread(id, threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread updated successfully", convertToDTO(thread)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(@PathVariable Long id) {
        threadService.deleteThread(id);
        return ResponseEntity.ok(ApiResponse.success("Thread deleted successfully", null));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<ThreadDTO>> followThread(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.followThread(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thread followed successfully", convertToDTO(thread)));
    }

    @PostMapping("/{id}/unfollow")
    public ResponseEntity<ApiResponse<ThreadDTO>> unfollowThread(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.unfollowThread(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thread unfollowed successfully", convertToDTO(thread)));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<ThreadDTO>> voteThread(
            @PathVariable Long id,
            @RequestParam boolean isUpvote,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.voteThread(id, userId, isUpvote);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", convertToDTO(thread)));
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<ThreadDTO>> removeVote(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.removeVote(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", convertToDTO(thread)));
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
        dto.setTags(thread.getTags().stream()
                .map(tag -> tag.getLabel())
                .collect(Collectors.toSet()));
        dto.setFollowerIds(thread.getThreadFollowers().stream()
                .map(user -> user.getId())
                .collect(Collectors.toSet()));
        return dto;
    }
} 