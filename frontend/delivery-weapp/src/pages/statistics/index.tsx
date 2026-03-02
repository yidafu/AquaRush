import React, { useState, useEffect } from 'react'
import { View, Text, Button } from '@tarojs/components'
import { getTodayStatistics } from '../../services/delivery'
import { useAuth } from '../../hooks/useAuth'
import {PageContainer} from '../../components/PageContainer'
import './index.scss'

const StatisticsPage: React.FC = () => {
  const { workerInfo, isAuthenticated } = useAuth()
  const [statistics, setStatistics] = useState({
    totalOrders: 0,
    completedOrders: 0,
    pendingOrders: 0,
    earningCents: 0,
  })
  const [loading, setLoading] = useState(false)

  // 加载统计数据
  const loadStatistics = async () => {
    if (!workerInfo?.id) return

    setLoading(true)
    try {
      const res = await getTodayStatistics(workerInfo.id)
      if (res?.data?.todayStatistics) {
        setStatistics(res.data.todayStatistics)
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    }
    setLoading(false)
  }

  useEffect(() => {
    if (isAuthenticated && workerInfo?.id) {
      loadStatistics()
    }
  }, [isAuthenticated, workerInfo?.id])

  // 格式化金额
  const formatAmount = (cents: number) => {
    return `¥${(cents / 100).toFixed(2)}`
  }

  // 获取日期字符串
  const getTodayDate = () => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }

  return (
    <PageContainer title="当日统计">
      <View className='statistics-page'>
        <View className='statistics-header'>
          <Text className='date'>{getTodayDate()}</Text>
          <Text className='title'>当日统计</Text>
        </View>

        <View className='statistics-cards'>
          <View className='stat-card'>
            <Text className='stat-value'>{statistics.totalOrders}</Text>
            <Text className='stat-label'>今日总订单</Text>
          </View>

          <View className='stat-card'>
            <Text className='stat-value'>{statistics.completedOrders}</Text>
            <Text className='stat-label'>已完成</Text>
          </View>

          <View className='stat-card'>
            <Text className='stat-value'>{statistics.pendingOrders}</Text>
            <Text className='stat-label'>待配送</Text>
          </View>

          <View className='stat-card highlight'>
            <Text className='stat-value'>{formatAmount(statistics.earningCents)}</Text>
            <Text className='stat-label'>今日收入</Text>
          </View>
        </View>

        <View className='statistics-actions'>
          <Button className='refresh-btn' onClick={loadStatistics} loading={loading}>
            刷新数据
          </Button>
        </View>

        <View className='statistics-tip'>
          <Text>数据更新于: {new Date().toLocaleTimeString()}</Text>
        </View>
      </View>
    </PageContainer>
  )
}

export default StatisticsPage
