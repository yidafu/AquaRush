import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image, navigator } from '@tarojs/components'
import { AtGrid, AtLoadMore } from 'taro-ui'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import FavoriteService from '../../services/FavoriteService'
import { authService } from '../../utils/auth'
import { displayCents } from '../../utils/money'
import BasePageLayout from '../../components/BasePageLayout'

import "taro-ui/dist/style/components/grid.scss"
import "taro-ui/dist/style/components/load-more.scss"
import './index.scss'

interface FavoriteProduct {
  id: string
  name: string
  subtitle?: string
  price: string
  originalPrice?: string
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

const FavoritesPage: React.FC = () => {
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
      className='favorites-page'
      safeArea={true}
    >

      {/* Header with count */}
      <View className='favorites-header'>
        <Text className='count-text'>共 {totalElements} 件收藏</Text>
      </View>

      {/* Products list */}
      <View className='product-list'>
        {products.map((product, index) => (
          <View
            key={product.id}
            className='product-item'
            onClick={() => handleProductClick(product.id)}
          >
            <Image
              src={product.coverImageUrl}
              className='product-thumb'
              mode='aspectFill'
              lazyLoad
            />
            <View className='product-details'>
              <Text className='product-name'>{product.name}</Text>
              <View className='product-meta'>
                <Text className='product-price'>¥{displayCents(product.price)}</Text>
                {product.salesVolume > 0 && (
                  <Text className='product-sales'>已售{product.salesVolume}</Text>
                )}
              </View>
              {product.stock <= 0 && (
                <Text className='out-of-stock-label'>暂时缺货</Text>
              )}
            </View>
          </View>
        ))}
      </View>

      {/* Load more indicator */}
      {loadingMore && (
        <AtLoadMore
          status='loading'
          loadingText='加载更多...'
        />
      )}

      {/* No more data indicator */}
      {!hasMore && products.length > 0 && (
        <AtLoadMore
          status='noMore'
          noMoreText='没有更多了'
        />
      )}
    </BasePageLayout>
  )
}

export default FavoritesPage
