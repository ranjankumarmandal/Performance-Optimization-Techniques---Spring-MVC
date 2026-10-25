package com.example.optimization;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

/**
 * JVM & Memory Optimization Examples
 * 
 * This class demonstrates various JVM and memory optimization techniques.
 * Note: Many JVM optimizations are configured via command-line arguments,
 * but this shows programmatic monitoring and configuration.
 */
public class JvmMemoryOptimization {

    /**
     * Monitor JVM memory usage programmatically
     */
    public static void monitorMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        // Heap memory usage
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        
        System.out.println("Heap Memory:");
        System.out.println("  Used: " + (heapUsed / 1024 / 1024) + " MB");
        System.out.println("  Max: " + (heapMax / 1024 / 1024) + " MB");
        System.out.println("  Committed: " + (heapCommitted / 1024 / 1024) + " MB");
        
        // Non-heap memory usage (Metaspace, Code Cache, etc.)
        long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        
        System.out.println("Non-Heap Memory:");
        System.out.println("  Used: " + (nonHeapUsed / 1024 / 1024) + " MB");
        System.out.println("  Max: " + (nonHeapMax / 1024 / 1024) + " MB");
    }

    /**
     * Monitor specific memory pools (Metaspace, Old Gen, etc.)
     */
    public static void monitorMemoryPools() {
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        
        for (MemoryPoolMXBean pool : memoryPools) {
            System.out.println("Pool: " + pool.getName());
            System.out.println("  Type: " + pool.getType());
            System.out.println("  Used: " + (pool.getUsage().getUsed() / 1024 / 1024) + " MB");
            System.out.println("  Max: " + (pool.getUsage().getMax() / 1024 / 1024) + " MB");
            System.out.println("  Peak: " + (pool.getPeakUsage().getUsed() / 1024 / 1024) + " MB");
        }
    }

    /**
     * String deduplication example (requires G1GC with -XX:+UseStringDeduplication)
     * This demonstrates how to reduce memory footprint by reusing strings
     */
    public static class StringDeduplicationExample {
        private static final String COMMON_STRING = "frequently_used_string";
        
        public void demonstrateStringInterning() {
            // Use String.intern() for strings that are used frequently
            String s1 = "test".intern();
            String s2 = "test".intern();
            
            // s1 and s2 will reference the same string object
            System.out.println("s1 == s2: " + (s1 == s2));
            
            // For large datasets, consider using a dedicated string pool
            // or rely on G1GC string deduplication
        }
    }

    /**
     * Object pooling example to reduce GC pressure
     */
    public static class ObjectPoolExample {
        private static final int POOL_SIZE = 100;
        
        // Simple object pool for frequently created objects
        private static class ObjectPool<T> {
            private final Object[] pool;
            private int index = 0;
            
            public ObjectPool(Class<T> clazz, int size) throws Exception {
                this.pool = new Object[size];
                for (int i = 0; i < size; i++) {
                    pool[i] = clazz.getDeclaredConstructor().newInstance();
                }
            }
            
            @SuppressWarnings("unchecked")
            public T borrowObject() {
                if (index < pool.length) {
                    return (T) pool[index++];
                }
                return null; // Pool exhausted
            }
            
            public void returnObject(T obj) {
                if (index > 0) {
                    pool[--index] = obj;
                }
            }
        }
        
        public void demonstrateObjectPooling() throws Exception {
            ObjectPool<StringBuilder> pool = new ObjectPool<>(StringBuilder.class, POOL_SIZE);
            
            StringBuilder sb = pool.borrowObject();
            sb.setLength(0); // Reset instead of creating new
            sb.append("Hello");
            
            pool.returnObject(sb);
        }
    }

    /**
     * Memory leak detection example
     */
    public static class MemoryLeakDetection {
        // Static collection that can cause memory leaks if not managed
        private static final java.util.Map<String, Object> CACHE = new java.util.WeakHashMap<>();
        
        public void addToCache(String key, Object value) {
            // WeakHashMap allows entries to be garbage collected
            CACHE.put(key, value);
        }
        
        public void detectPotentialLeaks() {
            // Check for growing collections
            System.out.println("Cache size: " + CACHE.size());
            
            // Use -XX:+HeapDumpOnOutOfMemoryError to generate heap dump on OOM
            // Analyze with VisualVM, Eclipse MAT, or jhat
        }
    }

    /**
     * Main method to run examples
     */
    public static void main(String[] args) {
        System.out.println("=== JVM Memory Optimization Examples ===\n");
        
        monitorMemoryUsage();
        System.out.println();
        
        monitorMemoryPools();
        System.out.println();
        
        StringDeduplicationExample stringExample = new StringDeduplicationExample();
        stringExample.demonstrateStringInterning();
        System.out.println();
        
        try {
            ObjectPoolExample poolExample = new ObjectPoolExample();
            poolExample.demonstrateObjectPooling();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/*
 * JVM COMMAND LINE ARGUMENTS FOR PRODUCTION:
 * 
 * # Heap sizing
 * -Xms4g -Xmx4g
 * 
 * # Garbage collection
 * -XX:+UseG1GC
 * -XX:MaxGCPauseMillis=200
 * -XX:G1HeapRegionSize=16m
 * 
 * # String deduplication
 * -XX:+UseStringDeduplication
 * -XX:StringDeduplicationAgeThreshold=3
 * 
 * # Metaspace
 * -XX:MetaspaceSize=256m
 * -XX:MaxMetaspaceSize=512m
 * 
 * # GC logging
 * -Xlog:gc*:file=/var/log/app/gc.log:time,tags
 * 
 * # Heap dump on OOM
 * -XX:+HeapDumpOnOutOfMemoryError
 * -XX:HeapDumpPath=/var/log/app/
 * 
 * # Native memory tracking (for debugging)
 * -XX:NativeMemoryTracking=detail
 * 
 * # JIT compilation
 * -XX:+TieredCompilation
 * -XX:CompileThreshold=10000
 */
