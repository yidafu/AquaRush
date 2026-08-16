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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class AdminQueryApiServiceImplTest :
  FunSpec({
    var adminRepository: AdminRepository = mockk()
    var service: AdminQueryApiServiceImpl = mockk()

    fun admin(): AdminModel =
      AdminModel(
        id = 1L,
        username = "admin",
        passwordHash = "hash",
        realName = "管理员",
        phone = "13900139000",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      adminRepository = mockk(relaxed = true)
      service = AdminQueryApiServiceImpl(adminRepository)
    }
    test("findById should delegate") {
      every { adminRepository.findAdminById(1L) } returns admin()
      service.findById(1L)?.id shouldBe 1L
    }
    test("findById should return null when absent") {
      every { adminRepository.findAdminById(9L) } returns null
      service.findById(9L).shouldBeNull()
    }
    test("findByUsername should delegate") {
      every { adminRepository.findByUsername("admin") } returns admin()
      service.findByUsername("admin")?.username shouldBe "admin"
    }
    test("findAll should delegate") {
      every { adminRepository.findAllAdmins() } returns listOf(admin())
      service.findAll().size shouldBe 1
      verify { adminRepository.findAllAdmins() }
    }
    test("existsById should delegate") {
      every { adminRepository.existsById(1L) } returns true
      service.existsById(1L) shouldBe true
    }
  })
