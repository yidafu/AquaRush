import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import CouponService from '../../services/CouponService'
import { authService } from '../../utils/auth'
import { UserCouponStatus, UserCouponVo, formatDateTime } from '@aquarush/common'
import {
  getCouponDiscountText,
  getCouponStatusLabel,
  getCouponThresholdText
} from '../../utils/marketing'
import BasePageLayout from '../../components/BasePageLayout'

import './index.scss'

interface CouponTab {
  key: UserCouponStatus
  label: string
}

const TABS: CouponTab[] = [
  { key: UserCouponStatus.RECEIVED, label: '可用' },
  { key: UserCouponStatus.USED, label: '已用' },
  { key: UserCouponStatus.RETURNED, label: '已退回' },
  { key: UserCouponStatus.EXPIRED, label: '已过期' }
]

/**
 * 我的券包(F-MKT-003)
 * 按状态 Tab 查看已领取优惠券(可用/已用/已退回/已过期)。
 */
const CouponListPage: React.FC = () => {
  const [coupons, setCoupons] = useState<UserCouponVo[]>([])
  const [activeTab, setActiveTab] = useState<UserCouponStatus>(UserCouponStatus.RECEIVED)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadCoupons = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await CouponService.getInstance().getMyCoupons()
      if (result.success) {
        setCoupons(result.data ?? [])
      } else {
        setError(result.error?.message || '获取我的券包失败')
      }
    } catch (e) {
      console.error('加载我的券包失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      loadCoupons()
    } else {
      setLoading(false)
    }
  })

  const goCouponCenter = useCallback(() => {
    Taro.navigateTo({ url: '/pages/coupon-center/index' })
  }, [])

  const filtered = coupons.filter(coupon => coupon.status === activeTab)

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadCoupons}
      empty={filtered.length === 0 && !loading}
      emptyTitle='暂无优惠券'
      emptySubtitle='去领券中心领取更多优惠'
      emptyActionText='去领券'
      onEmptyAction={goCouponCenter}
      className='coupon-list-page'
    >
      {/* 状态 Tab */}
      <View className='coupon-tabs'>
        {TABS.map(tab => (
          <View
            key={tab.key}
            className={`coupon-tab ${activeTab === tab.key ? 'coupon-tab-active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            <Text className='coupon-tab-text'>{tab.label}</Text>
          </View>
        ))}
      </View>

      {/* 优惠券列表 */}
      <View className='coupon-list'>
        {filtered.map(coupon => (
          <View key={coupon.id} className='coupon-card'>
            <View className='coupon-card-body'>
              <View className='coupon-card-left'>
                <Text className='coupon-name'>{coupon.name}</Text>
                <Text className='coupon-discount'>{getCouponDiscountText(coupon)}</Text>
                <Text className='coupon-threshold'>{getCouponThresholdText(coupon.thresholdCents)}</Text>
              </View>
              <View className='coupon-card-right'>
                <Text className='coupon-expire'>有效期至 {formatDateTime(coupon.expireTime)}</Text>
                <Text className='coupon-status'>{getCouponStatusLabel(coupon.status)}</Text>
              </View>
            </View>
          </View>
        ))}
      </View>
    </BasePageLayout>
  )
}

export default CouponListPage
