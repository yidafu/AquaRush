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

package dev.yidafu.aqua.common.domain.model.enums

/**
 * 配送照片核验状态(G23/F-ORD-021 后台核验流)
 * null(未核验) 与 PENDING 均视为「待核验」;APPROVED=通过,REJECTED=驳回。
 */
enum class DeliveryPhotoReviewStatus {
  PENDING, // 待核验(已上传照片未审核)
  APPROVED, // 已通过
  REJECTED, // 已驳回
  ;

  companion object {
    fun fromValue(value: String): DeliveryPhotoReviewStatus =
      when (value) {
        "PENDING" -> PENDING
        "APPROVED" -> APPROVED
        "REJECTED" -> REJECTED
        else -> throw IllegalArgumentException("Unknown DeliveryPhotoReviewStatus value: '" + value + "'")
      }
  }
}
