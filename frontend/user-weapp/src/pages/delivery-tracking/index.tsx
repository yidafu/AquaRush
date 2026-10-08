import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton, AtToast, AtLoading } from 'taro-ui'
import Taro from '@tarojs/taro'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { useDeliveryTracking } from './hooks/useDeliveryTracking'

// 子组件
import TrackingMap from './components/TrackingMap'
import DeliveryWorkerCard from '../../components/DeliveryWorkerCard'
import AddressSummaryCard from '../../components/AddressSummaryCard'
import TrackingStatusBar from './components/TrackingStatusBar'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/loading.scss"
import './index.scss'

/**
 * 配送跟踪页:地图 + 配送员/地址卡片 + 实时跟踪提示
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
      <TrackingMap
        latitude={mapCenter.latitude}
        longitude={mapCenter.longitude}
        markers={mapMarkers}
        refreshing={refreshing}
        onRefresh={handleRefreshLocation}
        onShowRoute={handleShowRoute}
      />

      {/* 配送员信息卡片 */}
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

      {/* 配送地址信息 */}
      <AddressSummaryCard
        title='配送地址'
        receiverName={order.deliveryAddress.receiverName}
        phone={order.deliveryAddress.phone}
        province={order.deliveryAddress.province}
        city={order.deliveryAddress.city}
        district={order.deliveryAddress.district}
        detailAddress={order.deliveryAddress.detailAddress}
      />

      {/* 实时跟踪提示 */}
      <TrackingStatusBar />

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
