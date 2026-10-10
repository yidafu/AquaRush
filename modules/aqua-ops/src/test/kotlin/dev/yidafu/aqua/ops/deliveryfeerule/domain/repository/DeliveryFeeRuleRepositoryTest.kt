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

package dev.yidafu.aqua.ops.deliveryfeerule.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.ops.common.exposed.OpsPgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * 配送费规则仓储实现测试（Testcontainers 真实 PG + Exposed，F-OPS-005 单行配置）。
 * 覆盖「固定主键插入/覆盖更新」与 hardDeleteAll 的物理删除语义。
 */
class DeliveryFeeRuleRepositoryTest :
  FunSpec({
    val database = OpsPgTestSupport.database
    val repository = DeliveryFeeRuleRepository(database)

    beforeSpec { OpsPgTestSupport.reset() }

    fun rule(
      id: Long? = DeliveryFeeRuleRepository.SINGLETON_ID,
      freeFloorLimit: Int = 4,
      surchargeCents: Long = 200L,
    ): DeliveryFeeRuleModel =
      DeliveryFeeRuleModel(
        id = id,
        freeFloorLimit = freeFloorLimit,
        surchargeCents = surchargeCents,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("save 新建 + findById 回读") {
      OpsPgTestSupport.reset()
      repository.save(rule(freeFloorLimit = 6, surchargeCents = 500L))

      val loaded = repository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).orElseThrow()
      loaded.freeFloorLimit shouldBe 6
      loaded.surchargeCents shouldBe 500L
    }

    test("save 覆盖更新单行配置") {
      OpsPgTestSupport.reset()
      repository.save(rule())
      repository.save(rule(freeFloorLimit = 3, surchargeCents = 300L))

      val reloaded = repository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).orElseThrow()
      reloaded.freeFloorLimit shouldBe 3
      reloaded.surchargeCents shouldBe 300L
    }

    test("findById 不存在返回 empty") {
      OpsPgTestSupport.reset()
      repository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).isPresent shouldBe false
    }

    test("hardDeleteAll 物理删除（软删行也清掉，复位主键占用）") {
      OpsPgTestSupport.reset()
      repository.save(rule())
      repository.hardDeleteAll()

      repository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).isPresent shouldBe false
      // 复位后可重新插入同主键
      repository.save(rule())
      repository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).isPresent shouldBe true
    }
  })
