package com.swe573.services.impl;

import com.swe573.dto.NodePreviewDTO;
import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.services.NodePreviewService;
import com.swe573.services.WikidataService;
import com.swe573.services.NlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class NodePreviewServiceImpl implements NodePreviewService {

    @Autowired
    private WikidataService wikidataService;

    @Autowired
    private NlpService nlpService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private static final int BATCH_SIZE = 5;
    private static final int TIMEOUT_SECONDS = 3;

    @Override
    @Cacheable(value = "nodePreviews", key = "'preview-' + #label + '-' + #description", unless = "#result == null")
    public NodePreviewDTO generatePreview(String label, String description) {
        // Create empty preview for null or empty inputs
        if ((label == null || label.trim().isEmpty()) && 
            (description == null || description.trim().isEmpty())) {
            return createEmptyPreview();
        }

        // Combine label and description for analysis, handling nulls
        String combinedText = String.join(" ", 
            label != null ? label : "", 
            description != null ? description : "").trim();

        // Extract keywords using NLP
        List<String> keywords = nlpService.extractKeywords(combinedText);

        // Filter out null or empty keywords and limit to top 10
        keywords = keywords.stream()
            .filter(k -> k != null && !k.trim().isEmpty())
            .limit(10)
            .collect(Collectors.toList());

        // Process keywords in batches
        List<WikidataEntityDTO> suggestedEntities = new ArrayList<>();
        List<WikidataPropertyDTO> suggestedProperties = new ArrayList<>();

        // Direct search using the label itself if provided
        if (label != null && !label.trim().isEmpty()) {
            try {
                var directLabelResults = wikidataService.searchEntities(label.trim(), 0, 5);
                if (directLabelResults != null && directLabelResults.getItems() != null) {
                    suggestedEntities.addAll(directLabelResults.getItems());
                }
            } catch (Exception e) {
                // Log but continue
                System.err.println("Error searching entities by label: " + e.getMessage());
            }
        }

        for (int i = 0; i < keywords.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, keywords.size());
            List<String> batch = keywords.subList(i, end);

            // Create futures for parallel processing of Wikidata queries
            CompletableFuture<List<WikidataEntityDTO>> entityFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    var entityResponse = wikidataService.searchEntities(String.join(" ", batch), 0, 5);
                    return entityResponse != null && entityResponse.getItems() != null ? entityResponse.getItems() : new ArrayList<WikidataEntityDTO>();
                } catch (Exception e) {
                    return new ArrayList<WikidataEntityDTO>();
                }
            }, executorService).orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<List<WikidataPropertyDTO>> propertyFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    var propertyResponse = wikidataService.searchProperties(String.join(" ", batch), 0, 5);
                    return propertyResponse != null && propertyResponse.getItems() != null ? propertyResponse.getItems() : new ArrayList<WikidataPropertyDTO>();
                } catch (Exception e) {
                    return new ArrayList<WikidataPropertyDTO>();
                }
            }, executorService).orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            try {
                // Wait for both futures to complete with timeout
                List<WikidataEntityDTO> batchEntities = entityFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                List<WikidataPropertyDTO> batchProperties = propertyFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Add results to the final lists
                suggestedEntities.addAll(batchEntities);
                suggestedProperties.addAll(batchProperties);
            } catch (Exception e) {
                // Log error and continue with next batch
                continue;
            }
        }

        // Remove duplicates and limit results
        suggestedEntities = suggestedEntities.stream()
            .filter(distinctByKey(WikidataEntityDTO::getId))
            .limit(10)
            .collect(Collectors.toList());

        suggestedProperties = suggestedProperties.stream()
            .filter(distinctByKey(WikidataPropertyDTO::getId))
            .limit(10)
            .collect(Collectors.toList());

        // Create and return the preview
        NodePreviewDTO preview = new NodePreviewDTO();
        preview.setKeywords(keywords);
        preview.setSuggestedEntities(suggestedEntities);
        preview.setSuggestedProperties(suggestedProperties);
        return preview;
    }

    private NodePreviewDTO createEmptyPreview() {
        NodePreviewDTO preview = new NodePreviewDTO();
        preview.setKeywords(new ArrayList<>());
        preview.setSuggestedEntities(new ArrayList<>());
        preview.setSuggestedProperties(new ArrayList<>());
        return preview;
    }

    private <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
} 