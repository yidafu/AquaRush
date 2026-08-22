import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import TicketService from '../../services/TicketService'
import { authService } from '../../utils/auth'
import { TicketTypeVo, displayCents, normalizeCents } from '@aquarush/common'
import { getValidityText } from '../../utils/marketing'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

// 单次购票张数上限(GraphQL input 与后端均限制 1~10)
const MAX_PACKAGE_COUNT = 10

/**
 * 水票商城(F-MKT-007)
 * 在售票种套餐列表(购票即预收款项,一经购买不支持退款 R-MKT-08);
 * 支持选择购票张数 1~10(上限 min(10, 剩余限购)),一次购买按张数生成对应水票记录(每张唯一递增票号);
 * 当前支付为桩实现(requiresSimulation),真实微信支付待 SDK 接入。
 */
const TicketShopPage: React.FC = () => {
  usePageViewTracking()

  const [ticketTypes, setTicketTypes] = useState<TicketTypeVo[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [purchasingId, setPurchasingId] = useState<string | null>(null)
  // 各票种已选张数(key=ticketType.id,默认 1)
  const [quantities, setQuantities] = useState<Record<string, number>>({})

  const loadTicketTypes = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await TicketService.getInstance().getTicketShop()
      if (result.success) {
        setTicketTypes(result.data ?? [])
      } else {
        setError(result.error?.message || '获取水票商城列表失败')
      }
    } catch (e) {
      console.error('加载水票商城失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      loadTicketTypes()
    } else {
      setLoading(false)
    }
  })

  const isLimitReached = (ticketType: TicketTypeVo): boolean =>
    ticketType.perUserLimit > 0 && ticketType.myPurchasedCount >= ticketType.perUserLimit

  /** 可购张数上限:min(10, 剩余限购);不限购时 10 */
  const maxQuantity = (ticketType: TicketTypeVo): number => {
    if (ticketType.perUserLimit > 0) {
      const remaining = ticketType.perUserLimit - ticketType.myPurchasedCount
      return Math.max(0, Math.min(MAX_PACKAGE_COUNT, remaining))
    }
    return MAX_PACKAGE_COUNT
  }

  const quantityOf = (ticketType: TicketTypeVo): number => {
    const q = quantities[ticketType.id] ?? 1
    return Math.min(q, maxQuantity(ticketType))
  }

  const changeQuantity = (ticketType: TicketTypeVo, delta: number): void => {
    setQuantities(prev => {
      const next = Math.min(maxQuantity(ticketType), Math.max(1, (prev[ticketType.id] ?? 1) + delta))
      return { ...prev, [ticketType.id]: next }
    })
  }

  const doPurchase = useCallback(async (ticketType: TicketTypeVo, count: number) => {
    setPurchasingId(ticketType.id)
    try {
      const result = await TicketService.getInstance().purchaseTickets(ticketType.id, count)
      if (!result.success || !result.data) {
        Taro.showToast({ title: result.error?.message || '购买失败，请重试', icon: 'none' })
        return
      }

      const purchase = result.data

      // 桩实现:支付功能待接入
      if (purchase.requiresSimulation) {
        Taro.showToast({ title: '支付功能待接入（微信支付SDK未配置）', icon: 'none' })
        setTimeout(() => {
          Taro.navigateTo({ url: '/pages/ticket-list/index' })
        }, 1500)
        return
      }

      // 测试环境假支付:服务端已自动完成,直接跳转水票列表
      if (purchase.paymentParams && (purchase.paymentParams as Record<string, any>).mock) {
        Taro.showToast({ title: `支付成功，已生成 ${purchase.totalCount} 张水票`, icon: 'success' })
        setTimeout(() => {
          Taro.navigateTo({ url: '/pages/ticket-list/index' })
        }, 1200)
        return
      }

      // 真实支付:发起微信支付
      if (purchase.paymentParams) {
        try {
          const params = purchase.paymentParams as Record<string, any>
          await Taro.requestPayment({
            timeStamp: params.timeStamp as string,
            nonceStr: params.nonceStr as string,
            package: params.package as string,
            signType: params.signType as 'MD5' | 'HMAC-SHA256' | 'RSA',
            paySign: params.paySign as string
          })
          Taro.showToast({ title: '支付成功', icon: 'success' })
          Taro.navigateTo({ url: '/pages/ticket-list/index' })
        } catch (e) {
          console.error('微信支付失败:', e)
          Taro.showToast({ title: '支付失败，请重试', icon: 'none' })
        }
        return
      }

      Taro.showToast({ title: '购买成功', icon: 'success' })
      Taro.navigateTo({ url: '/pages/ticket-list/index' })
    } catch (e) {
      console.error('购买水票失败:', e)
      Taro.showToast({ title: '购买失败，请重试', icon: 'none' })
    } finally {
      setPurchasingId(null)
    }
  }, [])

  const handlePurchase = useCallback((ticketType: TicketTypeVo) => {
    const count = quantityOf(ticketType)
    if (count <= 0) {
      Taro.showToast({ title: '已达限购张数', icon: 'none' })
      return
    }
    const totalCents = normalizeCents(ticketType.packagePriceCents) * count
    Taro.showModal({
      title: '确认购买',
      content: `购买 ${ticketType.name} ${count} 张，合计 ${displayCents(totalCents)}；一经购买不支持退款，确定购买吗？`,
      success: (res) => {
        if (res.confirm) {
          doPurchase(ticketType, count)
        }
      }
    })
  }, [quantities]) // eslint-disable-line react-hooks/exhaustive-deps

  const goMyTickets = useCallback(() => {
    Taro.navigateTo({ url: '/pages/ticket-list/index' })
  }, [])

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadTicketTypes}
      empty={ticketTypes.length === 0 && !loading}
      emptyTitle='暂无在售水票'
      emptySubtitle='敬请期待更多水票套餐'
      className='ticket-shop-page'
    >
      {/* 头部:我的水票入口 */}
      <View className='ticket-shop-header'>
        <Text className='ticket-shop-title'>水票商城</Text>
        <Text className='ticket-shop-link' onClick={goMyTickets}>我的水票 ›</Text>
      </View>

      {/* 水票套餐列表 */}
      <View className='ticket-list'>
        {ticketTypes.map(ticketType => {
          const limitReached = isLimitReached(ticketType)
          const count = quantityOf(ticketType)
          const max = maxQuantity(ticketType)
          const totalCents = normalizeCents(ticketType.packagePriceCents) * count
          return (
            <View key={ticketType.id} className='ticket-card'>
              <View className='ticket-card-body'>
                <View className='ticket-card-info'>
                  <Text className='ticket-name'>{ticketType.name}</Text>
                  <View className='ticket-count-row'>
                    <Text className='ticket-count'>{ticketType.packageCount}张</Text>
                    {ticketType.giftCount > 0 && (
                      <Text className='ticket-gift'>送{ticketType.giftCount}</Text>
                    )}
                  </View>
                  <Text className='ticket-validity'>{getValidityText(ticketType)}</Text>
                  <Text className='ticket-limit'>
                    {ticketType.perUserLimit === 0
                      ? '不限购'
                      : `已购 ${ticketType.myPurchasedCount}/${ticketType.perUserLimit}`}
                  </Text>
                </View>
                <View className='ticket-card-right'>
                  <Text className='ticket-price'>{displayCents(normalizeCents(ticketType.packagePriceCents))}</Text>
                  <View
                    trackTarget="ticket-buy"
                    trackElementType="button"
                    trackElementText={purchasingId === ticketType.id ? '购买中...' : limitReached ? '已购满' : '购买'}
                    trackDisabled={limitReached}
                    trackProps={{ ticketTypeId: ticketType.id, packageCount: count }}
                    onClick={() => {
                      if (!limitReached) handlePurchase(ticketType)
                    }}
                    className={`ticket-buy-btn ${limitReached ? 'ticket-buy-btn-disabled' : ''}`}
                  >
                    <Text className='ticket-buy-text'>
                      {purchasingId === ticketType.id ? '购买中...' : limitReached ? '已购满' : '购买'}
                    </Text>
                  </View>
                </View>
              </View>
              {/* 购票张数选择(1~10,一次购买按张数生成对应水票记录) */}
              <View className='ticket-quantity-row'>
                <Text className='ticket-quantity-label'>购票张数</Text>
                <View className='ticket-stepper'>
                  <View
                    className={`ticket-stepper-btn ${count <= 1 ? 'ticket-stepper-btn-disabled' : ''}`}
                    onClick={() => changeQuantity(ticketType, -1)}
                  >
                    <Text className='ticket-stepper-icon'>−</Text>
                  </View>
                  <Text className='ticket-stepper-value'>{count}</Text>
                  <View
                    className={`ticket-stepper-btn ${count >= max ? 'ticket-stepper-btn-disabled' : ''}`}
                    onClick={() => changeQuantity(ticketType, 1)}
                  >
                    <Text className='ticket-stepper-icon'>+</Text>
                  </View>
                </View>
                <Text className='ticket-quantity-total'>合计 {displayCents(totalCents)}</Text>
              </View>
            </View>
          )
        })}
      </View>
    </BasePageLayout>
  )
}

export default TicketShopPage
