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
  UserCouponStatus,
  UserCouponVo,
  formatDateTime,
  getCouponDiscountText,
  getCouponStatusLabel,
  getCouponThresholdText
} from '@aquarush/common'
import BasePageLayout, { EmptyState } from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

/**
 * 我的券包样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用;
 * 券卡面额/类型标签/去使用按钮用 tokens.action —— 设计系统 §2.1 券例外橙红;
 * 原 .coupon-card-muted 后代覆盖拍平为 muted 系列类,按可用态条件挂载)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box'
  },
  // Tab 行贴顶白底(coupon-mine.html .weui-tabs hair-b)
  tabs: {
    position: 'sticky',
    top: 0,
    zIndex: 10
  },
  // ---- 券卡列表(白底圆角卡,面额区 + 齿孔 + 虚线分割 + 券信息) ----
  list: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16rpx',
    paddingTop: '16rpx',
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
  // 非可用(已用/已退回/已过期):整卡置灰只读(coupon-mine.html opacity .62)
  cardMuted: {
    opacity: 0.62
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
    boxSizing: 'border-box'
  },
  faceBg: {
    backgroundColor: 'var(--coupon-primary-light)'
  },
  faceBgMuted: {
    backgroundColor: 'var(--weui-BG-1)'
  },
  // 券面额:优惠券品牌橙红(§2.1 券例外)
  facePrice: {
    color: tokens.action
  },
  facePriceMuted: {
    color: 'var(--weui-FG-1)'
  },
  faceThreshold: {
    fontSize: '24rpx',
    color: tokens.action
  },
  faceThresholdMuted: {
    color: 'var(--weui-FG-2)'
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
  // 可用券类型标签(橙红 tint);非可用走默认灰 tag
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
  // 「去使用」CTA(券品牌橙红,coupon-mine.html .weui-btn_xs)
  useBtn: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    backgroundColor: tokens.action,
    color: tokens.onBrand
  },
  // 非可用状态标签
  statusTag: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto'
  }
})

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
      rootStyles={[pageTheme, styles.page]}
    >
      {/* 状态 Tab(coupon-mine.html .weui-tabs 下划线式) */}
      <View {...props('weui-tabs', styles.tabs)}>
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
        <View {...stylex.props(styles.list)}>
          {filtered.map(coupon => {
            const available = coupon.status === UserCouponStatus.RECEIVED
            const { symbol, int, dec, unit } = parseDenomination(coupon)
            return (
              <View
                key={coupon.id}
                {...props(styles.card, !available && styles.cardMuted)}
              >
                <View {...props(styles.face, available ? styles.faceBg : styles.faceBgMuted)}>
                  <View {...props('weui-price', 'weui-price--lg', styles.facePrice, !available && styles.facePriceMuted)}>
                    {symbol && <Text className='weui-price__symbol'>{symbol}</Text>}
                    <Text className='weui-price__int'>{int}</Text>
                    {dec && <Text className='weui-price__dec'>.{dec}</Text>}
                    {unit && <Text className='weui-price__dec'>{unit}</Text>}
                  </View>
                  <Text {...props(styles.faceThreshold, !available && styles.faceThresholdMuted)}>{getCouponThresholdText(coupon.thresholdCents)}</Text>
                </View>
                <View {...props(styles.notch, styles.notchTop)} />
                <View {...props(styles.notch, styles.notchBottom)} />
                <View {...stylex.props(styles.divider)} />
                <View {...stylex.props(styles.info)}>
                  <View {...stylex.props(styles.infoMain)}>
                    <View {...stylex.props(styles.nameRow)}>
                      <Text {...stylex.props(styles.name)}>{coupon.name}</Text>
                      <Text {...props('weui-tag', available && styles.typeTag)}>
                        {getCouponStatusLabel(coupon.status)}
                      </Text>
                    </View>
                    <Text {...stylex.props(styles.scope)}>适用范围:{scopeText(coupon.scope)}</Text>
                    <Text {...stylex.props(styles.validity)}>有效期至 {formatDateTime(coupon.expireTime)}</Text>
                  </View>
                  {available ? (
                    <View
                      trackTarget="coupon-use"
                      trackElementType="button"
                      trackElementText="去使用"
                      trackProps={{ couponId: coupon.id }}
                      onClick={goUseCoupon}
                      {...props('weui-btn', 'weui-btn_xs', styles.useBtn)}
                    >
                      去使用
                    </View>
                  ) : (
                    <Text {...props('weui-tag', 'weui-tag--done', styles.statusTag)}>
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
