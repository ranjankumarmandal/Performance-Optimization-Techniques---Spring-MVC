package com.example.optimization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.concurrent.TimeUnit;

/**
 * Security Performance Optimization Examples
 * 
 * Demonstrates various techniques to optimize Spring Security
 * including stateless authentication, JWT, caching, and proper configuration.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityPerformance extends WebSecurityConfigurerAdapter {

    /**
     * Configure password encoder (BCrypt is recommended)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure UserDetailsService with caching
     */
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        
        // Create users with BCrypt encoded passwords
        manager.createUser(User.withUsername("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build());
        
        manager.createUser(User.withUsername("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN")
                .build());
        
        return manager;
    }

    /**
     * Configure authentication manager
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * Configure HTTP security for REST APIs (stateless)
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless APIs
            .csrf().disable()
            
            // Use stateless session management for REST APIs
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Configure authorization rules
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            .and()
            
            // Add JWT filter if using JWT authentication
            // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Configure HTTP security headers
            .headers()
                .frameOptions().deny()
                .xssProtection().block(true)
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
            .and()
            
            // Configure CORS
            .cors();
    }

    /**
     * JWT Authentication Filter (stateless authentication)
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * JWT Authentication Filter implementation
     */
    public static class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
        
        @Override
        protected org.springframework.security.core.Authentication attemptAuthentication(
                javax.servlet.http.HttpServletRequest request,
                javax.servlet.http.HttpServletResponse response) {
            
            // Extract JWT token from header
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // Validate token and create authentication
                // This is a simplified example
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = 
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user", null, java.util.Collections.emptyList());
                
                return auth;
            }
            
            return null;
        }
    }

    /**
     * Cached UserDetailsService for performance
     */
    @org.springframework.stereotype.Service
    public static class CachedUserDetailsService implements UserDetailsService {
        
        private final UserDetailsService delegate;
        private final java.util.Map<String, org.springframework.security.core.userdetails.UserDetails> cache = 
            new java.util.concurrent.ConcurrentHashMap<>();

        public CachedUserDetailsService(UserDetailsService delegate) {
            this.delegate = delegate;
        }

        @Override
        public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) {
            return cache.computeIfAbsent(username, key -> {
                System.out.println("Loading user from database: " + username);
                return delegate.loadUserByUsername(username);
            });
        }

        public void evictUser(String username) {
            cache.remove(username);
        }

        public void evictAll() {
            cache.clear();
        }
    }

    /**
     * JWT Token Service
     */
    @org.springframework.stereotype.Service
    public static class JwtTokenService {
        
        private final String secretKey = "my-secret-key";
        private final long tokenValidity = TimeUnit.HOURS.toMillis(1); // 1 hour
        private final long refreshTokenValidity = TimeUnit.DAYS.toMillis(7); // 7 days

        /**
         * Generate access token
         */
        public String generateAccessToken(String username) {
            // In production, use a proper JWT library like jjwt
            return "jwt-access-token-" + username + "-" + System.currentTimeMillis();
        }

        /**
         * Generate refresh token
         */
        public String generateRefreshToken(String username) {
            return "jwt-refresh-token-" + username + "-" + System.currentTimeMillis();
        }

        /**
         * Validate token
         */
        public boolean validateToken(String token) {
            // Implement proper JWT validation
            return token != null && token.startsWith("jwt-");
        }

        /**
         * Extract username from token
         */
        public String extractUsername(String token) {
            // Implement proper JWT parsing
            if (token.contains("-")) {
                String[] parts = token.split("-");
                return parts.length > 2 ? parts[2] : null;
            }
            return null;
        }
    }

    /**
     * Method-level security examples
     */
    @org.springframework.stereotype.Service
    public static class SecureService {

        /**
         * PreAuthorize - check before method execution
         */
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        public void adminOnlyMethod() {
            System.out.println("Admin method executed");
        }

        /**
         * PreAuthorize with custom expression
         */
        @org.springframework.security.access.prepost.PreAuthorize("#username == authentication.name")
        public void userSpecificMethod(String username) {
            System.out.println("User specific method executed for: " + username);
        }

        /**
         * PostAuthorize - check after method execution
         */
        @org.springframework.security.access.prepost.PostAuthorize("returnObject.owner == authentication.name")
        public Document getDocument(Long id) {
            return new Document(id, "user");
        }

        /**
         * Secured annotation (simpler)
         */
        @org.springframework.security.access.annotation.Secured("ROLE_USER")
        public void userMethod() {
            System.out.println("User method executed");
        }
    }

    /**
     * Document class for PostAuthorize example
     */
    public static class Document {
        private Long id;
        private String owner;

        public Document(Long id, String owner) {
            this.id = id;
            this.owner = owner;
        }

        public Long getId() { return id; }
        public String getOwner() { return owner; }
    }

    /**
     * Security context caching for async operations
     */
    @org.springframework.stereotype.Service
    public static class AsyncSecurityService {
        
        @org.springframework.scheduling.annotation.Async
        @org.springframework.security.core.context.SecurityContext
        public void asyncMethodWithContext() {
            // Security context is automatically propagated to async threads
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        }
    }

    /**
     * Rate limiting for security
     */
    @org.springframework.stereotype.Component
    public static class SecurityRateLimiter {
        
        private final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> requestCounts = 
            new java.util.concurrent.ConcurrentHashMap<>();
        private final int maxRequests = 100;
        private final long windowMillis = TimeUnit.MINUTES.toMillis(1);

        public boolean allowRequest(String clientId) {
            java.util.concurrent.atomic.AtomicInteger counter = requestCounts.computeIfAbsent(
                clientId, k -> new java.util.concurrent.atomic.AtomicInteger(0));
            
            int count = counter.incrementAndGet();
            
            if (count > maxRequests) {
                return false;
            }
            
            // Reset counter after window (simplified)
            if (count == 1) {
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
                    () -> counter.set(0), windowMillis, TimeUnit.MILLISECONDS);
            }
            
            return true;
        }
    }

    /**
     * Password hashing optimization
     */
    @org.springframework.stereotype.Service
    public static class PasswordService {
        
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10); // Lower strength for performance

        /**
         * Hash password with BCrypt
         */
        public String hashPassword(String rawPassword) {
            return passwordEncoder.encode(rawPassword);
        }

        /**
         * Check password
         */
        public boolean checkPassword(String rawPassword, String encodedPassword) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }

        /**
         * For high-performance scenarios, consider using Argon2 or SCrypt
         * but they may be slower than BCrypt
         */
    }

    /**
     * Security event logging
     */
    @org.springframework.stereotype.Service
    public static class SecurityAuditService {
        
        private final java.util.List<SecurityEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

        public void logEvent(String type, String username, String details) {
            SecurityEvent event = new SecurityEvent(type, username, details, System.currentTimeMillis());
            events.add(event);
            System.out.println("Security Event: " + type + " - User: " + username);
        }

        public java.util.List<SecurityEvent> getEvents() {
            return java.util.Collections.unmodifiableList(events);
        }
    }

    public static class SecurityEvent {
        private String type;
        private String username;
        private String details;
        private long timestamp;

        public SecurityEvent(String type, String username, String details, long timestamp) {
            this.type = type;
            this.username = username;
            this.details = details;
            this.timestamp = timestamp;
        }

        public String getType() { return type; }
        public String getUsername() { return username; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * HTTPS/TLS configuration (for production)
     */
    @Configuration
    public static class HttpsConfig {
        
        @Bean
        public org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory 
                servletContainer() {
            
            org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory tomcat = 
                new org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory();
            
            tomcat.addAdditionalTomcatConnectors(redirectConnector());
            return tomcat;
        }

        private org.apache.catalina.connector.Connector redirectConnector() {
            org.apache.catalina.connector.Connector connector = 
                new org.apache.catalina.connector.Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setScheme("http");
            connector.setPort(8080);
            connector.setSecure(false);
            connector.setRedirectPort(8443);
            return connector;
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR SECURITY OPTIMIZATION:
 * 
 * spring:
 *   security:
 *     user:
 *       name: user
 *       password: password
 *       roles: USER
 * 
 * # For JWT configuration:
 * jwt:
 *   secret: my-secret-key
 *   expiration: 3600000
 *   refresh-expiration: 604800000
 * 
 * # For HTTPS:
 * server:
 *   port: 8443
 *   ssl:
 *     enabled: true
 *     key-store: classpath:keystore.p12
 *     key-store-password: password
 *     key-store-type: PKCS12
 *     key-alias: tomcat
 * 
 * # Session management (if using sessions)
 * server:
 *   servlet:
 *     session:
 *       timeout: 1800
 *       tracking-modes: cookie
 *       cookie:
 *         http-only: true
 *         secure: true
 *         same-site: strict
 */
