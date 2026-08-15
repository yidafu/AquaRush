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

package dev.yidafu.aqua.logging.util

import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.logging.service.BusinessLogService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BizLoggerTest {
  private lateinit var businessLogService: BusinessLogService
  private lateinit var bizLogger: BizLogger

  @BeforeEach
  fun setUp() {
    businessLogService = mockk(relaxed = true)
    bizLogger = BizLogger(businessLogService)
    SecurityContextHolder.clearContext()
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
    CorrelationIdHolder.clear()
  }

  @Test
  fun `logOperation success should save with INFO level`() {
    bizLogger.logOperation("CREATE", "ORDER", "SUCCESS", "100")
    verify {
      businessLogService.saveBusinessLog(
        correlationId = null,
        level = "INFO",
        loggerName = "ORDER",
        message = match { it.contains("Operation: CREATE") && it.contains("Module: ORDER") },
        userId = null,
        username = null,
        operation = "CREATE",
      )
    }
  }

  @Test
  fun `logOperation failure should save with ERROR level`() {
    bizLogger.logOperation("CREATE", "ORDER", "ERROR", "100")
    verify {
      businessLogService.saveBusinessLog(
        correlationId = null,
        level = "ERROR",
        loggerName = "ORDER",
        message = any(),
        userId = null,
        username = null,
        operation = "CREATE",
      )
    }
  }

  @Test
  fun `logOperation with additional data should include it in message`() {
    bizLogger.logOperation("CREATE", "ORDER", "SUCCESS", "100", additionalData = mapOf("sku" to "A1"))
    verify {
      businessLogService.saveBusinessLog(
        correlationId = null,
        level = "INFO",
        loggerName = "ORDER",
        message = match { it.contains("Additional:") },
        userId = null,
        username = null,
        operation = "CREATE",
      )
    }
  }

  @Test
  fun `logOperation should pass correlation id and user info`() {
    CorrelationIdHolder.setCorrelationId("cid")
    bizLogger.logOperation("CREATE", "ORDER", "SUCCESS", "100", userId = 5L, username = "admin")
    verify {
      businessLogService.saveBusinessLog(
        correlationId = "cid",
        level = "INFO",
        loggerName = "ORDER",
        message = any(),
        userId = 5L,
        username = "admin",
        operation = "CREATE",
      )
    }
  }

  @Test
  fun `logOrderOperation should delegate with ORDER module`() {
    bizLogger.logOrderOperation(100L, "PAY", "SUCCESS", userId = 1L)
    verify { businessLogService.saveBusinessLog(any(), "INFO", "ORDER", any(), any(), 1L, any(), "PAY") }
  }

  @Test
  fun `logPaymentOperation should delegate with PAYMENT module`() {
    bizLogger.logPaymentOperation(200L, "PAY", "SUCCESS")
    verify { businessLogService.saveBusinessLog(any(), "INFO", "PAYMENT", any(), any(), any(), any(), "PAY") }
  }

  @Test
  fun `logDeliveryOperation should delegate with DELIVERY module`() {
    bizLogger.logDeliveryOperation(300L, "START", "FAILURE")
    verify { businessLogService.saveBusinessLog(any(), "ERROR", "DELIVERY", any(), any(), any(), any(), "START") }
  }

  @Test
  fun `logUserOperation should delegate with USER module`() {
    bizLogger.logUserOperation(400L, "UPDATE", "SUCCESS", operatorId = 9L, operatorName = "op")
    verify { businessLogService.saveBusinessLog(any(), "INFO", "USER", any(), any(), 9L, "op", "UPDATE") }
  }

  @Test
  fun `logLogin success should save SUCCESS with parsed user id`() {
    bizLogger.logLogin("42", "alice", "WECHAT", success = true)
    verify {
      businessLogService.saveBusinessLog(
        any(),
        "INFO",
        "AUTH",
        match { it.contains("loginMethod=WECHAT") },
        any(),
        42L,
        "alice",
        "USER_LOGIN",
      )
    }
  }

  @Test
  fun `logLogin failure should save FAILURE`() {
    bizLogger.logLogin("not-a-number", "alice", "PASSWORD", success = false, operation = "LOGIN_FAILED")
    verify { businessLogService.saveBusinessLog(any(), "ERROR", "AUTH", any(), any(), null, "alice", "LOGIN_FAILED") }
  }

  @Test
  fun `logProductOperation should delegate with PRODUCT module`() {
    bizLogger.logProductOperation(500L, "CREATE", "SUCCESS")
    verify { businessLogService.saveBusinessLog(any(), "INFO", "PRODUCT", any(), any(), any(), any(), "CREATE") }
  }

  @Test
  fun `logMarketingOperation should delegate with MARKETING module`() {
    bizLogger.logMarketingOperation(600L, "ISSUE", "SUCCESS")
    verify { businessLogService.saveBusinessLog(any(), "INFO", "MARKETING", any(), any(), any(), any(), "ISSUE") }
  }

  @Test
  fun `logOpsOperation with target id should compose target`() {
    bizLogger.logOpsOperation("BANNER", 700L, "UPDATE", "SUCCESS")
    verify {
      businessLogService.saveBusinessLog(any(), "INFO", "OPS", match { it.contains("BANNER:700") }, any(), any(), any(), "UPDATE")
    }
  }

  @Test
  fun `logOpsOperation without target id should use target type`() {
    bizLogger.logOpsOperation("BANNER", null, "UPDATE", "SUCCESS")
    verify {
      businessLogService.saveBusinessLog(any(), "INFO", "OPS", match { it.contains("Target: BANNER") }, any(), any(), any(), "UPDATE")
    }
  }

  @Test
  fun `resolveOperator should return principal id and username`() {
    val principal = UserPrincipal(1L, "admin", "ADMIN", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    assertEquals(1L to "admin", bizLogger.resolveOperator())
  }

  @Test
  fun `resolveOperator should return nulls for anonymous principal`() {
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken("anonymousUser", null)
    assertEquals(null to null, bizLogger.resolveOperator())
  }

  @Test
  fun `resolveOperator should return nulls when no authentication`() {
    assertNull(bizLogger.resolveOperator().first)
    assertNull(bizLogger.resolveOperator().second)
  }
}
