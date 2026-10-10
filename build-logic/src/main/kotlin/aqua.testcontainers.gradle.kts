/*
 * 约定插件：Testcontainers 真实 PostgreSQL 测试支撑。
 *
 * 背景：测试栈已从 H2 全面切到 Testcontainers 真实 PostgreSQL。
 * H2 的 `MODE=PostgreSQL` 兼容模式覆盖不到 `INSERT ... ON CONFLICT`(upsert)、
 * jsonString 列绑定、varchar 长度校验、类型转换、并发行锁等 PG 语义 ——
 * 「H2 单线程验证」不背书生产 PG 行为。仓储/集成测试一律打真实 PG。
 *
 * 本插件只做两件事：
 * 1. 给模块加 `org.testcontainers:testcontainers` + `:postgresql` 测试依赖；
 * 2. 把模块的 Test 任务接到本机容器运行时。
 *
 * 容器运行时（macOS 开发机）：
 *   本机没有 Docker Desktop，只有 podman。未显式设置 DOCKER_HOST 且 podman machine
 *   socket 存在时回退到它，并禁用 ryuk —— rootless podman 无法把宿主 socket 挂进
 *   ryuk 清理容器，容器改由 Testcontainers 的 JVM shutdown hook 清理。
 *   CI / 装了 Docker 的机器命中默认 socket，不受影响。
 *
 * 镜像：默认 `postgres:17-alpine`，可用环境变量 `AQUA_TEST_PG_IMAGE` 覆盖
 *   （海外/CI 可指向 mirror）。该值以系统属性 `aqua.test.pg.image` 下发给测试 JVM，
 *   由 foundation testFixtures 的 PostgresTestContainer 读取 —— 单一默认值出处。
 *
 * 注意：镜像必须是 `postgres` 仓库名 —— Testcontainers 的 compatible-substitute
 *   校验会拒绝 mirror 全名。mirror 场景请用 `AQUA_TEST_PG_IMAGE` 指向本地已打好
 *   `postgres:<tag>` 标签的镜像。
 */
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("aqua.kotlin.common")
}

val testcontainersLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  testImplementation(testcontainersLibs.findLibrary("testcontainers-core").get())
  testImplementation(testcontainersLibs.findLibrary("testcontainers-postgresql").get())
}

// 单一默认值出处：Kotlin 测试代码（PostgresTestContainer）读系统属性 `aqua.test.pg.image`。
// 走 Gradle 时由这里下发，IDE 里直跑测试时回退到环境变量 AQUA_TEST_PG_IMAGE / 默认值。
val pgImage: String = System.getenv("AQUA_TEST_PG_IMAGE") ?: "postgres:17-alpine"

// podman machine socket（macOS）。不存在（Docker / Linux CI）时整段跳过。
val podmanSocket = file("${System.getProperty("user.home")}/.local/share/containers/podman/machine/podman.sock")

tasks.withType<Test>().configureEach {
  systemProperty("aqua.test.pg.image", pgImage)

  if (System.getenv("DOCKER_HOST").isNullOrBlank() && podmanSocket.exists()) {
    val host = "unix://${podmanSocket.absolutePath}"
    environment("DOCKER_HOST", host)
    systemProperty("docker.host", host)
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
  }
}
