import { networkManager } from '../utils/networkManager'
import {
  Banner,
  Notice,
  StoreProfile,
  Address as GraphQLAddress,
  normalizeCents
} from '@aquarush/common'

/**
 * 首页聚合数据服务
 *
 * 按仓库规则 #10:一个页面的一次渲染只发起一个 GraphQL 请求。
 * 首页(地址/商品/轮播/公告/店铺信息)合并为单次聚合 query,
 * 通过字段选择一次取回该页所需全部数据,禁止串行多个独立请求。
 */

export interface HomeProduct {
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

export interface HomeData {
  /** 按销量降序的高频商品(最常买,top N) */
  hotProducts: HomeProduct[]
  /** 其余商品(更多商品区,≤3) */
  moreProducts: HomeProduct[]
  banners: Banner[]
  notices: Notice[]
  storeProfile: StoreProfile | null
  /** 默认收货地址(无则为 null) */
  defaultAddress: GraphQLAddress | null
  /** 营业时间文案("07:00-22:00",未配置回退默认) */
  businessHours: string
}

export interface HomeServiceResult {
  success: boolean
  data?: HomeData
  error?: {
    message: string
    code?: string
  }
}

class HomeService {
  private static instance: HomeService

  private constructor() {}

  public static getInstance(): HomeService {
    if (!HomeService.instance) {
      HomeService.instance = new HomeService()
    }
    return HomeService.instance
  }

  /**
   * 加载首页全部数据(单次 GraphQL 请求)
   * @param hotSize 最常买商品数(默认 4)
   * @param moreSize 更多商品数(默认 3)
   */
  public async loadHomeData(hotSize = 4, moreSize = 3): Promise<HomeServiceResult> {
    const query = `
      query HomeOverview($totalSize: Int) {
        # 商品:按销量降序取 hotSize+moreSize 条,前端拆分为 最常买 + 更多商品(R-USR-01)
        activeProducts(
          page: 0
          size: $totalSize
          sortBy: "salesVolume"
          sortDirection: "desc"
        ) {
          list {
            id
            name
            subtitle
            specification
            priceCents
            effectivePriceCents
            priceSource
            coverImageUrl
            stock
            salesVolume
          }
        }
        # 活动轮播(F-OPS-001,时间窗/停用由服务端过滤)
        activeBanners {
          id
          title
          subtitle
          imageUrl
          linkType
          linkTarget
          sortOrder
        }
        # 公告(F-OPS-002,置顶优先/发布时间倒序)
        activeNotices {
          id
          title
          content
          type
          pinned
          status
          publishedAt
        }
        # 店铺信息(F-OPS-004,营业时间)
        storeProfile {
          shopName
          logoUrl
          serviceHotline
          businessHoursStart
          businessHoursEnd
          shopIntro
        }
        # 默认收货地址(地址栏)
        userAddresses {
          id
          receiverName
          phone
          province
          city
          district
          detailAddress
          floor
          isDefault
        }
      }
    `

    try {
      const response = await networkManager.query<{
        activeProducts?: { list: any[] }
        activeBanners?: Banner[]
        activeNotices?: Notice[]
        storeProfile?: StoreProfile | null
        userAddresses?: GraphQLAddress[]
      }>(query, { totalSize: hotSize + moreSize })

      const toHomeProduct = (p: any): HomeProduct => ({
        id: p.id,
        name: p.name,
        subtitle: p.subtitle ?? undefined,
        specification: p.specification ?? '',
        priceCents: normalizeCents(p.priceCents) ?? 0,
        effectivePriceCents: normalizeCents(p.effectivePriceCents) ?? undefined,
        priceSource: p.priceSource,
        coverImageUrl: p.coverImageUrl ?? '',
        stock: p.stock ?? 0,
        salesVolume: p.salesVolume ?? 0
      })

      const allProducts = (response?.activeProducts?.list ?? []).map(toHomeProduct)
      // 按销量降序,前 hotSize 为「最常买」,其余为「更多商品」(≤3)
      const hotProducts = allProducts.slice(0, hotSize)
      const moreProducts = allProducts.slice(hotSize, hotSize + moreSize)

      const banners = response?.activeBanners ?? []
      const notices = response?.activeNotices ?? []
      const storeProfile = response?.storeProfile ?? null

      const defaultAddress =
        (response?.userAddresses ?? []).find((a) => a.isDefault) ?? (response?.userAddresses ?? [])[0] ?? null

      const start = storeProfile?.businessHoursStart || '07:00'
      const end = storeProfile?.businessHoursEnd || '22:00'
      const businessHours = `${start}-${end}`

      return {
        success: true,
        data: {
          hotProducts,
          moreProducts,
          banners,
          notices,
          storeProfile,
          defaultAddress,
          businessHours
        }
      }
    } catch (error: any) {
      console.error('Load home data failed:', error)
      let message = '加载失败，请重试'
      let code: string | undefined
      if (error && typeof error === 'object' && 'errors' in error) {
        const gqlErrors = (error as any).errors
        if (Array.isArray(gqlErrors) && gqlErrors.length > 0) {
          message = gqlErrors[0].message || message
          code = gqlErrors[0].extensions?.code
        }
      } else if (error instanceof Error && error.message) {
        message = error.message
      }
      return { success: false, error: { message, code } }
    }
  }
}

export default HomeService.getInstance()
