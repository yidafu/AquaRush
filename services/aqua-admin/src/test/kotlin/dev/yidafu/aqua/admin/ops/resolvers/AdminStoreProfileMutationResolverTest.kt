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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.StoreProfileMutationService
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.common.graphql.generated.SaveStoreProfileInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AdminStoreProfileMutationResolverTest {
  private lateinit var storeProfileMutationService: StoreProfileMutationService
  private lateinit var resolver: AdminStoreProfileMutationResolver

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    storeProfileMutationService = mockk(relaxed = true)
    resolver = AdminStoreProfileMutationResolver(storeProfileMutationService)
  }

  @Test
  fun `saveStoreProfile should delegate`() {
    // Given
    every { storeProfileMutationService.saveStoreProfile(1L, "admin", any()) } returns
      StoreProfileModel(
        id = 1L,
        shopName = "AquaRush水站",
        serviceHotline = "400-123-4567",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val input = SaveStoreProfileInput(shopName = "AquaRush水站", serviceHotline = "400-123-4567")

    // When
    val result = resolver.saveStoreProfile(input, principal())

    // Then
    assertEquals("AquaRush水站", result.shopName)
    verify { storeProfileMutationService.saveStoreProfile(1L, "admin", any()) }
  }
}
