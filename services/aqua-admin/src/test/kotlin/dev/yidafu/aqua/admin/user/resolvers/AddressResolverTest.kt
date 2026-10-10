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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AddressResolverTest :
  FunSpec({
    var addressService: AddressService = mockk()
    var resolver: AddressResolver = mockk()

    fun createSampleAddress(): AddressModel =
      AddressModel(
        id = 1L,
        userId = 1L,
        receiverName = "张三",
        phone = "13800138000",
        province = "广东省",
        provinceCode = "440000",
        city = "深圳市",
        cityCode = "440300",
        district = "南山区",
        districtCode = "440305",
        detailAddress = "科技园路1号",
        floor = 5,
      )

    fun validAddressInput(): AddressInput =
      AddressInput(
        city = "深圳市",
        cityCode = "440300",
        detailAddress = "科技园路1号",
        district = "南山区",
        districtCode = "440305",
        phone = "13800138000",
        province = "广东省",
        provinceCode = "440000",
        receiverName = "张三",
      )

    beforeTest {
      addressService = mockk(relaxed = true)
      resolver = AddressResolver(addressService)
    }
    test("userAddresses should map user addresses") {
      // Given
      every { addressService.findByUserId(1L) } returns listOf(createSampleAddress())

      // When
      val result = resolver.userAddresses(1L)

      // Then
      result.size shouldBe 1
      result[0].receiverName shouldBe "张三"
      verify { addressService.findByUserId(1L) }
    }
    test("searchAllAddresses should search with keyword and return page") {
      // Given
      every { addressService.searchAllAddresses("张三", PageRequest.of(0, 10)) } returns
        PageImpl(listOf(createSampleAddress()), PageRequest.of(0, 10), 1)

      // When
      val result = resolver.searchAllAddresses("张三", 0, 10)

      // Then
      result.content.size shouldBe 1
      result.content[0].receiverName shouldBe "张三"
      result.totalElements shouldBe 1
      result.totalPages shouldBe 1
      result.size shouldBe 10
      result.number shouldBe 0
      verify { addressService.searchAllAddresses("张三", PageRequest.of(0, 10)) }
    }
    test("createAdminAddress should save mapped address") {
      // Given
      every { addressService.save(any()) } returns createSampleAddress()

      // When
      val result = resolver.createAdminAddress(validAddressInput())

      // Then
      result.id shouldBe 1L
      verify { addressService.save(any()) }
    }
    test("batchImportAddresses should report counts") {
      // Given
      every { addressService.saveAll(any()) } returns listOf(createSampleAddress())

      // When
      val result = resolver.batchImportAddresses(listOf(validAddressInput(), validAddressInput()))

      // Then
      result.totalCount shouldBe 2
      result.successCount shouldBe 2
      result.failureCount shouldBe 0
      verify { addressService.saveAll(any()) }
    }
    test("updateAdminAddress should merge input into existing address and save") {
      // Given
      val existing = createSampleAddress()
      every { addressService.findById(1L) } returns existing
      every { addressService.save(any()) } returnsArgument 0

      // When
      val result = resolver.updateAdminAddress(1L, UpdateAddressInput(receiverName = "李四", detailAddress = "新地址2号"))

      // Then
      result.id shouldBe 1L
      result.receiverName shouldBe "李四"
      result.detailAddress shouldBe "新地址2号"
      // 未传入的字段保持不变
      result.phone shouldBe "13800138000"
      verify { addressService.findById(1L) }
      verify { addressService.save(existing) }
    }
    test("updateAdminAddress should throw when address not found") {
      // Given
      every { addressService.findById(999L) } returns null

      // When
      val e = shouldThrow<RuntimeException> { resolver.updateAdminAddress(999L, UpdateAddressInput(receiverName = "李四")) }

      // Then
      e.message shouldBe "更新地址失败: 地址不存在: 999"
      verify { addressService.findById(999L) }
      verify(exactly = 0) { addressService.save(any()) }
    }
    test("deleteAdminAddress should delegate") {
      // Given
      every { addressService.deleteAddressById(1L) } returns true

      // When
      val result = resolver.deleteAdminAddress(1L)

      // Then
      result shouldBe true
      verify { addressService.deleteAddressById(1L) }
    }
  })
