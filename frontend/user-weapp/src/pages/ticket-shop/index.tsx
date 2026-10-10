import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import TicketService from '../../services/TicketService'
import { authService } from '../../utils/auth'
import {
  TicketTypeVo,
  displayCents,
  getValidityText,
  normalizeCents
} from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

// 单次购票张数上限(GraphQL input 与后端均限制 1~10)
const MAX_PACKAGE_COUNT = 10

/**
 * 水票商城(F-MKT-007,ticket-mall.html:票种卡品牌蓝 + .weui-tag 策略标签)
 * 在售票种套餐列表(购票即预收款项,一经购买不支持退款 R-MKT-08);
 * 支持选择购票张数 1~10(.weui-stepper,上限 min(10, 剩余限购)),一次购买按张数生成对应水票记录(每张唯一递增票号);
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
      {/* 头部:标题 + 我的水票入口 */}
      <View className='ticket-shop-header'>
        <Text className='ticket-shop-title'>水票商城</Text>
        <Text
          className='ticket-shop-link'
          trackTarget='ticket-shop-mine'
          trackElementType='link'
          trackElementText='我的水票'
          onClick={goMyTickets}
        >
          我的水票 ›
        </Text>
      </View>

      {/* 通兑/限规格说明(ticket-mall.html .weui-notice--brand) */}
      <View className='mp-pad-16 notice-wrap'>
        <View className='weui-notice weui-notice--brand ticket-notice'>
          <Text className='ticket-notice-text'>通兑票按规格限定,跨容量不通用;票号可在我的水票查看</Text>
        </View>
      </View>

      {/* 水票套餐列表(票种卡:名称+策略标签 / 适用规格 / 有效期 / 限购 / 价格+购买) */}
      <View className='ticket-list'>
        {ticketTypes.map(ticketType => {
          const limitReached = isLimitReached(ticketType)
          const count = quantityOf(ticketType)
          const max = maxQuantity(ticketType)
          const totalCents = normalizeCents(ticketType.packagePriceCents) * count
          const [intPart, decPart = '00'] = displayCents(normalizeCents(ticketType.packagePriceCents)).split('.')
          return (
            <View key={ticketType.id} className='ticket-card'>
              {/* 标题行:票种名 + 策略标签(G97:买N送M .weui-tag--red) */}
              <View className='ticket-title-row'>
                <Text className='ticket-name'>{ticketType.name}</Text>
                <View className='ticket-tags'>
                  {ticketType.giftCount > 0 && (
                    <Text className='weui-tag weui-tag--red weui-tag--md'>买 {ticketType.packageCount} 送 {ticketType.giftCount}</Text>
                  )}
                  {ticketType.specification && (
                    <Text className='weui-tag weui-tag--brand'>限 {ticketType.specification}</Text>
                  )}
                </View>
              </View>

              {/* 说明行:有效期 / 限购 */}
              <View className='ticket-meta'>
                <Text className='ticket-meta-line'>有效期:{getValidityText(ticketType)}</Text>
                <Text className='ticket-meta-line'>
                  {ticketType.perUserLimit === 0
                    ? '限购:不限'
                    : `限购:每人 ${ticketType.perUserLimit} 份(已购 ${ticketType.myPurchasedCount})`}
                </Text>
              </View>

              {/* 价格行:套餐价 + 立即购买 */}
              <View className='ticket-price-row'>
                <View className='weui-price weui-price--lg'>
                  <Text className='weui-price__symbol'>¥</Text>
                  <Text className='weui-price__int'>{intPart}</Text>
                  <Text className='weui-price__dec'>.{decPart}</Text>
                </View>
                <View
                  trackTarget="ticket-buy"
                  trackElementType="button"
                  trackElementText={purchasingId === ticketType.id ? '购买中...' : limitReached ? '已购满' : '立即购买'}
                  trackDisabled={limitReached || purchasingId === ticketType.id}
                  trackProps={{ ticketTypeId: ticketType.id, packageCount: count }}
                  onClick={() => {
                    if (!limitReached && purchasingId !== ticketType.id) handlePurchase(ticketType)
                  }}
                  className={`weui-btn weui-btn_mini weui-btn_primary ${limitReached || purchasingId === ticketType.id ? 'weui-btn_disabled' : ''}`}
                >
                  {purchasingId === ticketType.id ? '购买中...' : limitReached ? '已购满' : '立即购买'}
                </View>
              </View>

              {/* 购票张数选择(1~10,.weui-stepper,一次购买按张数生成对应水票记录) */}
              <View className='ticket-quantity-row hair-t'>
                <Text className='ticket-quantity-label'>购票张数</Text>
                <View className='weui-stepper'>
                  <View
                    className={`weui-stepper__btn ${count <= 1 ? 'disabled' : ''}`}
                    onClick={() => changeQuantity(ticketType, -1)}
                  >
                    <Text>−</Text>
                  </View>
                  <Text className='weui-stepper__num'>{count}</Text>
                  <View
                    className={`weui-stepper__btn ${count >= max ? 'disabled' : ''}`}
                    onClick={() => changeQuantity(ticketType, 1)}
                  >
                    <Text>+</Text>
                  </View>
                </View>
                <Text className='ticket-quantity-total'>合计 {displayCents(totalCents)}</Text>
              </View>
            </View>
          )
        })}
      </View>

      {/* 购票须知(ticket-mall.html .weui-cells__tips) */}
      <View className='weui-cells__title'>购票须知</View>
      <View className='weui-cells__tips'>· 水票为电子票,购票成功后逐张生成唯一票号入账票夹。</View>
      <View className='weui-cells__tips'>· 有效期自购票成功之日起算,过期作废不可使用。</View>
      <View className='weui-cells__tips'>· 单次购票张数上限 10 张,超出按限购校验拦截。</View>
    </BasePageLayout>
  )
}

export default TicketShopPage
