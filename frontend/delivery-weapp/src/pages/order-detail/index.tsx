import React, { useState, useEffect, useRef } from 'react'
import { View, Text, ScrollView, Button } from '@tarojs/components'
import Taro, { useRouter, useShareAppMessage } from '@tarojs/taro'
import { AtButton, AtActionSheet, AtActionSheetItem, AtSteps } from 'taro-ui'
import 'taro-ui/dist/style/components/button.scss'
import 'taro-ui/dist/style/components/action-sheet.scss'
import 'taro-ui/dist/style/components/icon.scss'
import 'taro-ui/dist/style/components/steps.scss'
import 'taro-ui/dist/style/components/modal.scss'
import { formatDateTime, OrderStatus } from '@aquarush/common'

import './index.scss'

import { getOrderDetail, getOrderOperations, acceptDelivery, startDelivery, completeDelivery } from '../../services/delivery'
import { PageContainer } from '../../components/PageContainer'
import { AddressCard } from './components/AddressCard'
import { ProductCard } from './components/ProductCard'
import { OrderInfoCard } from './components/OrderInfoCard'
import { DeliveryInfoCard } from './components/DeliveryInfoCard'
import { PosterCanvas } from './components/PosterCanvas'

interface OrderOperation {
  id: string
  operationType: string
  description?: string
  createdAt: string
}


// Order status mapping
const ORDER_STATUS_MAP: Record<string, string> = {
  PENDING_DISPATCH: '待接单',
  PENDING_DELIVERY: '待配送',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
  PENDING_PAYMENT: '待支付',
}

// Order progress steps for AtSteps component
const ORDER_STEPS = [
  { title: '待接单' },
  { title: '待配送' },
  { title: '配送中' },
  { title: '已完成' },
]

// Get current step index based on order status
const getCurrentStep = (status: string): number => {
  const statusToStep: Record<string, number> = {
    PENDING_DISPATCH: 0,
    PENDING_DELIVERY: 1,
    DELIVERING: 2,
    COMPLETED: 3,
  }
  return statusToStep[status] ?? 0
}

// Operation type mapping
const OPERATION_TYPE_MAP: Record<string, string> = {
  ORDER_CREATED: '订单创建',
  DELIVERY_ASSIGNED: '分配配送',
  DELIVERY_STARTED: '开始配送',
  DELIVERY_COMPLETED: '配送完成',
  ORDER_COMPLETED: '订单完成',
  ORDER_CANCELLED: '订单取消',
  ORDER_PAID: '订单支付',
  PAYMENT_TIMEOUT: '支付超时',
  REFUND_COMPLETED: '退款完成',
}


// ============ Card Components ============


interface OperationsCardProps {
  operations?: OrderOperation[]
}

const OperationsCard: React.FC<OperationsCardProps> = ({ operations }) => {
  if (!operations?.length) return null

  return (
    <View className='card operations-card'>
      <View className='card-header'>
        <Text className='card-title'>订单记录</Text>
      </View>
      <View className='card-content'>
        {operations.map((op, index) => (
          <View key={op.id || index} className='operation-item'>
            <View className='operation-dot' />
            <View className='operation-content'>
              <Text className='operation-type'>
                {OPERATION_TYPE_MAP[op.operationType] || op.operationType}
              </Text>
              {op.description && (
                <Text className='operation-desc'>{op.description}</Text>
              )}
              <Text className='operation-time'>{formatDateTime(op.createdAt)}</Text>
            </View>
          </View>
        ))}
      </View>
    </View>
  )
}

interface BottomButtonsProps {
  status: string
  loading: boolean
  onAccept: () => void
  onStartDelivery: () => void
  onOpenPaymentSheet: () => void
  onConfirmSelfCollect: () => void
  hasDeliveryInfo?: boolean
  isSelfCollect?: boolean
}

const BottomButtons: React.FC<BottomButtonsProps> = ({
  status,
  loading,
  onAccept,
  onStartDelivery,
  onOpenPaymentSheet,
  onConfirmSelfCollect,
  hasDeliveryInfo = false,
  isSelfCollect = false,
}) => {
  return (
    <View className='bottom-buttons'>
      {status === OrderStatus.PENDING_DISPATCH && (
        <AtButton
          type='primary'
          loading={loading}
          onClick={onAccept}
          className='action-button'
        >
          接单
        </AtButton>
      )}

      {status === OrderStatus.PENDING_DELIVERY && (
        <AtButton
          type='primary'
          loading={loading}
          onClick={onStartDelivery}
          className='action-button'
        >
          开始配送
        </AtButton>
      )}

      {status === OrderStatus.DELIVERING && (
        <View className='delivery-buttons'>
          <AtButton
            type={hasDeliveryInfo ? 'primary' : 'secondary'}
            loading={loading}
            onClick={isSelfCollect ? onConfirmSelfCollect : onOpenPaymentSheet}
            className='action-button'
            disabled={!hasDeliveryInfo}
          >
            {hasDeliveryInfo ? '确认送达' : '请先填写配送信息'}
          </AtButton>
        </View>
      )}
    </View>
  )
}

// ============ Main Page Component ============

const OrderDetailPage: React.FC = () => {
  const [order, setOrder] = useState<any>(null)
  const [orderOperations, setOrderOperations] = useState<OrderOperation[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [paymentSheetVisible, setPaymentSheetVisible] = useState(false)
  // 配送信息状态（仅在配送中状态使用）
  const [deliveryPhotos, setDeliveryPhotos] = useState<string[]>([])
  const [deliveryRemark, setDeliveryRemark] = useState('')
  const router = useRouter()

  // Get order No from params

  const orderNo = router.params.id || ''
  // PosterCanvas 组件 ref
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const posterCanvasRef = useRef<any>(null)
  // 标记海报是否已生成
  const posterGeneratedRef = useRef(false)

  // 使用 ref 存储 order 和海报，确保 useShareAppMessage 能获取最新数据
  const orderRef = useRef(order)
  const posterRef = useRef('')
  useEffect(() => {
    orderRef.current = order
  }, [order])


  // 分享订单地址
  useShareAppMessage((res) => {
    const currentOrder = orderRef.current
    if (!currentOrder?.address) {
      return {
        title: '订单详情',
        path: `/pages/order-detail/index?id=${currentOrder?.orderNo || ''}`,
      }
    }
    const { address } = currentOrder
    const fullAddress = `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`

    // 使用预生成的海报作为分享封面
    const promise = new Promise((resolve) => {
      const imageUrl = posterRef.current || undefined
      if (imageUrl) {
        return resolve({
          title: `${address.receiverName} - ${fullAddress}`,
          path: `/pages/order-detail/index?id=${currentOrder?.orderNo || ''}`,
          imageUrl
        });
      }
      handleGeneratePoster().then(() => {
        resolve({
          title: `${address.receiverName} - ${fullAddress}`,
          path: `/pages/order-detail/index?id=${currentOrder?.orderNo || ''}`,
          imageUrl: posterRef.current
        })
      })
    })
    return promise
  })

  // Load order detail
  const loadOrderDetail = async () => {
    if (!orderNo) {
      Taro.showToast({ title: '订单不存在', icon: 'none' })
      return
    }

    setLoading(true)
    try {
      const res = await getOrderDetail(orderNo)
      if (res?.orderByNo) {
        setOrder(res.orderByNo)
        // Load order operations separately
        if (res.orderByNo.id) {
          const opsRes = await getOrderOperations(res.orderByNo.id)
          if (opsRes?.orderOperations) {
            setOrderOperations(opsRes.orderOperations)
          }
        }
      }
    } catch (error) {
      console.error('获取订单详情失败:', error)
      Taro.showToast({ title: '获取订单详情失败', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    // 启用分享菜单
    Taro.showShareMenu({
      withShareTicket: true,
      showShareItems: ['shareAppMessage']
    })
    loadOrderDetail()
  }, [orderNo])

  // Handle accept order
  const handleAcceptOrder = async () => {
    setActionLoading(true)
    try {
      await acceptDelivery(order?.id)
      Taro.showToast({ title: '接单成功', icon: 'success' })
      loadOrderDetail()
    } catch (error) {
      console.error('接单失败:', error)
      Taro.showToast({ title: '接单失败', icon: 'none' })
    } finally {
      setActionLoading(false)
    }
  }

  // Handle start delivery
  const handleStartDelivery = async () => {
    setActionLoading(true)
    try {
      await startDelivery(order?.id)
      Taro.showToast({ title: '开始配送成功', icon: 'success' })
      loadOrderDetail()
    } catch (error) {
      console.error('开始配送失败:', error)
      Taro.showToast({ title: '开始配送失败', icon: 'none' })
    } finally {
      setActionLoading(false)
    }
  }

  // Handle payment collection - 使用配送信息卡片中的照片和备注
  const handlePaymentCollection = async (paymentType: string) => {
    setPaymentSheetVisible(false)

    // 验证是否已填写配送信息
    if (deliveryPhotos.length === 0) {
      Taro.showToast({ title: '请先拍摄配送照片', icon: 'none' })
      return
    }

    setActionLoading(true)
    try {
      await completeDelivery(order?.id, deliveryPhotos, paymentType, deliveryRemark || undefined)
      Taro.showToast({ title: '配送完成', icon: 'success' })
      // 清空配送信息状态
      setDeliveryPhotos([])
      setDeliveryRemark('')
      loadOrderDetail()
    } catch (error) {
      console.error('完成配送失败:', error)
      Taro.showToast({ title: '完成配送失败', icon: 'none' })
    } finally {
      setActionLoading(false)
    }
  }

  // 生成海报用于分享
  const handleGeneratePoster = async () => {
    if (!order) return

    const canvasRef = posterCanvasRef.current
    if (canvasRef?.render) {
      try {
        const posterPath = await canvasRef.render(order)
        console.log('生成海报结果', posterPath)
        if (posterPath) {
          posterRef.current = posterPath
          posterGeneratedRef.current = true
        }
      } catch (error) {
        console.error('生成海报失败:', error)
      }
    }
  }

  if (loading) {
    return (
      <PageContainer>
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      </PageContainer>
    )
  }

  if (!order) {
    return (
      <PageContainer>
        <View className='empty-container'>
          <Text>订单不存在</Text>
        </View>
      </PageContainer>
    )
  }

  return (
    <PageContainer>
      <ScrollView className='page-content' scrollY>
        {/* Order Status Steps */}
        {order.status !== 'CANCELLED' && order.status !== 'REFUNDED' && (
          // @ts-ignore TypeScript compatibility issue between taro-ui and React 18
          <AtSteps
            items={ORDER_STEPS}
            current={getCurrentStep(order.status)}
            onChange={() => { }}
          />

        )}
        {order.status === OrderStatus.CANCELLED && (
          <View className='status-banner'>
            <Text className='status-text'>{ORDER_STATUS_MAP[order.status]}</Text>
          </View>
        )}
        {order.status === OrderStatus.REFUNDED && (
          <View className='status-banner'>
            <Text className='status-text'>{ORDER_STATUS_MAP[order.status]}</Text>
          </View>
        )}

        {/* Address */}
        <AddressCard address={order.address} />

        {/* 分享按钮 */}
        {/* <View className='poster-actions'>
          <Button
            className='poster-action-btn'
            open-type='share'
          >
            <Text>分享</Text>
          </Button>
        </View>
 */}

        {/* PosterCanvas 组件 */}
        <PosterCanvas ref={posterCanvasRef} />
        {/* Product */}
        <ProductCard
          product={order.product}
          quantity={order.quantity}
          remark={order.remark}
        />

        {/* Delivery Info Card - Only show in DELIVERING status */}
        {(order.status === OrderStatus.DELIVERING || order.status === OrderStatus.COMPLETED) && (
          <DeliveryInfoCard
            photos={deliveryPhotos}
            remark={deliveryRemark}
            onPhotosChange={setDeliveryPhotos}
            onRemarkChange={setDeliveryRemark}
          />
        )}

        {/* Order Operations */}
        <OperationsCard operations={orderOperations} />

        {/* Order Info */}
        <OrderInfoCard order={order} />

        {/* Spacer for bottom buttons */}
        <View className='bottom-spacer' />
      </ScrollView>

      {/* Bottom Buttons */}
      <BottomButtons
        status={order.status}
        loading={actionLoading}
        onAccept={handleAcceptOrder}
        onStartDelivery={handleStartDelivery}
        onOpenPaymentSheet={() => setPaymentSheetVisible(true)}
        onConfirmSelfCollect={() => handlePaymentCollection(null)}
        hasDeliveryInfo={deliveryPhotos.length > 0}
        isSelfCollect={order.isSelfCollect}
      />

      {/* Payment Action Sheet */}
      <AtActionSheet
        isOpened={paymentSheetVisible}
        title='选择收款方式'
        onClose={() => setPaymentSheetVisible(false)}
      >
        <AtActionSheetItem onClick={() => handlePaymentCollection('WATER_TICKET')}>
          水票收款
        </AtActionSheetItem>
        <AtActionSheetItem onClick={() => handlePaymentCollection('CASH')}>
          现金收款
        </AtActionSheetItem>
        <AtActionSheetItem onClick={() => handlePaymentCollection('QR_CODE')}>
          二维码收款
        </AtActionSheetItem>
      </AtActionSheet>

    </PageContainer>
  )
}

export default OrderDetailPage
