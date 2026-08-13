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

package dev.yidafu.aqua.ops.storeprofile.service.impl

import dev.yidafu.aqua.api.dto.SaveStoreProfileRequest
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.storeprofile.domain.repository.StoreProfileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertEquals

/**
 * 店铺信息变更服务单元测试(F-OPS-004:单行配置 upsert、审计 before/after)
 */
class StoreProfileMutationServiceImplTest {
  private lateinit var storeProfileRepository: StoreProfileRepository
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: StoreProfileMutationServiceImpl

  private fun request(shopName: String = "好喝山泉") =
    SaveStoreProfileRequest(
      shopName = shopName,
      logoUrl = "http://localhost:9090/api/v1/storage/files/logo.png",
      serviceHotline = "400-800-8888",
      businessHoursStart = LocalTime.of(8, 0),
      businessHoursEnd = LocalTime.of(21, 0),
      shopIntro = "天然山泉水",
      aboutUs = "关于我们文案",
    )

  @BeforeEach
  fun setUp() {
    storeProfileRepository = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = StoreProfileMutationServiceImpl(storeProfileRepository, opsConfigAuditService)
  }

  @Test
  @DisplayName("首次保存创建单行配置(id=1)")
  fun `first save creates singleton row`() {
    every { storeProfileRepository.findById(StoreProfileRepository.SINGLETON_ID) } returns java.util.Optional.empty()
    every { storeProfileRepository.save(any()) } answers { firstArg() }

    val saved = service.saveStoreProfile(1L, "admin", request())

    assertEquals(StoreProfileRepository.SINGLETON_ID, saved.id)
    assertEquals("好喝山泉", saved.shopName)
  }

  @Test
  @DisplayName("再次保存覆盖原配置并落审计(含前后值)")
  fun `subsequent save updates in place with audit`() {
    val existing =
      StoreProfileModel(
        id = StoreProfileRepository.SINGLETON_ID,
        shopName = "旧店名",
        serviceHotline = "400-800-8888",
      )
    every { storeProfileRepository.findById(StoreProfileRepository.SINGLETON_ID) } returns java.util.Optional.of(existing)
    every { storeProfileRepository.save(any()) } answers { firstArg() }

    val saved = service.saveStoreProfile(1L, "admin", request(shopName = "新店名"))

    assertEquals("新店名", saved.shopName)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_STORE_PROFILE,
        StoreProfileRepository.SINGLETON_ID,
        OpsConfigAuditService.ACTION_STORE_PROFILE_SAVE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }
}
