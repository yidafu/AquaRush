import React, { useState, useEffect } from 'react'
import { View, Text, Image, Swiper, SwiperItem } from '@tarojs/components'
import { AtButton, AtDivider, AtLoadMore, AtFab } from 'taro-ui'
import Taro from '@tarojs/taro'
import {
  useReady,
  useDidShow,
  usePullDownRefresh,
  useReachBottom
} from '@tarojs/taro'
import CustomNavBar from '../../components/CustomNavBar'
import { authService } from '../../utils/auth'
import { displayCents, Banner, Notice } from '@aquarush/common'
import ProductService from '../../services/ProductService'
import ConfigService from '../../services/ConfigService'
import NoticeService from '../../services/NoticeService'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { userActionTracker } from '../../utils/tracking'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/grid.scss"
import "taro-ui/dist/style/components/divider.scss"
import "taro-ui/dist/style/components/load-more.scss"
import "taro-ui/dist/style/components/fab.scss"
import './index.scss'

// Import the ExtendedProduct type from ProductService
interface ExtendedProduct {
  id: string
  name: string
  subtitle?: string
  price: number // Converted to number for displayCents compatibility
  // 当前用户有效成交单价(分,熟客价优先 —— R-MKT-01)
  effectivePrice?: number
  // 是否命中熟客价(分组价)
  hasGroupPrice?: boolean
  originalPrice?: number
  depositPrice?: number
  coverImageUrl: string
  image: string // Mapped from coverImageUrl for homepage compatibility
  description: string // Additional field for homepage compatibility
  imageGallery?: any[]
  specification: string
  waterSource?: string
  mineralContent?: string
  stock: number
  salesVolume: number
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE'
  sortOrder: number
  tags?: any[]
  detailContent?: string
  certificateImages?: any[]
  deliverySettings?: Record<string, any>
  createdAt: string
  updatedAt: string
}

const HomePage: React.FC = () => {
  usePageViewTracking()

  const productService = ProductService.getInstance()

  const [products, setProducts] = useState<ExtendedProduct[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [userInfo, setUserInfo] = useState<any>(null)
  const [currentSlide, setCurrentSlide] = useState<number>(0)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [totalCount, setTotalCount] = useState<number>(0)
  const [error, setError] = useState<string | undefined>(undefined)
  const [banners, setBanners] = useState<Banner[]>([])
  const [notices, setNotices] = useState<Notice[]>([])

  // 组件挂载时执行
  useEffect(() => {
    loadUserInfo()
    loadProducts()
    loadBanners()
    loadNotices()
  }, [])

  // 页面准备就绪时执行
  useReady(() => {
    console.log('Home page ready')
  })

  // 页面显示时执行
  useDidShow(() => {
    // 页面显示时刷新用户信息、产品数据与运营配置(停用/到期即时生效,R-OPS-01)
    loadUserInfo()
    refreshProducts()
    loadBanners()
    loadNotices()
  })

  // 下拉刷新
  usePullDownRefresh(async () => {
    await refreshProducts()
  })

  // 滚动到底部
  useReachBottom(() => {
    loadMoreProducts()
  })

  /**
   * 加载用户信息
   */
  const loadUserInfo = async () => {
    try {
      // 检查用户是否已登录，如果已登录则获取用户信息
      if (authService.isAuthenticated()) {
        const userInfo = authService.getUserInfo()
        setUserInfo(userInfo)
      } else {
        // 如果用户未登录，设置为null，组件会根据状态显示不同的内容
        setUserInfo(null)
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      setUserInfo(null)
    }
  }

  /**
   * 加载产品列表（首次加载）
   */
  const loadProducts = async () => {
    try {
      setLoading(true)
      setError(undefined)

      const result = await productService.getActiveProducts({
        page: 0,
        size: 20,
        sortBy: 'createdAt',
        sortDirection: 'desc'
      })

      if (result.success && result.data) {
        const { products, pagination } = result.data

        // Products are already transformed in ProductService with image and description fields
        setProducts(products)
        setCurrentPage(pagination.page)
        setHasMore(!pagination.last)
        setTotalCount(pagination.totalElements)
        setLoading(false)
      } else {
        throw new Error(result.error?.message || '获取产品列表失败')
      }
    } catch (error) {
      console.error('Load products error:', error)
      setLoading(false)
      setError(error instanceof Error ? error.message : '加载失败')

      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  }

  /**
   * 刷新产品列表（下拉刷新）
   */
  const refreshProducts = async () => {
    try {
      setLoading(true)
      setError(undefined)

      const result = await productService.getActiveProducts({
        page: 0,
        size: 20,
        sortBy: 'createdAt',
        sortDirection: 'desc'
      })

      if (result.success && result.data) {
        const { products, pagination } = result.data

        // Products are already transformed in ProductService with image and description fields
        setProducts(products)
        setCurrentPage(pagination.page)
        setHasMore(!pagination.last)
        setTotalCount(pagination.totalElements)
        setLoading(false)
        setError(undefined)
      } else {
        throw new Error(result.error?.message || '刷新产品列表失败')
      }
    } catch (error) {
      console.error('Refresh products error:', error)
      setLoading(false)
      setError(error instanceof Error ? error.message : '刷新失败')

      Taro.showToast({
        title: '刷新失败',
        icon: 'none'
      })
    } finally {
      Taro.stopPullDownRefresh()
    }
  }

  /**
   * 加载更多产品
   */
  const loadMoreProducts = async () => {
    try {
      if (loadingMore || !hasMore) {
        return
      }

      setLoadingMore(true)

      const nextPage = currentPage + 1
      const result = await productService.loadMoreProducts(nextPage, {
        size: 20,
        sortBy: 'createdAt',
        sortDirection: 'desc'
      })

      if (result.success && result.data) {
        const { products, pagination } = result.data

        // Transform products to match homepage expected format
        const transformedProducts = products.map(product => ({
          ...product,
          image: product.coverImageUrl,
          description: product.subtitle || product.specification || ''
        }))

        setProducts(prevProducts => [...prevProducts, ...transformedProducts])
        setCurrentPage(pagination.page)
        setHasMore(!pagination.last)
        setLoadingMore(false)
      } else {
        throw new Error(result.error?.message || '加载更多产品失败')
      }
    } catch (error) {
      console.error('Load more products error:', error)
      setLoadingMore(false)

      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  }

  // 加载轮播图配置(F-OPS-001:后台运营配置动态读取,时间窗过期/停用由服务端过滤)
  const loadBanners = async () => {
    const result = await ConfigService.getActiveBanners()
    if (result.success) {
      setBanners(result.data)
    } else {
      console.error('Load banners failed:', result.error.message)
      // 加载失败保持空列表,轮播位隐藏(空态)
      setBanners([])
    }
  }

  // 加载公告配置(F-OPS-002:已发布且在时间窗内、置顶优先/发布时间倒序由服务端过滤;无公告时隐藏公告栏)
  const loadNotices = async () => {
    const result = await NoticeService.getActiveNotices()
    if (result.success) {
      setNotices(result.data)
    } else {
      console.error('Load notices failed:', result.error.message)
      setNotices([])
    }
  }

  const handleNoticeClick = (notice: Notice) => {
    Taro.navigateTo({
      url: `/pages/notice-detail/index?id=${notice.id}`
    })
  }

  // 轮播图切换
  const handleSwiperChange = (e: any) => {
    setCurrentSlide(e.detail.current)
  }

  const handleProductClick = (product: ExtendedProduct) => {
    Taro.navigateTo({
      url: `/pages/product-detail/index?id=${product.id}`
    })
  }

  const handleCreateOrder = () => {
    // 点击 + 号悬浮按钮，先选择地址
    Taro.navigateTo({
      url: '/pages/address-list/index?select=true'
    })
  }

  const handleMenuClick = (item: any) => {
    const { url } = item
    if (url) {
      Taro.navigateTo({ url })
    }
  }

  const handleSearch = (query: string) => {
    console.log('Search query:', query)
    // 埋点:首页搜索(SEARCH)
    if (query.trim()) {
      userActionTracker.track('SEARCH', 'home-search', {
        elementType: 'input',
        elementText: '搜索框',
        properties: { searchQuery: query.trim() }
      })
    }
    // TODO: Implement search functionality
    // You can filter products based on the query
  }

  const handleCartClick = () => {
    Taro.navigateTo({
      url: '/pages/cart/index'
    })
  }

  const handleProfileClick = () => {
    Taro.navigateTo({
      url: '/pages/my/index'
    })
  }

  const handleCategoryToggle = () => {
    Taro.showActionSheet({
      itemList: ['全部商品', '矿泉水', '纯净水', '饮料'],
      success: (res) => {
        console.log('Selected category:', res.tapIndex)
        // TODO: Implement category filtering
      }
    })
  }

  // 轮播图数据:后台运营配置动态读取(F-OPS-001,G18 硬编码替换)
  const carouselData = banners

  const handleBannerClick = (item: Banner) => {
    if (item.linkType === 'PRODUCT') {
      Taro.navigateTo({
        url: `/pages/product-detail/index?id=${item.linkTarget}`
      })
    } else {
      Taro.navigateTo({
        url: item.linkTarget
      })
    }
  }

  return (
    <View className='home-page'>
      {/* 顶部公告栏(无生效公告时隐藏;展示第 1 条=置顶优先/发布时间倒序,AC-OPS-002-02) */}
      {notices.length > 0 && (
        <View className='notice-bar' onClick={() => handleNoticeClick(notices[0])}>
          <Text className='notice-bar-label'>公告</Text>
          <Text className='notice-bar-title' numberOfLines={1}>
            {notices[0].title}
          </Text>
        </View>
      )}

      {/* 顶部轮播图(未配置/无生效条目时隐藏) */}
      {carouselData.length > 0 && (
        <View className='carousel-section'>
          <Swiper
            className='carousel-swiper'
            indicatorColor='rgba(255, 255, 255, 0.4)'
            indicatorActiveColor='white'
            indicatorDots
            circular
            autoplay
            interval={3000}
            duration={500}
            onChange={handleSwiperChange}
          >
            {carouselData.map((item) => (
              <SwiperItem key={item.id} className='carousel-swiper-item' onClick={() => handleBannerClick(item)}>
                <View className='carousel-slide'>
                  <View className='carousel-background'>
                    <Image
                      src={item.imageUrl}
                      className='carousel-bg-image'
                      mode='aspectFill'
                    />
                    <View className='carousel-overlay' />
                  </View>
                  <View className='carousel-content'>
                    <Text className='carousel-title'>{item.title}</Text>
                    <Text className='carousel-subtitle'>{item.subtitle}</Text>
                    <AtButton
                      type='primary'
                      size='small'
                      onClick={() => handleBannerClick(item)}
                      className='carousel-button'
                    >
                      查看详情
                    </AtButton>
                  </View>
                </View>
              </SwiperItem>
            ))}
          </Swiper>
        </View>
      )}

      <AtDivider content='推荐产品' />

      {/* 产品网格 */}
      <View className='product-section'>
        {loading ? (
          <View className='loading-container'>
            <Text>加载中...</Text>
          </View>
        ) : error ? (
          <View className='error-container'>
            <Text>{error}</Text>
            <AtButton
              type='primary'
              size='small'
              onClick={refreshProducts}
              className='retry-button'
            >
              重试
            </AtButton>
          </View>
        ) : products.length === 0 ? (
          <View className='empty-container'>
            <Text>暂无产品</Text>
          </View>
        ) : (
          <>
            <View className='product-grid'>
              {products.map((product) => {
                // 促销价展示(R-MKT-02 单价链):特价/熟客价/新人价 命中且有效价低于目录价时优先展示
                const source = product.priceSource
                const hasDiscountedPrice =
                  (source === 'GROUP_PRICE' || source === 'FLASH_SALE_PRICE' || source === 'NEWCOMER_PRICE') &&
                  product.effectivePrice != null &&
                  product.effectivePrice < product.price
                const displayPrice = hasDiscountedPrice ? product.effectivePrice : product.price
                const priceTag =
                  source === 'FLASH_SALE_PRICE' ? '特价' : source === 'NEWCOMER_PRICE' ? '新人价' : source === 'GROUP_PRICE' ? '熟客价' : ''

                return (
                  <View
                    key={product.id}
                    className='product-card'
                    onClick={() => handleProductClick(product)}
                  >
                    <View className='product-image-container'>
                      <Image
                        src={product.image}
                        mode='aspectFill'
                        className='product-image'
                      />
                    </View>
                    <View className='product-content'>
                      <Text className='product-name'>{product.name}</Text>
                      <Text className='product-desc' numberOfLines={2}>{product.description}</Text>
                      <View className='product-bottom'>
                        <View className='product-price-wrap'>
                          <Text className='product-price'>{displayCents(displayPrice)}</Text>
                          {hasDiscountedPrice && priceTag && (
                            <Text className='product-group-tag'>{priceTag}</Text>
                          )}
                        </View>
                        <Text className='product-stock'>库存{product.stock}</Text>
                      </View>
                    </View>
                  </View>
                )
              })}
            </View>

            {/* 加载更多组件 */}
            {hasMore && (
              <View className='load-more-container'>
                <AtLoadMore
                  status={loadingMore ? 'loading' : 'more'}
                  onClick={loadMoreProducts}
                />
              </View>
            )}

            {/* 无更多数据提示 */}
            {!hasMore && products.length > 0 && (
              <View className='no-more-container'>
                <Text>没有更多产品了</Text>
              </View>
            )}
          </>
        )}
      </View>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />

      {/* 购物车悬浮按钮(F-PRO-004:本地购物车多商品结算入口) */}
      <View className='fab-button fab-cart-button'>
        <AtFab onClick={handleCartClick} size='small'>
          <Text className='fab-icon'>🛒</Text>
        </AtFab>
      </View>

      {/* + 号悬浮按钮 */}
      <View className='fab-button'>
        <AtFab onClick={handleCreateOrder}>
          <Text className='fab-icon'>+</Text>
        </AtFab>
      </View>
    </View>
  )
}

export default HomePage