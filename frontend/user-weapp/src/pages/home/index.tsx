import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, {
  useDidShow,
  usePullDownRefresh,
  useShareAppMessage
} from '@tarojs/taro'
import HomeService, { HomeProduct, HomeData } from '../../services/HomeService'
import { addToCart } from '../../utils/cart'
import { getDisplayPrice } from '../../utils/productPrice'
import { displayCents } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { userActionTracker } from '../../utils/tracking'
import CustomIcon from '@/components/CustomIcon'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme, gradientBox } from '../../styles/theme.stylex'

// 子组件
import HomeNoticeBar from './components/HomeNoticeBar'
import AddressBar from './components/AddressBar'
import CarouselSection from './components/CarouselSection'

/**
 * 首页(spec/design/user-weapp/01-product/home.html)
 * 布局:渐变 hero(定位行 + 白底搜索框)→ 运营轮播 → 通告栏 →
 *      快捷入口四宫格(立即订水/我的订单/领券中心/我的水票)→
 *      「推荐产品」两列商品网格 → Loadmore
 * 数据:单次 GraphQL 请求聚合(规则 #10,HomeService.loadHomeData),
 *      最常买/更多商品合并为推荐产品网格(取前 6)。
 * 时效选择能力在 order-confirm 页,首页不再有时效卡(R-PRO-01 首屏 ≤3 信息)。
 *
 * 样式:StyleX(原 index.scss 迁移;750 基准 rpx,后代选择器拍平;
 * hero 装饰圆与搜索放大镜柄原为伪元素,改为真实子节点)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    paddingBottom: tokens.s5, // 原 var(--spacing-xl) = 24px
  },
  // ① 渐变 hero(渐变底由 gradientBox 提供)
  hero: {
    position: 'relative',
    overflow: 'hidden',
    paddingTop: '24rpx',
    paddingRight: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '32rpx',
    color: tokens.onBrand,
  },
  // 原型 hero 伪元素装饰圆(右上两个白色半透明圆)
  heroCircleA: {
    position: 'absolute',
    right: '-80rpx',
    top: '-60rpx',
    width: '340rpx',
    height: '340rpx',
    borderRadius: '50%',
    backgroundColor: 'rgba(255, 255, 255, 0.10)',
  },
  heroCircleB: {
    position: 'absolute',
    right: '80rpx',
    top: '120rpx',
    width: '180rpx',
    height: '180rpx',
    borderRadius: '50%',
    backgroundColor: 'rgba(255, 255, 255, 0.07)',
  },
  // 白底搜索框(weui 类自带底色/圆角/行高)
  search: {
    position: 'relative',
    zIndex: 1,
    marginTop: '20rpx',
  },
  searchPh: {
    color: 'var(--weui-FG-2)',
  },
  // 搜索放大镜(weapp 无 svg,用 CSS 画:圆环 + 斜柄)
  searchGlyph: {
    position: 'relative',
    width: '28rpx',
    height: '28rpx',
    borderWidth: '3rpx',
    borderStyle: 'solid',
    borderColor: 'var(--weui-FG-2)',
    borderRadius: '50%',
  },
  searchGlyphHandle: {
    position: 'absolute',
    right: '-9rpx',
    bottom: '-7rpx',
    width: '12rpx',
    height: '3rpx',
    backgroundColor: 'var(--weui-FG-2)',
    borderRadius: '2rpx',
    transform: 'rotate(45deg)',
  },
  // ④ 快捷入口四宫格(weui-cells 包裹 + 上下 8px 内边距)
  gridsCells: {
    marginTop: '16rpx',
    paddingTop: '8rpx',
    paddingBottom: '8rpx',
    backgroundColor: 'var(--weui-BG-2)',
  },
  // 四宫格图标 tint 底(CustomIcon 图标置入容器居中)
  gridIcon: {
    borderRadius: '50%',
    backgroundColor: 'var(--weui-BRAND-100)',
  },
  // ⑤ 推荐产品两列网格
  prodPriceCol: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: '0',
  },
  prodPriceRow: {
    display: 'flex',
    alignItems: 'baseline',
    flexWrap: 'wrap',
  },
  prodTagRow: {
    marginTop: '8rpx',
    display: 'flex',
    alignItems: 'center',
    rowGap: '8rpx',
    columnGap: '8rpx',
  },
  // 封面:1:1 hack 容器内子元素绝对定位铺满
  prodCoverImg: {
    position: 'absolute',
    left: '0',
    top: '0',
    width: '100%',
    height: '100%',
  },
  // 无图占位:品牌 tint 底 + 商品名首字
  prodCoverPlaceholder: {
    position: 'absolute',
    left: '0',
    top: '0',
    width: '100%',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  prodCoverPlaceholderText: {
    fontSize: '64rpx',
    fontWeight: 600,
    color: 'var(--weui-BRAND-DARK)',
  },
})

/** 快捷入口四宫格(order-list 为 tabBar 页,须 switchTab) */
const QUICK_ENTRIES = [
  { icon: '/assets/icons/order/shopping-bag', label: '立即订水', type: 'product' },
  { icon: '/assets/icons/order/all', label: '我的订单', type: 'order' },
  { icon: '/assets/icons/service/coupon', label: '领券中心', type: 'coupon' },
  { icon: '/assets/icons/service/ticket', label: '我的水票', type: 'ticket' }
] as const

/** 库存偏低阈值:≤10 桶展示「仅剩 N 桶」橙标(home.html 商品卡规格) */
const LOW_STOCK_THRESHOLD = 10

const HomePage: React.FC = () => {
  usePageViewTracking()

  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | undefined>(undefined)
  const [hotProducts, setHotProducts] = useState<HomeProduct[]>([])
  const [moreProducts, setMoreProducts] = useState<HomeProduct[]>([])
  const [banners, setBanners] = useState<HomeData['banners']>([])
  const [notices, setNotices] = useState<HomeData['notices']>([])
  const [defaultAddress, setDefaultAddress] = useState<HomeData['defaultAddress']>(null)

  /**
   * 加载首页数据(单次 GraphQL 请求,规则 #10)
   * 推荐产品取 hotSize+moreSize=6 条(两列网格 4~6 张卡)
   */
  const loadHomeData = useCallback(async () => {
    try {
      setLoading(true)
      setError(undefined)
      const result = await HomeService.loadHomeData(4, 2)
      if (result.success && result.data) {
        const d = result.data
        setHotProducts(d.hotProducts)
        setMoreProducts(d.moreProducts)
        setBanners(d.banners)
        setNotices(d.notices)
        setDefaultAddress(d.defaultAddress)
      } else {
        throw new Error(result.error?.message || '加载失败')
      }
    } catch (e) {
      console.error('Load home data error:', e)
      setError(e instanceof Error ? e.message : '加载失败')
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
      Taro.stopPullDownRefresh()
    }
  }, [])

  useEffect(() => {
    // tabBar 角标同步(不触发数据请求)
  }, [])

  // 页面显示时加载首页数据(首次显示 + 每次回前台刷新;运营配置停用/到期即时生效,R-OPS-01)
  useDidShow(() => {
    loadHomeData()
  })

  // 下拉刷新
  usePullDownRefresh(async () => {
    await loadHomeData()
  })

  // 分享
  useShareAppMessage(() => ({
    title: '好喝山泉 · 桶装水配送',
    path: '/pages/home/index'
  }))

  /** 推荐产品网格数据:最常买 + 更多商品合并,取前 6 */
  const products = [...hotProducts, ...moreProducts].slice(0, 6)

  const addressText = useCallback(() => {
    if (!defaultAddress) return '请选择收货地址'
    return `${defaultAddress.city}${defaultAddress.district}${defaultAddress.detailAddress}`
  }, [defaultAddress])

  const handleAddressClick = useCallback(() => {
    // 埋点:首页定位行(地址切换入口)
    userActionTracker.track('CLICK', 'home-address-entry', {
      elementType: 'cell',
      elementText: '切换地址'
    })
    Taro.navigateTo({ url: '/pages/address-list/index' })
  }, [])

  const handleSearchClick = useCallback(() => {
    // 埋点:首页搜索入口(SEARCH);product-list 未支持 keyword 入参,先跳列表页
    userActionTracker.track('SEARCH', 'home-search-entry', {
      elementType: 'icon',
      elementText: '搜索'
    })
    Taro.navigateTo({ url: '/pages/product-list/index' })
  }, [])

  const handleProductClick = useCallback((product: HomeProduct) => {
    userActionTracker.track('CLICK', 'home-product-card', {
      elementType: 'card',
      elementText: product.name,
      properties: { productId: product.id }
    })
    Taro.navigateTo({ url: `/pages/product-detail/index?id=${product.id}` })
  }, [])

  /**
   * 加购圆钮(.mp-prod__cart):本地购物车累积(同商品合并数量),
   * 加购后轻提示;埋点与外层卡片点击分开上报(嵌套节点不配 track-* props)
   */
  const handleAddToCart = useCallback((product: HomeProduct) => {
    addToCart({
      id: product.id,
      name: product.name,
      priceCents: getDisplayPrice(product).price,
      quantity: 1,
      image: product.coverImageUrl,
      specification: product.subtitle || product.specification
    })
    userActionTracker.track('CLICK', 'home-add-cart', {
      elementType: 'button',
      elementText: '加入购物车',
      properties: { productId: product.id }
    })
    Taro.showToast({ title: '已加入购物车', icon: 'success' })
  }, [])

  /** 快捷入口四宫格(order-list 是 tabBar 页,必须 switchTab) */
  const handleQuickEntry = useCallback((type: string) => {
    if (type === 'product') {
      Taro.navigateTo({ url: '/pages/product-list/index' })
    } else if (type === 'order') {
      Taro.switchTab({ url: '/pages/order-list/index' })
    } else if (type === 'coupon') {
      Taro.navigateTo({ url: '/pages/coupon-center/index' })
    } else if (type === 'ticket') {
      Taro.navigateTo({ url: '/pages/ticket-list/index' })
    }
  }, [])

  const handleBannerClick = useCallback((linkType: string, linkTarget: string) => {
    userActionTracker.track('CLICK', 'home-banner', {
      elementType: 'banner',
      properties: { linkType, linkTarget }
    })
    if (linkType === 'PRODUCT') {
      Taro.navigateTo({ url: `/pages/product-detail/index?id=${linkTarget}` })
    } else if (linkTarget) {
      Taro.navigateTo({ url: linkTarget })
    }
  }, [])

  const handleNoticeClick = useCallback((id: string) => {
    userActionTracker.track('CLICK', 'home-notice', {
      elementType: 'notice',
      properties: { noticeId: id }
    })
    Taro.navigateTo({ url: `/pages/notice-detail/index?id=${id}` })
  }, [])

  /** 价格展示(R-MKT-02):有成交优惠时优惠价红 + 目录价划线 */
  const renderPrice = (product: HomeProduct) => {
    const { price, tag } = getDisplayPrice(product)
    const [intPart, decPart = '00'] = displayCents(price).split('.')
    const showOrigin = tag !== ''
    const lowStock = product.stock <= LOW_STOCK_THRESHOLD

    return (
      <View {...stylex.props(styles.prodPriceCol)}>
        <View {...stylex.props(styles.prodPriceRow)}>
          <View className='weui-price weui-price--md'>
            <Text className='weui-price__symbol'>¥</Text>
            <Text className='weui-price__int'>{intPart}</Text>
            <Text className='weui-price__dec'>.{decPart}</Text>
          </View>
          {showOrigin && (
            <Text className='weui-price--origin'>¥{displayCents(product.priceCents)}</Text>
          )}
        </View>
        {(tag !== '' || lowStock) && (
          <View {...stylex.props(styles.prodTagRow)}>
            {tag !== '' && <Text className='weui-tag weui-tag--brand'>{tag}</Text>}
            {lowStock && <Text className='weui-tag weui-tag--orange'>仅剩 {product.stock} 桶</Text>}
          </View>
        )}
      </View>
    )
  }

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
      {/* ① 渐变 hero:定位行 + 白底搜索框(home.html .hero) */}
      <View {...stylex.props(gradientBox.gradient, styles.hero)}>
        {/* 原 hero 伪元素装饰圆(右上两个白色半透明圆) */}
        <View {...stylex.props(styles.heroCircleA)} />
        <View {...stylex.props(styles.heroCircleB)} />
        <AddressBar
          addressText={addressText()}
          onAddressClick={handleAddressClick}
        />
        <View
          {...props('weui-searchbar__box on-brand', styles.search)}
          onClick={handleSearchClick}
        >
          <View {...stylex.props(styles.searchGlyph)}>
            <View {...stylex.props(styles.searchGlyphHandle)} />
          </View>
          <Text {...stylex.props(styles.searchPh)}>搜索「怡宝」「农夫山泉」</Text>
        </View>
      </View>

      {/* ② 运营轮播(容器对齐 .banner 16px 圆角;无生效条目隐藏) */}
      <View className='mp-pad'>
        <CarouselSection banners={banners} onBannerClick={handleBannerClick} />
      </View>

      {/* ③ 通告栏(.weui-notice 橙 tint,仅生效公告时显示,F-OPS-002) */}
      {notices.length > 0 && (
        <HomeNoticeBar
          title={notices[0].title}
          onClick={() => handleNoticeClick(notices[0].id)}
        />
      )}

      {/* ④ 快捷入口四宫格(.weui-cells 包裹 + .weui-grids 官方四宫格) */}
      <View {...props('weui-cells', styles.gridsCells)}>
        <View className='weui-grids'>
          {QUICK_ENTRIES.map((entry) => (
            <View
              key={entry.type}
              className='weui-grid'
              trackTarget={`home-grid-${entry.type}`}
              trackElementType='grid'
              trackElementText={entry.label}
              onClick={() => handleQuickEntry(entry.type)}
            >
              <View {...props('weui-grid__icon', styles.gridIcon)}>
                <CustomIcon value={entry.icon} size={30} />
              </View>
              <Text className='weui-grid__label'>{entry.label}</Text>
            </View>
          ))}
        </View>
      </View>

      {/* ⑤ 「推荐产品」两列商品网格(.mp-grid2 + .mp-prod) */}
      <View className='weui-cells__title'>推荐产品</View>
      {loading ? (
        <View className='weui-loadmore weui-loadmore_loading'>
          <View className='spinner' />
          <Text>加载中...</Text>
        </View>
      ) : error ? (
        <View className='weui-empty'>
          <View className='weui-empty__icon'>
            <CustomIcon value='/assets/icons/order/shopping-bag' size={64} />
          </View>
          <View className='weui-empty__text'>{error}</View>
          <View
            className='weui-btn weui-btn_plain-primary weui-btn_mini'
            trackTarget='home-retry'
            trackElementType='button'
            trackElementText='重试'
            onClick={loadHomeData}
          >
            重试
          </View>
        </View>
      ) : products.length === 0 ? (
        <View className='weui-empty'>
          <View className='weui-empty__icon'>
            <CustomIcon value='/assets/icons/order/shopping-bag' size={64} />
          </View>
          <View className='weui-empty__text'>
            <Text>{'暂无产品\n门店补货中，稍后再来看看'}</Text>
          </View>
        </View>
      ) : (
        <View className='mp-grid2'>
          {products.map((p) => (
            <View key={p.id} className='mp-prod' onClick={() => handleProductClick(p)}>
              <View className='mp-prod__cover'>
                {p.coverImageUrl ? (
                  <Image
                    src={p.coverImageUrl}
                    mode='aspectFill'
                    {...stylex.props(styles.prodCoverImg)}
                    lazyLoad
                  />
                ) : (
                  <View {...stylex.props(styles.prodCoverPlaceholder)}>
                    <Text {...stylex.props(styles.prodCoverPlaceholderText)}>{p.name.slice(0, 1)}</Text>
                  </View>
                )}
              </View>
              <View className='mp-prod__body'>
                <View className='mp-prod__name'>
                  <Text>{p.name}</Text>
                </View>
                <View className='mp-prod__desc'>
                  <Text>{p.subtitle || p.specification}</Text>
                </View>
                <View className='mp-prod__foot'>
                  {renderPrice(p)}
                  <View
                    className='mp-prod__cart'
                    onClick={(e: any) => {
                      e.stopPropagation?.()
                      handleAddToCart(p)
                    }}
                  >
                    <Text>＋</Text>
                  </View>
                </View>
              </View>
            </View>
          ))}
        </View>
      )}

      {/* ⑥ Loadmore 双短线(有商品时) */}
      {!loading && !error && products.length > 0 && (
        <View className='weui-loadmore'>
          <View className='weui-loadmore__line' />
          <Text>没有更多产品了</Text>
          <View className='weui-loadmore__line' />
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default HomePage
