package com.example.optimization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Asynchronous Processing Examples
 * 
 * Demonstrates various asynchronous processing techniques in Spring
 * including @Async, CompletableFuture, scheduling, and reactive programming.
 */
@Configuration
@EnableAsync
public class AsyncProcessing {

    /**
     * Configure async task executor with optimal settings
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size = number of CPU cores
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        
        // Max pool size = 2x core pool size
        executor.setMaxPoolSize(corePoolSize * 2);
        
        // Queue capacity
        executor.setQueueCapacity(1000);
        
        // Thread name prefix for easy debugging
        executor.setThreadNamePrefix("Async-");
        
        // Reuse threads
        executor.setKeepAliveSeconds(60);
        
        // Policy when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * Configure separate executor for I/O-bound tasks
     */
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("IO-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Service with async methods
     */
    @Service
    public static class AsyncService {

        /**
         * Simple async method
         */
        @Async("taskExecutor")
        public void asyncMethod() {
            System.out.println("Async method running in thread: " + Thread.currentThread().getName());
            // Simulate long-running task
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Async method with return value using CompletableFuture
         */
        @Async("taskExecutor")
        public CompletableFuture<String> asyncMethodWithReturn() {
            System.out.println("Async method with return running in thread: " + 
                Thread.currentThread().getName());
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Task completed";
            });
        }

        /**
         * Multiple async tasks running in parallel
         */
        @Async("taskExecutor")
        public CompletableFuture<String> task1() {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Task 1 running");
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                return "Task 1 result";
            });
        }

        @Async("taskExecutor")
        public CompletableFuture<String> task2() {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Task 2 running");
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                return "Task 2 result";
            });
        }

        @Async("taskExecutor")
        public CompletableFuture<String> task3() {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Task 3 running");
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                return "Task 3 result";
            });
        }

        /**
         * Combine multiple async tasks
         */
        public CompletableFuture<Void> executeAllTasks() {
            CompletableFuture<String> f1 = task1();
            CompletableFuture<String> f2 = task2();
            CompletableFuture<String> f3 = task3();

            // Wait for all to complete
            return CompletableFuture.allOf(f1, f2, f3)
                .thenAccept(v -> {
                    System.out.println("All tasks completed");
                    System.out.println("Task 1: " + f1.join());
                    System.out.println("Task 2: " + f2.join());
                    System.out.println("Task 3: " + f3.join());
                });
        }

        /**
         * Async method with exception handling
         */
        @Async("taskExecutor")
        public CompletableFuture<String> asyncMethodWithException() {
            return CompletableFuture.supplyAsync(() -> {
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Random error occurred");
                }
                return "Success";
            }).exceptionally(ex -> {
                System.err.println("Error in async method: " + ex.getMessage());
                return "Fallback value";
            });
        }

        /**
         * Async method for I/O operations
         */
        @Async("ioExecutor")
        public CompletableFuture<String> asyncIoOperation(String url) {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Fetching data from: " + url);
                // Simulate HTTP call
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Data from " + url;
            });
        }
    }

    /**
     * Scheduled task examples
     */
    @Service
    public static class ScheduledService {

        /**
         * Fixed rate - runs at fixed intervals
         */
        @Scheduled(fixedRate = 5000)
        public void fixedRateTask() {
            System.out.println("Fixed rate task running at: " + System.currentTimeMillis());
        }

        /**
         * Fixed delay - runs after previous completion + delay
         */
        @Scheduled(fixedDelay = 5000)
        public void fixedDelayTask() {
            System.out.println("Fixed delay task running at: " + System.currentTimeMillis());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Cron expression - runs at specific times
         */
        @Scheduled(cron = "0 0 * * * *") // Every hour
        public void cronTask() {
            System.out.println("Cron task running at: " + System.currentTimeMillis());
        }

        /**
         * Initial delay before first execution
         */
        @Scheduled(fixedRate = 10000, initialDelay = 5000)
        public void taskWithInitialDelay() {
            System.out.println("Task with initial delay running at: " + System.currentTimeMillis());
        }
    }

    /**
     * Event-driven async processing
     */
    @Service
    public static class EventService {

        @org.springframework.context.event.EventListener
        @Async("taskExecutor")
        public void handleCustomEvent(CustomEvent event) {
            System.out.println("Handling event asynchronously: " + event.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @org.springframework.context.event.EventListener
        @Async("taskExecutor")
        public void handleAnotherEvent(AnotherEvent event) {
            System.out.println("Handling another event asynchronously: " + event.getData());
        }
    }

    /**
     * Custom event classes
     */
    public static class CustomEvent {
        private String message;

        public CustomEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class AnotherEvent {
        private String data;

        public AnotherEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * CompletableFuture composition examples
     */
    @Service
    public static class CompletableFutureService {

        /**
         * Chain async operations
         */
        public CompletableFuture<String> chainOperations() {
            return CompletableFuture.supplyAsync(() -> "Step 1")
                .thenApply(result -> result + " -> Step 2")
                .thenApply(result -> result + " -> Step 3")
                .thenApply(result -> result + " -> Complete");
        }

        /**
         * Combine results from two futures
         */
        public CompletableFuture<String> combineFutures() {
            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

            return future1.thenCombine(future2, (s1, s2) -> s1 + " " + s2);
        }

        /**
         * Accept result from first completed future
         */
        public CompletableFuture<String> acceptEither() {
            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                return "Fast";
            });

            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                return "Slow";
            });

            return future1.applyToEither(future2, result -> result + " won!");
        }

        /**
         * Handle exceptions in CompletableFuture chain
         */
        public CompletableFuture<String> withExceptionHandling() {
            return CompletableFuture.supplyAsync(() -> {
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Error");
                }
                return "Success";
            }).handle((result, ex) -> {
                if (ex != null) {
                    return "Recovered from error: " + ex.getMessage();
                }
                return result;
            });
        }
    }

    /**
     * Reactive programming with WebFlux (if using reactive stack)
     */
    @Service
    public static class ReactiveService {

        /**
         * Simple reactive stream
         */
        public reactor.core.publisher.Flux<String> reactiveStream() {
            return reactor.core.publisher.Flux.just("Item 1", "Item 2", "Item 3")
                .map(item -> item.toUpperCase())
                .delayElements(java.time.Duration.ofMillis(500));
        }

        /**
         * Reactive with error handling
         */
        public reactor.core.publisher.Mono<String> reactiveWithErrorHandling() {
            return reactor.core.publisher.Mono.error(new RuntimeException("Error"))
                .onErrorResume(ex -> reactor.core.publisher.Mono.just("Fallback"));
        }

        /**
         * Parallel reactive processing
         */
        public reactor.core.publisher.Flux<String> parallelProcessing() {
            return reactor.core.publisher.Flux.range(1, 10)
                .parallel()
                .runOn(reactor.core.scheduler.Schedulers.parallel())
                .map(i -> "Processed " + i)
                .sequential();
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR ASYNC PROCESSING:
 * 
 * spring:
 *   task:
 *     execution:
 *       pool:
 *         core-size: 8
 *         max-size: 16
 *         queue-capacity: 1000
 *         keep-alive: 60s
 *       thread-name-prefix: async-
 *     scheduling:
 *       pool:
 *         size: 5
 * 
 * # For reactive applications:
 * spring:
 *   main:
 *     web-application-type: reactive
 */
