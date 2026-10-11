import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { pageTheme } from '../../styles/theme.stylex'
import TicketService from '../../services/TicketService'
import { authService } from '../../utils/auth'
import {
  UserTicketStatus,
  UserTicketVo,
  formatDateTime,
  getTicketStatusLabel
} from '@aquarush/common'
import BasePageLayout, { EmptyState } from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

interface TicketTab {
  key: string
  label: string
}

// 可用 = 未用 + 已退回(退回票可再次使用)
const AVAILABLE_TAB_KEY = 'AVAILABLE'

const TABS: TicketTab[] = [
  { key: AVAILABLE_TAB_KEY, label: '可用' },
  { key: UserTicketStatus.USED, label: '已核销' },
  { key: UserTicketStatus.RETURNED, label: '已退回' },
  { key: UserTicketStatus.EXPIRED, label: '已过期' }
]

// 票状态 → weui-tag 修饰(可用绿 / 已核销灰 / 已退回橙 / 已过期红)
const statusTagClass = (ticket: UserTicketVo): string => {
  if (ticket.status === UserTicketStatus.USED) return 'weui-tag--done'
  if (ticket.status === UserTicketStatus.EXPIRED) return 'weui-tag--cancel'
  if (ticket.status === UserTicketStatus.RETURNED) return 'weui-tag--orange'
  return 'weui-tag--green'
}

/**
 * 我的水票(F-MKT-007,ticket-mine.html:汇总 statgrid + tabs + 票夹卡)
 * 展示已购水票,按状态 Tab(.weui-tabs)过滤;可用张数 = 未用 + 已退回;
 * 汇总行 .mp-statgrid(可用/总张数/已核销);票号长按复制(.weui-cell 行)。
 *
 * 样式:StyleX(原 index.scss 迁移,750 基准 rpx;weui- / mp- / hair-t 保留)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box',
  },
  // 汇总卡(白底 .mp-statgrid)
  statgridWrap: {
    marginLeft: '16rpx',
    marginRight: '16rpx',
    borderRadius: 'var(--weui-radius)',
  },
  // 水票商城入口行(右对齐链接)
  ticketListHeader: {
    display: 'flex',
    justifyContent: 'flex-end',
    paddingTop: '16rpx',
    paddingRight: '32rpx',
    paddingBottom: 0,
    paddingLeft: '32rpx',
  },
  ticketShopLink: {
    fontSize: '26rpx',
    color: 'var(--weui-LINK)',
  },
  // 状态 Tab(白底下划线式)
  ticketTabs: {
    marginTop: '16rpx',
  },
  // ---- 票夹卡列表 ----
  ticketList: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16rpx',
    paddingTop: '16rpx',
    paddingRight: '16rpx',
    paddingBottom: 0,
    paddingLeft: '16rpx',
  },
  ticketItem: {
    backgroundColor: 'var(--weui-BG-2)',
    borderRadius: 'var(--weui-radius)',
    paddingTop: '32rpx',
    paddingRight: '32rpx',
    paddingBottom: 0,
    paddingLeft: '32rpx',
    boxSizing: 'border-box',
  },
  // 卡头:票名(+赠标签)+ 状态标签
  ticketItemHead: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '16rpx',
  },
  ticketItemNameRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '12rpx',
    minWidth: 0,
  },
  ticketItemName: {
    fontSize: '34rpx',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  // 规格 / 有效期说明行
  ticketItemSpec: {
    display: 'block',
    marginTop: '12rpx',
    fontSize: '26rpx',
    lineHeight: 1.5,
    color: 'var(--weui-FG-1)',
  },
  ticketItemExpire: {
    display: 'block',
    marginTop: '12rpx',
    fontSize: '26rpx',
    lineHeight: 1.5,
    color: 'var(--weui-FG-1)',
  },
  // 票号行(等宽字体,长按复制)
  ticketItemNoRow: {
    marginTop: '24rpx',
    paddingTop: '24rpx',
    paddingRight: 0,
    paddingBottom: '24rpx',
    paddingLeft: 0,
    display: 'flex',
    alignItems: 'center',
    columnGap: '16rpx',
  },
  ticketItemNoLabel: {
    flexGrow: 0,
    flexShrink: 0,
    fontSize: '24rpx',
    color: 'var(--weui-FG-1)',
  },
  ticketItemNo: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
    minWidth: 0,
    fontSize: '26rpx',
    fontFamily: 'Menlo, Monaco, monospace',
    color: 'var(--weui-FG-0)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  ticketItemNoCopy: {
    flexGrow: 0,
    flexShrink: 0,
    fontSize: '24rpx',
    color: 'var(--weui-BRAND)',
  },
})
const TicketListPage: React.FC = () => {
  usePageViewTracking()

  const [tickets, setTickets] = useState<UserTicketVo[]>([])
  const [activeTab, setActiveTab] = useState<string>(AVAILABLE_TAB_KEY)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const loadTickets = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const result = await TicketService.getInstance().getMyTickets()
      if (result.success) {
        setTickets(result.data ?? [])
      } else {
        setError(result.error?.message || '获取我的水票失败')
      }
    } catch (e) {
      console.error('加载我的水票失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      loadTickets()
    } else {
      setLoading(false)
    }
  })

  const goTicketShop = useCallback(() => {
    Taro.navigateTo({ url: '/pages/ticket-shop/index' })
  }, [])

  // 长按复制水票票号(F-MKT-007 每张票唯一递增票号)
  const copyTicketNo = useCallback((ticketNo: string) => {
    Taro.setClipboardData({
      data: ticketNo,
      success: () => {
        Taro.showToast({ title: '票号已复制', icon: 'none' })
      }
    })
  }, [])

  const isAvailable = (ticket: UserTicketVo): boolean =>
    ticket.status === UserTicketStatus.UNUSED || ticket.status === UserTicketStatus.RETURNED

  const availableCount = tickets.filter(isAvailable).length
  const usedCount = tickets.filter(t => t.status === UserTicketStatus.USED).length

  const filtered = activeTab === AVAILABLE_TAB_KEY
    ? tickets.filter(isAvailable)
    : tickets.filter(ticket => ticket.status === activeTab)

  return (
    <BasePageLayout
      requireAuth={true}
      loading={loading}
      error={error}
      onRetry={loadTickets}
      rootStyles={[pageTheme, styles.page]}
    >
      {/* 汇总(ticket-mine.html .mp-statgrid:可用张数/总张数/已核销) */}
      <View {...props('weui-cells', 'mt2', styles.statgridWrap)}>
        <View className='mp-statgrid'>
          <View className='mp-statgrid__item'>
            <View className='mp-statgrid__num'>{availableCount}<Text className='unit'>张</Text></View>
            <View className='mp-statgrid__lbl'>可用张数</View>
          </View>
          <View className='mp-statgrid__item'>
            <View className='mp-statgrid__num'>{tickets.length}<Text className='unit'>张</Text></View>
            <View className='mp-statgrid__lbl'>总张数</View>
          </View>
          <View className='mp-statgrid__item'>
            <View className='mp-statgrid__num'>{usedCount}<Text className='unit'>张</Text></View>
            <View className='mp-statgrid__lbl'>已核销</View>
          </View>
        </View>
      </View>

      {/* 头部:水票商城入口 */}
      <View {...stylex.props(styles.ticketListHeader)}>
        <Text {...stylex.props(styles.ticketShopLink)} onClick={goTicketShop}>水票商城 ›</Text>
      </View>

      {/* 状态 Tab(coupon-mine.html .weui-tabs 下划线式) */}
      <View {...props('weui-tabs', styles.ticketTabs)}>
        {TABS.map(tab => (
          <View
            key={tab.key}
            {...props('weui-tabs__item', activeTab === tab.key && 'on')}
            trackTarget='ticket-list-tab'
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
          title='暂无水票'
          subtitle='去水票商城购买水票'
          actionText='去购票'
          onAction={goTicketShop}
        />
      ) : (
        /* 票夹卡(ticket-mine.html:白底圆角卡,票名 + 状态标签 + 规格/有效期/票号) */
        <View {...stylex.props(styles.ticketList)}>
          {filtered.map(ticket => (
            <View key={ticket.id} {...stylex.props(styles.ticketItem)}>
              <View {...stylex.props(styles.ticketItemHead)}>
                <View {...stylex.props(styles.ticketItemNameRow)}>
                  <Text {...stylex.props(styles.ticketItemName)}>{ticket.ticketTypeName}</Text>
                  {ticket.isGift && (
                    <Text className='weui-tag weui-tag--brand'>赠</Text>
                  )}
                </View>
                <Text {...props('weui-tag', 'weui-tag--md', statusTagClass(ticket))}>
                  {getTicketStatusLabel(ticket.status)}
                </Text>
              </View>
              {ticket.specification && (
                <Text {...stylex.props(styles.ticketItemSpec)}>规格限定:{ticket.specification}</Text>
              )}
              <Text {...stylex.props(styles.ticketItemExpire)}>有效期至 {formatDateTime(ticket.expireTime)}</Text>
              {/* 水票唯一递增票号(长按复制,ticket-mine.html 票号行) */}
              <View
                {...props('hair-t', styles.ticketItemNoRow)}
                onLongPress={() => copyTicketNo(ticket.ticketNo)}
              >
                <Text {...stylex.props(styles.ticketItemNoLabel)}>票号</Text>
                <Text {...stylex.props(styles.ticketItemNo)}>{ticket.ticketNo}</Text>
                <Text {...stylex.props(styles.ticketItemNoCopy)}>长按复制</Text>
              </View>
            </View>
          ))}
        </View>
      )}
    </BasePageLayout>
  )
}

export default TicketListPage
