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

package dev.yidafu.aqua.ops.storeprofile.domain.repository

import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.ops.common.exposed.OpsPgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 店铺信息仓储实现测试（Testcontainers 真实 PG + Exposed，F-OPS-004 单行配置）。
 * 重点覆盖 `time` 类型列（营业时间）的读写回环。
 */
class StoreProfileRepositoryTest :
  FunSpec({
    val database = OpsPgTestSupport.database
    val repository = StoreProfileRepository(database)

    beforeSpec { OpsPgTestSupport.reset() }

    fun profile(
      shopName: String = "南山送水站",
      start: LocalTime? = LocalTime.of(8, 0),
      end: LocalTime? = LocalTime.of(20, 30),
    ): StoreProfileModel =
      StoreProfileModel(
        id = StoreProfileRepository.SINGLETON_ID,
        shopName = shopName,
        logoUrl = "/static/logo.png",
        serviceHotline = "400-800-8888",
        businessHoursStart = start,
        businessHoursEnd = end,
        shopIntro = "桶装水配送",
        aboutUs = "关于我们",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("save 新建 + findById 回读（含营业时间 time 列回环）") {
      OpsPgTestSupport.reset()
      repository.save(profile())

      val loaded = repository.findById(StoreProfileRepository.SINGLETON_ID).orElseThrow()
      loaded.shopName shouldBe "南山送水站"
      loaded.businessHoursStart shouldBe LocalTime.of(8, 0)
      loaded.businessHoursEnd shouldBe LocalTime.of(20, 30)
      loaded.aboutUs shouldBe "关于我们"
    }

    test("save 覆盖更新：名称与营业时间变更") {
      OpsPgTestSupport.reset()
      repository.save(profile())
      repository.save(profile(shopName = "改名的店", start = LocalTime.of(9, 15), end = LocalTime.of(18, 0)))

      val reloaded = repository.findById(StoreProfileRepository.SINGLETON_ID).orElseThrow()
      reloaded.shopName shouldBe "改名的店"
      reloaded.businessHoursStart shouldBe LocalTime.of(9, 15)
      reloaded.businessHoursEnd shouldBe LocalTime.of(18, 0)
    }

    test("findById 不存在返回 empty，hardDeleteAll 物理删除") {
      OpsPgTestSupport.reset()
      repository.findById(StoreProfileRepository.SINGLETON_ID).isPresent shouldBe false

      repository.save(profile())
      repository.hardDeleteAll()
      repository.findById(StoreProfileRepository.SINGLETON_ID).isPresent shouldBe false
    }
  })
