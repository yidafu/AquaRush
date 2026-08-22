/**
 * 促销价展示(R-MKT-02 单价链):命中且低于目录价时展示促销价 + tint 标签。
 * 首页「最常买/更多商品」与全部商品列表页共用(消除重复实现)。
 */
export interface PriceDisplaySource {
  /** 成交单价来源标签:GROUP_PRICE/FLASH_SALE_PRICE/NEWCOMER_PRICE/CATALOG_PRICE */
  priceSource?: string
  /** 目录价(分) */
  priceCents: number
  /** 促销价(分,有效成交单价 R-MKT-02) */
  effectivePriceCents?: number | null
}

export interface PriceDisplay {
  price: number
  tag: string
}

export const getDisplayPrice = (p: PriceDisplaySource): PriceDisplay => {
  const source = p.priceSource
  const hasDiscounted =
    (source === 'GROUP_PRICE' || source === 'FLASH_SALE_PRICE' || source === 'NEWCOMER_PRICE') &&
    p.effectivePriceCents != null &&
    p.effectivePriceCents < p.priceCents
  const tag =
    source === 'FLASH_SALE_PRICE' ? '特价' : source === 'NEWCOMER_PRICE' ? '新人价' : source === 'GROUP_PRICE' ? '熟客价' : ''
  return { price: hasDiscounted && p.effectivePriceCents != null ? p.effectivePriceCents : p.priceCents, tag }
}
