import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import CouponService from '../../services/CouponService'
import { authService } from '../../utils/auth'
import {
  CouponScope,
  CouponTemplateVo,
  getCouponDiscountText,
  getCouponThresholdText,
  getCouponTypeLabel,
  getValidityText
} from '@aquarush/common'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

/**
 * 领券中心(F-MKT-003,coupon-center.html:券卡橙红品牌元素)
 * 展示可领取的优惠券模板(仅自动领取),点击领取后刷新已领数量。
 * 券卡:左侧面额区(橙红 tint 底 + .weui-price--lg)+ 齿孔/虚线分割 + 券名/类型/适用范围/有效期 + 立即领取。
 */

// 适用范围文案(CouponScope → 展示文案,展示层映射)
const scopeText = (scope: CouponScope): string =>
  scope === CouponScope.PRODUCT ? '指定商品' : '全场通用'

// 面额大字:cash → ¥10.00;折扣 → 8.5折(数值走 common marketing 文案解析,展示层拆分)
const parseDenomination = (template: CouponTemplateVo): { symbol: string; int: string; dec: string; unit?: string } => {
  const text = getCouponDiscountText(template)
  if (text.endsWith('折')) {
    const rate = text.slice(0, -1)
    const [int, dec = ''] = rate.split('.')
    return { symbol: '', int, dec, unit: '折' }
  }
  const amount = text.replace(/^立减/, '')
  const [int, dec = '00'] = amount.split('.')
  return { symbol: '¥', int, dec }
}

const CouponCenterPage: React.FC = () => {
  usePageViewTracking()

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
      emptySubtitle='新券上线后会第一时间通知你'
      className='coupon-center-page'
    >
      {/* 头部:我的券包入口(coupon-center.html .weui-cells__title 行) */}
      <View className='coupon-center-header'>
        <Text className='coupon-center-title'>领券中心</Text>
        <Text
          className='coupon-center-link'
          trackTarget='coupon-center-mine'
          trackElementType='link'
          trackElementText='我的券包'
          onClick={goMyCoupons}
        >
          我的券包 ›
        </Text>
      </View>

      {/* 优惠券模板列表(券卡:面额区 + 齿孔 + 券信息 + 领取按钮) */}
      <View className='coupon-list'>
        {templates.map(template => {
          const claimedFull = isClaimedFull(template)
          const { symbol, int, dec, unit } = parseDenomination(template)
          return (
            <View key={template.id} className='coupon-card'>
              <View className='coupon-face'>
                <View className='weui-price weui-price--lg coupon-face-price'>
                  {symbol && <Text className='weui-price__symbol'>{symbol}</Text>}
                  <Text className='weui-price__int'>{int}</Text>
                  {dec && <Text className='weui-price__dec'>.{dec}</Text>}
                  {unit && <Text className='weui-price__dec'>{unit}</Text>}
                </View>
                <Text className='coupon-face-threshold'>{getCouponThresholdText(template.thresholdCents)}</Text>
              </View>
              <View className='coupon-notch coupon-notch-top' />
              <View className='coupon-notch coupon-notch-bottom' />
              <View className='coupon-divider' />
              <View className='coupon-info'>
                <View className='coupon-info-main'>
                  <View className='coupon-name-row'>
                    <Text className='coupon-name'>{template.name}</Text>
                    <Text className='weui-tag coupon-type-tag'>{getCouponTypeLabel(template.couponType)}</Text>
                  </View>
                  <Text className='coupon-scope'>适用范围:{scopeText(template.scope)}</Text>
                  <Text className='coupon-validity'>有效期:{getValidityText(template)}</Text>
                </View>
                <View className='coupon-claim-wrap'>
                  {claimedFull ? (
                    <View className='weui-btn weui-btn_mini weui-btn_disabled'>
                      已领完
                    </View>
                  ) : (
                    <View
                      trackTarget="coupon-claim"
                      trackElementType="button"
                      trackElementText={claimingId === template.id ? '领取中...' : '立即领取'}
                      trackProps={{ couponTemplateId: template.id }}
                      onClick={() => handleClaim(template)}
                      className={`weui-btn weui-btn_mini coupon-claim-btn ${claimingId === template.id ? 'weui-btn_disabled' : ''}`}
                    >
                      {claimingId === template.id ? '领取中...' : '立即领取'}
                    </View>
                  )}
                  <Text className='coupon-claimed-text'>
                    已领 {template.claimedCount}/{template.perUserLimit}
                  </Text>
                </View>
              </View>
            </View>
          )
        })}
      </View>

      {/* 券使用规则(coupon-center.html 底部说明) */}
      <View className='weui-cells__title'>券使用规则</View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View className='weui-cell__bd coupon-rules'>
            <Text className='coupon-rule'>· 同一订单仅可使用 1 张优惠券,不与折扣券叠加。</Text>
            <Text className='coupon-rule'>· 满足门槛(满 X 元)方可使用,门槛按商品小计计算。</Text>
            <Text className='coupon-rule'>· 已领取券请在有效期内使用,过期自动作废不可恢复。</Text>
            <Text className='coupon-rule'>· 订单取消 / 退款后,未过期券将退回券包。</Text>
          </View>
        </View>
      </View>
    </BasePageLayout>
  )
}

export default CouponCenterPage
