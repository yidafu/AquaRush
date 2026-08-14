import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import CouponService from '../../services/CouponService'
import { authService } from '../../utils/auth'
import { CouponTemplateVo } from '@aquarush/common'
import {
  getCouponDiscountText,
  getCouponThresholdText,
  getCouponTypeLabel,
  getValidityText
} from '../../utils/marketing'
import BasePageLayout from '../../components/BasePageLayout'

import './index.scss'

/**
 * 领券中心(F-MKT-003)
 * 展示可领取的优惠券模板(仅自动领取),点击领取后刷新已领数量。
 */
const CouponCenterPage: React.FC = () => {
  const [templates, setTemplates] = useState<CouponTemplateVo[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [claimingId, setClaimingId] = useState<string | null>(null)

  const loadCoupons = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await CouponService.getInstance().getCouponCenter()
      if (result.success) {
        setTemplates(result.data ?? [])
      } else {
        setError(result.error?.message || '获取优惠券列表失败')
      }
    } catch (e) {
      console.error('加载领券中心失败:', e)
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

  const handleClaim = useCallback(async (template: CouponTemplateVo) => {
    if (claimingId) return
    setClaimingId(template.id)
    try {
      const result = await CouponService.getInstance().claimCoupon(template.id)
      if (result.success) {
        Taro.showToast({ title: '领取成功', icon: 'success' })
        loadCoupons()
      } else {
        Taro.showToast({ title: result.error?.message || '领取失败，请重试', icon: 'none' })
      }
    } catch (e) {
      console.error('领取优惠券失败:', e)
      Taro.showToast({ title: '领取失败，请重试', icon: 'none' })
    } finally {
      setClaimingId(null)
    }
  }, [claimingId, loadCoupons])

  const goMyCoupons = useCallback(() => {
    Taro.navigateTo({ url: '/pages/coupon-list/index' })
  }, [])

  const isClaimedFull = (template: CouponTemplateVo): boolean =>
    template.claimedCount >= template.perUserLimit

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadCoupons}
      empty={templates.length === 0 && !loading}
      emptyTitle='暂无可用优惠券'
      emptySubtitle='敬请期待更多优惠活动'
      className='coupon-center-page'
    >
      {/* 头部:我的券包入口 */}
      <View className='coupon-center-header'>
        <Text className='coupon-center-title'>领券中心</Text>
        <Text className='coupon-center-link' onClick={goMyCoupons}>我的券包 ›</Text>
      </View>

      {/* 优惠券模板列表 */}
      <View className='coupon-list'>
        {templates.map(template => {
          const claimedFull = isClaimedFull(template)
          return (
            <View key={template.id} className='coupon-card'>
              <View className='coupon-card-body'>
                <View className='coupon-card-left'>
                  <Text className='coupon-type'>{getCouponTypeLabel(template.couponType)}</Text>
                  <Text className='coupon-discount'>{getCouponDiscountText(template)}</Text>
                  <Text className='coupon-threshold'>{getCouponThresholdText(template.thresholdCents)}</Text>
                </View>
                <View className='coupon-card-right'>
                  <Text className='coupon-validity'>{getValidityText(template)}</Text>
                  <View className='coupon-claim-wrap'>
                    {claimedFull ? (
                      <View className='coupon-claim-btn coupon-claim-btn-disabled'>
                        <Text className='coupon-claim-text'>已领完</Text>
                      </View>
                    ) : (
                      <View
                        className='coupon-claim-btn'
                        onClick={() => handleClaim(template)}
                      >
                        <Text className='coupon-claim-text'>
                          {claimingId === template.id ? '领取中...' : '领取'}
                        </Text>
                      </View>
                    )}
                  </View>
                </View>
              </View>
              <View className='coupon-card-footer'>
                <Text className='coupon-claimed-text'>
                  已领 {template.claimedCount}/{template.perUserLimit}
                </Text>
              </View>
            </View>
          )
        })}
      </View>
    </BasePageLayout>
  )
}

export default CouponCenterPage
