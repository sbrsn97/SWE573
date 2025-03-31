package com.swe573.tests;

import com.swe573.services.NlpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.ArrayList;
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
public class StanfordNlpServicePerformanceTest {

    @Autowired
    private NlpService nlpService;

    private static final int CONCURRENT_REQUESTS = 3;
    private static final int TIMEOUT_SECONDS = 10;
    private static final List<String> TEST_TEXTS = List.of(
        "Artificial intelligence and machine learning are transforming the way we develop software.",
        "The new iPhone features advanced camera technology and improved system performance.",
        "Software engineering best practices include code review, testing, and continuous integration."
    );

    @Test
    public void measureKeywordExtractionPerformance() throws Exception {
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
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent requests
        for (String text : TEST_TEXTS) {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    return nlpService.extractKeywords(text);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<String>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average keyword extraction duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Text " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 1000, "Average keyword extraction should take less than 1 second");
    }

    @Test
    public void measureNamedEntityExtractionPerformance() throws Exception {
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
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent requests
        for (String text : TEST_TEXTS) {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    return nlpService.extractNamedEntities(text);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<String>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average named entity extraction duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Text " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 1000, "Average named entity extraction should take less than 1 second");
    }

    @Test
    public void measureTopicAnalysisPerformance() throws Exception {
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
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent requests
        for (String text : TEST_TEXTS) {
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    return nlpService.analyzeTopics(text);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<String>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average topic analysis duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Text " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 1500, "Average topic analysis should take less than 1.5 seconds");
    }
} 