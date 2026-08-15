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

/**
 * 水票商城(F-MKT-007)
 * 在售票种套餐列表(购票即预收款项,一经购买不支持退款 R-MKT-08);
 * 当前支付为桩实现(requiresSimulation),真实微信支付待 SDK 接入。
 */
const TicketShopPage: React.FC = () => {
  usePageViewTracking()

  const [ticketTypes, setTicketTypes] = useState<TicketTypeVo[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [purchasingId, setPurchasingId] = useState<string | null>(null)

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

  const doPurchase = useCallback(async (ticketType: TicketTypeVo) => {
    setPurchasingId(ticketType.id)
    try {
      const result = await TicketService.getInstance().purchaseTickets(ticketType.id, 1)
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
        Taro.showToast({ title: '支付成功（模拟支付自动完成）', icon: 'success' })
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
    Taro.showModal({
      title: '确认购买',
      content: '一经购买不支持退款，确定购买吗？',
      success: (res) => {
        if (res.confirm) {
          doPurchase(ticketType)
        }
      }
    })
  }, [doPurchase])

  const goMyTickets = useCallback(() => {
    Taro.navigateTo({ url: '/pages/ticket-list/index' })
  }, [])

  const isLimitReached = (ticketType: TicketTypeVo): boolean =>
    ticketType.perUserLimit > 0 && ticketType.myPurchasedCount >= ticketType.perUserLimit

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
                    trackProps={{ ticketTypeId: ticketType.id }}
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
            </View>
          )
        })}
      </View>
    </BasePageLayout>
  )
}

export default TicketShopPage