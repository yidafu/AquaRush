package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.ProductModelStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class ProductQuery(
  @field:Size(max = 100, message = "搜索关键词长度不能超过100个字符")
  val keyword: String? = null,
  @field:Min(value = 1, message = "最小价格不能小于1分")
  @field:Max(value = 9999900, message = "最小价格不能大于99999元")
  val minPrice: Long? = null,
  @field:Positive(message = "最大价格必须为正数")
  @field:Min(value = 1, message = "最大价格不能小于1分")
  @field:Max(value = 9999900, message = "最大价格不能大于99999元")
  val maxPrice: Long? = null,
  @field:Min(value = 0, message = "最大销量不能小于0")
  val maxSalesVolume: Int? = null,
  @field:Min(value = 0, message = "最小销量不能小于0")
  val minSalesVolume: Int? = null,
  @field:Min(value = 0, message = "最大库存不能小于0")
  val maxStock: Int? = null,
  @field:Positive(message = "最小价格必须为正数")
  @field:Min(value = 0, message = "最小库存不能小于0")
  val minStock: Int? = null,
  val sortBy: String? = null,
  val status: ProductModelStatus? = null,
)
