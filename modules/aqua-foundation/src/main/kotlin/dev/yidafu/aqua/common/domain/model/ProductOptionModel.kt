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

import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import java.time.LocalDateTime

/**
 * 商品属性选项字典实体(F-PRO-009):
 * 商品表单「规格 / 水源地」下拉选项的后台可配置来源,替换前端硬编码候选列表。
 * - optionType + value 组合在未删除行内唯一(服务层校验,配合软删除);
 * - 禁用(enabled=false)选项在商品表单下拉中不展示(已使用中的存量值仍回显)。
 */
open class ProductOptionModel(
  var id: Long? = null,
  var optionType: ProductOptionType,
  var value: String,
  var sortOrder: Int = DEFAULT_SORT_ORDER,
  var enabled: Boolean = true,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  companion object {
    /** 默认排序号(新建时未指定则排到同类型末尾) */
    const val DEFAULT_SORT_ORDER = 999
  }
}
