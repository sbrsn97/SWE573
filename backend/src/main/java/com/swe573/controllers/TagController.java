package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.TagDTO;
import com.swe573.models.Tag;
import com.swe573.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @PostMapping
    public ResponseEntity<ApiResponse<TagDTO>> createTag(@RequestBody TagDTO tagDTO) {
        Tag tag = tagService.createTag(tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag created successfully", convertToDTO(tag)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> getTag(@PathVariable Long id) {
        Tag tag = tagService.getTag(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

    @GetMapping("/label/{label}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagByLabel(@PathVariable String label) {
        Tag tag = tagService.getTagByLabel(label);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

    @GetMapping("/wikidata/{wikidataEntityId}")
    public ResponseEntity<ApiResponse<TagDTO>> getTagByWikidataEntityId(@PathVariable String wikidataEntityId) {
        Tag tag = tagService.getTagByWikidataEntityId(wikidataEntityId);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(tag)));
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDTO>> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.updateTag(id, tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", convertToDTO(tag)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
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