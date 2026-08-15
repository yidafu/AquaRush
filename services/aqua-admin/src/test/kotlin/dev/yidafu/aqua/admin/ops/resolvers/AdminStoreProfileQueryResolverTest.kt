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

import dev.yidafu.aqua.api.service.ops.StoreProfileQueryService
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminStoreProfileQueryResolverTest {
  private lateinit var storeProfileQueryService: StoreProfileQueryService
  private lateinit var resolver: AdminStoreProfileQueryResolver

  @BeforeEach
  fun setUp() {
    storeProfileQueryService = mockk(relaxed = true)
    resolver = AdminStoreProfileQueryResolver(storeProfileQueryService)
  }

  @Test
  fun `storeProfile should return mapped profile`() {
    // Given
    every { storeProfileQueryService.getStoreProfile() } returns
      StoreProfileModel(
        id = 1L,
        shopName = "AquaRush水站",
        serviceHotline = "400-123-4567",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    // When
    val result = resolver.storeProfile()

    // Then
    assertEquals("AquaRush水站", result!!.shopName)
    verify { storeProfileQueryService.getStoreProfile() }
  }

  @Test
  fun `storeProfile should return null when missing`() {
    // Given
    every { storeProfileQueryService.getStoreProfile() } returns null

    // When
    val result = resolver.storeProfile()

    // Then
    assertNull(result)
  }
}
