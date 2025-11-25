# Build Logic - Gradle Convention Plugins

This directory contains Gradle Convention Plugins for managing multi-module builds in the AquaRush project.

## 📁 Structure

```
build-logic/
├── settings.gradle.kts              # Build logic settings
└── convention/
    ├── build.gradle.kts             # Convention plugins configuration
    └── src/main/kotlin/             # Plugin implementations
        ├── KotlinCommonConventionPlugin.kt
        ├── KotlinSpringConventionPlugin.kt
        ├── KotlinJpaConventionPlugin.kt
        ├── SpringBootApplicationConventionPlugin.kt
        └── SpringBootLibraryConventionPlugin.kt
```

## 🎯 Convention Plugins

### 1. `aqua.kotlin.common`
**Base Kotlin configuration for all modules**

- Applies `org.jetbrains.kotlin.jvm` plugin
- Configures Java 21 compatibility
- Sets up Kotlin compiler options (JSR-305, JVM target)
- Configures JUnit Platform for tests
- Adds common Kotlin dependencies

**Usage:**
```kotlin
plugins {
    id("aqua.kotlin.common")
}
```

### 2. `aqua.kotlin.spring`
**Spring-specific Kotlin configuration**

- Extends `aqua.kotlin.common`
- Applies `kotlin-spring` and `spring-dependency-management` plugins
- Adds Spring Boot and Jackson Kotlin dependencies
- Adds Spring Boot test dependencies

**Usage:**
```kotlin
plugins {
    id("aqua.kotlin.spring")
}
```

### 3. `aqua.kotlin.jpa`
**JPA-specific Kotlin configuration**

- Extends `aqua.kotlin.spring`
- Applies `kotlin-jpa` plugin
- Adds Spring Data JPA dependencies
- Adds PostgreSQL driver

**Usage:**
```kotlin
plugins {
    id("aqua.kotlin.jpa")
}
```

### 4. `aqua.spring.boot.library`
**For library modules (non-executable)**

- Extends `aqua.kotlin.jpa`
- Disables `bootJar` task
- Enables `jar` task for plain JAR generation

**Usage:**
```kotlin
plugins {
    id("aqua.spring.boot.library")
}
```

**Applicable to:**
- common-module
- user-module
- product-module
- order-module
- delivery-module
- payment-module
- statistics-module

### 5. `aqua.spring.boot.application`
**For executable Spring Boot applications**

- Extends `aqua.kotlin.jpa`
- Applies `org.springframework.boot` plugin
- Configures `bootJar` task
- Adds application-specific dependencies (Web, Actuator, Liquibase)

**Usage:**
```kotlin
plugins {
    id("aqua.spring.boot.application")
}
```

**Applicable to:**
- entry-module

## 📦 Version Catalog

All dependencies are centrally managed in `gradle/libs.versions.toml`:

### Versions
- Kotlin: 1.9.21
- Spring Boot: 3.2.1
- Java: 21
- Liquibase: 4.25.0
- WeChat Pay SDK: 0.5.0

### Usage Examples

```kotlin
dependencies {
    // Single library
    implementation(libs.spring.boot.starter.web)
    
    // Bundle of libraries
    implementation(libs.bundles.kotlin)
    
    // Plugin reference
    plugins {
        alias(libs.plugins.kotlin.jvm)
    }
}
```

## 🔧 Benefits

### 1. **DRY (Don't Repeat Yourself)**
- Common configuration defined once
- Applied consistently across all modules
- Reduces build script duplication by ~80%

### 2. **Centralized Dependency Management**
- All versions in one place (`libs.versions.toml`)
- Type-safe dependency references
- Easy version upgrades

### 3. **Type Safety**
- IDE autocomplete for dependencies
- Compile-time validation
- Refactoring support

### 4. **Maintainability**
- Changes propagate to all modules automatically
- Clear separation between application and library modules
- Easier to understand build configuration

### 5. **Consistency**
- Same compiler settings across all modules
- Uniform test configuration
- Standardized dependency versions

## 📊 Before & After Comparison

### Before (Traditional approach)
```kotlin
// build.gradle.kts (51 lines of repetitive configuration)
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }
    
    // ... 30+ more lines
}
```

### After (Convention Plugins)
```kotlin
// build.gradle.kts (clean and minimal)
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spring.boot) apply false
}

group = "dev.yidafu.aqua"
version = "0.1.0-SNAPSHOT"
```

```kotlin
// modules/user-module/build.gradle.kts (concise)
plugins {
    id("aqua.spring.boot.library")
}

dependencies {
    implementation(project(":modules:common-module"))
    implementation(libs.spring.boot.starter.security)
}
```

## 🚀 Adding New Dependencies

### 1. Add to Version Catalog
Edit `gradle/libs.versions.toml`:

```toml
[versions]
jwt = "0.12.3"

[libraries]
jwt-api = { module = "io.jsonwebtoken:jjwt-api", version.ref = "jwt" }
jwt-impl = { module = "io.jsonwebtoken:jjwt-impl", version.ref = "jwt" }
```

### 2. Use in Module
```kotlin
dependencies {
    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
}
```

## 🔍 Troubleshooting

### Sync Issues
```bash
./gradlew --stop
./gradlew clean build
```

### Plugin Not Found
Ensure `build-logic` is included in root `settings.gradle.kts`:
```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

### Version Catalog Errors
Check that `gradle/libs.versions.toml` exists and is valid TOML format.

## 📚 References

- [Gradle Convention Plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
- [Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

---

**Built with best practices for maintainable Gradle builds** 🎯
