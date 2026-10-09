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
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.util.MoneyUtils
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "products")
open class ProductModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false)
  var name: String = "",
  @Column(name = "subtitle", length = 500)
  var subtitle: String? = null,
  @Column(name = "price_cents", nullable = false)
  var price: Long = 0,
  @Column(name = "original_price_cents")
  var originalPrice: Long? = null,
  @Column(name = "thumbnail_url", nullable = false)
  var coverImageUrl: String = "",
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "detail_images", columnDefinition = "json")
  @Convert(converter = ArrayNodeConverter::class)
  var imageGallery: ArrayNode? = null,

  // 遗留死列(V100 即有,业务只写 detail_images):此前实体未映射,ddl-auto 测试库
  // 建不出该列,Exposed 全量映射的 SELECT 炸列 —— P4 补齐与 DDL 对齐。
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "image_gallery", columnDefinition = "json")
  @Convert(converter = ArrayNodeConverter::class)
  var imageGalleryLegacy: ArrayNode? = null,
  @Column(name = "specification", nullable = false, length = 100)
  var specification: String = "",
  @Column(name = "water_source", length = 200)
  var waterSource: String? = null,
  @Column(name = "mineral_content", length = 200)
  var mineralContent: String? = null,
  @Column(name = "stock_quantity", nullable = false)
  var stock: Int = 0,
  @Column(name = "sales_volume", nullable = false)
  var salesVolume: Int = 0,
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: ProductModelStatus = ProductModelStatus.OFFLINE,
  @Column(name = "sort_order", nullable = false)
  var sortOrder: Int = 999,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tags", columnDefinition = "json")
  @Convert(converter = ArrayNodeConverter::class)
  var tags: ArrayNode? = null,
  @Column(name = "description", columnDefinition = "TEXT")
  var detailContent: String? = null,

  // 遗留死列(V100 即有,业务实际用 description 列,见上一字段):Exposed 全量映射的
  // SELECT 会列出该列,ddl-auto 测试库必须建出 —— P4 补齐与 DDL 对齐,业务不读写。
  @Column(name = "detail_content", columnDefinition = "TEXT")
  var detailContentLegacy: String? = null,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "certificate_images", columnDefinition = "json")
  @Convert(converter = ArrayNodeConverter::class)
  var certificateImages: ArrayNode? = null,
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deposit_price_cents", nullable = false)
  var depositPriceCents: Long = 0L,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // Compatibility properties for existing code - returns prices in yuan as BigDecimal
  val priceYuan: BigDecimal
    get() = MoneyUtils.fromCents(price)

  val originalPriceYuan: BigDecimal?
    get() = originalPrice?.let { MoneyUtils.fromCents(it) }

  // Helper method to check if product is low stock
  fun isLowStock(threshold: Int = 10): Boolean = stock <= threshold

  // Helper method to check if product is available for sale
  // Note: isDeleted check is handled by Hibernate @SoftDelete filtering
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
