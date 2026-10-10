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

import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest

/**
 * 退押申请仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * 保存/本人隔离/处理中查询/分页/状态谓词流转幂等。
 */
class DepositRefundApplicationRepositoryImplTest :
  FunSpec({
    val repo = DepositRefundApplicationRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    fun seedApp(
      userId: Long = 100L,
      spec: String = "19L",
      status: String = "PENDING",
    ): DepositRefundApplicationModel =
      repo.save(
        DepositRefundApplicationModel(
          userId = userId,
          specification = spec,
          bucketCount = 1,
          depositCents = 4000,
          receiverName = "张三",
          receiverPhone = "13800000000",
          receiverDetail = "某小区1栋101",
          status = DepositRefundApplicationStatus.fromValue(status),
        ),
      )

    test("save 回填 id,findById/findByIdAndUserId 本人隔离") {
      val app = seedApp(userId = 100L)
      app.id.shouldNotBeNull()

      repo.findById(app.id!!).shouldNotBeNull().specification shouldBe "19L"
      repo.findByIdAndUserId(app.id!!, 100L).shouldNotBeNull()
      repo.findByIdAndUserId(app.id!!, 999L) shouldBe null
    }

    test("findActiveByUserAndSpec 仅命中 PENDING/RECOVERED") {
      seedApp(userId = 100L, spec = "19L", status = "PENDING")
      seedApp(userId = 100L, spec = "5L", status = "RECOVERED")
      seedApp(userId = 100L, spec = "12L", status = "REFUNDED")
      seedApp(userId = 100L, spec = "7L", status = "REJECTED")
      seedApp(userId = 200L, spec = "19L", status = "PENDING")

      repo.findActiveByUserAndSpec(100L, "19L").shouldNotBeNull().status shouldBe DepositRefundApplicationStatus.PENDING
      repo.findActiveByUserAndSpec(100L, "5L").shouldNotBeNull().status shouldBe DepositRefundApplicationStatus.RECOVERED
      repo.findActiveByUserAndSpec(100L, "12L") shouldBe null
      repo.findActiveByUserAndSpec(100L, "7L") shouldBe null
      repo.findActiveByUserAndSpec(100L, "不存在") shouldBe null
      // 不同用户同规格互不影响
      repo.findActiveByUserAndSpec(200L, "5L") shouldBe null
    }

    test("search 按用户与状态分页") {
      seedApp(userId = 100L, spec = "A", status = "PENDING")
      seedApp(userId = 100L, spec = "B", status = "RECOVERED")
      seedApp(userId = 200L, spec = "A", status = "PENDING")

      repo.search(100L, null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.search(null, DepositRefundApplicationStatus.PENDING, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.search(100L, DepositRefundApplicationStatus.PENDING, PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.search(null, null, PageRequest.of(0, 1)).totalElements shouldBe 3L
    }

    test("transition 状态谓词流转幂等:仅 fromStatuses 命中才更新") {
      val app = seedApp(userId = 100L, spec = "19L", status = "PENDING")

      // PENDING → RECOVERED 成功
      repo
        .transition(
          id = app.id!!,
          fromStatuses = listOf(DepositRefundApplicationStatus.PENDING),
          to = DepositRefundApplicationStatus.RECOVERED,
          handlerAdminId = 7L,
          rejectReason = null,
        ) shouldBe true
      val recovered = repo.findById(app.id!!).shouldNotBeNull()
      recovered.status shouldBe DepositRefundApplicationStatus.RECOVERED
      recovered.recoveredAt.shouldNotBeNull()
      recovered.handlerAdminId shouldBe 7L

      // 重复确认(已是 RECOVERED,from=PENDING)→ 0 行 false(幂等防并发双确认)
      repo
        .transition(
          id = app.id!!,
          fromStatuses = listOf(DepositRefundApplicationStatus.PENDING),
          to = DepositRefundApplicationStatus.RECOVERED,
          handlerAdminId = 7L,
          rejectReason = null,
        ) shouldBe false

      // RECOVERED → REFUNDED 落 refundedAt
      repo
        .transition(
          id = app.id!!,
          fromStatuses = listOf(DepositRefundApplicationStatus.RECOVERED),
          to = DepositRefundApplicationStatus.REFUNDED,
          handlerAdminId = 7L,
          rejectReason = null,
        ) shouldBe true
      repo
        .findById(app.id!!)
        .shouldNotBeNull()
        .refundedAt
        .shouldNotBeNull()
    }

    test("transition 驳回落 rejectedAt 与原因") {
      val app = seedApp(userId = 100L, spec = "19L", status = "PENDING")

      repo
        .transition(
          id = app.id!!,
          fromStatuses = listOf(DepositRefundApplicationStatus.PENDING, DepositRefundApplicationStatus.RECOVERED),
          to = DepositRefundApplicationStatus.REJECTED,
          handlerAdminId = 9L,
          rejectReason = "空桶破损",
        ) shouldBe true

      val rejected = repo.findById(app.id!!).shouldNotBeNull()
      rejected.status shouldBe DepositRefundApplicationStatus.REJECTED
      rejected.rejectReason shouldBe "空桶破损"
      rejected.rejectedAt.shouldNotBeNull()
    }

    test("search 分页总数不受页大小影响") {
      repeat(3) { seedApp(userId = 100L, spec = "S$it") }
      repo.search(100L, null, PageRequest.of(0, 2)).let {
        it.content shouldHaveSize 2
        it.totalElements shouldBe 3L
        it.totalPages shouldBe 2
      }
    }
  })
