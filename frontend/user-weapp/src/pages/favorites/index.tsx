import React, { useState, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { pageTheme } from '../../styles/theme.stylex'
import FavoriteService from '../../services/FavoriteService'
import { authService } from '../../utils/auth'
import { displayCents } from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

/**
 * 我的收藏样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)'
  },
  // 缩略图(product-list 原型 144×144,圆角走 .weui-media-box__thumb)
  thumb: {
    display: 'block'
  },
  // 价格行:.weui-price 居左 + 已售居右(favorites.html row between)
  priceRow: {
    marginTop: '16rpx',
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    columnGap: '16rpx'
  },
  sales: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)'
  },
  // 缺货标签行
  tagRow: {
    marginTop: '12rpx',
    display: 'flex'
  }
})

interface FavoriteProduct {
  id: string
  name: string
  subtitle?: string
  priceCents: number
  originalPriceCents?: number
  coverImageUrl: string
  stock: number
  salesVolume: number
  status: string
  addedAt: string
}

interface PageInfo {
  total: number
  pageSize: number
  pageNum: number
  hasNext: boolean
  hasPrevious: boolean
  totalPages: number
}

/**
 * 我的收藏(favorites.html:WeUI 收藏列表)
 * .weui-cells__title 统计 + .weui-media-box 商品行(缩略图 + 名称 + 价格 + 已售/缺货);
 * 分页加载(useReachBottom)+ 下拉刷新;useDidShow 检查登录态并刷新。
 */
const FavoritesPage: React.FC = () => {
  usePageViewTracking()

  const [products, setProducts] = useState<FavoriteProduct[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [totalElements, setTotalElements] = useState<number>(0)
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null)

  const favoriteService = FavoriteService.getInstance()

  const loadFavorites = useCallback(async (page: number = 0, append: boolean = false) => {
    try {
      if (page === 0) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }

      const result = await favoriteService.getFavoriteProducts(page, 20)

      const newProducts = result.list.map(product => ({
        ...product,
        image: product.coverImageUrl
      }))

      if (append) {
        setProducts(prev => [...prev, ...newProducts])
      } else {
        setProducts(newProducts)
      }

      setTotalElements(result.pageInfo.total)
      setPageInfo(result.pageInfo)
      setHasMore(result.pageInfo.hasNext)
      setCurrentPage(page)
    } catch (error) {
      console.error('Failed to load favorites:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [favoriteService])

  const handleRefresh = useCallback(() => {
    loadFavorites(0, false)
  }, [loadFavorites])

  const handleLoadMore = useCallback(() => {
    if (hasMore && !loadingMore) {
      loadFavorites(currentPage + 1, true)
    }
  }, [hasMore, loadingMore, currentPage, loadFavorites])

  const handleProductClick = useCallback((productId: string) => {
    Taro.navigateTo({
      url: `/pages/product-detail/index?id=${productId}`
    })
  }, [])

  const handleGoHome = useCallback(() => {
    Taro.switchTab({
      url: '/pages/home/index'
    })
  }, [])

  const handleGoLogin = useCallback(() => {
    Taro.navigateTo({
      url: '/pages/my/index'
    })
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      handleRefresh()
    } else {
      setLoading(false)
    }
  })

  useReachBottom(() => {
    handleLoadMore()
  })

  usePullDownRefresh(async () => {
    await handleRefresh()
    Taro.stopPullDownRefresh()
  })

  return (
    <BasePageLayout
      requireAuth={true}
      onLoginClick={handleGoLogin}
      loading={loading}
      error={null}
      empty={products.length === 0 && !loading}
      emptyTitle='暂无收藏'
      emptySubtitle='快去收藏喜欢的商品吧'
      emptyActionText='去逛逛'
      onEmptyAction={handleGoHome}
      rootStyles={[pageTheme, styles.page]}
      safeArea={true}
    >
      {/* 页头统计(favorites.html .weui-cells__title) */}
      <View className='weui-cells__title'>共 {totalElements} 件收藏</View>

      {/* 收藏列表(.weui-cells 内 .weui-media-box 行,整卡可点击进详情) */}
      <View className='weui-cells'>
        {products.map((product) => {
          const [intPart, decPart = '00'] = displayCents(product.priceCents).split('.')
          return (
            <View
              key={product.id}
              className='weui-media-box'
              trackTarget='favorites-item'
              trackElementType='card'
              trackElementText={product.name}
              onClick={() => handleProductClick(product.id)}
            >
              <View className='weui-media-box__hd'>
                <Image
                  src={product.coverImageUrl}
                  {...props('weui-media-box__thumb', styles.thumb)}
                  mode='aspectFill'
                  lazyLoad
                />
              </View>
              <View className='weui-media-box__bd'>
                <Text className='weui-media-box__title'>{product.name}</Text>
                {product.subtitle && (
                  <Text className='weui-media-box__desc'>{product.subtitle}</Text>
                )}
                <View {...stylex.props(styles.priceRow)}>
                  <View className='weui-price weui-price--md'>
                    <Text className='weui-price__symbol'>¥</Text>
                    <Text className='weui-price__int'>{intPart}</Text>
                    <Text className='weui-price__dec'>.{decPart}</Text>
                  </View>
                  {product.salesVolume > 0 && (
                    <Text {...stylex.props(styles.sales)}>已售 {product.salesVolume}</Text>
                  )}
                </View>
                {product.stock <= 0 && (
                  <View {...stylex.props(styles.tagRow)}>
                    <Text className='weui-tag weui-tag--orange'>暂时缺货</Text>
                  </View>
                )}
              </View>
            </View>
          )
        })}
      </View>

      {/* 加载更多 / 没有更多(.weui-loadmore 双短线) */}
      {(loadingMore || (!hasMore && products.length > 0)) && (
        <View className='weui-loadmore'>
          <View className='weui-loadmore__line' />
          <Text>{loadingMore ? '加载更多…' : '没有更多了'}</Text>
          <View className='weui-loadmore__line' />
        </View>
      )}
    </BasePageLayout>
  )
}

export default FavoritesPage
