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

import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * 桶押金记录仓储 Exposed 实现测试(Testcontainers 真实 PG):saveAll 双分支/软删可见性/派生查询。
 */
class DepositRecordRepositoryImplTest :
  FunSpec({
    val repo = DepositRecordRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("findByOrderIdAndStatus 过滤软删与状态") {
      MarketingPgTestSupport.depositRecord(orderId = 10L, userId = 100L, status = "HELD")
      MarketingPgTestSupport.depositRecord(orderId = 10L, userId = 100L, status = "REFUNDED")
      MarketingPgTestSupport.depositRecord(orderId = 11L, userId = 100L, status = "HELD")
      MarketingPgTestSupport.depositRecord(orderId = 10L, userId = 100L, status = "HELD", deleted = true)

      repo.findByOrderIdAndStatus(10L, DepositStatus.HELD) shouldHaveSize 1
      repo.findByOrderIdAndStatus(10L, DepositStatus.REFUNDED) shouldHaveSize 1
      repo.findByOrderIdAndStatus(99L, DepositStatus.HELD) shouldHaveSize 0
    }

    test("saveAll 新增回填 id,再保存走 update 分支") {
      val records =
        listOf(
          DepositRecordModel(orderId = 10L, userId = 100L, depositAmountCents = 500L),
          DepositRecordModel(orderId = 10L, userId = 100L, depositAmountCents = 600L),
        )
      val saved = repo.saveAll(records)
      saved shouldHaveSize 2
      saved.forEach { it.id.shouldNotBeNull() }

      val found = repo.findByOrderIdAndStatus(10L, DepositStatus.PENDING)
      found shouldHaveSize 2

      saved.forEach { it.status = DepositStatus.REFUNDED }
      repo.saveAll(saved)
      repo.findByOrderIdAndStatus(10L, DepositStatus.PENDING) shouldHaveSize 0
      repo.findByOrderIdAndStatus(10L, DepositStatus.REFUNDED) shouldHaveSize 2
    }

    test("saveAll 空列表 no-op") {
      repo.saveAll(emptyList()) shouldHaveSize 0
      repo.findByOrderIdAndStatus(1L, DepositStatus.PENDING) shouldHaveSize 0
    }
  })
