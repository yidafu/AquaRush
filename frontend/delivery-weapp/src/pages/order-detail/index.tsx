import React, { useState, useEffect } from 'react'
import { View, Text, Image, ScrollView } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { AtButton, AtActionSheet, AtActionSheetItem } from 'taro-ui'
import 'taro-ui/dist/style/components/button.scss'
import 'taro-ui/dist/style/components/action-sheet.scss'
import 'taro-ui/dist/style/components/icon.scss'
import './index.scss'
import { getOrderDetail, getOrderOperations, acceptDelivery, startDelivery, completeDelivery } from '../../services/delivery'
import { formatDateTime, OrderStatus } from '@aquarush/common'
import { PageContainer } from '../../components/PageContainer'
import { AddressCard } from './components/AddressCard'
import { ProductCard } from './components/ProductCard'
import { OrderInfoCard } from './components/OrderInfoCard'
import { DeliveryInfoCard } from './components/DeliveryInfoCard'

// Types
interface OrderAddress {
  id: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

interface OrderProduct {
  id: string
  name: string
  price: number
  image?: string
}

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
  hasDeliveryInfo?: boolean
}

const BottomButtons: React.FC<BottomButtonsProps> = ({
  status,
  loading,
  onAccept,
  onStartDelivery,
  onOpenPaymentSheet,
  hasDeliveryInfo = false,
}) => {
  return (
    <View className='bottom-buttons'>
      {status === OrderStatus.PENDING_DISPATCH  && (
        <AtButton
          type='primary'
          loading={loading}
          onClick={onAccept}
          className='action-button'
        >
          接单（拍照确认）
        </AtButton>
      )}

      {status === OrderStatus.PENDING_DELIVERY && (
        <AtButton
          type='primary'
          loading={loading}
          onClick={onStartDelivery}
          className='action-button'
        >
          开始配送（拍照确认）
        </AtButton>
      )}

      {status === OrderStatus.DELIVERING && (
        <View className='delivery-buttons'>
          <AtButton
            type={hasDeliveryInfo ? 'primary' : 'secondary'}
            loading={loading}
            onClick={onOpenPaymentSheet}
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
    loadOrderDetail()
  }, [orderNo])

  // Take photo
  const takePhoto = async (): Promise<string | null> => {
    try {
      const result = await Taro.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['camera'],
      })

      if (result?.tempFiles?.[0]?.tempFilePath) {
        const uploadResult = await Taro.uploadFile({
          url: `/upload`,
          filePath: result.tempFiles[0].tempFilePath,
          name: 'file',
        })

        if (uploadResult.statusCode === 200) {
          const data = JSON.parse(uploadResult.data)
          return data.url || result.tempFiles[0].tempFilePath
        }
      }
      return result.tempFiles?.[0]?.tempFilePath || null
    } catch (error) {
      console.error('拍照失败:', error)
      Taro.showToast({ title: '拍照失败', icon: 'none' })
      return null
    }
  }

  // Handle accept order
  const handleAcceptOrder = async () => {
    const photo = await takePhoto()
    if (!photo) return

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
    const photo = await takePhoto()
    if (!photo) return

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
        {/* Order Status */}
        <View className='status-banner'>
          <Text className='status-text'>{ORDER_STATUS_MAP[order.status] || order.status}</Text>
        </View>

        {/* Address */}
        <AddressCard address={order.address} />

        {/* Product */}
        <ProductCard
          product={order.product}
          quantity={order.quantity}
          remark={order.remark}
        />

        {/* Delivery Info Card - Only show in DELIVERING status */}
        {order.status === 'DELIVERING' && (
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
        hasDeliveryInfo={deliveryPhotos.length > 0}
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
