import React, { useState } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { AtTabs, AtTabsPane } from 'taro-ui'
import 'taro-ui/dist/style/components/tabs.scss'
import './index.scss'
import { FloatingButton } from '../../components/FloatingButton'
import {
  getPendingDeliveryOrders,
  getMyAssignedOrders,
  getMyDeliveringOrders,
  acceptDelivery,
  startDelivery,
} from '../../services/delivery'
import { useAuth } from '../../hooks/useAuth'
import {PageContainer} from '../../components/PageContainer'
import OrderCard from '../../components/OrderCard'

const TaskListPage: React.FC = () => {
  const { workerInfo } = useAuth()

  // Get current worker ID
  const getCurrentWorkerId = (): number => {
    return workerInfo?.id || 0
  }
  const [currentTab, setCurrentTab] = useState(0)
  const [pendingOrders, setPendingOrders] = useState<any[]>([])
  const [assignedOrders, setAssignedOrders] = useState<any[]>([])
  const [deliveringOrders, setDeliveringOrders] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  // 加载待接单订单
  const loadPendingOrders = async () => {
    try {
      const res = await getPendingDeliveryOrders()
      if (res?.data?.pendingDeliveryOrders) {
        setPendingOrders(res.data.pendingDeliveryOrders)
      }
    } catch (error) {
      console.error('加载待接单订单失败:', error)
    }
  }

  // 加载我的已接单订单
  const loadAssignedOrders = async () => {
    const workerId = getCurrentWorkerId()
    if (!workerId) return

    try {
      const res = await getMyAssignedOrders(workerId)
      if (res?.data?.assignedOrders) {
        setAssignedOrders(res.data.assignedOrders)
      }
    } catch (error) {
      console.error('加载已接单订单失败:', error)
    }
  }

  // 加载我的配送中订单
  const loadDeliveringOrders = async () => {
    const workerId = getCurrentWorkerId()
    if (!workerId) return

    try {
      const res = await getMyDeliveringOrders(workerId)
      if (res?.data?.deliveringOrders) {
        setDeliveringOrders(res.data.deliveringOrders)
      }
    } catch (error) {
      console.error('加载配送中订单失败:', error)
    }
  }

  // 加载所有数据
  const loadAllData = async () => {
    setLoading(true)
    await Promise.all([
      loadPendingOrders(),
      loadAssignedOrders(),
      loadDeliveringOrders(),
    ])
    setLoading(false)
  }

  // 接单
  const handleAcceptOrder = async (orderId: number) => {
    const workerId = getCurrentWorkerId()
    if (!workerId) return

    try {
      Taro.showLoading({ title: '接单中...' })
      await acceptDelivery(orderId, workerId)
      Taro.hideLoading()
      Taro.showToast({ title: '接单成功', icon: 'success' })
      loadAllData()
    } catch (error: any) {
      Taro.hideLoading()
      Taro.showToast({ title: error.message || '接单失败', icon: 'none' })
    }
  }

  // 开始配送
  const handleStartDelivery = async (orderId: number) => {
    try {
      Taro.showLoading({ title: '开始配送...' })
      await startDelivery(orderId)
      Taro.hideLoading()
      Taro.showToast({ title: '已开始配送', icon: 'success' })
      loadAllData()
    } catch (error: any) {
      Taro.hideLoading()
      Taro.showToast({ title: error.message || '操作失败', icon: 'none' })
    }
  }

  const tabList = [
    { title: `未接单 (${pendingOrders.length})` },
    { title: `已接单 (${assignedOrders.length})` },
    { title: `配送中 (${deliveringOrders.length})` },
  ]
  const handleCreateTask = () => {
    Taro.navigateTo({
      url: '/pages/create-order/index'
    })
  }

  return (
    <PageContainer title="配送订单">
      <View className='task-list-page'>
        <View className='tabs-container'>
          <AtTabs
            current={currentTab}
            tabList={tabList}
            onClick={setCurrentTab}
            swipeable={false}
            className='task-tabs'
          >
            <AtTabsPane current={currentTab} index={0}>
              {pendingOrders.length === 0 ? (
                <View className='empty'>
                  <Text>暂无待接单订单</Text>
                </View>
              ) : (
                pendingOrders.map((order) => (
                  <OrderCard
                    key={order.id}
                    order={order}
                    type='pending'
                    onAccept={handleAcceptOrder}
                  />
                ))
              )}
            </AtTabsPane>
            <AtTabsPane current={currentTab} index={1}>
              {assignedOrders.length === 0 ? (
                <View className='empty'>
                  <Text>暂无已接单订单</Text>
                </View>
              ) : (
                assignedOrders.map((order) => (
                  <OrderCard
                    key={order.id}
                    order={order}
                    type='assigned'
                    onStartDelivery={handleStartDelivery}
                  />
                ))
              )}
            </AtTabsPane>
            <AtTabsPane current={currentTab} index={2}>
              {deliveringOrders.length === 0 ? (
                <View className='empty'>
                  <Text>暂无配送中订单</Text>
                </View>
              ) : (
                deliveringOrders.map((order) => (
                  <OrderCard
                    key={order.id}
                    order={order}
                    type='delivering'
                  />
                ))
              )}
            </AtTabsPane>
          </AtTabs>
          </View>
        {/* 悬浮创建订单按钮 */}
        <FloatingButton onClick={handleCreateTask}>+</FloatingButton>
      </View>
    </PageContainer>
  )
}

export default TaskListPage
