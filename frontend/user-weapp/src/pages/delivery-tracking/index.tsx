import React, { useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { pageTheme } from '../../styles/theme.stylex'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { useDeliveryTracking } from './hooks/useDeliveryTracking'

// 子组件
import TrackingMap from './components/TrackingMap'
import DeliveryWorkerCard from '../../components/DeliveryWorkerCard'
import AddressSummaryCard from '../../components/AddressSummaryCard'
import TrackingStatusBar from './components/TrackingStatusBar'

/** 配送跟踪页样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用) */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    display: 'flex',
    flexDirection: 'column',
    boxSizing: 'border-box'
  },
  loadingContainer: {
    flexGrow: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: '32rpx',
    columnGap: '32rpx'
  },
  loadingText: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)'
  },
  emptyGlyph: {
    fontSize: '72rpx'
  },
  // 原 .weui-empty__text .empty-desc 后代选择器拍平到子元素
  emptyDesc: {
    display: 'block',
    marginTop: '8rpx'
  }
})

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
      <View {...stylex.props(pageTheme, styles.page)}>
        <View {...stylex.props(styles.loadingContainer)}>
          <View className='weui-loadmore weui-loadmore_loading'>
            <View className='spinner' />
          </View>
          <Text {...stylex.props(styles.loadingText)}>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!order) {
    return (
      <View {...stylex.props(pageTheme, styles.page)}>
        <View className='weui-empty'>
          <View className='weui-empty__icon'>
            <Text {...stylex.props(styles.emptyGlyph)}>📍</Text>
          </View>
          <View className='weui-empty__text'>
            <Text>订单不存在</Text>
            <Text {...stylex.props(styles.emptyDesc)}>订单ID缺失或订单不可跟踪，请返回订单列表重试</Text>
          </View>
          <View className='weui-btn weui-btn_plain-primary weui-btn_mini' onClick={() => Taro.navigateBack()}>
            返回
          </View>
        </View>
      </View>
    )
  }

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
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
