package com.example.optimization;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.concurrent.TimeUnit;

/**
 * Connection Pooling Examples
 * 
 * Demonstrates various connection pooling strategies for HTTP clients,
 * databases, Redis, and message queues to optimize resource usage.
 */
@Configuration
public class ConnectionPooling {

    /**
     * Configure Apache HttpClient with connection pooling
     */
    @Bean
    public CloseableHttpClient httpClient() {
        // Connection pool configuration
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        
        // Max total connections
        connectionManager.setMaxTotal(200);
        
        // Max connections per route (e.g., per host)
        connectionManager.setDefaultMaxPerRoute(100);
        
        // Request configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000) // 5 seconds
                .setSocketTimeout(30000) // 30 seconds
                .setConnectionRequestTimeout(5000) // 5 seconds
                .build();
        
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(60, TimeUnit.SECONDS)
                .evictExpiredConnections()
                .build();
    }

    /**
     * Configure RestTemplate with connection pooling
     */
    @Bean
    public RestTemplate restTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }

    /**
     * Configure HikariCP for database connection pooling
     */
    @Bean
    public javax.sql.DataSource dataSource() {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("user");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool size calculation: (core_count * 2) + effective_spindle_count
        int coreCount = Runtime.getRuntime().availableProcessors();
        config.setMaximumPoolSize((coreCount * 2) + 1);
        config.setMinimumIdle(coreCount);
        
        // Connection timeout
        config.setConnectionTimeout(20000); // 20 seconds
        
        // Idle timeout
        config.setIdleTimeout(600000); // 10 minutes
        
        // Max lifetime
        config.setMaxLifetime(1800000); // 30 minutes
        
        // Pool name for monitoring
        config.setPoolName("DatabaseHikariCP");
        
        // Leak detection
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * Configure Redis connection factory with Lettuce pooling
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LettucePoolingClientConfiguration poolConfig = LettucePoolingClientConfiguration.builder()
                .maxActive(8)
                .maxIdle(8)
                .minIdle(0)
                .build();
        
        return new LettuceConnectionFactory(
            new org.springframework.data.redis.connection.RedisStandaloneConfiguration("localhost", 6379),
            poolConfig
        );
    }

    /**
     * Configure RabbitMQ connection factory with pooling
     */
    @Bean
    public org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory() {
        org.springframework.amqp.rabbit.connection.CachingConnectionFactory connectionFactory = 
            new org.springframework.amqp.rabbit.connection.CachingConnectionFactory();
        
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        
        // Channel cache mode
        connectionFactory.setChannelCacheSize(25);
        connectionFactory.setCacheMode(
            org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode.CHANNEL
        );
        
        // Connection cache mode (for multiple connections)
        // connectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CONNECTION);
        // connectionFactory.setConnectionCacheSize(5);
        
        return connectionFactory;
    }

    /**
     * HTTP connection pool monitoring
     */
    @org.springframework.stereotype.Service
    public static class ConnectionPoolMonitor {
        
        @org.springframework.beans.factory.annotation.Autowired
        private CloseableHttpClient httpClient;

        public void printHttpClientPoolStats() {
            if (httpClient != null) {
                PoolingHttpClientConnectionManager manager = 
                    (PoolingHttpClientConnectionManager) httpClient.getConnectionManager();
                
                System.out.println("HTTP Connection Pool Stats:");
                System.out.println("  Max Total: " + manager.getMaxTotal());
                System.out.println("  Leased: " + manager.getTotalStats().getLeased());
                System.out.println("  Pending: " + manager.getTotalStats().getPending());
                System.out.println("  Available: " + manager.getTotalStats().getAvailable());
            }
        }
    }

    /**
     * Database connection pool monitoring
     */
    @org.springframework.stereotype.Service
    public static class DatabasePoolMonitor {
        
        @org.springframework.beans.factory.annotation.Autowired
        private javax.sql.DataSource dataSource;

        public void printDatabasePoolStats() {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDS = 
                    (com.zaxxer.hikari.HikariDataSource) dataSource;
                com.zaxxer.hikari.HikariPoolMXBean poolProxy = hikariDS.getHikariPoolMXBean();
                
                System.out.println("Database Connection Pool Stats:");
                System.out.println("  Active Connections: " + poolProxy.getActiveConnections());
                System.out.println("  Idle Connections: " + poolProxy.getIdleConnections());
                System.out.println("  Total Connections: " + poolProxy.getTotalConnections());
                System.out.println("  Threads Awaiting Connection: " + poolProxy.getThreadsAwaitingConnection());
            }
        }
    }

    /**
     * Custom connection pool for generic resources
     */
    public static class GenericConnectionPool<T> {
        private final java.util.concurrent.BlockingQueue<T> pool;
        private final java.util.function.Supplier<T> connectionFactory;
        private final int maxSize;

        public GenericConnectionPool(java.util.function.Supplier<T> connectionFactory, int maxSize) {
            this.connectionFactory = connectionFactory;
            this.maxSize = maxSize;
            this.pool = new java.util.concurrent.LinkedBlockingQueue<>(maxSize);
        }

        public T borrowConnection() throws InterruptedException {
            T connection = pool.poll();
            if (connection == null) {
                connection = connectionFactory.get();
            }
            return connection;
        }

        public void returnConnection(T connection) {
            if (pool.size() < maxSize) {
                pool.offer(connection);
            }
        }

        public void closeAll() {
            pool.clear();
        }

        public int getAvailableCount() {
            return pool.size();
        }
    }

    /**
     * Example: Custom FTP connection pool
     */
    public static class FtpConnectionPool {
        private final GenericConnectionPool<Object> pool;

        public FtpConnectionPool(String host, int port, int maxSize) {
            this.pool = new GenericConnectionPool<>(
                () -> createFtpConnection(host, port),
                maxSize
            );
        }

        private Object createFtpConnection(String host, int port) {
            // Simulate FTP connection creation
            return new Object(); // Replace with actual FTP connection
        }

        public Object borrowConnection() throws InterruptedException {
            return pool.borrowConnection();
        }

        public void returnConnection(Object connection) {
            pool.returnConnection(connection);
        }
    }

    /**
     * Connection pool health check
     */
    @org.springframework.stereotype.Service
    public static class ConnectionPoolHealthCheck {
        
        @org.springframework.beans.factory.annotation.Autowired
        private javax.sql.DataSource dataSource;

        @org.springframework.beans.factory.annotation.Autowired
        private RedisConnectionFactory redisConnectionFactory;

        public boolean isDatabasePoolHealthy() {
            try (java.sql.Connection conn = dataSource.getConnection()) {
                return conn.isValid(5); // 5 second timeout
            } catch (Exception e) {
                return false;
            }
        }

        public boolean isRedisPoolHealthy() {
            try {
                redisConnectionFactory.getConnection().close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public void printHealthStatus() {
            System.out.println("Connection Pool Health Status:");
            System.out.println("  Database: " + (isDatabasePoolHealthy() ? "HEALTHY" : "UNHEALTHY"));
            System.out.println("  Redis: " + (isRedisPoolHealthy() ? "HEALTHY" : "UNHEALTHY"));
        }
    }

    /**
     * Dynamic connection pool sizing
     */
    @org.springframework.stereotype.Service
    public static class DynamicPoolSizer {
        
        private final com.zaxxer.hikari.HikariDataSource dataSource;
        
        public DynamicPoolSizer(com.zaxxer.hikari.HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }

        public void adjustPoolSizeBasedOnLoad(double cpuUsage) {
            int currentMax = dataSource.getMaximumPoolSize();
            int newMax;

            if (cpuUsage > 0.8) {
                // High CPU - reduce pool size
                newMax = Math.max(currentMax / 2, 5);
            } else if (cpuUsage < 0.5) {
                // Low CPU - increase pool size
                newMax = Math.min(currentMax * 2, 50);
            } else {
                // Normal CPU - keep current size
                return;
            }

            System.out.println("Adjusting pool size from " + currentMax + " to " + newMax);
            dataSource.setMaximumPoolSize(newMax);
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR CONNECTION POOLING:
 * 
 * # Database (HikariCP)
 * spring:
 *   datasource:
 *     hikari:
 *       maximum-pool-size: 20
 *       minimum-idle: 10
 *       connection-timeout: 20000
 *       idle-timeout: 600000
 *       max-lifetime: 1800000
 *       leak-detection-threshold: 60000
 *       pool-name: DatabaseHikariCP
 * 
 * # Redis (Lettuce)
 * spring:
 *   redis:
 *     host: localhost
 *     port: 6379
 *     timeout: 60000
 *     lettuce:
 *       pool:
 *         max-active: 8
 *         max-idle: 8
 *         min-idle: 0
 * 
 * # RabbitMQ
 * spring:
 *   rabbitmq:
 *     host: localhost
 *     port: 5672
 *     username: guest
 *     password: guest
 *     cache:
 *       channel:
 *         size: 25
 *       connection:
 *         mode: channel
 * 
 * # HTTP Client
 * http:
 *   client:
 *     max-total: 200
 *     max-per-route: 100
 *     connect-timeout: 5000
 *     socket-timeout: 30000
 */
