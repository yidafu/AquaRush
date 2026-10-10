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
 * 软删查询约定守卫（`SoftDeletableTable` 的过滤不再靠自觉）。
 *
 * 规则：
 * - R1 软删表的查询入口 `XxxTable.selectAll()` / `XxxTable.select(...)` 返回的 Query 已经带
 *   `is_deleted = false`，而 Exposed 的 `Query.where` 只能调用一次（第二次抛
 *   `IllegalStateException: WHERE clause is specified twice`）。因此入口链上**禁止**出现 `.where`，
 *   追加条件必须用 `andWhere {}`；需要读到已删行的唯一出口是 `selectAllIncludingDeleted()`（那里
 *   用的是原生 Query，`.where` 合法）。
 * - R2 main 源码禁止手写 `XxxTable.isDeleted eq false`（过滤已固定在入口上，重复写会掩盖真正的口径）。
 * - R3 软删表禁止硬删（`deleteWhere`/`deleteAll`/`deleteReturning`），软删用 `softDelete {}`；
 *   确属业务需要的物理删除，同样在语句上方写 `软删豁免:` 注释说明理由。
 * - R4 join 链上出现的每个软删表都必须显式 `T.activeOnly {}`（Exposed 的 `Join.joinParts` 是 internal，
 *   无法自动枚举参与 join 的表）；确属故意豁免时，在语句上方写 `软删豁免:` 注释说明理由。
 *
 * 为什么是源码扫描而不是 ArchUnit：字节码里扩展调用的接收者只是静态方法
 * `QueriesKt.selectAll(FieldSet)` 的第一个实参，ArchUnit 不暴露调用实参，无法按接收者类型判定。
 *
 * 扫描范围：仓库 main 源码（services/aqua-admin 测试的 workingDir = services/aqua-admin，仓库根 = ../..）。
 */
private val REPO_ROOT = File("../..").canonicalFile

private val SCAN_ROOTS = listOf("modules", "services")

private val EXCLUDED_PATH_SEGMENTS = listOf("/build/", "/.worktrees/", "/node_modules/", "/src/test/", "/.git/")

private const val EXEMPTION_MARKER = "软删豁免"

private val SOFT_TABLE_PATTERN = Regex("""object\s+(\w+)\s*:\s*(?:SoftDeletableTable|SoftDeleteFlagTable)\s*\(""")

private val CHAIN_ENTRY_PATTERN = Regex("""\.\s*(selectAll|select)\s*[({]""")

private val JOIN_METHODS = listOf("join", "innerJoin", "leftJoin", "rightJoin", "fullJoin", "crossJoin")

private val HARD_DELETE_METHODS = listOf("deleteWhere", "deleteAll", "deleteReturning")

private fun kotlinSources(): List<File> =
  SCAN_ROOTS
    .flatMap { root -> File(REPO_ROOT, root).walkTopDown().toList() }
    .filter { it.isFile && it.extension == "kt" }
    .filterNot { file -> EXCLUDED_PATH_SEGMENTS.any { it in file.invariantSeparatorsPath } }
    .sortedBy { it.invariantSeparatorsPath }

private fun relative(file: File): String = file.relativeTo(REPO_ROOT).invariantSeparatorsPath

private fun softTableNames(sources: List<File>): Set<String> =
  sources
    .flatMap { SOFT_TABLE_PATTERN.findAll(it.readText()).map { m -> m.groupValues[1] }.toList() }
    .toSet()

private fun chainEntryPattern(softTables: Set<String>): Regex =
  Regex("""\b(${softTables.joinToString("|")})\s*(?=\.\s*(?:selectAll|select)\s*[({])""")

private fun joinEntryPattern(softTables: Set<String>): Regex =
  Regex("""\b(${softTables.joinToString("|")})\s*(?=\.\s*(?:${JOIN_METHODS.joinToString("|")})\s*[({])""")

private fun joinArgPattern(): Regex = Regex("""\.\s*(?:${JOIN_METHODS.joinToString("|")})\s*\(\s*(\w+)""")

/** 链式调用的一次 `.name(...)` / `.name { ... }`。 */
private class ChainCall(
  val name: String,
  val start: Int,
  val end: Int,
  val argStart: Int?,
  val argEnd: Int?,
  val blockStart: Int?,
  val blockEnd: Int?,
)

/** 从 [open]（`(`/`{`/`[`）出发返回配对的闭括号下标，跳过字符串与注释。 */
private fun matchBracket(
  src: String,
  open: Int,
): Int {
  var depth = 0
  var i = open
  while (i < src.length) {
    val c = src[i]
    when {
      c == '"' && src.startsWith("\"\"\"", i) -> {
        i = src.indexOf("\"\"\"", i + 3).let { if (it < 0) src.length - 1 else it + 2 }
      }

      c == '"' -> {
        i++
        while (i < src.length && src[i] != '"') {
          if (src[i] == '\\') i++
          i++
        }
      }

      c == '\'' -> {
        i++
        while (i < src.length && src[i] != '\'') {
          if (src[i] == '\\') i++
          i++
        }
      }

      c == '/' && i + 1 < src.length && src[i + 1] == '/' -> {
        while (i < src.length && src[i] != '\n') i++
      }

      c == '/' && i + 1 < src.length && src[i + 1] == '*' -> {
        i = src.indexOf("*/", i + 2).let { if (it < 0) src.length - 1 else it + 1 }
      }

      c == '(' || c == '{' || c == '[' -> depth++

      c == ')' || c == '}' || c == ']' -> {
        depth--
        if (depth == 0) return i
        check(depth >= 0) { "unbalanced bracket at $i of ${src.length} chars" }
      }
    }
    i++
  }
  error("unbalanced bracket from offset $open")
}

/** 解析 [from] 起的 `.name(...)` / `.name { ... }` 链。 */
private fun parseChain(
  src: String,
  from: Int,
): List<ChainCall> {
  val calls = mutableListOf<ChainCall>()
  var i = from
  while (true) {
    var j = i
    while (j < src.length && src[j].isWhitespace()) j++
    if (j >= src.length || src[j] != '.') return calls
    j++
    while (j < src.length && src[j].isWhitespace()) j++
    val nameMatch = Regex("""[A-Za-z_]\w*""").find(src, j) ?: return calls
    if (nameMatch.range.first != j) return calls
    val name = nameMatch.value
    j = nameMatch.range.last + 1
    var k = j
    while (k < src.length && src[k].isWhitespace()) k++
    var argStart: Int? = null
    var argEnd: Int? = null
    var blockStart: Int? = null
    var blockEnd: Int? = null
    var end = j
    if (k < src.length && src[k] == '(') {
      val close = matchBracket(src, k)
      argStart = k + 1
      argEnd = close
      end = close + 1
    } else if (k < src.length && src[k] == '{') {
      val close = matchBracket(src, k)
      blockStart = k + 1
      blockEnd = close
      end = close + 1
    }
    calls += ChainCall(name, j, end, argStart, argEnd, blockStart, blockEnd)
    i = end
  }
}

private fun lineOf(
  src: String,
  index: Int,
): Int = src.take(index).count { it == '\n' } + 1

/** 取 [index] 之前 [count] 行文本（用于识别 `软删豁免:` 标记）。 */
private fun precedingLines(
  src: String,
  index: Int,
  count: Int = 4,
): String {
  var from = index
  repeat(count) {
    val prev = src.lastIndexOf('\n', (from - 1).coerceAtLeast(0))
    if (prev <= 0) return src.substring(0, index)
    from = prev
  }
  return src.substring(from, index)
}

/** R1：软删表入口链上出现 `.where`。 */
private fun whereOnEntryViolations(
  file: File,
  src: String,
  softTables: Set<String>,
): List<String> {
  val entryPattern = chainEntryPattern(softTables)
  val out = mutableListOf<String>()
  entryPattern.findAll(src).forEach { m ->
    val entry = CHAIN_ENTRY_PATTERN.find(src, m.range.last + 1) ?: return@forEach
    if (src.substring(m.range.last + 1, entry.range.first).isNotBlank()) return@forEach
    parseChain(src, entry.range.last + 1)
      .filter { it.name == "where" }
      .forEach {
        out += "${relative(file)}:${lineOf(src, it.start)} ${m.groupValues[1]}.${entry.groupValues[1]}().where{} → 用 andWhere"
      }
  }
  return out
}

/** R4：join 链上未显式 activeOnly 的软删表。 */
private fun joinChainViolations(
  file: File,
  src: String,
  softTables: Set<String>,
): List<String> {
  val out = mutableListOf<String>()
  joinEntryPattern(softTables).findAll(src).forEach { m ->
    val line = lineOf(src, m.range.first)
    if (EXEMPTION_MARKER in precedingLines(src, m.range.first)) return@forEach
    val calls = parseChain(src, m.range.last + 1)
    if (calls.isEmpty()) return@forEach
    val joinSegment = src.substring(m.range.last + 1, calls.last().end)
    val participants =
      (
        setOf(m.groupValues[1]) +
          joinArgPattern().findAll(joinSegment).map { it.groupValues[1] }.filter { it in softTables }
      ).sorted()
    val whereCall = calls.firstOrNull { it.name == "where" }
    val whereBody =
      if (whereCall?.blockStart != null && whereCall.blockEnd != null) {
        src.substring(whereCall.blockStart, whereCall.blockEnd)
      } else {
        ""
      }
    participants
      .filterNot { table -> Regex("""\b${Regex.escape(table)}\s*\.\s*activeOnly""").containsMatchIn(whereBody) }
      .forEach { table -> out += "${relative(file)}:$line join 链 $table 未附加 activeOnly" }
  }
  return out
}

private fun report(
  rule: String,
  violations: List<String>,
) {
  if (violations.isNotEmpty()) {
    error("$rule 违反 ${violations.size} 处:\n${violations.distinct().sorted().joinToString("\n")}")
  }
}

class SoftDeleteQueryConventionTest :
  FunSpec({
    val sources by lazy { kotlinSources() }
    val softTables by lazy { softTableNames(sources) }

    test("R1 软删表查询入口链禁止 .where(追加条件用 andWhere;含已删行走 selectAllIncludingDeleted)") {
      report(
        "R1",
        sources.flatMap { file ->
          val src = file.readText()
          if ("selectAll" !in src && ".select" !in src) emptyList() else whereOnEntryViolations(file, src, softTables)
        },
      )
    }

    test("R2 main 源码禁止手写 isDeleted eq false(过滤已固定在查询入口)") {
      val pattern = Regex("""\bisDeleted\s*eq\s*false\b""")
      val allowed = setOf("SoftDeleteFlagTable.kt", "SoftDeletableTable.kt", "SoftDeleteOps.kt")
      report(
        "R2",
        sources
          .filterNot { it.name in allowed }
          .flatMap { file ->
            val src = file.readText()
            pattern.findAll(src).map { "${relative(file)}:${lineOf(src, it.range.first)}" }.toList()
          },
      )
    }

    test("R3 软删表禁止硬删(deleteWhere/deleteAll/deleteReturning,软删用 softDelete)") {
      val pattern =
        Regex("""\b(${softTables.joinToString("|")})\s*\.\s*(${HARD_DELETE_METHODS.joinToString("|")})\s*[({]""")
      report(
        "R3",
        sources
          .flatMap { file ->
            val src = file.readText()
            pattern
              .findAll(src)
              .filterNot { EXEMPTION_MARKER in precedingLines(src, it.range.first, count = 8) }
              .map {
                "${relative(file)}:${lineOf(src, it.range.first)} ${it.groupValues[1]}.${it.groupValues[2]}"
              }.toList()
          },
      )
    }

    test("R4 join 链上的每个软删表都要显式 activeOnly(故意豁免需写 `软删豁免:` 注释)") {
      report(
        "R4",
        sources.flatMap { file ->
          val src = file.readText()
          if (JOIN_METHODS.none { it in src }) emptyList() else joinChainViolations(file, src, softTables)
        },
      )
    }
  })
