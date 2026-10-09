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

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

class ModuleDependencyTest :
  FunSpec({
    // 模块前缀 → 模块名（foundation/services 不是“业务模块”）；以下所有包列表均由其派生
    val moduleByPrefix =
      listOf(
        "dev.yidafu.aqua.order" to "trade",
        "dev.yidafu.aqua.payment" to "trade",
        "dev.yidafu.aqua.delivery" to "trade",
        "dev.yidafu.aqua.review" to "trade",
        "dev.yidafu.aqua.analytics" to "analytics",
        "dev.yidafu.aqua.storage" to "platform",
        "dev.yidafu.aqua.notice" to "platform",
        "dev.yidafu.aqua.notification" to "platform",
        "dev.yidafu.aqua.ops" to "ops",
        "dev.yidafu.aqua.marketing" to "marketing",
        "dev.yidafu.aqua.user" to "user",
        "dev.yidafu.aqua.product" to "product",
      )

    val allBusinessPackages = moduleByPrefix.map { (p, _) -> "$p.." }
    val servicePackages =
      listOf(
        "dev.yidafu.aqua.admin..",
        "dev.yidafu.aqua.client..",
      )

    fun businessModuleOf(packageName: String): String? =
      moduleByPrefix
        .firstOrNull { (prefix, _) -> packageName == prefix || packageName.startsWith("$prefix.") }
        ?.second

    fun isRepositoryPackage(packageName: String): Boolean =
      packageName.endsWith(".domain.repository") || packageName.contains(".domain.repository.")

    // P1 只约束业务模块（moduleByPrefix）之间的 repository 直连；admin/client 服务
    // 直接 import 模块 repository 属计划内豁免，不在本规则范围。
    test("P1 业务模块不得依赖其他业务模块的 domain.repository") {
      val violations =
        backendClasses
          .filter { !it.packageName.startsWith("dev.yidafu.aqua.common.graphql.generated") }
          .flatMap { cls ->
            val source = businessModuleOf(cls.packageName) ?: return@flatMap emptyList<String>()
            cls.directDependenciesFromSelf
              .mapNotNull { it.targetClass }
              .filter { target ->
                val dest = businessModuleOf(target.packageName)
                dest != null && dest != source && isRepositoryPackage(target.packageName)
              }.map { "${cls.fullName} -> ${it.fullName}" }
          }
      if (violations.isNotEmpty()) {
        fail("P1 ${violations.size} violations:\n${violations.joinToString("\n")}")
      }
    }

    test("P2a aqua-ops 只依赖 foundation") {
      val forbiddenTargets =
        allBusinessPackages.filter { !it.startsWith("dev.yidafu.aqua.ops") } + servicePackages
      noClasses()
        .that()
        .resideInAPackage("dev.yidafu.aqua.ops..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(*forbiddenTargets.toTypedArray())
        .because("architecture.md §Module Dependencies：aqua-ops 和 aqua-marketing 只依赖 aqua-foundation")
        .check(backendClasses)
    }

    test("P2b aqua-marketing 只依赖 foundation") {
      val forbiddenTargets =
        allBusinessPackages.filter { !it.startsWith("dev.yidafu.aqua.marketing") } + servicePackages
      noClasses()
        .that()
        .resideInAPackage("dev.yidafu.aqua.marketing..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(*forbiddenTargets.toTypedArray())
        .because("architecture.md §Module Dependencies：aqua-ops 和 aqua-marketing 只依赖 aqua-foundation")
        .check(backendClasses)
    }

    test("P3 foundation 不依赖任何业务模块") {
      noClasses()
        .that()
        .resideInAnyPackage(
          "dev.yidafu.aqua.common..",
          "dev.yidafu.aqua.api..",
          "dev.yidafu.aqua.logging..",
        ).and()
        .resideOutsideOfPackage("dev.yidafu.aqua.common.graphql.generated..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(*allBusinessPackages.toTypedArray())
        .because("architecture.md：所有模块依赖 foundation，foundation 不得反向依赖业务模块")
        .check(backendClasses)
    }

    test("P4 业务模块间无循环依赖") {
      fun moduleOf(packageName: String): String? =
        when {
          packageName.startsWith("dev.yidafu.aqua.common.graphql.generated") -> null
          packageName == "dev.yidafu.aqua.common" || packageName.startsWith("dev.yidafu.aqua.common.") -> "foundation"
          packageName == "dev.yidafu.aqua.api" || packageName.startsWith("dev.yidafu.aqua.api.") -> "foundation"
          packageName == "dev.yidafu.aqua.logging" || packageName.startsWith("dev.yidafu.aqua.logging.") -> "foundation"
          else -> businessModuleOf(packageName)
        }

      val edges =
        backendClasses
          .flatMap { cls ->
            val from = moduleOf(cls.packageName) ?: return@flatMap emptyList<Pair<String, String>>()
            cls.directDependenciesFromSelf
              .mapNotNull { it.targetClass }
              .mapNotNull { moduleOf(it.packageName) }
              .filter { it != from }
              .map { from to it }
          }.toSet()

      val adjacency = edges.groupBy({ it.first }, { it.second })
      val nodes = edges.flatMap { listOf(it.first, it.second) }.toSet()
      val visited = mutableSetOf<String>()
      val onStack = mutableSetOf<String>()
      val cycles = mutableListOf<List<String>>()

      fun dfs(
        node: String,
        path: List<String>,
      ): Boolean {
        if (node in onStack) {
          cycles += path.drop(path.indexOf(node)) + node
          return true
        }
        if (node in visited) return false
        visited += node
        onStack += node
        for (next in adjacency[node].orEmpty()) {
          if (dfs(next, path + next)) return true
        }
        onStack -= node
        return false
      }

      for (node in nodes) {
        if (node !in visited) dfs(node, listOf(node))
      }
      cycles.shouldBeEmpty()
    }
  })
