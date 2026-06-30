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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.common.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserModelSoftDeleteTest {
  @Test
  fun `test UserModel implements SoftDeletable`() {
    // Given
    val user =
      UserModel(
        wechatOpenId = "test_openid",
        email = "test@example.com",
      )

    // When & Then
    assertFalse(user.isDeleted)
    assertNull(user.deletedAt)
    assertNull(user.deletedBy)
  }

  @Test
  fun `test soft delete functionality`() {
    // Given
    val user =
      UserModel(
        wechatOpenId = "test_openid",
        email = "test@example.com",
      )

    // When
    user.isDeleted = true

    // Then
    assertTrue(user.isDeleted)
    assertTrue(user.isDeleted())
    assertFalse(user.isActive())
  }
}
