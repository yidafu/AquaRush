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
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Optional

class AddressQueryApiServiceImplTest :
  FunSpec({
    var addressRepository: AddressRepository = mockk()
    var service: AddressQueryApiServiceImpl = mockk()

    fun address(): AddressModel =
      AddressModel(
        id = 1L,
        userId = 1L,
        receiverName = "张三",
        phone = "13800138000",
        province = "广东省",
        city = "深圳市",
        district = "南山区",
        detailAddress = "科技园",
        isDefault = true,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    beforeTest {
      addressRepository = mockk(relaxed = true)
      service = AddressQueryApiServiceImpl(addressRepository)
    }
    test("findById should return address when present") {
      every { addressRepository.findById(1L) } returns Optional.of(address())
      service.findById(1L).shouldNotBeNull()
    }
    test("findById should return null when absent") {
      every { addressRepository.findById(9L) } returns Optional.empty()
      service.findById(9L).shouldBeNull()
    }
    test("findByUserId should delegate") {
      every { addressRepository.findByUserId(1L) } returns listOf(address())
      service.findByUserId(1L).size shouldBe 1
    }
    test("findDefaultByUserId should delegate") {
      every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns address()
      service.findDefaultByUserId(1L)?.id shouldBe 1L
    }
    test("findDefaultByUserId should return null when absent") {
      every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns null
      service.findDefaultByUserId(1L).shouldBeNull()
    }
    test("existsById should delegate") {
      every { addressRepository.existsById(1L) } returns true
      service.existsById(1L) shouldBe true
      every { addressRepository.existsById(2L) } returns false
      service.existsById(2L) shouldBe false
    }
  })
