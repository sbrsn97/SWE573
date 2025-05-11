package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.ThreadHistoryDTO;
import com.swe573.models.ThreadHistoryEntry;
import com.swe573.services.ThreadHistoryService;
import com.swe573.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Thread History", description = "APIs for tracking thread history")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/threads/{threadId}/history")
public class ThreadHistoryController {

    @Autowired
    private ThreadHistoryService threadHistoryService;

    @Autowired
    private AuthenticationService authenticationService;

    @Operation(summary = "Get thread history", description = "Retrieves the full history of a thread")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ThreadHistoryDTO>>> getThreadHistory(
            @Parameter(description = "ID of the thread", required = true) 
            @PathVariable Long threadId) {
        
        List<ThreadHistoryDTO> history = threadHistoryService.getThreadHistory(threadId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "Get paginated thread history", description = "Retrieves a paginated history of a thread")
    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<ThreadHistoryDTO>>> getThreadHistoryPaginated(
            @Parameter(description = "ID of the thread", required = true) 
            @PathVariable Long threadId,
            @Parameter(description = "Page number (zero-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ThreadHistoryDTO> history = threadHistoryService.getThreadHistoryPaginated(
            threadId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "Get entity history in thread", description = "Retrieves history for a specific entity in a thread")
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<ThreadHistoryDTO>>> getEntityHistory(
            @Parameter(description = "ID of the thread", required = true) 
            @PathVariable Long threadId,
            @Parameter(description = "Type of entity (THREAD, NODE, EDGE, COMMENT, TAG)", required = true) 
            @PathVariable String entityType,
            @Parameter(description = "ID of the entity", required = true) 
            @PathVariable Long entityId) {
        
        ThreadHistoryEntry.EntityType type = ThreadHistoryEntry.EntityType.valueOf(entityType.toUpperCase());
        List<ThreadHistoryDTO> history = threadHistoryService.getThreadEntityHistory(threadId, type, entityId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "Get user actions in thread", description = "Retrieves history of a specific user's actions in all threads")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ThreadHistoryDTO>>> getUserActionHistory(
            @Parameter(description = "ID of the user", required = true) 
            @PathVariable Long userId) {
        
        // Verify the current user has permission (admin or the user themselves)
        if (!authenticationService.isAdmin() && 
            !authenticationService.getCurrentUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(
                ApiResponse.error("You don't have permission to view this user's history"));
        }
        
        List<ThreadHistoryDTO> history = threadHistoryService.getUserActionHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
} 