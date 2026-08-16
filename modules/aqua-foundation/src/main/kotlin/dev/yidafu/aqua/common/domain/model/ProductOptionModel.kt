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
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 商品属性选项字典实体(F-PRO-009):
 * 商品表单「规格 / 水源地」下拉选项的后台可配置来源,替换前端硬编码候选列表。
 * - optionType + value 组合在未删除行内唯一(服务层校验,配合软删除);
 * - 禁用(enabled=false)选项在商品表单下拉中不展示(已使用中的存量值仍回显)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "product_options",
  indexes = [
    Index(name = "idx_product_options_type_enabled", columnList = "option_type,is_deleted"),
    Index(name = "idx_product_options_type_sort", columnList = "option_type,sort_order"),
  ],
)
open class ProductOptionModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "option_type", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  var optionType: ProductOptionType,
  @Column(name = "option_value", nullable = false, length = 100)
  var value: String,
  @Column(name = "sort_order", nullable = false)
  var sortOrder: Int = DEFAULT_SORT_ORDER,
  @Column(name = "enabled", nullable = false)
  var enabled: Boolean = true,
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

  companion object {
    /** 默认排序号(新建时未指定则排到同类型末尾) */
    const val DEFAULT_SORT_ORDER = 999
  }
}
