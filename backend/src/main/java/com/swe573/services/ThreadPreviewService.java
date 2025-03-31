package com.swe573.services;

import com.swe573.dto.ThreadPreviewDTO;

public interface ThreadPreviewService {
    ThreadPreviewDTO generatePreview(String title, String content);
} 