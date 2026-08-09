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

package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证 client 端角色唯一来源为 users.role(R-AUTH-04),手机号前缀不再影响角色。
 */
class ClientUserDetailsServiceTest {

  private val userRepository = mockk<UserRepository>(relaxed = true)
  private val service = ClientUserDetailsService(userRepository)

  private fun user(
    role: UserRole,
    openId: String = "openid-123",
    phone: String = "13900139000",
  ): UserModel =
    UserModel(
      id = 1L,
      wechatOpenId = openId,
      phone = phone,
      role = role,
    )

  @Test
  fun `USER role should map to ROLE_USER`() {
    every { userRepository.findByWechatOpenId("openid-123") } returns user(UserRole.USER)
    val principal = service.loadUserByUsername("openid-123")
    assertTrue(principal.authorities.any { it.authority == "ROLE_USER" })
    assertEquals("USER", (principal as UserPrincipal).userType)
  }

  @Test
  fun `WORKER role should map to ROLE_DELIVERY_WORKER aligned with AdminRoleModel`() {
    every { userRepository.findByWechatOpenId("openid-123") } returns user(UserRole.WORKER)
    val principal = service.loadUserByUsername("openid-123")
    assertTrue(principal.authorities.any { it.authority == "ROLE_DELIVERY_WORKER" })
    assertTrue(principal.authorities.any { it.authority == "PERMISSION_DELIVERY_TASK" })
    assertEquals("DELIVERY_WORKER", (principal as UserPrincipal).userType)
  }

  @Test
  fun `ADMIN role should map to ROLE_ADMIN`() {
    every { userRepository.findByWechatOpenId("openid-123") } returns user(UserRole.ADMIN)
    val principal = service.loadUserByUsername("openid-123")
    assertTrue(principal.authorities.any { it.authority == "ROLE_ADMIN" })
  }

  @Test
  fun `phone prefix should not influence role`() {
    // 手机号以 admin 开头但 users.role=USER,仍应为普通用户(废除旧手机号前缀推断)
    every { userRepository.findByWechatOpenId("openid-123") } returns
      user(UserRole.USER, phone = "admin123456")
    val principal = service.loadUserByUsername("openid-123")
    assertTrue(principal.authorities.any { it.authority == "ROLE_USER" })
    assertTrue(principal.authorities.none { it.authority == "ROLE_ADMIN" })
  }

  @Test
  fun `NONE role should default to ROLE_USER`() {
    every { userRepository.findByWechatOpenId("openid-123") } returns user(UserRole.NONE)
    val principal = service.loadUserByUsername("openid-123")
    assertTrue(principal.authorities.any { it.authority == "ROLE_USER" })
  }
}
