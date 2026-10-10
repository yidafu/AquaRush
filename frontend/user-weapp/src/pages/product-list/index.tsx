import React, { useState, useCallback, useEffect } from 'react'
import { View, Text, Input } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh, useShareAppMessage } from '@tarojs/taro'
import ProductService from '../../services/ProductService'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { userActionTracker } from '../../utils/tracking'
import EmptyState from '../../components/EmptyState'

// 子组件
import ProductCard from './components/ProductCard'
import SortBar from './components/SortBar'
import type { ProductListItem } from './components/constants'

import './index.scss'

/**
 * 全部商品列表页(product-list.html:WeUI 搜索页)
 * 首页「更多商品 > 更多」入口(以及后续搜索入口)落地页。
 * - 支持关键词搜索(服务端过滤,activeProducts.keyword)
 * - 排序:综合(最新上架) / 销量 / 价格(asc↔desc 切换,.weui-segmented)
 * - 分页:上拉加载更多(useReachBottom,.weui-loadmore),下拉刷新
 * - 促销价展示走 R-MKT-02 单价链(priceSource/effectivePriceCents,见 utils/productPrice)
 * 渲染由 components/ 完成。
 */

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

  const handleClearFilter = useCallback(() => {
    setKeyword('')
    loadProducts(0, false, { keyword: '' })
  }, [loadProducts])

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

  return (
    <View className='product-list-page'>
      {/* 搜索栏(关键词过滤,服务端 activeProducts.keyword;product-list.html .weui-searchbar) */}
      <View className='search-bar-wrap'>
        <View className='weui-searchbar'>
          <View
            className='weui-searchbar__box start'
            trackTarget='product-list-search'
            trackElementType='input'
            trackElementText='搜索商品'
          >
            <View className='search-icon' />
            <Input
              className='search-input'
              value={keyword}
              onInput={(e) => setKeyword(e.detail.value)}
              onConfirm={handleSearch}
              placeholder='搜索商品名称'
              confirmType='search'
            />
          </View>
        </View>
      </View>

      {/* 排序分段控件(综合/销量/价格)+ 总数 */}
      <SortBar
        activeSort={activeSort}
        priceAsc={priceAsc}
        total={total}
        onSortClick={handleSortClick}
      />

      {/* 商品网格(2 列,.mp-grid2) */}
      {loading ? (
        <View className='weui-loadmore weui-loadmore_loading'>
          <View className='spinner' />
          <Text>加载中...</Text>
        </View>
      ) : error ? (
        <EmptyState
          icon='⚠️'
          title='加载失败'
          description={error}
          actionText='重试'
          onAction={() => loadProducts(0, false)}
        />
      ) : products.length === 0 ? (
        <EmptyState
          icon='🔍'
          title='没有找到相关商品'
          description={keyword ? '换个关键词或调整筛选条件试试' : '商品正在上架中，敬请期待'}
          actionText={keyword ? '清空筛选' : undefined}
          onAction={keyword ? handleClearFilter : undefined}
        />
      ) : (
        <View className='mp-grid2'>
          {products.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              onClick={() => handleProductClick(p)}
            />
          ))}
        </View>
      )}

      {/* 加载更多指示(.weui-loadmore 双短线) */}
      {!loading && !error && products.length > 0 && (
        <View className='weui-loadmore'>
          <View className='weui-loadmore__line' />
          <Text>{loadingMore ? '加载更多…' : '没有更多了'}</Text>
          <View className='weui-loadmore__line' />
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default ProductListPage
