// 转换自 db/changelog/db.changelog-master.xml (liquibase-kotlin 4.33.0-0.10.1)
// compiled changelog 主入口：spring.liquibase.change-log = db/changelog/DbChangelogMaster
// 路径即类名（KotlinCompiledLiquibaseChangeLogParser 用 physicalFilePath 反射加载本类）。
package db.changelog

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class DbChangelogMaster :
  KotlinCompiledDatabaseChangeLog({
    // Development schema recreation - drops and recreates all tables
    include(file = "db/changelog/changes/V100InitComprehensiveSchema", logicalFilePath = "db/changelog/changes/v1.0.0-init-comprehensive-schema.xml")
    include(file = "db/changelog/changes/V103AddOrderSearchContent", logicalFilePath = "db/changelog/changes/v1.0.3-add-order-search-content.xml")
    include(file = "db/changelog/changes/V104AddDailyCollectionTables", logicalFilePath = "db/changelog/changes/v1.0.4-add-daily-collection-tables.xml")
    include(file = "db/changelog/changes/V105AddCalculatedFields", logicalFilePath = "db/changelog/changes/v1.0.5-add-calculated-fields.xml")
    include(file = "db/changelog/changes/V106AddOpsConfigTables", logicalFilePath = "db/changelog/changes/v1.0.6-add-ops-config-tables.xml")
    include(file = "db/changelog/changes/V107AddOpsNoticeStoreDeliveryFee", logicalFilePath = "db/changelog/changes/v1.0.7-add-ops-notice-store-delivery-fee.xml")
    include(file = "db/changelog/changes/V108AddSystemConfigParams", logicalFilePath = "db/changelog/changes/v1.0.8-add-system-config-params.xml")
    include(file = "db/changelog/changes/V109AddMarketingTables", logicalFilePath = "db/changelog/changes/v1.0.9-add-marketing-tables.xml")
    include(file = "db/changelog/changes/V110AddMarketingFlashGiftFirstorder", logicalFilePath = "db/changelog/changes/v1.1.0-add-marketing-flash-gift-firstorder.xml")
    include(file = "db/changelog/changes/V111AddBusinessLogOperation", logicalFilePath = "db/changelog/changes/v1.1.1-add-business-log-operation.xml")
    include(file = "db/changelog/changes/V120DropOrderSingleProductColumns", logicalFilePath = "db/changelog/changes/v1.2.0-drop-order-single-product-columns.xml")
    include(file = "db/changelog/changes/V121MergeDeliveryWorkerIntoAdmins", logicalFilePath = "db/changelog/changes/v1.2.1-merge-delivery-worker-into-admins.xml")
    include(file = "db/changelog/changes/V122AddAdminAccountStatus", logicalFilePath = "db/changelog/changes/v1.2.2-add-admin-account-status.xml")
    include(file = "db/changelog/changes/V123AddDeliveryTrajectory", logicalFilePath = "db/changelog/changes/v1.2.3-add-delivery-trajectory.xml")
    include(file = "db/changelog/changes/V124AddSystemSecurityNotificationDataConfig", logicalFilePath = "db/changelog/changes/v1.2.4-add-system-security-notification-data-config.xml")
    include(file = "db/changelog/changes/V125AddOrderIdempotencyDeliveryTime", logicalFilePath = "db/changelog/changes/v1.2.5-add-order-idempotency-delivery-time.xml")
    include(file = "db/changelog/changes/V126AddDeliveryPushConfigParams", logicalFilePath = "db/changelog/changes/v1.2.6-add-delivery-push-config-params.xml")
    include(file = "db/changelog/changes/V130LogPartitionBackup", logicalFilePath = "db/changelog/changes/v1.3.0-log-partition-backup.xml")
    include(file = "db/changelog/changes/V140FixPaymentRefundsStatusCheck", logicalFilePath = "db/changelog/changes/v1.4.0-fix-payment-refunds-status-check.xml")
    include(file = "db/changelog/changes/V150AddNotificationTables", logicalFilePath = "db/changelog/changes/v1.5.0-add-notification-tables.xml")
    include(file = "db/changelog/changes/V160ConvertAddressCoordsToWgs84", logicalFilePath = "db/changelog/changes/v1.6.0-convert-address-coords-to-wgs84.xml")
    include(file = "db/changelog/changes/V170AddProductOptions", logicalFilePath = "db/changelog/changes/v1.7.0-add-product-options.xml")
    include(file = "db/changelog/changes/V180DropProductDeliverySettings", logicalFilePath = "db/changelog/changes/v1.8.0-drop-product-delivery-settings.xml")
    include(file = "db/changelog/changes/V190AddOrderTicketDiscountSnapshot", logicalFilePath = "db/changelog/changes/v1.9.0-add-order-ticket-discount-snapshot.xml")
    // G23 照片核验 / G28 争议日报驳回留痕
    include(file = "db/changelog/changes/V200AddReviewColumns", logicalFilePath = "db/changelog/changes/v2.0.0-add-review-columns.xml")
    // R-PAY-02 退款审核流
    include(file = "db/changelog/changes/V201AddRefundReviewColumns", logicalFilePath = "db/changelog/changes/v2.0.1-add-refund-review-columns.xml")
    // G06 桶押金模块
    include(file = "db/changelog/changes/V202AddDeposit", logicalFilePath = "db/changelog/changes/v2.0.2-add-deposit.xml")
    // G06 水桶费改造(押金 → 一次性塑料水桶费:价目表/购桶单/明细)
    include(file = "db/changelog/changes/V210AddBarrelFee", logicalFilePath = "db/changelog/changes/v2.1.0-add-barrel-fee.xml")
    // 南山牌桶装水演示数据:位于全部 schema 变更之后,可引用最终 schema;重建数据库后生效
    include(file = "db/changelog/changes/V170SeedNanshanDemoData", logicalFilePath = "db/changelog/changes/v1.7.0-seed-nanshan-demo-data.xml")
    // F-MKT-007 水票唯一递增票号(SP+年月+8位序号,按月重置):置于 seed 之后以回填种子数据
    include(file = "db/changelog/changes/V220AddUserTicketNo", logicalFilePath = "db/changelog/changes/v2.2.0-add-user-ticket-no.xml")
    // 数据修正:seed 将 2 笔订单 payment_method 写成 CASH_ON_DELIVERY,不在域枚举 PaymentMethod(WECHAT_PAY/CASH)中,导致 orderStatistics 的 findAll() 实体映射崩溃 → 归一为 CASH(PRD 04-order.md §R-PAY-03)
    include(file = "db/changelog/changes/V230FixOrderPaymentMethod", logicalFilePath = "db/changelog/changes/v2.3.0-fix-order-payment-method.xml")
    // 第 3 层历史趋势指标:原子日汇总中间表(夜间快照任务写入,查询侧两段合并)
    include(file = "db/changelog/changes/V240AddDailyStatFacts", logicalFilePath = "db/changelog/changes/v2.4.0-add-daily-stat-facts.xml")
    // F-STA-007 营销统计:营销日事实 + 营销使用日事实(同一夜间快照任务写入)
    include(file = "db/changelog/changes/V250AddMarketingDailyFacts", logicalFilePath = "db/changelog/changes/v2.5.0-add-marketing-daily-facts.xml")
    // 消息发送补齐:notifications 模板上下文列(失败消息手动重发) + 优惠过期提醒窗口/配送超时阈值参数
    include(file = "db/changelog/changes/V260AddNotificationSendSupport", logicalFilePath = "db/changelog/changes/v2.6.0-add-notification-send-support.xml")
    // R-ORD-04 支付超时优化:payments.version 乐观锁列 + chk_payments_status 对齐枚举
    include(file = "db/changelog/changes/V270AddPaymentExpiryOptimization", logicalFilePath = "db/changelog/changes/v2.7.0-add-payment-expiry-optimization.xml")
    // 营收口径修正:已完成营收按完成时间(completed_at)归属日的日事实表(订单数仍按下单日)
    include(file = "db/changelog/changes/V280AddDailyRevenueFacts", logicalFilePath = "db/changelog/changes/v2.8.0-add-daily-revenue-facts.xml")
    // G23/F-ORD-008/F-ORD-024:chk_operation_type_valid 对齐域枚举(照片核验/催单/改址操作类型)
    include(file = "db/changelog/changes/V290FixOrderOperationTypeCheck", logicalFilePath = "db/changelog/changes/v2.9.0-fix-order-operation-type-check.xml")
    // JPA→Exposed P4:orders.status 加宽到 varchar(30),容纳枚举 AWAITING_CONFIRMATION(21 字符)
    include(file = "db/changelog/changes/V300WidenOrdersStatus", logicalFilePath = "db/changelog/changes/v3.0.0-widen-orders-status.xml")
    // P5 收口:补回历史 Hibernate ddl-auto 建出、changelog 缺失的 notification_settings 表
    include(file = "db/changelog/changes/V310AddNotificationSettings", logicalFilePath = "db/changelog/changes/v3.1.0-add-notification-settings.xml")
    // Outbox 替代 Artemis MQ:重建 V100 遗留的 outbox_events(从未被读写)
    include(file = "db/changelog/changes/V320RecreateOutboxEvents", logicalFilePath = "db/changelog/changes/v3.2.0-recreate-outbox-events.xml")
    // 缺陷 #6:删除 users.balance_cents(储值不做,PRD 2026-10-09 决策)
    include(file = "db/changelog/changes/V330DropUsersBalanceCents", logicalFilePath = "db/changelog/changes/v3.3.0-drop-users-balance-cents.xml")
  })
