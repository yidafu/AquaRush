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

package dev.yidafu.aqua.logging.context

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserContextHolderTest {
  private val context = UserContext(userId = "1", username = "alice", role = "ADMIN", tenantId = "t1")

  @AfterEach
  fun tearDown() {
    UserContextHolder.clear()
  }

  @Test
  fun `set and get should round trip`() {
    UserContextHolder.setUserContext(context)
    assertEquals(context, UserContextHolder.getUserContext())
  }

  @Test
  fun `get should return null when unset`() {
    assertNull(UserContextHolder.getUserContext())
  }

  @Test
  fun `getUserId should return user id`() {
    UserContextHolder.setUserContext(context)
    assertEquals("1", UserContextHolder.getUserId())
  }

  @Test
  fun `getUsername should return username`() {
    UserContextHolder.setUserContext(context)
    assertEquals("alice", UserContextHolder.getUsername())
  }

  @Test
  fun `getUserRole should return role`() {
    UserContextHolder.setUserContext(context)
    assertEquals("ADMIN", UserContextHolder.getUserRole())
  }

  @Test
  fun `getUserRole should return null when role is null`() {
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "alice"))
    assertNull(UserContextHolder.getUserRole())
  }

  @Test
  fun `accessors should return null when no context`() {
    assertNull(UserContextHolder.getUserId())
    assertNull(UserContextHolder.getUsername())
    assertNull(UserContextHolder.getUserRole())
  }

  @Test
  fun `clear should remove context`() {
    UserContextHolder.setUserContext(context)
    UserContextHolder.clear()
    assertNull(UserContextHolder.getUserContext())
  }

  @Test
  fun `hasUserContext should reflect state`() {
    assertFalse(UserContextHolder.hasUserContext())
    UserContextHolder.setUserContext(context)
    assertTrue(UserContextHolder.hasUserContext())
    UserContextHolder.clear()
    assertFalse(UserContextHolder.hasUserContext())
  }
}
