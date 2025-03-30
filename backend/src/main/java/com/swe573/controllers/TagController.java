package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.TagDTO;
import com.swe573.models.Tag;
import com.swe573.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @PostMapping
    public ResponseEntity<ApiResponse<Tag>> createTag(@RequestBody TagDTO tagDTO) {
        Tag tag = tagService.createTag(tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag created successfully", tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tag>> getTag(@PathVariable Long id) {
        Tag tag = tagService.getTag(id);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    @GetMapping("/label/{label}")
    public ResponseEntity<ApiResponse<Tag>> getTagByLabel(@PathVariable String label) {
        Tag tag = tagService.getTagByLabel(label);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    @GetMapping("/wikidata/{wikidataEntityId}")
    public ResponseEntity<ApiResponse<Tag>> getTagByWikidataEntityId(@PathVariable String wikidataEntityId) {
        Tag tag = tagService.getTagByWikidataEntityId(wikidataEntityId);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Tag>>> searchTags(@RequestParam String keyword) {
        List<Tag> tags = tagService.searchTags(keyword);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Tag>> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.updateTag(id, tagDTO);
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", tag));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted successfully", null));
    }
} 