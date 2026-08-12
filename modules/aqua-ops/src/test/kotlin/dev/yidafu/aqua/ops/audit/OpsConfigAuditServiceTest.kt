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
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * 运营配置审计服务单元测试(R-OPS-01:统一写入 user_action_logs,操作人/时间/前后值)
 */
class OpsConfigAuditServiceTest {
  private lateinit var userActionLogService: UserActionLogService
  private lateinit var service: OpsConfigAuditService

  @BeforeEach
  fun setUp() {
    userActionLogService = mockk(relaxed = true)
    service = OpsConfigAuditService(userActionLogService)
  }

  @Test
  @DisplayName("record 经 saveUserActionLogSync 写入 user_action_logs,properties 含前后值 JSON")
  fun `record writes to user action log with before and after`() {
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
    assertTrue(userIdSlot.captured == "1")
    assertTrue(usernameSlot.captured == "admin")
    assertTrue(actionTypeSlot.captured == OpsConfigAuditService.ACTION_BANNER_UPDATE)
    assertTrue(targetSlot.captured == "banner:42")
    assertTrue(propertiesSlot.captured.contains("\"before\""))
    assertTrue(propertiesSlot.captured.contains("\"after\""))
    assertTrue(propertiesSlot.captured.contains("旧标题"))
    assertTrue(propertiesSlot.captured.contains("新标题"))
  }

  @Test
  @DisplayName("record 无 targetId 时 target 仅含 targetType")
  fun `record target without id`() {
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
}
