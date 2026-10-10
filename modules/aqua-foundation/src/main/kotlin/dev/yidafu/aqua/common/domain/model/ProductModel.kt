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

import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.util.MoneyUtils
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime

open class ProductModel(
  var id: Long? = null,
  var name: String = "",
  var subtitle: String? = null,
  var price: Long = 0,
  var originalPrice: Long? = null,
  var coverImageUrl: String = "",
  var imageGallery: ArrayNode? = null,
  // 遗留死列(V100 即有,业务只写 detail_images):此前实体未映射,ddl-auto 测试库
  // 建不出该列,Exposed 全量映射的 SELECT 炸列 —— P4 补齐与 DDL 对齐。
  var imageGalleryLegacy: ArrayNode? = null,
  var specification: String = "",
  var waterSource: String? = null,
  var mineralContent: String? = null,
  var stock: Int = 0,
  var salesVolume: Int = 0,
  var status: ProductModelStatus = ProductModelStatus.OFFLINE,
  var sortOrder: Int = 999,
  var tags: ArrayNode? = null,
  var detailContent: String? = null,
  // 遗留死列(V100 即有,业务实际用 description 列,见上一字段):Exposed 全量映射的
  // SELECT 会列出该列,ddl-auto 测试库必须建出 —— P4 补齐与 DDL 对齐,业务不读写。
  var detailContentLegacy: String? = null,
  var certificateImages: ArrayNode? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  var depositPriceCents: Long = 0L,
) : SoftDeletable {
  // Compatibility properties for existing code - returns prices in yuan as BigDecimal
  val priceYuan: BigDecimal
    get() = MoneyUtils.fromCents(price)

  val originalPriceYuan: BigDecimal?
    get() = originalPrice?.let { MoneyUtils.fromCents(it) }

  // Helper method to check if product is low stock
  fun isLowStock(threshold: Int = 10): Boolean = stock <= threshold

  // Helper method to check if product is available for sale
  // Note: isDeleted check is handled by Hibernate  filtering
  fun isAvailable(): Boolean = status == ProductModelStatus.ONLINE && stock > 0

  // Helper methods for JSON field handling

  /**
   * Get tags as a list of strings
   */
  fun getTagsAsList(): List<String> = tags?.mapNotNull { if (it.isTextual) it.asText() else null } ?: emptyList()

  /**
   * Set tags from a list of strings
   */
  fun setTagsFromList(tagList: List<String>) {
    tags =
      if (tagList.isEmpty()) {
        null
      } else {
        val objectMapper = jacksonObjectMapper()
        objectMapper.valueToTree<ArrayNode>(tagList)
      }
  }

  /**
   * Get image gallery as a list of URLs
   */
  fun getImageGalleryAsList(): List<String> = imageGallery?.mapNotNull { if (it.isTextual) it.asText() else null } ?: emptyList()

  /**
   * Set image gallery from a list of URLs
   */
  fun setImageGalleryFromList(imageUrls: List<String>) {
    imageGallery =
      if (imageUrls.isEmpty()) {
        null
      } else {
        val objectMapper = jacksonObjectMapper()
        objectMapper.valueToTree<ArrayNode>(imageUrls)
      }
  }

  /**
   * Get certificate images as a list of URLs
   */
  fun getCertificateImagesAsList(): List<String> = certificateImages?.mapNotNull { if (it.isTextual) it.asText() else null } ?: emptyList()

  /**
   * Set certificate images from a list of URLs
   */
  fun setCertificateImagesFromList(imageUrls: List<String>) {
    certificateImages =
      if (imageUrls.isEmpty()) {
        null
      } else {
        val objectMapper = jacksonObjectMapper()
        objectMapper.valueToTree<ArrayNode>(imageUrls)
      }
  }
}
