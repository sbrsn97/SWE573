package com.swe573.controllers;

import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.dto.PaginatedResponse;
import com.swe573.services.WikidataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.InvalidInputException;
import com.swe573.exceptions.RateLimitExceededException;
import com.swe573.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bucket4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Duration;

@Tag(name = "Wikidata", description = "APIs for interacting with Wikidata")
@RestController
@RequestMapping("/api/wikidata")
@CrossOrigin(origins = "*")
@Validated
@PreAuthorize("isAuthenticated()")
public class WikidataController {

    @Autowired
    private WikidataService wikidataService;

    private final Bucket bucket;

    public WikidataController() {
        // Rate limit: 60 requests per minute
        Bandwidth limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
    }

    @Operation(summary = "Get topic details", description = "Retrieves details for a specific topic from Wikidata")
    @GetMapping("/topics/{id}")
    public ResponseEntity<WikidataEntityDTO> getTopicDetails(
            @Parameter(description = "Wikidata entity ID", required = true) @PathVariable String id) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        if (!isValidWikidataId(id)) {
            throw new InvalidInputException("Invalid Wikidata ID format");
        }
        return ResponseEntity.ok(wikidataService.getTopicDetails(id));
    }

    @Operation(summary = "Search topics", description = "Searches for topics in Wikidata")
    @GetMapping("/topics/search")
    public ResponseEntity<PaginatedResponse<WikidataEntityDTO>> searchTopics(
            @Parameter(description = "Search query", required = true) @RequestParam String query,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        return ResponseEntity.ok(wikidataService.searchTopics(query, page, size));
    }

    @Operation(summary = "Get all entities", description = "Retrieves a paginated list of all entities from Wikidata")
    @GetMapping("/entities")
    public ResponseEntity<PaginatedResponse<WikidataEntityDTO>> getAllEntities(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        return ResponseEntity.ok(wikidataService.getAllEntities(page, size));
    }

    @Operation(summary = "Search entities", description = "Searches for entities in Wikidata")
    @GetMapping("/entities/search")
    public ResponseEntity<PaginatedResponse<WikidataEntityDTO>> searchEntities(
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        return ResponseEntity.ok(wikidataService.searchEntities(query, page, size));
    }

    @Operation(summary = "Get entity by ID", description = "Retrieves details for a specific entity from Wikidata")
    @GetMapping("/entities/{id}")
    public ResponseEntity<WikidataEntityDTO> getEntityById(
            @Parameter(description = "Wikidata entity ID", required = true) @PathVariable String id) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        if (!isValidWikidataId(id)) {
            throw new InvalidInputException("Invalid Wikidata ID format");
        }
        return ResponseEntity.ok(wikidataService.getEntityById(id));
    }

    @Operation(summary = "Get all properties", description = "Retrieves a paginated list of all properties from Wikidata")
    @GetMapping("/properties")
    public ResponseEntity<PaginatedResponse<WikidataPropertyDTO>> getAllProperties(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        return ResponseEntity.ok(wikidataService.getAllProperties(page, size));
    }

    @Operation(summary = "Search properties", description = "Searches for properties in Wikidata")
    @GetMapping("/properties/search")
    public ResponseEntity<PaginatedResponse<WikidataPropertyDTO>> searchProperties(
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        return ResponseEntity.ok(wikidataService.searchProperties(query, page, size));
    }

    @Operation(summary = "Get property by ID", description = "Retrieves details for a specific property from Wikidata")
    @GetMapping("/properties/{id}")
    public ResponseEntity<WikidataPropertyDTO> getPropertyById(
            @Parameter(description = "Wikidata property ID", required = true) @PathVariable String id) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
        if (!isValidWikidataId(id)) {
            throw new InvalidInputException("Invalid Wikidata ID format");
        }
        return ResponseEntity.ok(wikidataService.getPropertyById(id));
    }

    private boolean isValidWikidataId(String id) {
        // Wikidata entity IDs start with 'Q' followed by numbers
        // Wikidata property IDs start with 'P' followed by numbers
        return id != null && (id.matches("^Q\\d+$") || id.matches("^P\\d+$"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInputException(InvalidInputException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }
} 