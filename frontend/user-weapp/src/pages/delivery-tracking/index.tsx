import React, { useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { useDeliveryTracking } from './hooks/useDeliveryTracking'

// 子组件
import TrackingMap from './components/TrackingMap'
import DeliveryWorkerCard from '../../components/DeliveryWorkerCard'
import AddressSummaryCard from '../../components/AddressSummaryCard'
import TrackingStatusBar from './components/TrackingStatusBar'

import './index.scss'

/**
 * 配送跟踪页(设计稿 02-order/delivery-tracking.html WeUI 视觉):
 * 地图 + 配送员信息卡(.weui-cells)+ 配送地址 + 实时跟踪提示(.weui-notice--brand)。
 * 跟踪逻辑(定位/轮询/地图标记)封装在 hooks/useDeliveryTracking,渲染由 components/ 完成。
 */
const DeliveryTracking: React.FC = () => {
  usePageViewTracking()

  const {
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
  } = useDeliveryTracking()

  // WeUI toast 视觉容器:保留原 AtToast 的 3s 自动关闭行为
  useEffect(() => {
    if (!showToastVisible) return
    const timer = setTimeout(hideToast, 3000)
    return () => clearTimeout(timer)
  }, [showToastVisible, toastText, toastType, hideToast])

  if (loading) {
    return (
      <View className='delivery-tracking-page'>
        <View className='loading-container'>
          <View className='weui-loadmore weui-loadmore_loading'>
            <View className='spinner' />
          </View>
          <Text className='loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!order) {
    return (
      <View className='delivery-tracking-page'>
        <View className='weui-empty'>
          <View className='weui-empty__icon'>
            <Text className='empty-glyph'>📍</Text>
          </View>
          <View className='weui-empty__text'>
            <Text>订单不存在</Text>
            <Text className='empty-desc'>订单ID缺失或订单不可跟踪，请返回订单列表重试</Text>
          </View>
          <View className='weui-btn weui-btn_plain-primary weui-btn_mini' onClick={() => Taro.navigateBack()}>
            返回
          </View>
        </View>
      </View>
    )
  }

  return (
    <View className='delivery-tracking-page'>
      {/* 地图区域 */}
      <TrackingMap
        latitude={mapCenter.latitude}
        longitude={mapCenter.longitude}
        markers={mapMarkers}
        refreshing={refreshing}
        onRefresh={handleRefreshLocation}
        onShowRoute={handleShowRoute}
      />

      {/* 配送员信息卡片(.weui-cells) */}
      {deliveryWorker && (
        <DeliveryWorkerCard
          name={deliveryWorker.name}
          phone={deliveryWorker.phone}
          avatar={deliveryWorker.avatar}
          location={deliveryWorker.currentLocation?.address}
          showStatus
          estimatedDeliveryTime={order.estimatedDeliveryTime}
          onContact={handleContactDelivery}
        />
      )}

      {/* 实时跟踪提示(.weui-notice--brand) */}
      <TrackingStatusBar />

      {/* 配送地址信息(.weui-cells) */}
      <AddressSummaryCard
        title='配送地址'
        receiverName={order.deliveryAddress.receiverName}
        phone={order.deliveryAddress.phone}
        province={order.deliveryAddress.province}
        city={order.deliveryAddress.city}
        district={order.deliveryAddress.district}
        detailAddress={order.deliveryAddress.detailAddress}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />

      {/* Toast 提示(WeUI .weui-toast 视觉容器,状态逻辑与原 AtToast 一致) */}
      {showToastVisible && (
        <View className='weui-toast weui-toast_text'>
          <Text className='weui-toast__text'>{toastText}</Text>
        </View>
      )}
    </View>
  )
}

export default DeliveryTracking
