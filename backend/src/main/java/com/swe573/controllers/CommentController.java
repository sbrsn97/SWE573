package com.swe573.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.swe573.models.Comment;
import com.swe573.models.User;
import com.swe573.services.AuthenticationService;
import com.swe573.services.CommentService;
import com.swe573.dto.ApiResponse;
import com.swe573.dto.CommentDTO;
import com.swe573.dto.CreateCommentDTO;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.UnauthorizedException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@Tag(name = "Comments", description = "APIs for managing comments")
public class CommentController {
    private final CommentService commentService;
    private final AuthenticationService authenticationService;

    public CommentController(CommentService commentService, AuthenticationService authenticationService) {
        this.commentService = commentService;
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Create comment", description = "Creates a new comment on a thread")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @Parameter(description = "Comment creation data", required = true) 
            @Valid @RequestBody CreateCommentDTO createCommentDTO) {
        User currentUser = authenticationService.getCurrentUser();
        Comment comment = commentService.createComment(createCommentDTO, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment created successfully", convertToDTO(comment)));
    }

    @Operation(summary = "Get comment", description = "Retrieves a comment by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDTO>> getComment(
            @Parameter(description = "ID of the comment to retrieve", required = true) 
            @PathVariable Long id) {
        Comment comment = commentService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(comment)));
    }

    @Operation(summary = "Get thread comments", description = "Retrieves all comments for a specific thread")
    @GetMapping("/thread/{threadId}")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getThreadComments(
            @Parameter(description = "ID of the thread", required = true) 
            @PathVariable Long threadId) {
        List<Comment> comments = commentService.findByThreadId(threadId);
        List<CommentDTO> commentDTOs = comments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(commentDTOs));
    }
    
    @Operation(summary = "Get parent comments for thread", description = "Retrieves all top-level comments for a specific thread")
    @GetMapping("/thread/{threadId}/parents")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getThreadParentComments(
            @Parameter(description = "ID of the thread", required = true) 
            @PathVariable Long threadId) {
        List<Comment> comments = commentService.findParentCommentsByThreadId(threadId);
        List<CommentDTO> commentDTOs = comments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(commentDTOs));
    }
    
    @Operation(summary = "Get child comments", description = "Retrieves all child comments for a specific parent comment")
    @GetMapping("/{parentId}/children")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getChildComments(
            @Parameter(description = "ID of the parent comment", required = true) 
            @PathVariable Long parentId) {
        // First verify the parent exists
        commentService.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            
        List<Comment> comments = commentService.findChildCommentsByParentId(parentId);
        List<CommentDTO> commentDTOs = comments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(commentDTOs));
    }

    @Operation(summary = "Update comment", description = "Updates the content of a comment")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentDTO>> updateComment(
            @Parameter(description = "ID of the comment to update", required = true) 
            @PathVariable Long id,
            @Parameter(description = "New content for the comment", required = true) 
            @RequestBody String newContent) {
        User currentUser = authenticationService.getCurrentUser();
        Comment comment = commentService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to update this comment");
        }

        comment = commentService.updateComment(id, newContent);
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", convertToDTO(comment)));
    }

    @Operation(summary = "Delete comment", description = "Soft deletes a comment")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "ID of the comment to delete", required = true) 
            @PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        commentService.softDeleteComment(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    @Operation(summary = "Hard delete comment", description = "Permanently deletes a comment")
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteComment(
            @Parameter(description = "ID of the comment to delete permanently", required = true) 
            @PathVariable Long id) {
        commentService.hardDeleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("Comment permanently deleted", null));
    }

    @Operation(summary = "Reactivate comment", description = "Reactivates a soft-deleted comment")
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<CommentDTO>> reactivateComment(
            @Parameter(description = "ID of the comment to reactivate", required = true) 
            @PathVariable Long id) {
        Comment comment = commentService.reactivateComment(id);
        return ResponseEntity.ok(ApiResponse.success("Comment reactivated successfully", convertToDTO(comment)));
    }

    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setAuthorId(comment.getAuthor().getId());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setThreadId(comment.getThread().getId());
        
        // Set parent ID if this is a child comment
        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }
        
        dto.setReferencedNodeIds(comment.getReferencedNodes().stream()
            .map(node -> node.getId())
            .collect(Collectors.toSet()));
        dto.setUpvoteCount(comment.getUpvoteCount());
        dto.setDownvoteCount(comment.getDownvoteCount());
        dto.setActive(comment.isActive());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }
} 