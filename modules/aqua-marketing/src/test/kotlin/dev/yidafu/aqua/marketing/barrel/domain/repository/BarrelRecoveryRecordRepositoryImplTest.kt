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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelRecoveryRecordModel
import dev.yidafu.aqua.common.domain.table.BarrelRecoveryRecordsTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * 空桶回收登记仓储 Exposed 实现测试(Testcontainers 真实 PG):保存/按订单查询/软删过滤。
 */
class BarrelRecoveryRecordRepositoryImplTest :
  FunSpec({
    val repo = BarrelRecoveryRecordRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 回填 id,listByOrderId 过滤软删") {
      val r1 =
        repo.save(
          BarrelRecoveryRecordModel(workerId = 7L, orderId = 10L, specification = "19L", bucketCount = 2),
        )
      val r2 =
        repo.save(
          BarrelRecoveryRecordModel(workerId = 7L, orderId = 10L, specification = "5L", bucketCount = 1),
        )
      val r3 =
        repo.save(
          BarrelRecoveryRecordModel(workerId = 8L, orderId = 11L, specification = "19L", bucketCount = 3),
        )
      r1.id.shouldNotBeNull()
      r2.id.shouldNotBeNull()
      r3.id.shouldNotBeNull()

      val forOrder10 = repo.listByOrderId(10L)
      forOrder10 shouldHaveSize 2
      forOrder10.map { it.specification } shouldBe listOf("19L", "5L")
      forOrder10.first().bucketCount shouldBe 2

      repo.listByOrderId(11L) shouldHaveSize 1
      repo.listByOrderId(999L) shouldHaveSize 0
    }

    test("软删回收记录不进结果") {
      val keep = repo.save(BarrelRecoveryRecordModel(workerId = 7L, orderId = 10L, specification = "19L", bucketCount = 1))
      val removed = repo.save(BarrelRecoveryRecordModel(workerId = 7L, orderId = 10L, specification = "12L", bucketCount = 1))
      repo.listByOrderId(10L) shouldHaveSize 2

      // 直接置软删(仓储无软删入口;验证查询过滤可见性)
      transaction(MarketingPgTestSupport.database) {
        BarrelRecoveryRecordsTable.update({ BarrelRecoveryRecordsTable.id eq removed.id!! }) {
          it[BarrelRecoveryRecordsTable.isDeleted] = true
        }
      }

      val visible = repo.listByOrderId(10L)
      visible shouldHaveSize 1
      visible.first().id shouldBe keep.id
    }
  })
