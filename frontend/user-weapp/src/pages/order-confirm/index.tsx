import React, { useState } from 'react'
import { View } from '@tarojs/components'
import { AtToast } from 'taro-ui'
import Taro, {
  useReady,
  useDidShow
} from '@tarojs/taro'
import AddressService from '../../services/AddressService'
import { networkManager } from '../../utils/networkManager'
import { Address } from '../../types/address'

import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/checkbox.scss"
import "taro-ui/dist/style/components/icon.scss"
import './index.scss'

// 导入子组件
import AddressDisplay from './components/AddressDisplay'
import ProductList from './components/ProductList'
import DeliveryTimeSelector from './components/DeliveryTimeSelector'
import PaymentMethodSelector from './components/PaymentMethodSelector'
import OrderRemark from './components/OrderRemark'
import CostSummary from './components/CostSummary'
import TermsAgreement from './components/TermsAgreement'
import BottomActions from './components/BottomActions'

interface OrderItem {
  id: string
  name: string
  price: number
  quantity: number
  image: string
  specifications?: string[]
}

// 使用导入的 Address 类型，这里只需要定义扩展字段
interface AddressDisplay extends Address {
  // 扩展字段如果需要
}


interface OrderConfirmProps {}

const OrderConfirm: React.FC<OrderConfirmProps> = () => {
  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [selectedAddress, setSelectedAddress] = useState<Address | null>(null)
  const [addresses, setAddresses] = useState<Address[]>([])
  const [paymentMethod, setPaymentMethod] = useState<string>('wechat')
  const [deliveryTime, setDeliveryTime] = useState<string>('immediate')
  const [remark, setRemark] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(false)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [agreeTerms, setAgreeTerms] = useState<boolean[]>([false])

  const addressService = AddressService.getInstance()

  // Load initial data
  useReady(() => {
    loadOrderData()
    loadAddresses()
  })

  // Refresh addresses when page is shown
  useDidShow(() => {
    loadAddresses()
  })

  const loadOrderData = (): void => {
    try {
      // 从本地存储获取订单数据
      const pendingOrder = Taro.getStorageSync('pendingOrder')

      if (pendingOrder && pendingOrder.products) {
        setOrderItems(pendingOrder.products)
      } else {
        // 如果没有订单数据，跳转到首页
        displayToast('订单数据异常，请重新选择商品', 'error')
        setTimeout(() => {
          Taro.switchTab({
            url: '/pages/home/index'
          })
        }, 1500)
      }
    } catch (error) {
      console.error('加载订单数据失败:', error)
      displayToast('加载订单数据失败', 'error')
    }
  }

  const loadAddresses = async (): Promise<void> => {
    try {
      // 首先检查是否有从地址列表页面选中的地址
      const selectedAddr = Taro.getStorageSync('selectedAddress')
      if (selectedAddr) {
        setSelectedAddress(selectedAddr)
        // 清除缓存的选中地址
        Taro.removeStorageSync('selectedAddress')
      }

      // 获取用户地址列表
      const result = await addressService.getUserAddresses()

      if (result.success && result.data) {
        const addressList = result.data
        setAddresses(addressList)

        // 如果没有选中地址，选择默认地址
        if (!selectedAddress && !selectedAddr) {
          const defaultAddress = addressList.find(addr => addr.isDefault) || addressList[0]
          if (defaultAddress) {
            setSelectedAddress(defaultAddress)
          }
        }
      } else {
        console.error('获取地址列表失败:', result.error)
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
    }
  }

  const displayToast = (text: string, type: 'success' | 'error' | 'loading' = 'success'): void => {
    setToastText(text)
    setToastType(type)
    setShowToast(true)
  }

  const hideToast = (): void => {
    setShowToast(false)
  }

  const handleAddressSelect = (): void => {
    Taro.navigateTo({
      url: '/pages/address-list/index?select=true'
    })
  }

  const handlePaymentMethodChange = (method: string): void => {
    setPaymentMethod(method)
  }

  const handleDeliveryTimeChange = (time: string): void => {
    setDeliveryTime(time)
  }

  const handleRemarkChange = (remark: string): void => {
    setRemark(remark)
  }

  const handleTermsChange = (value: boolean[]): void => {
    setAgreeTerms(value)
  }

  const calculateTotalAmount = (): number => {
    return orderItems.reduce((total: number, item: OrderItem) => total + (item.price * item.quantity), 0)
  }

  const calculateDeliveryFee = (): number => {
    // 配送费计算逻辑
    const totalAmount = calculateTotalAmount()
    return totalAmount >= 30 ? 0 : 5 // 满30元免配送费
  }

  const handleSubmitOrder = async (): Promise<void> => {
    if (!selectedAddress) {
      displayToast('请选择收货地址', 'error')
      return
    }

    if (orderItems.length === 0) {
      displayToast('订单中没有商品', 'error')
      return
    }

    if (!agreeTerms[0]) {
      displayToast('请同意服务条款', 'error')
      return
    }

    setLoading(true)

    try {
      // 创建订单 GraphQL mutation
      const mutation = `
        mutation CreateOrder($input: CreateOrderInput!) {
          createOrder(input: $input) {
            id
            orderNo
            status
            totalAmountCents
          }
        }
      `

      // 只支持单个商品，使用第一个商品
      const firstItem = orderItems[0]

      const variables = {
        input: {
          productId: parseInt(firstItem.id, 10),
          addressId: selectedAddress.id,
          quantity: firstItem.quantity
        }
      }

      console.log('Creating order with input:', variables)

      const response = await networkManager.mutate<{
        createOrder: {
          id: string
          orderNo: string
          status: string
          totalAmountCents: number
        }
      }>(mutation, variables)

      if (!response?.createOrder) {
        throw new Error('创建订单失败')
      }

      const createdOrder = response.createOrder

      console.log('Order created successfully:', createdOrder)

      // 清除缓存的订单数据
      Taro.removeStorageSync('pendingOrder')

      displayToast('订单创建成功', 'success')

      // 跳转到订单详情页面
      setTimeout(() => {
        Taro.redirectTo({
          url: `/pages/order-detail/index?orderId=${createdOrder.id}`
        })
      }, 1500)
    } catch (error) {
      console.error('创建订单失败:', error)

      let errorMessage = '创建订单失败，请重试'
      if (error && typeof error === 'object' && 'errors' in error) {
        const graphqlErrors = (error as any).errors
        if (Array.isArray(graphqlErrors) && graphqlErrors.length > 0) {
          errorMessage = graphqlErrors[0].message || errorMessage
        }
      }

      displayToast(errorMessage, 'error')
    } finally {
      setLoading(false)
    }
  }


  const totalAmount = calculateTotalAmount()
  const deliveryFee = calculateDeliveryFee()
  const finalAmount = totalAmount + deliveryFee

  return (
    <View className='order-confirm-page'>
      {/* 收货地址 */}
      <AddressDisplay
        selectedAddress={selectedAddress}
        onAddressSelect={handleAddressSelect}
      />

      {/* 商品列表 */}
      <ProductList orderItems={orderItems} />

      {/* 配送时间 */}
      <DeliveryTimeSelector
        selectedTime={deliveryTime}
        onTimeChange={handleDeliveryTimeChange}
      />

      {/* 支付方式 */}
      <PaymentMethodSelector
        selectedMethod={paymentMethod}
        onMethodChange={handlePaymentMethodChange}
      />

      {/* 订单备注 */}
      <OrderRemark
        remark={remark}
        onRemarkChange={handleRemarkChange}
      />

      {/* 费用明细 */}
      <CostSummary
        totalAmount={totalAmount}
        deliveryFee={deliveryFee}
        finalAmount={finalAmount}
      />

      {/* 服务条款 */}
      <TermsAgreement
        agreeTerms={agreeTerms}
        onTermsChange={handleTermsChange}
      />

      {/* 底部操作栏 */}
      <BottomActions
        totalAmount={finalAmount}
        loading={loading}
        onSubmit={handleSubmitOrder}
      />

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderConfirm
