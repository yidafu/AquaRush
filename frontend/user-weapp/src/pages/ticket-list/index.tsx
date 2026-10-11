import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
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

import './index.scss'

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

// 票状态 → weui-tag 修饰(可用绿 / 待激活橙 G97 / 已核销灰 / 已退回橙 / 已过期红)
const statusTagClass = (ticket: UserTicketVo): string => {
  if (ticket.pendingActivation) return 'weui-tag--orange'
  if (ticket.status === UserTicketStatus.USED) return 'weui-tag--done'
  if (ticket.status === UserTicketStatus.EXPIRED) return 'weui-tag--cancel'
  if (ticket.status === UserTicketStatus.RETURNED) return 'weui-tag--orange'
  return 'weui-tag--green'
}

// 待激活票状态文案(G97:复购条件未满足,完成复购自动激活)
const statusLabel = (ticket: UserTicketVo): string =>
  ticket.pendingActivation ? '待激活' : getTicketStatusLabel(ticket.status)

/**
 * 我的水票(F-MKT-007,ticket-mine.html:汇总 statgrid + tabs + 票夹卡)
 * 展示已购水票,按状态 Tab(.weui-tabs)过滤;可用张数 = 未用 + 已退回;
 * 汇总行 .mp-statgrid(可用/总张数/已核销);票号长按复制(.weui-cell 行)。
 */
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

  // 可用张数 = 未用 + 已退回 且非待激活(待激活票不可核销,与确认订单页可用数口径一致 G97)
  const availableCount = tickets.filter(t => isAvailable(t) && !t.pendingActivation).length
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
      className='ticket-list-page'
    >
      {/* 汇总(ticket-mine.html .mp-statgrid:可用张数/总张数/已核销) */}
      <View className='weui-cells mt2 statgrid-wrap'>
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
      <View className='ticket-list-header'>
        <Text className='ticket-shop-link' onClick={goTicketShop}>水票商城 ›</Text>
      </View>

      {/* 状态 Tab(coupon-mine.html .weui-tabs 下划线式) */}
      <View className='weui-tabs ticket-tabs'>
        {TABS.map(tab => (
          <View
            key={tab.key}
            className={`weui-tabs__item ${activeTab === tab.key ? 'on' : ''}`}
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
        <View className='ticket-list'>
          {filtered.map(ticket => (
            <View
              key={ticket.id}
              className={`ticket-item ${ticket.pendingActivation ? 'ticket-item--pending' : ''}`}
            >
              <View className='ticket-item-head'>
                <View className='ticket-item-name-row'>
                  <Text className='ticket-item-name'>{ticket.ticketTypeName}</Text>
                  {ticket.isGift && (
                    <Text className='weui-tag weui-tag--brand'>赠</Text>
                  )}
                </View>
                <Text className={`weui-tag weui-tag--md ${statusTagClass(ticket)}`}>
                  {statusLabel(ticket)}
                </Text>
              </View>
              {ticket.specification && (
                <Text className='ticket-item-spec'>规格限定:{ticket.specification}</Text>
              )}
              {/* G97 待激活提示:置灰警示,完成复购自动激活(ticket-mine.html) */}
              {ticket.pendingActivation && (ticket.activationOrderCount ?? 0) > 0 && (
                <Text className='ticket-item-pending-hint'>
                  需复购 {ticket.activationOrderCount} 单后可核销,完成复购自动激活
                </Text>
              )}
              <Text className='ticket-item-expire'>有效期至 {formatDateTime(ticket.expireTime)}</Text>
              {/* 水票唯一递增票号(长按复制,ticket-mine.html 票号行) */}
              <View
                className='ticket-item-no-row hair-t'
                onLongPress={() => copyTicketNo(ticket.ticketNo)}
              >
                <Text className='ticket-item-no-label'>票号</Text>
                <Text className='ticket-item-no'>{ticket.ticketNo}</Text>
                <Text className='ticket-item-no-copy'>长按复制</Text>
              </View>
            </View>
          ))}
        </View>
      )}
    </BasePageLayout>
  )
}

export default TicketListPage
