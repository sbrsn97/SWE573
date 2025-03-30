package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.VoteDTO;
import com.swe573.models.User;
import com.swe573.models.Vote;
import com.swe573.models.enums.VoteType;
import com.swe573.services.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Votes", description = "APIs for managing thread and comment votes")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/votes")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @Operation(summary = "Get vote by ID", description = "Retrieves a specific vote by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoteDTO>> getVote(
            @Parameter(description = "ID of the vote to retrieve", required = true) @PathVariable Long id) {
        Vote vote = voteService.getVote(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(vote)));
    }

    @Operation(summary = "Vote on thread", description = "Creates or updates a vote on a thread")
    @PostMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteThread(
            @Parameter(description = "ID of the thread to vote on", required = true) @PathVariable Long threadId,
            @Parameter(description = "Whether this is an upvote (true) or downvote (false)", required = true) @RequestParam boolean isUpvote,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        
        Vote vote = voteService.createThreadVote(userId, threadId, voteType);
        int voteCount = voteService.getThreadVoteCount(threadId);

        VoteDTO voteDTO = convertToDTO(vote);

        Map<String, Object> response = new HashMap<>();
        response.put("vote", voteDTO);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", response));
    }

    @Operation(summary = "Vote on comment", description = "Creates or updates a vote on a comment")
    @PostMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteComment(
            @Parameter(description = "ID of the comment to vote on", required = true) @PathVariable Long commentId,
            @Parameter(description = "Whether this is an upvote (true) or downvote (false)", required = true) @RequestParam boolean isUpvote,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        VoteType voteType = isUpvote ? VoteType.UPVOTE : VoteType.DOWNVOTE;
        
        Vote vote = voteService.createCommentVote(userId, commentId, voteType);
        int voteCount = voteService.getCommentVoteCount(commentId);

        VoteDTO voteDTO = convertToDTO(vote);

        Map<String, Object> response = new HashMap<>();
        response.put("vote", voteDTO);
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", response));
    }

    @Operation(summary = "Remove thread vote", description = "Removes a user's vote from a thread")
    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeThreadVote(
            @Parameter(description = "ID of the thread to remove vote from", required = true) @PathVariable Long threadId,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        voteService.deleteVoteByUserAndThread(userId, threadId);
        int voteCount = voteService.getThreadVoteCount(threadId);

        Map<String, Object> response = new HashMap<>();
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", response));
    }

    @Operation(summary = "Remove comment vote", description = "Removes a user's vote from a comment")
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeCommentVote(
            @Parameter(description = "ID of the comment to remove vote from", required = true) @PathVariable Long commentId,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
        Long userId = ((User) authentication.getPrincipal()).getId();
        
        voteService.deleteVoteByUserAndComment(userId, commentId);
        int voteCount = voteService.getCommentVoteCount(commentId);

        Map<String, Object> response = new HashMap<>();
        response.put("voteCount", voteCount);

        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", response));
    }

    @Operation(summary = "Get thread vote status", description = "Retrieves the current user's vote status on a thread")
    @GetMapping("/thread/{threadId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadVoteStatus(
            @Parameter(description = "ID of the thread to check vote status", required = true) @PathVariable Long threadId,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
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

    @Operation(summary = "Get comment vote status", description = "Retrieves the current user's vote status on a comment")
    @GetMapping("/comment/{commentId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommentVoteStatus(
            @Parameter(description = "ID of the comment to check vote status", required = true) @PathVariable Long commentId,
            @Parameter(description = "Authentication object containing user details", required = true) Authentication authentication) {
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

    private VoteDTO convertToDTO(Vote vote) {
        VoteDTO dto = new VoteDTO();
        dto.setId(vote.getId());
        dto.setUserId(vote.getUser().getId());
        dto.setThreadId(vote.getThread() != null ? vote.getThread().getId() : null);
        dto.setCommentId(vote.getComment() != null ? vote.getComment().getId() : null);
        dto.setType(vote.getType());
        return dto;
    }
} 