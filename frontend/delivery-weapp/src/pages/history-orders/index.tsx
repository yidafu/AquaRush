import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, useReachBottom, usePullDownRefresh, useRouter } from '@tarojs/taro'
import { AtTabs, AtTabsPane } from 'taro-ui'
import 'taro-ui/dist/style/components/tabs.scss'
import './index.scss'
import { getDeliveryWorkerHistoryOrders } from '../../services/delivery'
import { useAuth } from '../../hooks/useAuth'
import { PageContainer } from '../../components/PageContainer'
import { OrderStatus } from '@aquarush/common'
import { OrderCard } from './components/OrderCard'

// Tab 列表
const TAB_LIST = [
  { title: '全部', status: undefined },
  { title: '已完成', status: OrderStatus.COMPLETED },
  { title: '已取消', status: OrderStatus.CANCELLED },
  { title: '已退款', status: OrderStatus.REFUNDED },
]

// 订单卡片组件已移动到 ./components/OrderCard.tsx

interface OrderItem {
  id: string
  orderNo: string
  quantity: number
  amount: number
  status: string
  isSelfCollect: boolean
  createdAt: string
  deliveryConfirmedAt?: string
  user?: {
    nickname: string
    phone: string
  }
  address?: {
    detailAddress: string
    receiverName: string
    phone: string
  }
  product?: {
    name: string
  }
}

const HistoryOrdersPage: React.FC = () => {
  const { workerInfo } = useAuth()
  const router = useRouter()
  const [currentTab, setCurrentTab] = useState(0)
  const [orders, setOrders] = useState<OrderItem[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [totalElements, setTotalElements] = useState(0)
  const [shouldReset, setShouldReset] = useState(false)

  const pageSize = 20

  // 根据 URL 参数初始化 tab
  useEffect(() => {
    const tabParam = router.params.tab
    if (tabParam !== undefined) {
      const tabIndex = parseInt(tabParam, 10)
      if (!isNaN(tabIndex) && tabIndex >= 0 && tabIndex < TAB_LIST.length) {
        setCurrentTab(tabIndex)
        setShouldReset(true)
      }
    }
  }, [router.params.tab])

  // 获取当前 worker ID
  const getCurrentWorkerId = useCallback((): string => {
    return workerInfo?.id?.toString() || ''
  }, [workerInfo?.id])

  // 加载历史订单
  const loadHistoryOrders = useCallback(async (reset = false) => {
    const workerId = getCurrentWorkerId()
    if (!workerId) return

    const currentPage = reset ? 0 : page
    const status = TAB_LIST[currentTab].status

    try {
      setLoading(true)
      const res = await getDeliveryWorkerHistoryOrders(
        status,
        currentPage,
        pageSize
      )

      if (res?.deliveryWorkerHistoryOrders) {
        const { content, totalElements: total, totalPages } = res.deliveryWorkerHistoryOrders
        const mappedContent = content.map(item => ({
          ...item,
          amount: Number(item.amount),
        })) as OrderItem[]
        setOrders((prev: OrderItem[]) => reset ? mappedContent : [...prev, ...mappedContent])
        setTotalElements(total)
        setHasMore(currentPage + 1 < totalPages)
        setPage(currentPage + 1)
      }
    } catch (error) {
      console.error('加载历史订单失败:', error)
      Taro.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }, [getCurrentWorkerId, page, currentTab])

  // Tab 切换
  const handleTabClick = useCallback((index: number) => {
    setCurrentTab(index)
    setPage(0)
    setOrders([])
    setHasMore(true)
    setShouldReset(true)
  }, [])

  // 监听 shouldReset 变化，在状态更新后执行加载
  useEffect(() => {
    if (shouldReset && workerInfo?.id) {
      loadHistoryOrders(true)
      setShouldReset(false)
    }
  }, [shouldReset, workerInfo?.id, loadHistoryOrders])

  // 页面显示时加载数据
  useDidShow(() => {
    if (workerInfo?.id) {
      loadHistoryOrders(true)
    }
  })

  // 下拉刷新
  usePullDownRefresh(async () => {
    await loadHistoryOrders(true)
    Taro.stopPullDownRefresh()
  })

  // 上拉加载更多
  useReachBottom(() => {
    if (!loading && hasMore) {
      loadHistoryOrders(false)
    }
  })

  return (
    <PageContainer title='历史订单'>
      <View className='history-orders-page'>
        <AtTabs
          current={currentTab}
          tabList={TAB_LIST}
          onClick={handleTabClick}
          swipeable={false}
          className='tabs-container'
        >
          {TAB_LIST.map((tab, index) => (
            <AtTabsPane current={currentTab} index={index} key={tab.status}>
              <ScrollView
                scrollY
                style={{ height: 'calc(100vh - 180px)' }}
              >
                {orders.length === 0 && !loading ? (
                  <View className='empty'>
                    <Text>暂无历史订单</Text>
                  </View>
                ) : (
                  orders.map((order) => (
                    <OrderCard key={order.id} order={order} />
                  ))
                )}
                {loading && (
                  <View className='load-more'>
                    <Text>加载中...</Text>
                  </View>
                )}
                {!hasMore && orders.length > 0 && (
                  <View className='load-more'>
                    <Text>没有更多了</Text>
                  </View>
                )}
              </ScrollView>
            </AtTabsPane>
          ))}
        </AtTabs>
      </View>
    </PageContainer>
  )
}

export default HistoryOrdersPage
