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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceItemInput
import dev.yidafu.aqua.common.graphql.generated.SaveGroupProductPricesInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/** 分组商品专属价变更服务单元测试(F-MKT-002):批量 upsert/清空/clearUnlisted。 */
class GroupProductPriceMutationServiceImplTest :
  FunSpec({
    var groupProductPriceRepository: GroupProductPriceRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: GroupProductPriceMutationServiceImpl = mockk()

    fun price(
      id: Long,
      groupId: Long,
      productId: Long,
      priceCents: Long,
    ): GroupProductPriceModel =
      GroupProductPriceModel(
        id = id,
        customerGroupId = groupId,
        productId = productId,
        priceCents = priceCents,
      )

    beforeTest {
      groupProductPriceRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service = GroupProductPriceMutationServiceImpl(groupProductPriceRepository, bizLogger)
    }
    test("savePrices creates new price") {
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null
      val slot = slot<GroupProductPriceModel>()
      every { groupProductPriceRepository.save(capture(slot)) } answers { firstArg() }

      val result =
        service.savePrices(
          1L,
          "admin",
          SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 1500))),
        )

      result shouldBe true
      slot.captured.customerGroupId shouldBe 5L
      slot.captured.productId shouldBe 1L
      slot.captured.priceCents shouldBe 1500
      verify {
        bizLogger.logMarketingOperation(
          targetId = 5L,
          operation = "GROUP_PRICE_SAVED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("savePrices updates existing price") {
      val existing = price(1L, 5L, 1L, 1000)
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns existing
      every { groupProductPriceRepository.save(any()) } answers { firstArg() }

      service.savePrices(
        1L,
        "admin",
        SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 1800))),
      )

      existing.priceCents shouldBe 1800
      verify { groupProductPriceRepository.save(existing) }
    }
    test("savePrices null price deletes existing") {
      val existing = price(1L, 5L, 1L, 1000)
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns existing
      every { groupProductPriceRepository.delete(any<GroupProductPriceModel>()) } returns Unit

      service.savePrices(
        1L,
        "admin",
        SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = null))),
      )

      verify { groupProductPriceRepository.delete(existing) }
    }
    test("savePrices null price without existing does nothing") {
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null

      service.savePrices(
        1L,
        "admin",
        SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = null))),
      )

      verify(exactly = 0) { groupProductPriceRepository.delete(any<GroupProductPriceModel>()) }
      verify(exactly = 0) { groupProductPriceRepository.save(any()) }
    }
    test("savePrices clearUnlisted deletes unlisted") {
      val listed = price(2L, 5L, 2L, 1200)
      val unlisted = price(3L, 5L, 3L, 1300)
      every { groupProductPriceRepository.findByCustomerGroupId(5L) } returns listOf(listed, unlisted)
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 2L) } returns listed
      every { groupProductPriceRepository.save(any()) } answers { firstArg() }
      every { groupProductPriceRepository.delete(any<GroupProductPriceModel>()) } returns Unit

      service.savePrices(
        1L,
        "admin",
        SaveGroupProductPricesInput(
          customerGroupId = 5L,
          items = listOf(GroupProductPriceItemInput(productId = 2L, priceCents = 1100)),
          clearUnlisted = true,
        ),
      )

      verify { groupProductPriceRepository.delete(unlisted) }
      verify(exactly = 0) { groupProductPriceRepository.delete(listed) }
    }
    test("savePrices without clearUnlisted keeps others") {
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null
      every { groupProductPriceRepository.save(any()) } answers { firstArg() }

      service.savePrices(
        1L,
        "admin",
        SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 1500))),
      )

      verify(exactly = 0) { groupProductPriceRepository.findByCustomerGroupId(5L) }
    }
    test("savePrices invalid price throws") {
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null

      shouldThrow<IllegalArgumentException> {
        service.savePrices(
          1L,
          "admin",
          SaveGroupProductPricesInput(customerGroupId = 5L, items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 0))),
        )
      }

      shouldThrow<IllegalArgumentException> {
        service.savePrices(
          1L,
          "admin",
          SaveGroupProductPricesInput(
            customerGroupId = 5L,
            items = listOf(GroupProductPriceItemInput(productId = 1L, priceCents = 9999901)),
          ),
        )
      }
    }
  })
