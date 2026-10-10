import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import BarrelService from '../../services/BarrelService'
import { authService } from '../../utils/auth'
import { BarrelPurchaseVo, BarrelPaymentStatus, displayCents, normalizeCents } from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

/**
 * 我的水桶(G06:一次性塑料水桶费,独立支付)
 * 展示当前用户购桶单(状态/明细/金额/时间);未购买显示空态并提供「去买水桶」入口。
 * 视觉:WeUI cells 结构(状态行 / 购买明细 / 时间信息),金额统一 .weui-price。
 */
const BarrelMinePage: React.FC = () => {
  usePageViewTracking()

  const [purchase, setPurchase] = useState<BarrelPurchaseVo | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadPurchase = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await BarrelService.getInstance().getMyBarrelPurchase()
      if (result.success) {
        setPurchase(result.data)
      } else {
        setError(result.error?.message || '获取我的水桶失败')
      }
    } catch (e) {
      console.error('加载我的水桶失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      loadPurchase()
    } else {
      setLoading(false)
    }
  })

  const goShop = useCallback(() => {
    Taro.navigateTo({ url: '/pages/barrel-shop/index' })
  }, [])

  const statusText = (status: BarrelPaymentStatus): string => {
    switch (status) {
      case BarrelPaymentStatus.PAID:
        return '已支付'
      case BarrelPaymentStatus.PENDING:
        return '待支付'
      case BarrelPaymentStatus.CANCELLED:
        return '已取消'
      case BarrelPaymentStatus.REFUNDED:
        return '已退还'
      default:
        return status
    }
  }

  const [totalInt, totalDec = '00'] = purchase
    ? displayCents(normalizeCents(purchase.totalAmountCents)).split('.')
    : ['', '']

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadPurchase}
      empty={!purchase && !loading}
      emptyTitle='暂未购买水桶'
      emptySubtitle='水桶费为一次性塑料水桶费用,购买后长期使用'
      className='barrel-mine-page'
    >
      {purchase ? (
        <>
          {/* 购买状态行(weui cells:状态 + 购桶单号) */}
          <View className='weui-cells mt2'>
            <View className='weui-cell'>
              <View className='weui-cell__bd'>
                <Text className='barrel-mine-status-text'>{statusText(purchase.paymentStatus)}</Text>
              </View>
              <View className='weui-cell__ft'>
                <Text className='barrel-mine-order-no'>购桶单 {purchase.orderNo}</Text>
              </View>
            </View>
          </View>

          {/* 购买明细(weui cells:明细行 + 合计) */}
          <View className='weui-cells__title'>购买明细</View>
          <View className='weui-cells'>
            {purchase.items.map((item, idx) => (
              <View key={idx} className='weui-cell'>
                <View className='weui-cell__bd'>
                  <Text>{item.specification}</Text>
                </View>
                <View className='weui-cell__ft'>
                  <Text className='barrel-mine-item-qty'>{item.quantity} × {displayCents(normalizeCents(item.unitPriceCents))}</Text>
                </View>
              </View>
            ))}
            <View className='weui-cell'>
              <View className='weui-cell__bd barrel-mine-total-label'>
                <Text>合计</Text>
              </View>
              <View className='weui-cell__ft'>
                <View className='weui-price weui-price--md'>
                  <Text className='weui-price__symbol'>¥</Text>
                  <Text className='weui-price__int'>{totalInt}</Text>
                  <Text className='weui-price__dec'>.{totalDec}</Text>
                </View>
              </View>
            </View>
          </View>

          {/* 时间信息(weui cells) */}
          <View className='weui-cells__title'>订单信息</View>
          <View className='weui-cells'>
            <View className='weui-cell'>
              <View className='weui-cell__bd'>
                <Text>下单时间</Text>
              </View>
              <View className='weui-cell__ft'>
                <Text>{purchase.createdAt ? new Date(purchase.createdAt).toLocaleString('zh-CN') : '-'}</Text>
              </View>
            </View>
            <View className='weui-cell'>
              <View className='weui-cell__bd'>
                <Text>支付时间</Text>
              </View>
              <View className='weui-cell__ft'>
                <Text>{purchase.paidAt ? new Date(purchase.paidAt).toLocaleString('zh-CN') : '-'}</Text>
              </View>
            </View>
            {purchase.paymentStatus === BarrelPaymentStatus.REFUNDED && (
              <View className='weui-cell'>
                <View className='weui-cell__bd'>
                  <Text>退还时间</Text>
                </View>
                <View className='weui-cell__ft'>
                  <Text>{purchase.refundedAt ? new Date(purchase.refundedAt).toLocaleString('zh-CN') : '-'}</Text>
                </View>
              </View>
            )}
          </View>

          <View className='weui-cells__tips barrel-mine-note'>
            水桶归您所有,一般不可退;特殊情况(退桶/停止服务)请联系门店管理员处理。
          </View>
        </>
      ) : (
        <View className='barrel-mine-empty-action'>
          <View
            trackTarget='barrel-go-shop'
            trackElementType='button'
            trackElementText='去买水桶'
            onClick={goShop}
            className='weui-btn weui-btn_primary barrel-mine-buy-btn'
          >
            去买水桶
          </View>
        </View>
      )}
    </BasePageLayout>
  )
}

export default BarrelMinePage
