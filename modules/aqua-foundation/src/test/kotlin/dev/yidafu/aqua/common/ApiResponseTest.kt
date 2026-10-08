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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ApiResponseTest :
  FunSpec({

    test("success with data should set success flag") {
      val response = ApiResponse.success("data")
      response.success shouldBe true
      response.data shouldBe "data"
      response.message.shouldBeNull()
      response.code.shouldBeNull()
    }
    test("success with message should include message") {
      val response = ApiResponse.success(42, "created")
      response.success shouldBe true
      response.data shouldBe 42
      response.message shouldBe "created"
    }
    test("error should set failure flags") {
      val response = ApiResponse.error("failed", code = "E1", data = "payload")
      response.success shouldBe false
      response.message shouldBe "failed"
      response.code shouldBe "E1"
      response.data shouldBe "payload"
    }
    test("unauthorized should use default message") {
      val response = ApiResponse.unauthorized()
      response.success shouldBe false
      response.message shouldBe "认证失败，请先登录"
      response.code shouldBe "UNAUTHORIZED"
    }
    test("unauthorized should accept custom message") {
      val response = ApiResponse.unauthorized("custom")
      response.message shouldBe "custom"
    }
    test("forbidden should use default message") {
      val response = ApiResponse.forbidden()
      response.success shouldBe false
      response.message shouldBe "权限不足"
      response.code shouldBe "FORBIDDEN"
    }
    test("notFound should use default message") {
      val response = ApiResponse.notFound()
      response.success shouldBe false
      response.message shouldBe "资源未找到"
      response.code shouldBe "NOT_FOUND"
    }
    test("internalServerError should use default message") {
      val response = ApiResponse.internalServerError()
      response.success shouldBe false
      response.message shouldBe "服务器内部错误"
      response.code shouldBe "INTERNAL_SERVER_ERROR"
    }
    test("timestamp should be set") {
      val response = ApiResponse.success(1)
      response.timestamp.isAfter(
        java.time.LocalDateTime
          .now()
          .minusMinutes(1),
      ) shouldBe true
    }
  })
