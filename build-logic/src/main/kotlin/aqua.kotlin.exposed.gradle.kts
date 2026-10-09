/*
 * 约定插件：Exposed(1.x) 接入。
 *
 * 用途：JPA -> Exposed 迁移期间，让「已迁移模块」用 Exposed 落库，同时不影响仍是 JPA 的模块。
 *
 * 设计要点（完整背景见仓库根 AquaRush-JPA-to-Exposed-迁移方案.md §5.1）：
 * 1. 不应用 kotlin("plugin.jpa") / allopen / noarg —— Exposed DSL 不需要实体代理。
 * 2. 不引入 org.jetbrains.exposed:exposed-spring-boot4-starter：其自动配置会把
 *    SpringTransactionManager 注册成主事务管理器，与 Spring Boot 的 JpaTransactionManager
 *    争抢 @Transactional 解析。当前仓库绝大多数代码仍是 JPA，主事务管理器必须留给 JPA。
 *    迁移期的 Exposed Database 由 aqua-foundation 的 ExposedAutoConfiguration 统一提供。
 * 3. 只声明依赖；Database/事务边界策略由 foundation 的自动配置与各模块自行定义。
 *
 * 实现注意：预编译脚本插件里**没有** `libs` 访问器（included build 不生成），所以不能用
 * `libs.bundles.exposed`。这里用 VersionCatalogsExtension 读同一份 gradle/libs.versions.toml，
 * 避免把版本号在插件里再写一遍造成漂移。
 */
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("aqua.kotlin.spring")
}

val exposedLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  implementation(exposedLibs.findLibrary("exposed-core").get())
  implementation(exposedLibs.findLibrary("exposed-jdbc").get())
  implementation(exposedLibs.findLibrary("exposed-java-time").get())
  implementation(exposedLibs.findLibrary("exposed-json").get())
  implementation(exposedLibs.findLibrary("exposed-spring-transaction").get())
}
