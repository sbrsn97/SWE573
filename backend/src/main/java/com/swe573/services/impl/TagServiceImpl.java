package com.swe573.services.impl;

import com.swe573.dto.TagDTO;
import com.swe573.models.Tag;
import com.swe573.repositories.TagRepository;
import com.swe573.services.TagService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagRepository tagRepository;

    @Override
    @Transactional
    public Tag createTag(TagDTO tagDTO) {
        if (tagRepository.existsByLabel(tagDTO.getLabel())) {
            throw new IllegalArgumentException("Tag with this label already exists");
        }

        // We're removing Wikidata entity ID uniqueness validation to allow creating tags with the same or null Wikidata ID
        // This allows tags created without a selected Wikidata entity to work properly

        Tag tag = new Tag();
        tag.setLabel(tagDTO.getLabel());
        tag.setDescription(tagDTO.getDescription());
        tag.setWikidataEntityId(tagDTO.getWikidataEntityId());
        tag.setColorCodeString(tagDTO.getColorCodeString());

        return tagRepository.save(tag);
    }

    @Override
    public Tag getTag(Long id) {
        return tagRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found"));
    }

    @Override
    public Tag getTagByLabel(String label) {
        return tagRepository.findByLabel(label)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found"));
    }

    @Override
    public Tag getTagByWikidataEntityId(String wikidataEntityId) {
        return tagRepository.findByWikidataEntityId(wikidataEntityId)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found"));
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Override
    public List<Tag> searchTags(String keyword) {
        return tagRepository.searchTags(keyword);
    }

    @Override
    @Transactional
    public Tag updateTag(Long id, TagDTO tagDTO) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found"));

        // Check if new label conflicts with existing tag (excluding current tag)
        if (!tag.getLabel().equals(tagDTO.getLabel()) && 
            tagRepository.existsByLabel(tagDTO.getLabel())) {
            throw new IllegalArgumentException("Tag with this label already exists");
        }

        // We're removing Wikidata entity ID uniqueness validation to be consistent with the create method
        // This allows tags to be updated with the same Wikidata entity ID

        tag.setLabel(tagDTO.getLabel());
        tag.setDescription(tagDTO.getDescription());
        tag.setWikidataEntityId(tagDTO.getWikidataEntityId());
        tag.setColorCodeString(tagDTO.getColorCodeString());

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Tag not found"));
        tagRepository.delete(tag);
    }

    @Override
    public boolean existsByLabel(String label) {
        return tagRepository.existsByLabel(label);
    }

    @Override
    public boolean existsByWikidataEntityId(String wikidataEntityId) {
        return tagRepository.existsByWikidataEntityId(wikidataEntityId);
    }
} 