import React, { useState, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { pageTheme } from '../../styles/theme.stylex'
import FeedbackService from '../../services/FeedbackService'
import { authService } from '../../utils/auth'
import {
  FEEDBACK_STATUS_TONE,
  FeedbackRecordVo,
  formatDateTime,
  getFeedbackStatusLabel,
  getFeedbackTypeLabel,
  getStatusTone
} from '@aquarush/common'
import StatusTag from '../../components/StatusTag'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

const PAGE_SIZE = 10

/** 反馈记录样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用;原嵌套后代选择器拍平) */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box'
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16rpx',
    paddingTop: '0',
    paddingRight: '16rpx',
    paddingBottom: '0',
    paddingLeft: '16rpx'
  },
  // 面板内 media-box 纵向排布(feedback-records.html flex-direction:column)
  record: {
    flexDirection: 'column',
    alignItems: 'stretch'
  },
  recordHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '16rpx'
  },
  recordContent: {
    display: '-webkit-box',
    WebkitBoxOrient: 'vertical',
    WebkitLineClamp: 2,
    overflow: 'hidden',
    marginTop: '24rpx',
    fontSize: '30rpx',
    lineHeight: 1.6,
    color: 'var(--weui-FG-0)'
  },
  recordImages: {
    display: 'flex',
    flexWrap: 'wrap',
    rowGap: '16rpx',
    columnGap: '16rpx',
    marginTop: '24rpx'
  },
  recordImage: {
    display: 'block'
  },
  recordTime: {
    marginTop: '24rpx',
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)'
  },
  // 处理结果内嵌块(浅灰底圆角,feedback-records.html 嵌套 .weui-cells 形态)
  recordHandle: {
    marginTop: '24rpx',
    paddingTop: '24rpx',
    paddingRight: '24rpx',
    paddingBottom: '24rpx',
    paddingLeft: '24rpx',
    borderRadius: 'var(--weui-radius)',
    backgroundColor: 'var(--weui-BG-1)'
  },
  handleTitle: {
    paddingTop: '0',
    paddingRight: '0',
    paddingBottom: '12rpx',
    paddingLeft: '0'
  },
  handleText: {
    display: 'block',
    fontSize: '26rpx',
    lineHeight: 1.6,
    color: 'var(--weui-FG-1)'
  },
  handledBy: {
    display: 'block',
    marginTop: '12rpx',
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)'
  }
})

/**
 * 我的-反馈记录(F-USR-005 处理结果回传,feedback-records.html)
 * 每条反馈一个 .weui-panel(类型 tag + 状态 StatusTag + 内容 clamp2 +
 * 缩略图 --sm + 提交时间 + 处理结果内嵌块);分页加载 + 下拉刷新;
 * 后台处理流转后本页即时可见。
 */
const FeedbackRecordsPage: React.FC = () => {
  usePageViewTracking()

  const [records, setRecords] = useState<FeedbackRecordVo[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [totalElements, setTotalElements] = useState<number>(0)
  const [error, setError] = useState<string | null>(null)

  const loadRecords = useCallback(async (page: number = 0, append: boolean = false) => {
    try {
      if (page === 0) {
        setLoading(true)
        setError(null)
      } else {
        setLoadingMore(true)
      }

      const result = await FeedbackService.getMyFeedbacks(page, PAGE_SIZE)
      if (!result.success) {
        setError(result.error?.message || '获取反馈记录失败,请重试')
        return
      }

      const data = result.data
      const newRecords = [...(data.content ?? [])]
      setRecords(prev => (append ? [...prev, ...newRecords] : newRecords))
      setTotalElements(data.totalElements ?? 0)
      setHasMore(page + 1 < (data.totalPages ?? 0))
      setCurrentPage(page)
    } catch (e) {
      console.error('加载反馈记录失败:', e)
      setError('加载失败，请重试')
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [])

  useDidShow(() => {
    if (authService.isAuthenticated()) {
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

  const handleGoFeedback = useCallback(() => {
    Taro.navigateTo({ url: '/pages/feedback/index' })
  }, [])

  const handleGoLogin = useCallback(() => {
    // 「我的」是 tabBar 页,必须用 switchTab 切换
    Taro.switchTab({ url: '/pages/my/index' })
  }, [])

  return (
    <BasePageLayout
      requireAuth={true}
      onLoginClick={handleGoLogin}
      loading={loading}
      error={error}
      onRetry={() => loadRecords(0, false)}
      empty={records.length === 0 && !loading && !error}
      emptyTitle='暂无反馈记录'
      emptySubtitle='您的意见对我们很重要'
      emptyActionText='去反馈'
      onEmptyAction={handleGoFeedback}
      rootStyles={[pageTheme, styles.page]}
    >
      {/* 统计行 */}
      <View className='weui-cells__title'>共 {totalElements} 条反馈</View>

      {/* 记录列表(.weui-panel 块) */}
      <View {...stylex.props(styles.list)}>
        {records.map(record => (
          <View key={record.id} className='weui-panel'>
            <View className='weui-panel__bd'>
              <View {...props('weui-media-box', styles.record)}>
                <View {...stylex.props(styles.recordHeader)}>
                  <Text className='weui-tag'>{getFeedbackTypeLabel(record.feedbackType)}</Text>
                  <StatusTag
                    tone={getStatusTone(FEEDBACK_STATUS_TONE, record.status)}
                    text={getFeedbackStatusLabel(record.status)}
                    size='small'
                  />
                </View>

                <Text {...stylex.props(styles.recordContent)}>{record.content}</Text>

                {record.images && record.images.length > 0 && (
                  <View {...stylex.props(styles.recordImages)}>
                    {record.images.map((url, index) => (
                      <Image
                        key={index}
                        {...props('weui-media-box__thumb', 'weui-media-box__thumb--sm', styles.recordImage)}
                        src={url}
                        mode='aspectFill'
                        lazyLoad
                      />
                    ))}
                  </View>
                )}

                <Text {...stylex.props(styles.recordTime)}>提交时间:{formatDateTime(record.createdAt)}</Text>

                {record.handleResult && (
                  <View {...stylex.props(styles.recordHandle)}>
                    <View {...props('weui-cells__title', styles.handleTitle)}>处理结果</View>
                    <Text {...stylex.props(styles.handleText)}>{record.handleResult}</Text>
                    {record.handledByName && (
                      <Text {...stylex.props(styles.handledBy)}>
                        {record.handledAt ? `${formatDateTime(record.handledAt)} · ` : ''}{record.handledByName} 处理
                      </Text>
                    )}
                  </View>
                )}
              </View>
            </View>
          </View>
        ))}
      </View>

      {/* 加载更多 / 没有更多(.weui-loadmore 双短线) */}
      {(loadingMore || (!hasMore && records.length > 0)) && (
        <View className='weui-loadmore'>
          <View className='weui-loadmore__line' />
          <Text>{loadingMore ? '上拉加载更多' : '没有更多了'}</Text>
          <View className='weui-loadmore__line' />
        </View>
      )}
    </BasePageLayout>
  )
}

export default FeedbackRecordsPage
