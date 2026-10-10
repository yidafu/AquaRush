import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import CouponService from '../../services/CouponService'
import { authService } from '../../utils/auth'
import {
  CouponScope,
  UserCouponStatus,
  UserCouponVo,
  formatDateTime,
  getCouponDiscountText,
  getCouponStatusLabel,
  getCouponThresholdText
} from '@aquarush/common'
import BasePageLayout, { EmptyState } from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

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
 * 我的券包(F-MKT-003,coupon-mine.html:WeUI tabs + 券卡齿孔形态)
 * 按状态 Tab(.weui-tabs)查看已领取优惠券(可用/已用/已退回/已过期);
 * 可用券卡带「去使用」入口,非可用置灰只读(.weui-tag--done)。
 */

// 适用范围文案(CouponScope → 展示文案,展示层映射)
const scopeText = (scope: CouponScope): string =>
  scope === CouponScope.PRODUCT ? '指定商品' : '全场通用'

// 面额大字:cash → ¥10.00;折扣 → 8.5折(数值走 common marketing 文案解析,展示层拆分)
const parseDenomination = (coupon: UserCouponVo): { symbol: string; int: string; dec: string; unit?: string } => {
  const text = getCouponDiscountText(coupon)
  if (text.endsWith('折')) {
    const rate = text.slice(0, -1)
    const [int, dec = ''] = rate.split('.')
    return { symbol: '', int, dec, unit: '折' }
  }
  const amount = text.replace(/^立减/, '')
  const [int, dec = '00'] = amount.split('.')
  return { symbol: '¥', int, dec }
}

const CouponListPage: React.FC = () => {
  usePageViewTracking()

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

  // 「去使用」:回首页选购(coupon-mine.html 可用券 CTA;券在确认订单页选择器中消费)
  const goUseCoupon = useCallback(() => {
    Taro.switchTab({ url: '/pages/home/index' })
  }, [])

  const filtered = coupons.filter(coupon => coupon.status === activeTab)

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadCoupons}
      className='coupon-list-page'
    >
      {/* 状态 Tab(coupon-mine.html .weui-tabs 下划线式) */}
      <View className='weui-tabs coupon-tabs'>
        {TABS.map(tab => (
          <View
            key={tab.key}
            className={`weui-tabs__item ${activeTab === tab.key ? 'on' : ''}`}
            trackTarget='coupon-list-tab'
            trackElementType='tab'
            trackElementText={tab.label}
            onClick={() => setActiveTab(tab.key)}
          >
            <Text>{tab.label}</Text>
          </View>
        ))}
      </View>

      {filtered.length === 0 ? (
        // 仅列表区域展示空态，保留 Tab 以便切换其他状态
        <EmptyState
          title='暂无优惠券'
          subtitle='去领券中心领取更多优惠'
          actionText='去领券'
          onAction={goCouponCenter}
        />
      ) : (
        /* 优惠券列表(券卡:面额区 + 齿孔 + 券信息 + 状态) */
        <View className='coupon-list'>
          {filtered.map(coupon => {
            const available = coupon.status === UserCouponStatus.RECEIVED
            const { symbol, int, dec, unit } = parseDenomination(coupon)
            return (
              <View
                key={coupon.id}
                className={`coupon-card ${available ? '' : 'coupon-card-muted'}`}
              >
                <View className='coupon-face'>
                  <View className='weui-price weui-price--lg coupon-face-price'>
                    {symbol && <Text className='weui-price__symbol'>{symbol}</Text>}
                    <Text className='weui-price__int'>{int}</Text>
                    {dec && <Text className='weui-price__dec'>.{dec}</Text>}
                    {unit && <Text className='weui-price__dec'>{unit}</Text>}
                  </View>
                  <Text className='coupon-face-threshold'>{getCouponThresholdText(coupon.thresholdCents)}</Text>
                </View>
                <View className='coupon-notch coupon-notch-top' />
                <View className='coupon-notch coupon-notch-bottom' />
                <View className='coupon-divider' />
                <View className='coupon-info'>
                  <View className='coupon-info-main'>
                    <View className='coupon-name-row'>
                      <Text className='coupon-name'>{coupon.name}</Text>
                      <Text className={`weui-tag ${available ? 'coupon-type-tag' : ''}`}>
                        {getCouponStatusLabel(coupon.status)}
                      </Text>
                    </View>
                    <Text className='coupon-scope'>适用范围:{scopeText(coupon.scope)}</Text>
                    <Text className='coupon-validity'>有效期至 {formatDateTime(coupon.expireTime)}</Text>
                  </View>
                  {available ? (
                    <View
                      trackTarget="coupon-use"
                      trackElementType="button"
                      trackElementText="去使用"
                      trackProps={{ couponId: coupon.id }}
                      onClick={goUseCoupon}
                      className="weui-btn weui-btn_xs coupon-use-btn"
                    >
                      去使用
                    </View>
                  ) : (
                    <Text className='weui-tag weui-tag--done coupon-status-tag'>
                      {getCouponStatusLabel(coupon.status)}
                    </Text>
                  )}
                </View>
              </View>
            )
          })}
        </View>
      )}

      {/* 已展示全部(coupon-mine.html .weui-loadmore) */}
      {filtered.length > 0 && (
        <View className='weui-loadmore'>
          <View className='weui-loadmore__line' />
          <Text>已展示全部{activeTab === UserCouponStatus.RECEIVED ? '可用' : ''}券</Text>
          <View className='weui-loadmore__line' />
        </View>
      )}
    </BasePageLayout>
  )
}

export default CouponListPage
