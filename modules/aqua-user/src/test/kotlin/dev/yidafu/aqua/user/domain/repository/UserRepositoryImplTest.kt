/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.user.common.exposed.UserPgTestSupport
import dev.yidafu.aqua.user.common.exposed.UserPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class UserRepositoryImplTest :
  FunSpec({
    val repo = UserRepositoryImpl(UserPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById 可查") {
      val model = UserModel(wechatOpenId = "wx-1", email = "a@b.c", status = UserStatus.ACTIVE)
      val saved = repo.save(model)
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .wechatOpenId shouldBe "wx-1"
    }

    test("save 更新已有实体走 update 分支") {
      val id = UserPgTestSupport.user()
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      repo.save(loaded.apply { nickname = "新昵称" })
      repo
        .findById(id)
        .orElse(null)
        .shouldNotBeNull()
        .nickname shouldBe "新昵称"
    }

    test("findAll/分页/count/existsById/findAllById 与软删过滤") {
      val keep = UserPgTestSupport.user()
      UserPgTestSupport.user(deleted = true)

      repo.findAll() shouldHaveSize 1
      repo.findAll(PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.count() shouldBe 1L
      repo.existsById(keep) shouldBe true
      repo.existsById(999L) shouldBe false
      repo.findAllById(listOf(keep, 999L)) shouldHaveSize 1
    }

    test("deleteById 软删后不可见") {
      val id = UserPgTestSupport.user()
      repo.deleteById(id)
      repo.existsById(id) shouldBe false
      repo.count() shouldBe 0L
    }

    test("findByWechatOpenId/findByPhone/existsByWechatOpenId") {
      UserPgTestSupport.user()
      val found = repo.findAll().first()
      repo.findByWechatOpenId("openid-x") shouldBe null
      repo.findByWechatOpenId(found.wechatOpenId).shouldNotBeNull()
      repo.findByPhone(found.phone!!).shouldNotBeNull()
      repo.existsByWechatOpenId(found.wechatOpenId) shouldBe true
    }

    test("昵称/手机号分页搜索与状态分页") {
      UserPgTestSupport.user(nickname = "张三丰", phone = "13811112222")
      UserPgTestSupport.user(nickname = "李四", phone = "13833334444")

      repo
        .findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase("张三", "x", PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase("x", "1383333", PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo.findByStatus(UserStatus.ACTIVE, PageRequest.of(0, 10)).totalElements shouldBe 2L
    }

    test("客户分组:列表/分页/计数") {
      UserPgTestSupport.user(customerGroupId = 5L)
      UserPgTestSupport.user(customerGroupId = 5L)
      UserPgTestSupport.user(customerGroupId = 6L)

      repo.findByCustomerGroupId(5L) shouldHaveSize 2
      repo.findByCustomerGroupId(5L, PageRequest.of(0, 1)).totalElements shouldBe 2L
      repo.countByCustomerGroupId(5L) shouldBe 2L
      repo.countByCustomerGroupId(9L) shouldBe 0L
    }

    test("创建时间窗口查询(严格开区间)") {
      val base = LocalDateTime.of(2026, 1, 10, 0, 0)
      UserPgTestSupport.user(createdAt = base.plusHours(5))
      UserPgTestSupport.user(createdAt = base.plusDays(2))

      repo.findByCreatedAtGreaterThan(base) shouldHaveSize 2
      repo.findByCreatedAtGreaterThanAndCreatedAtLessThan(base.plusHours(1), base.plusDays(1)) shouldHaveSize 1
      repo.countByCreatedAtGreaterThanAndCreatedAtLessThan(base.plusHours(1), base.plusDays(1)) shouldBe 1L
    }

    test("Custom 查询:金额/地址数/评价资格/关键词状态分页") {
      val id = UserPgTestSupport.user(totalSpentCents = 12300L, balanceCents = 456L)
      UserPgTestSupport.address(userId = id)

      repo.getUserTotalSpent(id) shouldBe 123.0
      repo.getUserBalance(id) shouldBe 4.56
      repo.getUserAddressCount(id) shouldBe 1
      repo.canUserReview(id, 1L) shouldBe true
      repo.canUserReview(999L, 1L) shouldBe false

      val page =
        repo.findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
          "用户",
          UserStatus.ACTIVE,
          PageRequest.of(0, 10),
        )
      page.totalElements shouldBe 1L
    }

    test("金额查询的空值回退(无行)") {
      repo.getUserTotalSpent(999L) shouldBe 0.0
      repo.getUserBalance(999L) shouldBe 0.0
    }
  })
