export interface ProductListItem {
  id: string
  name: string
  subtitle?: string
  specification: string
  /** 目录价(分) */
  priceCents: number
  /** 促销价(分,有效成交单价 R-MKT-02) */
  effectivePriceCents?: number
  /** 成交单价来源标签:GROUP_PRICE/FLASH_SALE_PRICE/NEWCOMER_PRICE/CATALOG_PRICE */
  priceSource?: string
  coverImageUrl: string
  stock: number
  salesVolume: number
}

export interface SortTab {
  key: string
  label: string
}

export const SORT_TABS: SortTab[] = [
  { key: 'default', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price', label: '价格' }
]
