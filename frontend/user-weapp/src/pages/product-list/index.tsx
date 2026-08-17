import React, { useState, useCallback, useEffect } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtSearchBar, AtLoadMore, AtButton } from 'taro-ui'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh, useShareAppMessage } from '@tarojs/taro'
import ProductService from '../../services/ProductService'
import { displayCents } from '@aquarush/common'
import { syncCartTabBadge } from '../../utils/cart'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { userActionTracker } from '../../utils/tracking'

import "taro-ui/dist/style/components/search-bar.scss"
import "taro-ui/dist/style/components/load-more.scss"
import "taro-ui/dist/style/components/button.scss"
import './index.scss'

/**
 * 全部商品列表页
 * 首页「更多商品 > 更多」入口(以及后续搜索入口)落地页。
 * - 支持关键词搜索(服务端过滤,activeProducts.keyword)
 * - 排序:综合(最新上架) / 销量 / 价格(asc↔desc 切换)
 * - 分页:上拉加载更多(useReachBottom),下拉刷新
 * - 促销价展示走 R-MKT-02 单价链(priceSource/effectivePriceCents)
 */

interface SortTab {
  key: string
  label: string
}

const SORT_TABS: SortTab[] = [
  { key: 'default', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price', label: '价格' }
]

const PAGE_SIZE = 10

// 排序 Tab → 服务端 sortBy/sortDirection(activeProducts 支持 price/salesVolume/sortOrder/createdAt)
const toSortParams = (sortKey: string, priceAsc: boolean): { sortBy: string; sortDirection: string } => {
  switch (sortKey) {
    case 'sales':
      return { sortBy: 'salesVolume', sortDirection: 'desc' }
    case 'price':
      return { sortBy: 'price', sortDirection: priceAsc ? 'asc' : 'desc' }
    default:
      // 综合 = 最新上架
      return { sortBy: 'createdAt', sortDirection: 'desc' }
  }
}

interface ProductListItem {
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

const ProductListPage: React.FC = () => {
  usePageViewTracking()

  const [keyword, setKeyword] = useState<string>('')
  const [activeSort, setActiveSort] = useState<string>('default')
  // 价格排序方向:默认升序,再点切换为降序
  const [priceAsc, setPriceAsc] = useState<boolean>(true)

  const [products, setProducts] = useState<ProductListItem[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [total, setTotal] = useState<number>(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // tabBar 角标同步(不触发数据请求)
    syncCartTabBadge()
  }, [])

  const resolveSort = useCallback((): { sortBy: string; sortDirection: string } => {
    return toSortParams(activeSort, priceAsc)
  }, [activeSort, priceAsc])

  interface LoadOptions {
    sortBy?: string
    sortDirection?: string
    keyword?: string
  }

  /**
   * 加载商品列表(单次 GraphQL 请求;分页加载更多属独立刷新语义,规则 #10 例外)
   * opts 显式覆盖排序/关键词,避免 setState 未生效时的过期闭包。
   */
  const loadProducts = useCallback(async (page: number, append: boolean, opts?: LoadOptions) => {
    const sort = opts?.sortBy
      ? { sortBy: opts.sortBy, sortDirection: opts.sortDirection! }
      : resolveSort()
    const kw = opts?.keyword !== undefined ? opts.keyword : keyword
    try {
      if (append) {
        setLoadingMore(true)
      } else {
        setLoading(true)
        setError(null)
      }

      const result = await ProductService.getInstance().getActiveProducts({
        page,
        size: PAGE_SIZE,
        sortBy: sort.sortBy,
        sortDirection: sort.sortDirection,
        keyword: kw.trim() || undefined
      })

      if (result.success && result.data) {
        const newProducts: ProductListItem[] = result.data.products.map((p: any) => ({
          id: p.id,
          name: p.name,
          subtitle: p.subtitle ?? undefined,
          specification: p.specification ?? '',
          priceCents: p.price,
          effectivePriceCents: p.effectivePrice ?? undefined,
          priceSource: p.priceSource,
          coverImageUrl: p.coverImageUrl ?? '',
          stock: p.stock ?? 0,
          salesVolume: p.salesVolume ?? 0
        }))
        setProducts((prev) => (append ? [...prev, ...newProducts] : newProducts))
        setTotal(result.data.pagination.totalElements)
        setHasMore(!result.data.pagination.last)
        setCurrentPage(page)
      } else {
        throw new Error(result.error?.message || '加载失败')
      }
    } catch (e) {
      console.error('Load product list error:', e)
      if (!append) {
        setError(e instanceof Error ? e.message : '加载失败，请重试')
        Taro.showToast({ title: '加载失败', icon: 'none' })
      }
    } finally {
      if (append) {
        setLoadingMore(false)
      } else {
        setLoading(false)
      }
      Taro.stopPullDownRefresh()
    }
  }, [resolveSort, keyword])

  // 页面显示时刷新(首次 + 回前台,运营上下架即时生效)
  useDidShow(() => {
    loadProducts(0, false)
  })

  // 下拉刷新
  usePullDownRefresh(async () => {
    await loadProducts(0, false)
  })

  // 上拉加载更多
  useReachBottom(() => {
    if (hasMore && !loadingMore && !loading) {
      loadProducts(currentPage + 1, true)
    }
  })

  // 分享
  useShareAppMessage(() => ({
    title: '好喝山泉 · 桶装水配送',
    path: '/pages/product-list/index'
  }))

  const handleSearch = useCallback(() => {
    userActionTracker.track('SEARCH', 'product-list-search', {
      elementType: 'input',
      elementText: '搜索商品',
      properties: { keyword }
    })
    loadProducts(0, false, { keyword })
  }, [keyword, loadProducts])

  const handleSortClick = useCallback((key: string) => {
    userActionTracker.track('CLICK', 'product-list-sort', {
      elementType: 'tab',
      elementText: key,
      properties: { sortBy: key }
    })
    if (key === 'price' && activeSort === 'price') {
      // 价格再点切换升降序(显式传入新排序,避免过期闭包)
      const nextPriceAsc = !priceAsc
      setPriceAsc(nextPriceAsc)
      loadProducts(0, false, toSortParams('price', nextPriceAsc))
    } else {
      setActiveSort(key)
      setPriceAsc(true)
      loadProducts(0, false, toSortParams(key, true))
    }
  }, [activeSort, priceAsc, loadProducts])

  const handleProductClick = useCallback((product: ProductListItem) => {
    userActionTracker.track('CLICK', 'product-list-item', {
      elementType: 'card',
      elementText: product.name,
      properties: { productId: product.id }
    })
    Taro.navigateTo({ url: `/pages/product-detail/index?id=${product.id}` })
  }, [])

  // 促销价展示(R-MKT-02 单价链):命中且低于目录价时展示促销价 + tint 标签
  const getDisplayPrice = (p: ProductListItem): { price: number; tag: string } => {
    const source = p.priceSource
    const hasDiscounted =
      (source === 'GROUP_PRICE' || source === 'FLASH_SALE_PRICE' || source === 'NEWCOMER_PRICE') &&
      p.effectivePriceCents != null &&
      p.effectivePriceCents < p.priceCents
    const tag =
      source === 'FLASH_SALE_PRICE' ? '特价' : source === 'NEWCOMER_PRICE' ? '新人价' : source === 'GROUP_PRICE' ? '熟客价' : ''
    return { price: hasDiscounted && p.effectivePriceCents != null ? p.effectivePriceCents : p.priceCents, tag }
  }

  const renderProductCard = (product: ProductListItem) => {
    const { price, tag } = getDisplayPrice(product)
    const outOfStock = product.stock <= 0
    return (
      <View
        key={product.id}
        className='product-card'
        onClick={() => handleProductClick(product)}
      >
        <View className='product-img-wrap'>
          <Image
            src={product.coverImageUrl}
            mode='aspectFill'
            className='product-img'
            lazyLoad
          />
          {outOfStock && (
            <View className='product-sold-out'>
              <Text className='product-sold-out-text'>暂时缺货</Text>
            </View>
          )}
        </View>
        <View className='product-info'>
          <Text className='product-name' numberOfLines={2}>{product.name}</Text>
          <Text className='product-spec' numberOfLines={1}>{product.subtitle || product.specification}</Text>
          <View className='product-bottom'>
            <View className='product-price-wrap'>
              <Text className='product-price'>{displayCents(price)}</Text>
              {tag && <Text className='product-price-tag'>{tag}</Text>}
            </View>
            {product.salesVolume > 0 && (
              <Text className='product-sales'>已售{product.salesVolume}</Text>
            )}
          </View>
        </View>
      </View>
    )
  }

  return (
    <View className='product-list-page'>
      {/* 搜索栏(关键词过滤,服务端 activeProducts.keyword) */}
      <View className='search-bar-wrap'>
        <AtSearchBar
          value={keyword}
          onChange={(v) => setKeyword(v)}
          onConfirm={handleSearch}
          onActionClick={handleSearch}
          onClear={() => {
            setKeyword('')
            loadProducts(0, false, { keyword: '' })
          }}
          placeholder='搜索商品名称'
          showActionButton
        />
      </View>

      {/* 排序 Tab */}
      <View className='sort-bar'>
        {SORT_TABS.map((tab) => (
          <View
            key={tab.key}
            className={`sort-tab ${activeSort === tab.key ? 'sort-tab-active' : ''}`}
            onClick={() => handleSortClick(tab.key)}
          >
            <Text className='sort-tab-text'>{tab.label}</Text>
            {tab.key === 'price' && activeSort === 'price' && (
              <Text className='sort-tab-arrow'>{priceAsc ? '↑' : '↓'}</Text>
            )}
          </View>
        ))}
        {total > 0 && <Text className='sort-total'>共 {total} 件</Text>}
      </View>

      {/* 商品网格(2 列) */}
      {loading ? (
        <View className='state-box'>
          <Text className='state-text'>加载中...</Text>
        </View>
      ) : error ? (
        <View className='state-box'>
          <Text className='state-title'>加载失败</Text>
          <Text className='state-text'>{error}</Text>
          <AtButton size='small' type='primary' onClick={() => loadProducts(0, false)} className='retry-btn'>重试</AtButton>
        </View>
      ) : products.length === 0 ? (
        <View className='state-box'>
          <Text className='state-icon'>📦</Text>
          <Text className='state-title'>暂无商品</Text>
          <Text className='state-text'>{keyword ? '换个关键词试试吧' : '商品正在上架中，敬请期待'}</Text>
        </View>
      ) : (
        <View className='product-grid'>
          {products.map((p) => renderProductCard(p))}
        </View>
      )}

      {/* 加载更多指示 */}
      {!loading && !error && products.length > 0 && (
        <View className='load-more-wrap'>
          {loadingMore && <AtLoadMore status='loading' loadingText='加载更多...' />}
          {!hasMore && <AtLoadMore status='noMore' noMoreText='没有更多了' />}
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default ProductListPage
