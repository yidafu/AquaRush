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
          {/* 购买状态卡 */}
          <View className='barrel-mine-status'>
            <Text className='barrel-mine-status-text'>{statusText(purchase.paymentStatus)}</Text>
            <Text className='barrel-mine-order-no'>购桶单 {purchase.orderNo}</Text>
          </View>

          {/* 购买明细 */}
          <View className='barrel-mine-card'>
            <View className='barrel-mine-card-header'>
              <Text className='barrel-mine-card-title'>购买明细</Text>
              <Text className='barrel-mine-card-total'>{displayCents(normalizeCents(purchase.totalAmountCents))}</Text>
            </View>
            {purchase.items.map((item, idx) => (
              <View key={idx} className='barrel-mine-item'>
                <Text className='barrel-mine-item-name'>{item.specification}</Text>
                <Text className='barrel-mine-item-qty'>{item.quantity} × {displayCents(normalizeCents(item.unitPriceCents))}</Text>
                <Text className='barrel-mine-item-amount'>{displayCents(normalizeCents(item.amountCents))}</Text>
              </View>
            ))}
          </View>

          {/* 时间信息 */}
          <View className='barrel-mine-card'>
            <View className='barrel-mine-meta'>
              <Text className='barrel-mine-meta-label'>下单时间</Text>
              <Text className='barrel-mine-meta-value'>
                {purchase.createdAt ? new Date(purchase.createdAt).toLocaleString('zh-CN') : '-'}
              </Text>
            </View>
            <View className='barrel-mine-meta'>
              <Text className='barrel-mine-meta-label'>支付时间</Text>
              <Text className='barrel-mine-meta-value'>
                {purchase.paidAt ? new Date(purchase.paidAt).toLocaleString('zh-CN') : '-'}
              </Text>
            </View>
            {purchase.paymentStatus === BarrelPaymentStatus.REFUNDED && (
              <View className='barrel-mine-meta'>
                <Text className='barrel-mine-meta-label'>退还时间</Text>
                <Text className='barrel-mine-meta-value'>
                  {purchase.refundedAt ? new Date(purchase.refundedAt).toLocaleString('zh-CN') : '-'}
                </Text>
              </View>
            )}
          </View>

          <View className='barrel-mine-note'>
            <Text className='barrel-mine-note-text'>
              水桶归您所有,一般不可退;特殊情况(退桶/停止服务)请联系门店管理员处理。
            </Text>
          </View>
        </>
      ) : (
        <View className='barrel-mine-empty-action'>
          <View
            trackTarget='barrel-go-shop'
            trackElementType='button'
            trackElementText='去买水桶'
            onClick={goShop}
            className='barrel-mine-buy-btn'
          >
            <Text className='barrel-mine-buy-text'>去买水桶</Text>
          </View>
        </View>
      )}
    </BasePageLayout>
  )
}

export default BarrelMinePage
