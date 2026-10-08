# Event Processing, Cache, and WeChat Notes

> Imported by the root `CLAUDE.md`. Cross-cutting backend patterns for domain events, caching, and WeChat integration.

## Event Processing

When adding new domain events:
1. Define event in the appropriate domain module
2. Publish event via SimplifiedEventPublishService using Artemis MQ
3. Create event handler to consume from Artemis queue
4. Add retry logic for idempotent processing

## Cache Usage

Leverage the built-in caching system:
```kt
@Cacheable(value = ["orders"], key = "#orderId")
fun getOrderById(orderId: UUID): Order? {
    return orderRepository.findById(orderId).orElse(null)
}
```

Use namespaces for cache isolation and configure appropriate TTL based on data volatility.

## WeChat Integration Notes

- WeChat Mini Program login requires proper app-id and app-secret configuration
- WeChat Pay v3 needs merchant private key and platform certificate
- Template messages require pre-approved template IDs from WeChat
- All WeChat API calls should include proper error handling and retry logic

## WeChat Pay V3 (支付域,PRD 04-order.md §F-ORD-005/R-PAY-01)

- 网关抽象:`WechatPayGateway`(aqua-trade payment 域),真实实现 `SdkWechatPayGateway` 基于 wechatpay-java 0.2.17:
  `RSAAutoCertificateConfig`(自动下载平台证书) + `JsapiServiceExtension.prepayWithRequestPayment`(返回小程序
  requestPayment 参数含 paySign) + `NotificationParser` 验签解密回调 + `RefundService.create` 退款。
- 配置:`wechat.pay.*`(appid/mchid/private-key-path/merchant-serial-number/api-v3-key/notify-url),
  密钥不落库,全部环境变量注入;两个服务(admin/client)均需配置(事件侧退款/超时处理)。
- 测试环境假支付:`wechat.pay.mock-enabled=true`(默认),发起支付即自动完成(与回调共用 completePaymentSuccess 完成路径,
  幂等),返回 `{mock:true, paid:true, transactionId:"MOCK-<orderNo>"}`,前端跳过 requestPayment 直接刷新状态;
  生产置 `WECHAT_PAY_MOCK_ENABLED=false`。
- 回调端点:`POST /api/pay/notify`(aqua-client,permitAll),必须透传原始报文与 wechatpay-* 头;
  验签/金额校验失败返回 500 FAIL 由微信重试;重复回调/已取消订单返回成功但不流转状态。
- 支付超时:R-ORD-04,延迟消息(MQ)为主触发 + 定时扫表兜底,仅 admin 服务处理——
  创建/重置待支付记录时发布 `PAYMENT_EXPIRY_CHECK` 延迟消息(Artemis
  `AMQ_SCHEDULED_DELIVERY_TIME`=expiredAt,投递到独立队列 `payment-expiry-events`),
  到期后由 `PaymentExpiryCheckConsumer`(`@AdminService`,client 不消费)触发
  `handlePaymentTimeout` 自动取消并恢复库存;`PaymentExpiryScheduler`(`@AdminService`,
  默认 5 分钟,分页游标扫描)兜底;幂等与互斥由 `payments.version` 乐观锁 CAS 认领
  (`claimExpired`:PENDING 且到期 → EXPIRED, version+1)保证,并发触发仅一方生效;
  用户/管理员手动取消与支付超时并发防重复恢复库存:订单级乐观锁 CAS 认领
  (`OrderRepository.cancelAtomically`:可取消状态 → CANCELLED 仅一方成功,先认领再恢复库存)。