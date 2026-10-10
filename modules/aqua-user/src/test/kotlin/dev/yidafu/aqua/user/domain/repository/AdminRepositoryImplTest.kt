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

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.common.exposed.UserExposedH2TestSupport
import dev.yidafu.aqua.user.common.exposed.UserExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest

class AdminRepositoryImplTest :
  FunSpec({
    val repo = AdminRepositoryImpl(UserExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById/findAll/existsById/count") {
      val saved = repo.save(AdminModel(username = "admin1", passwordHash = "h"))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .username shouldBe "admin1"
      repo.findAllAdmins(null, null, PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.existsById(saved.id!!) shouldBe true
      repo.count() shouldBe 1L
    }

    test("save 更新走 update 分支") {
      val id = UserExposedH2TestSupport.admin()
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      repo.save(loaded.apply { realName = "改名" })
      repo
        .findById(id)
        .orElse(null)
        .shouldNotBeNull()
        .realName shouldBe "改名"
    }

    test("deleteById 软删;deleteAll 全表软删") {
      val a = UserExposedH2TestSupport.admin()
      UserExposedH2TestSupport.admin()
      repo.deleteById(a)
      repo.findAllAdmins(null, null, PageRequest.of(0, 10)).totalElements shouldBe 1L

      repo.deleteAll()
      repo.findAllAdmins(null, null, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("按用户名/手机/角色/在线状态查询") {
      UserExposedH2TestSupport.admin(username = "w1", role = "DELIVERY_WORKER", onlineStatus = "ONLINE")
      UserExposedH2TestSupport.admin(username = "boss", role = "ADMIN", phone = "13999991111")

      repo.findByUsername("w1").shouldNotBeNull()
      repo.findByUsername("nobody") shouldBe null
      repo.existsByUsername("w1") shouldBe true
      repo.existsByUsername("nobody") shouldBe false
      repo.findByPhone("13999991111").shouldNotBeNull()
      repo.existsByPhone("13999991111") shouldBe true
      repo.findByRole(AdminRoleModel.ADMIN) shouldHaveSize 1
      repo.countByRole(AdminRoleModel.ADMIN) shouldBe 1L
      repo.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE, 100) shouldHaveSize 1
      repo.findAllAdmins(null, null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      // 关键字/角色过滤下推 SQL
      repo
        .findAllAdmins("w1", null, PageRequest.of(0, 10))
        .content
        .single()
        .username shouldBe "w1"
      repo
        .findAllAdmins(null, AdminRoleModel.ADMIN, PageRequest.of(0, 10))
        .content
        .single()
        .username shouldBe "boss"
      repo.findAdminById(999L) shouldBe null
    }

    test("角色集合查询与计数(空集合回退)") {
      UserExposedH2TestSupport.admin(role = "DELIVERY_WORKER")
      UserExposedH2TestSupport.admin(role = "ADMIN")

      val roles = listOf(AdminRoleModel.DELIVERY_WORKER, AdminRoleModel.ADMIN)
      repo.findAllByRoleIn(roles, 100) shouldHaveSize 2
      repo.findAllByRoleIn(roles, null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.countByRoleIn(roles) shouldBe 2L
      repo.findAllByRoleIn(emptyList(), 100) shouldHaveSize 0
      repo.countByRoleIn(emptyList()) shouldBe 0L
    }

    test("findByUsernameAndRole 与 findByUserId") {
      val uid = 555L
      UserExposedH2TestSupport.admin(username = "wx", role = "DELIVERY_WORKER", userId = uid)

      repo.findByUsernameAndRole("wx", AdminRoleModel.DELIVERY_WORKER).shouldNotBeNull()
      repo.findByUsernameAndRole("wx", AdminRoleModel.ADMIN) shouldBe null
      repo.findByUserId(uid).shouldNotBeNull()
    }
  })
