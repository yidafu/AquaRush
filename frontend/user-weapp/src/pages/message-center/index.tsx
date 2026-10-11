import React, { useState, useCallback, useEffect, useRef } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import NotificationService, { NotificationType, NotificationVO } from '../../services/NotificationService'
import { wsPushService } from '../../services/wsPush'
import { isDuplicateNotification } from '../../utils/notificationDedup'
import { authService } from '../../utils/auth'
import BasePageLayout from '../../components/BasePageLayout'
import EmptyState from '../../components/EmptyState'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { formatDateTime } from '@aquarush/common'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { pageTheme } from '../../styles/theme.stylex'

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
 * 消息中心样式(原 index.scss 迁移;750 基准 rpx,后代选择器拍平;
 * 原 `.tabs + .tabs` 相邻兄弟选择器 stylex 无法表达,发丝线直接挂第二行 Tab)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
  },
  // 全部已读工具行(右对齐链接)
  toolbar: {
    display: 'flex',
    justifyContent: 'flex-end',
    paddingTop: '16rpx',
    paddingRight: '32rpx',
    paddingBottom: '0',
    paddingLeft: '32rpx',
  },
  markAll: {
    fontSize: '26rpx',
    color: 'var(--weui-LINK)',
  },
  // 第二行已读状态 Tab(顶部发丝线分隔)
  tabsSecond: {
    borderTopWidth: '2rpx',
    borderTopStyle: 'solid',
    borderTopColor: 'var(--weui-FG-3)',
  },
  item: {
    alignItems: 'flex-start',
  },
  itemTitleRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    rowGap: '16rpx',
    columnGap: '16rpx',
    width: '100%',
  },
  // 未读标题加粗
  itemUnreadTitle: {
    fontWeight: 600,
  },
  itemTime: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    fontSize: '22rpx',
    color: 'var(--weui-FG-2)',
  },
  itemDot: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    marginTop: '8rpx',
    marginLeft: '16rpx',
  },
})

/**
 * 消息中心(F-NTF-002,PRD 13-notification.md,message-center.html)
 * 站内消息列表(仅本人数据,后端强制 userId=登录用户),分页 + 下拉刷新;
 * 双行筛选:第一行类型 .weui-tabs(全部/订单/系统),第二行已读状态 .weui-tabs(全部/未读/已读),两维度组合过滤;
 * 消息行 .weui-media-box(未读加粗 + .weui-badge_dot 红点);
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

  const handleTabClick = (key: TabKey): void => {
    setActiveTab(key)
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
      rootStyles={[pageTheme, styles.page]}
    >
      {/* 全部已读 */}
      {records.length > 0 && (
        <View {...stylex.props(styles.toolbar)}>
          <Text
            {...stylex.props(styles.markAll)}
            trackTarget='message-mark-all'
            trackElementType='link'
            trackElementText='全部已读'
            onClick={handleMarkAllRead}
          >
            全部已读
          </Text>
        </View>
      )}

      {/* 第一行:类型 Tab(message-center.html .weui-tabs:全部/订单/系统) */}
      <View {...props('weui-tabs')}>
        {TABS.map(tab => (
          <View
            key={tab.key}
            {...props('weui-tabs__item', activeTab === tab.key && 'on')}
            trackTarget='message-type-tab'
            trackElementType='tab'
            trackElementText={tab.title}
            onClick={() => handleTabClick(tab.key)}
          >
            <Text>{tab.title}</Text>
          </View>
        ))}
      </View>

      {/* 第二行:已读状态 Tab(全部/未读/已读),与类型 Tab 组合筛选 */}
      <View {...props('weui-tabs', styles.tabsSecond)}>
        {READ_TABS.map(tab => (
          <View
            key={tab.key}
            {...props('weui-tabs__item', activeReadTab === tab.key && 'on')}
            trackTarget='message-read-tab'
            trackElementType='tab'
            trackElementText={tab.title}
            onClick={() => handleReadTabClick(tab.key)}
          >
            <Text>{tab.title}</Text>
          </View>
        ))}
      </View>

      {/* 消息列表(.weui-media-box 行:标题加粗 + 时间 + 摘要 2 行 + 未读红点) */}
      {records.length === 0 && !loading && !error ? (
        <EmptyState
          icon='🔔'
          title={activeReadTab === 'UNREAD' ? '暂无未读消息' : activeReadTab === 'READ' ? '暂无已读消息' : '暂无消息'}
          description='订单与系统通知会在这里展示'
        />
      ) : (
        <View className='weui-cells'>
          {records.map(item => (
            <View
              key={item.id}
              {...props('weui-media-box', styles.item)}
              trackTarget='message-item'
              trackElementType='cell'
              trackElementText={item.title}
              onClick={() => handleReadAndNavigate(item)}
            >
              <View className='weui-media-box__bd'>
                <View {...stylex.props(styles.itemTitleRow)}>
                  <Text {...props('weui-media-box__title', !item.read && styles.itemUnreadTitle)}>
                    {item.title}
                  </Text>
                  <Text {...stylex.props(styles.itemTime)}>{formatRelativeTime(item.createdAt)}</Text>
                </View>
                {item.summary && (
                  <Text className='weui-media-box__desc weui-media-box__desc--2'>
                    {item.summary}
                  </Text>
                )}
              </View>
              {!item.read && <View {...props('weui-badge weui-badge_dot', styles.itemDot)} />}
            </View>
          ))}
          {/* 加载更多 / 没有更多(.weui-loadmore 双短线) */}
          {(loadingMore || (!hasMore && records.length > 0)) && (
            <View className='weui-loadmore'>
              <View className='weui-loadmore__line' />
              <Text>{loadingMore ? '加载更多…' : '没有更多了'}</Text>
              <View className='weui-loadmore__line' />
            </View>
          )}
        </View>
      )}
    </BasePageLayout>
  )
}

export default MessageCenterPage
