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

package dev.yidafu.aqua.admin.system.resolvers

import dev.yidafu.aqua.api.service.ops.SystemConfigMutationService
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.graphql.generated.SaveSystemConfigInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals

class AdminSystemConfigMutationResolverTest {
  private lateinit var systemConfigMutationService: SystemConfigMutationService
  private lateinit var resolver: AdminSystemConfigMutationResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    systemConfigMutationService = mockk(relaxed = true)
    resolver = AdminSystemConfigMutationResolver(systemConfigMutationService)
  }

  @Test
  fun `saveSystemConfigs should delegate with operator info`() {
    // Given
    every { systemConfigMutationService.saveSystemConfigs(1L, "admin", any()) } returns
      listOf(SystemSettingsModel(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "15"))
    val input = listOf(SaveSystemConfigInput(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "15"))

    // When
    val result = resolver.saveSystemConfigs(input, principal())

    // Then
    assertEquals(1, result.size)
    assertEquals("LOW_STOCK_THRESHOLD", result[0].settingKey)
    verify { systemConfigMutationService.saveSystemConfigs(1L, "admin", any()) }
  }

  @Test
  fun `restoreSystemConfig should delegate with operator info`() {
    // Given
    every { systemConfigMutationService.restoreSystemConfig("LOW_STOCK_THRESHOLD", 1L, "admin") } returns
      SystemSettingsModel(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "10")

    // When
    val result = resolver.restoreSystemConfig("LOW_STOCK_THRESHOLD", principal())

    // Then
    assertEquals("LOW_STOCK_THRESHOLD", result.settingKey)
    verify { systemConfigMutationService.restoreSystemConfig("LOW_STOCK_THRESHOLD", 1L, "admin") }
  }
}
