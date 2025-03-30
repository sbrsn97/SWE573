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

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/threads")
public class ThreadController {

    @Autowired
    private ThreadService threadService;

    @PostMapping
    public ResponseEntity<ApiResponse<Thread>> createThread(@RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.createThread(threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread created successfully", thread));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Thread>> getThread(@PathVariable Long id) {
        Thread thread = threadService.getThread(id);
        return ResponseEntity.ok(ApiResponse.success(thread));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Thread>>> getAllThreads() {
        List<Thread> threads = threadService.getAllThreads();
        return ResponseEntity.ok(ApiResponse.success(threads));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Thread>>> getThreadsByAuthor(@PathVariable Long userId) {
        List<Thread> threads = threadService.getThreadsByAuthor(userId);
        return ResponseEntity.ok(ApiResponse.success(threads));
    }

    @GetMapping("/tag/{tagLabel}")
    public ResponseEntity<ApiResponse<List<Thread>>> getThreadsByTag(@PathVariable String tagLabel) {
        List<Thread> threads = threadService.getThreadsByTag(tagLabel);
        return ResponseEntity.ok(ApiResponse.success(threads));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Thread>>> searchThreads(@RequestParam String keyword) {
        List<Thread> threads = threadService.searchThreads(keyword);
        return ResponseEntity.ok(ApiResponse.success(threads));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<Thread>>> getFollowedThreads(Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        List<Thread> threads = threadService.getThreadsFollowedByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(threads));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Thread>> updateThread(
            @PathVariable Long id,
            @RequestBody ThreadDTO threadDTO) {
        Thread thread = threadService.updateThread(id, threadDTO);
        return ResponseEntity.ok(ApiResponse.success("Thread updated successfully", thread));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(@PathVariable Long id) {
        threadService.deleteThread(id);
        return ResponseEntity.ok(ApiResponse.success("Thread deleted successfully", null));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Thread>> followThread(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.followThread(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thread followed successfully", thread));
    }

    @PostMapping("/{id}/unfollow")
    public ResponseEntity<ApiResponse<Thread>> unfollowThread(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.unfollowThread(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thread unfollowed successfully", thread));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<Thread>> voteThread(
            @PathVariable Long id,
            @RequestParam boolean isUpvote,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.voteThread(id, userId, isUpvote);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", thread));
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<Thread>> removeVote(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        Thread thread = threadService.removeVote(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", thread));
    }
} 