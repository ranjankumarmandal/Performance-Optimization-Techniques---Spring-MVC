package com.example.optimization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Database & Data Access Optimization Examples
 * 
 * Demonstrates various techniques to optimize database operations
 * and data access layer performance.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseDataAccessOptimization {

    /**
     * Configure HikariCP connection pool (default in Spring Boot 2.x+)
     */
    @Bean
    public DataSource dataSource() {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("user");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setPoolName("SpringHikariCP");
        
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * Configure JPA with Hibernate optimizations
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.optimization");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties());
        return em;
    }

    /**
     * Hibernate optimization properties
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        
        // Batch operations
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.batch_versioned_data", "true");
        
        // Second-level cache
        properties.put("hibernate.cache.use_second_level_cache", "true");
        properties.put("hibernate.cache.use_query_cache", "true");
        properties.put("hibernate.cache.region.factory_class", 
            "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        
        // Lazy loading
        properties.put("hibernate.default_batch_fetch_size", "16");
        
        // Statistics (disable in production)
        properties.put("hibernate.generate_statistics", "false");
        
        // DDL (validate in production)
        properties.put("hibernate.hbm2ddl.auto", "validate");
        
        // SQL logging (disable in production)
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        
        return properties;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    /**
     * JdbcTemplate for simple queries (faster than JPA)
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * SimpleJdbcInsert for optimized inserts
     */
    @Bean
    public SimpleJdbcInsert simpleJdbcInsert(DataSource dataSource) {
        return new SimpleJdbcInsert(dataSource).withTableName("users");
    }

    /**
     * Entity with lazy loading configuration
     */
    @Entity
    @Table(name = "users")
    @Cacheable
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        private String name;
        private String email;
        
        // Lazy loading for relationships
        @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
        private List<Order> orders;
        
        @ManyToOne(fetch = FetchType.LAZY)
        private Department department;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<Order> getOrders() { return orders; }
        public void setOrders(List<Order> orders) { this.orders = orders; }
        public Department getDepartment() { return department; }
        public void setDepartment(Department department) { this.department = department; }
    }

    @Entity
    @Table(name = "orders")
    public static class Order {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @ManyToOne(fetch = FetchType.LAZY)
        private User user;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }

    @Entity
    @Table(name = "departments")
    public static class Department {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        private String name;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Repository with custom query optimization
     */
    public interface UserRepository extends JpaRepository<User, Long> {
        
        // Use EntityGraph to avoid N+1 problem
        @EntityGraph(attributePaths = {"department", "orders"})
        User findByIdWithRelations(Long id);
        
        // Use projection for partial data
        @Query("SELECT u.id, u.name FROM User u WHERE u.email = :email")
        List<Object[]> findPartialByEmail(@Param("email") String email);
        
        // Use native query for complex operations
        @Query(value = "SELECT * FROM users WHERE name LIKE %:name%", nativeQuery = true)
        List<User> findByNameNative(@Param("name") String name);
        
        // Read-only transaction for queries
        @Transactional(readOnly = true)
        @Query("SELECT u FROM User u WHERE u.department.id = :deptId")
        List<User> findByDepartmentId(@Param("deptId") Long deptId);
    }

    /**
     * Service with transaction optimization
     */
    @org.springframework.stereotype.Service
    public static class UserService {
        
        @PersistenceContext
        private EntityManager entityManager;
        
        private JdbcTemplate jdbcTemplate;
        
        /**
         * Batch insert using JPA
         */
        @Transactional
        public void batchInsertUsers(List<User> users) {
            for (int i = 0; i < users.size(); i++) {
                entityManager.persist(users.get(i]);
                
                // Flush and clear periodically to avoid memory issues
                if (i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }
        
        /**
         * Read-only transaction for queries
         */
        @Transactional(readOnly = true)
        public User findById(Long id) {
            return entityManager.find(User.class, id);
        }
        
        /**
         * Use JdbcTemplate for simple queries (faster than JPA)
         */
        public User findByIdWithJdbcTemplate(Long id) {
            String sql = "SELECT id, name, email FROM users WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(User.class), id);
        }
        
        /**
         * Batch update using JdbcTemplate
         */
        public int[] batchUpdateUsers(List<User> users) {
            String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
            return jdbcTemplate.batchUpdate(sql, users.stream()
                .map(user -> new Object[]{user.getName(), user.getEmail(), user.getId()})
                .toArray(Object[][]::new));
        }
        
        /**
         * Use JOIN FETCH to avoid N+1 problem
         */
        @Transactional(readOnly = true)
        public User findByIdWithOrders(Long id) {
            String jpql = "SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id";
            TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
            query.setParameter("id", id);
            return query.getSingleResult();
        }
        
        /**
         * Use EntityGraph for complex queries
         */
        @Transactional(readOnly = true)
        public User findByIdWithEntityGraph(Long id) {
            EntityGraph<?> graph = entityManager.getEntityGraph("User.withDepartmentAndOrders");
            Map<String, Object> hints = new java.util.HashMap<>();
            hints.put("javax.persistence.fetchgraph", graph);
            return entityManager.find(User.class, id, hints);
        }
    }

    /**
     * Projection interface for partial data retrieval
     */
    public interface UserNameOnly {
        Long getId();
        String getName();
    }

    /**
     * DTO for complex queries
     */
    public static class UserSummaryDto {
        private Long id;
        private String name;
        private Long orderCount;
        
        public UserSummaryDto(Long id, String name, Long orderCount) {
            this.id = id;
            this.name = name;
            this.orderCount = orderCount;
        }
        
        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getOrderCount() { return orderCount; }
    }
}

/*
 * APPLICATION.YML CONFIGURATION FOR DATABASE OPTIMIZATION:
 * 
 * spring:
 *   datasource:
 *     hikari:
 *       maximum-pool-size: 20
 *       minimum-idle: 10
 *       connection-timeout: 20000
 *       idle-timeout: 30000
 *       max-lifetime: 1800000
 *       pool-name: SpringHikariCP
 *   jpa:
 *     hibernate:
 *       ddl-auto: validate
 *       jdbc:
 *         batch_size: 50
 *       cache:
 *         use_second_level_cache: true
 *         use_query_cache: true
 *         region:
 *           factory_class: org.hibernate.cache.ehcache.EhCacheRegionFactory
 *     properties:
 *       hibernate:
 *         order_inserts: true
 *         order_updates: true
 *         default_batch_fetch_size: 16
 *         generate_statistics: false
 *         show_sql: false
 *         format_sql: false
 */
