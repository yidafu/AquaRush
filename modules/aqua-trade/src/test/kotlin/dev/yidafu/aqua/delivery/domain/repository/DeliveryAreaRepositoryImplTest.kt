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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.table.DeliveryAreasTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * 配送区域仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * save 双分支/软删可见性/派生查询/启用过滤。
 */
class DeliveryAreaRepositoryImplTest :
  FunSpec({
    val repo = DeliveryAreaRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id,findById 可查") {
      val saved =
        repo.save(
          dev.yidafu.aqua.common.domain.model.DeliveryAreaModel(
            name = "朝阳区",
            province = "北京市",
            city = "北京市",
            district = "朝阳区",
          ),
        )
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "朝阳区"
    }

    test("save 更新已有实体走 update 分支") {
      val id =
        seedArea(
          name = "旧名称",
          province = "浙江省",
          city = "杭州市",
          district = "西湖区",
        )
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.name = "新名称"
      loaded.enabled = false
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.name shouldBe "新名称"
      reloaded.enabled shouldBe false
    }

    test("软删区域不可见:findById/findAll/findByEnabledTrue/findByProvinceAndCityAndDistrict 均过滤") {
      val keep = seedArea(name = "存活", province = "广东省", city = "深圳市", district = "南山区")
      val removed =
        seedArea(
          name = "已删",
          province = "广东省",
          city = "深圳市",
          district = "福田区",
          deleted = true,
        )

      repo.findById(keep).isPresent shouldBe true
      repo.findById(removed).isPresent shouldBe false
      repo.findByProvinceAndCityAndDistrict("广东省", "深圳市", "福田区").shouldBeNull()
      repo.findByProvinceAndCityAndDistrict("广东省", "深圳市", "南山区").shouldNotBeNull()
      repo.findByEnabledTrue() shouldHaveSize 1
    }

    test("findByEnabledTrue 只返回启用区域") {
      val enabled = seedArea(name = "启用", province = "A省", city = "A市", district = "A区")
      seedArea(name = "停用", province = "B省", city = "B市", district = "B区", enabled = false)

      repo
        .findByEnabledTrue()
        .map { it.id } shouldContainExactly listOf(enabled)
    }

    test("findByProvinceAndCityAndDistrict 精确匹配且返回启用/停用均可见") {
      seedArea(name = "天河", province = "广东省", city = "广州市", district = "天河区", enabled = false)
      repo.findByProvinceAndCityAndDistrict("广东省", "广州市", "天河区").shouldNotBeNull()
      repo.findByProvinceAndCityAndDistrict("广东省", "广州市", "越秀区").shouldBeNull()
    }
  })

/** 直接插行的种子构造(含软删/停用开关)。 */
private fun seedArea(
  name: String,
  province: String,
  city: String,
  district: String,
  enabled: Boolean = true,
  deleted: Boolean = false,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    DeliveryAreasTable.insert {
      it[DeliveryAreasTable.id] = id
      it[DeliveryAreasTable.name] = name
      it[DeliveryAreasTable.province] = province
      it[DeliveryAreasTable.city] = city
      it[DeliveryAreasTable.district] = district
      it[DeliveryAreasTable.isEnabled] = enabled
      it[DeliveryAreasTable.createdAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      it[DeliveryAreasTable.updatedAt] = LocalDateTime.of(2026, 1, 1, 10, 0)
      if (deleted) {
        it[DeliveryAreasTable.isDeleted] = true
        it[DeliveryAreasTable.deletedAt] = LocalDateTime.of(2026, 1, 2, 10, 0)
      }
    }
  }
  return id
}
