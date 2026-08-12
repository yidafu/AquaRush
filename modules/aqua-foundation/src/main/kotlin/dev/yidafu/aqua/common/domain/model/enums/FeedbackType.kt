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
 * 意见反馈类型
 * 对齐用户端反馈页 6 类(frontend/user-weapp/src/pages/feedback/index.tsx FEEDBACK_TYPES)。
 */
enum class FeedbackType(
  val label: String,
) {
  PRODUCT_QUALITY("产品质量问题"),
  DELIVERY_SERVICE("配送服务问题"),
  APP_USAGE("小程序使用问题"),
  SUGGESTION("功能建议"),
  COMPLAINT("投诉建议"),
  OTHER("其他问题"),
}
