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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.UserCouponsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 横切问题(见迁移报告):SourceTables.OrdersTable.status 定义为 varchar(20),短于枚举
 * AWAITING_CONFIRMATION(21 字符)—— 生产 changelog 未见加宽记录。
 * 测试库在建表后放宽列宽,以覆盖该状态的写入/查询路径。
 */
private const val RELAX_ORDERS_STATUS_SQL = "ALTER TABLE orders ALTER COLUMN status TYPE varchar(32)"

private var ordersStatusRelaxed = false

private fun seedOrder(
  id: Long = TradePgTestSupport.id(),
  orderNo: String = "ORD-$id",
  userId: Long = 1001L,
  status: OrderModelStatus = OrderModelStatus.PENDING_DISPATCH,
  amountCents: Long = 1000L,
  addressId: Long = 501L,
  deliveryWorkerId: Long? = null,
  paymentType: PaymentType? = null,
  paymentMethod: String? = null,
  clientRequestId: String? = null,
  deliveryTimeSlot: DeliveryTimeSlot? = null,
  priceSource: PriceSource? = null,
  deliveryPhotoReviewStatus: DeliveryPhotoReviewStatus? = null,
  deliveryPhotos: String? = null,
  createdAt: LocalDateTime = LocalDateTime.of(2026, 5, 1, 10, 0),
  updatedAt: LocalDateTime = createdAt,
  deliveryStartedAt: LocalDateTime? = null,
  deliveryConfirmedAt: LocalDateTime? = null,
  completedAt: LocalDateTime? = null,
  promotionActivityId: Long? = null,
  promotionDiscountCents: Long = 0L,
  firstOrderActivityId: Long? = null,
  firstOrderDiscountCents: Long = 0L,
  couponId: Long? = null,
  couponDiscountCents: Long = 0L,
  deleted: Boolean = false,
): Long {
  transaction(TradePgTestSupport.database) {
    // 横切问题绕过(见迁移报告):OrdersTable.status 表定义 varchar(20) 短于
    // AWAITING_CONFIRMATION(21),Exposed 写前校验(与列宽无关的客户端 require)会拒绝;
    // 测试库已 ALTER 放宽列宽,此处先插占位状态、再用裸 SQL 写入目标状态。
    val insertStatus =
      if (status == OrderModelStatus.AWAITING_CONFIRMATION) OrderModelStatus.PENDING_DISPATCH else status
    OrdersTable.insert {
      it[OrdersTable.id] = id
      it[OrdersTable.orderNo] = orderNo
      it[OrdersTable.userId] = userId
      it[OrdersTable.status] = insertStatus.name
      it[OrdersTable.totalAmountCents] = amountCents
      it[OrdersTable.deliveryFeeCents] = 0L
      it[OrdersTable.depositAmountCents] = 0L
      it[OrdersTable.addressId] = addressId
      it[OrdersTable.deliveryWorkerId] = deliveryWorkerId
      it[OrdersTable.paymentType] = paymentType?.name
      it[OrdersTable.paymentMethod] = paymentMethod
      it[OrdersTable.clientRequestId] = clientRequestId
      it[OrdersTable.deliveryTimeSlot] = deliveryTimeSlot?.name
      it[OrdersTable.priceSource] = priceSource?.name
      it[OrdersTable.deliveryPhotoReviewStatus] = deliveryPhotoReviewStatus?.name
      it[OrdersTable.deliveryPhotos] = deliveryPhotos
      it[OrdersTable.promotionActivityId] = promotionActivityId
      it[OrdersTable.promotionDiscountCents] = promotionDiscountCents
      it[OrdersTable.firstOrderActivityId] = firstOrderActivityId
      it[OrdersTable.firstOrderDiscountCents] = firstOrderDiscountCents
      it[OrdersTable.couponId] = couponId
      it[OrdersTable.couponDiscountCents] = couponDiscountCents
      it[OrdersTable.createdAt] = createdAt
      it[OrdersTable.updatedAt] = updatedAt
      it[OrdersTable.deliveryStartedAt] = deliveryStartedAt
      it[OrdersTable.deliveryConfirmedAt] = deliveryConfirmedAt
      it[OrdersTable.completedAt] = completedAt
    }
    if (deleted) {
      OrdersTable.update({ OrdersTable.id eq id }) { it[isDeleted] = true }
    }
    if (status == OrderModelStatus.AWAITING_CONFIRMATION) {
      exec("UPDATE orders SET status = 'AWAITING_CONFIRMATION' WHERE id = $id")
    }
  }
  return id
}

/**
 * 订单仓储 Exposed 实现测试(**Testcontainers 真实 PostgreSQL**):
 * save upsert/软删可见性/派生查询/分页不重不漏/时间窗边界/聚合口径/原子条件更新幂等。
 *
 * 口径说明:覆盖 [OrderRepositoryImpl] 全部公开方法;count 与 content 同事务由用例内的
 * totalElements/content 断言间接覆盖。
 */
class OrderRepositoryImplTest :
  FunSpec({
    val repo = OrderRepositoryImpl(TradePgTestSupport.database)

    beforeTest {
      TradePgTestSupport.reset()
      transaction(TradePgTestSupport.database) {
        // order 子域营销/赠品测试依赖的旁表(不在 TradePgTestSupport 的13 张表内),自行建表+清空
        SchemaUtils.create(UserCouponsTable)
        UserCouponsTable.deleteAll()
        if (!ordersStatusRelaxed) {
          exec(RELAX_ORDERS_STATUS_SQL)
          ordersStatusRelaxed = true
        }
      }
    }

    // ================================================================
    // save / findById / findAll / findAllById
    // ================================================================

    test("save 新增回填雪花 id,枚举/金额/json 列经 findById 完整往返") {
      val now = LocalDateTime.of(2026, 5, 2, 8, 30)
      val saved =
        repo.save(
          dev.yidafu.aqua.common.domain.model.OrderModel(
            orderNo = "RT-${TradePgTestSupport.id()}",
            userId = 42L,
            clientRequestId = "req-42",
            deliveryTimeSlot = DeliveryTimeSlot.MORNING,
            amountCents = 12345L,
            deliveryFeeCents = 500L,
            unitPriceCents = 6000L,
            priceSource = PriceSource.FLASH_SALE_PRICE,
            promotionDiscountCents = 300L,
            couponDiscountCents = 200L,
            ticketRedeemCount = 1,
            ticketDiscountCents = 100L,
            firstOrderDiscountCents = 50L,
            addressId = 7L,
            status = OrderModelStatus.PENDING_PAYMENT,
            paymentMethod = dev.yidafu.aqua.common.domain.model.PaymentMethod.WECHAT_PAY,
            paymentType = PaymentType.QR_CODE,
            paymentTime = now.minusMinutes(5),
            deliveryPhotos = """["https://a/1.jpg"]""",
            deliveryPhotoReviewStatus = DeliveryPhotoReviewStatus.APPROVED,
            depositCents = 3000L,
            isSelfCollect = true,
            remark = "放门口",
            deliveryRemark = "尽快",
            searchContent = "农夫山泉 5L",
            createdAt = now,
            updatedAt = now,
          ),
        )
      saved.id.shouldNotBeNull()

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.orderNo shouldBe saved.orderNo
      loaded.userId shouldBe 42L
      loaded.clientRequestId shouldBe "req-42"
      loaded.deliveryTimeSlot shouldBe DeliveryTimeSlot.MORNING
      loaded.amountCents shouldBe 12345L
      loaded.deliveryFeeCents shouldBe 500L
      loaded.unitPriceCents shouldBe 6000L
      loaded.priceSource shouldBe PriceSource.FLASH_SALE_PRICE
      loaded.promotionDiscountCents shouldBe 300L
      loaded.couponDiscountCents shouldBe 200L
      loaded.ticketRedeemCount shouldBe 1
      loaded.ticketDiscountCents shouldBe 100L
      loaded.firstOrderDiscountCents shouldBe 50L
      loaded.addressId shouldBe 7L
      loaded.status shouldBe OrderModelStatus.PENDING_PAYMENT
      loaded.paymentMethod shouldBe dev.yidafu.aqua.common.domain.model.PaymentMethod.WECHAT_PAY
      loaded.paymentType shouldBe PaymentType.QR_CODE
      loaded.paymentTime shouldBe now.minusMinutes(5)
      loaded.deliveryPhotos shouldBe """["https://a/1.jpg"]"""
      loaded.deliveryPhotoReviewStatus shouldBe DeliveryPhotoReviewStatus.APPROVED
      loaded.depositCents shouldBe 3000L
      loaded.isSelfCollect shouldBe true
      loaded.remark shouldBe "放门口"
      loaded.deliveryRemark shouldBe "尽快"
      loaded.searchContent shouldBe "农夫山泉 5L"
      loaded.createdAt shouldBe now
      loaded.deletedAt.shouldBeNull()
      loaded.user.shouldBeNull()
      loaded.address.shouldBeNull()
      loaded.deliveryWorker.shouldBeNull()
    }

    test("save 更新已有行:业务列覆盖、createdAt 不可变、updatedAt 刷新") {
      val id = seedOrder(status = OrderModelStatus.PENDING_DISPATCH, amountCents = 1000L)
      val before = repo.findById(id).orElse(null).shouldNotBeNull()
      val oldUpdatedAt = before.updatedAt

      before.status = OrderModelStatus.DELIVERING
      before.amountCents = 9999L
      before.deliveryWorkerId = 88L
      repo.save(before)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.status shouldBe OrderModelStatus.DELIVERING
      reloaded.amountCents shouldBe 9999L
      reloaded.deliveryWorkerId shouldBe 88L
      reloaded.createdAt shouldBe before.createdAt
      (reloaded.updatedAt >= oldUpdatedAt) shouldBe true
    }

    test("findByOrderNo 命中/缺失,软删不可见") {
      seedOrder(orderNo = "FIND-1", deleted = true)
      repo.findByOrderNo("FIND-1").shouldBeNull()
      val live = seedOrder(orderNo = "FIND-2")
      repo.findByOrderNo("FIND-2").shouldNotBeNull().id shouldBe live
      repo.findByOrderNo("NOPE").shouldBeNull()
    }

    test("findByUserIdAndClientRequestId 服务端幂等键命中") {
      val id = seedOrder(userId = 9L, clientRequestId = "idem-1")
      repo.findByUserIdAndClientRequestId(9L, "idem-1").shouldNotBeNull().id shouldBe id
      repo.findByUserIdAndClientRequestId(9L, "other").shouldBeNull()
      // 其他用户的同名幂等键不命中
      repo.findByUserIdAndClientRequestId(10L, "idem-1").shouldBeNull()
    }

    test("findByUserId/findByUserIdAndStatus:id 升序 + 软删过滤") {
      val u = 500L
      val a = seedOrder(userId = u, status = OrderModelStatus.COMPLETED, createdAt = LocalDateTime.of(2026, 4, 1, 0, 0))
      val b = seedOrder(userId = u, status = OrderModelStatus.PENDING_DISPATCH, createdAt = LocalDateTime.of(2026, 4, 2, 0, 0))
      seedOrder(userId = u, status = OrderModelStatus.COMPLETED, deleted = true)
      seedOrder(userId = u + 1, status = OrderModelStatus.COMPLETED)

      repo.findByUserId(u).map { it.id } shouldContainExactly listOf(a, b)
      repo.findByUserIdAndStatus(u, OrderModelStatus.COMPLETED).map { it.id } shouldContainExactly listOf(a)
    }

    test("existsByUserIdAndStatus:存在/不存在/软删均 false") {
      seedOrder(userId = 600L, status = OrderModelStatus.CANCELLED)
      seedOrder(userId = 601L, status = OrderModelStatus.CANCELLED, deleted = true)

      repo.existsByUserIdAndStatus(600L, OrderModelStatus.CANCELLED) shouldBe true
      repo.existsByUserIdAndStatus(600L, OrderModelStatus.COMPLETED) shouldBe false
      repo.existsByUserIdAndStatus(601L, OrderModelStatus.CANCELLED) shouldBe false
      repo.existsByUserIdAndStatus(602L, OrderModelStatus.CANCELLED) shouldBe false
    }

    test("findByStatus + findByStatusOrderByCreatedAtAsc 排序与软删过滤") {
      val t1 = LocalDateTime.of(2026, 3, 1, 0, 0)
      val t2 = LocalDateTime.of(2026, 3, 2, 0, 0)
      val late = seedOrder(status = OrderModelStatus.DELIVERING, createdAt = t2)
      val early = seedOrder(status = OrderModelStatus.DELIVERING, createdAt = t1)
      seedOrder(status = OrderModelStatus.DELIVERING, createdAt = LocalDateTime.of(2026, 3, 3, 0, 0), deleted = true)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = t1)

      repo.findByStatus(OrderModelStatus.DELIVERING).map { it.id } shouldContainExactly listOf(late, early)
      repo.findByStatusOrderByCreatedAtAsc(OrderModelStatus.DELIVERING).map { it.id } shouldContainExactly
        listOf(
          early,
          late,
        )
    }

    test("findByStatusAndDeliveryStartedAtBefore:严格早于截止 + 分页游标不重不漏") {
      val before = LocalDateTime.of(2026, 5, 10, 12, 0)
      val o1 = seedOrder(status = OrderModelStatus.DELIVERING, deliveryStartedAt = before.minusHours(3))
      val o2 = seedOrder(status = OrderModelStatus.DELIVERING, deliveryStartedAt = before.minusHours(2))
      val o3 = seedOrder(status = OrderModelStatus.DELIVERING, deliveryStartedAt = before.minusHours(1))
      seedOrder(status = OrderModelStatus.DELIVERING, deliveryStartedAt = before) // 恰等边界,不含
      seedOrder(status = OrderModelStatus.DELIVERING) // 未开始配送
      seedOrder(status = OrderModelStatus.DELIVERING, deliveryStartedAt = before.minusHours(4), deleted = true)

      val page0 =
        repo.findByStatusAndDeliveryStartedAtBefore(
          OrderModelStatus.DELIVERING,
          before,
          PageRequest.of(0, 2),
        )
      page0.totalElements shouldBe 3L
      page0.content shouldHaveSize 2
      val page1 =
        repo.findByStatusAndDeliveryStartedAtBefore(
          OrderModelStatus.DELIVERING,
          before,
          PageRequest.of(1, 2),
        )
      page1.content shouldHaveSize 1
      // 无 sort → 默认 id 升序破平,跨页不重不漏
      (page0.content + page1.content).map { it.id } shouldContainExactly listOf(o1, o2, o3)
    }

    test("findByStatusAndDeliveryConfirmedAtBefore:严格早于截止 + 传入 Sort(id ASC)") {
      val deadline = LocalDateTime.of(2026, 5, 20, 0, 0)
      val o1 =
        seedOrder(
          status = OrderModelStatus.AWAITING_CONFIRMATION,
          deliveryConfirmedAt = deadline.minusDays(4),
        )
      val o2 =
        seedOrder(
          status = OrderModelStatus.AWAITING_CONFIRMATION,
          deliveryConfirmedAt = deadline.minusDays(1),
        )
      seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION, deliveryConfirmedAt = deadline) // 边界不含
      seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION) // 未送达
      seedOrder(status = OrderModelStatus.COMPLETED, deliveryConfirmedAt = deadline.minusDays(2)) // 状态不符

      val page =
        repo.findByStatusAndDeliveryConfirmedAtBefore(
          OrderModelStatus.AWAITING_CONFIRMATION,
          deadline,
          PageRequest.of(
            0,
            10,
            org.springframework.data.domain.Sort
              .by("id"),
          ),
        )
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(o1, o2)
    }

    test("findByDeliveryWorkerIdOrderByCreatedAtDesc:倒序 + id 破平") {
      val w = 300L
      val t1 = LocalDateTime.of(2026, 5, 1, 0, 0)
      val t2 = LocalDateTime.of(2026, 5, 2, 0, 0)
      val first = seedOrder(deliveryWorkerId = w, createdAt = t1)
      val second = seedOrder(deliveryWorkerId = w, createdAt = t2)
      seedOrder(deliveryWorkerId = w + 1, createdAt = t2)

      repo.findByDeliveryWorkerIdOrderByCreatedAtDesc(w).map { it.id } shouldContainExactly listOf(second, first)
    }

    test("findByAddressId 软删过滤") {
      val a = seedOrder(addressId = 77L)
      seedOrder(addressId = 77L, deleted = true)
      seedOrder(addressId = 78L)

      repo.findByAddressId(77L).map { it.id } shouldContainExactly listOf(a)
      repo.findByAddressId(79L) shouldHaveSize 0
    }

    test("countByStatus 对软删行不计数") {
      seedOrder(status = OrderModelStatus.REFUNDED)
      seedOrder(status = OrderModelStatus.REFUNDED, deleted = true)
      seedOrder(status = OrderModelStatus.CANCELLED)

      repo.countByStatus(OrderModelStatus.REFUNDED) shouldBe 1L
      repo.countByStatus(OrderModelStatus.CANCELLED) shouldBe 1L
      repo.countByStatus(OrderModelStatus.COMPLETED) shouldBe 0L
    }

    test("findAllById:软删过滤 + 命中子集 + 空列表") {
      val a = seedOrder(userId = 1L)
      seedOrder(userId = 2L, deleted = true)
      val c = seedOrder(userId = 3L)

      repo.findAllById(listOf(c, a)).map { it.id } shouldContainExactly listOf(a, c)
      repo.findAllById(emptyList()) shouldHaveSize 0
    }

    // ================================================================
    // 分页(findOrdersPaginated / findByDeliveryWorkerIdAndStatusIn / findUserOrdersPaginated)
    // ================================================================

    test("findOrdersPaginated 组合过滤:关键词/状态/用户/配送员/时间窗/金额区间") {
      val day = LocalDateTime.of(2026, 6, 1, 0, 0)
      val hit =
        seedOrder(
          userId = 10L,
          deliveryWorkerId = 20L,
          status = OrderModelStatus.COMPLETED,
          amountCents = 5000L,
          createdAt = day.plusHours(1),
        )
      seedOrder(orderNo = "KEY-MATCH", createdAt = day.plusHours(1)) // 关键词命中(大小写不敏感)
      seedOrder(userId = 10L, createdAt = day.plusHours(2), amountCents = 4999L) // 金额下界不含
      seedOrder(userId = 10L, createdAt = day.plusHours(3), amountCents = 10001L) // 金额上界不含
      seedOrder(userId = 10L, createdAt = day.minusSeconds(1)) // 时间窗起点外
      seedOrder(userId = 10L, createdAt = day.plusDays(1)) // 时间窗终点外
      seedOrder(userId = 10L, status = OrderModelStatus.CANCELLED, createdAt = day.plusHours(4)) // 状态不符

      val byKeyword =
        repo.findOrdersPaginated(keyword = "key-match", page = 0, size = 10)
      byKeyword.totalElements shouldBe 1L
      byKeyword.content.single().orderNo shouldBe "KEY-MATCH"

      val filtered =
        repo.findOrdersPaginated(
          userId = 10L,
          deliveryWorkerId = 20L,
          status = OrderModelStatus.COMPLETED,
          startDate = day,
          endDate = day.plusDays(1),
          minAmount = 5000L,
          maxAmount = 10000L,
          page = 0,
          size = 10,
        )
      filtered.content.map { it.id } shouldContainExactly listOf(hit)
      filtered.totalElements shouldBe 1L
    }

    test("findOrdersPaginated 排序白名单与方向 + 分页不重不漏") {
      val base = LocalDateTime.of(2026, 6, 2, 0, 0)
      val cheap = seedOrder(userId = 77L, amountCents = 100L, createdAt = base.plusHours(3))
      val mid = seedOrder(userId = 77L, amountCents = 200L, createdAt = base.plusHours(1))
      val rich = seedOrder(userId = 77L, amountCents = 300L, createdAt = base.plusHours(2))

      // totalamount asc:金额升序(未知字段回退 createdAt —— 不会抛错)
      repo
        .findOrdersPaginated(userId = 77L, page = 0, size = 10, sortField = "total_amount", sortDirection = "asc")
        .content
        .map { it.id } shouldContainExactly listOf(cheap, mid, rich)
      // updatedat desc
      repo
        .findOrdersPaginated(userId = 77L, page = 0, size = 10, sortField = "updatedAt", sortDirection = "desc")
        .content shouldHaveSize 3
      // 未知 sortField 回退 createdAt desc(默认方向 desc)
      repo
        .findOrdersPaginated(userId = 77L, page = 0, size = 10, sortField = "unknown", sortDirection = "desc")
        .content
        .map { it.id } shouldContainExactly listOf(cheap, rich, mid)

      // 分页不重不漏(size=2 + id 破平)
      val p0 = repo.findOrdersPaginated(userId = 77L, page = 0, size = 2, sortField = "createdAt", sortDirection = "desc")
      val p1 = repo.findOrdersPaginated(userId = 77L, page = 1, size = 2, sortField = "createdAt", sortDirection = "desc")
      p0.totalElements shouldBe 3L
      (p0.content + p1.content).map { it.id } shouldContainExactly listOf(cheap, rich, mid)
      // 软删不计入
      seedOrder(userId = 77L, amountCents = 400L, deleted = true)
      repo.findOrdersPaginated(userId = 77L, page = 0, size = 10).totalElements shouldBe 3L
    }

    test("findByDeliveryWorkerIdAndStatusIn:状态集合 + 配送员过滤 + 分页 count") {
      val w = 400L
      val t1 = LocalDateTime.of(2026, 6, 5, 0, 0)
      val t2 = LocalDateTime.of(2026, 6, 6, 0, 0)
      val d1 = seedOrder(deliveryWorkerId = w, status = OrderModelStatus.COMPLETED, createdAt = t2)
      val d2 = seedOrder(deliveryWorkerId = w, status = OrderModelStatus.CANCELLED, createdAt = t1)
      seedOrder(deliveryWorkerId = w, status = OrderModelStatus.DELIVERING, createdAt = t1) // 状态集合外
      seedOrder(deliveryWorkerId = w + 1, status = OrderModelStatus.COMPLETED, createdAt = t1) // 配送员外
      seedOrder(deliveryWorkerId = w, status = OrderModelStatus.COMPLETED, createdAt = t1, deleted = true)

      val page =
        repo.findByDeliveryWorkerIdAndStatusIn(
          deliveryWorkerId = w,
          statuses = listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
          keyword = "ignored(原实现死代码)",
          page = 0,
          size = 10,
        )
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(d1, d2) // createdAt desc

      // deliveryWorkerId = null → 不限配送员
      repo
        .findByDeliveryWorkerIdAndStatusIn(
          deliveryWorkerId = null,
          statuses = listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
          page = 0,
          size = 10,
        ).totalElements shouldBe 3L

      // 空 statuses → 不匹配任何行
      repo
        .findByDeliveryWorkerIdAndStatusIn(deliveryWorkerId = w, statuses = emptyList(), page = 0, size = 10)
        .totalElements shouldBe 0L
    }

    test("findUserOrdersPaginated:statuses 空=全部/非空过滤 + 倒序分页不重不漏") {
      val u = 88L
      val t = LocalDateTime.of(2026, 6, 8, 0, 0)
      val o1 = seedOrder(userId = u, status = OrderModelStatus.COMPLETED, createdAt = t)
      val o2 = seedOrder(userId = u, status = OrderModelStatus.DELIVERING, createdAt = t.plusHours(1))
      val o3 = seedOrder(userId = u, status = OrderModelStatus.PENDING_PAYMENT, createdAt = t.plusHours(2))
      seedOrder(userId = u + 1, status = OrderModelStatus.COMPLETED, createdAt = t.plusHours(3)) // 越权不可见
      seedOrder(userId = u, status = OrderModelStatus.COMPLETED, createdAt = t.plusHours(4), deleted = true)

      // statuses null → 全部
      val all = repo.findUserOrdersPaginated(u, null, 0, 10)
      all.totalElements shouldBe 3L
      all.content.map { it.id } shouldContainExactly listOf(o3, o2, o1) // createdAt desc
      // statuses 空列表 → 也是全部(与原 takeIf 一致)
      repo.findUserOrdersPaginated(u, emptyList(), 0, 10).totalElements shouldBe 3L
      // 非空过滤
      val completed = repo.findUserOrdersPaginated(u, listOf(OrderModelStatus.COMPLETED), 0, 10)
      completed.content.map { it.id } shouldContainExactly listOf(o1)
      // 分页不重不漏
      val p0 = repo.findUserOrdersPaginated(u, null, 0, 2)
      val p1 = repo.findUserOrdersPaginated(u, null, 1, 2)
      (p0.content + p1.content).map { it.id } shouldContainExactly listOf(o3, o2, o1)
    }

    // ================================================================
    // 日期窗口统计(半开 [start,end) 与严格开 (start,end) 两种口径)
    // ================================================================

    test("countOrdersByDateRange [start,end) 边界 + 配送员/状态过滤 + 空 statuses") {
      val start = LocalDateTime.of(2026, 7, 1, 0, 0)
      val end = LocalDateTime.of(2026, 7, 2, 0, 0)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start) // 含(闭下界)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = end.minusSeconds(1)) // 含
      seedOrder(status = OrderModelStatus.CANCELLED, createdAt = start.plusHours(2)) // 状态过滤用
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = end) // 不含(开上界)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.minusSeconds(1)) // 不含
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.plusHours(1), deliveryWorkerId = 9L)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.plusHours(3), deleted = true) // 软删不计

      repo.countOrdersByDateRange(start, end, null, null) shouldBe 4L
      repo.countOrdersByDateRange(start, end, 9L, null) shouldBe 1L
      repo.countOrdersByDateRange(start, end, null, listOf(OrderModelStatus.COMPLETED)) shouldBe 3L
      repo.countOrdersByDateRange(start, end, null, listOf(OrderModelStatus.CANCELLED)) shouldBe 1L
      // 空 statuses 列表 → 0(原 in 空集合语义)
      repo.countOrdersByDateRange(start, end, null, emptyList()) shouldBe 0L
    }

    test("sumAmountCentsByStatusAndDateRange 半开窗口 + 状态/配送员") {
      val start = LocalDateTime.of(2026, 7, 5, 0, 0)
      val end = LocalDateTime.of(2026, 7, 6, 0, 0)
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 100L, createdAt = start)
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 200L, createdAt = end.minusSeconds(1), deliveryWorkerId = 6L)
      seedOrder(status = OrderModelStatus.CANCELLED, amountCents = 400L, createdAt = start.plusHours(1))
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 800L, createdAt = end) // 窗外

      repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, null) shouldBe 300L
      repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 6L) shouldBe 200L
      repo.sumAmountCentsByStatusAndDateRange(
        listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
        start,
        end,
        null,
      ) shouldBe 700L
    }

    // ================================================================
    // 「已送达」口径(COALESCE(delivery_confirmed_at, completed_at))
    // ================================================================

    test("countDeliveredByDateRange:含待确认/已完成 + completedAt 回退 + 配送员过滤") {
      val start = LocalDateTime.of(2026, 8, 1, 0, 0)
      val end = LocalDateTime.of(2026, 8, 2, 0, 0)
      // 待确认收货,按送达时间锚定
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        deliveryConfirmedAt = start.plusHours(1),
        completedAt = null,
        deliveryWorkerId = 11L,
      )
      // 已完成,deliveryConfirmedAt 有值 → 按送达时间
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        deliveryConfirmedAt = start.plusHours(2),
        completedAt = end.plusDays(5),
        deliveryWorkerId = 11L,
      )
      // 历史数据 deliveryConfirmedAt 为 null → 回退 completedAt
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        deliveryConfirmedAt = null,
        completedAt = start.plusHours(3),
        deliveryWorkerId = 12L,
      )
      seedOrder(status = OrderModelStatus.DELIVERING, deliveryConfirmedAt = start.plusHours(4)) // 状态不符
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        deliveryConfirmedAt = end, // 开上界不含
        completedAt = null,
      )
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        deliveryConfirmedAt = start.minusSeconds(1), // 闭下界外
        completedAt = null,
      )
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        deliveryConfirmedAt = start.plusHours(5),
        completedAt = null,
        deleted = true,
      )
      // 送达与完成时间都为 null → 不进任何窗口
      seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION, deliveryConfirmedAt = null, completedAt = null)

      repo.countDeliveredByDateRange(start, end, null) shouldBe 3L
      repo.countDeliveredByDateRange(start, end, 11L) shouldBe 2L
      repo.countDeliveredByDateRange(start, end, 12L) shouldBe 1L
      repo.countDeliveredByDateRange(start, end, 13L) shouldBe 0L
    }

    test("sumDeliveredAmountCentsByDateRange 金额口径与计数同窗口") {
      val start = LocalDateTime.of(2026, 8, 3, 0, 0)
      val end = LocalDateTime.of(2026, 8, 4, 0, 0)
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        amountCents = 111L,
        deliveryConfirmedAt = start.plusMinutes(30),
        completedAt = null,
      )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        amountCents = 222L,
        deliveryConfirmedAt = null,
        completedAt = start.plusMinutes(40),
      )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        amountCents = 444L,
        deliveryConfirmedAt = end, // 窗外
        completedAt = null,
      )

      repo.sumDeliveredAmountCentsByDateRange(start, end, null) shouldBe 333L
      repo.sumDeliveredAmountCentsByDateRange(start, end, 99L) shouldBe 0L
    }

    test("countAndSumDeliveredByPaymentType:[订单数, 金额] 按支付类型统计") {
      val start = LocalDateTime.of(2026, 8, 5, 0, 0)
      val end = LocalDateTime.of(2026, 8, 6, 0, 0)
      val w = 21L
      seedOrder(
        status = OrderModelStatus.AWAITING_CONFIRMATION,
        paymentType = PaymentType.CASH,
        amountCents = 1000L,
        deliveryConfirmedAt = start.plusHours(1),
        deliveryWorkerId = w,
      )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        paymentType = PaymentType.CASH,
        amountCents = 2000L,
        deliveryConfirmedAt = null,
        completedAt = start.plusHours(2),
        deliveryWorkerId = w,
      )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        paymentType = PaymentType.QR_CODE,
        amountCents = 5000L,
        deliveryConfirmedAt = start.plusHours(3),
        deliveryWorkerId = w,
      )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        paymentType = PaymentType.CASH,
        amountCents = 7000L,
        deliveryConfirmedAt = start.plusHours(4),
        deliveryWorkerId = w + 1, // 其他配送员
      )

      val cash = repo.countAndSumDeliveredByPaymentType(w, PaymentType.CASH, start, end)
      cash[0] shouldBe 2L
      cash[1] shouldBe 3000L
      val qr = repo.countAndSumDeliveredByPaymentType(w, PaymentType.QR_CODE, start, end)
      qr[0] shouldBe 1L
      qr[1] shouldBe 5000L
      val none = repo.countAndSumDeliveredByPaymentType(w + 5, PaymentType.CASH, start, end)
      none[0] shouldBe 0L
      none[1] shouldBe 0L
    }

    test("findDeliveredOrdersByDateRange:送达时间升序/id 降序分页 + count 同口径") {
      val start = LocalDateTime.of(2026, 8, 7, 0, 0)
      val end = LocalDateTime.of(2026, 8, 8, 0, 0)
      val t1 = start.plusHours(1)
      val t2 = start.plusHours(2)
      val t3 = start.plusHours(3)
      val late = seedOrder(status = OrderModelStatus.COMPLETED, deliveryConfirmedAt = t3, completedAt = null)
      val early = seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION, deliveryConfirmedAt = t1, completedAt = null)
      val mid = seedOrder(status = OrderModelStatus.COMPLETED, deliveryConfirmedAt = t2, completedAt = null)
      seedOrder(status = OrderModelStatus.COMPLETED, deliveryConfirmedAt = end, completedAt = null) // 窗外

      val p0 = repo.findDeliveredOrdersByDateRange(null, start, end, PageRequest.of(0, 2))
      p0.totalElements shouldBe 3L
      p0.content.map { it.id } shouldContainExactly listOf(early, mid)
      val p1 = repo.findDeliveredOrdersByDateRange(null, start, end, PageRequest.of(1, 2))
      p1.content.map { it.id } shouldContainExactly listOf(late)
      // 按配送员过滤
      repo.findDeliveredOrdersByDateRange(999L, start, end, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("countOrdersByUserIdGroupByStatus:分组计数,缺失状态不计,软删不计") {
      val u = 55L
      seedOrder(userId = u, status = OrderModelStatus.COMPLETED)
      seedOrder(userId = u, status = OrderModelStatus.COMPLETED)
      seedOrder(userId = u, status = OrderModelStatus.CANCELLED)
      seedOrder(userId = u, status = OrderModelStatus.CANCELLED, deleted = true)
      seedOrder(userId = u + 1, status = OrderModelStatus.COMPLETED)

      val grouped = repo.countOrdersByUserIdGroupByStatus(u)
      grouped shouldBe
        mapOf(
          OrderModelStatus.COMPLETED to 2L,
          OrderModelStatus.CANCELLED to 1L,
        )
      grouped.containsKey(OrderModelStatus.REFUNDED) shouldBe false
      repo.countOrdersByUserIdGroupByStatus(66L) shouldBe emptyMap()
    }

    // ================================================================
    // orderStatistics 严格开区间 (start, end) 口径
    // ================================================================

    test("countByStatusAndCreatedAtBetween 严格开区间边界 + 配送员/状态/null/空 statuses") {
      val start = LocalDateTime.of(2026, 9, 1, 0, 0)
      val end = LocalDateTime.of(2026, 9, 2, 0, 0)
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start) // 恰在起点,排除
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.plusSeconds(1)) // 含
      seedOrder(
        status = OrderModelStatus.PENDING_DISPATCH,
        createdAt = end.minusSeconds(1),
        deliveryWorkerId = 5L,
      )
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = end) // 恰在终点,排除
      seedOrder(status = OrderModelStatus.CANCELLED, createdAt = start.plusHours(2), deleted = true)

      repo.countByStatusAndCreatedAtBetween(start, end, null, null) shouldBe 2L
      repo.countByStatusAndCreatedAtBetween(start, end, 5L, null) shouldBe 1L
      repo.countByStatusAndCreatedAtBetween(start, end, 6L, null) shouldBe 0L
      repo.countByStatusAndCreatedAtBetween(start, end, null, listOf(OrderModelStatus.COMPLETED)) shouldBe 1L
      repo.countByStatusAndCreatedAtBetween(start, end, null, emptyList()) shouldBe 0L
    }

    test("sumAmountCentsByStatusAndCreatedAtBetween 严格开区间金额合计") {
      val start = LocalDateTime.of(2026, 9, 3, 0, 0)
      val end = LocalDateTime.of(2026, 9, 4, 0, 0)
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 1111L, createdAt = start) // 排除
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 2222L, createdAt = start.plusSeconds(1))
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 3333L, createdAt = end.minusSeconds(1))
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 4444L, createdAt = end) // 排除
      seedOrder(status = OrderModelStatus.CANCELLED, amountCents = 5555L, createdAt = start.plusHours(1))

      repo.sumAmountCentsByStatusAndCreatedAtBetween(
        listOf(OrderModelStatus.COMPLETED),
        start,
        end,
        null,
      ) shouldBe (2222L + 3333L)
      repo.sumAmountCentsByStatusAndCompletedAtBetween(
        listOf(OrderModelStatus.CANCELLED),
        start,
        end,
        null,
      ) shouldBe 0L
    }

    test("countByStatusAndCompletedAtBetween 按完成时间归属(跨日完成计入完成日)") {
      val start = LocalDateTime.of(2026, 9, 5, 0, 0)
      val end = LocalDateTime.of(2026, 9, 6, 0, 0)
      // 下单日早于窗口、完成日在窗口 → 计入完成日
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        createdAt = start.minusDays(3),
        completedAt = start.plusHours(1),
      )
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start, completedAt = null) // 无完成时间
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start, completedAt = end) // 恰等终点,排除
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start, completedAt = start) // 恰等起点,排除
      seedOrder(
        status = OrderModelStatus.CANCELLED,
        createdAt = start,
        completedAt = start.plusHours(2), // 状态过滤
      )
      // statuses=null 全状态:窗口内 1 单 COMPLETED(回退口径)+ 1 单 CANCELLED = 2
      repo.countByStatusAndCompletedAtBetween(start, end, null, null) shouldBe 2L
      repo.countByStatusAndCompletedAtBetween(start, end, null, listOf(OrderModelStatus.COMPLETED)) shouldBe 1L
      repo.countByStatusAndCompletedAtBetween(start, end, null, listOf(OrderModelStatus.CANCELLED)) shouldBe 1L
    }

    test("sumAmountCentsByStatusAndCompletedAtBetween 按完成时间归属金额合计") {
      val start = LocalDateTime.of(2026, 9, 7, 0, 0)
      val end = LocalDateTime.of(2026, 9, 8, 0, 0)
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 100L, createdAt = start.minusDays(1), completedAt = start.plusHours(1))
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 200L, createdAt = start, completedAt = null)
      seedOrder(status = OrderModelStatus.COMPLETED, amountCents = 400L, createdAt = start, completedAt = end) // 排除

      repo.sumAmountCentsByStatusAndCompletedAtBetween(
        listOf(OrderModelStatus.COMPLETED),
        start,
        end,
        null,
      ) shouldBe 100L
    }

    test("findByCreatedAtBetween/findByStatusAndCreatedAtBetween:严格开区间 + 软删过滤 + id 升序") {
      val start = LocalDateTime.of(2026, 9, 9, 0, 0)
      val end = LocalDateTime.of(2026, 9, 10, 0, 0)
      val a = seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.plusSeconds(1))
      val b = seedOrder(status = OrderModelStatus.CANCELLED, createdAt = end.minusSeconds(1))
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start) // 起点排除
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = end) // 终点排除
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = start.plusHours(1), deleted = true)

      repo.findByCreatedAtBetween(start, end).map { it.id } shouldContainExactly listOf(a, b)
      repo.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end).map { it.id } shouldContainExactly
        listOf(a)
      repo.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.CANCELLED), start, end).map { it.id } shouldContainExactly
        listOf(b)
    }

    // ================================================================
    // 营销使用情况(相关订单/使用汇总/商品聚合行)
    // ================================================================

    test("findOrdersByMarketingUsage PROMOTION_ACTIVITY:倒序分页 + count 同口径 + 软删过滤") {
      val base = LocalDateTime.of(2026, 10, 1, 0, 0)
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, refId = 301L)
      val o1 = seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 301L, promotionDiscountCents = 500L, createdAt = base)
      val o2 =
        seedOrder(
          status = OrderModelStatus.COMPLETED,
          promotionActivityId = 301L,
          promotionDiscountCents = 700L,
          createdAt = base.plusHours(1),
        )
      seedOrder(
        status = OrderModelStatus.COMPLETED,
        promotionActivityId = 302L,
        promotionDiscountCents = 9999L,
        createdAt = base.plusHours(2),
      )
      seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 301L, createdAt = base.plusHours(3), deleted = true)

      val page = repo.findOrdersByMarketingUsage(criteria, 0, 1)
      page.totalElements shouldBe 2L
      page.content.map { it.id } shouldContainExactly listOf(o2)
      val page1 = repo.findOrdersByMarketingUsage(criteria, 1, 1)
      page1.content.map { it.id } shouldContainExactly listOf(o1)
      repo.countOrdersByMarketingUsage(criteria) shouldBe 2L
      repo.countOrdersByMarketingUsage(
        MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, refId = 302L),
      ) shouldBe 1L
    }

    test("营销追溯 COUPON_TEMPLATE/GIFT_ACTIVITY 子查询口径") {
      // 券模板:user_coupons 子查询(含已退回/已使用券的订单 —— 订单快照保留 coupon_id)
      val couponId = seedUserCoupon(templateId = 501L)
      val viaCoupon = seedOrder(status = OrderModelStatus.COMPLETED, couponId = couponId, couponDiscountCents = 600L)
      seedOrder(status = OrderModelStatus.COMPLETED, couponId = 999999L, couponDiscountCents = 800L) // 无对应券
      val couponCriteria = MarketingUsageOrderCriteria(MarketingUsageKind.COUPON_TEMPLATE, refId = 501L)
      repo.countOrdersByMarketingUsage(couponCriteria) shouldBe 1L
      repo.findOrdersByMarketingUsage(couponCriteria, 0, 10).content.map { it.id } shouldContainExactly
        listOf(viaCoupon)
      repo.sumMarketingDiscountCents(couponCriteria) shouldBe 600L

      // 赠品活动:order_gifts 子查询(裸表无软删)
      val giftOrder = seedOrder(status = OrderModelStatus.COMPLETED)
      seedOrderGift(orderId = giftOrder, giftActivityId = 601L, productId = 11L, quantity = 2)
      seedOrderGift(orderId = giftOrder, giftActivityId = 601L, productId = 12L, quantity = 1)
      val otherGiftOrder = seedOrder(status = OrderModelStatus.COMPLETED)
      seedOrderGift(orderId = otherGiftOrder, giftActivityId = 602L, productId = 11L, quantity = 9)

      val giftCriteria = MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, refId = 601L)
      repo.countOrdersByMarketingUsage(giftCriteria) shouldBe 1L
      repo.findOrdersByMarketingUsage(giftCriteria, 0, 10).content.map { it.id } shouldContainExactly
        listOf(giftOrder)
    }

    test("sumMarketingDiscountCents:满减/首单按活动 id 求和,券模板同追溯口径,其余类型返回 0") {
      seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 701L, promotionDiscountCents = 300L)
      seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 701L, promotionDiscountCents = 400L)
      seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 702L, promotionDiscountCents = 5000L)
      seedOrder(status = OrderModelStatus.COMPLETED, promotionActivityId = 701L, promotionDiscountCents = 600L, deleted = true)
      seedOrder(status = OrderModelStatus.COMPLETED, firstOrderActivityId = 801L, firstOrderDiscountCents = 150L)
      seedOrder(status = OrderModelStatus.COMPLETED, firstOrderActivityId = 802L, firstOrderDiscountCents = 900L)

      repo.sumMarketingDiscountCents(
        MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, refId = 701L),
      ) shouldBe 700L
      repo.sumMarketingDiscountCents(
        MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, refId = 702L),
      ) shouldBe 5000L
      repo.sumMarketingDiscountCents(
        MarketingUsageOrderCriteria(MarketingUsageKind.FIRST_ORDER_REDUCE, refId = 801L),
      ) shouldBe 150L
      // 限时特价/新人价/买赠:无订单列快照 → 0
      repo.sumMarketingDiscountCents(
        MarketingUsageOrderCriteria(MarketingUsageKind.FLASH_SALE_ITEM, refId = 0L),
      ) shouldBe 0L
      repo.sumMarketingDiscountCents(
        MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, refId = 0L),
      ) shouldBe 0L
    }

    test("aggregateMarketingUsageItems GIFT_ACTIVITY:按商品分组数量合计") {
      val order = seedOrder(status = OrderModelStatus.DELIVERING)
      seedOrderGift(orderId = order, giftActivityId = 901L, productId = 21L, quantity = 3)
      seedOrderGift(orderId = order, giftActivityId = 901L, productId = 21L, quantity = 4)
      seedOrderGift(orderId = order, giftActivityId = 901L, productId = 22L, quantity = 5)
      seedOrderGift(orderId = order, giftActivityId = 902L, productId = 21L, quantity = 100)

      val rows =
        repo.aggregateMarketingUsageItems(
          MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, refId = 901L),
        )
      rows shouldHaveSize 2
      rows.first { it.productId == 21L }.quantitySum shouldBe 7L
      rows.first { it.productId == 21L }.unitPriceCents.shouldBeNull()
      rows.first { it.productId == 22L }.quantitySum shouldBe 5L
      // 其他类型 → 空
      repo.aggregateMarketingUsageItems(
        MarketingUsageOrderCriteria(MarketingUsageKind.COUPON_TEMPLATE, refId = 1L),
      ) shouldHaveSize 0
    }

    test("aggregateMarketingUsageItems FLASH_SALE:商品+活动价+时间窗子查询口径") {
      val validFrom = LocalDateTime.of(2026, 11, 1, 0, 0)
      val validTo = LocalDateTime.of(2026, 11, 3, 0, 0)
      // 窗口内订单:同价两单合并
      val w1 = seedOrder(status = OrderModelStatus.COMPLETED, createdAt = validFrom) // 含(闭下界)
      val w2 = seedOrder(status = OrderModelStatus.COMPLETED, createdAt = validTo.minusSeconds(1)) // 含
      seedOrder(status = OrderModelStatus.COMPLETED, createdAt = validTo) // 排除(开上界)
      val wrongPriceOrder = seedOrder(status = OrderModelStatus.COMPLETED, createdAt = validFrom.plusHours(1))
      seedOrderItem(orderId = w1, productId = 31L, unitPriceCents = 990L, quantity = 2)
      seedOrderItem(orderId = w2, productId = 31L, unitPriceCents = 990L, quantity = 3)
      seedOrderItem(orderId = wrongPriceOrder, productId = 31L, unitPriceCents = 1000L, quantity = 8)
      // 窗口外同价单不计
      val outside = seedOrder(status = OrderModelStatus.COMPLETED, createdAt = validFrom.minusDays(1))
      seedOrderItem(orderId = outside, productId = 31L, unitPriceCents = 990L, quantity = 50)
      // 软删明细不计
      seedOrderItem(orderId = w1, productId = 31L, unitPriceCents = 990L, quantity = 7, deleted = true)

      val criteria =
        MarketingUsageOrderCriteria(
          kind = MarketingUsageKind.FLASH_SALE_ITEM,
          refId = 0L,
          productId = 31L,
          unitPriceCents = 990L,
          validFrom = validFrom,
          validTo = validTo,
        )
      val rows = repo.aggregateMarketingUsageItems(criteria)
      rows shouldHaveSize 1
      rows.single().productId shouldBe 31L
      rows.single().unitPriceCents shouldBe 990L
      rows.single().quantitySum shouldBe 5L
      // 订单级追溯同窗口口径
      repo.countOrdersByMarketingUsage(criteria) shouldBe 2L
      // 缺参直接抛错(与原 requireNotNull 一致)
      shouldThrow<IllegalArgumentException> {
        repo.aggregateMarketingUsageItems(
          MarketingUsageOrderCriteria(MarketingUsageKind.FLASH_SALE_ITEM, refId = 0L),
        )
      }
    }

    // ================================================================
    // 原子条件 UPDATE(单条 UPDATE,行数=1 成功 / 0 失败)
    // ================================================================

    test("cancelAtomically:命中状态才置 CANCELLED,幂等/状态不符/软删均返回 0") {
      val id = seedOrder(status = OrderModelStatus.PENDING_PAYMENT)
      val now = LocalDateTime.of(2026, 12, 1, 12, 0)

      repo.cancelAtomically(id, listOf(OrderModelStatus.PENDING_PAYMENT, OrderModelStatus.PENDING_DISPATCH), now) shouldBe 1
      val cancelled = repo.findById(id).orElse(null).shouldNotBeNull()
      cancelled.status shouldBe OrderModelStatus.CANCELLED
      cancelled.updatedAt shouldBe now

      // 二次取消(状态已流转)→ 幂等 0
      repo.cancelAtomically(id, listOf(OrderModelStatus.PENDING_PAYMENT), now) shouldBe 0
      // 状态不在候选集合 → 0
      val completedId = seedOrder(status = OrderModelStatus.COMPLETED)
      repo.cancelAtomically(completedId, listOf(OrderModelStatus.PENDING_PAYMENT), now) shouldBe 0
      // 软删行不可认领 → 0
      val deletedId = seedOrder(status = OrderModelStatus.PENDING_PAYMENT, deleted = true)
      repo.cancelAtomically(deletedId, listOf(OrderModelStatus.PENDING_PAYMENT), now) shouldBe 0
      // 不存在的 id → 0
      repo.cancelAtomically(999_999_999L, listOf(OrderModelStatus.PENDING_PAYMENT), now) shouldBe 0
      // 空候选集合 → 0
      val emptyCandidates = seedOrder(status = OrderModelStatus.PENDING_PAYMENT)
      repo.cancelAtomically(emptyCandidates, emptyList(), now) shouldBe 0
    }

    test("confirmReceiptAtomically:仅待确认可确认,写 completedAt/updatedAt,重复确认返回 0") {
      val id = seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION)
      val confirmAt = LocalDateTime.of(2026, 12, 2, 9, 30)

      repo.confirmReceiptAtomically(id, confirmAt) shouldBe 1
      val completed = repo.findById(id).orElse(null).shouldNotBeNull()
      completed.status shouldBe OrderModelStatus.COMPLETED
      completed.completedAt shouldBe confirmAt
      completed.updatedAt shouldBe confirmAt

      // 幂等:已完成再确认 → 0
      repo.confirmReceiptAtomically(id, confirmAt.plusMinutes(1)) shouldBe 0
      // 非待确认状态 → 0
      val pendingId = seedOrder(status = OrderModelStatus.DELIVERING)
      repo.confirmReceiptAtomically(pendingId, confirmAt) shouldBe 0
      // 软删行不可确认 → 0
      val deletedId = seedOrder(status = OrderModelStatus.AWAITING_CONFIRMATION, deleted = true)
      repo.confirmReceiptAtomically(deletedId, confirmAt) shouldBe 0
      // 不存在的 id → 0
      repo.confirmReceiptAtomically(999_999_998L, confirmAt) shouldBe 0
    }
  })

/** 种子:用户券行(COUPON_TEMPLATE 追溯子查询依赖,表不在 TradePgTestSupport 13 张表内)。 */
private fun seedUserCoupon(
  templateId: Long,
  userId: Long = 700L,
): Long {
  val id = TradePgTestSupport.id()
  val now = LocalDateTime.of(2026, 10, 1, 0, 0)
  transaction(TradePgTestSupport.database) {
    UserCouponsTable.insert {
      it[UserCouponsTable.id] = id
      it[UserCouponsTable.userId] = userId
      it[UserCouponsTable.couponTemplateId] = templateId
      it[UserCouponsTable.status] = "USED"
      it[UserCouponsTable.name] = "测试券"
      it[UserCouponsTable.couponType] = "DISCOUNT"
      it[UserCouponsTable.scope] = "PRODUCT"
      it[UserCouponsTable.expireTime] = now.plusDays(30)
      it[UserCouponsTable.claimTime] = now
      it[UserCouponsTable.usedOrderId] = 1L
      it[UserCouponsTable.usedAt] = now
      it[UserCouponsTable.createdAt] = now
      it[UserCouponsTable.updatedAt] = now
    }
  }
  return id
}

/** 种子:订单赠品行(order_gifts 裸表)。 */
private fun seedOrderGift(
  orderId: Long,
  giftActivityId: Long,
  productId: Long,
  quantity: Int,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    OrderGiftsTable.insert {
      it[OrderGiftsTable.id] = id
      it[OrderGiftsTable.orderId] = orderId
      it[OrderGiftsTable.giftActivityId] = giftActivityId
      it[OrderGiftsTable.giftActivityName] = "测试活动"
      it[OrderGiftsTable.productId] = productId
      it[OrderGiftsTable.productName] = "赠品$productId"
      it[OrderGiftsTable.quantity] = quantity
      it[OrderGiftsTable.createdAt] = LocalDateTime.of(2026, 10, 1, 0, 0)
    }
  }
  return id
}

/** 种子:订单商品明细行(order_items 软删表)。 */
private fun seedOrderItem(
  orderId: Long,
  productId: Long,
  unitPriceCents: Long,
  quantity: Int,
  deleted: Boolean = false,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    OrderItemsTable.insert {
      it[OrderItemsTable.id] = id
      it[OrderItemsTable.orderId] = orderId
      it[OrderItemsTable.productId] = productId
      it[OrderItemsTable.quantity] = quantity
      it[OrderItemsTable.unitPriceCents] = unitPriceCents
      it[OrderItemsTable.totalPriceCents] = unitPriceCents * quantity
      it[OrderItemsTable.productSnapshot] = """{"id":$productId,"name":"商品$productId"}"""
      it[OrderItemsTable.createdAt] = LocalDateTime.of(2026, 10, 1, 0, 0)
      it[OrderItemsTable.updatedAt] = LocalDateTime.of(2026, 10, 1, 0, 0)
    }
    if (deleted) {
      OrderItemsTable.update({ OrderItemsTable.id eq id }) { it[isDeleted] = true }
    }
  }
  return id
}
