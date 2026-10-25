package com.example.optimization;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Monitoring & Observability Examples
 * 
 * Demonstrates various monitoring and observability techniques using
 * Spring Boot Actuator, Micrometer metrics, distributed tracing, and logging.
 */
@Configuration
public class MonitoringObservability {

    /**
     * Configure HTTP trace repository for request tracking
     */
    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    /**
     * Custom health indicator
     */
    @Bean
    public HealthIndicator customHealthIndicator() {
        return new CustomHealthIndicator();
    }

    /**
     * Custom health indicator implementation
     */
    public static class CustomHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            // Check application health
            boolean isHealthy = checkHealth();
            
            if (isHealthy) {
                return Health.up()
                        .withDetail("status", "All systems operational")
                        .withDetail("version", "1.0.0")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "System degraded")
                        .build();
            }
        }

        private boolean checkHealth() {
            // Implement actual health check logic
            return true;
        }
    }

    /**
     * Database health indicator
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(javax.sql.DataSource dataSource) {
        return new DatabaseHealthIndicator(dataSource);
    }

    public static class DatabaseHealthIndicator implements HealthIndicator {
        private final javax.sql.DataSource dataSource;

        public DatabaseHealthIndicator(javax.sql.DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Health health() {
            try (java.sql.Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    return Health.up()
                            .withDetail("database", "MySQL")
                            .withDetail("validation", "Passed")
                            .build();
                }
                return Health.down().withDetail("error", "Connection validation failed").build();
            } catch (Exception e) {
                return Health.down().withDetail("error", e.getMessage()).build();
            }
        }
    }

    /**
     * Redis health indicator
     */
    @Bean
    public HealthIndicator redisHealthIndicator(org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
        return new RedisHealthIndicator(connectionFactory);
    }

    public static class RedisHealthIndicator implements HealthIndicator {
        private final org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

        public RedisHealthIndicator(org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        @Override
        public Health health() {
            try {
                connectionFactory.getConnection().ping();
                return Health.up().withDetail("redis", "Connected").build();
            } catch (Exception e) {
                return Health.down().withDetail("error", e.getMessage()).build();
            }
        }
    }

    /**
     * Service with custom metrics
     */
    @Component
    public static class MetricsService {
        
        private final Counter requestCounter;
        private final Timer responseTimer;
        private final MeterRegistry meterRegistry;

        public MetricsService(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Create a counter for requests
            this.requestCounter = Counter.builder("api.requests")
                    .description("Total number of API requests")
                    .tag("type", "custom")
                    .register(meterRegistry);
            
            // Create a timer for response times
            this.responseTimer = Timer.builder("api.response.time")
                    .description("API response time")
                    .register(meterRegistry);
        }

        /**
         * Method with @Timed annotation
         */
        @Timed(value = "api.process.time", description = "Time taken to process request")
        public String processRequest() {
            requestCounter.increment();
            
            return responseTimer.record(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Processed";
            });
        }

        /**
         * Manual metric recording
         */
        public void recordCustomMetric(String operation, long duration) {
            Timer.builder("custom.operations")
                    .tag("operation", operation)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
        }

        /**
         * Gauge for monitoring values
         */
        public void monitorQueueSize(java.util.Queue<?> queue) {
            meterRegistry.gauge("queue.size", queue, java.util.Collection::size);
        }
    }

    /**
     * Service with performance monitoring
     */
    @Component
    public static class PerformanceMonitor {
        
        private final MeterRegistry meterRegistry;

        public PerformanceMonitor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        /**
         * Monitor JVM memory
         */
        public void monitorMemory() {
            Runtime runtime = Runtime.getRuntime();
            
            meterRegistry.gauge("jvm.memory.used", runtime, r -> 
                (r.totalMemory() - r.freeMemory()) / 1024 / 1024);
            
            meterRegistry.gauge("jvm.memory.max", runtime, r -> 
                r.maxMemory() / 1024 / 1024);
        }

        /**
         * Monitor thread count
         */
        public void monitorThreads() {
            meterRegistry.gauge("jvm.threads.count", Thread::activeCount);
        }

        /**
         * Monitor GC time
         */
        public void monitorGc() {
            java.lang.management.MemoryMXBean memoryMXBean = 
                java.lang.management.ManagementFactory.getMemoryMXBean();
            
            meterRegistry.gauge("jvm.gc.time", memoryMXBean, m -> 
                java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                    .stream()
                    .mapToLong(java.lang.management.GarbageCollectorMXBean::getCollectionTime)
                    .sum());
        }
    }

    /**
     * Structured logging with MDC
     */
    @Component
    public static class StructuredLogger {
        
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class);

        public void logRequest(String requestId, String userId, String endpoint) {
            org.slf4j.MDC.put("requestId", requestId);
            org.slf4j.MDC.put("userId", userId);
            org.slf4j.MDC.put("endpoint", endpoint);
            
            try {
                logger.info("Processing request");
            } finally {
                org.slf4j.MDC.clear();
            }
        }

        public void logError(String requestId, Exception ex) {
            org.slf4j.MDC.put("requestId", requestId);
            try {
                logger.error("Error occurred", ex);
            } finally {
                org.slf4j.MDC.clear();
            }
        }
    }

    /**
     * Async logging configuration
     */
    @Configuration
    public static class AsyncLoggingConfig {
        
        @Bean
        public ch.qos.logback.classic.LoggerContext loggerContext() {
            return (ch.qos.logback.classic.LoggerContext) 
                org.slf4j.LoggerFactory.getILoggerFactory();
        }
    }

    /**
     * Distributed tracing with Sleuth (if using Spring Cloud)
     */
    @Component
    public static class TracingService {
        
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(TracingService.class);

        public void tracedOperation(String operation) {
            // Sleuth automatically adds trace and span IDs to logs
            logger.info("Executing operation: {}", operation);
            
            // Simulate operation
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Custom metrics for business logic
     */
    @Component
    public static class BusinessMetrics {
        
        private final MeterRegistry meterRegistry;
        private final Counter orderCounter;
        private final Counter failedOrderCounter;

        public BusinessMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            this.orderCounter = Counter.builder("orders.total")
                    .description("Total number of orders")
                    .register(meterRegistry);
            
            this.failedOrderCounter = Counter.builder("orders.failed")
                    .description("Total number of failed orders")
                    .register(meterRegistry);
        }

        public void recordOrder(boolean success) {
            if (success) {
                orderCounter.increment();
            } else {
                failedOrderCounter.increment();
            }
        }

        public void recordOrderValue(double value) {
            meterRegistry.gauge("orders.value", value);
        }
    }

    /**
     * Alerting service
     */
    @Component
    public static class AlertingService {
        
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(AlertingService.class);

        public void checkAlerts(MeterRegistry meterRegistry) {
            // Check error rate
            double errorRate = meterRegistry.counter("http.server.requests", "status", "500").count();
            
            if (errorRate > 10) {
                logger.warn("High error rate detected: {}", errorRate);
                // Send alert (email, Slack, PagerDuty, etc.)
            }
            
            // Check response time
            Timer timer = meterRegistry.find("http.server.requests").timer();
            if (timer != null && timer.mean(TimeUnit.MILLISECONDS) > 1000) {
                logger.warn("High response time detected: {}ms", timer.mean(TimeUnit.MILLISECONDS));
                // Send alert
            }
        }
    }

    /**
     * Performance profiling service
     */
    @Component
    public static class ProfilingService {
        
        public void profileMethod(Runnable method) {
            long startTime = System.nanoTime();
            
            try {
                method.run();
            } finally {
                long duration = System.nanoTime() - startTime;
                System.out.printf("Method execution time: %.2f ms%n", duration / 1_000_000.0);
            }
        }
    }

    /**
     * Custom endpoint for application metrics
     */
    @org.springframework.boot.actuate.endpoint.annotation.Endpoint(id = "custommetrics")
    @Component
    public static class CustomMetricsEndpoint {
        
        @org.springframework.boot.actuate.endpoint.annotation.ReadOperation
        public java.util.Map<String, Object> customMetrics() {
            java.util.Map<String, Object> metrics = new java.util.HashMap<>();
            
            Runtime runtime = Runtime.getRuntime();
            metrics.put("memory.used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            metrics.put("memory.max", runtime.maxMemory() / 1024 / 1024);
            metrics.put("threads.active", Thread.activeCount());
            metrics.put("timestamp", System.currentTimeMillis());
            
            return metrics;
        }
    }

    /**
     * Log aggregation service
     */
    @Component
    public static class LogAggregator {
        
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(LogAggregator.class);

        public void logStructuredEvent(String eventType, java.util.Map<String, Object> data) {
            // Log in JSON format for easy parsing by log aggregators
            java.util.Map<String, Object> logEntry = new java.util.HashMap<>();
            logEntry.put("event", eventType);
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("data", data);
            
            logger.info("{}", logEntry);
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR MONITORING:
 * 
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,metrics,prometheus,httptrace,info,custommetrics
 *       base-path: /actuator
 *   endpoint:
 *     health:
 *       show-details: when-authorized
 *       show-components: always
 *     metrics:
 *       enabled: true
 *   metrics:
 *     export:
 *       prometheus:
 *         enabled: true
 *     tags:
 *       application: ${spring.application.name}
 *       environment: ${spring.profiles.active}
 *     distribution:
 *       percentiles-histogram:
 *         http.server.requests: true
 *       percentiles:
 *         http.server.requests: 0.5,0.95,0.99
 *   health:
 *     defaults:
 *       enabled: true
 * 
 * # Logging configuration
 * logging:
 *   pattern:
 *     console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
 *     file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
 *   level:
 *     root: INFO
 *     com.example.optimization: DEBUG
 *   file:
 *     name: logs/application.log
 *   logback:
 *     rollingpolicy:
 *       max-file-size: 10MB
 *       max-history: 30
 * 
 * # Distributed tracing (if using Spring Cloud Sleuth)
 * spring:
 *   sleuth:
 *     zipkin:
 *       base-url: http://localhost:9411
 *     sampler:
 *       probability: 0.1
 */
