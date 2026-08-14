package dev.yidafu.aqua.common.domain.model.enums

/**
 * 成交单价来源(PRD 10-marketing.md §R-MKT-01/02):
 * - CATALOG_PRICE: 商品目录价
 * - GROUP_PRICE: 客户分组商品专属价(熟客价)
 * - FLASH_SALE_PRICE: 限时特价活动价(R-MKT-05,链路③)
 * - NEWCOMER_PRICE: 新人专享价(R-MKT-10,链路④)
 * - MANUAL: 送水工代客下单手动实收价(预留,本轮未接入)
 */
enum class PriceSource {
  CATALOG_PRICE,
  GROUP_PRICE,
  FLASH_SALE_PRICE,
  NEWCOMER_PRICE,
  MANUAL,
}
