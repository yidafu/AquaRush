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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientAddressQueryResolverTest {
  private lateinit var addressService: AddressService
  private lateinit var resolver: ClientAddressQueryResolver
  private lateinit var userPrincipal: UserPrincipal

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
      isDefault = true,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun createOtherUserAddress(): AddressModel =
    AddressModel(
      id = 2L,
      userId = 2L,
      receiverName = "李四",
      phone = "13800138001",
      province = "广东省",
      city = "深圳市",
      district = "南山区",
      detailAddress = "其他用户地址",
      isDefault = false,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    addressService = mockk(relaxed = true)
    resolver = ClientAddressQueryResolver(addressService)

    userPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  fun `userAddresses should return all user addresses`() {
    // Given
    val addresses = listOf(createSampleAddress())
    every { addressService.findByUserId(1L) } returns addresses

    // When
    val result = resolver.userAddresses(userPrincipal)

    // Then
    assertEquals(1, result.size)
    verify { addressService.findByUserId(1L) }
  }

  @Test
  fun `userDefaultAddress should return default address`() {
    // Given
    every { addressService.findDefaultByUserId(1L) } returns createSampleAddress()

    // When
    val result = resolver.userDefaultAddress(userPrincipal)

    // Then
    assertNotNull(result)
  }

  @Test
  fun `userDefaultAddress should return null when no default`() {
    // Given
    every { addressService.findDefaultByUserId(1L) } returns null

    // When
    val result = resolver.userDefaultAddress(userPrincipal)

    // Then
    assertNull(result)
  }

  @Test
  fun `address should return address when it belongs to user`() {
    // Given
    every { addressService.findById(1L) } returns createSampleAddress()

    // When
    val result = resolver.address(1L, userPrincipal)

    // Then
    assertNotNull(result)
    assertEquals(1L, result?.id)
  }

  @Test
  fun `address should throw exception when accessing other user address`() {
    // Given
    every { addressService.findById(1L) } returns createOtherUserAddress()

    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> { resolver.address(1L, userPrincipal) }
    assertTrue(exception.message!!.contains("无权访问"))
  }

  @Test
  fun `address should return null when address not found`() {
    // Given
    every { addressService.findById(999L) } returns null

    // When
    val result = resolver.address(999L, userPrincipal)

    // Then
    assertNull(result)
  }

  @Test
  fun `userAddressCount should return address count`() {
    // Given
    every { addressService.countByUserId(1L) } returns 5

    // When
    val result = resolver.userAddressCount(userPrincipal)

    // Then
    assertEquals(5, result)
  }

  @Test
  fun `getNearbyDeliveryWorkers should throw exception when accessing other user address`() {
    // Given
    every { addressService.findById(1L) } returns createOtherUserAddress()

    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> { resolver.getNearbyDeliveryWorkers(1L, 5.0, 10, userPrincipal) }
    assertTrue(exception.message!!.contains("无权访问"))
  }

  @Test
  fun `getNearbyDeliveryWorkers should return empty list`() {
    // Given
    every { addressService.findById(1L) } returns createSampleAddress()

    // When
    val result = resolver.getNearbyDeliveryWorkers(1L, 5.0, 10, userPrincipal)

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `getNearbyAddresses should return empty list`() {
    // When
    val result = resolver.getNearbyAddresses(114.0579, 22.5431, 2.0, 20, userPrincipal)

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `validateAddressForDelivery should throw exception when accessing other user address`() {
    // Given
    every { addressService.findById(1L) } returns createOtherUserAddress()

    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> { resolver.validateAddressForDelivery(1L, userPrincipal) }
    assertTrue(exception.message!!.contains("无权访问"))
  }

  @Test
  fun `validateAddressForDelivery should return validation result`() {
    // Given
    every { addressService.findById(1L) } returns createSampleAddress()

    // When
    val result = resolver.validateAddressForDelivery(1L, userPrincipal)

    // Then
    assertNotNull(result)
    assertTrue(result.isValid)
    assertTrue(result.isInRange)
    assertEquals("30-45分钟", result.estimatedDeliveryTime)
  }
}
