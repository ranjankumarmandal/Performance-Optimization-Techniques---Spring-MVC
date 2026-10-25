package com.example.optimization;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Architecture Patterns for Performance Examples
 * 
 * Demonstrates various architectural patterns that improve performance
 * including microservices, CQRS, event-driven architecture, and more.
 */
public class ArchitecturePatterns {

    /**
     * Microservices Pattern
     * 
     * Benefits:
     * - Independent scaling
     * - Fault isolation
     * - Technology diversity
     * - Faster deployment
     * 
     * Trade-offs:
     * - Increased complexity
     * - Network latency
     * - Distributed transactions
     */
    @Service
    public static class MicroservicesPattern {
        
        /**
         * Service A - User Service
         */
        @Service
        public static class UserService {
            public User getUser(Long id) {
                // User business logic
                return new User(id, "User " + id);
            }
        }

        /**
         * Service B - Order Service
         */
        @Service
        public static class OrderService {
            public Order createOrder(Long userId, List<String> items) {
                // Order business logic
                return new Order(1L, userId, items);
            }
        }

        /**
         * API Gateway - Routes requests to appropriate services
         */
        @Service
        public static class ApiGateway {
            
            private final UserService userService;
            private final OrderService orderService;

            public ApiGateway(UserService userService, OrderService orderService) {
                this.userService = userService;
                this.orderService = orderService;
            }

            public Object routeRequest(String service, String action, Object data) {
                switch (service) {
                    case "user":
                        return handleUserRequest(action, data);
                    case "order":
                        return handleOrderRequest(action, data);
                    default:
                        throw new IllegalArgumentException("Unknown service: " + service);
                }
            }

            private Object handleUserRequest(String action, Object data) {
                switch (action) {
                    case "getUser":
                        return userService.getUser((Long) data);
                    default:
                        throw new IllegalArgumentException("Unknown action: " + action);
                }
            }

            private Object handleOrderRequest(String action, Object data) {
                switch (action) {
                    case "createOrder":
                        return orderService.createOrder(((OrderRequest) data).userId, ((OrderRequest) data).items);
                    default:
                        throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
        }

        static class User {
            Long id;
            String name;
            
            public User(Long id, String name) {
                this.id = id;
                this.name = name;
            }
        }

        static class Order {
            Long id;
            Long userId;
            List<String> items;
            
            public Order(Long id, Long userId, List<String> items) {
                this.id = id;
                this.userId = userId;
                this.items = items;
            }
        }

        static class OrderRequest {
            Long userId;
            List<String> items;
        }
    }

    /**
     * CQRS (Command Query Responsibility Segregation)
     * 
     * Benefits:
     * - Optimized read models
     * - Scalable reads and writes separately
     * - Better performance for complex queries
     * 
     * Trade-offs:
     * - Increased complexity
     * - Eventual consistency
     * - More code to maintain
     */
    @Service
    public static class CQRSPattern {
        
        /**
         * Write Model - Handles commands
         */
        @Service
        public static class WriteModel {
            
            private final EventStore eventStore;

            public WriteModel(EventStore eventStore) {
                this.eventStore = eventStore;
            }

            public void createUser(CreateUserCommand command) {
                // Validate command
                // Execute business logic
                // Store event
                UserCreatedEvent event = new UserCreatedEvent(command.getUserId(), command.getName());
                eventStore.save(event);
            }

            public void updateUser(UpdateUserCommand command) {
                UserUpdatedEvent event = new UserUpdatedEvent(command.getUserId(), command.getName());
                eventStore.save(event);
            }
        }

        /**
         * Read Model - Handles queries
         */
        @Service
        public static class ReadModel {
            
            private final UserReadRepository readRepository;

            public ReadModel(UserReadRepository readRepository) {
                this.readRepository = readRepository;
            }

            public UserReadModel getUser(Long userId) {
                return readRepository.findById(userId);
            }

            public List<UserReadModel> getAllUsers() {
                return readRepository.findAll();
            }
        }

        /**
         * Event Store - Stores domain events
         */
        @Service
        public static class EventStore {
            private final List<Object> events = new java.util.concurrent.CopyOnWriteArrayList<>();

            public void save(Object event) {
                events.add(event);
            }

            public List<Object> getEvents() {
                return java.util.Collections.unmodifiableList(events);
            }
        }

        /**
         * Projector - Updates read model from events
         */
        @Service
        public static class Projector {
            
            private final UserReadRepository readRepository;

            public Projector(UserReadRepository readRepository) {
                this.readRepository = readRepository;
            }

            public void project(UserCreatedEvent event) {
                UserReadModel model = new UserReadModel(event.getUserId(), event.getName());
                readRepository.save(model);
            }

            public void project(UserUpdatedEvent event) {
                UserReadModel model = readRepository.findById(event.getUserId());
                if (model != null) {
                    model.setName(event.getName());
                    readRepository.save(model);
                }
            }
        }

        static class CreateUserCommand {
            Long userId;
            String name;
            
            public CreateUserCommand(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
            
            public Long getUserId() { return userId; }
            public String getName() { return name; }
        }

        static class UpdateUserCommand {
            Long userId;
            String name;
            
            public UpdateUserCommand(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
            
            public Long getUserId() { return userId; }
            public String getName() { return name; }
        }

        static class UserCreatedEvent {
            Long userId;
            String name;
            
            public UserCreatedEvent(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
            
            public Long getUserId() { return userId; }
            public String getName() { return name; }
        }

        static class UserUpdatedEvent {
            Long userId;
            String name;
            
            public UserUpdatedEvent(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
            
            public Long getUserId() { return userId; }
            public String getName() { return name; }
        }

        static class UserReadModel {
            Long userId;
            String name;
            
            public UserReadModel(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
            
            public Long getUserId() { return userId; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        static class UserReadRepository {
            private final java.util.Map<Long, UserReadModel> storage = new java.util.concurrent.ConcurrentHashMap<>();
            
            public UserReadModel findById(Long userId) {
                return storage.get(userId);
            }
            
            public List<UserReadModel> findAll() {
                return new java.util.ArrayList<>(storage.values());
            }
            
            public void save(UserReadModel model) {
                storage.put(model.getUserId(), model);
            }
        }
    }

    /**
     * Event-Driven Architecture
     * 
     * Benefits:
     * - Loose coupling
     * - Scalability
     * - Asynchronous processing
     * - Better resilience
     * 
     * Trade-offs:
     * - Eventual consistency
     * - Debugging complexity
     * - Event schema evolution
     */
    @Service
    public static class EventDrivenArchitecture {
        
        /**
         * Event Publisher
         */
        @Service
        public static class EventPublisher {
            
            private final List<EventHandler> handlers = new java.util.concurrent.CopyOnWriteArrayList<>();

            public void registerHandler(EventHandler handler) {
                handlers.add(handler);
            }

            public void publish(DomainEvent event) {
                handlers.forEach(handler -> handler.handle(event));
            }
        }

        /**
         * Event Handler Interface
         */
        public interface EventHandler {
            void handle(DomainEvent event);
        }

        /**
         * Domain Event
         */
        public static class DomainEvent {
            private final String eventType;
            private final Object data;
            private final long timestamp;

            public DomainEvent(String eventType, Object data) {
                this.eventType = eventType;
                this.data = data;
                this.timestamp = System.currentTimeMillis();
            }

            public String getEventType() { return eventType; }
            public Object getData() { return data; }
            public long getTimestamp() { return timestamp; }
        }

        /**
         * Example Event Handler
         */
        @Service
        public static class UserCreatedEventHandler implements EventHandler {
            
            @Override
            public void handle(DomainEvent event) {
                if ("UserCreated".equals(event.getEventType())) {
                    System.out.println("Handling UserCreated event: " + event.getData());
                    // Process event
                }
            }
        }

        /**
         * Event Sourcing - Store events instead of current state
         */
        @Service
        public static class EventSourcing {
            
            private final List<DomainEvent> eventStream = new java.util.concurrent.CopyOnWriteArrayList<>();

            public void saveEvent(DomainEvent event) {
                eventStream.add(event);
            }

            public List<DomainEvent> getEvents() {
                return java.util.Collections.unmodifiableList(eventStream);
            }

            public Object rebuildState() {
                // Rebuild state from events
                Object state = new Object();
                for (DomainEvent event : eventStream) {
                    state = applyEvent(state, event);
                }
                return state;
            }

            private Object applyEvent(Object state, DomainEvent event) {
                // Apply event to state
                return state;
            }
        }
    }

    /**
     * Saga Pattern - Distributed Transactions
     * 
     * Benefits:
     * - Maintains data consistency across services
     * - Handles long-running transactions
     * - Supports compensation actions
     * 
     * Trade-offs:
     * - Increased complexity
     * - Requires compensation logic
     * - Eventual consistency
     */
    @Service
    public static class SagaPattern {
        
        /**
         * Saga Orchestrator
         */
        @Service
        public static class SagaOrchestrator {
            
            private final List<SagaStep> steps = new java.util.ArrayList<>();

            public void addStep(SagaStep step) {
                steps.add(step);
            }

            public void execute() {
                List<SagaStep> completedSteps = new java.util.ArrayList<>();
                
                try {
                    // Execute all steps
                    for (SagaStep step : steps) {
                        step.execute();
                        completedSteps.add(step);
                    }
                } catch (Exception e) {
                    // Compensate completed steps in reverse order
                    for (int i = completedSteps.size() - 1; i >= 0; i--) {
                        try {
                            completedSteps.get(i).compensate();
                        } catch (Exception ex) {
                            System.err.println("Compensation failed: " + ex.getMessage());
                        }
                    }
                    throw new RuntimeException("Saga failed", e);
                }
            }
        }

        /**
         * Saga Step Interface
         */
        public interface SagaStep {
            void execute();
            void compensate();
        }

        /**
         * Example Saga Step
         */
        public static class CreateOrderStep implements SagaStep {
            
            @Override
            public void execute() {
                System.out.println("Creating order...");
                // Execute order creation
            }

            @Override
            public void compensate() {
                System.out.println("Compensating: Canceling order...");
                // Cancel order
            }
        }

        public static class ReserveInventoryStep implements SagaStep {
            
            @Override
            public void execute() {
                System.out.println("Reserving inventory...");
                // Reserve inventory
            }

            @Override
            public void compensate() {
                System.out.println("Compensating: Releasing inventory...");
                // Release inventory
            }
        }

        public static class ProcessPaymentStep implements SagaStep {
            
            @Override
            public void execute() {
                System.out.println("Processing payment...");
                // Process payment
            }

            @Override
            public void compensate() {
                System.out.println("Compensating: Refunding payment...");
                // Refund payment
            }
        }
    }

    /**
     * Backend for Frontend (BFF) Pattern
     * 
     * Benefits:
     * - Optimized APIs per client
     * - Reduced client complexity
     * - Better performance
     * - Client-specific data formatting
     * 
     * Trade-offs:
     * - Additional layer
     * - More code to maintain
     */
    @Service
    public static class BFFPattern {
        
        /**
         * Web BFF - For web clients
         */
        @Service
        public static class WebBFF {
            
            public WebResponse getUserProfile(Long userId) {
                // Aggregate data from multiple services
                User user = getUserService().getUser(userId);
                List<Order> orders = getOrderService().getOrders(userId);
                
                return new WebResponse(user, orders);
            }

            private UserService getUserService() {
                return new UserService();
            }

            private OrderService getOrderService() {
                return new OrderService();
            }
        }

        /**
         * Mobile BFF - For mobile clients
         */
        @Service
        public static class MobileBFF {
            
            public MobileResponse getUserProfile(Long userId) {
                // Optimized data for mobile
                User user = getUserService().getUser(userId);
                return new MobileResponse(user.getId(), user.getName());
            }

            private UserService getUserService() {
                return new UserService();
            }
        }

        static class WebResponse {
            User user;
            List<Order> orders;
            
            public WebResponse(User user, List<Order> orders) {
                this.user = user;
                this.orders = orders;
            }
        }

        static class MobileResponse {
            Long userId;
            String name;
            
            public MobileResponse(Long userId, String name) {
                this.userId = userId;
                this.name = name;
            }
        }

        static class UserService {
            public User getUser(Long id) {
                return new User(id, "User " + id);
            }
        }

        static class OrderService {
            public List<Order> getOrders(Long userId) {
                return List.of();
            }
        }

        static class User {
            Long id;
            String name;
            
            public User(Long id, String name) {
                this.id = id;
                this.name = name;
            }
            
            public Long getId() { return id; }
            public String getName() { return name; }
        }

        static class Order {
            Long id;
        }
    }

    /**
     * Read Replica Pattern
     * 
     * Benefits:
     * - Scalable read operations
     * - Reduced load on primary database
     * - Better performance for read-heavy workloads
     * 
     * Trade-offs:
     * - Eventual consistency
     * - Replication lag
     * - Increased infrastructure cost
     */
    @Service
    public static class ReadReplicaPattern {
        
        /**
         * Routing data source
         */
        @Service
        public static class RoutingDataSource {
            
            private javax.sql.DataSource primaryDataSource;
            private javax.sql.DataSource replicaDataSource;
            
            private final ThreadLocal<String> currentDataSource = new ThreadLocal<>();

            public void setPrimaryDataSource(javax.sql.sql.DataSource primaryDataSource) {
                this.primaryDataSource = primaryDataSource;
            }

            public void setReplicaDataSource(javax.sql.sql.DataSource replicaDataSource) {
                this.replicaDataSource = replicaDataSource;
            }

            public void usePrimary() {
                currentDataSource.set("primary");
            }

            public void useReplica() {
                currentDataSource.set("replica");
            }

            public javax.sql.sql.DataSource getDataSource() {
                return "primary".equals(currentDataSource.get()) ? primaryDataSource : replicaDataSource;
            }
        }

        /**
         * Service with read replica routing
         */
        @Service
        public static class DataService {
            
            private final RoutingDataSource routingDataSource;

            public DataService(RoutingDataSource routingDataSource) {
                this.routingDataSource = routingDataSource;
            }

            public User getUser(Long id) {
                // Use replica for reads
                routingDataSource.useReplica();
                try {
                    return queryUser(id);
                } finally {
                    routingDataSource.usePrimary();
                }
            }

            public void updateUser(User user) {
                // Use primary for writes
                routingDataSource.usePrimary();
                try {
                    saveUser(user);
                } finally {
                    routingDataSource.useReplica();
                }
            }

            private User queryUser(Long id) {
                return new User(id, "User " + id);
            }

            private void saveUser(User user) {
                // Save user
            }
        }

        static class User {
            Long id;
            String name;
            
            public User(Long id, String name) {
                this.id = id;
                this.name = name;
            }
        }
    }

    /**
     * Sharding Pattern
     * 
     * Benefits:
     * - Horizontal scalability
     * - Better performance for large datasets
     * - Distributed load
     * 
     * Trade-offs:
     * - Increased complexity
     * - Cross-shard queries
     * - Rebalancing challenges
     */
    @Service
    public static class ShardingPattern {
        
        /**
         * Shard router
         */
        @Service
        public static class ShardRouter {
            
            private final List<Shard> shards = new java.util.ArrayList<>();

            public ShardRouter() {
                // Initialize shards
                for (int i = 0; i < 4; i++) {
                    shards.add(new Shard(i));
                }
            }

            public Shard getShard(Long key) {
                // Consistent hashing
                int shardIndex = (int) (key % shards.size());
                return shards.get(shardIndex);
            }
        }

        /**
         * Shard
         */
        public static class Shard {
            private final int shardId;
            private final java.util.Map<Long, Object> data = new java.util.concurrent.ConcurrentHashMap<>();

            public Shard(int shardId) {
                this.shardId = shardId;
            }

            public void put(Long key, Object value) {
                data.put(key, value);
            }

            public Object get(Long key) {
                return data.get(key);
            }

            public int getShardId() {
                return shardId;
            }
        }

        /**
         * Sharded service
         */
        @Service
        public static class ShardedService {
            
            private final ShardRouter shardRouter;

            public ShardedService(ShardRouter shardRouter) {
                this.shardRouter = shardRouter;
            }

            public void save(Long key, Object value) {
                Shard shard = shardRouter.getShard(key);
                shard.put(key, value);
            }

            public Object get(Long key) {
                Shard shard = shardRouter.getShard(key);
                return shard.get(key);
            }
        }
    }

    /**
     * Circuit Breaker Pattern
     * 
     * Benefits:
     * - Prevents cascading failures
     * - Fast failure when service is down
     * - Automatic recovery
     * 
     * Trade-offs:
     * - Added complexity
     * - Configuration tuning required
     */
    @Service
    public static class CircuitBreakerPattern {
        
        /**
         * Circuit breaker implementation
         */
        public static class CircuitBreaker {
            private final int failureThreshold;
            private final long timeoutMillis;
            private int failureCount = 0;
            private long lastFailureTime = 0;
            private State state = State.CLOSED;

            public CircuitBreaker(int failureThreshold, long timeoutMillis) {
                this.failureThreshold = failureThreshold;
                this.timeoutMillis = timeoutMillis;
            }

            public <T> T execute(java.util.concurrent.Callable<T> callable) throws Exception {
                if (state == State.OPEN) {
                    if (System.currentTimeMillis() - lastFailureTime > timeoutMillis) {
                        state = State.HALF_OPEN;
                    } else {
                        throw new RuntimeException("Circuit breaker is OPEN");
                    }
                }

                try {
                    T result = callable.call();
                    onSuccess();
                    return result;
                } catch (Exception e) {
                    onFailure();
                    throw e;
                }
            }

            private void onSuccess() {
                failureCount = 0;
                state = State.CLOSED;
            }

            private void onFailure() {
                failureCount++;
                lastFailureTime = System.currentTimeMillis();
                if (failureCount >= failureThreshold) {
                    state = State.OPEN;
                }
            }

            public State getState() {
                return state;
            }

            enum State {
                CLOSED, OPEN, HALF_OPEN
            }
        }

        /**
         * Service with circuit breaker
         */
        @Service
        public static class ExternalService {
            
            private final CircuitBreaker circuitBreaker;

            public ExternalService() {
                this.circuitBreaker = new CircuitBreaker(5, 60000);
            }

            public String callExternalApi() throws Exception {
                return circuitBreaker.execute(() -> {
                    // Call external API
                    if (Math.random() > 0.8) {
                        throw new RuntimeException("External service failed");
                    }
                    return "Success";
                });
            }
        }
    }

    /**
     * Bulkhead Pattern
     * 
     * Benefits:
     * - Isolates failures
     * - Limits resource usage
     * - Prevents resource exhaustion
     * 
     * Trade-offs:
     * - Rejected requests under load
     * - Configuration complexity
     */
    @Service
    public static class BulkheadPattern {
        
        /**
         * Semaphore-based bulkhead
         */
        public static class Bulkhead {
            private final java.util.concurrent.Semaphore semaphore;

            public Bulkhead(int maxConcurrent) {
                this.semaphore = new java.util.concurrent.Semaphore(maxConcurrent);
            }

            public <T> T execute(java.util.concurrent.Callable<T> callable) throws Exception {
                if (!semaphore.tryAcquire(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new RuntimeException("Bulkhead limit reached");
                }

                try {
                    return callable.call();
                } finally {
                    semaphore.release();
                }
            }
        }

        /**
         * Service with bulkhead
         */
        @Service
        public static class DatabaseService {
            
            private final Bulkhead bulkhead;

            public DatabaseService() {
                this.bulkhead = new Bulkhead(10);
            }

            public Object queryDatabase() throws Exception {
                return bulkhead.execute(() -> {
                    // Database query
                    Thread.sleep(100);
                    return "Result";
                });
            }
        }
    }
}

/*
 * ARCHITECTURE PATTERNS SUMMARY:
 * 
 * 1. Microservices:
 *    - Independent scaling
 *    - Fault isolation
 *    - Technology diversity
 * 
 * 2. CQRS:
 *    - Separate read and write models
 *    - Optimized for specific operations
 *    - Eventual consistency
 * 
 * 3. Event-Driven:
 *    - Loose coupling
 *    - Asynchronous processing
 *    - Event sourcing
 * 
 * 4. Saga:
 *    - Distributed transactions
 *    - Compensation actions
 *    - Long-running transactions
 * 
 * 5. BFF:
 *    - Client-specific APIs
 *    - Optimized data transfer
 *    - Reduced client complexity
 * 
 * 6. Read Replicas:
 *    - Scalable reads
 *    - Reduced primary load
 *    - Eventual consistency
 * 
 * 7. Sharding:
 *    - Horizontal scalability
 *    - Distributed load
 *    - Consistent hashing
 * 
 * 8. Circuit Breaker:
 *    - Prevent cascading failures
 *    - Fast failure
 *    - Automatic recovery
 * 
 * 9. Bulkhead:
 *    - Resource isolation
 *    - Limit concurrency
 *    - Prevent exhaustion
 */
