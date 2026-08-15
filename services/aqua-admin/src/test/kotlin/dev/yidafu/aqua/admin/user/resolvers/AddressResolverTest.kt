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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddressResolverTest {
  private lateinit var addressService: AddressService
  private lateinit var resolver: AddressResolver

  private fun createSampleAddress(): AddressModel =
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

  private fun validAddressInput(): AddressInput =
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

  @BeforeEach
  fun setUp() {
    addressService = mockk(relaxed = true)
    resolver = AddressResolver(addressService)
  }

  @Test
  fun `userAddresses should map user addresses`() {
    // Given
    every { addressService.findByUserId(1L) } returns listOf(createSampleAddress())

    // When
    val result = resolver.userAddresses(1L)

    // Then
    assertEquals(1, result.size)
    assertEquals("张三", result[0].receiverName)
    verify { addressService.findByUserId(1L) }
  }

  @Test
  fun `addresses should return all addresses`() {
    // Given
    every { addressService.findAllAddresses() } returns listOf(createSampleAddress())

    // When
    val result = resolver.addresses()

    // Then
    assertEquals(1, result.size)
    verify { addressService.findAllAddresses() }
  }

  @Test
  fun `searchAllAddresses should search with keyword`() {
    // Given
    every { addressService.searchAllAddresses("张三", PageRequest.of(0, 10)) } returns
      PageImpl(listOf(createSampleAddress()))

    // When
    val result = resolver.searchAllAddresses("张三", 0, 10)

    // Then
    assertEquals(1, result.size)
    verify { addressService.searchAllAddresses("张三", PageRequest.of(0, 10)) }
  }

  @Test
  fun `createAdminAddress should save mapped address`() {
    // Given
    every { addressService.save(any()) } returns createSampleAddress()

    // When
    val result = resolver.createAdminAddress(validAddressInput())

    // Then
    assertEquals(1L, result.id)
    verify { addressService.save(any()) }
  }

  @Test
  fun `batchImportAddresses should report counts`() {
    // Given
    every { addressService.saveAll(any()) } returns listOf(createSampleAddress())

    // When
    val result = resolver.batchImportAddresses(listOf(validAddressInput(), validAddressInput()))

    // Then
    assertEquals(2, result.totalCount)
    assertEquals(2, result.successCount)
    assertEquals(0, result.failureCount)
    verify { addressService.saveAll(any()) }
  }

  @Test
  fun `deleteAdminAddress should delegate`() {
    // Given
    every { addressService.deleteAddressById(1L) } returns true

    // When
    val result = resolver.deleteAdminAddress(1L)

    // Then
    assertTrue(result)
    verify { addressService.deleteAddressById(1L) }
  }
}
