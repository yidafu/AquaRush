// 转换自 db/changelog/db.changelog-master.xml (liquibase-kotlin 4.33.0-0.10.1)
// compiled changelog 主入口：spring.liquibase.change-log = db/changelog/DbChangelogMaster
// 路径即类名（KotlinCompiledLiquibaseChangeLogParser 用 physicalFilePath 反射加载本类）。
package db.changelog

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class DbChangelogMaster :
  KotlinCompiledDatabaseChangeLog({
    // Development schema recreation - drops and recreates all tables
    include(file = "db/changelog/changes/V100InitComprehensiveSchema")
    include(file = "db/changelog/changes/V103AddOrderSearchContent")
    include(file = "db/changelog/changes/V104AddDailyCollectionTables")
    include(file = "db/changelog/changes/V105AddCalculatedFields")
    include(file = "db/changelog/changes/V106AddOpsConfigTables")
    include(file = "db/changelog/changes/V107AddOpsNoticeStoreDeliveryFee")
    include(file = "db/changelog/changes/V108AddSystemConfigParams")
    include(file = "db/changelog/changes/V109AddMarketingTables")
    include(file = "db/changelog/changes/V110AddMarketingFlashGiftFirstorder")
    include(file = "db/changelog/changes/V111AddBusinessLogOperation")
    include(file = "db/changelog/changes/V120DropOrderSingleProductColumns")
    include(file = "db/changelog/changes/V121MergeDeliveryWorkerIntoAdmins")
    include(file = "db/changelog/changes/V122AddAdminAccountStatus")
    include(file = "db/changelog/changes/V123AddDeliveryTrajectory")
    include(file = "db/changelog/changes/V124AddSystemSecurityNotificationDataConfig")
    include(file = "db/changelog/changes/V125AddOrderIdempotencyDeliveryTime")
    include(file = "db/changelog/changes/V126AddDeliveryPushConfigParams")
    include(file = "db/changelog/changes/V130LogPartitionBackup")
    include(file = "db/changelog/changes/V140FixPaymentRefundsStatusCheck")
    include(file = "db/changelog/changes/V150AddNotificationTables")
    include(file = "db/changelog/changes/V160ConvertAddressCoordsToWgs84")
    include(file = "db/changelog/changes/V170AddProductOptions")
    include(file = "db/changelog/changes/V180DropProductDeliverySettings")
    include(file = "db/changelog/changes/V190AddOrderTicketDiscountSnapshot")
    // G23 照片核验 / G28 争议日报驳回留痕
    include(file = "db/changelog/changes/V200AddReviewColumns")
    // R-PAY-02 退款审核流
    include(file = "db/changelog/changes/V201AddRefundReviewColumns")
    // G06 桶押金模块
    include(file = "db/changelog/changes/V202AddDeposit")
    // G06 水桶费改造(押金 → 一次性塑料水桶费:价目表/购桶单/明细)
    include(file = "db/changelog/changes/V210AddBarrelFee")
    // 南山牌桶装水演示数据:位于全部 schema 变更之后,可引用最终 schema;重建数据库后生效
    include(file = "db/changelog/changes/V170SeedNanshanDemoData")
    // F-MKT-007 水票唯一递增票号(SP+年月+8位序号,按月重置):置于 seed 之后以回填种子数据
    include(file = "db/changelog/changes/V220AddUserTicketNo")
    // 数据修正:seed 将 2 笔订单 payment_method 写成 CASH_ON_DELIVERY,不在域枚举 PaymentMethod(WECHAT_PAY/CASH)中,导致 orderStatistics 的 findAll() 实体映射崩溃 → 归一为 CASH(PRD 04-order.md §R-PAY-03)
    include(file = "db/changelog/changes/V230FixOrderPaymentMethod")
    // 第 3 层历史趋势指标:原子日汇总中间表(夜间快照任务写入,查询侧两段合并)
    include(file = "db/changelog/changes/V240AddDailyStatFacts")
    // F-STA-007 营销统计:营销日事实 + 营销使用日事实(同一夜间快照任务写入)
    include(file = "db/changelog/changes/V250AddMarketingDailyFacts")
    // 消息发送补齐:notifications 模板上下文列(失败消息手动重发) + 优惠过期提醒窗口/配送超时阈值参数
    include(file = "db/changelog/changes/V260AddNotificationSendSupport")
    // R-ORD-04 支付超时优化:payments.version 乐观锁列 + chk_payments_status 对齐枚举
    include(file = "db/changelog/changes/V270AddPaymentExpiryOptimization")
    // 营收口径修正:已完成营收按完成时间(completed_at)归属日的日事实表(订单数仍按下单日)
    include(file = "db/changelog/changes/V280AddDailyRevenueFacts")
    // G23/F-ORD-008/F-ORD-024:chk_operation_type_valid 对齐域枚举(照片核验/催单/改址操作类型)
    include(file = "db/changelog/changes/V290FixOrderOperationTypeCheck")
  })
