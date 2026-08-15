import React, { useState, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import { AtLoadMore } from 'taro-ui'
import FeedbackService from '../../services/FeedbackService'
import { authService } from '../../utils/auth'
import { FeedbackRecordVo, formatDateTime } from '@aquarush/common'
import {
  getFeedbackStatusColor,
  getFeedbackStatusLabel,
  getFeedbackTypeLabel
} from '../../utils/feedback'
import BasePageLayout from '../../components/BasePageLayout'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import "taro-ui/dist/style/components/load-more.scss"
import './index.scss'

const PAGE_SIZE = 10

/**
 * 我的-反馈记录(F-USR-005 处理结果回传)
 * 展示本人提交的反馈(仅本人数据,后端强制 userId=登录用户),含处理状态与处理结果;
 * 分页加载 + 下拉刷新;后台处理流转后本页即时可见。
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
    Taro.navigateTo({ url: '/pages/my/index' })
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
      {/* 记录列表 */}
      <View className='feedback-records-header'>
        <Text className='feedback-records-count'>共 {totalElements} 条反馈</Text>
      </View>

      <View className='feedback-records-list'>
        {records.map(record => (
          <View key={record.id} className='feedback-record-item'>
            <View className='feedback-record-header'>
              <Text className='feedback-record-type'>{getFeedbackTypeLabel(record.feedbackType)}</Text>
              <Text
                className='feedback-record-status'
                style={{ color: getFeedbackStatusColor(record.status) }}
              >
                {getFeedbackStatusLabel(record.status)}
              </Text>
            </View>

            <Text className='feedback-record-content'>{record.content}</Text>

            {record.images && record.images.length > 0 && (
              <View className='feedback-record-images'>
                {record.images.map((url, index) => (
                  <Image key={index} className='feedback-record-image' src={url} mode='aspectFill' />
                ))}
              </View>
            )}

            <View className='feedback-record-footer'>
              <Text className='feedback-record-time'>{formatDateTime(record.createdAt)}</Text>
              {record.handleResult && (
                <View className='feedback-record-handle'>
                  <Text className='feedback-record-handle-label'>处理结果:</Text>
                  <Text className='feedback-record-handle-text'>{record.handleResult}</Text>
                </View>
              )}
              {record.handledByName && (
                <Text className='feedback-record-handled-by'>
                  {record.handledAt ? `${formatDateTime(record.handledAt)} · ` : ''}{record.handledByName} 处理
                </Text>
              )}
            </View>
          </View>
        ))}
      </View>

      {/* 加载更多 / 没有更多 */}
      {loadingMore && <AtLoadMore status='loading' loadingText='加载更多...' />}
      {!hasMore && records.length > 0 && <AtLoadMore status='noMore' noMoreText='没有更多了' />}
    </BasePageLayout>
  )
}

export default FeedbackRecordsPage
