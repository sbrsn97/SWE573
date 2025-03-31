package com.swe573.tests;

import com.swe573.services.WikidataService;
import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class WikidataServicePerformanceTest {

    @Autowired
    private WikidataService wikidataService;

    private static final int CONCURRENT_REQUESTS = 5;
    private static final int TIMEOUT_SECONDS = 5;
    private static final List<String> TEST_QUERIES = List.of(
        "computer science",
        "artificial intelligence",
        "machine learning",
        "data science",
        "software engineering"
    );

    @Test
    public void measureConcurrentEntitySearch() throws Exception {
        // Record initial memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("\nMemory Usage at Start:");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (heapUsage.getMax() / 1024 / 1024) + "MB");
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (nonHeapUsage.getMax() / 1024 / 1024) + "MB");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<CompletableFuture<List<WikidataEntityDTO>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent requests
        for (String query : TEST_QUERIES) {
            CompletableFuture<List<WikidataEntityDTO>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    var response = wikidataService.searchEntities(query, 0, 5);
                    return response != null && response.getItems() != null ? response.getItems() : new ArrayList<>();
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<WikidataEntityDTO>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average request duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Query " + i + ": " + durations.get(i) + "ms");
        }

        // Record final memory usage
        heapUsage = memoryBean.getHeapMemoryUsage();
        nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("\nMemory Usage at End:");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (heapUsage.getMax() / 1024 / 1024) + "MB");
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (nonHeapUsage.getMax() / 1024 / 1024) + "MB");

        // Assert performance requirements
        assertTrue(avgDuration < 2000, "Average request duration should be less than 2 seconds");
    }

    @Test
    public void measureConcurrentPropertySearch() throws Exception {
        // Record initial memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("\nMemory Usage at Start:");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (heapUsage.getMax() / 1024 / 1024) + "MB");
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (nonHeapUsage.getMax() / 1024 / 1024) + "MB");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<CompletableFuture<List<WikidataPropertyDTO>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent requests
        for (String query : TEST_QUERIES) {
            CompletableFuture<List<WikidataPropertyDTO>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    var response = wikidataService.searchProperties(query, 0, 5);
                    return response != null && response.getItems() != null ? response.getItems() : new ArrayList<>();
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<WikidataPropertyDTO>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average request duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Query " + i + ": " + durations.get(i) + "ms");
        }

        // Record final memory usage
        heapUsage = memoryBean.getHeapMemoryUsage();
        nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("\nMemory Usage at End:");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (heapUsage.getMax() / 1024 / 1024) + "MB");
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (nonHeapUsage.getMax() / 1024 / 1024) + "MB");

        // Assert performance requirements
        assertTrue(avgDuration < 2000, "Average request duration should be less than 2 seconds");
    }
} 