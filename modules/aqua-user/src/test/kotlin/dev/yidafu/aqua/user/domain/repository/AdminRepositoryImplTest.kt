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
import dev.yidafu.aqua.user.common.exposed.UserPgTestSupport
import dev.yidafu.aqua.user.common.exposed.UserPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class AdminRepositoryImplTest :
  FunSpec({
    val repo = AdminRepositoryImpl(UserPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById/findAll/existsById/count") {
      val saved = repo.save(AdminModel(username = "admin1", passwordHash = "h"))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .username shouldBe "admin1"
      repo.findAll() shouldHaveSize 1
      repo.existsById(saved.id!!) shouldBe true
      repo.count() shouldBe 1L
    }

    test("save 更新走 update 分支") {
      val id = UserPgTestSupport.admin()
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      repo.save(loaded.apply { realName = "改名" })
      repo
        .findById(id)
        .orElse(null)
        .shouldNotBeNull()
        .realName shouldBe "改名"
    }

    test("deleteById 软删;deleteAll 全表软删") {
      val a = UserPgTestSupport.admin()
      UserPgTestSupport.admin()
      repo.deleteById(a)
      repo.findAll() shouldHaveSize 1

      repo.deleteAll()
      repo.findAll() shouldHaveSize 0
    }

    test("按用户名/手机/角色/在线状态查询") {
      UserPgTestSupport.admin(username = "w1", role = "DELIVERY_WORKER", onlineStatus = "ONLINE")
      UserPgTestSupport.admin(username = "boss", role = "ADMIN", phone = "13999991111")

      repo.findByUsername("w1").shouldNotBeNull()
      repo.findByUsername("nobody") shouldBe null
      repo.existsByUsername("w1") shouldBe true
      repo.existsByUsername("nobody") shouldBe false
      repo.findByPhone("13999991111").shouldNotBeNull()
      repo.existsByPhone("13999991111") shouldBe true
      repo.findByRole(AdminRoleModel.ADMIN) shouldHaveSize 1
      repo.countByRole(AdminRoleModel.ADMIN) shouldBe 1L
      repo.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) shouldHaveSize 1
      repo.findAllAdmins() shouldHaveSize 2
      repo.findAdminById(999L) shouldBe null
    }

    test("角色集合查询与计数(空集合回退)") {
      UserPgTestSupport.admin(role = "DELIVERY_WORKER")
      UserPgTestSupport.admin(role = "ADMIN")

      val roles = listOf(AdminRoleModel.DELIVERY_WORKER, AdminRoleModel.ADMIN)
      repo.findAllByRoleIn(roles) shouldHaveSize 2
      repo.countByRoleIn(roles) shouldBe 2L
      repo.findAllByRoleIn(emptyList()) shouldHaveSize 0
      repo.countByRoleIn(emptyList()) shouldBe 0L
    }

    test("findByUsernameAndRole 与 findByUserId") {
      val uid = 555L
      UserPgTestSupport.admin(username = "wx", role = "DELIVERY_WORKER", userId = uid)

      repo.findByUsernameAndRole("wx", AdminRoleModel.DELIVERY_WORKER).shouldNotBeNull()
      repo.findByUsernameAndRole("wx", AdminRoleModel.ADMIN) shouldBe null
      repo.findByUserId(uid).shouldNotBeNull()
    }
  })
