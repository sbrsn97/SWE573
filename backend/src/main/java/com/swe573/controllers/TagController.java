package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.TagDTO;
import com.swe573.models.Tag;
import com.swe573.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.stream.Collectors;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "APIs for managing thread tags")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @Operation(summary = "Create tag", description = "Creates a new tag")
    @PostMapping
    public ResponseEntity<ApiResponse<TagDTO>> createTag(
            @Parameter(description = "Tag data", required = true) @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.createTag(tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag created successfully", convertToDTO(tag)));
    }

    @Operation(summary = "Get tag by ID", description = "Retrieves a specific tag by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> getTag(
            @Parameter(description = "ID of the tag to retrieve", required = true) @PathVariable Long id) {
        Tag tag = tagService.getTag(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

    @Operation(summary = "Get tag by label", description = "Retrieves a tag by its label")
    @GetMapping("/label/{label}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagByLabel(
            @Parameter(description = "Label of the tag to retrieve", required = true) @PathVariable String label) {
        Tag tag = tagService.getTagByLabel(label);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

    @GetMapping("/wikidata/{wikidataEntityId}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagByWikidataEntityId(@PathVariable String wikidataEntityId) {
        Tag tag = tagService.getTagByWikidataEntityId(wikidataEntityId);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

    @Operation(summary = "Get all tags", description = "Retrieves a list of all tags")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagDTO>>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        List<TagDTO> dtos = tags.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TagDTO>>> searchTags(@RequestParam String keyword) {
        List<Tag> tags = tagService.searchTags(keyword);
        List<TagDTO> dtos = tags.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Update tag", description = "Updates an existing tag")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> updateTag(
            @Parameter(description = "ID of the tag to update", required = true) @PathVariable Long id,
            @Parameter(description = "Updated tag data", required = true) @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.updateTag(id, tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", convertToDTO(tag)));
    }

    @Operation(summary = "Delete tag", description = "Deletes a tag")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(
            @Parameter(description = "ID of the tag to delete", required = true) @PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted successfully", null));
    }

    private TagDTO convertToDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setLabel(tag.getLabel());
        dto.setDescription(tag.getDescription());
        dto.setWikidataEntityId(tag.getWikidataEntityId());
        dto.setColorCodeString(tag.getColorCodeString());
        return dto;
    }
} 