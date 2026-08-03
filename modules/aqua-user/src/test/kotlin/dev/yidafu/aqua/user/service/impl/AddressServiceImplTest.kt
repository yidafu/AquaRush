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
import dev.yidafu.aqua.common.dto.AddressUpdateRequest
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.service.GeolocationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddressServiceImplTest {
  private lateinit var addressRepository: AddressRepository
  private lateinit var regionRepository: RegionRepository
  private lateinit var geolocationService: GeolocationService
  private lateinit var eventPublishService: SimplifiedEventPublishService
  private lateinit var addressService: AddressServiceImpl

  private fun createSampleAddress(): AddressModel =
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

  private fun createSampleAddress2(): AddressModel =
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

  @BeforeEach
  fun setUp() {
    addressRepository = mockk(relaxed = true)
    regionRepository = mockk()
    geolocationService = mockk()
    eventPublishService = mockk(relaxed = true)
    addressService =
      AddressServiceImpl(
        addressRepository,
        regionRepository,
        geolocationService,
        eventPublishService,
      )
  }

  @Test
  fun `getUserAddresses should return user addresses`() {
    // Given
    val addresses = listOf(createSampleAddress(), createSampleAddress2())
    every { addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L) } returns addresses

    // When
    val result = addressService.getUserAddresses(1L)

    // Then
    assertEquals(2, result.size)
    verify { addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(1L) }
  }

  @Test
  fun `getUserDefaultAddress should return default address`() {
    // Given
    every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns createSampleAddress()

    // When
    val result = addressService.getUserDefaultAddress(1L)

    // Then
    assertNotNull(result)
    assertTrue(result!!.isDefault)
  }

  @Test
  fun `getUserDefaultAddress should return null when no default`() {
    // Given
    every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns null

    // When
    val result = addressService.getUserDefaultAddress(1L)

    // Then
    assertNull(result)
  }

  @Test
  fun `getAddressById should return address when found`() {
    // Given
    every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())

    // When
    val result = addressService.getAddressById(1L)

    // Then
    assertNotNull(result)
    assertEquals(1L, result?.id)
  }

  @Test
  fun `createAddress should create address and set as default when no existing addresses`() {
    // Given
    val newAddress = createSampleAddress().also { it.id = null }
    every { addressRepository.findByUserId(1L) } returns emptyList()
    every { addressRepository.save(any()) } returns
      createSampleAddress().also {
        it.id = 5L
        it.isDefault = true
      }

    // When
    val result = addressService.createAddress(newAddress)

    // Then
    assertTrue(result.isDefault)
    verify { addressRepository.save(any()) }
  }

  @Test
  fun `createAddress should clear other defaults when setting as default`() {
    // Given
    val newAddress =
      createSampleAddress().also {
        it.id = null
        it.isDefault = true
      }
    every { addressRepository.findByUserId(1L) } returns listOf(createSampleAddress(), createSampleAddress2())
    every { addressRepository.save(any()) } returns
      createSampleAddress().also {
        it.id = 5L
        it.isDefault = true
      }

    // When
    addressService.createAddress(newAddress)

    // Then
    verify { addressRepository.clearDefaultAddresses(1L) }
  }

  @Test
  fun `updateAddress should update address successfully`() {
    // Given
    val updateRequest = AddressUpdateRequest(detailAddress = "新地址")
    every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())
    every { addressRepository.save(any()) } answers {
      val saved = createSampleAddress()
      saved.detailAddress = "新地址"
      saved
    }

    // When
    val result = addressService.updateAddress(1L, updateRequest)

    // Then
    assertNotNull(result)
  }

  @Test
  fun `deleteAddress should delete address successfully`() {
    // Given
    val sampleAddress = createSampleAddress()
    every { addressRepository.findById(1L) } returns Optional.of(sampleAddress)

    // When
    val result = addressService.deleteAddress(1L, 1L)

    // Then
    assertTrue(result)
  }

  @Test
  fun `deleteAddress should set new default when deleting default address`() {
    // Given
    val sampleAddress2 = createSampleAddress2()
    every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())
    every { addressRepository.findByUserId(1L) } returns listOf(sampleAddress2)
    every { addressRepository.save(any()) } answers {
      val saved = createSampleAddress2()
      saved.isDefault = true
      saved
    }

    // When
    val result = addressService.deleteAddress(1L, 1L)

    // Then
    assertTrue(result)
    verify { addressRepository.save(any()) }
  }

  @Test
  fun `deleteAddress should return false when address not found`() {
    // Given
    every { addressRepository.findById(1L) } returns Optional.empty()

    // When
    val result = addressService.deleteAddress(1L, 1L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `deleteAddress should return false when userId mismatch`() {
    // Given
    every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())

    // When
    val result = addressService.deleteAddress(1L, 999L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `setDefaultAddress should return false when address not found`() {
    // Given
    every { addressRepository.findById(1L) } returns Optional.empty()

    // When
    val result = addressService.setDefaultAddress(1L, 1L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `setDefaultAddress should return false when userId mismatch`() {
    // Given
    every { addressRepository.findById(1L) } returns Optional.of(createSampleAddress())

    // When
    val result = addressService.setDefaultAddress(1L, 999L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `findNearbyAddresses should return empty list for now`() {
    // When
    val result = addressService.findNearbyAddresses(114.0579, 22.5431, 5.0)

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `isDuplicateAddress should detect duplicate address`() {
    // Given
    val newAddress = createSampleAddress().also { it.id = null }
    every { addressRepository.findByUserId(1L) } returns listOf(createSampleAddress(), createSampleAddress2())

    // When
    val result = addressService.isDuplicateAddress(1L, newAddress)

    // Then
    assertTrue(result)
  }

  @Test
  fun `isDuplicateAddress should return false when no duplicate`() {
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
    assertFalse(result)
  }

  @Test
  fun `countByUserId should return address count`() {
    // Given
    every { addressRepository.countByUserId(1L) } returns 3

    // When
    val result = addressService.countByUserId(1L)

    // Then
    assertEquals(3, result)
  }

  @Test
  fun `save should save address and publish event`() {
    // Given
    every { addressRepository.save(any()) } returns createSampleAddress()

    // When
    val result = addressService.save(createSampleAddress())

    // Then
    assertNotNull(result)
    verify { eventPublishService.publishAddressUpdate(1L, 1L) }
  }

  @Test
  fun `deleteById should delete address by id`() {
    // When
    addressService.deleteById(1L)

    // Then
    verify { addressRepository.deleteById(1L) }
  }

  @Test
  fun `unsetDefaultAddresses should clear default addresses`() {
    // When
    addressService.unsetDefaultAddresses(1L)

    // Then
    verify { addressRepository.clearDefaultAddresses(1L) }
  }

  @Test
  fun `saveAll should save all addresses`() {
    // Given
    val addresses = listOf(createSampleAddress(), createSampleAddress2())
    every { addressRepository.saveAll(addresses) } returns addresses

    // When
    val result = addressService.saveAll(addresses)

    // Then
    assertEquals(2, result.size)
    verify { addressRepository.saveAll(addresses) }
  }

  @Test
  fun `findAllAddresses should return all addresses`() {
    // Given
    every { addressRepository.findAll() } returns listOf(createSampleAddress(), createSampleAddress2())

    // When
    val result = addressService.findAllAddresses()

    // Then
    assertEquals(2, result.size)
    verify { addressRepository.findAll() }
  }

  @Test
  fun `deleteAddressById should delete address by id for admin`() {
    // When
    val result = addressService.deleteAddressById(1L)

    // Then
    assertTrue(result)
    verify { addressRepository.deleteById(1L) }
  }

  @Test
  fun `searchByUserIdAndKeyword should return page of addresses`() {
    // Given
    val page: Page<AddressModel> = Page.empty()
    every { addressRepository.searchByUserIdAndKeyword(any(), any(), any()) } returns page

    // When
    val result = addressService.searchByUserIdAndKeyword(1L, "深圳", PageRequest.of(0, 10))

    // Then
    assertTrue(result.isEmpty)
    verify { addressRepository.searchByUserIdAndKeyword(1L, "深圳", PageRequest.of(0, 10)) }
  }
}
