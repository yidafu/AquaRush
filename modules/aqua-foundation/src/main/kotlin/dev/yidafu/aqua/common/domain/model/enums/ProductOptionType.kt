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
 * 商品属性选项字典类型(F-PRO-009):
 * SPECIFICATION 商品规格(如 500ml/19L)、WATER_SOURCE 水源地(如 长白山)。
 * 商品表单对应字段的下拉选项由该字典配置提供(替换前端硬编码候选)。
 */
enum class ProductOptionType {
  /** 商品规格 */
  SPECIFICATION,

  /** 水源地 */
  WATER_SOURCE,
}
