package com.swe573.services;

import com.swe573.dto.TagDTO;
import com.swe573.models.Tag;
import java.util.List;

public interface TagService {
    Tag createTag(TagDTO tagDTO);
    Tag getTag(Long id);
    Tag getTagByLabel(String label);
    Tag getTagByWikidataEntityId(String wikidataEntityId);
    List<Tag> getAllTags();
    List<Tag> searchTags(String keyword);
    Tag updateTag(Long id, TagDTO tagDTO);
    void deleteTag(Long id);
    boolean existsByLabel(String label);
    boolean existsByWikidataEntityId(String wikidataEntityId);
} 