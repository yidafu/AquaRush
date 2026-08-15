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
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddressQueryApiServiceImplTest {
  private lateinit var addressRepository: AddressRepository
  private lateinit var service: AddressQueryApiServiceImpl

  private fun address(): AddressModel =
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

  @BeforeEach
  fun setUp() {
    addressRepository = mockk(relaxed = true)
    service = AddressQueryApiServiceImpl(addressRepository)
  }

  @Test
  fun `findById should return address when present`() {
    every { addressRepository.findById(1L) } returns Optional.of(address())
    assertNotNull(service.findById(1L))
  }

  @Test
  fun `findById should return null when absent`() {
    every { addressRepository.findById(9L) } returns Optional.empty()
    assertNull(service.findById(9L))
  }

  @Test
  fun `findByUserId should delegate`() {
    every { addressRepository.findByUserId(1L) } returns listOf(address())
    assertEquals(1, service.findByUserId(1L).size)
  }

  @Test
  fun `findDefaultByUserId should delegate`() {
    every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns address()
    assertEquals(1L, service.findDefaultByUserId(1L)?.id)
  }

  @Test
  fun `findDefaultByUserId should return null when absent`() {
    every { addressRepository.findByUserIdAndIsDefaultTrue(1L) } returns null
    assertNull(service.findDefaultByUserId(1L))
  }

  @Test
  fun `existsById should delegate`() {
    every { addressRepository.existsById(1L) } returns true
    assertTrue(service.existsById(1L))
    every { addressRepository.existsById(2L) } returns false
    assertFalse(service.existsById(2L))
  }
}

class AdminQueryApiServiceImplTest {
  private lateinit var adminRepository: AdminRepository
  private lateinit var service: AdminQueryApiServiceImpl

  private fun admin(): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hash",
      realName = "管理员",
      phone = "13900139000",
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    adminRepository = mockk(relaxed = true)
    service = AdminQueryApiServiceImpl(adminRepository)
  }

  @Test
  fun `findById should delegate`() {
    every { adminRepository.findAdminById(1L) } returns admin()
    assertEquals(1L, service.findById(1L)?.id)
  }

  @Test
  fun `findById should return null when absent`() {
    every { adminRepository.findAdminById(9L) } returns null
    assertNull(service.findById(9L))
  }

  @Test
  fun `findByUsername should delegate`() {
    every { adminRepository.findByUsername("admin") } returns admin()
    assertEquals("admin", service.findByUsername("admin")?.username)
  }

  @Test
  fun `findAll should delegate`() {
    every { adminRepository.findAllAdmins() } returns listOf(admin())
    assertEquals(1, service.findAll().size)
    verify { adminRepository.findAllAdmins() }
  }

  @Test
  fun `existsById should delegate`() {
    every { adminRepository.existsById(1L) } returns true
    assertTrue(service.existsById(1L))
  }
}
