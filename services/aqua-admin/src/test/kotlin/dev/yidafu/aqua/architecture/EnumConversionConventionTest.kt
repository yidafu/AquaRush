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

import io.kotest.core.spec.style.FunSpec
import java.io.File

/**
 * 枚举转换约定守卫(枚举互转不再依赖运行时 `valueOf` 字符串匹配)。
 *
 * 规则:
 * - R1 main 源码禁止 `Xxx.valueOf(...)` 枚举互转调用:
 *   - 领域/GraphQL 枚举互转用 `toGraphQL()`/`toDomain()`(mapper 扩展,`when` 穷举,
 *     任一侧改名/增删取值即编译失败,而不是线上抛 `IllegalArgumentException`);
 *   - DB 列值/查询参数/事件 payload 字符串解析用枚举伴生 `fromValue`(显式 `when` + else 抛明确异常)。
 * - 白名单:数值类型与 `String` 的 `valueOf` 是 JDK API,不涉及枚举匹配;
 *   生成代码(`graphql-gen/`)由 codegen 产生,不参与本守卫。
 *
 * 为什么是源码扫描而不是 ArchUnit:需要区分 `valueOf` 的接收者类型(数值 vs 枚举),
 * 字节码层面不可靠地还原源码语义,直接扫源码 + 白名单更简单可控。
 *
 * 扫描范围:仓库 main 源码(services/aqua-admin 测试的 workingDir = services/aqua-admin,仓库根 = ../..)。
 */
private val REPO_ROOT = File("../..").canonicalFile

private val SCAN_ROOTS = listOf("modules", "services")

private val EXCLUDED_PATH_SEGMENTS =
  listOf("/build/", "/.worktrees/", "/node_modules/", "/src/test/", "/.git/", "/graphql-gen/")

/** JDK 数值/字符串类型的 `valueOf` 属于白名单,不涉及枚举名匹配。 */
private val ALLOWED_RECEIVERS =
  setOf("BigDecimal", "BigInteger", "Integer", "Long", "Double", "Float", "Short", "Byte", "Boolean", "Character", "String")

private val VALUE_OF_PATTERN = Regex("""(\w+)\s*\.\s*valueOf\s*\(""")

private fun kotlinSources(): List<File> =
  SCAN_ROOTS
    .flatMap { root -> File(REPO_ROOT, root).walkTopDown().toList() }
    .filter { it.isFile && it.extension == "kt" }
    .filterNot { file -> EXCLUDED_PATH_SEGMENTS.any { it in file.invariantSeparatorsPath } }
    .sortedBy { it.invariantSeparatorsPath }

private fun relative(file: File): String = file.relativeTo(REPO_ROOT).invariantSeparatorsPath

private fun lineOf(
  src: String,
  index: Int,
): Int = src.take(index).count { it == '\n' } + 1

class EnumConversionConventionTest :
  FunSpec({
    test("R1 main 源码禁止枚举 valueOf 互转(用 toGraphQL/toDomain/fromValue)") {
      val violations =
        kotlinSources().flatMap { file ->
          val src = file.readText()
          VALUE_OF_PATTERN
            .findAll(src)
            .filter { it.groupValues[1] !in ALLOWED_RECEIVERS }
            .map { m -> "${relative(file)}:${lineOf(src, m.range.first)} ${m.groupValues[1]}.valueOf(...)" }
            .toList()
        }
      if (violations.isNotEmpty()) {
        error(
          "R1 违反 ${violations.size} 处(枚举互转请用 toGraphQL/toDomain,字符串解析用 fromValue):\n" +
            violations.sorted().joinToString("\n"),
        )
      }
    }
  })
