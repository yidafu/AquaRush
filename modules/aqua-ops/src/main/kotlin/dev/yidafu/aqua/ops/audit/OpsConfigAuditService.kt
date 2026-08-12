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
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 运营配置审计服务(R-OPS-01)
 * 统一写入现有日志表 user_action_logs(actionType 前缀 OPS_*):
 * - userId/username = 操作人
 * - target = "banner:123" / "feedback:456"
 * - properties = JSON {"before": ..., "after": ..., "description": ...}
 * 同步写入与业务同事务,审计失败使业务事务回滚,保证留痕一致。
 */
@Service
class OpsConfigAuditService(
  private val userActionLogService: UserActionLogService,
) {
  /**
   * 记录运营配置审计日志
   * @param before 变更前快照(任意可序列化对象,建议 Map 或实体)
   * @param after 变更后快照
   */
  fun record(
    targetType: String,
    targetId: Long?,
    actionType: String,
    operatorId: Long,
    operatorName: String,
    before: Any? = null,
    after: Any? = null,
    description: String? = null,
  ) {
    userActionLogService.saveUserActionLogSync(
      userId = operatorId.toString(),
      username = operatorName,
      actionType = actionType,
      target = if (targetId != null) "$targetType:$targetId" else targetType,
      pageUrl = "ops",
      properties = propertiesJson(before, after, description),
    )
  }

  private fun propertiesJson(
    before: Any?,
    after: Any?,
    description: String?,
  ): String {
    val objectMapper = jacksonObjectMapper()
    val node = objectMapper.createObjectNode()
    snapshot(before)?.let { node.set("before", it) }
    snapshot(after)?.let { node.set("after", it) }
    if (!description.isNullOrBlank()) {
      node.put("description", description)
    }
    return objectMapper.writeValueAsString(node)
  }

  private fun snapshot(value: Any?): JsonNode? = value?.let { jacksonObjectMapper().valueToTree(it) }

  companion object {
    // actionType 命名(actionType 为字符串文本,沿用 user_action_logs 现有约定)
    const val ACTION_BANNER_CREATE = "OPS_BANNER_CREATE"
    const val ACTION_BANNER_UPDATE = "OPS_BANNER_UPDATE"
    const val ACTION_BANNER_DELETE = "OPS_BANNER_DELETE"
    const val ACTION_BANNER_REORDER = "OPS_BANNER_REORDER"
    const val ACTION_FEEDBACK_PROCESS = "OPS_FEEDBACK_PROCESS"

    // targetType
    const val TARGET_BANNER = "banner"
    const val TARGET_FEEDBACK = "feedback"
  }
}
