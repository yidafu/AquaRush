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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.converter.ArrayNodeConverter
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 意见反馈实体
 * 与 PRD 12-operations-config.md §F-OPS-003 / 13-misc.md §核心实体 保持一致:
 * 类型、内容、图片、联系方式、状态、处理结果、处理人;状态流转 待处理 → 处理中 → 已处理。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "feedbacks",
  indexes = [Index(name = "idx_feedbacks_status_created_at", columnList = "status, created_at")],
)
open class FeedbackModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  var userId: Long = -1L,
  @Column(name = "feedback_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var feedbackType: FeedbackType = FeedbackType.OTHER,
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  var content: String = "",
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "images", columnDefinition = "json")
  @Convert(converter = ArrayNodeConverter::class)
  var images: ArrayNode? = null,
  @Column(name = "contact_info", length = 100)
  var contactInfo: String? = null,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: FeedbackStatus = FeedbackStatus.PENDING,
  @Column(name = "handle_result", columnDefinition = "TEXT")
  var handleResult: String? = null,
  @Column(name = "handled_by_id")
  var handledById: Long? = null,
  @Column(name = "handled_by_name", length = 100)
  var handledByName: String? = null,
  @Column(name = "handled_at")
  var handledAt: LocalDateTime? = null,
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

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
