plugins {
    id("aqua.kotlin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}
