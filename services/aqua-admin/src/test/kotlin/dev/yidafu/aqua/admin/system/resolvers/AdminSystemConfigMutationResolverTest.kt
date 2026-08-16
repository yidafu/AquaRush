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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority

class AdminSystemConfigMutationResolverTest :
  FunSpec({
    var systemConfigMutationService: SystemConfigMutationService = mockk()
    var resolver: AdminSystemConfigMutationResolver = mockk()

    fun principal(): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
      )

    beforeTest {
      systemConfigMutationService = mockk(relaxed = true)
      resolver = AdminSystemConfigMutationResolver(systemConfigMutationService)
    }
    test("saveSystemConfigs should delegate with operator info") {
      // Given
      every { systemConfigMutationService.saveSystemConfigs(1L, "admin", any()) } returns
        listOf(SystemSettingsModel(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "15"))
      val input = listOf(SaveSystemConfigInput(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "15"))

      // When
      val result = resolver.saveSystemConfigs(input, principal())

      // Then
      result.size shouldBe 1
      result[0].settingKey shouldBe "LOW_STOCK_THRESHOLD"
      verify { systemConfigMutationService.saveSystemConfigs(1L, "admin", any()) }
    }
    test("restoreSystemConfig should delegate with operator info") {
      // Given
      every { systemConfigMutationService.restoreSystemConfig("LOW_STOCK_THRESHOLD", 1L, "admin") } returns
        SystemSettingsModel(settingKey = "LOW_STOCK_THRESHOLD", settingValue = "10")

      // When
      val result = resolver.restoreSystemConfig("LOW_STOCK_THRESHOLD", principal())

      // Then
      result.settingKey shouldBe "LOW_STOCK_THRESHOLD"
      verify { systemConfigMutationService.restoreSystemConfig("LOW_STOCK_THRESHOLD", 1L, "admin") }
    }
  })
