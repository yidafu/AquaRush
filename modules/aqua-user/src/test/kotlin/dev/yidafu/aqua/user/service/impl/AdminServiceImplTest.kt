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

import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

/**
 * 账号密码重置(PRD/07-user.md §F-USR-020/021,G11 落地):
 * - newPassword 为空时重置为默认密码 123456(与 PRD F-USR-010/021 一致);
 * - 新密码长度 <6 位被拒。
 */
class AdminServiceImplTest {
  private val adminRepository: AdminRepository = mockk(relaxed = true)
  private val userService: UserService = mockk(relaxed = true)
  private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
  private val snowflakeIdGenerator: SnowflakeIdGenerator = mockk(relaxed = true)
  private val bizLogger: BizLogger = mockk(relaxed = true)

  private val service =
    AdminServiceImpl(
      adminRepository,
      userService,
      passwordEncoder,
      snowflakeIdGenerator,
      bizLogger,
    )

  private fun admin(id: Long = 1L): AdminModel =
    AdminModel(
      id = id,
      username = "delivery_1",
      passwordHash = requireNotNull(passwordEncoder.encode("oldpass123")),
      realName = "送水工甲",
      phone = "13900139000",
      role = AdminRoleModel.DELIVERY_WORKER,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @Test
  fun `resetPassword with null newPassword resets to default 123456`() {
    val target = admin()
    val oldHash = target.passwordHash
    every { adminRepository.findAdminById(1L) } returns target
    every { adminRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (1L to "admin")

    val saved = service.resetPassword(1L, null)

    assertNotEquals(oldHash, saved.passwordHash)
    assertTrue(passwordEncoder.matches("123456", saved.passwordHash), "缺省新密码应为 123456")
    verify { adminRepository.save(target) }
  }

  @Test
  fun `resetPassword sets the provided new password`() {
    val target = admin()
    every { adminRepository.findAdminById(1L) } returns target
    every { adminRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (1L to "admin")

    val saved = service.resetPassword(1L, "newpass456")

    assertTrue(passwordEncoder.matches("newpass456", saved.passwordHash))
  }

  @Test
  fun `resetPassword rejects new password shorter than 6 chars`() {
    val target = admin()
    every { adminRepository.findAdminById(1L) } returns target

    val exception = assertFailsWith<BadRequestException> { service.resetPassword(1L, "12345") }
    assertTrue(exception.message!!.contains("6"))
  }

  @Test
  fun `resetPassword throws when admin not found`() {
    every { adminRepository.findAdminById(999L) } returns null

    val exception = assertFailsWith<BadRequestException> { service.resetPassword(999L, null) }
    assertTrue(exception.message!!.contains("不存在"))
  }
}
