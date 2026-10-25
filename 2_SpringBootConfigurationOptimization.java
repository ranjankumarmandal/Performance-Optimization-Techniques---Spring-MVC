package com.example.optimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Spring Boot Configuration Optimization Examples
 * 
 * Demonstrates various Spring Boot configuration optimization techniques
 * to improve startup time and runtime performance.
 */
@SpringBootApplication(
    // Exclude unnecessary auto-configurations
    exclude = {DataSourceAutoConfiguration.class}
)
// Limit component scan to specific packages
@ComponentScan(
    basePackages = {"com.example.optimization"},
    // Exclude specific packages from scanning
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.example\\.optimization\\.legacy\\..*"
        )
    }
)
public class SpringBootConfigurationOptimization {

    /**
     * Lazy initialization example
     * Beans are initialized only when needed, reducing startup time
     */
    @Bean
    @org.springframework.context.annotation.Lazy
    public ExpensiveService expensiveService() {
        return new ExpensiveService();
    }

    /**
     * Conditional bean creation based on properties
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "feature.enabled",
        havingValue = "true"
    )
    public FeatureService featureService() {
        return new FeatureService();
    }

    /**
     * Primary bean for disambiguation
     */
    @Bean
    @Primary
    public CacheService primaryCacheService() {
        return new RedisCacheService();
    }

    /**
     * Profile-specific bean configuration
     */
    @Bean
    @Profile("production")
    public ProductionCacheService productionCacheService() {
        return new ProductionCacheService();
    }

    @Bean
    @Profile("development")
    public DevCacheService devCacheService() {
        return new DevCacheService();
    }

    /**
     * Custom SpringApplication builder for additional optimizations
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringBootConfigurationOptimization.class);
        
        // Disable banner for faster startup
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        
        // Disable web application type if not needed
        // app.setWebApplicationType(WebApplicationType.NONE);
        
        // Set additional properties
        app.setDefaultProperties(java.util.Map.of(
            "spring.main.lazy-initialization", "true",
            "spring.main.allow-bean-definition-overriding", "false",
            "spring.main.register-shutdown-hook", "false"
        ));
        
        Environment env = app.run(args).getEnvironment();
        System.out.println("Application started with profile: " + 
            java.util.Arrays.toString(env.getActiveProfiles()));
    }

    // Example service classes
    static class ExpensiveService {
        public ExpensiveService() {
            System.out.println("ExpensiveService initialized");
        }
    }

    static class FeatureService {
        public FeatureService() {
            System.out.println("FeatureService initialized");
        }
    }

    interface CacheService {}
    static class RedisCacheService implements CacheService {
        public RedisCacheService() {
            System.out.println("RedisCacheService initialized");
        }
    }
    static class ProductionCacheService implements CacheService {
        public ProductionCacheService() {
            System.out.println("ProductionCacheService initialized");
        }
    }
    static class DevCacheService implements CacheService {
        public DevCacheService() {
            System.out.println("DevCacheService initialized");
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR OPTIMIZATION:
 * 
 * spring:
 *   main:
 *     lazy-initialization: true
 *     allow-bean-definition-overriding: false
 *     register-shutdown-hook: false
 *   autoconfigure:
 *     exclude: 
 *       - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
 *       - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
 *   jmx:
 *     enabled: false
 * 
 * # Banner
 * spring:
 *   main:
 *     banner-mode: off
 * 
 * # Actuator - disable unused endpoints
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         exclude: '*'
 * 
 * # Logging - reduce noise
 * logging:
 *   level:
 *     root: WARN
 *     org.springframework: WARN
 */

/*
 * PROFILE-SPECIFIC CONFIGURATION:
 * 
 * application-prod.yml:
 * spring:
 *   profiles:
 *     active: prod
 *   jpa:
 *     show-sql: false
 *   thymeleaf:
 *     cache: true
 * 
 * application-dev.yml:
 * spring:
 *   profiles:
 *     active: dev
 *   jpa:
 *     show-sql: true
 *   thymeleaf:
 *     cache: false
 */
