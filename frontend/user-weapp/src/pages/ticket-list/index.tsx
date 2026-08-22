import React, { useState, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import TicketService from '../../services/TicketService'
import { authService } from '../../utils/auth'
import { UserTicketStatus, UserTicketVo, formatDateTime } from '@aquarush/common'
import { getTicketStatusLabel } from '../../utils/marketing'
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
  { key: UserTicketStatus.USED, label: '已用' },
  { key: UserTicketStatus.RETURNED, label: '已退回' },
  { key: UserTicketStatus.EXPIRED, label: '已过期' }
]

/**
 * 我的水票(F-MKT-007)
 * 展示已购水票,按状态 Tab 过滤;可用张数 = 未用 + 已退回。
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

  const availableCount = tickets.filter(isAvailable).length

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
      {/* 头部:可用张数 + 水票商城入口 */}
      <View className='ticket-list-header'>
        <View className='ticket-available'>
          <Text className='ticket-available-label'>可用水票</Text>
          <Text className='ticket-available-count'>{availableCount}</Text>
          <Text className='ticket-available-unit'>张</Text>
        </View>
        <Text className='ticket-shop-link' onClick={goTicketShop}>水票商城 ›</Text>
      </View>

      {/* 状态 Tab */}
      <View className='ticket-tabs'>
        {TABS.map(tab => (
          <View
            key={tab.key}
            className={`ticket-tab ${activeTab === tab.key ? 'ticket-tab-active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            <Text className='ticket-tab-text'>{tab.label}</Text>
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
        /* 水票列表 */
        <View className='ticket-list'>
          {filtered.map(ticket => (
            <View key={ticket.id} className='ticket-item'>
              <View className='ticket-item-left'>
                <View className='ticket-item-name-row'>
                  <Text className='ticket-item-name'>{ticket.ticketTypeName}</Text>
                  {ticket.isGift && (
                    <Text className='ticket-item-gift'>赠</Text>
                  )}
                </View>
                {ticket.specification && (
                  <Text className='ticket-item-spec'>限{ticket.specification}规格</Text>
                )}
                <Text className='ticket-item-expire'>有效期至 {formatDateTime(ticket.expireTime)}</Text>
                {/* 水票唯一递增票号(长按复制) */}
                <Text
                  className='ticket-item-no'
                  onLongPress={() => copyTicketNo(ticket.ticketNo)}
                >
                  票号 {ticket.ticketNo}
                </Text>
              </View>
              <Text className='ticket-item-status'>{getTicketStatusLabel(ticket.status)}</Text>
            </View>
          ))}
        </View>
      )}
    </BasePageLayout>
  )
}

export default TicketListPage
