package com.example.optimization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Spring MVC Performance Optimization Examples
 * 
 * Demonstrates various techniques to optimize Spring MVC controllers
 * and web layer performance.
 */
@Configuration
@EnableAsync
public class SpringMvcPerformanceOptimization implements WebMvcConfigurer {

    /**
     * Configure async task executor for async controllers
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
            new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    /**
     * Configure interceptors - order matters for performance
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PerformanceInterceptor())
                .addPathPatterns("/**")
                .order(1);
        
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/metrics")
                .order(2);
    }

    /**
     * Optimized Controller examples
     */
    @Controller
    @RequestMapping("/api")
    @Validated
    public static class OptimizedController {

        /**
         * Use specific HTTP method annotations
         * Return DTOs instead of entities
         * Use proper HTTP status codes
         */
        @GetMapping("/users/{id}")
        public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
            UserDto user = userService().findById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        }

        /**
         * Use @PostMapping with @Valid for validation
         * Return created status with location header
         */
        @PostMapping("/users")
        public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserDto userDto) {
            UserDto created = userService().create(userDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/users/" + created.getId())
                    .body(created);
        }

        /**
         * Asynchronous controller for long-running operations
         */
        @GetMapping("/async/users/{id}")
        @Async("taskExecutor")
        public CompletableFuture<ResponseEntity<UserDto>> getUserAsync(@PathVariable Long id) {
            return CompletableFuture.supplyAsync(() -> {
                UserDto user = userService().findById(id);
                return ResponseEntity.ok(user);
            });
        }

        /**
         * Use DeferredResult for more control over async processing
         */
        @GetMapping("/deferred/users/{id}")
        public DeferredResult<ResponseEntity<UserDto>> getUserDeferred(@PathVariable Long id) {
            DeferredResult<ResponseEntity<UserDto>> deferredResult = 
                new DeferredResult<>(30000L); // 30 second timeout
            
            // Process asynchronously
            CompletableFuture.supplyAsync(() -> userService().findById(id))
                    .thenAccept(user -> deferredResult.setResult(ResponseEntity.ok(user)))
                    .exceptionally(ex -> {
                        deferredResult.setErrorResult(ex);
                        return null;
                    });
            
            return deferredResult;
        }

        /**
         * Content negotiation with produces/consumes
         */
        @GetMapping(value = "/users/{id}", produces = {"application/json", "application/xml"})
        public ResponseEntity<UserDto> getUserWithContentNegotiation(
                @PathVariable Long id,
                @RequestHeader("Accept") String acceptHeader) {
            UserDto user = userService().findById(id);
            return ResponseEntity.ok(user);
        }

        /**
         * Pagination for large datasets
         */
        @GetMapping("/users")
        public ResponseEntity<org.springframework.data.domain.Page<UserDto>> getUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "20") int size) {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<UserDto> users = userService().findAll(pageable);
            return ResponseEntity.ok(users);
        }

        private UserService userService() {
            return new UserService();
        }
    }

    /**
     * Global exception handler
     */
    @org.springframework.web.bind.annotation.RestControllerAdvice
    public static class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
            ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
            ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Supporting classes
    static class PerformanceInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        @Override
        public boolean preHandle(javax.servlet.http.HttpServletRequest request, 
                                javax.servlet.http.HttpServletResponse response, 
                                Object handler) {
            request.setAttribute("startTime", System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(javax.servlet.http.HttpServletRequest request, 
                                    javax.servlet.http.HttpServletResponse response, 
                                    Object handler, Exception ex) {
            long startTime = (Long) request.getAttribute("startTime");
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Request to " + request.getRequestURI() + " took " + duration + "ms");
        }
    }

    static class LoggingInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        @Override
        public boolean preHandle(javax.servlet.http.HttpServletRequest request, 
                                javax.servlet.http.HttpServletResponse response, 
                                Object handler) {
            System.out.println("Incoming request: " + request.getMethod() + " " + request.getRequestURI());
            return true;
        }
    }

    // DTOs and Service classes
    static class UserDto {
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

    static class CreateUserDto {
        private String name;
        private String email;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    static class ErrorResponse {
        private String code;
        private String message;
        
        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    static class UserService {
        public UserDto findById(Long id) {
            UserDto user = new UserDto();
            user.setId(id);
            user.setName("Test User");
            user.setEmail("test@example.com");
            return user;
        }
        
        public UserDto create(CreateUserDto dto) {
            UserDto user = new UserDto();
            user.setId(1L);
            user.setName(dto.getName());
            user.setEmail(dto.getEmail());
            return user;
        }
        
        public org.springframework.data.domain.Page<UserDto> findAll(
                org.springframework.data.domain.Pageable pageable) {
            return org.springframework.data.domain.Page.empty();
        }
    }

    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR MVC OPTIMIZATION:
 * 
 * spring:
 *   mvc:
 *     async:
 *       request-timeout: 30000
 *   resources:
 *     cache:
 *       period: 3600
 *     chain:
 *       cache: true
 * server:
 *   compression:
 *     enabled: true
 *     mime-types: application/json,application/xml,text/html,text/xml,text/plain
 *     min-response-size: 1024
 *   tomcat:
 *     threads:
 *       max: 200
 *       min-spare: 10
 *     max-connections: 8192
 *     accept-count: 100
 */
