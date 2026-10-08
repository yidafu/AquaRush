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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.api.dto.ProductOptionCreateRequest
import dev.yidafu.aqua.api.dto.ProductOptionUpdateRequest
import dev.yidafu.aqua.api.service.product.ProductOptionMutationService
import dev.yidafu.aqua.api.service.product.ProductOptionQueryService
import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.common.graphql.generated.CreateProductOptionInput
import dev.yidafu.aqua.common.graphql.generated.UpdateProductOptionInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import dev.yidafu.aqua.common.graphql.generated.ProductOptionType as GqlProductOptionType

/**
 * 商品属性选项字典 Resolver 单元测试(F-PRO-009):
 * 查询:类型过滤 + enabledOnly 透传;写操作:创建/更新/删除的入参映射与返回映射。
 */
class AdminProductOptionResolverTest :
  FunSpec({
    var queryService: ProductOptionQueryService = mockk()
    var mutationService: ProductOptionMutationService = mockk()
    var queryResolver: AdminProductOptionQueryResolver = mockk()
    var mutationResolver: AdminProductOptionMutationResolver = mockk()

    fun option(
      id: Long = 1L,
      type: ProductOptionType = ProductOptionType.SPECIFICATION,
      value: String = "19L",
      sortOrder: Int = 1,
      enabled: Boolean = true,
    ): ProductOptionModel = ProductOptionModel(id = id, optionType = type, value = value, sortOrder = sortOrder, enabled = enabled)

    val principal =
      UserPrincipal(
        id = 1L,
        _username = "admin",
        userType = "ADMIN",
        _authorities = emptyList(),
      )

    beforeTest {
      queryService = mockk(relaxed = true)
      mutationService = mockk(relaxed = true)
      queryResolver = AdminProductOptionQueryResolver(queryService)
      mutationResolver = AdminProductOptionMutationResolver(mutationService)
    }

    test("productOptions should query with mapped type and enabledOnly") {
      // Given
      every { queryService.listOptions(ProductOptionType.SPECIFICATION, true) } returns listOf(option())

      // When
      val result = queryResolver.productOptions(GqlProductOptionType.SPECIFICATION, enabledOnly = true)

      // Then
      result.size shouldBe 1
      result[0].value shouldBe "19L"
      result[0].optionType shouldBe GqlProductOptionType.SPECIFICATION
      verify { queryService.listOptions(ProductOptionType.SPECIFICATION, true) }
    }

    test("productOptions should pass enabledOnly=false by default") {
      // Given
      every { queryService.listOptions(ProductOptionType.WATER_SOURCE, false) } returns
        listOf(option(2L, ProductOptionType.WATER_SOURCE, "长白山", enabled = false))

      // When
      val result = queryResolver.productOptions(GqlProductOptionType.WATER_SOURCE)

      // Then
      result.size shouldBe 1
      result[0].enabled shouldBe false
      verify { queryService.listOptions(ProductOptionType.WATER_SOURCE, false) }
    }

    test("createProductOption should map input and return mapped option") {
      // Given
      every {
        mutationService.createOption(1L, "admin", any())
      } answers {
        val request = arg<ProductOptionCreateRequest>(2)
        ProductOptionModel(
          id = 9L,
          optionType = request.optionType,
          value = request.value,
          sortOrder = request.sortOrder ?: 1,
          enabled = request.enabled,
        )
      }

      // When
      val result =
        mutationResolver.createProductOption(
          input = CreateProductOptionInput(optionType = GqlProductOptionType.SPECIFICATION, value = "18.9L", sortOrder = 5, enabled = true),
          principal = principal,
        )

      // Then
      result.id shouldBe 9L
      result.value shouldBe "18.9L"
      result.sortOrder shouldBe 5
      result.optionType shouldBe GqlProductOptionType.SPECIFICATION
      verify { mutationService.createOption(1L, "admin", any()) }
    }

    test("updateProductOption should map input and return mapped option") {
      // Given
      every {
        mutationService.updateOption(1L, "admin", 3L, any())
      } answers {
        val request = arg<ProductOptionUpdateRequest>(3)
        ProductOptionModel(
          id = 3L,
          optionType = ProductOptionType.WATER_SOURCE,
          value = request.value,
          sortOrder = request.sortOrder ?: 1,
          enabled = request.enabled,
        )
      }

      // When
      val result =
        mutationResolver.updateProductOption(
          id = 3L,
          input = UpdateProductOptionInput(value = "千岛湖", sortOrder = 2, enabled = false),
          principal = principal,
        )

      // Then
      result.value shouldBe "千岛湖"
      result.enabled shouldBe false
      verify { mutationService.updateOption(1L, "admin", 3L, any()) }
    }

    test("deleteProductOption should delegate to service") {
      // Given
      every { mutationService.deleteOption(1L, "admin", 3L) } returns true

      // When
      val result = mutationResolver.deleteProductOption(id = 3L, principal = principal)

      // Then
      result shouldBe true
      verify { mutationService.deleteOption(1L, "admin", 3L) }
    }
  })
