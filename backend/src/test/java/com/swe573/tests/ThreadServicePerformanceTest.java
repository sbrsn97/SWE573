package com.swe573.tests;

import com.swe573.models.Thread;
import com.swe573.dto.ThreadDTO;
import com.swe573.services.ThreadService;
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
public class ThreadServicePerformanceTest {

    @Autowired
    private ThreadService threadService;

    private static final int CONCURRENT_REQUESTS = 5;
    private static final int TIMEOUT_SECONDS = 10;
    private static final List<String> TEST_TITLES = List.of(
        "Best practices for software development",
        "Latest trends in artificial intelligence",
        "Mobile app development challenges",
        "Cloud computing solutions",
        "Web security best practices"
    );

    @Test
    public void measureThreadCreationPerformance() throws Exception {
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
        List<CompletableFuture<Thread>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent thread creation requests
        for (String title : TEST_TITLES) {
            CompletableFuture<Thread> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    ThreadDTO threadDTO = new ThreadDTO();
                    threadDTO.setTitle(title);
                    threadDTO.setDescription("This is a test thread about " + title);
                    threadDTO.setAuthorId(1L); // Using a test user ID
                    return threadService.createThread(threadDTO);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<Thread> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average thread creation duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Thread " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 500, "Average thread creation should take less than 500ms");
    }

    @Test
    public void measureThreadSearchPerformance() throws Exception {
        // Create some test threads first
        List<Thread> testThreads = new ArrayList<>();
        for (String title : TEST_TITLES) {
            ThreadDTO threadDTO = new ThreadDTO();
            threadDTO.setTitle(title);
            threadDTO.setDescription("This is a test thread about " + title);
            threadDTO.setAuthorId(1L); // Using a test user ID
            testThreads.add(threadService.createThread(threadDTO));
        }

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
        List<CompletableFuture<List<Thread>>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent search requests
        for (String title : TEST_TITLES) {
            CompletableFuture<List<Thread>> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    return threadService.searchThreads(title);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<List<Thread>> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average thread search duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Search " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 200, "Average thread search should take less than 200ms");
    }

    @Test
    public void measureThreadRetrievalPerformance() throws Exception {
        // Create a test thread first
        ThreadDTO threadDTO = new ThreadDTO();
        threadDTO.setTitle("Test Thread");
        threadDTO.setDescription("This is a test thread");
        threadDTO.setAuthorId(1L); // Using a test user ID
        Thread testThread = threadService.createThread(threadDTO);

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
        List<CompletableFuture<Thread>> futures = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        // Start concurrent retrieval requests
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            CompletableFuture<Thread> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    return threadService.getThread(testThread.getId());
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    durations.add(duration);
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        for (CompletableFuture<Thread> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        // Calculate and print performance metrics
        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average thread retrieval duration: " + avgDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.size(); i++) {
            System.out.println("Retrieval " + i + ": " + durations.get(i) + "ms");
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
        assertTrue(avgDuration < 100, "Average thread retrieval should take less than 100ms");
    }

    @Test
    public void measureCachingPerformance() throws Exception {
        // Create multiple test threads with complex content
        for (int i = 0; i < 10; i++) {
            ThreadDTO threadDTO = new ThreadDTO();
            threadDTO.setTitle("Complex Test Thread " + i);
            threadDTO.setDescription("This is a complex test thread about software development, artificial intelligence, and cloud computing. " +
                "It contains many technical terms and concepts that will require significant processing to search through. " +
                "The content includes discussions about microservices architecture, containerization, and distributed systems. " +
                "We also cover topics like machine learning, neural networks, and deep learning frameworks. " +
                "Additionally, we discuss cloud platforms, serverless computing, and edge computing solutions.");
            threadDTO.setAuthorId(1L); // Using a test user ID
            threadService.createThread(threadDTO);
        }
        
        // First request (should be slow)
        long startTime = System.currentTimeMillis();
        var firstResponse = threadService.searchThreads("software development artificial intelligence cloud computing microservices");
        long firstDuration = System.currentTimeMillis() - startTime;
        
        // Second request (should be fast due to caching)
        startTime = System.currentTimeMillis();
        var secondResponse = threadService.searchThreads("software development artificial intelligence cloud computing microservices");
        long secondDuration = System.currentTimeMillis() - startTime;
        
        // Calculate improvement
        double improvement = ((double) (firstDuration - secondDuration) / firstDuration) * 100;
        
        System.out.println("\nCaching Performance:");
        System.out.println("First request duration: " + firstDuration + "ms");
        System.out.println("Second request duration: " + secondDuration + "ms");
        System.out.println("Cache improvement: " + String.format("%.2f", improvement) + "%");
        
        // Assert caching is working
        assertTrue(secondDuration < firstDuration, "Cached request should be faster than first request");
        assertTrue(improvement > 0, "Cache improvement should be positive");
    }
} 