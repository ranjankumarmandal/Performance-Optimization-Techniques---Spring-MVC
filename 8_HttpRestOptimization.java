package com.example.optimization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP & REST Optimization Examples
 * 
 * Demonstrates various techniques to optimize HTTP clients and REST APIs
 * including compression, HTTP/2, keep-alive, JSON optimization, and reactive clients.
 */
@Configuration
public class HttpRestOptimization implements WebMvcConfigurer {

    /**
     * Configure RestTemplate with connection pooling and timeouts
     */
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        // Connection timeout
        factory.setConnectTimeout(5000);
        
        // Read timeout
        factory.setReadTimeout(30000);
        
        // Connection request timeout
        factory.setConnectionRequestTimeout(5000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Configure Jackson for JSON optimization
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        restTemplate.setMessageConverters(List.of(converter));
        
        return restTemplate;
    }

    /**
     * Configure RestTemplate with builder pattern
     */
    @Bean
    public RestTemplate optimizedRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Configure WebClient for reactive HTTP (Spring 5+)
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://api.example.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * Configure Jackson for JSON optimization
     */
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        // Don't include null values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Don't write dates as timestamps
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Ignore unknown properties
        mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        return mapper;
    }

    /**
     * Optimized REST Controller
     */
    @RestController
    @RequestMapping("/api")
    public static class OptimizedRestController {

        /**
         * Use specific HTTP method annotations
         * Return proper HTTP status codes
         * Use DTOs instead of entities
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
         * Use @PostMapping with @Valid
         * Return 201 Created with location header
         */
        @PostMapping("/users")
        public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto userDto) {
            UserDto created = userService().create(userDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/api/users/" + created.getId())
                    .body(created);
        }

        /**
         * Content negotiation with produces/consumes
         */
        @GetMapping(value = "/users/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
        public ResponseEntity<UserDto> getUserWithContentNegotiation(@PathVariable Long id) {
            UserDto user = userService().findById(id);
            return ResponseEntity.ok(user);
        }

        /**
         * Pagination for large datasets
         */
        @GetMapping("/users")
        public ResponseEntity<Page<UserDto>> getUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "20") int size) {
            Pageable pageable = new Pageable(page, size);
            Page<UserDto> users = userService().findAll(pageable);
            return ResponseEntity.ok(users);
        }

        /**
         * Field selection/projection to reduce payload size
         */
        @GetMapping("/users/{id}/fields")
        public ResponseEntity<UserDto> getUserWithFields(
                @PathVariable Long id,
                @RequestParam(required = false) List<String> fields) {
            UserDto user = userService().findById(id);
            if (fields != null && !fields.isEmpty()) {
                user = filterFields(user, fields);
            }
            return ResponseEntity.ok(user);
        }

        /**
         * Use @JsonView for partial serialization
         */
        @GetMapping("/users/{id}/public")
        @JsonView(PublicView.class)
        public ResponseEntity<UserDto> getPublicUser(@PathVariable Long id) {
            UserDto user = userService().findById(id);
            return ResponseEntity.ok(user);
        }

        /**
         * ETag support for conditional requests
         */
        @GetMapping("/users/{id}/etag")
        public ResponseEntity<UserDto> getUserWithEtag(
                @PathVariable Long id,
                @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
            UserDto user = userService().findById(id);
            String etag = generateEtag(user);
            
            if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            
            return ResponseEntity.ok()
                    .eTag(etag)
                    .body(user);
        }

        /**
         * Batch operations to reduce HTTP round trips
         */
        @PostMapping("/users/batch")
        public ResponseEntity<List<UserDto>> createUsers(@RequestBody List<CreateUserDto> userDtos) {
            List<UserDto> created = userService().createBatch(userDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        private UserService userService() {
            return new UserService();
        }

        private UserDto filterFields(UserDto user, List<String> fields) {
            // Implement field filtering logic
            return user;
        }

        private String generateEtag(UserDto user) {
            return String.valueOf(user.hashCode());
        }
    }

    /**
     * JSON View for partial serialization
     */
    public static class PublicView {}
    public static class InternalView extends PublicView {}

    /**
     * DTO with JsonView annotations
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDto {
        @JsonView(PublicView.class)
        private Long id;
        
        @JsonView(PublicView.class)
        private String name;
        
        @JsonView(InternalView.class)
        private String email;
        
        @JsonView(InternalView.class)
        private String phone;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class CreateUserDto {
        private String name;
        private String email;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * Page and Pageable for pagination
     */
    public static class Page<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public Page(List<T> content, int page, int size, long totalElements) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
        }

        public List<T> getContent() { return content; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
    }

    public static class Pageable {
        private int page;
        private int size;

        public Pageable(int page, int size) {
            this.page = page;
            this.size = size;
        }

        public int getPage() { return page; }
        public int getSize() { return size; }
    }

    /**
     * Service class
     */
    public static class UserService {
        public UserDto findById(Long id) {
            UserDto user = new UserDto();
            user.setId(id);
            user.setName("User " + id);
            user.setEmail("user" + id + "@example.com");
            return user;
        }

        public UserDto create(CreateUserDto dto) {
            UserDto user = new UserDto();
            user.setId(1L);
            user.setName(dto.getName());
            user.setEmail(dto.getEmail());
            return user;
        }

        public Page<UserDto> findAll(Pageable pageable) {
            List<UserDto> users = List.of();
            return new Page<>(users, pageable.getPage(), pageable.getSize(), 0);
        }

        public List<UserDto> createBatch(List<CreateUserDto> userDtos) {
            return List.of();
        }
    }

    /**
     * HTTP client service with keep-alive and connection pooling
     */
    @org.springframework.stereotype.Service
    public static class HttpClientService {
        
        private final RestTemplate restTemplate;

        public HttpClientService(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        /**
         * GET request with proper headers
         */
        public UserDto getUser(Long id) {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "MyApp/1.0");
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<UserDto> response = restTemplate.exchange(
                    "/api/users/" + id,
                    HttpMethod.GET,
                    entity,
                    UserDto.class
            );
            
            return response.getBody();
        }

        /**
         * POST request with JSON body
         */
        public UserDto createUser(CreateUserDto userDto) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<CreateUserDto> entity = new HttpEntity<>(userDto, headers);
            
            ResponseEntity<UserDto> response = restTemplate.postForEntity(
                    "/api/users",
                    entity,
                    UserDto.class
            );
            
            return response.getBody();
        }

        /**
         * Batch GET requests using parallel streams
         */
        public List<UserDto> getUsersBatch(List<Long> ids) {
            return ids.parallelStream()
                    .map(this::getUser)
                    .toList();
        }
    }

    /**
     * Reactive HTTP client service
     */
    @org.springframework.stereotype.Service
    public static class ReactiveHttpClientService {
        
        private final WebClient webClient;

        public ReactiveHttpClientService(WebClient webClient) {
            this.webClient = webClient;
        }

        /**
         * Reactive GET request
         */
        public reactor.core.publisher.Mono<UserDto> getUser(Long id) {
            return webClient.get()
                    .uri("/api/users/{id}", id)
                    .retrieve()
                    .bodyToMono(UserDto.class);
        }

        /**
         * Reactive POST request
         */
        public reactor.core.publisher.Mono<UserDto> createUser(CreateUserDto userDto) {
            return webClient.post()
                    .uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userDto)
                    .retrieve()
                    .bodyToMono(UserDto.class);
        }

        /**
         * Parallel reactive requests
         */
        public reactor.core.publisher.Flux<UserDto> getUsersBatch(List<Long> ids) {
            return reactor.core.publisher.Flux.fromIterable(ids)
                    .flatMap(this::getUser);
        }
    }

    /**
     * Rate limiting interceptor
     */
    @org.springframework.stereotype.Component
    public static class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        
        private final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(100);

        @Override
        public boolean preHandle(javax.servlet.http.HttpServletRequest request,
                                javax.servlet.http.HttpServletResponse response,
                                Object handler) throws Exception {
            if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return false;
            }
            return true;
        }

        @Override
        public void afterCompletion(javax.servlet.http.HttpServletRequest request,
                                    javax.servlet.http.HttpServletResponse response,
                                    Object handler, Exception ex) {
            semaphore.release();
        }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR HTTP/REST OPTIMIZATION:
 * 
 * server:
 *   compression:
 *     enabled: true
 *     mime-types: application/json,application/xml,text/html,text/xml,text/plain
 *     min-response-size: 1024
 *   http2:
 *     enabled: true
 *   tomcat:
 *     keep-alive-timeout: 30000
 *     max-keep-alive-requests: 100
 *     threads:
 *       max: 200
 *       min-spare: 10
 * 
 * spring:
 *   mvc:
 *     async:
 *       request-timeout: 30000
 *   jackson:
 *     serialization:
 *       write-dates-as-timestamps: false
 *     default-property-inclusion: non_null
 *     time-zone: UTC
 */
