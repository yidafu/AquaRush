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

package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * 停用账号不可登录(AC-USR-020-07,PRD/07-user.md §F-USR-020):
 * AdminUserDetailsService 对 DISABLED 账号返回 disabled=true,AuthenticationManager 抛 DisabledException。
 */
class AdminUserDetailsServiceTest {
  private val adminService: AdminService = mockk(relaxed = true)
  private val service = AdminUserDetailsService(adminService)

  private fun admin(accountStatus: AdminAccountStatus): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hash",
      realName = "管理员",
      phone = "13900139000",
      role = AdminRoleModel.ADMIN,
      accountStatus = accountStatus,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @Test
  fun `disabled account is flagged as disabled`() {
    every { adminService.findByUsername("admin") } returns admin(AdminAccountStatus.DISABLED)

    val details = service.loadUserByUsername("admin")

    assertFalse(details.isEnabled, "停用账号 isEnabled 应为 false")
  }

  @Test
  fun `active account is enabled`() {
    every { adminService.findByUsername("admin") } returns admin(AdminAccountStatus.ACTIVE)

    val details = service.loadUserByUsername("admin")

    assertTrue(details.isEnabled, "正常账号 isEnabled 应为 true")
  }
}
