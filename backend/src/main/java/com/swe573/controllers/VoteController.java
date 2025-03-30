package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.services.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/votes")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @PostMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteThread(
            @PathVariable Long threadId,
            @RequestParam boolean isUpvote,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        
        Vote vote = voteService.createThreadVote(userId, threadId, voteType);
        int voteCount = voteService.getThreadVoteCount(threadId);

        Map<String, Object> response = new HashMap<>();
        response.put("vote", vote);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", response));
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteComment(
            @PathVariable Long commentId,
            @RequestParam boolean isUpvote,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        
        Vote vote = voteService.createCommentVote(userId, commentId, voteType);
        int voteCount = voteService.getCommentVoteCount(commentId);

        Map<String, Object> response = new HashMap<>();
        response.put("vote", vote);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", response));
    }

    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeThreadVote(
            @PathVariable Long threadId,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        voteService.deleteVoteByUserAndThread(userId, threadId);
        int voteCount = voteService.getThreadVoteCount(threadId);

        Map<String, Object> response = new HashMap<>();
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", response));
    }

    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeCommentVote(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        voteService.deleteVoteByUserAndComment(userId, commentId);
        int voteCount = voteService.getCommentVoteCount(commentId);

        Map<String, Object> response = new HashMap<>();
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", response));
    }

    @GetMapping("/thread/{threadId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadVoteStatus(
            @PathVariable Long threadId,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        boolean hasVoted = voteService.hasUserVotedOnThread(userId, threadId);
        VoteType voteType = voteService.getUserVoteTypeOnThread(userId, threadId);
        int voteCount = voteService.getThreadVoteCount(threadId);

        Map<String, Object> response = new HashMap<>();
        response.put("hasVoted", hasVoted);
        response.put("voteType", voteType);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/comment/{commentId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommentVoteStatus(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        boolean hasVoted = voteService.hasUserVotedOnComment(userId, commentId);
        VoteType voteType = voteService.getUserVoteTypeOnComment(userId, commentId);
        int voteCount = voteService.getCommentVoteCount(commentId);

        Map<String, Object> response = new HashMap<>();
        response.put("hasVoted", hasVoted);
        response.put("voteType", voteType);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 