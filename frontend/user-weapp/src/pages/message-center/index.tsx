import React, { useState, useCallback, useEffect, useRef } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import { AtLoadMore, AtTabs } from 'taro-ui'
import NotificationService, { NotificationType, NotificationVO } from '../../services/NotificationService'
import { wsPushService } from '../../services/wsPush'
import { isDuplicateNotification } from '../../utils/notificationDedup'
import { authService } from '../../utils/auth'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { formatDateTime } from '@aquarush/common'

import 'taro-ui/dist/style/components/tabs.scss'
import 'taro-ui/dist/style/components/load-more.scss'
import './index.scss'

const PAGE_SIZE = 10

type TabKey = 'ALL' | 'ORDER' | 'SYSTEM'
type ReadTabKey = 'ALL' | 'UNREAD' | 'READ'

const TABS: { title: string; key: TabKey }[] = [
  { title: '全部', key: 'ALL' },
  { title: '订单', key: 'ORDER' },
  { title: '系统', key: 'SYSTEM' }
]

const READ_TABS: { title: string; key: ReadTabKey }[] = [
  { title: '全部', key: 'ALL' },
  { title: '未读', key: 'UNREAD' },
  { title: '已读', key: 'READ' }
]

/** 状态 Tab → read 筛选值:全部=不过滤,未读=false,已读=true */
const readTabToValue = (key: ReadTabKey): boolean | undefined =>
  key === 'ALL' ? undefined : key === 'UNREAD' ? false : true

/**
 * 消息中心(F-NTF-002,PRD 13-notification.md)
 * 站内消息列表(仅本人数据,后端强制 userId=登录用户),分页 + 下拉刷新;
 * 双行筛选:第一行类型 Tab(全部/订单/系统),第二行已读状态 Tab(全部/未读/已读),两维度组合过滤;
 * 点击消息标已读并跳转关联订单详情;WebSocket 实时推送到达时增量刷新(R-NTF-02 幂等去重)。
 * 公告/反馈分类聚合为开放问题(PRD 13-notification.md §3.1),枚举与 Tab 预留。
 */
const MessageCenterPage: React.FC = () => {
  usePageViewTracking()

  const [activeTab, setActiveTab] = useState<TabKey>('ALL')
  const [activeReadTab, setActiveReadTab] = useState<ReadTabKey>('ALL')
  const [records, setRecords] = useState<NotificationVO[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [error, setError] = useState<string | null>(null)
  const userIdRef = useRef<string | null>(null)

  const tabToType = (tab: TabKey): NotificationType | undefined =>
    tab === 'ALL' ? undefined : (tab as NotificationType)

  const loadRecords = useCallback(async (page: number = 0, append: boolean = false) => {
    try {
      if (page === 0) {
        setLoading(true)
        setError(null)
      } else {
        setLoadingMore(true)
      }
      const data = await NotificationService.getMyNotifications(
        tabToType(activeTab),
        readTabToValue(activeReadTab),
        page,
        PAGE_SIZE
      )
      const newRecords = data.content ?? []
      setRecords(prev => (append ? [...prev, ...newRecords] : newRecords))
      setHasMore(page + 1 < (data.totalPages ?? 0))
      setCurrentPage(page)
    } catch (e: any) {
      console.error('加载消息失败:', e)
      setError(e?.message || '加载失败,请重试')
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [activeTab, activeReadTab])

  // Tab 切换时重置列表
  useEffect(() => {
    if (!loading) {
      loadRecords(0, false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, activeReadTab])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
      userIdRef.current = authService.getUserInfo()?.id ?? null
      loadRecords(0, false)
    } else {
      setLoading(false)
    }
  })

  useReachBottom(() => {
    if (hasMore && !loadingMore && !loading) {
      loadRecords(currentPage + 1, true)
    }
  })

  usePullDownRefresh(async () => {
    await loadRecords(0, false)
    Taro.stopPullDownRefresh()
  })

  // WebSocket 实时推送:到达时按幂等键去重,增量刷新列表
  useEffect(() => {
    const unsubscribe = wsPushService.onMessage((message) => {
      const userId = userIdRef.current
      if (userId && isDuplicateNotification(userId, message.idempotencyKey)) {
        return
      }
      // 推送到达即刷新第一页(新消息置顶)
      loadRecords(0, false)
    })
    return unsubscribe
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, activeReadTab])

  const handleTabClick = (index: number): void => {
    setActiveTab(TABS[index].key as TabKey)
  }

  const handleReadTabClick = (key: ReadTabKey): void => {
    setActiveReadTab(key)
  }

  const handleReadAndNavigate = async (item: NotificationVO): Promise<void> => {
    if (!item.read) {
      try {
        await NotificationService.markRead(item.id)
        if (activeReadTab === 'UNREAD') {
          // 未读 Tab 下点击即读:从列表移除(否则残留灰色已读项)
          setRecords(prev => prev.filter(r => r.id !== item.id))
        } else {
          setRecords(prev => prev.map(r => (r.id === item.id ? { ...r, read: true } : r)))
        }
      } catch (e) {
        console.error('标记已读失败:', e)
      }
    }
    // 订单消息跳转订单详情(关联单号为订单 id)
    if (item.type === 'ORDER' && item.relatedNo) {
      Taro.navigateTo({ url: `/pages/order-detail/index?orderId=${item.relatedNo}` })
    }
  }

  const handleMarkAllRead = async (): Promise<void> => {
    try {
      await NotificationService.markAllRead()
      if (activeReadTab === 'UNREAD') {
        // 未读 Tab 下全部置读后列表应为空,重新拉取
        loadRecords(0, false)
      } else {
        setRecords(prev => prev.map(r => ({ ...r, read: true })))
      }
    } catch (e) {
      console.error('全部已读失败:', e)
      Taro.showToast({ title: '操作失败,请重试', icon: 'none' })
    }
  }

  const handleGoLogin = (): void => {
    // 「我的」是 tabBar 页,必须用 switchTab 切换
    Taro.switchTab({ url: '/pages/my/index' })
  }

  const formatRelativeTime = (time: string): string => {
    const date = new Date(time)
    const diff = Date.now() - date.getTime()
    const minute = 60_000
    const hour = 60 * minute
    const day = 24 * hour
    if (diff < minute) return '刚刚'
    if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
    if (diff < day) return `${Math.floor(diff / hour)}小时前`
    if (diff < 7 * day) return `${Math.floor(diff / day)}天前`
    return formatDateTime(time)
  }

  return (
    <BasePageLayout
      requireAuth={true}
      onLoginClick={handleGoLogin}
      loading={loading}
      error={error}
      onRetry={() => loadRecords(0, false)}
      className='message-center-page'
    >
      {/* 全部已读 */}
      {records.length > 0 && (
        <View className='message-center-toolbar'>
          <Text className='message-center-mark-all' onClick={handleMarkAllRead}>
            全部已读
          </Text>
        </View>
      )}

      {/* 第一行:类型 Tab(全部/订单/系统) */}
      <AtTabs current={TABS.findIndex(t => t.key === activeTab)} tabList={TABS} onClick={handleTabClick} />

      {/* 第二行:已读状态 Tab(全部/未读/已读),与类型 Tab 组合筛选 */}
      <View className='message-center-read-tabs'>
        {READ_TABS.map(tab => {
          const active = activeReadTab === tab.key
          return (
            <View
              key={tab.key}
              className={`message-center-read-tab ${active ? 'message-center-read-tab-active' : ''}`}
              onClick={() => handleReadTabClick(tab.key)}
            >
              <Text className={`message-center-read-tab-text ${active ? 'message-center-read-tab-text-active' : ''}`}>
                {tab.title}
              </Text>
            </View>
          )
        })}
      </View>

      <View className='message-center-list'>
        {records.length === 0 && !loading && !error && (
          <View className='message-center-empty'>
            <Text>
              {activeReadTab === 'UNREAD' ? '暂无未读消息' : activeReadTab === 'READ' ? '暂无已读消息' : '暂无消息'}
            </Text>
          </View>
        )}
        {records.map(item => (
          <View
            key={item.id}
            className={`message-center-item ${item.read ? '' : 'message-center-item-unread'}`}
            onClick={() => handleReadAndNavigate(item)}
          >
            <View className='message-center-item-body'>
              <View className='message-center-item-title-row'>
                {!item.read && <View className='message-center-item-dot' />}
                <Text className='message-center-item-title'>{item.title}</Text>
                <Text className='message-center-item-time'>{formatRelativeTime(item.createdAt)}</Text>
              </View>
              {item.summary && (
                <Text className='message-center-item-summary' numberOfLines={2}>
                  {item.summary}
                </Text>
              )}
            </View>
          </View>
        ))}
        {loadingMore && <AtLoadMore status='loading' loadingText='加载更多...' />}
        {!hasMore && records.length > 0 && <AtLoadMore status='noMore' noMoreText='没有更多了' />}
      </View>
    </BasePageLayout>
  )
}

export default MessageCenterPage
