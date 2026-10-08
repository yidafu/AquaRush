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

package dev.yidafu.aqua.ops.audit

import dev.yidafu.aqua.logging.service.UserActionLogService
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * 运营配置审计服务单元测试(R-OPS-01:统一写入 user_action_logs,操作人/时间/前后值)
 */
class OpsConfigAuditServiceTest :
  FunSpec({
    var userActionLogService: UserActionLogService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: OpsConfigAuditService = mockk()

    beforeTest {
      userActionLogService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service = OpsConfigAuditService(userActionLogService, bizLogger)
    }
    test("record writes to user action log with before and after") {
      val propertiesSlot = slot<String>()
      val userIdSlot = slot<String>()
      val usernameSlot = slot<String>()
      val actionTypeSlot = slot<String>()
      val targetSlot = slot<String>()

      service.record(
        targetType = OpsConfigAuditService.TARGET_BANNER,
        targetId = 42L,
        actionType = OpsConfigAuditService.ACTION_BANNER_UPDATE,
        operatorId = 1L,
        operatorName = "admin",
        before = mapOf("title" to "旧标题"),
        after = mapOf("title" to "新标题"),
      )

      verify {
        userActionLogService.saveUserActionLogSync(
          userId = capture(userIdSlot),
          username = capture(usernameSlot),
          actionType = capture(actionTypeSlot),
          target = capture(targetSlot),
          pageUrl = "ops",
          properties = capture(propertiesSlot),
        )
      }
      (userIdSlot.captured == "1") shouldBe true
      (usernameSlot.captured == "admin") shouldBe true
      (actionTypeSlot.captured == OpsConfigAuditService.ACTION_BANNER_UPDATE) shouldBe true
      (targetSlot.captured == "banner:42") shouldBe true
      propertiesSlot.captured.contains("\"before\"") shouldBe true
      propertiesSlot.captured.contains("\"after\"") shouldBe true
      propertiesSlot.captured.contains("旧标题") shouldBe true
      propertiesSlot.captured.contains("新标题") shouldBe true
    }
    test("record target without id") {
      service.record(
        targetType = OpsConfigAuditService.TARGET_FEEDBACK,
        targetId = null,
        actionType = OpsConfigAuditService.ACTION_FEEDBACK_PROCESS,
        operatorId = 1L,
        operatorName = "admin",
      )

      verify {
        userActionLogService.saveUserActionLogSync(
          userId = "1",
          username = "admin",
          actionType = OpsConfigAuditService.ACTION_FEEDBACK_PROCESS,
          target = OpsConfigAuditService.TARGET_FEEDBACK,
          pageUrl = "ops",
          properties = any(),
        )
      }
    }
    test("record writes business log via bizLogger") {
      service.record(
        targetType = OpsConfigAuditService.TARGET_BANNER,
        targetId = 42L,
        actionType = OpsConfigAuditService.ACTION_BANNER_CREATE,
        operatorId = 1L,
        operatorName = "admin",
        description = "direction=UP",
      )

      verify(exactly = 1) {
        bizLogger.logOpsOperation(
          targetType = OpsConfigAuditService.TARGET_BANNER,
          targetId = 42L,
          operation = OpsConfigAuditService.ACTION_BANNER_CREATE,
          result = "SUCCESS",
          operatorId = 1L,
          operatorName = "admin",
          additionalData = mapOf("description" to "direction=UP"),
        )
      }
    }
  })
