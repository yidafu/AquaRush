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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class AddressServiceImplTest :
  FunSpec({
    var addressRepository: AddressRepository = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var bizLogger: BizLogger = mockk()
    var addressService: AddressServiceImpl = mockk()

    fun createSampleAddress(): AddressModel =
      AddressModel(
        id = 1L,
        userId = 1L,
        receiverName = "张三",
        phone = "13800138000",
        province = "广东省",
        city = "深圳市",
        district = "南山区",
        detailAddress = "测试地址1号",
        provinceCode = "440000",
        cityCode = "440300",
        districtCode = "440305",
        isDefault = true,
        latitude = 22.5431,
        longitude = 114.0579,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createSampleAddress2(): AddressModel =
      AddressModel(
        id = 2L,
        userId = 1L,
        receiverName = "李四",
        phone = "13800138001",
        province = "广东省",
        city = "广州市",
        district = "天河区",
        detailAddress = "测试地址2号",
        provinceCode = "440000",
        cityCode = "440100",
        districtCode = "440106",
        isDefault = false,
        latitude = 23.1291,
        longitude = 113.2644,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      addressRepository = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      addressService =
        AddressServiceImpl(
          addressRepository,
          eventPublishService,
          bizLogger,
        )
    }
    test("setDefaultAddress should return false when address not found") {
      // Given
      every { addressRepository.findById(1L) } returns Optional.empty()

      // When
      val result = addressService.setDefaultAddress(1L, 1L)

      // Then
      result shouldBe false
    }
    test("setDefaultAddress should return false when userId mismatch") {
      // Given
      every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())

      // When
      val result = addressService.setDefaultAddress(1L, 999L)

      // Then
      result shouldBe false
    }
    test("findNearbyAddresses should return empty list for now") {
      // When
      val result = addressService.findNearbyAddresses(114.0579, 22.5431, 5.0)

      // Then
      result.isEmpty() shouldBe true
    }
    test("isDuplicateAddress should detect duplicate address") {
      // Given
      val newAddress = createSampleAddress().also { it.id = null }
      every { addressRepository.findByUserId(1L) } returns listOf(createSampleAddress(), createSampleAddress2())

      // When
      val result = addressService.isDuplicateAddress(1L, newAddress)

      // Then
      result shouldBe true
    }
    test("isDuplicateAddress should return false when no duplicate") {
      // Given
      val newAddress =
        createSampleAddress().also {
          it.id = null
          it.district = "福田区"
          it.detailAddress = "新地址"
        }
      every { addressRepository.findByUserId(1L) } returns listOf(createSampleAddress(), createSampleAddress2())

      // When
      val result = addressService.isDuplicateAddress(1L, newAddress)

      // Then
      result shouldBe false
    }
    test("countByUserId should return address count") {
      // Given
      every { addressRepository.countByUserId(1L) } returns 3

      // When
      val result = addressService.countByUserId(1L)

      // Then
      result shouldBe 3
    }
    test("save should save address and publish event") {
      // Given
      every { addressRepository.save(any()) } returns createSampleAddress()

      // When
      val result = addressService.save(createSampleAddress())

      // Then
      result.shouldNotBeNull()
      verify { eventPublishService.publishAddressUpdate(1L, 1L) }
    }
    test("deleteById should delete address by id") {
      // Given(地址不存在时仍记录业务日志)
      every { addressRepository.findById(1L) } returns Optional.empty()

      // When
      addressService.deleteById(1L)

      // Then
      verify { addressRepository.deleteById(1L) }
    }
    test("unsetDefaultAddresses should clear default addresses") {
      // When
      addressService.unsetDefaultAddresses(1L)

      // Then
      verify { addressRepository.clearDefaultAddresses(1L) }
    }
    test("saveAll should save all addresses") {
      // Given
      val addresses = listOf(createSampleAddress(), createSampleAddress2())
      every { addressRepository.saveAll(addresses) } returns addresses

      // When
      val result = addressService.saveAll(addresses)

      // Then
      result.size shouldBe 2
      verify { addressRepository.saveAll(addresses) }
    }
    test("deleteAddressById should delete address by id for admin") {
      // When
      val result = addressService.deleteAddressById(1L)

      // Then
      result shouldBe true
      verify { addressRepository.deleteById(1L) }
    }
  })
