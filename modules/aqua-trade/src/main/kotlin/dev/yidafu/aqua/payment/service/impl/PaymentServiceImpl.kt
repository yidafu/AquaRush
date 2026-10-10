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

package dev.yidafu.aqua.payment.service.impl

import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.payment.service.WechatPayGateway
import dev.yidafu.aqua.payment.service.WechatPrepayRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 支付服务实现(PRD 04-order.md §F-ORD-005/R-PAY-01~03):
 * - 真实模式:对接微信支付 V3(JSAPI 小程序支付),发起预支付/验签解析回调/查询交易/申请退款;
 * - 假支付模式(测试环境,wechat.pay.mock-enabled=true 默认开启):发起支付即自动完成
 *   (与回调共用同一完成路径),不依赖商户密钥与微信沙箱,便于开发测试;
 * - 安全:回调必须验签且金额与订单一致(R-PAY-01);重复回调幂等;已取消订单回调不流转状态。
 */
@Service
class PaymentServiceImpl(
  private val orderQueryService: OrderQueryService,
  private val orderQueryApiService: OrderQueryApiService,
  private val orderMutationService: OrderMutationService,
  private val ticketQueryApiService: TicketQueryApiService,
  private val ticketMutationApiService: TicketMutationApiService,
  private val barrelPurchaseQueryService: BarrelPurchaseQueryService,
  private val barrelPurchaseMutationService: BarrelPurchaseMutationService,
  private val paymentRepository: PaymentRepository,
  private val paymentRefundRepository: PaymentRefundRepository,
  private val wechatPayGateway: WechatPayGateway,
  private val bizLogger: BizLogger,
  /** 延迟消息发布(仅 admin 消费,R-ORD-04 支付超时优化);messaging 关闭时跳过发布 */
  private val eventPublishServiceProvider: ObjectProvider<SimplifiedEventPublishService>,
) : PaymentService {
  private val logger = LoggerFactory.getLogger(PaymentService::class.java)

  @Value("\${wechat.pay.appid:}")
  private val appId: String = ""

  @Value("\${wechat.pay.mchid:}")
  private val mchId: String = ""

  @Value("\${wechat.pay.notify-url:}")
  private val notifyUrl: String = ""

  /** 测试环境假支付开关(默认开启;生产置 WECHAT_PAY_MOCK_ENABLED=false 并配置商户参数) */
  @Value("\${wechat.pay.mock-enabled:true}")
  var mockEnabled: Boolean = true

  companion object {
    /** 支付时限(分钟,R-ORD-04:待支付订单超过 30 分钟自动取消) */
    private const val PAYMENT_EXPIRE_MINUTES = 30L

    /** 假支付交易号前缀(保证 payments.transaction_id 唯一) */
    private const val MOCK_PREFIX = "MOCK-"
  }

  /**
   * 创建微信小程序支付订单(F-ORD-005):
   * 校验订单待支付;金额一律服务端计算(R-MON-01,实付=商品金额+配送费);
   * 假支付模式:直接自动完成支付;真实模式:调用微信预支付并返回 Taro.requestPayment 参数。
   */
  override fun createWechatJsapiPay(
    orderId: Long,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any> {
    val order = orderQueryService.getOrderById(orderId)
    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      throw BadRequestException("订单状态不正确，无法创建支付")
    }
    // 金额一律服务端计算:实付 = 商品金额 + 配送费(R-MON-01)
    val payAmountCents = order.payAmountCents
    if (amountCents != payAmountCents) {
      throw BadRequestException("支付金额与订单不一致，无法创建支付")
    }
    upsertPendingPayment(orderId, order.userId, order.orderNo, payAmountCents, description)

    // 测试环境假支付:发起即自动完成(与回调共用完成路径)
    if (mockEnabled) {
      logger.info("假支付模式:订单 {} 支付自动完成", orderId)
      completePaymentSuccess(orderId, mockTransactionId(order.orderNo))
      return mockPaymentParams(order.orderNo)
    }

    if (!wechatPayGateway.isConfigured) {
      throw NotImplementedError("微信支付未配置(wechat.pay.*),请配置商户参数或开启假支付模式")
    }
    return try {
      val params =
        wechatPayGateway.prepayWithRequestPayment(
          WechatPrepayRequest(
            appId = appId,
            mchId = mchId,
            description = description,
            outTradeNo = order.orderNo,
            amountCents = payAmountCents,
            openId = openId,
            notifyUrl = notifyUrl,
          ),
        )
      // 回填 prepay_id 便于对账与超时处理
      paymentRepository.findByOrderId(orderId).ifPresent { payment ->
        payment.prepayId = params.packageVal.removePrefix("prepay_id=")
        paymentRepository.save(payment)
      }
      // 业务日志(F-LOG-003 PAYMENT 域):支付发起
      bizLogger.logPaymentOperation(
        paymentId = orderId,
        operation = "PAYMENT_CREATED",
        result = "SUCCESS",
        userId = order.userId,
        additionalData = mapOf("amountCents" to payAmountCents),
      )
      mapOf(
        "appId" to params.appId,
        "timeStamp" to params.timeStamp,
        "nonceStr" to params.nonceStr,
        "package" to params.packageVal,
        "signType" to params.signType,
        "paySign" to params.paySign,
      )
    } catch (e: Exception) {
      logger.error("创建微信支付失败", e)
      // 业务日志(F-LOG-003 PAYMENT 域):支付发起失败
      bizLogger.logOperation(
        operation = "PAYMENT_FAILED",
        module = "PAYMENT",
        result = "FAILURE",
        target = orderId.toString(),
        additionalData = mapOf("error" to (e.message ?: "unknown error")),
      )
      throw BadRequestException("创建支付失败: ${e.message}")
    }
  }

  /**
   * 处理微信支付回调(PRD 04-order.md §F-ORD-005):
   * 1) SDK 验签并解密(R-PAY-01,验签失败抛异常 → 返回 false 由微信重试);
   * 2) 按 out_trade_no 匹配订单/购票单,校验回调金额与业务单一致(R-PAY-01);
   * 3) 幂等:重复回调/已取消订单均返回成功但不重复流转状态(AC-ORD-005-05/08)。
   */
  @Transactional
  override fun handleWechatPayCallback(
    rawBody: String,
    headers: Map<String, String>,
  ): Boolean {
    try {
      // 1. 验签并解密回调内容
      val transaction =
        wechatPayGateway.parseNotification(
          rawBody = rawBody,
          serialNumber = headers["wechatpay-serial"] ?: "",
          timestamp = headers["wechatpay-timestamp"] ?: "",
          nonce = headers["wechatpay-nonce"] ?: "",
          signature = headers["wechatpay-signature"] ?: "",
        )
      val outTradeNo = transaction.outTradeNo

      // 2. 派发:先查订单,再查购票单(桶水票购票支付,PRD 10-marketing.md §F-MKT-007)
      val order = orderQueryApiService.findByOrderNumber(outTradeNo)
      if (order != null) {
        if (transaction.amountCents != order.payAmountCents) {
          logger.error(
            "支付回调金额不一致: 单号={}, 回调金额={}, 订单应付={}",
            outTradeNo,
            transaction.amountCents,
            order.payAmountCents,
          )
          bizLogger.logOperation(
            operation = "PAYMENT_FAILED",
            module = "PAYMENT",
            result = "FAILURE",
            target = outTradeNo,
            additionalData = mapOf("error" to "amount mismatch, callback=${transaction.amountCents}, expected=${order.payAmountCents}"),
          )
          return false
        }
        val processed = completePaymentSuccess(order.id!!, transaction.transactionId)
        logger.info("支付回调处理成功(订单): 订单号={}, 交易号={}", outTradeNo, transaction.transactionId)
        return processed
      }

      // 购桶单(水桶费独立支付,G06:一次性塑料水桶费,前缀 BK 单号,见 14-misc.md §G06)
      val barrelPurchase = barrelPurchaseQueryService.findByOrderNo(outTradeNo)
      if (barrelPurchase != null) {
        if (transaction.amountCents != barrelPurchase.totalAmountCents) {
          logger.error(
            "支付回调金额不一致(购桶): 单号={}, 回调金额={}, 应付={}",
            outTradeNo,
            transaction.amountCents,
            barrelPurchase.totalAmountCents,
          )
          return false
        }
        // 幂等:已支付/已退还/已取消的购桶单不重复流转
        if (barrelPurchase.paymentStatus != BarrelPaymentStatus.PENDING) {
          logger.info("购桶单 {} 状态为 {},回调跳过", outTradeNo, barrelPurchase.paymentStatus)
          return true
        }
        barrelPurchaseMutationService.handleBarrelPurchasePaid(barrelPurchase.id!!, transaction.transactionId)
        logger.info("支付回调处理成功(购桶): 单号={}, 交易号={}", outTradeNo, transaction.transactionId)
        // 业务日志(F-LOG-003 PAYMENT 域):支付完成(购桶)
        bizLogger.logOperation(
          operation = "PAYMENT_COMPLETED",
          module = "PAYMENT",
          result = "SUCCESS",
          target = "barrel:$outTradeNo",
          additionalData = mapOf("transactionId" to transaction.transactionId),
        )
        return true
      }

      val purchase = ticketQueryApiService.findPurchaseByOrderNo(outTradeNo)
      if (purchase != null) {
        if (transaction.amountCents != purchase.packagePriceCents) {
          logger.error(
            "支付回调金额不一致(购票): 单号={}, 回调金额={}, 应付={}",
            outTradeNo,
            transaction.amountCents,
            purchase.packagePriceCents,
          )
          return false
        }
        // 幂等:已支付/已取消的购票单不重复流转
        if (purchase.paymentStatus != TicketPurchaseStatus.PENDING) {
          logger.info("购票单 {} 状态为 {},回调跳过", outTradeNo, purchase.paymentStatus)
          return true
        }
        ticketMutationApiService.handleTicketPurchasePaid(purchase.id!!, transaction.transactionId)
        logger.info("支付回调处理成功(购票): 单号={}, 交易号={}", outTradeNo, transaction.transactionId)
        // 业务日志(F-LOG-003 PAYMENT 域):支付完成(购票)
        bizLogger.logOperation(
          operation = "PAYMENT_COMPLETED",
          module = "PAYMENT",
          result = "SUCCESS",
          target = "purchase:$outTradeNo",
          additionalData = mapOf("transactionId" to transaction.transactionId),
        )
        return true
      }

      logger.error("支付回调未匹配业务单: out_trade_no={}", outTradeNo)
      bizLogger.logOperation(
        operation = "PAYMENT_FAILED",
        module = "PAYMENT",
        result = "FAILURE",
        target = outTradeNo,
        additionalData = mapOf("error" to "no matching business order"),
      )
      return false
    } catch (e: Exception) {
      logger.error("处理支付回调失败", e)
      // 业务日志(F-LOG-003 PAYMENT 域):支付回调失败
      bizLogger.logOperation(
        operation = "PAYMENT_FAILED",
        module = "PAYMENT",
        result = "FAILURE",
        target = "callback",
        additionalData = mapOf("error" to (e.message ?: "unknown error")),
      )
      return false
    }
  }

  /**
   * 查询支付状态:待支付订单在假支付模式下以订单状态为准;
   * 真实模式调用微信交易查询接口映射 SUCCESS/CLOSED/UNKNOWN。
   */
  override fun queryPaymentStatus(orderId: Long): String {
    val order = orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      return when (order.status) {
        OrderModelStatus.PENDING_DELIVERY -> "SUCCESS"
        OrderModelStatus.CANCELLED -> "CLOSED"
        else -> "UNKNOWN"
      }
    }
    if (mockEnabled || !wechatPayGateway.isConfigured) {
      return "UNKNOWN"
    }
    return try {
      val transaction = wechatPayGateway.queryTransaction(order.orderNo) ?: return "UNKNOWN"
      when (transaction.tradeState) {
        "SUCCESS" -> "SUCCESS"
        "CLOSED", "REVOKED", "PAYERROR" -> "CLOSED"
        else -> "UNKNOWN"
      }
    } catch (e: Exception) {
      logger.error("查询支付状态失败", e)
      throw BadRequestException("查询支付状态失败: ${e.message}")
    }
  }

  /**
   * 申请退款(订单取消触发,R-PAY-02 退款能力):默认订单退款类型 ORDER_REFUND
   */
  @Transactional
  override fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
    reason: String,
  ): Map<String, Any> = refund(transactionId, refundAmountCents, totalAmountCents, reason, RefundType.ORDER_REFUND)

  /**
   * 申请退款(带退款类型,R-PAY-02/G06):
   * 真实模式调用微信退款接口;假支付模式直接标记成功。
   * 退款成功后落 `payment_refunds` 退款记录(status=COMPLETED + refund_type,
   * PRD「退款须与退款记录一致」),供营收统计「退款/净营收」维度按退款成功时间(processed_at)聚合;
   * DEPOSIT_REFUND(水桶费/桶押金退还)不计入退款统计(G06,见 14-misc.md);
   * 交易号未命中支付记录时退款仍执行,仅跳过 payments 更新/落库(warn 日志,人工核对);
   * refundRequestId 非空(R-PAY-02 审核制)时回写该条申请为 COMPLETED,不再新插记录。
   */
  @Transactional
  override fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
    reason: String,
    refundType: RefundType,
    refundRequestId: Long?,
  ): Map<String, Any> {
    try {
      // 先取支付记录:退款记录落库与 payments.status 更新共用同一记录
      val payment = paymentRepository.findByTransactionId(transactionId).orElse(null)
      if (payment == null) {
        logger.warn("退款未命中支付记录,跳过 payments 更新/退款记录落库: transactionId={}", transactionId)
      }
      val orderNo =
        payment?.let { orderQueryService.getOrderById(it.orderId).orderNo }
          ?: System.currentTimeMillis().toString()
      val outRefundNo = "R-$orderNo-${System.currentTimeMillis()}"

      val result =
        if (mockEnabled) {
          mapOf(
            "refundId" to "MOCK-REFUND-${System.currentTimeMillis()}",
            "outRefundNo" to outRefundNo,
            "mock" to true,
          )
        } else {
          if (!wechatPayGateway.isConfigured) {
            throw NotImplementedError("微信支付未配置(wechat.pay.*),无法执行真实退款")
          }
          mapOf("refundId" to wechatPayGateway.createRefund(outRefundNo, transactionId, refundAmountCents, totalAmountCents, reason))
        }

      payment?.let {
        it.status = PaymentStatus.REFUNDED
        paymentRepository.save(it)
      }
      // 退款记录落库(R-PAY-02:退款记录与支付记录一致;退款统计按 processed_at 归属窗口;refund_type 区分订单/押金退款)
      val existingRequest = refundRequestId?.let { id -> paymentRefundRepository.findById(id).orElse(null) }
      if (existingRequest != null) {
        // 审核制:取消订单已生成 PENDING 申请,此处回写同一条记录为 COMPLETED,避免双记录双计
        existingRequest.refundId = result["refundId"] as? String
        existingRequest.outRefundNo = outRefundNo
        existingRequest.refundAmount = refundAmountCents
        existingRequest.refundReason = reason
        existingRequest.status = RefundStatus.COMPLETED
        existingRequest.refundType = refundType
        existingRequest.refundedAt = LocalDateTime.now()
        existingRequest.failureReason = null
        paymentRefundRepository.save(existingRequest)
      } else {
        payment?.let {
          paymentRefundRepository.save(
            PaymentRefundModel(
              paymentId = it.id!!,
              orderId = it.orderId,
              userId = it.userId,
              refundId = result["refundId"] as? String,
              outRefundNo = outRefundNo,
              refundAmount = refundAmountCents,
              refundReason = reason,
              status = RefundStatus.COMPLETED,
              refundType = refundType,
              refundedAt = LocalDateTime.now(),
            ),
          )
        }
      }
      // 业务日志(F-LOG-003 PAYMENT 域):退款申请成功
      bizLogger.logOperation(
        operation = "PAYMENT_REFUNDED",
        module = "PAYMENT",
        result = "SUCCESS",
        target = transactionId,
        additionalData =
          mapOf(
            "refundAmountCents" to refundAmountCents,
            "totalAmountCents" to totalAmountCents,
            "reason" to reason,
            "refundType" to refundType.name,
          ),
      )
      return result
    } catch (e: Exception) {
      logger.error("申请退款失败", e)
      // 业务日志(F-LOG-003 PAYMENT 域):退款失败
      bizLogger.logOperation(
        operation = "PAYMENT_REFUNDED",
        module = "PAYMENT",
        result = "FAILURE",
        target = transactionId,
        additionalData = mapOf("error" to (e.message ?: "unknown error"), "reason" to reason),
      )
      throw BadRequestException("申请退款失败: ${e.message}")
    }
  }

  /**
   * 创建桶水票购票支付订单(F-MKT-007,PRD 10-marketing.md):
   * 校验购票单存在且待支付;购票即预收款项,不支持退款;
   * 假支付模式自动完成入账;真实模式走微信预支付。
   */
  override fun createWechatJsapiPayForTicket(
    ticketPurchaseId: Long,
    orderNo: String,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any> {
    val purchase =
      ticketQueryApiService.findPurchaseById(ticketPurchaseId)
        ?: throw BadRequestException("购票单不存在: $ticketPurchaseId")
    if (purchase.paymentStatus != TicketPurchaseStatus.PENDING) {
      throw BadRequestException("购票单状态不正确,无法创建支付")
    }
    if (mockEnabled) {
      logger.info("假支付模式:购票单 {} 支付自动完成", ticketPurchaseId)
      ticketMutationApiService.handleTicketPurchasePaid(ticketPurchaseId, mockTransactionId(orderNo))
      // 业务日志(F-LOG-003 PAYMENT 域):购票支付完成
      bizLogger.logOperation(
        operation = "PAYMENT_COMPLETED",
        module = "PAYMENT",
        result = "SUCCESS",
        target = "purchase:$orderNo",
        additionalData = mapOf("transactionId" to mockTransactionId(orderNo)),
      )
      return mockPaymentParams(orderNo)
    }
    if (!wechatPayGateway.isConfigured) {
      throw NotImplementedError("微信支付未配置(wechat.pay.*),请配置商户参数或开启假支付模式")
    }
    return try {
      val params =
        wechatPayGateway.prepayWithRequestPayment(
          WechatPrepayRequest(
            appId = appId,
            mchId = mchId,
            description = description,
            outTradeNo = orderNo,
            amountCents = amountCents,
            openId = openId,
            notifyUrl = notifyUrl,
          ),
        )
      // 业务日志(F-LOG-003 PAYMENT 域):购票支付发起
      bizLogger.logPaymentOperation(
        paymentId = ticketPurchaseId,
        operation = "PAYMENT_CREATED",
        result = "SUCCESS",
        userId = purchase.userId,
        additionalData = mapOf("amountCents" to amountCents, "orderNo" to orderNo),
      )
      mapOf(
        "appId" to params.appId,
        "timeStamp" to params.timeStamp,
        "nonceStr" to params.nonceStr,
        "package" to params.packageVal,
        "signType" to params.signType,
        "paySign" to params.paySign,
      )
    } catch (e: Exception) {
      logger.error("创建购票支付失败", e)
      throw BadRequestException("创建支付失败: ${e.message}")
    }
  }

  /**
   * 创建购桶(水桶费)支付订单(G06,PRD 14-misc.md):
   * 校验购桶单存在且待支付;一次性塑料水桶费,独立支付,每用户总限购一次;
   * 假支付模式自动完成入账;真实模式走微信预支付。
   */
  override fun createWechatJsapiPayForBarrelPurchase(
    barrelPurchaseId: Long,
    orderNo: String,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any> {
    val purchase =
      barrelPurchaseQueryService.getById(barrelPurchaseId)
        ?: throw BadRequestException("购桶单不存在: $barrelPurchaseId")
    if (purchase.paymentStatus != BarrelPaymentStatus.PENDING) {
      throw BadRequestException("购桶单状态不正确,无法创建支付")
    }
    if (mockEnabled) {
      logger.info("假支付模式:购桶单 {} 支付自动完成", barrelPurchaseId)
      barrelPurchaseMutationService.handleBarrelPurchasePaid(barrelPurchaseId, mockTransactionId(orderNo))
      // 业务日志(F-LOG-003 PAYMENT 域):购桶支付完成
      bizLogger.logOperation(
        operation = "PAYMENT_COMPLETED",
        module = "PAYMENT",
        result = "SUCCESS",
        target = "barrel:$orderNo",
        additionalData = mapOf("transactionId" to mockTransactionId(orderNo)),
      )
      return mockPaymentParams(orderNo)
    }
    if (!wechatPayGateway.isConfigured) {
      throw NotImplementedError("微信支付未配置(wechat.pay.*),请配置商户参数或开启假支付模式")
    }
    return try {
      val params =
        wechatPayGateway.prepayWithRequestPayment(
          WechatPrepayRequest(
            appId = appId,
            mchId = mchId,
            description = description,
            outTradeNo = orderNo,
            amountCents = amountCents,
            openId = openId,
            notifyUrl = notifyUrl,
          ),
        )
      // 业务日志(F-LOG-003 PAYMENT 域):购桶支付发起
      bizLogger.logOperation(
        operation = "PAYMENT_CREATED",
        module = "PAYMENT",
        result = "SUCCESS",
        target = "barrel:$orderNo",
        additionalData = mapOf("amountCents" to amountCents, "barrelPurchaseId" to barrelPurchaseId),
      )
      mapOf(
        "appId" to params.appId,
        "timeStamp" to params.timeStamp,
        "nonceStr" to params.nonceStr,
        "package" to params.packageVal,
        "signType" to params.signType,
        "paySign" to params.paySign,
      )
    } catch (e: Exception) {
      logger.error("创建购桶支付失败", e)
      throw BadRequestException("创建支付失败: ${e.message}")
    }
  }

  /**
   * 处理支付超时(R-ORD-04 支付超时优化):
   * 由 admin 的延迟消息消费(PaymentExpiryCheckConsumer)或兜底扫描(PaymentExpiryScheduler)触发;
   * ① 数据库条件 UPDATE 认领(WHERE status=PENDING 且已到期 → EXPIRED),0 行=已被处理/未到期,
   *    直接返回——幂等且互斥(MQ 重复投递、MQ 与扫描并发、admin 多实例并发均只处理一次);
   * ② 订单仍待支付才流转取消(恢复库存/退回权益/发布 PAYMENT_TIMEOUT),异常上抛使事务回滚、
   *    认领撤销,触发 MQ 重投或扫描重试,不产生「支付已 EXPIRED 但订单未取消」卡死态;
   * ③ 回调竞态对账:订单已支付(PENDING_DELIVERY)→ 支付记录回写 SUCCESS。
   */
  @Transactional
  override fun handlePaymentTimeout(orderId: Long) {
    val now = LocalDateTime.now()
    val claimed = paymentRepository.claimExpired(orderId, now)
    if (claimed == 0) {
      // 已被其他触发方处理 / 支付已成功 / 未到期(旧延迟消息或重复消息),幂等跳过
      logger.debug("支付超时认领跳过(已处理或未到期): 订单 {}", orderId)
      return
    }
    val order = orderQueryService.getOrderById(orderId)
    if (order.status == OrderModelStatus.PENDING_PAYMENT) {
      orderMutationService.handlePaymentTimeout(orderId)
      logger.info("支付超时处理成功: 订单ID={}", orderId)
    }
    // 回调与超时竞态且回调胜出:支付记录同步为 SUCCESS
    if (order.status == OrderModelStatus.PENDING_DELIVERY) {
      val payment = paymentRepository.findByOrderId(orderId).orElse(null)
      if (payment != null && payment.status != PaymentStatus.SUCCESS) {
        payment.status = PaymentStatus.SUCCESS
        payment.transactionId = order.paymentTransactionId
        payment.paidAt = order.paymentTime
        paymentRepository.save(payment)
      }
    }
    // 业务日志(F-LOG-003 PAYMENT 域):支付超时
    bizLogger.logPaymentOperation(
      paymentId = orderId,
      operation = "PAYMENT_TIMEOUT",
      result = "SUCCESS",
      additionalData = mapOf("orderId" to orderId),
    )
  }

  // ================================================================
  // 内部方法
  // ================================================================

  /**
   * 幂等落库/更新待支付记录(同一订单重复发起支付时复用并重置)。
   * 落库后发布支付到期检查延迟消息(R-ORD-04 支付超时优化):
   * broker 在 expiredAt 时刻投递,由 admin 消费触发超时处理;重复消息/旧消息
   * 由消费端 CAS 认领(状态+到期时间)幂等跳过。
   */
  private fun upsertPendingPayment(
    orderId: Long,
    userId: Long,
    orderNo: String,
    amountCents: Long,
    description: String,
  ) {
    val existing = paymentRepository.findByOrderId(orderId).orElse(null)
    val payment =
      existing
        ?: PaymentModel(
          orderId = orderId,
          userId = userId,
          amount = amountCents,
          description = description,
          status = PaymentStatus.PENDING,
          paymentMethod = PaymentMethod.WECHAT_PAY,
          expiredAt = LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES),
        )
    if (existing != null && existing.status != PaymentStatus.PENDING) {
      // 重新支付:重置为待支付
      existing.status = PaymentStatus.PENDING
      existing.transactionId = null
      existing.prepayId = null
      existing.paidAt = null
      existing.expiredAt = LocalDateTime.now().plusMinutes(PAYMENT_EXPIRE_MINUTES)
    }
    val saved = paymentRepository.save(payment)
    saved.expiredAt?.let { expireAt ->
      eventPublishServiceProvider.ifAvailable { eventPublishService ->
        eventPublishService.publishPaymentExpiryCheck(saved.orderId, saved.userId, expireAt)
      }
    }
  }

  /**
   * 支付成功公共完成路径(假支付与真实回调共用):
   * 幂等处理,已取消/已完成的订单不流转状态(AC-ORD-005-08),支付记录置 SUCCESS。
   */
  private fun completePaymentSuccess(
    orderId: Long,
    transactionId: String,
  ): Boolean {
    val order = orderQueryService.getOrderById(orderId)
    // 幂等:已支付且交易号一致 → 视为成功,不重复流转(R-PAY-01)
    if (order.status == OrderModelStatus.PENDING_DELIVERY && order.paymentTransactionId == transactionId) {
      markPaymentSuccess(orderId, transactionId)
      return true
    }
    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      logger.warn("订单 {} 状态为 {},支付回调不流转状态", orderId, order.status)
      return true
    }
    orderMutationService.handlePaymentSuccess(orderId, transactionId)
    markPaymentSuccess(orderId, transactionId)
    // 业务日志(F-LOG-003 PAYMENT 域):支付完成(订单)
    bizLogger.logPaymentOperation(
      paymentId = orderId,
      operation = "PAYMENT_COMPLETED",
      result = "SUCCESS",
      userId = order.userId,
      additionalData = mapOf("transactionId" to transactionId),
    )
    return true
  }

  private fun markPaymentSuccess(
    orderId: Long,
    transactionId: String,
  ) {
    paymentRepository.findByOrderId(orderId).ifPresent { payment ->
      if (payment.status != PaymentStatus.SUCCESS) {
        payment.status = PaymentStatus.SUCCESS
        payment.transactionId = transactionId
        payment.paidAt = LocalDateTime.now()
        paymentRepository.save(payment)
      }
    }
  }

  /** 假支付交易号(基于商户订单号,保证唯一) */
  private fun mockTransactionId(orderNo: String): String = MOCK_PREFIX + orderNo

  /** 假支付返回参数:前端据此跳过 Taro.requestPayment 直接刷新订单状态 */
  private fun mockPaymentParams(orderNo: String): Map<String, Any> =
    mapOf(
      "mock" to true,
      "paid" to true,
      "transactionId" to mockTransactionId(orderNo),
    )

  // 保持原有接口兼容性
  override fun createWechatPayOrder(
    orderId: Long,
    amountCents: Long,
    description: String,
  ): Map<String, Any> = createWechatJsapiPay(orderId, amountCents, description, "mock_openid")

  override fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
  ): Boolean =
    try {
      refund(transactionId, refundAmountCents, totalAmountCents, "订单取消退款")
      true
    } catch (e: Exception) {
      false
    }
}
