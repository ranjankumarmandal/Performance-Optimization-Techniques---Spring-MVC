package com.example.optimization;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.loader.tools.LayeredJarTools;

/**
 * Build & Deployment Optimization Examples
 * 
 * Demonstrates various techniques to optimize build times and deployment
 * including Maven/Gradle configuration, Docker optimization, and native images.
 * 
 * Note: This file contains configuration examples and documentation.
 * Actual build configurations should be in pom.xml, build.gradle, or Dockerfile.
 */
public class BuildDeploymentOptimization {

    /**
     * Spring Boot application builder for startup optimization
     */
    public static class OptimizedApplicationBuilder {
        
        public static org.springframework.context.ConfigurableApplicationContext build(Class<?>... sources) {
            return new SpringApplicationBuilder(sources)
                    // Disable banner for faster startup
                    .bannerMode(org.springframework.boot.Banner.Mode.OFF)
                    // Enable lazy initialization
                    .properties("spring.main.lazy-initialization=true")
                    // Disable bean definition overriding
                    .properties("spring.main.allow-bean-definition-overriding=false")
                    // Disable shutdown hook if not needed
                    .properties("spring.main.register-shutdown-hook=false")
                    // Log startup info
                    .logStartupInfo(true)
                    .run();
        }
    }

    /**
     * JVM startup optimization with AppCDS
     * 
     * Steps to enable Application Class-Data Sharing:
     * 1. Create class list: java -XX:DumpLoadedClassList=classes.lst -jar app.jar
     * 2. Create shared archive: java -Xshare:dump -XX:SharedArchiveFile=app.jsa -XX:SharedClassListFile=classes.lst -cp app.jar
     * 3. Run with shared archive: java -Xshare:auto -XX:SharedArchiveFile=app.jsa -jar app.jar
     */
    public static class AppCDSOptimization {
        
        public static void main(String[] args) {
            System.out.println("AppCDS Optimization:");
            System.out.println("Step 1: java -XX:DumpLoadedClassList=classes.lst -jar app.jar");
            System.out.println("Step 2: java -Xshare:dump -XX:SharedArchiveFile=app.jsa -XX:SharedClassListFile=classes.lst -cp app.jar");
            System.out.println("Step 3: java -Xshare:auto -XX:SharedArchiveFile=app.jsa -jar app.jar");
        }
    }

    /**
     * GraalVM Native Image configuration
     * 
     * Benefits:
     * - Faster startup time (instant)
     * - Lower memory footprint
     * - No JVM overhead
     * 
     * Trade-offs:
     * - Longer build time
     * - Limited reflection support
     * - No dynamic class loading
     * 
     * Maven plugin configuration:
     * <plugin>
     *   <groupId>org.graalvm.buildtools</groupId>
     *   <artifactId>native-maven-plugin</artifactId>
     *   <configuration>
     *     <imageName>myapp</imageName>
     *     <buildArgs>
     *       <buildArg>--no-fallback</buildArg>
     *       <buildArg>-H:+ReportExceptionStackTraces</buildArg>
     *     </buildArgs>
     *   </configuration>
     * </plugin>
     */
    public static class GraalVMNativeImage {
        
        public static void main(String[] args) {
            System.out.println("GraalVM Native Image:");
            System.out.println("Build: mvn -Pnative native:compile");
            System.out.println("Run: ./target/myapp");
        }
    }

    /**
     * Docker multi-stage build optimization
     * 
     * Benefits:
     * - Smaller final image size
     * - Faster deployment
     * - Better security (no build tools in final image)
     */
    public static class DockerOptimization {
        
        public static String getMultiStageDockerfile() {
            return """
                # Build stage
                FROM maven:3.8.4-openjdk-17 AS build
                WORKDIR /app
                COPY pom.xml .
                COPY src ./src
                RUN mvn clean package -DskipTests
                
                # Runtime stage
                FROM openjdk:17-slim
                WORKDIR /app
                COPY --from=build /app/target/*.jar app.jar
                EXPOSE 8080
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """;
        }

        public static String getLayeredJarDockerfile() {
            return """
                # Using Spring Boot layered JARs for better caching
                FROM openjdk:17-slim
                WORKDIR /app
                COPY target/*.jar app.jar
                # Extract layers for better Docker layer caching
                RUN java -Djarmode=layertools -jar app.jar extract
                EXPOSE 8080
                ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
                """;
        }
    }

    /**
     * Maven optimization configuration
     * 
     * Add to pom.xml:
     * <properties>
     *   <maven.compiler.source>17</maven.compiler.source>
     *   <maven.compiler.target>17</maven.compiler.target>
     *   <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
     * </properties>
     * 
     * Parallel builds: mvn -T 4 clean install
     * Skip tests: mvn clean install -DskipTests
     * Offline mode: mvn -o clean install
     */
    public static class MavenOptimization {
        
        public static String getMavenPluginConfig() {
            return """
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                            <configuration>
                                <layers>
                                    <enabled>true</enabled>
                                </layers>
                                <excludes>
                                    <exclude>
                                        <groupId>org.projectlombok</groupId>
                                        <artifactId>lombok</artifactId>
                                    </exclude>
                                </excludes>
                            </configuration>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <source>17</source>
                                <target>17</target>
                                <compilerArgs>
                                    <arg>-parameters</arg>
                                </compilerArgs>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
                """;
        }
    }

    /**
     * Gradle optimization configuration
     * 
     * Add to build.gradle:
     * 
     * plugins {
     *     id 'org.springframework.boot' version '3.1.0'
     *     id 'io.spring.dependency-management' version '1.1.0'
     * }
     * 
     * // Enable configuration cache
     * org.gradle.configuration-cache=true
     * 
     * // Enable build cache
     * org.gradle.caching=true
     * 
     * // Parallel execution
     * org.gradle.parallel=true
     * 
     * // Configure JVM args for Gradle daemon
     * org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
     */
    public static class GradleOptimization {
        
        public static String getGradleConfig() {
            return """
                springBoot {
                    layeredJar {
                        enabled = true
                    }
                }
                
                bootJar {
                    layered {
                        enabled = true
                    }
                }
                
                tasks.withType(JavaCompile).configureEach {
                    options.compilerArgs.add('-parameters')
                }
                """;
        }
    }

    /**
     * CI/CD pipeline optimization
     * 
     * Strategies:
     * - Use build caching
     * - Parallelize stages
     * - Use Docker layer caching
     * - Deploy only changed services
     */
    public static class CICDOptimization {
        
        public static String getGitHubActionsConfig() {
            return """
                name: Build and Deploy
                
                on:
                  push:
                    branches: [ main ]
                
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v3
                      - name: Set up JDK 17
                        uses: actions/setup-java@v3
                        with:
                          java-version: '17'
                          distribution: 'temurin'
                          cache: 'maven'
                      - name: Build with Maven
                        run: mvn -B -DskipTests clean package
                      - name: Build Docker image
                        run: docker build -t myapp:${{ github.sha }} .
                      - name: Push to registry
                        run: docker push myapp:${{ github.sha }}
                """;
        }
    }

    /**
     * Kubernetes deployment optimization
     * 
     * Strategies:
     * - Use resource limits
     * - Configure liveness and readiness probes
     * - Use horizontal pod autoscaler
     * - Use init containers for startup tasks
     */
    public static class KubernetesOptimization {
        
        public static String getDeploymentConfig() {
            return """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: myapp
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: myapp
                  template:
                    metadata:
                      labels:
                        app: myapp
                    spec:
                      containers:
                      - name: myapp
                        image: myapp:latest
                        ports:
                        - containerPort: 8080
                        resources:
                          requests:
                            memory: "512Mi"
                            cpu: "500m"
                          limits:
                            memory: "1Gi"
                            cpu: "1000m"
                        livenessProbe:
                          httpGet:
                            path: /actuator/health
                            port: 8080
                          initialDelaySeconds: 60
                          periodSeconds: 10
                        readinessProbe:
                          httpGet:
                            path: /actuator/health
                            port: 8080
                          initialDelaySeconds: 30
                          periodSeconds: 5
                """;
        }
    }

    /**
     * Blue-green deployment strategy
     * 
     * Benefits:
     * - Zero downtime deployment
     * - Easy rollback
     * - Testing in production
     */
    public static class BlueGreenDeployment {
        
        public static String getKubernetesConfig() {
            return """
                # Blue deployment
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: myapp-blue
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: myapp
                      version: blue
                  template:
                    metadata:
                      labels:
                        app: myapp
                        version: blue
                    spec:
                      containers:
                      - name: myapp
                        image: myapp:blue
                        ports:
                        - containerPort: 8080
                
                # Green deployment
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: myapp-green
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: myapp
                      version: green
                  template:
                    metadata:
                      labels:
                        app: myapp
                        version: green
                    spec:
                      containers:
                      - name: myapp
                        image: myapp:green
                        ports:
                        - containerPort: 8080
                
                # Service to switch between blue and green
                apiVersion: v1
                kind: Service
                metadata:
                  name: myapp
                spec:
                  selector:
                    app: myapp
                    version: blue  # Switch to green for new deployment
                  ports:
                  - port: 80
                    targetPort: 8080
                """;
        }
    }

    /**
     * Canary deployment strategy
     * 
     * Benefits:
     * - Gradual rollout
     * - Risk mitigation
     * - Real user testing
     */
    public static class CanaryDeployment {
        
        public static String getIstioConfig() {
            return """
                apiVersion: networking.istio.io/v1alpha3
                kind: VirtualService
                metadata:
                  name: myapp
                spec:
                  hosts:
                  - myapp
                  http:
                  - match:
                    - headers:
                        x-canary:
                          exact: "true"
                    route:
                    - destination:
                        host: myapp
                        subset: v2
                  - route:
                    - destination:
                        host: myapp
                        subset: v1
                      weight: 90
                    - destination:
                        host: myapp
                        subset: v2
                      weight: 10
                """;
        }
    }

    /**
     * Main method to display all optimization examples
     */
    public static void main(String[] args) {
        System.out.println("=== Build & Deployment Optimization ===\n");
        
        System.out.println("1. Docker Multi-stage Build:");
        System.out.println(DockerOptimization.getMultiStageDockerfile());
        
        System.out.println("\n2. Maven Optimization:");
        System.out.println(MavenOptimization.getMavenPluginConfig());
        
        System.out.println("\n3. Gradle Optimization:");
        System.out.println(GradleOptimization.getGradleConfig());
    }
}

/*
 * MAVEN COMMANDS FOR OPTIMIZATION:
 * 
 * # Parallel builds
 * mvn -T 4 clean install
 * 
 * # Skip tests
 * mvn clean install -DskipTests
 * 
 * # Offline mode
 * mvn -o clean install
 * 
 * # Build specific module
 * mvn -pl module-name clean install
 * 
 * # Also build dependencies
 * mvn -pl module-name -am clean install
 * 
 * # Build with profile
 * mvn -Pproduction clean install
 * 
 * # Build native image
 * mvn -Pnative native:compile
 */

/*
 * GRADLE COMMANDS FOR OPTIMIZATION:
 * 
 * # Parallel execution
 * gradle build --parallel
 * 
 * # Configuration cache
 * gradle build --configuration-cache
 * 
 * # Build cache
 * gradle build --build-cache
 * 
 * # Dry run
 * gradle build --dry-run
 * 
 * # Continuous build
 * gradle build --continuous
 * 
 * # Build specific task
 * gradle :module-name:build
 * 
 * # Build native image
 * gradle nativeCompile
 */

/*
 * DOCKER COMMANDS FOR OPTIMIZATION:
 * 
 * # Build with BuildKit
 * DOCKER_BUILDKIT=1 docker build -t myapp .
 * 
 * # Multi-stage build
 * docker build --target build -t myapp-build .
 * 
 * # Use layer caching
 * docker build --cache-from myapp:latest -t myapp .
 * 
 * # Scan for vulnerabilities
 * docker scan myapp:latest
 * 
 * # Export layered JAR
 * java -Djarmode=layertools -jar app.jar extract
 */

/*
 * JVM OPTIONS FOR PRODUCTION:
 * 
 * java -Xms2g -Xmx2g \\
 *      -XX:+UseG1GC \\
 *      -XX:MaxGCPauseMillis=200 \\
 *      -XX:+UseStringDeduplication \\
 *      -XX:MetaspaceSize=256m \\
 *      -XX:MaxMetaspaceSize=512m \\
 *      -XX:+HeapDumpOnOutOfMemoryError \\
 *      -XX:HeapDumpPath=/var/log/app/ \\
 *      -jar app.jar
 */
