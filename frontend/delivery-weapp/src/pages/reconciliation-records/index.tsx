import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/taro'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import './index.scss'
import { getDailyReconciliations, DailyReconciliation } from '../../services/dailyCollection'
import { formatCentsToCurrency } from '../../utils/money'
import { PageContainer } from '../../components/PageContainer'

// 状态颜色映射
const statusColorMap: Record<string, string> = {
  PENDING: '#faad14',
  MATCHED: '#52c41a',
  DISCREPANCY: '#ff4d4f',
  REVIEWED: '#1890ff',
}

const statusTextMap: Record<string, string> = {
  PENDING: '待对账',
  MATCHED: '匹配',
  DISCREPANCY: '有差异',
  REVIEWED: '已复核',
}

const ReconciliationRecordsPage: React.FC = () => {
  const [records, setRecords] = useState<DailyReconciliation[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [allData, setAllData] = useState<DailyReconciliation[]>([])

  const pageSize = 10

  // 加载对账记录
  const loadRecords = useCallback(async () => {
    if (allData.length > 0) return // 已有全部数据

    try {
      setLoading(true)
      const res = await getDailyReconciliations()
      if (res?.dailyReconciliations) {
        // 按日期倒序排列
        const sortedData = [...res.dailyReconciliations].sort(
          (a, b) => new Date(b.reconciliationDate).getTime() - new Date(a.reconciliationDate).getTime()
        )
        setAllData(sortedData)
        // 初始加载第一页
        setRecords(sortedData.slice(0, pageSize))
        setPage(1)
        setHasMore(sortedData.length > pageSize)
      }
    } catch (error) {
      console.error('加载对账记录失败:', error)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }, [allData.length])

  // 页面显示时加载数据
  useDidShow(() => {
    loadRecords()
  })

  // 下拉刷新
  usePullDownRefresh(async () => {
    setAllData([])
    setPage(0)
    await loadRecords()
    Taro.stopPullDownRefresh()
  })

  // 上拉加载更多
  useReachBottom(() => {
    if (!loading && hasMore) {
      const nextPage = page
      const start = nextPage * pageSize
      const end = start + pageSize
      const newRecords = allData.slice(start, end)

      setRecords((prev) => [...prev, ...newRecords])
      setPage(nextPage + 1)
      setHasMore(end < allData.length)
    }
  })

  return (
    <PageContainer title='对账记录'>
      <ScrollView scrollY className='reconciliation-page'>
        {records.length === 0 && !loading ? (
          <View className='empty'>
            <Text>暂无对账记录</Text>
          </View>
        ) : (
          records.map((record) => (
            <View key={record.id} className='reconciliation-card'>
              <View className='card-header'>
                <Text className='card-date'>{record.reconciliationDate}</Text>
                <Text
                  className='card-status'
                  style={{ backgroundColor: statusColorMap[record.status] }}
                >
                  {statusTextMap[record.status] || record.status}
                </Text>
              </View>

              <View className='card-stats'>
                <View className='stat-item'>
                  <Text className='stat-label'>订单总数</Text>
                  <Text className='stat-value'>{record.totalOrderCount} 单</Text>
                </View>
                <View className='stat-item'>
                  <Text className='stat-label'>订单金额</Text>
                  <Text className='stat-value'>
                    {formatCentsToCurrency(record.totalOrderAmountCents)}
                  </Text>
                </View>
              </View>

              <View className='card-stats'>
                <View className='stat-item'>
                  <Text className='stat-label'>现金收款</Text>
                  <Text className='stat-value'>
                    {formatCentsToCurrency(record.totalCashAmountCents)}
                  </Text>
                </View>
                <View className='stat-item'>
                  <Text className='stat-label'>扫码收款</Text>
                  <Text className='stat-value'>
                    {formatCentsToCurrency(record.totalQrCodeAmountCents)}
                  </Text>
                </View>
              </View>

              <View className='card-stats'>
                <View className='stat-item'>
                  <Text className='stat-label'>水票数量</Text>
                  <Text className='stat-value'>{record.totalWaterTicketCount} 张</Text>
                </View>
                <View className='stat-item'>
                  <Text className='stat-label'>差异金额</Text>
                  <Text
                    className='stat-value'
                    style={{
                      color: Math.abs(record.discrepancyAmountCents) <= 100 ? '#52c41a' : '#ff4d4f',
                    }}
                  >
                    {formatCentsToCurrency(record.discrepancyAmountCents)}
                  </Text>
                </View>
              </View>

              {record.reviewedAt && (
                <View className='card-review'>
                  <Text className='review-info'>
                    复核于 {record.reviewedAt} {record.reviewNotes && ` - ${record.reviewNotes}`}
                  </Text>
                </View>
              )}
            </View>
          ))
        )}

        {loading && (
          <View className='load-more'>
            <Text>加载中...</Text>
          </View>
        )}

        {!hasMore && records.length > 0 && (
          <View className='load-more'>
            <Text>没有更多了</Text>
          </View>
        )}
      </ScrollView>
    </PageContainer>
  )
}

export default ReconciliationRecordsPage