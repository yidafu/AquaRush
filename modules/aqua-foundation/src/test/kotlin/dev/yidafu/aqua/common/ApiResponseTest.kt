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

package dev.yidafu.aqua.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {
  @Test
  fun `success with data should set success flag`() {
    val response = ApiResponse.success("data")
    assertTrue(response.success)
    assertEquals("data", response.data)
    assertNull(response.message)
    assertNull(response.code)
  }

  @Test
  fun `success with message should include message`() {
    val response = ApiResponse.success(42, "created")
    assertTrue(response.success)
    assertEquals(42, response.data)
    assertEquals("created", response.message)
  }

  @Test
  fun `error should set failure flags`() {
    val response = ApiResponse.error("failed", code = "E1", data = "payload")
    assertFalse(response.success)
    assertEquals("failed", response.message)
    assertEquals("E1", response.code)
    assertEquals("payload", response.data)
  }

  @Test
  fun `unauthorized should use default message`() {
    val response = ApiResponse.unauthorized()
    assertFalse(response.success)
    assertEquals("认证失败，请先登录", response.message)
    assertEquals("UNAUTHORIZED", response.code)
  }

  @Test
  fun `unauthorized should accept custom message`() {
    val response = ApiResponse.unauthorized("custom")
    assertEquals("custom", response.message)
  }

  @Test
  fun `forbidden should use default message`() {
    val response = ApiResponse.forbidden()
    assertFalse(response.success)
    assertEquals("权限不足", response.message)
    assertEquals("FORBIDDEN", response.code)
  }

  @Test
  fun `notFound should use default message`() {
    val response = ApiResponse.notFound()
    assertFalse(response.success)
    assertEquals("资源未找到", response.message)
    assertEquals("NOT_FOUND", response.code)
  }

  @Test
  fun `internalServerError should use default message`() {
    val response = ApiResponse.internalServerError()
    assertFalse(response.success)
    assertEquals("服务器内部错误", response.message)
    assertEquals("INTERNAL_SERVER_ERROR", response.code)
  }

  @Test
  fun `timestamp should be set`() {
    val response = ApiResponse.success(1)
    assertTrue(
      response.timestamp.isAfter(
        java.time.LocalDateTime
          .now()
          .minusMinutes(1),
      ),
    )
  }
}
