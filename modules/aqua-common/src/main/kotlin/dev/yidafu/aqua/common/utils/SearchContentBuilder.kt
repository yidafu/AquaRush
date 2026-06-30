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

package dev.yidafu.aqua.common.utils

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.ProductModel

/**
 * 构建订单搜索内容的工具类
 * 用于生成 orders 表的 search_content 字段值
 */
object SearchContentBuilder {

  /**
   * 构建搜索内容字符串
   * 包含：详细地址、收货人姓名、手机号、商品名称
   */
  fun buildSearchContent(
    address: AddressModel?,
    product: ProductModel?,
  ): String {
    val parts = mutableListOf<String>()

    address?.let {
      it.detailAddress?.let { addr -> parts.add(addr) }
      it.receiverName?.let { name -> parts.add(name) }
      it.phone?.let { phone -> parts.add(phone) }
    }

    product?.let {
      it.name?.let { name -> parts.add(name) }
    }

    return parts.joinToString(" ")
  }

  /**
   * 构建搜索内容字符串（无关联实体时使用）
   */
  fun buildSearchContent(
    detailAddress: String?,
    receiverName: String?,
    phone: String?,
    productName: String?,
  ): String {
    val parts = mutableListOf<String>()

    detailAddress?.let { parts.add(it) }
    receiverName?.let { parts.add(it) }
    phone?.let { parts.add(it) }
    productName?.let { parts.add(it) }

    return parts.joinToString(" ")
  }
}
