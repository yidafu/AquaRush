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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 意见反馈实体
 * 与 PRD 12-operations-config.md §F-OPS-003 / 13-misc.md §核心实体 保持一致:
 * 类型、内容、图片、联系方式、状态、处理结果、处理人;状态流转 待处理 → 处理中 → 已处理。
 */
open class FeedbackModel(
  var id: Long? = null,
  var userId: Long = -1L,
  var feedbackType: FeedbackType = FeedbackType.OTHER,
  var content: String = "",
  var images: ArrayNode? = null,
  var contactInfo: String? = null,
  var status: FeedbackStatus = FeedbackStatus.PENDING,
  var handleResult: String? = null,
  var handledById: Long? = null,
  var handledByName: String? = null,
  var handledAt: LocalDateTime? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  /**
   * Get images as a list of URLs
   */
  fun getImagesAsList(): List<String> = images?.mapNotNull { if (it.isTextual) it.asText() else null } ?: emptyList()

  /**
   * Set images from a list of URLs (最多 3 张,由服务层校验)
   */
  fun setImagesFromList(imageUrls: List<String>) {
    images =
      if (imageUrls.isEmpty()) {
        null
      } else {
        val objectMapper = jacksonObjectMapper()
        objectMapper.valueToTree<ArrayNode>(imageUrls)
      }
  }
}
