package com.swe573.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaginatedResponse<T> {
    private List<T> items;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private int pageSize;
} 