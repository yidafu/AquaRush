import React, { useState, useEffect, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton, AtLoadMore, AtCard } from 'taro-ui'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import BucketDepositService, { BucketDeposit } from '@/services/BucketDepositService'
import { displayCents } from '@/utils/money'
import BasePageLayout from '@/components/BasePageLayout'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/load-more.scss"
import "taro-ui/dist/style/components/card.scss"
import './index.scss'

const BucketDepositListPage: React.FC = () => {
  const [deposits, setDeposits] = useState<BucketDeposit[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [loadingMore, setLoadingMore] = useState<boolean>(false)
  const [hasMore, setHasMore] = useState<boolean>(true)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [activeBucketCount, setActiveBucketCount] = useState<number>(0)

  const bucketDepositService = BucketDepositService

  const loadDeposits = useCallback(async (page: number = 0, append: boolean = false) => {
    try {
      if (page === 0) {
        setLoading(true)
      } else {
        setLoadingMore(true)
      }

      const result = await bucketDepositService.getBucketDepositsPaged(page, 20)

      if (append) {
        setDeposits(prev => [...prev, ...result.list])
      } else {
        setDeposits(result.list)
      }

      setHasMore(result.pageInfo.hasNext)
      setCurrentPage(page)
    } catch (error) {
      console.error('Failed to load bucket deposits:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [bucketDepositService])

  const loadActiveBucketCount = useCallback(async () => {
    try {
      const count = await bucketDepositService.getActiveBucketCount()
      setActiveBucketCount(count)
    } catch (error) {
      console.error('Failed to load active bucket count:', error)
    }
  }, [bucketDepositService])

  useEffect(() => {
    loadDeposits(0)
    loadActiveBucketCount()
  }, [loadDeposits, loadActiveBucketCount])

  useDidShow(() => {
    loadDeposits(0)
    loadActiveBucketCount()
  })

  useReachBottom(() => {
    if (hasMore && !loadingMore) {
      loadDeposits(currentPage + 1, true)
    }
  })

  usePullDownRefresh(() => {
    loadDeposits(0)
    loadActiveBucketCount()
    Taro.stopPullDownRefresh()
  })

  const handleAddBucket = () => {
    Taro.navigateTo({
      url: '/pages/bucket-deposit/create/index'
    })
  }

  const getStatusText = (status: string) => {
    return status === 'DEPOSITED' ? '押桶中' : '已退还'
  }

  const getStatusClass = (status: string) => {
    return status === 'DEPOSITED' ? 'status-deposited' : 'status-refunded'
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  return (
    <BasePageLayout>
      <View className='bucket-deposit-list-page'>
        {/* 当前押桶数量 */}
        <View className='active-bucket-card'>
          <View className='active-bucket-info'>
            <Text className='active-bucket-label'>当前押桶数量</Text>
            <Text className='active-bucket-count'>{activeBucketCount}</Text>
            <Text className='active-bucket-unit'>个</Text>
          </View>
        </View>

        {/* 押桶记录列表 */}
        <View className='deposit-list'>
          <Text className='list-title'>押桶记录</Text>

          {loading ? (
            <View className='loading-container'>
              <Text className='loading-text'>加载中...</Text>
            </View>
          ) : deposits.length === 0 ? (
            <View className='empty-container'>
              <Text className='empty-text'>暂无押桶记录</Text>
            </View>
          ) : (
            <>
              {deposits.map((deposit) => (
                <AtCard key={deposit.id} className='deposit-card'>
                  <View className='deposit-item'>
                    <View className='deposit-header'>
                      <Text className='deposit-quantity'>{deposit.quantity}个桶</Text>
                      <Text className={`deposit-status ${getStatusClass(deposit.status)}`}>
                        {getStatusText(deposit.status)}
                      </Text>
                    </View>
                    <View className='deposit-amount'>
                      <Text className='amount-label'>押金：</Text>
                      <Text className='amount-value'>¥{displayCents(deposit.amountCents * deposit.quantity)}</Text>
                    </View>
                    <View className='deposit-time'>
                      <Text className='time-label'>
                        {deposit.status === 'REFUNDED' ? '退还时间：' : '押桶时间：'}
                      </Text>
                      <Text className='time-value'>
                        {formatDate(deposit.status === 'REFUNDED' && deposit.refundedAt ? deposit.refundedAt : deposit.createdAt)}
                      </Text>
                    </View>
                    {deposit.remark && (
                      <View className='deposit-remark'>
                        <Text className='remark-label'>备注：</Text>
                        <Text className='remark-value'>{deposit.remark}</Text>
                      </View>
                    )}
                  </View>
                </AtCard>
              ))}

              {loadingMore && (
                <AtLoadMore status='loading' />
              )}

              {!hasMore && deposits.length > 0 && (
                <AtLoadMore status='noMore' />
              )}
            </>
          )}
        </View>

        {/* 新增押桶按钮 */}
        <View className='add-bucket-container'>
          <AtButton
            type='primary'
            className='add-bucket-button'
            onClick={handleAddBucket}
          >
            新增押桶
          </AtButton>
        </View>
      </View>
    </BasePageLayout>
  )
}

export default BucketDepositListPage
