package com.example.optimization;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caching Strategies Examples
 * 
 * Demonstrates various caching techniques using Spring Cache abstraction
 * with Caffeine (local cache) and Redis (distributed cache).
 */
@Configuration
@EnableCaching
public class CachingStrategies {

    /**
     * Configure Caffeine cache manager (local cache)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache configuration
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Service with caching annotations
     */
    @Service
    @CacheConfig(cacheNames = "users")
    public static class UserService {

        /**
         * Cacheable - caches the result of the method
         */
        @Cacheable(key = "#id")
        public User getUserById(Long id) {
            System.out.println("Fetching user from database for id: " + id);
            return fetchUserFromDatabase(id);
        }

        /**
         * Cacheable with custom key generator
         */
        @Cacheable(key = "#user.email")
        public User getUserByEmail(User user) {
            System.out.println("Fetching user from database for email: " + user.getEmail());
            return fetchUserFromDatabaseByEmail(user.getEmail());
        }

        /**
         * CachePut - updates the cache without affecting method execution
         */
        @CachePut(key = "#user.id")
        public User updateUser(User user) {
            System.out.println("Updating user in database: " + user.getId());
            return saveUserToDatabase(user);
        }

        /**
         * CacheEvict - removes entry from cache
         */
        @CacheEvict(key = "#id")
        public void deleteUser(Long id) {
            System.out.println("Deleting user from database: " + id);
            deleteUserFromDatabase(id);
        }

        /**
         * CacheEvict - removes all entries from cache
         */
        @CacheEvict(allEntries = true)
        public void clearAllUsersCache() {
            System.out.println("Clearing all users cache");
        }

        /**
         * Caching - multiple cache operations
         */
        @Caching(
            cacheable = {
                @Cacheable(key = "#id", cacheNames = "users"),
                @Cacheable(key = "'user_' + #id", cacheNames = "userDetails")
            }
        )
        public User getUserWithMultipleCaches(Long id) {
            System.out.println("Fetching user with multiple caches for id: " + id);
            return fetchUserFromDatabase(id);
        }

        /**
         * Cacheable with condition
         */
        @Cacheable(key = "#id", condition = "#id > 0")
        public User getUserByIdWithCondition(Long id) {
            System.out.println("Fetching user from database with condition for id: " + id);
            return fetchUserFromDatabase(id);
        }

        /**
         * Cacheable with unless (spares caching based on result)
         */
        @Cacheable(key = "#id", unless = "#result == null")
        public User getUserByIdUnlessNull(Long id) {
            System.out.println("Fetching user from database with unless for id: " + id);
            return fetchUserFromDatabase(id);
        }

        // Simulated database operations
        private User fetchUserFromDatabase(Long id) {
            User user = new User();
            user.setId(id);
            user.setName("User " + id);
            user.setEmail("user" + id + "@example.com");
            return user;
        }

        private User fetchUserFromDatabaseByEmail(String email) {
            User user = new User();
            user.setId(1L);
            user.setName("User");
            user.setEmail(email);
            return user;
        }

        private User saveUserToDatabase(User user) {
            return user;
        }

        private void deleteUserFromDatabase(Long id) {
            // Delete operation
        }
    }

    /**
     * Custom key generator
     */
    @org.springframework.stereotype.Component("customKeyGenerator")
    public static class CustomKeyGenerator implements org.springframework.cache.interceptor.KeyGenerator {
        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());
            sb.append(".");
            for (Object param : params) {
                sb.append(param.toString());
                sb.append(".");
            }
            return sb.toString();
        }
    }

    /**
     * Cache-aside pattern implementation
     */
    public static class CacheAsidePattern<K, V> {
        private final java.util.Map<K, V> cache = new java.util.HashMap<>();
        private final Function<K, V> dbLoader;

        public CacheAsidePattern(Function<K, V> dbLoader) {
            this.dbLoader = dbLoader;
        }

        public V get(K key) {
            // Check cache first
            V value = cache.get(key);
            if (value != null) {
                System.out.println("Cache hit for key: " + key);
                return value;
            }

            // Cache miss - load from database
            System.out.println("Cache miss for key: " + key);
            value = dbLoader.apply(key);
            
            // Update cache
            if (value != null) {
                cache.put(key, value);
            }

            return value;
        }

        public void put(K key, V value) {
            // Update database
            dbLoader.apply(key); // Simulate DB update
            
            // Update cache
            cache.put(key, value);
        }

        public void evict(K key) {
            cache.remove(key);
        }

        public void evictAll() {
            cache.clear();
        }
    }

    /**
     * Multi-level caching (L1: local, L2: distributed)
     */
    public static class MultiLevelCache<K, V> {
        private final java.util.Map<K, V> l1Cache = new java.util.HashMap<>();
        private final java.util.Map<K, V> l2Cache = new java.util.HashMap<>();
        private final Function<K, V> dbLoader;

        public MultiLevelCache(Function<K, V> dbLoader) {
            this.dbLoader = dbLoader;
        }

        public V get(K key) {
            // Check L1 cache first
            V value = l1Cache.get(key);
            if (value != null) {
                System.out.println("L1 cache hit for key: " + key);
                return value;
            }

            // Check L2 cache
            value = l2Cache.get(key);
            if (value != null) {
                System.out.println("L2 cache hit for key: " + key);
                l1Cache.put(key, value); // Promote to L1
                return value;
            }

            // Load from database
            System.out.println("Cache miss for key: " + key);
            value = dbLoader.apply(key);
            
            if (value != null) {
                l1Cache.put(key, value);
                l2Cache.put(key, value);
            }

            return value;
        }

        public void evict(K key) {
            l1Cache.remove(key);
            l2Cache.remove(key);
        }

        public void evictAll() {
            l1Cache.clear();
            l2Cache.clear();
        }
    }

    /**
     * User entity
     */
    public static class User {
        private Long id;
        private String name;
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * Cache warming example
     */
    @Service
    public static class CacheWarmupService {
        
        @org.springframework.cache.annotation.Cacheable(cacheNames = "users")
        public User getUser(Long id) {
            return new User();
        }

        /**
         * Warm up cache on application startup
         */
        @org.springframework.boot.context.event.ApplicationReadyEvent
        public void warmupCache() {
            System.out.println("Warming up cache...");
            // Pre-load frequently accessed data
            for (Long id = 1L; id <= 100L; id++) {
                getUser(id);
            }
            System.out.println("Cache warmup complete");
        }
    }

    /**
     * Cache statistics monitoring
     */
    @Service
    public static class CacheStatisticsService {
        
        @org.springframework.beans.factory.annotation.Autowired
        private CacheManager cacheManager;

        public void printCacheStatistics() {
            if (cacheManager instanceof CaffeineCacheManager) {
                CaffeineCacheManager caffeineManager = (CaffeineCacheManager) cacheManager;
                com.github.benmanes.caffeine.cache.Cache<Object, Object> cache = 
                    caffeineManager.getCache("users").getNativeCache();
                
                com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
                System.out.println("Cache Statistics:");
                System.out.println("  Hit Rate: " + stats.hitRate());
                System.out.println("  Hit Count: " + stats.hitCount());
                System.out.println("  Miss Count: " + stats.missCount());
                System.out.println("  Eviction Count: " + stats.evictionCount());
            }
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR CACHING:
 * 
 * spring:
 *   cache:
 *     type: caffeine
 *     caffeine:
 *       spec: maximumSize=1000,expireAfterWrite=10m,recordStats
 *     cache-names: users, userDetails, products
 * 
 * # For Redis distributed cache:
 * spring:
 *   cache:
 *     type: redis
 *   redis:
 *     host: localhost
 *     port: 6379
 *     timeout: 60000
 *     lettuce:
 *       pool:
 *         max-active: 8
 *         max-idle: 8
 *         min-idle: 0
 */
