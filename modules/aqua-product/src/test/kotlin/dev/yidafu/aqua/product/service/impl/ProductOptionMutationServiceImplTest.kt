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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.common.graphql.generated.CreateProductOptionInput
import dev.yidafu.aqua.common.graphql.generated.UpdateProductOptionInput
import dev.yidafu.aqua.product.domain.exception.DuplicateProductOptionValueException
import dev.yidafu.aqua.product.domain.exception.ProductOptionNotFoundException
import dev.yidafu.aqua.product.domain.repository.ProductOptionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import dev.yidafu.aqua.common.graphql.generated.ProductOptionType as GqlProductOptionType

/**
 * 商品属性选项字典写操作服务单元测试(F-PRO-009):
 * 增改删、同类型内值唯一、sortOrder 缺省排末尾、软删除。
 */
class ProductOptionMutationServiceImplTest :
  FunSpec({
    var productOptionRepository: ProductOptionRepository = mockk()
    var service: ProductOptionMutationServiceImpl = mockk()

    fun option(
      id: Long = 1L,
      type: ProductOptionType = ProductOptionType.SPECIFICATION,
      value: String = "19L",
      sortOrder: Int = 1,
      enabled: Boolean = true,
    ): ProductOptionModel = ProductOptionModel(id = id, optionType = type, value = value, sortOrder = sortOrder, enabled = enabled)

    beforeTest {
      productOptionRepository = mockk(relaxed = true)
      service = ProductOptionMutationServiceImpl(productOptionRepository)
    }

    test("createOption should save with trimmed value and default sortOrder to tail") {
      // Given
      every { productOptionRepository.findByOptionTypeAndValue(ProductOptionType.SPECIFICATION, "20L") } returns null
      every { productOptionRepository.findMaxSortOrder(ProductOptionType.SPECIFICATION) } returns 11
      every { productOptionRepository.save(any()) } answers { firstArg() }

      // When
      val result =
        service.createOption(
          adminId = 1L,
          adminName = "admin",
          input =
            CreateProductOptionInput(
              optionType = GqlProductOptionType.SPECIFICATION,
              value = "  20L  ",
              sortOrder = null,
              enabled = true,
            ),
        )

      // Then
      result.value shouldBe "20L"
      result.sortOrder shouldBe 12
      result.optionType shouldBe ProductOptionType.SPECIFICATION
      verify { productOptionRepository.save(any()) }
    }

    test("createOption should throw when value duplicates in same type") {
      // Given
      every { productOptionRepository.findByOptionTypeAndValue(ProductOptionType.SPECIFICATION, "19L") } returns option()

      // When / Then
      shouldThrow<DuplicateProductOptionValueException> {
        service.createOption(
          adminId = 1L,
          adminName = "admin",
          input = CreateProductOptionInput(optionType = GqlProductOptionType.SPECIFICATION, value = "19L", sortOrder = 1, enabled = true),
        )
      }
      verify(exactly = 0) { productOptionRepository.save(any()) }
    }

    test("updateOption should update value sortOrder and enabled") {
      // Given
      val existing = option(5L, ProductOptionType.WATER_SOURCE, "长白山", 1, true)
      every { productOptionRepository.findById(5L) } returns Optional.of(existing)
      every { productOptionRepository.findByOptionTypeAndValue(ProductOptionType.WATER_SOURCE, "昆仑山") } returns null
      every { productOptionRepository.save(any()) } answers { firstArg() }

      // When
      val result =
        service.updateOption(
          adminId = 1L,
          adminName = "admin",
          id = 5L,
          input = UpdateProductOptionInput(value = "昆仑山", sortOrder = 3, enabled = false),
        )

      // Then
      result.value shouldBe "昆仑山"
      result.sortOrder shouldBe 3
      result.enabled shouldBe false
    }

    test("updateOption should keep own value without duplicate check failure") {
      // Given
      val existing = option(5L, ProductOptionType.WATER_SOURCE, "长白山", 1, true)
      every { productOptionRepository.findById(5L) } returns Optional.of(existing)
      every { productOptionRepository.findByOptionTypeAndValue(ProductOptionType.WATER_SOURCE, "长白山") } returns existing
      every { productOptionRepository.save(any()) } answers { firstArg() }

      // When
      val result =
        service.updateOption(
          adminId = 1L,
          adminName = "admin",
          id = 5L,
          input = UpdateProductOptionInput(value = "长白山", sortOrder = null, enabled = true),
        )

      // Then(自身值不触发重复异常,缺省 sortOrder 保留原值)
      result.value shouldBe "长白山"
      result.sortOrder shouldBe 1
    }

    test("updateOption should throw when renaming to another existing value") {
      // Given
      val existing = option(5L, ProductOptionType.WATER_SOURCE, "长白山", 1, true)
      every { productOptionRepository.findById(5L) } returns Optional.of(existing)
      every { productOptionRepository.findByOptionTypeAndValue(ProductOptionType.WATER_SOURCE, "昆仑山") } returns
        option(6L, ProductOptionType.WATER_SOURCE, "昆仑山")

      // When / Then
      shouldThrow<DuplicateProductOptionValueException> {
        service.updateOption(
          adminId = 1L,
          adminName = "admin",
          id = 5L,
          input = UpdateProductOptionInput(value = "昆仑山", sortOrder = 1, enabled = true),
        )
      }
    }

    test("updateOption should throw when option missing") {
      // Given
      every { productOptionRepository.findById(999L) } returns Optional.empty()

      // When / Then
      shouldThrow<ProductOptionNotFoundException> {
        service.updateOption(
          adminId = 1L,
          adminName = "admin",
          id = 999L,
          input = UpdateProductOptionInput(value = "新值", sortOrder = 1, enabled = true),
        )
      }
    }

    test("deleteOption should soft delete existing option") {
      // Given
      val existing = option(5L)
      every { productOptionRepository.findById(5L) } returns Optional.of(existing)

      // When
      val result = service.deleteOption(1L, "admin", 5L)

      // Then
      result shouldBe true
      verify { productOptionRepository.delete(existing) }
    }

    test("deleteOption should throw when option missing") {
      // Given
      every { productOptionRepository.findById(999L) } returns Optional.empty()

      // When / Then
      shouldThrow<ProductOptionNotFoundException> {
        service.deleteOption(1L, "admin", 999L)
      }
    }
  })
