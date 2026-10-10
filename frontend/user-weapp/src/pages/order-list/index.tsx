import React, { useCallback, useState } from 'react'
import { View, Text } from '@tarojs/components'
import { AtToast } from 'taro-ui'
import Taro, { useReady, useDidShow, usePullDownRefresh, useReachBottom } from '@tarojs/taro'
import { ORDER_TABS, OrderStatus, normalizeCents, usePagedList } from '@aquarush/common'
import { userActionTracker } from '../../utils/tracking'
import OrderService from '../../services/OrderService'

// 子组件
import OrderTabs from './components/OrderTabs'
import OrderCardItem from './components/OrderCardItem'
import OrderEmptyState from './components/OrderEmptyState'
import type { OrderItem, OrderProduct } from './components/constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/tag.scss"
import './index.scss'

const PAGE_SIZE = 10

/**
 * 订单列表页(F-ORD-004 多商品下单)
 * 分页状态机/刷新/加载更多由 @aquarush/common 的 usePagedList 共享,Tab 定义来自
 * common ORDER_TABS(全部/待付款/待配送/配送中/已完成);页面保留 Tab 路由定位、
 * 行映射与事件处理(支付/取消/再次购买)。
 */
const OrderListPage: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<number>(0)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  /**
   * 商品 id 归一化(PrimaryId 传输为字符串)
   */
  const productId = useCallback((product: any): string => String(product.id), [])

  /**
   * GraphQL Order → 页面订单结构(金额均为分,Long 字符串经 normalizeCents 归一化)
   */
  const mapOrder = useCallback((order: any): OrderItem => {
    const rawItems: any[] = Array.isArray(order.items) && order.items.length > 0 ? order.items : []
    const items: OrderProduct[] = rawItems.map((item: any) => ({
      id: productId(item.product || { id: item.productId }),
      name: item.product?.name || '商品',
      image: item.product?.coverImageUrl || '/assets/default-product.png',
      priceCents: item.unitPriceCents ?? item.product?.priceCents ?? 0,
      specification: item.product?.specification
    }))
    const product = items[0] || null
    // 营销价判定(AC-ORD-002-09):首件成交单价 ≠ 目录价快照即命中(熟客价/特价/新人价 —— R-MKT-01/02)
    const unitPriceCents = normalizeCents(order.unitPriceCents) ?? normalizeCents(product?.priceCents) ?? null
    const hasMarketingPrice = product != null && unitPriceCents != null && unitPriceCents !== normalizeCents(product.priceCents)

    return {
      id: order.id,
      orderNo: order.orderNo,
      status: order.status as OrderStatus,
      product,
      items,
      quantity: order.items && order.items.length > 0 ? order.items[0].quantity : 0,
      unitPriceCents: items[0]?.priceCents ?? null,
      hasMarketingPrice,
      amountCents: order.amountCents,
      deliveryFeeCents: order.deliveryFeeCents,
      payAmountCents: order.payAmountCents,
      promotionDiscountCents: order.promotionDiscountCents ?? null,
      promotionActivityName: order.promotionActivityName || null,
      couponDiscountCents: order.couponDiscountCents ?? null,
      couponName: order.couponName || null,
      firstOrderDiscountCents: order.firstOrderDiscountCents ?? null,
      firstOrderActivityName: order.firstOrderActivityName || null,
      ticketRedeemCount: order.ticketRedeemCount ?? null,
      ticketDiscountCents: order.ticketDiscountCents ?? null,
      address: order.address
        ? {
            receiverName: order.address.receiverName,
            phone: order.address.phone,
            province: order.address.province,
            city: order.address.city,
            district: order.address.district,
            detailAddress: order.address.detailAddress
          }
        : null,
      createTime: order.createdAt || ''
    }
  }, [productId])

  // 共享分页状态机(usePagedList:items/loading/refreshing/loadingMore/hasMore/refresh/loadMore/setParams)
  const { items: orders, loading, loadingMore, hasMore, refresh, loadMore, setParams } = usePagedList<OrderItem>({
    fetchPage: async (page, params) => {
      const statuses = params.statuses as OrderStatus[] | null
      const result = await OrderService.getInstance().getMyOrders(statuses, page, PAGE_SIZE)
      if (!result.success || !result.data) {
        throw new Error(result.error?.message || '获取订单列表失败')
      }
      const { content, totalPages } = result.data
      return {
        items: content.map(order => mapOrder(order)),
        hasMore: page + 1 < totalPages
      }
    },
    initialParams: { statuses: ORDER_TABS[0].statuses }
  })

  // 初始化:从「我的」页订单宫格进入时定位对应 Tab(全部/待付款/待配送/配送中)
  useReady(() => {
    const instance = Taro.getCurrentInstance()
    const tabParam = instance.router?.params?.tab
    const TAB_INDEX_MAP: Record<string, number> = {
      all: 0,
      pending_payment: 1,
      pending_delivery: 2,
      delivering: 3
    }
    const initialTab = typeof tabParam === 'string' && tabParam in TAB_INDEX_MAP ? TAB_INDEX_MAP[tabParam] : 0
    setCurrentTab(initialTab)
    void setParams({ statuses: ORDER_TABS[initialTab].statuses })
  })

  // 页面显示时刷新第一页数据(支付完成/取消后返回即时同步)
  useDidShow(() => {
    userActionTracker.trackPageView()
    void refresh()
  })

  // 下拉刷新
  usePullDownRefresh(() => {
    refresh().finally(() => {
      Taro.stopPullDownRefresh()
    })
  })

  // 上拉加载更多(usePagedList 内部处理 hasMore/busy 守卫)
  useReachBottom(() => {
    void loadMore()
  })

  const handleTabChange = useCallback((value: number) => {
    setCurrentTab(value)
    void setParams({ statuses: ORDER_TABS[value].statuses })
  }, [setParams])

  const handleOrderClick = useCallback((order: OrderItem) => {
    Taro.navigateTo({
      url: `/pages/order-detail/index?orderId=${order.id}`
    })
  }, [])

  const handlePayment = useCallback(async (order: OrderItem) => {
    showToastMessage('正在发起支付...', 'loading')

    const payResult = await OrderService.getInstance().createOrderPayment(order.id)
    if (!payResult.success || !payResult.data) {
      showToastMessage(payResult.error?.message || '支付发起失败', 'error')
      return
    }
    const { autoCompleted, requiresSimulation, paymentParams } = payResult.data

    // 测试环境假支付:服务端已自动完成,直接刷新订单列表
    if (autoCompleted) {
      showToastMessage('支付成功（模拟支付自动完成）', 'success')
      setTimeout(() => refresh(), 800)
      return
    }
    if (requiresSimulation || !paymentParams) {
      showToastMessage('支付功能待接入', 'error')
      return
    }

    try {
      await Taro.requestPayment({
        timeStamp: paymentParams.timeStamp as string,
        nonceStr: paymentParams.nonceStr as string,
        package: paymentParams.package as string,
        signType: paymentParams.signType as 'MD5' | 'HMAC-SHA256' | 'RSA',
        paySign: paymentParams.paySign as string
      })
      showToastMessage('支付成功', 'success')
      setTimeout(() => refresh(), 800)
    } catch (e) {
      console.error('微信支付未完成:', e)
      showToastMessage('支付未完成，请重试', 'error')
    }
  }, [refresh, showToastMessage])

  const handleCancelOrder = useCallback((order: OrderItem) => {
    Taro.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？取消后无法恢复。',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await OrderService.getInstance().cancelOrder(order.id)
            if (!result.success) {
              throw new Error(result.error?.message || '取消订单失败')
            }
            showToastMessage('订单已取消', 'success')

            // 刷新订单列表
            refresh()
          } catch (error) {
            console.error('取消订单失败:', error)
            showToastMessage(error instanceof Error ? error.message : '取消订单失败', 'error')
          }
        }
      }
    })
  }, [refresh, showToastMessage])

  const handleReorder = useCallback((order: OrderItem) => {
    if (!order.product) {
      showToastMessage('订单商品信息缺失', 'error')
      return
    }
    // 写入 pendingOrder 并跳转确认订单页(F-ORD-004:多商品全部明细再次购买)
    const products = order.items.length > 0
      ? order.items.map((p, idx) => ({
          id: p.id,
          name: p.name,
          price: normalizeCents(idx === 0 ? order.unitPriceCents : p.priceCents) ?? 0,
          quantity: idx === 0 ? order.quantity : 1,
          image: p.image,
          specifications: p.specification ? [p.specification] : []
        }))
      : [{
          id: order.product.id,
          name: order.product.name,
          price: normalizeCents(order.unitPriceCents) ?? 0,
          quantity: order.quantity,
          image: order.product.image,
          specifications: order.product.specification ? [order.product.specification] : []
        }]
    const orderData = {
      products,
      totalAmount: normalizeCents(order.payAmountCents) ?? 0
    }

    Taro.setStorageSync('pendingOrder', orderData)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [showToastMessage])

  return (
    <View className='order-list-page'>
      <OrderTabs current={currentTab} onChange={handleTabChange} />

      <View className='order-content'>
        {loading && orders.length === 0 ? (
          <View className='loading-container'>
            <Text>加载中...</Text>
          </View>
        ) : orders.length === 0 ? (
          <OrderEmptyState
            isAllTab={currentTab === 0}
            tabTitle={ORDER_TABS[currentTab].title}
            onGoShopping={() => Taro.switchTab({ url: '/pages/home/index' })}
          />
        ) : (
          <View className='order-list'>
            {orders.map(order => (
              <OrderCardItem
                key={order.id}
                order={order}
                onPay={handlePayment}
                onCancel={handleCancelOrder}
                onReorder={handleReorder}
                onClick={handleOrderClick}
              />
            ))}

            {loadingMore && (
              <View className='loading-more'>
                <Text>加载中...</Text>
              </View>
            )}

            {!hasMore && orders.length > 0 && (
              <View className='no-more'>
                <Text>没有更多订单了</Text>
              </View>
            )}
          </View>
        )}
      </View>

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderListPage
