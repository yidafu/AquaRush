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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class ClientAddressQueryResolverTest :
  FunSpec({
    var addressService: AddressService = mockk()
    var resolver: ClientAddressQueryResolver = mockk()
    var userPrincipal: UserPrincipal = mockk()

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
        isDefault = true,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createOtherUserAddress(): AddressModel =
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

    beforeTest {
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
    test("userAddresses should return all user addresses") {
      // Given
      val addresses = listOf(createSampleAddress())
      every { addressService.findByUserId(1L) } returns addresses

      // When
      val result = resolver.userAddresses(userPrincipal)

      // Then
      result.size shouldBe 1
      verify { addressService.findByUserId(1L) }
    }
    test("address should return address when it belongs to user") {
      // Given
      every { addressService.findById(1L) } returns createSampleAddress()

      // When
      val result = resolver.address(1L, userPrincipal)

      // Then
      result.shouldNotBeNull()
      result?.id shouldBe 1L
    }
    test("address should throw exception when accessing other user address") {
      // Given
      every { addressService.findById(1L) } returns createOtherUserAddress()

      // When & Then
      val exception = shouldThrow<IllegalArgumentException> { resolver.address(1L, userPrincipal) }
      exception.message!!.contains("无权访问") shouldBe true
    }
    test("address should return null when address not found") {
      // Given
      every { addressService.findById(999L) } returns null

      // When
      val result = resolver.address(999L, userPrincipal)

      // Then
      result.shouldBeNull()
    }
  })
