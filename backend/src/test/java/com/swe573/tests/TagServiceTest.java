package com.swe573.tests;

import com.swe573.models.Tag;
import com.swe573.repositories.TagRepository;
import com.swe573.services.impl.TagServiceImpl;
import com.swe573.dto.TagDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;
    private TagDTO testTagDTO;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setLabel("testtag");
        testTag.setWikidataEntityId("Q123");
        testTag.setDescription("Test Description");

        testTagDTO = new TagDTO();
        testTagDTO.setId(1L);
        testTagDTO.setLabel("testtag");
        testTagDTO.setWikidataEntityId("Q123");
        testTagDTO.setDescription("Test Description");
    }

    @Test
    void createTag_Success() {
        // Arrange
        when(tagRepository.existsByLabel("testtag")).thenReturn(false);
        when(tagRepository.existsByWikidataEntityId("Q123")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        Tag result = tagService.createTag(testTagDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testTag.getLabel(), result.getLabel());
        assertEquals(testTag.getWikidataEntityId(), result.getWikidataEntityId());
        assertEquals(testTag.getDescription(), result.getDescription());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createTag_DuplicateLabel() {
        // Arrange
        when(tagRepository.existsByLabel("testtag")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tagService.createTag(testTagDTO));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getTag_Success() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        // Act
        Tag result = tagService.getTag(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testTag.getId(), result.getId());
        assertEquals(testTag.getLabel(), result.getLabel());
    }

    @Test
    void getTag_NotFound() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> tagService.getTag(1L));
    }

    @Test
    void getTagByLabel_Success() {
        // Arrange
        when(tagRepository.findByLabel("testtag")).thenReturn(Optional.of(testTag));

        // Act
        Tag result = tagService.getTagByLabel("testtag");

        // Assert
        assertNotNull(result);
        assertEquals(testTag.getLabel(), result.getLabel());
    }

    @Test
    void getTagByLabel_NotFound() {
        // Arrange
        when(tagRepository.findByLabel("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> tagService.getTagByLabel("nonexistent"));
    }

    @Test
    void getTagByWikidataId_Success() {
        // Arrange
        when(tagRepository.findByWikidataEntityId("Q123")).thenReturn(Optional.of(testTag));

        // Act
        Tag result = tagService.getTagByWikidataEntityId("Q123");

        // Assert
        assertNotNull(result);
        assertEquals(testTag.getWikidataEntityId(), result.getWikidataEntityId());
    }

    @Test
    void getTagByWikidataId_NotFound() {
        // Arrange
        when(tagRepository.findByWikidataEntityId("Q999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> tagService.getTagByWikidataEntityId("Q999"));
    }

    @Test
    void updateTag_Success() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        Tag result = tagService.updateTag(1L, testTagDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testTag.getLabel(), result.getLabel());
        assertEquals(testTag.getWikidataEntityId(), result.getWikidataEntityId());
        assertEquals(testTag.getDescription(), result.getDescription());
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void updateTag_NotFound() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> tagService.updateTag(1L, testTagDTO));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void deleteTag_Success() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        // Act
        tagService.deleteTag(1L);

        // Assert
        verify(tagRepository).delete(testTag);
    }

    @Test
    void deleteTag_NotFound() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> tagService.deleteTag(1L));
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    void searchTags_Success() {
        // Arrange
        List<Tag> expectedTags = Collections.singletonList(testTag);
        when(tagRepository.searchTags("test")).thenReturn(expectedTags);

        // Act
        List<Tag> result = tagService.searchTags("test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTag, result.get(0));
    }

    @Test
    void getAllTags_Success() {
        // Arrange
        List<Tag> expectedTags = Collections.singletonList(testTag);
        when(tagRepository.findAll()).thenReturn(expectedTags);

        // Act
        List<Tag> result = tagService.getAllTags();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTag, result.get(0));
    }
} 