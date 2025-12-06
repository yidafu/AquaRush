import React, { useState, useEffect } from 'react'
import { View, Text, Button, Image, Map } from '@tarojs/components'
import { AtCard, AtButton, AtIcon, AtToast, AtLoading, AtTag } from 'taro-ui'
import Taro, { useReady, useDidShow, useDidHide, usePullDownRefresh } from '@tarojs/taro'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/loading.scss"
import "taro-ui/dist/style/components/tag.scss"
import './index.scss'

interface DeliveryLocation {
  latitude: number
  longitude: number
  address?: string
  timestamp: number
}

interface DeliveryWorker {
  id: string
  name: string
  phone: string
  avatar?: string
  currentLocation?: DeliveryLocation
}

interface DeliveryTrackingState {
  orderId: string
  order: {
    id: string
    orderNo: string
    status: string
    deliveryAddress: {
      receiverName: string
      phone: string
      province: string
      city: string
      district: string
      detailAddress: string
      latitude?: number
      longitude?: number
    }
    estimatedDeliveryTime?: string
  } | null
  deliveryWorker: DeliveryWorker | null
  userLocation: DeliveryLocation | null
  mapMarkers: any[]
  mapCenter: {
    latitude: number
    longitude: number
  }
  loading: boolean
  showToastVisible: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  refreshing: boolean
  trackingInterval: NodeJS.Timeout | null
}

const DeliveryTracking: React.FC = () => {
  const [state, setState] = useState<DeliveryTrackingState>({
    orderId: '',
    order: null,
    deliveryWorker: null,
    userLocation: null,
    mapMarkers: [],
    mapCenter: {
      latitude: 22.547, // 默认深圳坐标
      longitude: 114.085
    },
    loading: true,
    showToastVisible: false,
    toastText: '',
    toastType: 'success',
    refreshing: false,
    trackingInterval: null
  })

  // 组件挂载时获取页面参数
  useReady(() => {
    const instance = Taro.getCurrentInstance()
    const { orderId } = instance.router?.params || {}

    if (orderId) {
      setState(prev => ({ ...prev, orderId }))
      loadOrderTracking(orderId)
    } else {
      showToast('订单ID缺失', 'error')
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    }
  })

  // 页面显示时开始实时跟踪
  useDidShow(() => {
    if (state.orderId) {
      startRealTimeTracking()
      // 获取用户当前位置
      getCurrentLocation()
    }
  })

  // 页面隐藏时停止实时跟踪
  useDidHide(() => {
    stopRealTimeTracking()
  })

  // 下拉刷新
  usePullDownRefresh(() => {
    setState(prev => ({ ...prev, refreshing: true }))
    if (state.orderId) {
      loadOrderTracking(state.orderId).finally(() => {
        Taro.stopPullDownRefresh()
        setState(prev => ({ ...prev, refreshing: false }))
      })
    } else {
      Taro.stopPullDownRefresh()
      setState(prev => ({ ...prev, refreshing: false }))
    }
  })

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      stopRealTimeTracking()
    }
  }, [])

  const showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setState(prev => ({
      ...prev,
      showToastVisible: true,
      toastText: text,
      toastType: type
    }))
  }

  const hideToast = () => {
    setState(prev => ({ ...prev, showToastVisible: false }))
  }

  // 获取用户当前位置
  const getCurrentLocation = async () => {
    try {
      const location = await Taro.getLocation({
        type: 'gcj02'
      })

      const userLocation: DeliveryLocation = {
        latitude: location.latitude,
        longitude: location.longitude,
        timestamp: Date.now()
      }

      setState(prev => ({
        ...prev,
        userLocation,
        mapCenter: {
          latitude: location.latitude,
          longitude: location.longitude
        }
      }))

      return location
    } catch (error) {
      console.error('获取用户位置失败:', error)
      showToast('获取位置信息失败', 'error')
      return null
    }
  }

  // 加载订单跟踪信息
  const loadOrderTracking = async (orderId: string) => {
    try {
      setState(prev => ({ ...prev, loading: true }))

      // TODO: 实际项目中调用获取订单跟踪信息的API
      // const trackingData = await getOrderTracking(orderId)

      // 模拟订单跟踪数据
      const mockOrder = {
        id: orderId,
        orderNo: 'ORD202412020001',
        status: 'delivering',
        deliveryAddress: {
          receiverName: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号',
          latitude: 22.5319,
          longitude: 113.9320
        },
        estimatedDeliveryTime: '2024-12-02 11:15:00'
      }

      const mockDeliveryWorker: DeliveryWorker = {
        id: 'DW001',
        name: '配送员小李',
        phone: '13600136000',
        avatar: '/assets/delivery-worker.jpg',
        currentLocation: {
          latitude: 22.5389,
          longitude: 113.9250,
          address: '科技园北区',
          timestamp: Date.now()
        }
      }

      setState(prev => ({
        ...prev,
        order: mockOrder,
        deliveryWorker: mockDeliveryWorker
      }))

      updateMapMarkers(mockOrder, mockDeliveryWorker)

    } catch (error) {
      console.error('加载订单跟踪失败:', error)
      showToast('加载跟踪信息失败', 'error')
    } finally {
      setState(prev => ({ ...prev, loading: false }))
    }
  }

  // 更新地图标记
  const updateMapMarkers = (order: any, deliveryWorker: DeliveryWorker) => {
    const markers = []

    // 配送员位置标记
    if (deliveryWorker.currentLocation) {
      markers.push({
        id: 1,
        latitude: deliveryWorker.currentLocation.latitude,
        longitude: deliveryWorker.currentLocation.longitude,
        iconPath: '/assets/delivery-marker.png',
        width: 30,
        height: 30,
        callout: {
          content: deliveryWorker.name,
          padding: 6,
          borderRadius: 4,
          display: 'ALWAYS'
        }
      })
    }

    // 配送地址标记
    if (order.deliveryAddress.latitude && order.deliveryAddress.longitude) {
      markers.push({
        id: 2,
        latitude: order.deliveryAddress.latitude,
        longitude: order.deliveryAddress.longitude,
        iconPath: '/assets/destination-marker.png',
        width: 30,
        height: 30,
        callout: {
          content: '配送地址',
          padding: 6,
          borderRadius: 4,
          display: 'ALWAYS'
        }
      })
    }

    setState(prev => ({
      ...prev,
      mapMarkers: markers
    }))
  }

  // 开始实时跟踪
  const startRealTimeTracking = () => {
    if (state.trackingInterval) {
      return
    }

    const interval = setInterval(() => {
      if (state.orderId) {
        updateDeliveryWorkerLocation()
      }
    }, 10000) // 每10秒更新一次

    setState(prev => ({
      ...prev,
      trackingInterval: interval
    }))
  }

  // 停止实时跟踪
  const stopRealTimeTracking = () => {
    if (state.trackingInterval) {
      clearInterval(state.trackingInterval)
      setState(prev => ({
        ...prev,
        trackingInterval: null
      }))
    }
  }

  // 更新配送员位置
  const updateDeliveryWorkerLocation = async () => {
    try {
      // TODO: 实际项目中调用获取配送员实时位置的API
      // const location = await getDeliveryWorkerLocation(state.orderId)

      // 模拟配送员移动
      const mockNewLocation = {
        latitude: state.deliveryWorker?.currentLocation?.latitude || 22.5389,
        longitude: (state.deliveryWorker?.currentLocation?.longitude || 113.9250) + 0.001,
        address: '科苑北路',
        timestamp: Date.now()
      }

      setState(prev => ({
        ...prev,
        deliveryWorker: prev.deliveryWorker ? {
          ...prev.deliveryWorker,
          currentLocation: mockNewLocation
        } : null
      }))

      if (state.order && state.deliveryWorker) {
        updateMapMarkers(state.order, {
          ...state.deliveryWorker,
          currentLocation: mockNewLocation
        })
      }

    } catch (error) {
      console.error('更新配送员位置失败:', error)
    }
  }

  // 联系配送员
  const handleContactDelivery = () => {
    if (!state.deliveryWorker?.phone) return

    Taro.makePhoneCall({
      phoneNumber: state.deliveryWorker.phone
    })
  }

  // 查看配送路线
  const handleShowRoute = () => {
    if (!state.deliveryWorker?.currentLocation || !state.order?.deliveryAddress.latitude) {
      showToast('位置信息不完整', 'error')
      return
    }

    // 使用微信内置地图查看路线
    Taro.openLocation({
      latitude: state.order.deliveryAddress.latitude,
      longitude: state.order.deliveryAddress.longitude,
      name: '配送地址',
      address: `${state.order.deliveryAddress.province}${state.order.deliveryAddress.city}${state.order.deliveryAddress.district}${state.order.deliveryAddress.detailAddress}`,
      scale: 16
    })
  }

  // 刷新位置
  const handleRefreshLocation = async () => {
    setState(prev => ({ ...prev, refreshing: true }))

    try {
      await getCurrentLocation()
      await updateDeliveryWorkerLocation()
      showToast('位置已更新', 'success')
    } catch (error) {
      showToast('更新失败', 'error')
    } finally {
      setState(prev => ({ ...prev, refreshing: false }))
    }
  }

  const { order, deliveryWorker, mapMarkers, mapCenter, loading, showToastVisible, toastText, toastType, refreshing } = state

  if (loading) {
    return (
      <View className='delivery-tracking-page'>
        <View className='loading-container'>
          <AtLoading mode='circle' size='32' />
          <Text className='loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!order) {
    return (
      <View className='delivery-tracking-page'>
        <View className='error-container'>
          <Text>订单不存在</Text>
          <AtButton
            type='primary'
            size='small'
            onClick={() => Taro.navigateBack()}
          >
            返回
          </AtButton>
        </View>
      </View>
    )
  }

  return (
    <View className='delivery-tracking-page'>
      {/* 地图区域 */}
      <View className='map-container'>
        <Map
          className='delivery-map'
          latitude={mapCenter.latitude}
          longitude={mapCenter.longitude}
          scale={14}
          markers={mapMarkers}
          showLocation={true}
          showScale={true}
          showCompass={true}
          enableScroll={true}
          enableZoom={true}
          enableRotate={false}
          enable3D={false}
        />

        {/* 地图控制按钮 */}
        <View className='map-controls'>
          <AtButton
            type='primary'
            size='small'
            circle
            onClick={handleRefreshLocation}
            loading={refreshing}
            className='control-button'
          >
            <AtIcon value='refresh' size='16' color='white' />
          </AtButton>
          <AtButton
            type='primary'
            size='small'
            circle
            onClick={handleShowRoute}
            className='control-button'
          >
            <AtIcon value='navigation' size='16' color='white' />
          </AtButton>
        </View>
      </View>

      {/* 配送员信息卡片 */}
      {deliveryWorker && (
        <AtCard
          title='配送员信息'
          className='delivery-worker-card'
        >
          <View className='worker-info'>
            <View className='worker-header'>
              <View className='worker-avatar'>
                <Image
                  src={deliveryWorker.avatar || '/assets/default-avatar.png'}
                  mode='aspectFill'
                  className='avatar-image'
                />
              </View>
              <View className='worker-details'>
                <Text className='worker-name'>{deliveryWorker.name}</Text>
                <Text className='worker-phone'>{deliveryWorker.phone}</Text>
                {deliveryWorker.currentLocation && (
                  <Text className='worker-location'>
                    <AtIcon value='map-pin' size='12' color='#999' />
                    {deliveryWorker.currentLocation.address || '位置更新中...'}
                  </Text>
                )}
              </View>
              <View className='worker-actions'>
                <AtButton
                  type='primary'
                  size='small'
                  circle
                  onClick={handleContactDelivery}
                >
                  <AtIcon value='phone' size='14' color='white' />
                </AtButton>
              </View>
            </View>

            {/* 配送状态标签 */}
            <View className='delivery-status'>
              <AtTag
                type='primary'
                size='small'
                circle
                customStyle={{
                  background: 'var(--theme-success)',
                  color: 'white',
                  border: 'none'
                }}
              >
                配送中
              </AtTag>
              <Text className='status-text'>
                预计 {order.estimatedDeliveryTime} 送达
              </Text>
            </View>
          </View>
        </AtCard>
      )}

      {/* 配送地址信息 */}
      <AtCard
        title='配送地址'
        className='address-card'
      >
        <View className='address-content'>
          <View className='address-header'>
            <Text className='receiver-name'>{order.deliveryAddress.receiverName}</Text>
            <Text className='receiver-phone'>{order.deliveryAddress.phone}</Text>
          </View>
          <Text className='address-detail'>
            {order.deliveryAddress.province} {order.deliveryAddress.city} {order.deliveryAddress.district} {order.deliveryAddress.detailAddress}
          </Text>
        </View>
      </AtCard>

      {/* 实时跟踪提示 */}
      <View className='tracking-info'>
        <View className='tracking-status'>
          <AtIcon value='clock' size='16' color='var(--theme-primary)' />
          <Text className='tracking-text'>实时跟踪中，每10秒更新位置</Text>
        </View>
      </View>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToastVisible}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />
    </View>
  )
}

export default DeliveryTracking