// 转换自 db/changelog/db.changelog-master.xml (liquibase-kotlin 4.33.0-0.10.1)
// compiled changelog 主入口：spring.liquibase.change-log = db/changelog/DbChangelogMaster
// 路径即类名（KotlinCompiledLiquibaseChangeLogParser 用 physicalFilePath 反射加载本类）。
package db.changelog

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class DbChangelogMaster :
  KotlinCompiledDatabaseChangeLog({
    // Development schema recreation - drops and recreates all tables
    include(file = "db/changelog/changes/V100InitComprehensiveSchema", logicalFilePath = "db/changelog/changes/v1.0.0-init-comprehensive-schema.xml")
    include(file = "db/changelog/changes/V104AddDailyCollectionTables", logicalFilePath = "db/changelog/changes/v1.0.4-add-daily-collection-tables.xml")
    include(file = "db/changelog/changes/V106AddOpsConfigTables", logicalFilePath = "db/changelog/changes/v1.0.6-add-ops-config-tables.xml")
    include(file = "db/changelog/changes/V107AddOpsNoticeStoreDeliveryFee", logicalFilePath = "db/changelog/changes/v1.0.7-add-ops-notice-store-delivery-fee.xml")
    include(file = "db/changelog/changes/V108AddSystemConfigParams", logicalFilePath = "db/changelog/changes/v1.0.8-add-system-config-params.xml")
    include(file = "db/changelog/changes/V109AddMarketingTables", logicalFilePath = "db/changelog/changes/v1.0.9-add-marketing-tables.xml")
    include(file = "db/changelog/changes/V110AddMarketingFlashGiftFirstorder", logicalFilePath = "db/changelog/changes/v1.1.0-add-marketing-flash-gift-firstorder.xml")
    include(file = "db/changelog/changes/V123AddDeliveryTrajectory", logicalFilePath = "db/changelog/changes/v1.2.3-add-delivery-trajectory.xml")
    include(file = "db/changelog/changes/V124AddSystemSecurityNotificationDataConfig", logicalFilePath = "db/changelog/changes/v1.2.4-add-system-security-notification-data-config.xml")
    include(file = "db/changelog/changes/V126AddDeliveryPushConfigParams", logicalFilePath = "db/changelog/changes/v1.2.6-add-delivery-push-config-params.xml")
    include(file = "db/changelog/changes/V150AddNotificationTables", logicalFilePath = "db/changelog/changes/v1.5.0-add-notification-tables.xml")
    include(file = "db/changelog/changes/V160ConvertAddressCoordsToWgs84", logicalFilePath = "db/changelog/changes/v1.6.0-convert-address-coords-to-wgs84.xml")
    include(file = "db/changelog/changes/V170AddProductOptions", logicalFilePath = "db/changelog/changes/v1.7.0-add-product-options.xml")
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
    // 消息发送补齐:优惠过期提醒窗口/配送超时阈值参数(模板上下文列已并入 v1.5.0 aqua_notifications 建表)
    include(file = "db/changelog/changes/V260AddNotificationSendSupport", logicalFilePath = "db/changelog/changes/v2.6.0-add-notification-send-support.xml")
    // 营收口径修正:已完成营收按完成时间(completed_at)归属日的日事实表(订单数仍按下单日)
    include(file = "db/changelog/changes/V280AddDailyRevenueFacts", logicalFilePath = "db/changelog/changes/v2.8.0-add-daily-revenue-facts.xml")
    // B2 缺陷 #9:aqua_orders.assigned_at 存量回填(列已并入 v1.0.1 aqua_orders 建表)
    include(file = "db/changelog/changes/V344AddOrdersAssignedAt", logicalFilePath = "db/changelog/changes/v3.4.4-add-orders-assigned-at.xml")
    // B2 配送费分档(结构 #6,R-MON-02):默认三档回填(tiers 列已并入 v1.0.7 建表)
    include(file = "db/changelog/changes/V345AddDeliveryFeeTiers", logicalFilePath = "db/changelog/changes/v3.4.5-add-delivery-fee-tiers.xml")
    // B2 押金模型回退(结构 #4,R-MON-03):aqua_deposit_records 状态值域迁移 + 退押申请/空桶回收表(扩列已并入 v2.0.2 建表)
    include(file = "db/changelog/changes/V346AddDepositModel", logicalFilePath = "db/changelog/changes/v3.4.6-add-deposit-model.xml")
    // G77/F-ORD-026:周期订购计划 + 明细表(后台入口,到期自动下单+提醒送水工)
    include(file = "db/changelog/changes/V347AddSubscriptions", logicalFilePath = "db/changelog/changes/v3.4.7-add-subscriptions.xml")
    // G94 接单超时预警(F-NTF-008/R-NTF-07):F-SYS-002 派单与确认组新增两阈值参数种子
    include(file = "db/changelog/changes/V347AddOrderAcceptTimeoutParams", logicalFilePath = "db/changelog/changes/v3.4.7-add-order-accept-timeout-params.xml")
  })
