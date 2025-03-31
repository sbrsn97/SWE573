package com.swe573.tests;

import com.swe573.dto.ThreadPreviewDTO;
import com.swe573.services.NlpService;
import com.swe573.services.impl.ThreadPreviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadPreviewServiceImplTest {

    @Mock
    private NlpService nlpService;

    @InjectMocks
    private ThreadPreviewServiceImpl threadPreviewServiceImpl;

    private String title;
    private String content;
    private Set<String> tags;
    private Set<String> followers;

    @BeforeEach
    void setUp() {
        title = "Test Thread";
        content = "This is a test thread content";
        tags = new HashSet<>();
        tags.add("java");
        followers = new HashSet<>();
        followers.add("testuser");
    }

    @Test
    void generatePreview_WithValidInput() {
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, content);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title + " " + content);
    }

    @Test
    void generatePreview_WithNullTitle() {
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(null, content);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(content);
    }

    @Test
    void generatePreview_WithNullContent() {
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, null);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title);
    }

    @Test
    void generatePreview_WithEmptyContent() {
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, "");

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title);
    }

    @Test
    void generatePreview_WithLongContent() {
        String longContent = "a".repeat(1000);
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, longContent);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title + " " + longContent);
    }

    @Test
    void generatePreview_WithSpecialCharacters() {
        String specialContent = "Test content with special chars: !@#$%^&*()";
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, specialContent);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title + " " + specialContent);
    }

    @Test
    void generatePreview_WithUnicodeCharacters() {
        String unicodeContent = "Test content with unicode: 你好世界";
        ThreadPreviewDTO preview = threadPreviewServiceImpl.generatePreview(title, unicodeContent);

        assertNotNull(preview);
        verify(nlpService).extractKeywords(title + " " + unicodeContent);
    }
} 