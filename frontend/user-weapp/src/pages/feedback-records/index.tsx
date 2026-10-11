import React, { useState, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
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

import './index.scss'

const PAGE_SIZE = 10

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
      className='feedback-records-page'
    >
      {/* 统计行 */}
      <View className='weui-cells__title'>共 {totalElements} 条反馈</View>

      {/* 记录列表(.weui-panel 块) */}
      <View className='feedback-records-list'>
        {records.map(record => (
          <View key={record.id} className='weui-panel'>
            <View className='weui-panel__bd'>
              <View className='weui-media-box feedback-record'>
                <View className='feedback-record-header'>
                  <Text className='weui-tag'>{getFeedbackTypeLabel(record.feedbackType)}</Text>
                  <StatusTag
                    tone={getStatusTone(FEEDBACK_STATUS_TONE, record.status)}
                    text={getFeedbackStatusLabel(record.status)}
                    size='small'
                  />
                </View>

                <Text className='feedback-record-content'>{record.content}</Text>

                {record.images && record.images.length > 0 && (
                  <View className='feedback-record-images'>
                    {record.images.map((url, index) => (
                      <Image
                        key={index}
                        className='weui-media-box__thumb weui-media-box__thumb--sm feedback-record-image'
                        src={url}
                        mode='aspectFill'
                        lazyLoad
                      />
                    ))}
                  </View>
                )}

                <Text className='feedback-record-time'>提交时间:{formatDateTime(record.createdAt)}</Text>

                {record.handleResult && (
                  <View className='feedback-record-handle'>
                    <View className='weui-cells__title feedback-record-handle-title'>处理结果</View>
                    <Text className='feedback-record-handle-text'>{record.handleResult}</Text>
                    {record.handledByName && (
                      <Text className='feedback-record-handled-by'>
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
