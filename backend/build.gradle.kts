plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.flywaydb.flyway") version "10.18.0"
}

group = "com.startrace"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // BCrypt password encoder (不引入完整的 spring-security)
    implementation("org.springframework.security:spring-security-crypto")

    // Sa-Token
    implementation("cn.dev33:sa-token-spring-boot3-starter:1.38.0")
    implementation("cn.dev33:sa-token-redis-jackson:1.38.0")
    implementation("cn.dev33:sa-token-jwt:1.38.0")

    // MyBatis-Plus
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")

    // Flyway
    implementation("org.flywaydb:flyway-core:10.18.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Redis
    implementation("org.apache.commons:commons-pool2")

    // API Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 输出 Spring Boot 构建信息
springBoot {
    buildInfo()
}
