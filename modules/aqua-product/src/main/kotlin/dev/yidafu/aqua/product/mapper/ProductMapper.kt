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

package dev.yidafu.aqua.product.mapper

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.PriceSource
import dev.yidafu.aqua.common.graphql.generated.Product
import dev.yidafu.aqua.common.graphql.generated.ProductSearchInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus

/**
 * ProductModel → GraphQL Product 类型
 * 金额字段两侧均为分制 Long,直接赋值无需转换
 */
fun ProductModel.toGraphQL(): Product =
  Product(
    certificateImages = certificateImages,
    coverImageUrl = coverImageUrl,
    createdAt = createdAt,
    detailContent = detailContent,
    // 管理侧无用户分组上下文:有效价=目录价,未命中熟客价(R-MKT-01)
    effectivePriceCents = price,
    hasGroupPrice = false,
    id = id ?: 0L,
    imageGallery = imageGallery,
    mineralContent = mineralContent,
    name = name,
    originalPriceCents = originalPrice,
    priceCents = price,
    // 管理侧成交单价来源=目录价(R-MKT-02,用户端按登录用户解析)
    priceSource = PriceSource.CATALOG_PRICE,
    salesVolume = salesVolume,
    sortOrder = sortOrder,
    specification = specification,
    status = status.toGraphQL(),
    stock = stock,
    subtitle = subtitle,
    tags = tags,
    updatedAt = updatedAt,
    waterSource = waterSource,
  )

/**
 * ProductSearchInput → ProductSearchRequest
 *
 * 保留该中间 DTO 的原因:字段改名(minPriceCents/maxPriceCents → minPrice/maxPrice)、
 * 枚举换型(ProductStatus/ProductSortBy → 领域枚举/排序串),
 * 且服务端 activeProducts 直接构造 Request 注入过滤条件(status=ONLINE、minStock=0)。
 * 入参 page/size 由 resolver 单独组装 Pageable,search 字段无对应目标(维持既有丢弃行为)。
 */
fun ProductSearchInput.toRequest(): ProductSearchRequest =
  ProductSearchRequest(
    keyword = keyword,
    minPrice = minPriceCents,
    maxPrice = maxPriceCents,
    maxSalesVolume = maxSalesVolume,
    minSalesVolume = minSalesVolume,
    maxStock = maxStock,
    minStock = minStock,
    sortBy = sortBy.toString(),
    status = status?.toDomain(),
  )

/**
 * ProductModelStatus ↔ ProductStatus 枚举同名互转
 */
fun ProductModelStatus.toGraphQL(): ProductStatus = ProductStatus.valueOf(name)

fun ProductStatus.toDomain(): ProductModelStatus = ProductModelStatus.valueOf(name)
