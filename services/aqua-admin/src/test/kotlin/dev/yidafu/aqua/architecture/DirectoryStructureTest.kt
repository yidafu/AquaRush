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

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass

private val d1NoUtilsPackage: ArchRule =
  noClasses()
    .should()
    .resideInAPackage("..utils..")
    .because("AGENTS.md 关键规则4：util/utils 双包禁用，统一使用 util")

private val d2ExceptionLocation: ArchRule =
  classes()
    .that()
    .resideInAPackage("..exception..")
    .should()
    .resideInAPackage("..domain.exception..")
    .orShould()
    .resideInAPackage("dev.yidafu.aqua.common.exception..")
    .because("architecture.md：禁止模块根 exception/，使用 domain/exception/；foundation common/exception 为成文基础设施")

private val d3ForbiddenPackages: ArchRule =
  noClasses()
    .should()
    .resideInAnyPackage(
      "dev.yidafu.aqua.api.query..",
      "dev.yidafu.aqua.common.dto..",
    ).because("architecture.md 核心规则2：不创建 api/query 或 common/dto")

private val d4JpaEntityLocation: ArchRule =
  classes()
    .that()
    .areAnnotatedWith(Entity::class.java)
    .or()
    .areAnnotatedWith(MappedSuperclass::class.java)
    .or()
    .areAnnotatedWith(Embeddable::class.java)
    .should()
    .resideInAPackage("dev.yidafu.aqua.common.domain.model..")
    .because("architecture.md 核心规则1：JPA 实体集中于 aqua-foundation（client 的 @EntityScan 依赖此约定）")

private val d5ResolversPlural: ArchRule =
  classes()
    .that()
    .haveSimpleNameEndingWith("Resolver")
    .and()
    .resideInAnyPackage("dev.yidafu.aqua.admin..", "dev.yidafu.aqua.client..")
    .should()
    .resideInAPackage("..resolvers..")
    .because("architecture.md：service <domain>/resolvers/ always plural")

private val d6NoInputInResolverPackage: ArchRule =
  noClasses()
    .that()
    .haveSimpleNameEndingWith("Input")
    .should()
    .resideInAnyPackage("..resolver..", "..resolvers..")
    .because("AGENTS.md 规则8：Resolver input DTO 不得内联，放 <admin|client>/<domain>/dto/")

class DirectoryStructureTest :
  FunSpec({
    test("D1 禁止 utils 包") {
      d1NoUtilsPackage.check(backendClasses)
    }

    test("D2 异常必须位于 domain.exception 或 foundation common.exception") {
      d2ExceptionLocation.check(backendClasses)
    }

    test("D3 禁止 api.query 与 common.dto 包") {
      d3ForbiddenPackages.check(backendClasses)
    }

    test("D4 JPA 实体集中在 foundation common.domain.model") {
      d4JpaEntityLocation.check(backendClasses)
    }

    test("D5 服务层 Resolver 位于 resolvers 复数目录") {
      d5ResolversPlural.check(backendClasses)
    }

    test("D6 Input 类不得位于 resolver 包") {
      d6NoInputInResolverPackage.check(backendClasses)
    }

    test("哨兵: importer 覆盖 foundation 与 client(防空转)") {
      backendClasses.any { it.fullName == "dev.yidafu.aqua.common.domain.model.OrderModel" } shouldBe true
      backendClasses.any { it.packageName.startsWith("dev.yidafu.aqua.client.") } shouldBe true
      (backendClasses.count() > 500) shouldBe true
    }
  })
