import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import Taro, {
  useDidShow,
  usePullDownRefresh,
  useShareAppMessage
} from '@tarojs/taro'
import HomeService, { HomeProduct, HomeData } from '../../services/HomeService'
import { syncCartTabBadge } from '../../utils/cart'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { userActionTracker } from '../../utils/tracking'

// 子组件
import HomeNoticeBar from './components/HomeNoticeBar'
import AddressBar from './components/AddressBar'
import CarouselSection from './components/CarouselSection'
import DeliveryTimeCard from './components/DeliveryTimeCard'
import HomeProductCard from './components/HomeProductCard'

import "taro-ui/dist/style/components/button.scss"
import './index.scss'

/**
 * 首页(user-weapp §1 首屏详设)
 * 首屏(100vh 内)只解决 3 件事:进入 → 看到 → 点击购买,路径最短。
 * 布局:顶部地址栏 → 品牌水站 Banner → 活动轮播(有配置时) →
 *      配送时效卡 → 最常买(横滑) → 更多商品(≤3)
 * 数据:单次 GraphQL 请求聚合(规则 #10,HomeService.loadHomeData)
 * 渲染:区块与商品卡由 components/ 子组件完成。
 */
const HomePage: React.FC = () => {
  usePageViewTracking()

  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | undefined>(undefined)
  const [hotProducts, setHotProducts] = useState<HomeProduct[]>([])
  const [moreProducts, setMoreProducts] = useState<HomeProduct[]>([])
  const [banners, setBanners] = useState<HomeData['banners']>([])
  const [notices, setNotices] = useState<HomeData['notices']>([])
  const [defaultAddress, setDefaultAddress] = useState<HomeData['defaultAddress']>(null)
  const [businessHours, setBusinessHours] = useState<string>('07:00-22:00')

  /**
   * 加载首页数据(单次 GraphQL 请求,规则 #10)
   */
  const loadHomeData = useCallback(async () => {
    try {
      setLoading(true)
      setError(undefined)
      const result = await HomeService.loadHomeData(4, 3)
      if (result.success && result.data) {
        const d = result.data
        setHotProducts(d.hotProducts)
        setMoreProducts(d.moreProducts)
        setBanners(d.banners)
        setNotices(d.notices)
        setDefaultAddress(d.defaultAddress)
        setBusinessHours(d.businessHours)
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
    syncCartTabBadge()
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

  const addressText = useCallback(() => {
    if (!defaultAddress) return '请选择收货地址'
    return `${defaultAddress.city}${defaultAddress.district}${defaultAddress.detailAddress}`
  }, [defaultAddress])

  const handleAddressClick = useCallback(() => {
    Taro.navigateTo({ url: '/pages/address-list/index' })
  }, [])

  const handleSearchClick = useCallback(() => {
    // 埋点:首页搜索入口(SEARCH)
    userActionTracker.track('SEARCH', 'home-search-entry', {
      elementType: 'icon',
      elementText: '搜索'
    })
    Taro.showToast({ title: '搜索功能开发中', icon: 'none' })
  }, [])

  const handleProductClick = useCallback((product: HomeProduct) => {
    Taro.navigateTo({ url: `/pages/product-detail/index?id=${product.id}` })
  }, [])

  /**
   * 更多商品「更多」入口 → 全部商品列表页(排序/搜索/分页)
   */
  const handleMoreClick = useCallback(() => {
    userActionTracker.track('CLICK', 'home-more-entry', {
      elementType: 'link',
      elementText: '更多',
      properties: { target: '/pages/product-list/index' }
    })
    Taro.navigateTo({ url: '/pages/product-list/index' })
  }, [])

  /**
   * 立即订水(最常买 CTA,强调橙 #FF5436):
   * 跳商品详情走最短购买路径(详情页 立即购买 → 确认订单)
   */
  const handleQuickOrder = useCallback((product: HomeProduct) => {
    userActionTracker.track('CLICK', 'home-quick-order', {
      elementType: 'button',
      elementText: '立即订水',
      properties: { productId: product.id }
    })
    handleProductClick(product)
  }, [handleProductClick])

  const handleBannerClick = useCallback((linkType: string, linkTarget: string) => {
    if (linkType === 'PRODUCT') {
      Taro.navigateTo({ url: `/pages/product-detail/index?id=${linkTarget}` })
    } else if (linkTarget) {
      Taro.navigateTo({ url: linkTarget })
    }
  }, [])

  const handleNoticeClick = useCallback((id: string) => {
    Taro.navigateTo({ url: `/pages/notice-detail/index?id=${id}` })
  }, [])

  return (
    <View className='home-page'>
      {/* 顶部公告栏(浅蓝 tint 底,仅生效公告时显示,F-OPS-002) */}
      {notices.length > 0 && (
        <HomeNoticeBar
          title={notices[0].title}
          onClick={() => handleNoticeClick(notices[0].id)}
        />
      )}

      {/* ① 顶部地址栏(44px 白底固定顶部) */}
      <AddressBar
        addressText={addressText()}
        onAddressClick={handleAddressClick}
        onSearchClick={handleSearchClick}
      />

      {/* ② 品牌水站 Banner(清水蓝渐变,信任条) */}
      <View className='brand-banner theme-gradient'>
        <View className='brand-banner-text'>
          <Text className='brand-banner-title'>最快 28 分钟送达</Text>
          <Text className='brand-banner-subtitle'>正品保障 · 实拍认证 · 假一赔十</Text>
        </View>
      </View>

      {/* ③ 活动轮播(卡片化,无生效条目隐藏) */}
      <CarouselSection banners={banners} onBannerClick={handleBannerClick} />

      {/* ④ 配送时效卡(浅蓝左侧色条) */}
      <DeliveryTimeCard businessHours={businessHours} />

      {/* ⑤ 最常买 · 快捷购买区(横滑卡片) */}
      <View className='section-block'>
        <View className='section-header'>
          <Text className='section-title'>🔥 最常买</Text>
        </View>
        {loading ? (
          <View className='loading-box'>
            <Text className='loading-text'>加载中...</Text>
          </View>
        ) : error ? (
          <View className='error-box'>
            <Text className='error-text'>{error}</Text>
            <AtButton size='small' type='primary' onClick={loadHomeData} className='retry-btn'>重试</AtButton>
          </View>
        ) : hotProducts.length === 0 ? (
          <View className='empty-box'>
            <Text className='empty-text'>暂无商品</Text>
          </View>
        ) : (
          <ScrollView scrollX className='hot-scroll' showScrollbar={false}>
            <View className='hot-list'>
              {hotProducts.map((p) => (
                <HomeProductCard
                  key={p.id}
                  product={p}
                  action
                  onClick={() => handleProductClick(p)}
                  onQuickOrder={() => handleQuickOrder(p)}
                />
              ))}
            </View>
          </ScrollView>
        )}
      </View>

      {/* ⑥ 更多商品(仅 2-3 个 SKU,不做长信息流;「更多」入口 → 全部商品列表页) */}
      {(hotProducts.length > 0 || moreProducts.length > 0) && (
        <View className='section-block'>
          <View className='section-header'>
            <Text className='section-title'>📦 更多商品</Text>
            <Text className='section-extra' onClick={handleMoreClick}>更多 ›</Text>
          </View>
          {moreProducts.length > 0 && (
            <View className='more-list'>
              {moreProducts.map((p) => (
                <HomeProductCard
                  key={p.id}
                  product={p}
                  onClick={() => handleProductClick(p)}
                  onQuickOrder={() => handleQuickOrder(p)}
                />
              ))}
            </View>
          )}
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default HomePage
