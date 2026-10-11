import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme } from '../../styles/theme.stylex'
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

/**
 * 领券中心样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用;
 * 券卡面额/类型标签/领取按钮用 tokens.action,即设计系统 §2.1 券例外橙红,
 * 非券元素用品牌蓝)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box'
  },
  // ---- 头部:标题 + 我的券包入口 ----
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: '32rpx',
    paddingRight: '32rpx',
    paddingBottom: '16rpx',
    paddingLeft: '32rpx'
  },
  title: {
    fontSize: '32rpx',
    fontWeight: 600,
    color: 'var(--weui-FG-0)'
  },
  link: {
    fontSize: '26rpx',
    color: 'var(--weui-LINK)'
  },
  // ---- 券卡列表(白底圆角卡,面额区 + 齿孔 + 虚线分割 + 券信息) ----
  list: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16rpx',
    paddingTop: '0',
    paddingRight: '16rpx',
    paddingBottom: '0',
    paddingLeft: '16rpx'
  },
  card: {
    position: 'relative',
    display: 'flex',
    alignItems: 'stretch',
    backgroundColor: 'var(--weui-BG-2)',
    borderRadius: 'var(--weui-radius)',
    overflow: 'hidden'
  },
  // 左侧面额区(104px → 208rpx,橙红 tint 底)
  face: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: '208rpx',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: '8rpx',
    paddingTop: '44rpx',
    paddingRight: '0',
    paddingBottom: '44rpx',
    paddingLeft: '0',
    backgroundColor: 'var(--coupon-primary-light)',
    boxSizing: 'border-box'
  },
  // 券面额:优惠券品牌橙红(§2.1 券例外)
  facePrice: {
    color: tokens.action
  },
  faceThreshold: {
    fontSize: '24rpx',
    color: tokens.action
  },
  // 齿孔(分割线上下两枚圆形遮罩,透出页面底色)
  notch: {
    position: 'absolute',
    left: '208rpx',
    transform: 'translateX(-50%)',
    width: '28rpx',
    height: '28rpx',
    borderRadius: '50%',
    backgroundColor: 'var(--weui-BG-0)',
    zIndex: 1
  },
  notchTop: {
    top: '-14rpx'
  },
  notchBottom: {
    bottom: '-14rpx'
  },
  // 虚线分割
  divider: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: '0%',
    width: '0',
    borderLeftWidth: '2rpx',
    borderLeftStyle: 'dashed',
    borderLeftColor: 'var(--weui-FG-3)',
    marginTop: '28rpx',
    marginBottom: '28rpx'
  },
  // 右侧券信息区
  info: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '24rpx',
    paddingTop: '32rpx',
    paddingRight: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '32rpx',
    boxSizing: 'border-box'
  },
  infoMain: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    display: 'flex',
    flexDirection: 'column',
    rowGap: '8rpx'
  },
  nameRow: {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    columnGap: '12rpx',
    rowGap: '12rpx'
  },
  name: {
    fontSize: '30rpx',
    fontWeight: 600,
    color: 'var(--weui-FG-0)'
  },
  // 类型标签:券品牌橙红(§2.1 券例外)
  typeTag: {
    backgroundColor: 'var(--coupon-primary-light)',
    color: tokens.action
  },
  scope: {
    fontSize: '24rpx',
    color: 'var(--weui-FG-1)'
  },
  validity: {
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)'
  },
  // 领取按钮(券品牌橙红)+ 已领进度
  claimWrap: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    rowGap: '12rpx'
  },
  claimBtn: {
    // 领取按钮:券品牌橙红(§2.1 券例外);禁用态不挂本类,背景由 weui-btn_disabled 提供
    backgroundColor: tokens.action,
    color: tokens.onBrand
  },
  claimedText: {
    fontSize: '20rpx',
    color: 'var(--weui-FG-2)'
  },
  // ---- 券使用规则 ----
  rules: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '8rpx',
    fontSize: '26rpx',
    lineHeight: 1.5,
    color: 'var(--weui-FG-1)'
  }
})

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
      rootStyles={[pageTheme, styles.page]}
    >
      {/* 头部:我的券包入口(coupon-center.html .weui-cells__title 行) */}
      <View {...stylex.props(styles.header)}>
        <Text {...stylex.props(styles.title)}>领券中心</Text>
        <Text
          {...stylex.props(styles.link)}
          trackTarget='coupon-center-mine'
          trackElementType='link'
          trackElementText='我的券包'
          onClick={goMyCoupons}
        >
          我的券包 ›
        </Text>
      </View>

      {/* 优惠券模板列表(券卡:面额区 + 齿孔 + 券信息 + 领取按钮) */}
      <View {...stylex.props(styles.list)}>
        {templates.map(template => {
          const claimedFull = isClaimedFull(template)
          const { symbol, int, dec, unit } = parseDenomination(template)
          return (
            <View key={template.id} {...stylex.props(styles.card)}>
              <View {...stylex.props(styles.face)}>
                <View {...props('weui-price', 'weui-price--lg', styles.facePrice)}>
                  {symbol && <Text className='weui-price__symbol'>{symbol}</Text>}
                  <Text className='weui-price__int'>{int}</Text>
                  {dec && <Text className='weui-price__dec'>.{dec}</Text>}
                  {unit && <Text className='weui-price__dec'>{unit}</Text>}
                </View>
                <Text {...stylex.props(styles.faceThreshold)}>{getCouponThresholdText(template.thresholdCents)}</Text>
              </View>
              <View {...props(styles.notch, styles.notchTop)} />
              <View {...props(styles.notch, styles.notchBottom)} />
              <View {...stylex.props(styles.divider)} />
              <View {...stylex.props(styles.info)}>
                <View {...stylex.props(styles.infoMain)}>
                  <View {...stylex.props(styles.nameRow)}>
                    <Text {...stylex.props(styles.name)}>{template.name}</Text>
                    <Text {...props('weui-tag', styles.typeTag)}>{getCouponTypeLabel(template.couponType)}</Text>
                  </View>
                  <Text {...stylex.props(styles.scope)}>适用范围:{scopeText(template.scope)}</Text>
                  <Text {...stylex.props(styles.validity)}>有效期:{getValidityText(template)}</Text>
                </View>
                <View {...stylex.props(styles.claimWrap)}>
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
                      {...props(
                        'weui-btn',
                        'weui-btn_mini',
                        claimingId === template.id ? 'weui-btn_disabled' : styles.claimBtn
                      )}
                    >
                      {claimingId === template.id ? '领取中...' : '立即领取'}
                    </View>
                  )}
                  <Text {...stylex.props(styles.claimedText)}>
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
          <View {...props('weui-cell__bd', styles.rules)}>
            <Text>· 同一订单仅可使用 1 张优惠券,不与折扣券叠加。</Text>
            <Text>· 满足门槛(满 X 元)方可使用,门槛按商品小计计算。</Text>
            <Text>· 已领取券请在有效期内使用,过期自动作废不可恢复。</Text>
            <Text>· 订单取消 / 退款后,未过期券将退回券包。</Text>
          </View>
        </View>
      </View>
    </BasePageLayout>
  )
}

export default CouponCenterPage
