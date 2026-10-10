/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import java.io.File

/**
 * GraphQL 请求类 / 返回类的类名后缀约束（architecture.md §Type Naming Conventions）。
 *
 * 约定：
 *  - 请求(入参)类后缀 `Input` / `Request`
 *  - 返回类后缀 `Vo` / `Page` / `Response` / `Result`（`VoPage` 由 `Page` 覆盖）
 *  - `VO` 拼写禁止：codegen(change-case) 一律把 schema 的尾缀规范化为 `Vo`，
 *    schema 文本也统一 `Vo`/`VoPage`（G6 直接校验 .graphqls 源文件，字节码侧无法区分 VO/Vo）
 *
 * 判别方式：G1–G5 依赖字节码，G6 直接读 .graphqls 源文件（见下）。
 * 字节码侧的判别规则：
 *  - 生成包中带 `(Map)` 次构造器的类 = graphql-codegen 为 GraphQL `input` 生成的请求类
 *    （codegen 同时为每个根字段生成 `Mutation*Args`/`Query*Args` 包装类，按名字豁免）；
 *    其余 data class 即输出类型。
 *  - resolver 中 `@QueryMapping`/`@MutationMapping`/`@SchemaMapping` 方法的
 *    `@Argument` 参数是请求类，返回类型是返回类。
 */
private val REQUEST_SUFFIXES = listOf("Input", "Request")

private val RESPONSE_SUFFIXES = listOf("Vo", "Page", "Response", "Result")

private const val GENERATED_PACKAGE = "dev.yidafu.aqua.common.graphql.generated"

/** 根操作类型由 schema 的 Query/Mutation 定义，不参与后缀约束。 */
private val ROOT_OPERATION_TYPES = setOf("Query", "Mutation", "Subscription")

/**
 * JDK / 框架 / 标量类型既不是项目请求类也不是返回类；
 * 容器 `kotlin.collections.*`、`org.springframework.data.domain.Page` 也在此列。
 */
private val EXTERNAL_TYPE_PREFIXES =
  listOf(
    "java.",
    "javax.",
    "jakarta.",
    "kotlin.",
    "org.springframework.",
    "tools.jackson.",
    "com.fasterxml.",
    "graphql.",
  )

private fun isProjectType(clazz: JavaClass): Boolean =
  !clazz.isEnum &&
    !clazz.isPrimitive &&
    EXTERNAL_TYPE_PREFIXES.none { clazz.fullName.startsWith(it) }

/** 展开 `List<Xxx>` / `Map<K, V>` / 可空类型，取出真正需要约束的项目类型。 */
private fun projectTypesIn(types: Collection<JavaClass>): List<JavaClass> = types.filter(::isProjectType)

/**
 * Kotlin 编译期合成类（`Foo$Companion`、`Foo$WhenMappings`）不是业务类型，
 * 不参与后缀约束——它们的 simpleName 只有 `Companion`，会误报。
 */
private fun isSyntheticType(clazz: JavaClass): Boolean = '$' in clazz.fullName

private fun suffixViolations(
  types: Collection<JavaClass>,
  allowed: List<String>,
): List<String> =
  projectTypesIn(types)
    .filterNot(::isSyntheticType)
    .filterNot { type -> allowed.any(type.simpleName::endsWith) }
    .map { it.fullName }

/** resolver 中所有 GraphQL 入口方法（Query / Mutation / Schema 映射）。 */
private fun graphQlMethods(): Sequence<JavaMethod> =
  backendClasses
    .asSequence()
    .flatMap { it.methods.asSequence() }
    .filter { method ->
      method.isAnnotatedWith(QueryMapping::class.java) ||
        method.isAnnotatedWith(MutationMapping::class.java) ||
        method.isAnnotatedWith(SchemaMapping::class.java)
    }

/**
 * graphql-codegen 为每个 GraphQL `input` 生成 `(args: Map<...>)` 次构造器，
 * 输出类型没有该构造器；`*Args` 是根字段参数包装类，不参与请求类约束。
 */
private fun JavaClass.isGeneratedInputType(): Boolean =
  constructors.any { ctor ->
    ctor.rawParameterTypes.size == 1 && ctor.rawParameterTypes.single().fullName == "java.util.Map"
  }

private fun JavaClass.isArgsWrapper(): Boolean = simpleName.endsWith("Args")

/**
 * GraphQL 边界可达的全部项目类型：从 resolver 的 `@Argument` 参数与返回类型出发，
 * 顺着字段类型继续下钻，得到"实际参与 GraphQL 序列化/反序列化"的类集合。
 * 用于把 dto 包规则(G5)限定在 GraphQL 相关类上，避免误伤微信记录、通知事件等非 GraphQL 类。
 */
private fun graphQlParticipatingTypes(): Set<JavaClass> {
  val seeds: List<JavaClass> =
    graphQlMethods()
      .toList()
      .flatMap { method ->
        val fromParams =
          method.parameters
            .filter { it.isAnnotatedWith(Argument::class.java) }
            .flatMap { param -> param.type.allInvolvedRawTypes.toList() }
        fromParams + method.returnType.allInvolvedRawTypes
      }.filter(::isProjectType)

  val visited = LinkedHashSet<JavaClass>()
  val queue: ArrayDeque<JavaClass> = ArrayDeque(seeds)
  while (queue.isNotEmpty()) {
    val current = queue.removeFirst()
    if (!visited.add(current)) continue
    current.fields.forEach { field ->
      field.rawType.allInvolvedRawTypes
        .filter(::isProjectType)
        .forEach { next -> if (next !in visited) queue.add(next) }
    }
  }
  return visited
}

/**
 * schema 源文件根目录（相对 services/aqua-admin 模块目录 = Gradle 测试 workingDir）：
 * admin main/test schema、shared-config 共享类型、client schema。
 * codegen(change-case) 会把 `OrderVO` 规范成 `OrderVo`，字节码无法区分 VO/Vo，
 * 因此 G6 必须直接校验 .graphqls 文本。
 */
private val SCHEMA_SOURCE_ROOTS =
  listOf(
    "src/main/resources/graphql",
    "src/test/resources/graphql",
    "../../shared-config/graphql",
    "../aqua-client/src/main/resources/graphql",
  )

private fun schemaSourceFiles(): List<File> =
  SCHEMA_SOURCE_ROOTS
    .flatMap { root -> File(root).walkTopDown().toList() }
    .filter { it.isFile && it.extension == "graphqls" }

/** 匹配 `type Foo` / `extend type Foo` / `interface` / `union` 定义行（行首，忽略前导空白）。 */
private val SCHEMA_TYPE_PATTERN = Regex("""^(?:extend\s+)?(?:type|interface|union)\s+(\w+)""")

private fun report(
  rule: String,
  violations: List<String>,
) {
  if (violations.isNotEmpty()) {
    fail("$rule ${violations.size} violations:\n${violations.distinct().sorted().joinToString("\n")}")
  }
}

class GraphQLNamingTest :
  FunSpec({
    test("G1 resolver @Argument 请求类必须以 Input/Request 结尾") {
      val violations =
        graphQlMethods()
          .flatMap { method ->
            method.parameters
              .asSequence()
              .filter { it.isAnnotatedWith(Argument::class.java) }
              .flatMap { param ->
                suffixViolations(param.type.allInvolvedRawTypes, REQUEST_SUFFIXES).asSequence()
              }.map { "${method.fullName} -> $it" }
          }.toList()
      report("G1", violations)
    }

    test("G2 resolver 返回类必须以 Vo/Page/Response/Result 结尾") {
      val violations =
        graphQlMethods()
          .flatMap { method ->
            suffixViolations(method.returnType.allInvolvedRawTypes, RESPONSE_SUFFIXES)
              .asSequence()
              .map { "${method.fullName} -> $it" }
          }.toList()
      report("G2", violations)
    }

    test("G3 生成包 input 类必须以 Input/Request 结尾") {
      val violations =
        backendClasses
          .filter {
            it.packageName == GENERATED_PACKAGE &&
              it.isGeneratedInputType() &&
              !it.isArgsWrapper()
          }.flatMap { suffixViolations(listOf(it), REQUEST_SUFFIXES) }
      report("G3", violations)
    }

    test("G4 生成包输出类必须以 Vo/Page/Response/Result 结尾") {
      val violations =
        backendClasses
          .filter {
            it.packageName == GENERATED_PACKAGE &&
              !it.isGeneratedInputType() &&
              !it.isArgsWrapper() &&
              it.simpleName !in ROOT_OPERATION_TYPES
          }.flatMap { suffixViolations(listOf(it), RESPONSE_SUFFIXES) }
      report("G4", violations)
    }

    test("G5 参与 GraphQL 的 dto 类必须携带明确的请求/返回后缀") {
      val allowed = REQUEST_SUFFIXES + RESPONSE_SUFFIXES + listOf("DTO", "Dto", "Info", "Row")
      val violations =
        graphQlParticipatingTypes()
          .filter { "dto" in it.packageName.split('.') }
          .flatMap { suffixViolations(listOf(it), allowed) }
      report("G5", violations)
    }

    test("G6 schema 输出类型名必须以 Vo/VoPage 结尾(禁止 VO 拼写)") {
      val violations =
        schemaSourceFiles()
          .flatMap { file ->
            file
              .readLines()
              .mapNotNull { line -> SCHEMA_TYPE_PATTERN.find(line.trim())?.groupValues?.get(1) }
              .filterNot { it in ROOT_OPERATION_TYPES }
              .filterNot { it.endsWith("Vo") || it.endsWith("VoPage") }
              .map { "${file.path}: $it" }
          }
      report("G6", violations)
    }

    test("哨兵: 规则确实覆盖生成包与 resolver(防空转)") {
      (backendClasses.count { it.packageName == GENERATED_PACKAGE } > 100) shouldBe true
      (graphQlMethods().count() > 200) shouldBe true
      (schemaSourceFiles().size > 20) shouldBe true
    }
  })
