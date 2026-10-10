// 由 generate.py 生成 — 勿手改 (数据源: plan_fixed.json / enums_fixed.json / defs.json)

val defWhens: Map<String, Map<String, String>> = mapOf(
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/mapper/BarrelFeeModelMapper.kt" to mapOf(
    "DomainBarrelFeeStatus|toGraphQL" to "when (this) {\n    DomainBarrelFeeStatus.ENABLED -> BarrelFeeStatus.ENABLED\n    DomainBarrelFeeStatus.DISABLED -> BarrelFeeStatus.DISABLED\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/coupon/mapper/CouponMapper.kt" to mapOf(
    "CouponIssuanceType|toDomain" to "when (this) {\n    CouponIssuanceType.AUTO_CLAIM -> DomainCouponIssuanceType.AUTO_CLAIM\n    CouponIssuanceType.TARGETED -> DomainCouponIssuanceType.TARGETED\n  }",
    "CouponScope|toDomain" to "when (this) {\n    CouponScope.ALL -> DomainCouponScope.ALL\n    CouponScope.PRODUCT -> DomainCouponScope.PRODUCT\n  }",
    "CouponTemplateStatus|toDomain" to "when (this) {\n    CouponTemplateStatus.ACTIVE -> DomainCouponTemplateStatus.ACTIVE\n    CouponTemplateStatus.DISABLED -> DomainCouponTemplateStatus.DISABLED\n  }",
    "CouponType|toDomain" to "when (this) {\n    CouponType.CASH -> DomainCouponType.CASH\n    CouponType.DISCOUNT -> DomainCouponType.DISCOUNT\n    CouponType.NO_THRESHOLD_CASH -> DomainCouponType.NO_THRESHOLD_CASH\n  }",
    "DomainCouponIssuanceType|toGraphQL" to "when (this) {\n    DomainCouponIssuanceType.AUTO_CLAIM -> CouponIssuanceType.AUTO_CLAIM\n    DomainCouponIssuanceType.TARGETED -> CouponIssuanceType.TARGETED\n  }",
    "DomainCouponScope|toGraphQL" to "when (this) {\n    DomainCouponScope.ALL -> CouponScope.ALL\n    DomainCouponScope.PRODUCT -> CouponScope.PRODUCT\n  }",
    "DomainCouponTemplateStatus|toGraphQL" to "when (this) {\n    DomainCouponTemplateStatus.ACTIVE -> CouponTemplateStatus.ACTIVE\n    DomainCouponTemplateStatus.DISABLED -> CouponTemplateStatus.DISABLED\n  }",
    "DomainCouponType|toGraphQL" to "when (this) {\n    DomainCouponType.CASH -> CouponType.CASH\n    DomainCouponType.DISCOUNT -> CouponType.DISCOUNT\n    DomainCouponType.NO_THRESHOLD_CASH -> CouponType.NO_THRESHOLD_CASH\n  }",
    "DomainValidityType|toGraphQL" to "when (this) {\n    DomainValidityType.FIXED_RANGE -> ValidityType.FIXED_RANGE\n    DomainValidityType.DAYS_AFTER_CLAIM -> ValidityType.DAYS_AFTER_CLAIM\n    DomainValidityType.DAYS_AFTER_PURCHASE -> ValidityType.DAYS_AFTER_PURCHASE\n  }",
    "IssueTargetType|toDomain" to "when (this) {\n    IssueTargetType.GROUP -> DomainIssueTargetType.GROUP\n    IssueTargetType.USER -> DomainIssueTargetType.USER\n  }",
    "ValidityType|toDomain" to "when (this) {\n    ValidityType.DAYS_AFTER_CLAIM -> DomainValidityType.DAYS_AFTER_CLAIM\n    ValidityType.DAYS_AFTER_PURCHASE -> DomainValidityType.DAYS_AFTER_PURCHASE\n    ValidityType.FIXED_RANGE -> DomainValidityType.FIXED_RANGE\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/firstorder/mapper/FirstOrderActivityMapper.kt" to mapOf(
    "DomainFirstOrderActivityType|toGraphQL" to "when (this) {\n    DomainFirstOrderActivityType.FIRST_ORDER_REDUCE -> FirstOrderActivityType.FIRST_ORDER_REDUCE\n    DomainFirstOrderActivityType.NEWCOMER_PRICE -> FirstOrderActivityType.NEWCOMER_PRICE\n  }",
    "DomainPromotionActivityStatus|toGraphQL" to "when (this) {\n    DomainPromotionActivityStatus.ACTIVE -> PromotionActivityStatus.ACTIVE\n    DomainPromotionActivityStatus.DISABLED -> PromotionActivityStatus.DISABLED\n  }",
    "DomainPromotionScope|toGraphQL" to "when (this) {\n    DomainPromotionScope.ALL -> PromotionScope.ALL\n    DomainPromotionScope.PRODUCT -> PromotionScope.PRODUCT\n    DomainPromotionScope.GROUP -> PromotionScope.GROUP\n  }",
    "FirstOrderActivityType|toDomain" to "when (this) {\n    FirstOrderActivityType.FIRST_ORDER_REDUCE -> DomainFirstOrderActivityType.FIRST_ORDER_REDUCE\n    FirstOrderActivityType.NEWCOMER_PRICE -> DomainFirstOrderActivityType.NEWCOMER_PRICE\n  }",
    "PromotionScope|toDomain" to "when (this) {\n    PromotionScope.ALL -> DomainPromotionScope.ALL\n    PromotionScope.GROUP -> DomainPromotionScope.GROUP\n    PromotionScope.PRODUCT -> DomainPromotionScope.PRODUCT\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/flashsale/mapper/FlashSaleActivityMapper.kt" to mapOf(
    "DomainPromotionActivityStatus|toGraphQL" to "when (this) {\n    DomainPromotionActivityStatus.ACTIVE -> PromotionActivityStatus.ACTIVE\n    DomainPromotionActivityStatus.DISABLED -> PromotionActivityStatus.DISABLED\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/gift/mapper/GiftActivityMapper.kt" to mapOf(
    "DomainGiftActivityType|toGraphQL" to "when (this) {\n    DomainGiftActivityType.FULL_GIFT -> GiftActivityType.FULL_GIFT\n    DomainGiftActivityType.BUY_GIFT -> GiftActivityType.BUY_GIFT\n  }",
    "DomainPromotionActivityStatus|toGraphQL" to "when (this) {\n    DomainPromotionActivityStatus.ACTIVE -> PromotionActivityStatus.ACTIVE\n    DomainPromotionActivityStatus.DISABLED -> PromotionActivityStatus.DISABLED\n  }",
    "DomainPromotionScope|toGraphQL" to "when (this) {\n    DomainPromotionScope.ALL -> PromotionScope.ALL\n    DomainPromotionScope.PRODUCT -> PromotionScope.PRODUCT\n    DomainPromotionScope.GROUP -> PromotionScope.GROUP\n  }",
    "GiftActivityType|toDomain" to "when (this) {\n    GiftActivityType.BUY_GIFT -> DomainGiftActivityType.BUY_GIFT\n    GiftActivityType.FULL_GIFT -> DomainGiftActivityType.FULL_GIFT\n  }",
    "PromotionScope|toDomain" to "when (this) {\n    PromotionScope.ALL -> DomainPromotionScope.ALL\n    PromotionScope.GROUP -> DomainPromotionScope.GROUP\n    PromotionScope.PRODUCT -> DomainPromotionScope.PRODUCT\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/promotion/mapper/PromotionActivityMapper.kt" to mapOf(
    "DomainPromotionActivityStatus|toGraphQL" to "when (this) {\n    DomainPromotionActivityStatus.ACTIVE -> PromotionActivityStatus.ACTIVE\n    DomainPromotionActivityStatus.DISABLED -> PromotionActivityStatus.DISABLED\n  }",
    "DomainPromotionActivityType|toGraphQL" to "when (this) {\n    DomainPromotionActivityType.FULL_REDUCE -> PromotionActivityType.FULL_REDUCE\n    DomainPromotionActivityType.FULL_DISCOUNT -> PromotionActivityType.FULL_DISCOUNT\n  }",
    "DomainPromotionScope|toGraphQL" to "when (this) {\n    DomainPromotionScope.ALL -> PromotionScope.ALL\n    DomainPromotionScope.PRODUCT -> PromotionScope.PRODUCT\n    DomainPromotionScope.GROUP -> PromotionScope.GROUP\n  }",
    "PromotionActivityType|toDomain" to "when (this) {\n    PromotionActivityType.FULL_DISCOUNT -> DomainPromotionActivityType.FULL_DISCOUNT\n    PromotionActivityType.FULL_REDUCE -> DomainPromotionActivityType.FULL_REDUCE\n  }",
    "PromotionScope|toDomain" to "when (this) {\n    PromotionScope.ALL -> DomainPromotionScope.ALL\n    PromotionScope.GROUP -> DomainPromotionScope.GROUP\n    PromotionScope.PRODUCT -> DomainPromotionScope.PRODUCT\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/ticket/mapper/TicketMapper.kt" to mapOf(
    "DomainTicketTypeKind|toGraphQL" to "when (this) {\n    DomainTicketTypeKind.SPECIFIC_PRODUCT -> TicketTypeKind.SPECIFIC_PRODUCT\n    DomainTicketTypeKind.UNIVERSAL -> TicketTypeKind.UNIVERSAL\n  }",
    "DomainTicketTypeStatus|toGraphQL" to "when (this) {\n    DomainTicketTypeStatus.ACTIVE -> TicketTypeStatus.ACTIVE\n    DomainTicketTypeStatus.DISABLED -> TicketTypeStatus.DISABLED\n  }",
    "DomainValidityType|toGraphQL" to "when (this) {\n    DomainValidityType.FIXED_RANGE -> ValidityType.FIXED_RANGE\n    DomainValidityType.DAYS_AFTER_CLAIM -> ValidityType.DAYS_AFTER_CLAIM\n    DomainValidityType.DAYS_AFTER_PURCHASE -> ValidityType.DAYS_AFTER_PURCHASE\n  }",
    "TicketTypeKind|toDomain" to "when (this) {\n    TicketTypeKind.SPECIFIC_PRODUCT -> DomainTicketTypeKind.SPECIFIC_PRODUCT\n    TicketTypeKind.UNIVERSAL -> DomainTicketTypeKind.UNIVERSAL\n  }",
    "TicketTypeStatus|toDomain" to "when (this) {\n    TicketTypeStatus.ACTIVE -> DomainTicketTypeStatus.ACTIVE\n    TicketTypeStatus.DISABLED -> DomainTicketTypeStatus.DISABLED\n  }",
    "ValidityType|toDomain" to "when (this) {\n    ValidityType.DAYS_AFTER_CLAIM -> DomainValidityType.DAYS_AFTER_CLAIM\n    ValidityType.DAYS_AFTER_PURCHASE -> DomainValidityType.DAYS_AFTER_PURCHASE\n    ValidityType.FIXED_RANGE -> DomainValidityType.FIXED_RANGE\n  }",
  ),
  "modules/aqua-ops/src/main/kotlin/dev/yidafu/aqua/ops/banner/mapper/BannerMapper.kt" to mapOf(
    "BannerLinkType|toDomain" to "when (this) {\n    BannerLinkType.CUSTOM_PAGE -> DomainBannerLinkType.CUSTOM_PAGE\n    BannerLinkType.PRODUCT -> DomainBannerLinkType.PRODUCT\n  }",
    "DomainBannerLinkType|toGraphQL" to "when (this) {\n    DomainBannerLinkType.PRODUCT -> BannerLinkType.PRODUCT\n    DomainBannerLinkType.CUSTOM_PAGE -> BannerLinkType.CUSTOM_PAGE\n  }",
  ),
  "modules/aqua-ops/src/main/kotlin/dev/yidafu/aqua/ops/feedback/mapper/FeedbackMapper.kt" to mapOf(
    "DomainFeedbackStatus|toGraphQL" to "when (this) {\n    DomainFeedbackStatus.PENDING -> FeedbackStatus.PENDING\n    DomainFeedbackStatus.PROCESSING -> FeedbackStatus.PROCESSING\n    DomainFeedbackStatus.RESOLVED -> FeedbackStatus.RESOLVED\n  }",
    "DomainFeedbackType|toGraphQL" to "when (this) {\n    DomainFeedbackType.PRODUCT_QUALITY -> FeedbackType.PRODUCT_QUALITY\n    DomainFeedbackType.DELIVERY_SERVICE -> FeedbackType.DELIVERY_SERVICE\n    DomainFeedbackType.APP_USAGE -> FeedbackType.APP_USAGE\n    DomainFeedbackType.SUGGESTION -> FeedbackType.SUGGESTION\n    DomainFeedbackType.COMPLAINT -> FeedbackType.COMPLAINT\n    DomainFeedbackType.OTHER -> FeedbackType.OTHER\n  }",
    "FeedbackStatus|toDomain" to "when (this) {\n    FeedbackStatus.PENDING -> DomainFeedbackStatus.PENDING\n    FeedbackStatus.PROCESSING -> DomainFeedbackStatus.PROCESSING\n    FeedbackStatus.RESOLVED -> DomainFeedbackStatus.RESOLVED\n  }",
    "FeedbackType|toDomain" to "when (this) {\n    FeedbackType.APP_USAGE -> DomainFeedbackType.APP_USAGE\n    FeedbackType.COMPLAINT -> DomainFeedbackType.COMPLAINT\n    FeedbackType.DELIVERY_SERVICE -> DomainFeedbackType.DELIVERY_SERVICE\n    FeedbackType.OTHER -> DomainFeedbackType.OTHER\n    FeedbackType.PRODUCT_QUALITY -> DomainFeedbackType.PRODUCT_QUALITY\n    FeedbackType.SUGGESTION -> DomainFeedbackType.SUGGESTION\n  }",
  ),
  "modules/aqua-ops/src/main/kotlin/dev/yidafu/aqua/ops/notice/mapper/NoticeMapper.kt" to mapOf(
    "DomainNoticeStatus|toGraphQL" to "when (this) {\n    DomainNoticeStatus.DRAFT -> NoticeStatus.DRAFT\n    DomainNoticeStatus.PUBLISHED -> NoticeStatus.PUBLISHED\n    DomainNoticeStatus.STOPPED -> NoticeStatus.STOPPED\n  }",
    "DomainNoticeType|toGraphQL" to "when (this) {\n    DomainNoticeType.PLATFORM_NOTICE -> NoticeType.PLATFORM_NOTICE\n    DomainNoticeType.WATER_OUTAGE -> NoticeType.WATER_OUTAGE\n    DomainNoticeType.ACTIVITY_PREVIEW -> NoticeType.ACTIVITY_PREVIEW\n  }",
    "NoticeStatus|toDomain" to "when (this) {\n    NoticeStatus.DRAFT -> DomainNoticeStatus.DRAFT\n    NoticeStatus.PUBLISHED -> DomainNoticeStatus.PUBLISHED\n    NoticeStatus.STOPPED -> DomainNoticeStatus.STOPPED\n  }",
    "NoticeType|toDomain" to "when (this) {\n    NoticeType.ACTIVITY_PREVIEW -> DomainNoticeType.ACTIVITY_PREVIEW\n    NoticeType.PLATFORM_NOTICE -> DomainNoticeType.PLATFORM_NOTICE\n    NoticeType.WATER_OUTAGE -> DomainNoticeType.WATER_OUTAGE\n  }",
  ),
  "modules/aqua-ops/src/main/kotlin/dev/yidafu/aqua/ops/systemconfig/mapper/SystemConfigMapper.kt" to mapOf(
    "DomainSystemSettingGroup|toGraphQL" to "when (this) {\n    DomainSystemSettingGroup.ORDER_AND_FULFILLMENT -> SystemConfigGroup.ORDER_AND_FULFILLMENT\n    DomainSystemSettingGroup.INVENTORY -> SystemConfigGroup.INVENTORY\n    DomainSystemSettingGroup.SECURITY -> SystemConfigGroup.SECURITY\n    DomainSystemSettingGroup.NOTIFICATION -> SystemConfigGroup.NOTIFICATION\n    DomainSystemSettingGroup.DATA_MAINTENANCE -> SystemConfigGroup.DATA_MAINTENANCE\n  }",
    "DomainSystemSettingType|toGraphQL" to "when (this) {\n    DomainSystemSettingType.NUMBER -> SystemSettingType.NUMBER\n    DomainSystemSettingType.BOOLEAN -> SystemSettingType.BOOLEAN\n    DomainSystemSettingType.STRING -> SystemSettingType.STRING\n    DomainSystemSettingType.ENUM -> SystemSettingType.ENUM\n  }",
    "SystemConfigGroup|toDomain" to "when (this) {\n    SystemConfigGroup.DATA_MAINTENANCE -> DomainSystemSettingGroup.DATA_MAINTENANCE\n    SystemConfigGroup.INVENTORY -> DomainSystemSettingGroup.INVENTORY\n    SystemConfigGroup.NOTIFICATION -> DomainSystemSettingGroup.NOTIFICATION\n    SystemConfigGroup.ORDER_AND_FULFILLMENT -> DomainSystemSettingGroup.ORDER_AND_FULFILLMENT\n    SystemConfigGroup.SECURITY -> DomainSystemSettingGroup.SECURITY\n  }",
  ),
  "modules/aqua-product/src/main/kotlin/dev/yidafu/aqua/product/mapper/ProductMapper.kt" to mapOf(
    "ProductModelStatus|toGraphQL" to "when (this) {\n    ProductModelStatus.OFFLINE -> ProductStatus.OFFLINE\n    ProductModelStatus.ONLINE -> ProductStatus.ONLINE\n    ProductModelStatus.OUT_OF_STOCK -> ProductStatus.OUT_OF_STOCK\n  }",
    "ProductStatus|toDomain" to "when (this) {\n    ProductStatus.OFFLINE -> ProductModelStatus.OFFLINE\n    ProductStatus.ONLINE -> ProductModelStatus.ONLINE\n    ProductStatus.OUT_OF_STOCK -> ProductModelStatus.OUT_OF_STOCK\n  }",
  ),
  "modules/aqua-product/src/main/kotlin/dev/yidafu/aqua/product/mapper/ProductOptionMapper.kt" to mapOf(
    "DomainProductOptionType|toGraphQL" to "when (this) {\n    DomainProductOptionType.SPECIFICATION -> ProductOptionType.SPECIFICATION\n    DomainProductOptionType.WATER_SOURCE -> ProductOptionType.WATER_SOURCE\n  }",
    "ProductOptionType|toDomain" to "when (this) {\n    ProductOptionType.SPECIFICATION -> DomainProductOptionType.SPECIFICATION\n    ProductOptionType.WATER_SOURCE -> DomainProductOptionType.WATER_SOURCE\n  }",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/delivery/mapper/DeliveryMappers.kt" to mapOf(
    "DeliverWorkerModelStatus|toGraphQL" to "when (this) {\n    DeliverWorkerModelStatus.ONLINE -> DeliveryWorkerStatus.ONLINE\n    DeliverWorkerModelStatus.OFFLINE -> DeliveryWorkerStatus.OFFLINE\n  }",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/mapper/OrderMappers.kt" to mapOf(
    "OrderModelStatus|toGraphQL" to "when (this) {\n    OrderModelStatus.PENDING_PAYMENT -> OrderStatusG.PENDING_PAYMENT\n    OrderModelStatus.PENDING_DISPATCH -> OrderStatusG.PENDING_DISPATCH\n    OrderModelStatus.PENDING_DELIVERY -> OrderStatusG.PENDING_DELIVERY\n    OrderModelStatus.DELIVERING -> OrderStatusG.DELIVERING\n    OrderModelStatus.AWAITING_CONFIRMATION -> OrderStatusG.AWAITING_CONFIRMATION\n    OrderModelStatus.COMPLETED -> OrderStatusG.COMPLETED\n    OrderModelStatus.CANCELLED -> OrderStatusG.CANCELLED\n    OrderModelStatus.REFUNDED -> OrderStatusG.REFUNDED\n  }",
    "OrderStatusG|toDomain" to "when (this) {\n    OrderStatusG.AWAITING_CONFIRMATION -> OrderModelStatus.AWAITING_CONFIRMATION\n    OrderStatusG.CANCELLED -> OrderModelStatus.CANCELLED\n    OrderStatusG.COMPLETED -> OrderModelStatus.COMPLETED\n    OrderStatusG.DELIVERING -> OrderModelStatus.DELIVERING\n    OrderStatusG.PENDING_DELIVERY -> OrderModelStatus.PENDING_DELIVERY\n    OrderStatusG.PENDING_DISPATCH -> OrderModelStatus.PENDING_DISPATCH\n    OrderStatusG.PENDING_PAYMENT -> OrderModelStatus.PENDING_PAYMENT\n    OrderStatusG.REFUNDED -> OrderModelStatus.REFUNDED\n  }",
    "PaymentType|toGraphQL" to "when (this) {\n    PaymentType.WATER_TICKET -> PaymentTypeG.WATER_TICKET\n    PaymentType.CASH -> PaymentTypeG.CASH\n    PaymentType.QR_CODE -> PaymentTypeG.QR_CODE\n  }",
  ),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/mapper/UserMappers.kt" to mapOf(
    "AdminRole|toDomain" to "when (this) {\n    AdminRole.ADMIN -> AdminRoleModel.ADMIN\n    AdminRole.DELIVERY_WORKER -> AdminRoleModel.DELIVERY_WORKER\n    AdminRole.SUPER_ADMIN -> AdminRoleModel.SUPER_ADMIN\n  }",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/marketing/resolvers/AdminUserCouponTicketQueryResolver.kt" to mapOf(
    "GraphQLUserCouponStatus|toDomain" to "when (this) {\n    GraphQLUserCouponStatus.EXPIRED -> DomainUserCouponStatus.EXPIRED\n    GraphQLUserCouponStatus.RECEIVED -> DomainUserCouponStatus.RECEIVED\n    GraphQLUserCouponStatus.RETURNED -> DomainUserCouponStatus.RETURNED\n    GraphQLUserCouponStatus.USED -> DomainUserCouponStatus.USED\n  }",
    "GraphQLUserTicketStatus|toDomain" to "when (this) {\n    GraphQLUserTicketStatus.EXPIRED -> DomainUserTicketStatus.EXPIRED\n    GraphQLUserTicketStatus.RETURNED -> DomainUserTicketStatus.RETURNED\n    GraphQLUserTicketStatus.UNUSED -> DomainUserTicketStatus.UNUSED\n    GraphQLUserTicketStatus.USED -> DomainUserTicketStatus.USED\n  }",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/system/resolvers/AdminDataMaintenanceQueryResolver.kt" to mapOf(
    "LogTableType|toDomain" to "when (this) {\n    LogTableType.API_LOG -> DomainLogTableType.API_LOG\n    LogTableType.BUSINESS_LOG -> DomainLogTableType.BUSINESS_LOG\n    LogTableType.USER_ACTION_LOG -> DomainLogTableType.USER_ACTION_LOG\n  }",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/marketing/mapper/ClientMarketingMapper.kt" to mapOf(
    "DomainCouponScope|toGraphQL" to "when (this) {\n    DomainCouponScope.ALL -> GraphQLCouponScope.ALL\n    DomainCouponScope.PRODUCT -> GraphQLCouponScope.PRODUCT\n  }",
    "DomainCouponType|toGraphQL" to "when (this) {\n    DomainCouponType.CASH -> GraphQLCouponType.CASH\n    DomainCouponType.DISCOUNT -> GraphQLCouponType.DISCOUNT\n    DomainCouponType.NO_THRESHOLD_CASH -> GraphQLCouponType.NO_THRESHOLD_CASH\n  }",
    "DomainPriceSource|toGraphQL" to "when (this) {\n    DomainPriceSource.CATALOG_PRICE -> GraphQLPriceSource.CATALOG_PRICE\n    DomainPriceSource.GROUP_PRICE -> GraphQLPriceSource.GROUP_PRICE\n    DomainPriceSource.FLASH_SALE_PRICE -> GraphQLPriceSource.FLASH_SALE_PRICE\n    DomainPriceSource.NEWCOMER_PRICE -> GraphQLPriceSource.NEWCOMER_PRICE\n    DomainPriceSource.MANUAL -> GraphQLPriceSource.MANUAL\n  }",
    "DomainTicketPurchaseStatus|toGraphQL" to "when (this) {\n    DomainTicketPurchaseStatus.PENDING -> GraphQLTicketPurchaseStatus.PENDING\n    DomainTicketPurchaseStatus.PAID -> GraphQLTicketPurchaseStatus.PAID\n    DomainTicketPurchaseStatus.FAILED -> GraphQLTicketPurchaseStatus.FAILED\n    DomainTicketPurchaseStatus.CANCELLED -> GraphQLTicketPurchaseStatus.CANCELLED\n  }",
    "DomainTicketTypeKind|toGraphQL" to "when (this) {\n    DomainTicketTypeKind.SPECIFIC_PRODUCT -> GraphQLTicketTypeKind.SPECIFIC_PRODUCT\n    DomainTicketTypeKind.UNIVERSAL -> GraphQLTicketTypeKind.UNIVERSAL\n  }",
    "DomainUserCouponStatus|toGraphQL" to "when (this) {\n    DomainUserCouponStatus.RECEIVED -> GraphQLUserCouponStatus.RECEIVED\n    DomainUserCouponStatus.USED -> GraphQLUserCouponStatus.USED\n    DomainUserCouponStatus.RETURNED -> GraphQLUserCouponStatus.RETURNED\n    DomainUserCouponStatus.EXPIRED -> GraphQLUserCouponStatus.EXPIRED\n  }",
    "DomainUserTicketStatus|toGraphQL" to "when (this) {\n    DomainUserTicketStatus.UNUSED -> GraphQLUserTicketStatus.UNUSED\n    DomainUserTicketStatus.USED -> GraphQLUserTicketStatus.USED\n    DomainUserTicketStatus.RETURNED -> GraphQLUserTicketStatus.RETURNED\n    DomainUserTicketStatus.EXPIRED -> GraphQLUserTicketStatus.EXPIRED\n  }",
    "DomainValidityType|toGraphQL" to "when (this) {\n    DomainValidityType.FIXED_RANGE -> GraphQLValidityType.FIXED_RANGE\n    DomainValidityType.DAYS_AFTER_CLAIM -> GraphQLValidityType.DAYS_AFTER_CLAIM\n    DomainValidityType.DAYS_AFTER_PURCHASE -> GraphQLValidityType.DAYS_AFTER_PURCHASE\n  }",
    "GraphQLUserCouponStatus|toDomain" to "when (this) {\n    GraphQLUserCouponStatus.EXPIRED -> DomainUserCouponStatus.EXPIRED\n    GraphQLUserCouponStatus.RECEIVED -> DomainUserCouponStatus.RECEIVED\n    GraphQLUserCouponStatus.RETURNED -> DomainUserCouponStatus.RETURNED\n    GraphQLUserCouponStatus.USED -> DomainUserCouponStatus.USED\n  }",
    "GraphQLUserTicketStatus|toDomain" to "when (this) {\n    GraphQLUserTicketStatus.EXPIRED -> DomainUserTicketStatus.EXPIRED\n    GraphQLUserTicketStatus.RETURNED -> DomainUserTicketStatus.RETURNED\n    GraphQLUserTicketStatus.UNUSED -> DomainUserTicketStatus.UNUSED\n    GraphQLUserTicketStatus.USED -> DomainUserTicketStatus.USED\n  }",
  ),
)

val inlineRepls: Map<String, Map<String, String>> = mapOf(
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/mapper/DailyCollectionMappers.kt" to mapOf(
    "DailyCollectionStatus|it.name" to "it.toGraphQL()",
    "DailyCollectionStatus|status.name" to "status.toGraphQL()",
    "DailyReconciliationStatus|status.name" to "status.toGraphQL()",
  ),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/mapper/ReconciliationMappers.kt" to mapOf(
    "DiscrepancyStatus|status.name" to "status.toGraphQL()",
    "DiscrepancyType|discrepancyType.name" to "discrepancyType.toGraphQL()",
    "ReconciliationTaskStatus|status.name" to "status.toGraphQL()",
    "ReconciliationTaskType|taskType.name" to "taskType.toGraphQL()",
    "SourceSystem|sourceSystem.name" to "sourceSystem.toGraphQL()",
  ),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/resolvers/AdminDailyCollectionQueryResolver.kt" to mapOf(
    "OrderStatus|order.status.name" to "order.status.toGraphQL()",
    "PaymentType|it.name" to "it.toGraphQL()",
  ),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/resolvers/AdminReconciliationQueryResolver.kt" to mapOf(
    "dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus|it.name" to "it.toDomain()",
    "dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus|it.name" to "it.toDomain()",
    "dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType|it.name" to "it.toDomain()",
  ),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/partition/mapper/LogPartitionMapper.kt" to mapOf(
    "LogBackupStatus|status.name" to "status.toGraphQL()",
    "LogTableType|logTable.name" to "logTable.toGraphQL()",
    "LogTableType|partition.logTable.name" to "partition.logTable.toGraphQL()",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/mapper/BarrelPurchaseModelMapper.kt" to mapOf(
    "BarrelPaymentStatus|model.paymentStatus.name" to "model.paymentStatus.toGraphQL()",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/resolvers/AdminBarrelFeeQueryResolver.kt" to mapOf(
    "BarrelFeeStatusModel|it.name" to "it.toDomain()",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/resolvers/AdminBarrelPurchaseQueryResolver.kt" to mapOf(
    "BarrelPaymentStatusModel|it.name" to "it.toDomain()",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/delivery/mapper/DeliveryMappers.kt" to mapOf(
    "AdminAccountStatus|accountStatus.name" to "accountStatus.toGraphQL()",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/mapper/OrderMappers.kt" to mapOf(
    "DeliveryPhotoReviewStatusG|it.name" to "it.toGraphQL()",
    "DeliveryTimeSlotG|it.name" to "it.toGraphQL()",
    "DomainDeliveryTimeSlot|it.name" to "it.toDomain()",
    "OperatorTypeG|operatorType.name" to "operatorType.toGraphQL()",
    "OrderOperationTypeG|operationType.name" to "operationType.toGraphQL()",
  ),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/mapper/UserMappers.kt" to mapOf(
    "AdminAccountStatus|accountStatus.name" to "accountStatus.toGraphQL()",
    "DeliveryWorkerStatus|onlineStatus.name" to "onlineStatus.toGraphQL()",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/marketing/resolvers/AdminUserCouponTicketQueryResolver.kt" to mapOf(
    "GraphQLCouponScope|scope.name" to "scope.toGraphQL()",
    "GraphQLCouponType|couponType.name" to "couponType.toGraphQL()",
    "GraphQLUserCouponStatus|status.name" to "status.toGraphQL()",
    "GraphQLUserTicketStatus|status.name" to "status.toGraphQL()",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/ops/resolvers/AdminBannerMutationResolver.kt" to mapOf(
    "dev.yidafu.aqua.api.dto.ReorderDirection|direction.name" to "direction.toDomain()",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/statistics/resolvers/AdminMarketingStatisticsQueryResolver.kt" to mapOf(
    "MarketingBreakdownKind|row.kind.name" to "row.kind.toGraphQL()",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/statistics/resolvers/AdminOrderStatisticsQueryResolver.kt" to mapOf(
    "OrderStatus|it.status.name" to "it.status.toGraphQL()",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/barrel/mapper/ClientBarrelMapper.kt" to mapOf(
    "BarrelPaymentStatus|paymentStatus.name" to "paymentStatus.toGraphQL()",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/order/mapper/ClientOrderMapper.kt" to mapOf(
    "DeliveryTimeSlot|it.name" to "it.toGraphQL()",
    "OrderStatus|status.name" to "status.toGraphQL()",
    "PaymentType|it.name" to "it.toGraphQL()",
    "ProductStatus|status.name" to "status.toGraphQL()",
    "dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus|it.name" to "it.toGraphQL()",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/order/resolvers/ClientOrderQueryResolver.kt" to mapOf(
    "OrderModelStatus|it.name" to "it.toDomain()",
  ),
)

val renames: Map<String, Set<String>> = mapOf(
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/domain/repository/ExposedReconciliationRepositoriesImpl.kt" to setOf("DiscrepancyStatus", "DiscrepancyType", "ReconciliationTaskStatus", "ReconciliationTaskType", "SourceSystem"),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/statistics/domain/repository/ProductSalesStatisticsRepositoryImpl.kt" to setOf("ProductModelStatus"),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/statistics/domain/repository/StatisticsOrderRepositoryImpl.kt" to setOf("OrderModelStatus"),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/statistics/service/impl/MarketingStatisticsServiceImpl.kt" to setOf("MarketingBreakdownKind"),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/statistics/service/impl/StatisticsServiceImpl.kt" to setOf("OrderModelStatus"),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/mapper/BusinessLogMapper.kt" to setOf("BizLogLevel"),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/partition/repository/LogBackupRecordRepositoryImpl.kt" to setOf("LogBackupStatus", "LogTableType"),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/repository/ApiLogRepositoryImpl.kt" to setOf("OperationType", "RequestType"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/domain/repository/BarrelFeeRepositoryImpl.kt" to setOf("BarrelFeeStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/domain/repository/BarrelPurchaseRepositoryImpl.kt" to setOf("BarrelPaymentStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/domain/repository/DepositRecordRepositoryImpl.kt" to setOf("DepositStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/coupon/domain/repository/CouponTemplateRepositoryImpl.kt" to setOf("CouponTemplateStatus", "dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType", "dev.yidafu.aqua.common.domain.model.enums.CouponScope", "dev.yidafu.aqua.common.domain.model.enums.CouponType", "dev.yidafu.aqua.common.domain.model.enums.ValidityType"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/coupon/domain/repository/UserCouponRepositoryImpl.kt" to setOf("CouponScope", "CouponType", "UserCouponStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/firstorder/domain/repository/FirstOrderActivityRepositoryImpl.kt" to setOf("FirstOrderActivityType", "PromotionActivityStatus", "PromotionScope"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/flashsale/domain/repository/FlashSaleActivityRepositoryImpl.kt" to setOf("PromotionActivityStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/gift/domain/repository/GiftActivityRepositoryImpl.kt" to setOf("GiftActivityType", "PromotionActivityStatus", "PromotionScope"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/promotion/domain/repository/PromotionActivityRepositoryImpl.kt" to setOf("PromotionActivityStatus", "dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType", "dev.yidafu.aqua.common.domain.model.enums.PromotionScope"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/ticket/domain/repository/TicketPurchaseRepositoryImpl.kt" to setOf("TicketPurchaseStatus"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/ticket/domain/repository/TicketTypeRepositoryImpl.kt" to setOf("TicketTypeKind", "TicketTypeStatus", "ValidityType"),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/ticket/domain/repository/UserTicketRepositoryImpl.kt" to setOf("UserTicketStatus"),
  "modules/aqua-platform/src/main/kotlin/dev/yidafu/aqua/notification/channel/ws/NotificationWsHandler.kt" to setOf("NotificationReceiverType"),
  "modules/aqua-platform/src/main/kotlin/dev/yidafu/aqua/notification/channel/ws/NotificationWsPushConsumer.kt" to setOf("NotificationReceiverType"),
  "modules/aqua-platform/src/main/kotlin/dev/yidafu/aqua/notification/domain/repository/NotificationRepositoryImpl.kt" to setOf("NotificationChannelStatus", "NotificationMessageType", "NotificationReceiverType"),
  "modules/aqua-platform/src/main/kotlin/dev/yidafu/aqua/notification/gateway/NotificationGatewayService.kt" to setOf("NotificationMessageType", "NotificationReceiverType"),
  "modules/aqua-platform/src/main/kotlin/dev/yidafu/aqua/storage/domain/repository/FileMetadataRepositoryImpl.kt" to setOf("FileType"),
  "modules/aqua-product/src/main/kotlin/dev/yidafu/aqua/product/domain/repository/ProductOptionRepositoryImpl.kt" to setOf("ProductOptionType"),
  "modules/aqua-product/src/main/kotlin/dev/yidafu/aqua/product/domain/repository/ProductRepositoryImpl.kt" to setOf("ProductModelStatus"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/delivery/domain/repository/DailyCollectionRecordRepositoryImpl.kt" to setOf("DailyCollectionStatus"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/delivery/domain/repository/DailyReconciliationRepositoryImpl.kt" to setOf("DailyReconciliationStatus"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/domain/repository/OrderOperationRepositoryImpl.kt" to setOf("OperatorType", "OrderOperationType"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/domain/repository/OrderRepositoryImpl.kt" to setOf("DeliveryPhotoReviewStatus", "DeliveryTimeSlot", "OrderModelStatus", "PaymentMethod", "PaymentType", "PriceSource"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/service/impl/OrderQueryServiceImpl.kt" to setOf("OrderModelStatus"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/payment/domain/repository/PaymentRefundRepositoryImpl.kt" to setOf("RefundStatus", "RefundType"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/payment/domain/repository/PaymentRepositoryImpl.kt" to setOf("PaymentMethod", "PaymentStatus"),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/domain/repository/AdminRepositoryImpl.kt" to setOf("AdminAccountStatus", "AdminRoleModel", "DeliverWorkerModelStatus"),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/domain/repository/UserRepositoryImpl.kt" to setOf("UserStatus", "dev.yidafu.aqua.common.graphql.generated.UserRole"),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/delivery/resolvers/AdminDeliveryWorkerQueryResolver.kt" to setOf("AdminRoleModel"),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/user/resolvers/AdminQueryResolver.kt" to setOf("AdminRoleModel"),
)

val importsToAdd: Map<String, List<String>> = mapOf(
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/resolvers/AdminDailyCollectionQueryResolver.kt" to listOf("import dev.yidafu.aqua.order.mapper.toGraphQL"),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/mapper/BusinessLogMapper.kt" to listOf("import dev.yidafu.aqua.common.graphql.fromValue"),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/payment/domain/repository/PaymentRefundRepositoryImpl.kt" to listOf("import dev.yidafu.aqua.common.graphql.fromValue"),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/domain/repository/UserRepositoryImpl.kt" to listOf("import dev.yidafu.aqua.common.graphql.fromValue"),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/marketing/resolvers/AdminUserCouponTicketQueryResolver.kt" to listOf("import dev.yidafu.aqua.marketing.coupon.mapper.toGraphQL"),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/statistics/resolvers/AdminOrderStatisticsQueryResolver.kt" to listOf("import dev.yidafu.aqua.order.mapper.toGraphQL"),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/order/mapper/ClientOrderMapper.kt" to listOf("import dev.yidafu.aqua.order.mapper.toGraphQL", "import dev.yidafu.aqua.product.mapper.toGraphQL"),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/order/resolvers/ClientOrderQueryResolver.kt" to listOf("import dev.yidafu.aqua.order.mapper.toDomain"),
)

val appends: Map<String, List<String>> = mapOf(
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/mapper/DailyCollectionMappers.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.toGraphQL(): DailyCollectionStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.DRAFT -> DailyCollectionStatus.DRAFT\n    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.SUBMITTED -> DailyCollectionStatus.SUBMITTED\n    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.CONFIRMED -> DailyCollectionStatus.CONFIRMED\n    dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.DISPUTED -> DailyCollectionStatus.DISPUTED\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.toGraphQL(): DailyReconciliationStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.PENDING -> DailyReconciliationStatus.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.MATCHED -> DailyReconciliationStatus.MATCHED\n    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.DISCREPANCY -> DailyReconciliationStatus.DISCREPANCY\n    dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.REVIEWED -> DailyReconciliationStatus.REVIEWED\n  }",
  ),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/mapper/ReconciliationMappers.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.toGraphQL(): ReconciliationTaskType =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.PAYMENT -> ReconciliationTaskType.PAYMENT\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.REFUND -> ReconciliationTaskType.REFUND\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.SETTLEMENT -> ReconciliationTaskType.SETTLEMENT\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.toGraphQL(): ReconciliationTaskStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.PENDING -> ReconciliationTaskStatus.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.RUNNING -> ReconciliationTaskStatus.RUNNING\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SUCCESS -> ReconciliationTaskStatus.SUCCESS\n    dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.FAILED -> ReconciliationTaskStatus.FAILED\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.toGraphQL(): DiscrepancyType =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISSING -> DiscrepancyType.MISSING\n    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISMATCH -> DiscrepancyType.MISMATCH\n    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.EXTRA -> DiscrepancyType.EXTRA\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.SourceSystem.toGraphQL(): SourceSystem =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.SourceSystem.INTERNAL -> SourceSystem.INTERNAL\n    dev.yidafu.aqua.common.domain.model.enums.SourceSystem.WECHAT -> SourceSystem.WECHAT\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.toGraphQL(): DiscrepancyStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.UNRESOLVED -> DiscrepancyStatus.UNRESOLVED\n    dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.RESOLVED -> DiscrepancyStatus.RESOLVED\n  }",
  ),
  "modules/aqua-analytics/src/main/kotlin/dev/yidafu/aqua/analytics/reconciliation/resolvers/AdminReconciliationQueryResolver.kt" to listOf(
    "private fun ReconciliationTaskType.toDomain(): dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType =\n  when (this) {\n    ReconciliationTaskType.PAYMENT -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.PAYMENT\n    ReconciliationTaskType.REFUND -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.REFUND\n    ReconciliationTaskType.SETTLEMENT -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.SETTLEMENT\n  }",
    "private fun ReconciliationTaskStatus.toDomain(): dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus =\n  when (this) {\n    ReconciliationTaskStatus.FAILED -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.FAILED\n    ReconciliationTaskStatus.PENDING -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.PENDING\n    ReconciliationTaskStatus.RUNNING -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.RUNNING\n    ReconciliationTaskStatus.SUCCESS -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SUCCESS\n  }",
    "private fun DiscrepancyStatus.toDomain(): dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus =\n  when (this) {\n    DiscrepancyStatus.RESOLVED -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.RESOLVED\n    DiscrepancyStatus.UNRESOLVED -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.UNRESOLVED\n  }",
  ),
  "modules/aqua-foundation/src/main/kotlin/dev/yidafu/aqua/logging/partition/mapper/LogPartitionMapper.kt" to listOf(
    "private fun dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.toGraphQL(): LogTableType =\n  when (this) {\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.API_LOG -> LogTableType.API_LOG\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.USER_ACTION_LOG -> LogTableType.USER_ACTION_LOG\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType.BUSINESS_LOG -> LogTableType.BUSINESS_LOG\n  }",
    "private fun dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.toGraphQL(): LogBackupStatus =\n  when (this) {\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.RUNNING -> LogBackupStatus.RUNNING\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.SUCCEEDED -> LogBackupStatus.SUCCEEDED\n    dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus.FAILED -> LogBackupStatus.FAILED\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/mapper/BarrelPurchaseModelMapper.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.toGraphQL(): BarrelPaymentStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PENDING -> BarrelPaymentStatus.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PAID -> BarrelPaymentStatus.PAID\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.CANCELLED -> BarrelPaymentStatus.CANCELLED\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.REFUNDED -> BarrelPaymentStatus.REFUNDED\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/resolvers/AdminBarrelFeeQueryResolver.kt" to listOf(
    "private fun dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.toDomain(): BarrelFeeStatusModel =\n  when (this) {\n    dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.DISABLED -> BarrelFeeStatusModel.DISABLED\n    dev.yidafu.aqua.common.graphql.generated.BarrelFeeStatus.ENABLED -> BarrelFeeStatusModel.ENABLED\n  }",
  ),
  "modules/aqua-marketing/src/main/kotlin/dev/yidafu/aqua/marketing/barrel/resolvers/AdminBarrelPurchaseQueryResolver.kt" to listOf(
    "private fun dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus.toDomain(): BarrelPaymentStatusModel =\n  when (this) {\n    dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus.CANCELLED -> BarrelPaymentStatusModel.CANCELLED\n    dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus.PAID -> BarrelPaymentStatusModel.PAID\n    dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus.PENDING -> BarrelPaymentStatusModel.PENDING\n    dev.yidafu.aqua.common.graphql.generated.BarrelPaymentStatus.REFUNDED -> BarrelPaymentStatusModel.REFUNDED\n  }",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/delivery/mapper/DeliveryMappers.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.toGraphQL(): AdminAccountStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.ACTIVE -> AdminAccountStatus.ACTIVE\n    dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.DISABLED -> AdminAccountStatus.DISABLED\n  }",
  ),
  "modules/aqua-trade/src/main/kotlin/dev/yidafu/aqua/order/mapper/OrderMappers.kt" to listOf(
    "private fun DomainDeliveryTimeSlot.toGraphQL(): DeliveryTimeSlotG =\n  when (this) {\n    DomainDeliveryTimeSlot.IMMEDIATE -> DeliveryTimeSlotG.IMMEDIATE\n    DomainDeliveryTimeSlot.MORNING -> DeliveryTimeSlotG.MORNING\n    DomainDeliveryTimeSlot.AFTERNOON -> DeliveryTimeSlotG.AFTERNOON\n    DomainDeliveryTimeSlot.EVENING -> DeliveryTimeSlotG.EVENING\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.toGraphQL(): DeliveryPhotoReviewStatusG =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.PENDING -> DeliveryPhotoReviewStatusG.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.APPROVED -> DeliveryPhotoReviewStatusG.APPROVED\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.REJECTED -> DeliveryPhotoReviewStatusG.REJECTED\n  }",
    "private fun DeliveryTimeSlotG.toDomain(): DomainDeliveryTimeSlot =\n  when (this) {\n    DeliveryTimeSlotG.AFTERNOON -> DomainDeliveryTimeSlot.AFTERNOON\n    DeliveryTimeSlotG.EVENING -> DomainDeliveryTimeSlot.EVENING\n    DeliveryTimeSlotG.IMMEDIATE -> DomainDeliveryTimeSlot.IMMEDIATE\n    DeliveryTimeSlotG.MORNING -> DomainDeliveryTimeSlot.MORNING\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.toGraphQL(): OrderOperationTypeG =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CREATED -> OrderOperationTypeG.ORDER_CREATED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_PAID -> OrderOperationTypeG.ORDER_PAID\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CANCELLED -> OrderOperationTypeG.ORDER_CANCELLED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_URGING -> OrderOperationTypeG.ORDER_URGING\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_ASSIGNED -> OrderOperationTypeG.DELIVERY_ASSIGNED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_STARTED -> OrderOperationTypeG.DELIVERY_STARTED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_COMPLETED -> OrderOperationTypeG.DELIVERY_COMPLETED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_CONFIRMED -> OrderOperationTypeG.ORDER_CONFIRMED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.AUTO_CONFIRMED -> OrderOperationTypeG.AUTO_CONFIRMED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ORDER_COMPLETED -> OrderOperationTypeG.ORDER_COMPLETED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.REFUND_INITIATED -> OrderOperationTypeG.REFUND_INITIATED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.REFUND_COMPLETED -> OrderOperationTypeG.REFUND_COMPLETED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.PAYMENT_TIMEOUT -> OrderOperationTypeG.PAYMENT_TIMEOUT\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_PHOTO_APPROVED -> OrderOperationTypeG.DELIVERY_PHOTO_APPROVED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.DELIVERY_PHOTO_REJECTED -> OrderOperationTypeG.DELIVERY_PHOTO_REJECTED\n    dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.ADDRESS_UPDATED -> OrderOperationTypeG.ADDRESS_UPDATED\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.OperatorType.toGraphQL(): OperatorTypeG =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.OperatorType.USER -> OperatorTypeG.USER\n    dev.yidafu.aqua.common.domain.model.enums.OperatorType.ADMIN -> OperatorTypeG.ADMIN\n    dev.yidafu.aqua.common.domain.model.enums.OperatorType.DELIVERY_WORKER -> OperatorTypeG.DELIVERY_WORKER\n    dev.yidafu.aqua.common.domain.model.enums.OperatorType.SYSTEM -> OperatorTypeG.SYSTEM\n  }",
  ),
  "modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/mapper/UserMappers.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.toGraphQL(): AdminAccountStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.ACTIVE -> AdminAccountStatus.ACTIVE\n    dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.DISABLED -> AdminAccountStatus.DISABLED\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus.toGraphQL(): DeliveryWorkerStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus.ONLINE -> DeliveryWorkerStatus.ONLINE\n    dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus.OFFLINE -> DeliveryWorkerStatus.OFFLINE\n  }",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/marketing/resolvers/AdminUserCouponTicketQueryResolver.kt" to listOf(
    "private fun DomainUserCouponStatus.toGraphQL(): GraphQLUserCouponStatus =\n  when (this) {\n    DomainUserCouponStatus.RECEIVED -> GraphQLUserCouponStatus.RECEIVED\n    DomainUserCouponStatus.USED -> GraphQLUserCouponStatus.USED\n    DomainUserCouponStatus.RETURNED -> GraphQLUserCouponStatus.RETURNED\n    DomainUserCouponStatus.EXPIRED -> GraphQLUserCouponStatus.EXPIRED\n  }",
    "private fun DomainUserTicketStatus.toGraphQL(): GraphQLUserTicketStatus =\n  when (this) {\n    DomainUserTicketStatus.UNUSED -> GraphQLUserTicketStatus.UNUSED\n    DomainUserTicketStatus.USED -> GraphQLUserTicketStatus.USED\n    DomainUserTicketStatus.RETURNED -> GraphQLUserTicketStatus.RETURNED\n    DomainUserTicketStatus.EXPIRED -> GraphQLUserTicketStatus.EXPIRED\n  }",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/ops/resolvers/AdminBannerMutationResolver.kt" to listOf(
    "private fun ReorderDirection.toDomain(): dev.yidafu.aqua.api.dto.ReorderDirection =\n  when (this) {\n    ReorderDirection.DOWN -> dev.yidafu.aqua.api.dto.ReorderDirection.DOWN\n    ReorderDirection.UP -> dev.yidafu.aqua.api.dto.ReorderDirection.UP\n  }",
  ),
  "services/aqua-admin/src/main/kotlin/dev/yidafu/aqua/admin/statistics/resolvers/AdminMarketingStatisticsQueryResolver.kt" to listOf(
    "private fun dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.toGraphQL(): MarketingBreakdownKind =\n  when (this) {\n    dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.COUPON_TEMPLATE -> MarketingBreakdownKind.COUPON_TEMPLATE\n    dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.PROMOTION_ACTIVITY -> MarketingBreakdownKind.PROMOTION_ACTIVITY\n    dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.FIRST_ORDER_ACTIVITY -> MarketingBreakdownKind.FIRST_ORDER_ACTIVITY\n    dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.GIFT_ACTIVITY -> MarketingBreakdownKind.GIFT_ACTIVITY\n    dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind.TICKET_TYPE -> MarketingBreakdownKind.TICKET_TYPE\n  }",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/barrel/mapper/ClientBarrelMapper.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.toGraphQL(): BarrelPaymentStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PENDING -> BarrelPaymentStatus.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.PAID -> BarrelPaymentStatus.PAID\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.CANCELLED -> BarrelPaymentStatus.CANCELLED\n    dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus.REFUNDED -> BarrelPaymentStatus.REFUNDED\n  }",
  ),
  "services/aqua-client/src/main/kotlin/dev/yidafu/aqua/client/order/mapper/ClientOrderMapper.kt" to listOf(
    "private fun dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot.toGraphQL(): DeliveryTimeSlot =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot.IMMEDIATE -> DeliveryTimeSlot.IMMEDIATE\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot.MORNING -> DeliveryTimeSlot.MORNING\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot.AFTERNOON -> DeliveryTimeSlot.AFTERNOON\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot.EVENING -> DeliveryTimeSlot.EVENING\n  }",
    "private fun dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.toGraphQL(): dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus =\n  when (this) {\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.PENDING -> dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus.PENDING\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.APPROVED -> dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus.APPROVED\n    dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus.REJECTED -> dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewStatus.REJECTED\n  }",
  ),
)
