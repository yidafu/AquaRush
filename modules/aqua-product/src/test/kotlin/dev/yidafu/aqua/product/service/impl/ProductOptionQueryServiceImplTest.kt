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
import dev.yidafu.aqua.product.domain.repository.ProductOptionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 商品属性选项字典查询服务单元测试(F-PRO-009)
 */
class ProductOptionQueryServiceImplTest :
  FunSpec({
    var productOptionRepository: ProductOptionRepository = mockk()
    var service: ProductOptionQueryServiceImpl = mockk()

    fun option(
      id: Long = 1L,
      type: ProductOptionType = ProductOptionType.SPECIFICATION,
      value: String = "19L",
      sortOrder: Int = 1,
      enabled: Boolean = true,
    ): ProductOptionModel = ProductOptionModel(id = id, optionType = type, value = value, sortOrder = sortOrder, enabled = enabled)

    beforeTest {
      productOptionRepository = mockk(relaxed = true)
      service = ProductOptionQueryServiceImpl(productOptionRepository)
    }

    test("listOptions with enabledOnly=true should query enabled options only") {
      // Given
      every {
        productOptionRepository.findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(ProductOptionType.SPECIFICATION)
      } returns listOf(option(), option(2L, value = "500ml", sortOrder = 2))

      // When
      val result = service.listOptions(ProductOptionType.SPECIFICATION, enabledOnly = true)

      // Then
      result.size shouldBe 2
      result[0].value shouldBe "19L"
      verify {
        productOptionRepository.findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(ProductOptionType.SPECIFICATION)
      }
    }

    test("listOptions with enabledOnly=false should query all options") {
      // Given
      every {
        productOptionRepository.findByOptionTypeOrderBySortOrderAscCreatedAtAsc(ProductOptionType.WATER_SOURCE)
      } returns listOf(option(1L, ProductOptionType.WATER_SOURCE, "长白山", 1, false))

      // When
      val result = service.listOptions(ProductOptionType.WATER_SOURCE, enabledOnly = false)

      // Then
      result.size shouldBe 1
      result[0].enabled shouldBe false
      verify {
        productOptionRepository.findByOptionTypeOrderBySortOrderAscCreatedAtAsc(ProductOptionType.WATER_SOURCE)
      }
    }

    test("getOption returns option when exists") {
      // Given
      val opt = option(7L)
      every { productOptionRepository.findById(7L) } returns Optional.of(opt)

      // When
      val result = service.getOption(7L)

      // Then
      result shouldBe opt
    }

    test("getOption returns null when missing") {
      // Given
      every { productOptionRepository.findById(999L) } returns Optional.empty()

      // When
      val result = service.getOption(999L)

      // Then
      result shouldBe null
    }
  })
