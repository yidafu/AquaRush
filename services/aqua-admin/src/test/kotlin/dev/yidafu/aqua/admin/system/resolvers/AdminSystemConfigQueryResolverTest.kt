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

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryService
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.graphql.generated.SystemConfigGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AdminSystemConfigQueryResolverTest {
  private lateinit var systemConfigQueryService: SystemConfigQueryService
  private lateinit var resolver: AdminSystemConfigQueryResolver

  @BeforeEach
  fun setUp() {
    systemConfigQueryService = mockk(relaxed = true)
    resolver = AdminSystemConfigQueryResolver(systemConfigQueryService)
  }

  @Test
  fun `systemConfigs should list by mapped group`() {
    // Given
    every { systemConfigQueryService.listByGroup(SystemSettingGroup.INVENTORY) } returns
      listOf(SystemSettingsModel(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "10"))

    // When
    val result = resolver.systemConfigs(SystemConfigGroup.INVENTORY)

    // Then
    assertEquals(1, result.size)
    assertEquals("LOW_STOCK_THRESHOLD", result[0].settingKey)
    verify { systemConfigQueryService.listByGroup(SystemSettingGroup.INVENTORY) }
  }
}
