package com.swe573.tests;

import com.swe573.dto.ThreadPreviewDTO;
import com.swe573.services.ThreadPreviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ThreadPreviewPerformanceTest {

    @Autowired
    private ThreadPreviewService threadPreviewService;

    private static final String TEST_TITLE = "Java Spring Boot Application";
    private static final String TEST_CONTENT = "Building a modern web application using Spring Boot, " +
            "RESTful APIs, and microservices architecture. The application uses Docker containers " +
            "and Kubernetes for deployment. It follows cloud-native principles and implements " +
            "best practices for scalability and maintainability.";

    private MemoryMXBean memoryMXBean;

    @BeforeEach
    void setUp() {
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    private void logMemoryUsage(String stage) {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        System.out.println("\nMemory Usage at " + stage + ":");
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (heapUsage.getMax() / 1024 / 1024) + "MB");
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsage.getUsed() / 1024 / 1024) + "MB");
        System.out.println("  Max: " + (nonHeapUsage.getMax() / 1024 / 1024) + "MB");
    }

    @Test
    void measurePreviewGenerationTime() {
        // Log initial memory usage
        logMemoryUsage("Start");

        // First call (no cache)
        long startTime = System.nanoTime();
        ThreadPreviewDTO preview = threadPreviewService.generatePreview(TEST_TITLE, TEST_CONTENT);
        long endTime = System.nanoTime();
        long firstCallDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Log memory usage after first call
        logMemoryUsage("After First Call");

        // Second call (should use cache)
        startTime = System.nanoTime();
        ThreadPreviewDTO cachedPreview = threadPreviewService.generatePreview(TEST_TITLE, TEST_CONTENT);
        endTime = System.nanoTime();
        long secondCallDuration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Log memory usage after second call
        logMemoryUsage("After Second Call");

        // Verify results
        assertNotNull(preview);
        assertNotNull(cachedPreview);
        assertEquals(preview.getKeywords(), cachedPreview.getKeywords());
        assertEquals(preview.getSuggestedEntities(), cachedPreview.getSuggestedEntities());
        assertEquals(preview.getSuggestedProperties(), cachedPreview.getSuggestedProperties());

        // Log performance metrics
        System.out.println("\nPerformance Metrics:");
        System.out.println("First call duration: " + firstCallDuration + "ms");
        System.out.println("Second call duration: " + secondCallDuration + "ms");
        System.out.println("Cache improvement: " + 
            ((double)(firstCallDuration - secondCallDuration) / firstCallDuration * 100) + "%");

        // Assert performance requirements
        assertTrue(firstCallDuration < 5000, "First call should take less than 5 seconds");
        assertTrue(secondCallDuration < 100, "Cached call should take less than 100ms");
    }

    @Test
    void measureConcurrentPreviewGeneration() throws InterruptedException {
        // Log initial memory usage
        logMemoryUsage("Start");

        int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        long[] durations = new long[numThreads];

        // Create and start threads
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                long startTime = System.nanoTime();
                ThreadPreviewDTO preview = threadPreviewService.generatePreview(TEST_TITLE, TEST_CONTENT);
                long endTime = System.nanoTime();
                durations[index] = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Log memory usage after concurrent calls
        logMemoryUsage("After Concurrent Calls");

        // Calculate average duration
        long totalDuration = 0;
        for (long duration : durations) {
            totalDuration += duration;
        }
        long averageDuration = totalDuration / numThreads;

        // Log performance metrics
        System.out.println("\nPerformance Metrics:");
        System.out.println("Average concurrent call duration: " + averageDuration + "ms");
        System.out.println("Individual durations:");
        for (int i = 0; i < durations.length; i++) {
            System.out.println("Thread " + i + ": " + durations[i] + "ms");
        }

        // Assert performance requirements
        assertTrue(averageDuration < 5000, "Average concurrent call should take less than 5 seconds");
    }

    @Test
    void measurePreviewGenerationWithDifferentContent() {
        // Log initial memory usage
        logMemoryUsage("Start");

        String[] contents = {
            "Short content about Java.",
            "Medium content about Spring Boot and REST APIs.",
            "Long content about microservices architecture, Docker containers, Kubernetes orchestration, " +
            "cloud computing, and various other technical concepts that should be processed by the NLP service."
        };

        for (String content : contents) {
            long startTime = System.nanoTime();
            ThreadPreviewDTO preview = threadPreviewService.generatePreview(TEST_TITLE, content);
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            assertNotNull(preview);
            System.out.println("\nContent Analysis:");
            System.out.println("Content length: " + content.length() + " characters");
            System.out.println("Processing duration: " + duration + "ms");

            // Log memory usage after each content processing
            logMemoryUsage("After Processing " + content.length() + " chars");

            // Assert performance requirements
            assertTrue(duration < 5000, "Processing should take less than 5 seconds");
        }
    }
} 