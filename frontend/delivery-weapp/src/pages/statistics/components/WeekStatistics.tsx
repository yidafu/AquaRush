import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { formatCentsToCurrency, formatCurrency } from '@aquarush/common'
import { getWeekStatistics } from '../../../services/delivery'
import { useAuth } from '../../../hooks/useAuth'
import { Chart, Line, Axis, Tooltip } from '@antv/f2'
import type { DailyStat, WeekStatistics } from '@aquarush/common'

import './index.scss'



const WeekStatistics: React.FC = () => {
  const { workerInfo } = useAuth()
  const [weekStatistics, setWeekStatistics] = useState<WeekStatistics | null>(null)
  const [loading, setLoading] = useState(false)

  const loadWeekStatistics = useCallback(async () => {
    if (!workerInfo?.id) return

    setLoading(true)
    try {
      const res = await getWeekStatistics()
      console.log('getWeekStatistics', res)
      if (res?.weekStatistics) {
        setWeekStatistics(res.weekStatistics)
      }
    } catch (error) {
      console.error('加载周统计数据失败:', error)
    }
    setLoading(false)
  }, [workerInfo?.id])

  useEffect(() => {
    if (workerInfo?.id) {
      loadWeekStatistics()
    }
  }, [workerInfo?.id, loadWeekStatistics])

  // 使用 useMemo 缓存图表数据
  const chartData = useMemo(() => {
    if (!weekStatistics?.dailyStats) return []

    return weekStatistics.dailyStats.map((item: DailyStat) => ({
      date: item.date,
      orderCount: item.orderCount,
      earning: item.earning,
    }))
  }, [weekStatistics])

  // 使用 useCallback 缓存图表渲染函数
  const renderChart = useCallback(() => {
    if (chartData.length === 0) return null

    return (
      <Chart data={chartData}>
        <Axis
          field='date'
          tickCount={7}
          label={{
            rotate: true,
            labelLimit: 40,
          }}
        />
        <Axis
          field='orderCount'
          tickCount={5}
          label={(text) => ({ text })}
          line={{
            style: { stroke: '#1890ff' },
          }}
          labelOffset={40}
        />
        <Axis
          field='earning'
          tickCount={5}
          label={(text) => ({ text: `¥${(Number(text) * 100).toFixed(0)}` })}
          line={null}
          labelOffset={10}
          position='right'
        />
        <Line
          x='date'
          y='orderCount'
          shape='smooth'
          style={{
            stroke: '#1890ff',
            lineWidth: 2,
          }}
        />
        <Line
          x='date'
          y='earning'
          shape='smooth'
          style={{
            stroke: '#faad14',
            lineWidth: 2,
          }}
        />
        <Tooltip
          showItemMarker
          onPress={(ev) => {
            const item = ev.data as { date: string; orderCount: number; earning: number }
            Taro.showToast({
              title: `${item.date}: 订单${item.orderCount}单, 营收¥${(item.earning * 100).toFixed(0)}`,
              icon: 'none',
            })
          }}
        />
      </Chart>
    )
  }, [chartData])
  const chartNode = useMemo(() => renderChart(), [renderChart])
  if (loading || !weekStatistics) {
    return (
      <View className='week-statistics'>
        <Text className='week-title'>近7天统计</Text>
        <View className='loading'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='week-statistics'>
      <View className='week-stats-header'>
        <Text className='week-title'>近7天统计</Text>
        <View className='week-summary'>
          <Text className='summary-text'>
            订单: {weekStatistics.totalOrders || 0} |
            营收: {formatCurrency(weekStatistics.totalEarning)}
          </Text>
        </View>
      </View>

      <View className='chart-container'>
        <f2 render={chartNode} />
      </View>
    </View>
  )
}

export default WeekStatistics
