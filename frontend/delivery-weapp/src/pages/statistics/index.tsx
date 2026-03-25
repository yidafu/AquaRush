import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { usePullDownRefresh } from '@tarojs/taro'
import { formatCentsToCurrency } from '@aquarush/common'
import { getTodayStatistics } from '../../services/delivery'
import { useAuth } from '../../hooks/useAuth'
import { PageContainer } from '../../components/PageContainer'
import { StatCard } from './components/StatCard'
import WeekStatistics from './components/WeekStatistics'

import './index.scss'

const StatisticsPage: React.FC = () => {
  const { workerInfo, isAuthenticated } = useAuth()
  const [statistics, setStatistics] = useState({
    totalOrders: 0,
    completedOrders: 0,
    unfinishedOrders: 0,
    earningCents: 0,
  })
  const [loading, setLoading] = useState(false)
  const [lastUpdateTime, setLastUpdateTime] = useState(new Date().toLocaleTimeString())

  // 加载当日统计数据
  const loadStatistics = useCallback(async () => {
    if (!workerInfo?.id) return

    setLoading(true)
    try {
      const res = await getTodayStatistics()
      if (res?.todayStatistics) {
        setStatistics(res.todayStatistics)
        setLastUpdateTime(new Date().toLocaleTimeString())
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
    }
    setLoading(false)
  }, [workerInfo?.id])

  useEffect(() => {
    if (isAuthenticated && workerInfo?.id) {
      loadStatistics()

      // 30秒轮询
      let timeoutId: ReturnType<typeof setTimeout>
      const poll = () => {
        timeoutId = setTimeout(() => {
          loadStatistics()
          poll()
        }, 30000)
      }
      poll()

      return () => clearTimeout(timeoutId)
    }
  }, [isAuthenticated, workerInfo?.id, loadStatistics])

  // 下拉刷新
  usePullDownRefresh(async () => {
    await loadStatistics()
    Taro.stopPullDownRefresh()
  })

  // 获取日期字符串
  const getTodayDate = () => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  }

  return (
    <PageContainer title='当日统计'>
      <ScrollView className='statistics-page' scrollY>
        <View className='statistics-header'>
          <Text className='date'>{getTodayDate()}</Text>
        </View>

        <View className='statistics-cards'>
          <StatCard
            value={statistics.totalOrders}
            label='今日总订单'
            onClick={() => Taro.navigateTo({ url: '/pages/history-orders/index?tab=0' })}
          />
          <StatCard
            value={statistics.completedOrders}
            label='已完成'
            onClick={() => Taro.navigateTo({ url: '/pages/history-orders/index?tab=1' })}
          />
          <StatCard
            value={statistics.unfinishedOrders}
            label='未完成'
            onClick={() => Taro.navigateTo({ url: '/pages/history-orders/index?tab=2' })}
          />
          <StatCard
            value={formatCentsToCurrency(statistics.earningCents)}
            label='今日营收'
            highlight
          />
        </View>

        {/* 一周统计组件 */}
        <WeekStatistics />

        <View className='statistics-tip'>
          <Text>数据更新于: {lastUpdateTime}</Text>
        </View>
      </ScrollView>
    </PageContainer>
  )
}

export default StatisticsPage
