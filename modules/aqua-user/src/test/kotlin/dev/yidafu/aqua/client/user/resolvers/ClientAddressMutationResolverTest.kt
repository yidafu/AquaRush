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
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
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
import kotlin.test.assertTrue

class ClientAddressMutationResolverTest {
  private lateinit var addressService: AddressService
  private lateinit var resolver: ClientAddressMutationResolver
  private lateinit var userPrincipal: UserPrincipal

  private fun createSampleAddress(): AddressModel {
    return AddressModel(
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
  }

  private fun createOtherUserAddress(): AddressModel {
    return AddressModel(
      id = 1L,
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
  }

  @BeforeEach
  fun setUp() {
    addressService = mockk(relaxed = true)
    resolver = ClientAddressMutationResolver(addressService)

    userPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  fun `createAddress should create address successfully`() {
    // Given
    val input = AddressInput(
      receiverName = "张三",
      phone = "13800138000",
      province = "广东省",
      provinceCode = "440000",
      city = "深圳市",
      cityCode = "440300",
      district = "南山区",
      districtCode = "440305",
      detailAddress = "测试地址1号",
      isDefault = false,
    )
    every { addressService.countByUserId(1L) } returns 0
    every { addressService.save(any()) } returns createSampleAddress()

    // When
    val result = resolver.createAddress(input, userPrincipal)

    // Then
    assertNotNull(result)
    verify { addressService.save(any()) }
  }

  @Test
  fun `updateAddress should update address successfully`() {
    // Given
    val input = UpdateAddressInput(detailAddress = "新地址")
    every { addressService.findById(1L) } returns createSampleAddress()
    every { addressService.save(any()) } returns createSampleAddress()

    // When
    val result = resolver.updateAddress(1L, input, userPrincipal)

    // Then
    assertNotNull(result)
    verify { addressService.save(any()) }
  }

  @Test
  fun `deleteAddress should delete address successfully`() {
    // Given
    every { addressService.findById(1L) } returns createSampleAddress()

    // When
    val result = resolver.deleteAddress(1L, userPrincipal)

    // Then
    assertTrue(result)
    verify { addressService.deleteById(1L) }
  }

  @Test
  fun `setDefaultAddress should set default address successfully`() {
    // Given
    val sampleAddress = createSampleAddress()
    sampleAddress.isDefault = false
    every { addressService.findById(1L) } returns sampleAddress

    // When
    val result = resolver.setDefaultAddress(1L, userPrincipal)

    // Then
    assertTrue(result)
    verify { addressService.setDefaultAddress(1L, 1L) }
  }

  @Test
  fun `copyUserAddress should copy address successfully`() {
    // Given
    val newAddress = createSampleAddress()
    newAddress.id = 2L
    every { addressService.findById(1L) } returns createSampleAddress()
    every { addressService.save(any()) } returns newAddress

    // When
    val result = resolver.copyUserAddress(1L, "新地址名称", userPrincipal)

    // Then
    assertNotNull(result)
    verify { addressService.save(any()) }
  }
}