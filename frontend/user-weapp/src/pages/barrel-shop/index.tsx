import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import BarrelService from '../../services/BarrelService'
import { authService } from '../../utils/auth'
import { BarrelFeeVo, displayCents, normalizeCents } from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

/**
 * 买水桶(G06:一次性塑料水桶费,独立支付,每用户总限购一次)
 * 价目按规格独立配置(与商品解耦);已购买过(PAID)后不可再购,由「我的水桶」查看。
 * 视觉:WeUI cells 价目行 + .weui-stepper 选数量 + 底部 .mp-footbar 合计购买栏。
 */
const BarrelShopPage: React.FC = () => {
  usePageViewTracking()

  const [fees, setFees] = useState<BarrelFeeVo[]>([])
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [purchasing, setPurchasing] = useState<boolean>(false)

  const loadFees = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await BarrelService.getInstance().getBarrelShop()
      if (result.success) {
        const list = result.data ?? []
        setFees(list)
        setQuantities(Object.fromEntries(list.map((fee) => [fee.id, 1])))
      } else {
        setError(result.error?.message || '获取水桶费价目失败')
      }
    } catch (e) {
      console.error('加载水桶费价目失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      loadFees()
    } else {
      setLoading(false)
    }
  })

  const totalCents = fees.reduce(
    (sum, fee) => sum + normalizeCents(fee.priceCents) * (quantities[fee.id] ?? 0),
    0
  )

  const changeQuantity = useCallback((id: string, delta: number) => {
    setQuantities((prev) => {
      const next = Math.max(1, Math.min(99, (prev[id] ?? 1) + delta))
      return { ...prev, [id]: next }
    })
  }, [])

  const handlePurchase = useCallback(() => {
    const items = fees
      .map((fee) => ({ barrelFeeId: fee.id, quantity: quantities[fee.id] ?? 1 }))
      .filter((item) => item.quantity > 0)
    if (items.length === 0) {
      Taro.showToast({ title: '请选择要购买的水桶', icon: 'none' })
      return
    }
    Taro.showModal({
      title: '确认购买',
      content: `水桶费为一次性塑料水桶费用,合计 ${displayCents(totalCents)},购买后一般不可退。确定购买吗?`,
      success: (res) => {
        if (res.confirm) {
          doPurchase(items)
        }
      }
    })
  }, [fees, quantities, totalCents])

  const doPurchase = useCallback(async (items: Array<{ barrelFeeId: string; quantity: number }>) => {
    setPurchasing(true)
    try {
      const result = await BarrelService.getInstance().purchaseBarrel(items)
      if (!result.success || !result.data) {
        Taro.showToast({ title: result.error?.message || '购买失败，请重试', icon: 'none' })
        return
      }
      const purchase = result.data

      // 桩实现:支付功能待接入
      if (purchase.requiresSimulation) {
        Taro.showToast({ title: '支付功能待接入（微信支付SDK未配置）', icon: 'none' })
        setTimeout(() => {
          Taro.navigateTo({ url: '/pages/barrel-mine/index' })
        }, 1500)
        return
      }

      // 测试环境假支付:服务端已自动完成,直接跳转我的水桶
      if (purchase.paymentParams && (purchase.paymentParams as Record<string, any>).mock) {
        Taro.showToast({ title: '支付成功（模拟支付自动完成）', icon: 'success' })
        setTimeout(() => {
          Taro.navigateTo({ url: '/pages/barrel-mine/index' })
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
          Taro.navigateTo({ url: '/pages/barrel-mine/index' })
        } catch (e) {
          console.error('微信支付失败:', e)
          Taro.showToast({ title: '支付失败，请重试', icon: 'none' })
        }
        return
      }

      Taro.showToast({ title: '购买成功', icon: 'success' })
      Taro.navigateTo({ url: '/pages/barrel-mine/index' })
    } catch (e) {
      console.error('购买水桶失败:', e)
      Taro.showToast({ title: '购买失败，请重试', icon: 'none' })
    } finally {
      setPurchasing(false)
    }
  }, [])

  const goMyBarrel = useCallback(() => {
    Taro.navigateTo({ url: '/pages/barrel-mine/index' })
  }, [])

  const [totalInt, totalDec = '00'] = displayCents(totalCents).split('.')

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadFees}
      empty={fees.length === 0 && !loading}
      emptyTitle='暂无可购水桶'
      emptySubtitle='运营暂未配置水桶费价目'
      className='barrel-shop-page'
    >
      {/* 头部:标题 + 我的水桶入口 */}
      <View className='barrel-shop-header'>
        <Text className='barrel-shop-title'>买水桶</Text>
        <Text
          className='barrel-shop-link'
          trackTarget='barrel-shop-mine'
          trackElementType='link'
          trackElementText='我的水桶'
          onClick={goMyBarrel}
        >
          我的水桶 ›
        </Text>
      </View>

      {/* 说明(brand notice,圆角) */}
      <View className='mp-pad-16 notice-wrap'>
        <View className='weui-notice weui-notice--brand barrel-notice'>
          <Text className='barrel-notice-text'>
            水桶费为一次性塑料水桶费用,独立支付(与商品订单不捆绑),每用户总限购一次,一般不可退。
          </Text>
        </View>
      </View>

      {/* 价目列表(weui cells 行:名称 + 金额 + .weui-stepper 选数量) */}
      <View className='barrel-list'>
        {fees.map((fee) => {
          const qty = quantities[fee.id] ?? 1
          const [intPart, decPart = '00'] = displayCents(normalizeCents(fee.priceCents)).split('.')
          return (
            <View key={fee.id} className='weui-cells barrel-cells'>
              <View className='weui-cell'>
                <View className='weui-cell__bd'>
                  <Text className='barrel-name'>{fee.specification} 水桶</Text>
                </View>
                <View className='weui-cell__ft barrel-cell-ft'>
                  <View className='weui-price weui-price--md'>
                    <Text className='weui-price__symbol'>¥</Text>
                    <Text className='weui-price__int'>{intPart}</Text>
                    <Text className='weui-price__dec'>.{decPart}</Text>
                  </View>
                  <View className='weui-stepper'>
                    <View
                      className={`weui-stepper__btn ${qty <= 1 ? 'disabled' : ''}`}
                      onClick={() => changeQuantity(fee.id, -1)}
                    >
                      <Text>−</Text>
                    </View>
                    <Text className='weui-stepper__num'>{qty}</Text>
                    <View className='weui-stepper__btn' onClick={() => changeQuantity(fee.id, 1)}>
                      <Text>+</Text>
                    </View>
                  </View>
                </View>
              </View>
            </View>
          )
        })}
      </View>

      {/* 底部合计与购买(.mp-footbar 固定栏) */}
      <View className='barrel-footer mp-footbar hair-t'>
        <View className='mp-footbar__info barrel-footer-total'>
          <Text className='mp-footbar__lbl'>合计</Text>
          <View className='weui-price weui-price--lg'>
            <Text className='weui-price__symbol'>¥</Text>
            <Text className='weui-price__int'>{totalInt}</Text>
            <Text className='weui-price__dec'>.{totalDec}</Text>
          </View>
        </View>
        <View
          trackTarget='barrel-buy'
          trackElementType='button'
          trackElementText={purchasing ? '购买中...' : '购买水桶'}
          trackDisabled={purchasing}
          onClick={() => {
            if (!purchasing) handlePurchase()
          }}
          className={`weui-btn weui-btn_primary ${purchasing ? 'weui-btn_disabled' : ''}`}
        >
          {purchasing ? '购买中...' : '购买水桶'}
        </View>
      </View>
      <View className='safe-bottom' />
    </BasePageLayout>
  )
}

export default BarrelShopPage
