import { useState, useEffect, useRef, useCallback } from 'react'
import Taro, { useReady, useDidShow, useDidHide, usePullDownRefresh } from '@tarojs/taro'

export interface DeliveryLocation {
  latitude: number
  longitude: number
  address?: string
  timestamp: number
}

export interface DeliveryWorker {
  id: string
  name: string
  phone: string
  avatar?: string
  currentLocation?: DeliveryLocation
}

export interface DeliveryOrder {
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
}

export interface UseDeliveryTrackingResult {
  order: DeliveryOrder | null
  deliveryWorker: DeliveryWorker | null
  mapMarkers: any[]
  mapCenter: {
    latitude: number
    longitude: number
  }
  loading: boolean
  refreshing: boolean
  showToastVisible: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  hideToast: () => void
  handleContactDelivery: () => void
  handleShowRoute: () => void
  handleRefreshLocation: () => Promise<void>
}

/**
 * 配送实时跟踪 Hook(配送跟踪页):
 * 订单跟踪信息加载(含模拟数据兜底)/用户定位/地图标记/10s 轮询配送员位置,
 * 页面显示时启动跟踪、隐藏时停止,卸载清理定时器。
 */
export const useDeliveryTracking = (): UseDeliveryTrackingResult => {
  const [orderId, setOrderId] = useState<string>('')
  const [order, setOrder] = useState<DeliveryOrder | null>(null)
  const [deliveryWorker, setDeliveryWorker] = useState<DeliveryWorker | null>(null)
  const [mapMarkers, setMapMarkers] = useState<any[]>([])
  const [mapCenter, setMapCenter] = useState({
    latitude: 22.547, // 默认深圳坐标
    longitude: 114.085
  })
  const [loading, setLoading] = useState(true)
  const [showToastVisible, setShowToastVisible] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [refreshing, setRefreshing] = useState(false)
  // 10s 轮询定时器(页面隐藏/卸载时清理)
  const trackingIntervalRef = useRef<NodeJS.Timeout | null>(null)

  const showToast = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToastVisible(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToastVisible(false)
  }, [])

  // 获取用户当前位置
  const getCurrentLocation = useCallback(async () => {
    try {
      const location = await Taro.getLocation({
        type: 'gcj02'
      })

      setMapCenter({
        latitude: location.latitude,
        longitude: location.longitude
      })

      return location
    } catch (error) {
      console.error('获取用户位置失败:', error)
      showToast('获取位置信息失败', 'error')
      return null
    }
  }, [showToast])

  // 更新地图标记
  const updateMapMarkers = useCallback((order: DeliveryOrder, worker: DeliveryWorker) => {
    const markers: any[] = []

    // 配送员位置标记
    if (worker.currentLocation) {
      markers.push({
        id: 1,
        latitude: worker.currentLocation.latitude,
        longitude: worker.currentLocation.longitude,
        iconPath: '/assets/delivery-marker.png',
        width: 30,
        height: 30,
        callout: {
          content: worker.name,
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

    setMapMarkers(markers)
  }, [])

  // 加载订单跟踪信息
  const loadOrderTracking = useCallback(async (id: string) => {
    try {
      setLoading(true)

      // TODO: 实际项目中调用获取订单跟踪信息的API
      // const trackingData = await getOrderTracking(id)

      // 模拟订单跟踪数据
      const mockOrder: DeliveryOrder = {
        id,
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

      setOrder(mockOrder)
      setDeliveryWorker(mockDeliveryWorker)

      updateMapMarkers(mockOrder, mockDeliveryWorker)
    } catch (error) {
      console.error('加载订单跟踪失败:', error)
      showToast('加载跟踪信息失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [updateMapMarkers, showToast])

  // 更新配送员位置
  const updateDeliveryWorkerLocation = useCallback(async () => {
    try {
      // TODO: 实际项目中调用获取配送员实时位置的API
      // const location = await getDeliveryWorkerLocation(orderId)

      // 模拟配送员移动
      const mockNewLocation = {
        latitude: deliveryWorker?.currentLocation?.latitude || 22.5389,
        longitude: (deliveryWorker?.currentLocation?.longitude || 113.9250) + 0.001,
        address: '科苑北路',
        timestamp: Date.now()
      }

      setDeliveryWorker(prev => prev ? {
        ...prev,
        currentLocation: mockNewLocation
      } : null)

      if (order && deliveryWorker) {
        updateMapMarkers(order, {
          ...deliveryWorker,
          currentLocation: mockNewLocation
        })
      }
    } catch (error) {
      console.error('更新配送员位置失败:', error)
    }
  }, [order, deliveryWorker, updateMapMarkers])

  // 开始实时跟踪
  const startRealTimeTracking = useCallback(() => {
    if (trackingIntervalRef.current) {
      return
    }
    const interval = setInterval(() => {
      if (orderId) {
        updateDeliveryWorkerLocation()
      }
    }, 10000) // 每10秒更新一次
    trackingIntervalRef.current = interval
  }, [orderId, updateDeliveryWorkerLocation])

  // 停止实时跟踪
  const stopRealTimeTracking = useCallback(() => {
    if (trackingIntervalRef.current) {
      clearInterval(trackingIntervalRef.current)
      trackingIntervalRef.current = null
    }
  }, [])

  // 联系配送员
  const handleContactDelivery = useCallback(() => {
    if (!deliveryWorker?.phone) return

    Taro.makePhoneCall({
      phoneNumber: deliveryWorker.phone
    })
  }, [deliveryWorker])

  // 查看配送路线
  const handleShowRoute = useCallback(() => {
    if (!deliveryWorker?.currentLocation || !order?.deliveryAddress.latitude) {
      showToast('位置信息不完整', 'error')
      return
    }

    // 使用微信内置地图查看路线
    Taro.openLocation({
      // 上方守卫已确保 deliveryAddress.latitude/longitude 存在(仅类型收窄不到,断言不改运行时)
      latitude: order.deliveryAddress.latitude!,
      longitude: order.deliveryAddress.longitude!,
      name: '配送地址',
      address: `${order.deliveryAddress.province}${order.deliveryAddress.city}${order.deliveryAddress.district}${order.deliveryAddress.detailAddress}`,
      scale: 16
    })
  }, [order, deliveryWorker, showToast])

  // 刷新位置
  const handleRefreshLocation = useCallback(async () => {
    setRefreshing(true)

    try {
      await getCurrentLocation()
      await updateDeliveryWorkerLocation()
      showToast('位置已更新', 'success')
    } catch (error) {
      showToast('更新失败', 'error')
    } finally {
      setRefreshing(false)
    }
  }, [getCurrentLocation, updateDeliveryWorkerLocation, showToast])

  // 组件挂载时获取页面参数
  useReady(() => {
    const instance = Taro.getCurrentInstance()
    const { orderId: paramId } = instance.router?.params || {}

    if (paramId) {
      setOrderId(paramId)
      loadOrderTracking(paramId)
    } else {
      showToast('订单ID缺失', 'error')
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    }
  })

  // 页面显示时开始实时跟踪
  useDidShow(() => {
    if (orderId) {
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
    setRefreshing(true)
    if (orderId) {
      loadOrderTracking(orderId).finally(() => {
        Taro.stopPullDownRefresh()
        setRefreshing(false)
      })
    } else {
      Taro.stopPullDownRefresh()
      setRefreshing(false)
    }
  })

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      stopRealTimeTracking()
    }
  }, [stopRealTimeTracking])

  return {
    order,
    deliveryWorker,
    mapMarkers,
    mapCenter,
    loading,
    refreshing,
    showToastVisible,
    toastText,
    toastType,
    hideToast,
    handleContactDelivery,
    handleShowRoute,
    handleRefreshLocation
  }
}
