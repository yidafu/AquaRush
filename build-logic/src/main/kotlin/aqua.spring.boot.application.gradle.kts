plugins {
    id("aqua.kotlin.jpa")
    id("org.springframework.boot")
}

// Configure bootJar task
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
}
