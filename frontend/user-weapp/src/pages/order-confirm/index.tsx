import React, { useState, useCallback, useEffect, useRef } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, {
  useReady,
  useDidShow
} from '@tarojs/taro'
import AddressService from '../../services/AddressService'
import OrderService from '../../services/OrderService'
import ProductService, { ProductPriceInfo } from '../../services/ProductService'
import { Address } from '../../types/address'
import { normalizeCents, OrderPreviewVo } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/checkbox.scss"
import "taro-ui/dist/style/components/icon.scss"
import './index.scss'

// 导入子组件
import AddressDisplay from './components/AddressDisplay'
import ProductList from './components/ProductList'
import TicketRedeemSection from './components/TicketRedeemSection'
import CouponSelector from './components/CouponSelector'
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
  specification?: string
}

interface OrderConfirmProps {}

/**
 * 确认订单页(F-ORD-023)
 * 计价全部由服务端 orderPreview 计算(R-MON-01,促销引擎 R-MKT-02/07/08 全链路):
 * 成交单价/水票抵扣/满减/优惠券(最优默认勾选,可改选/取消)/配送费/实付;
 * 提交订单时回传 preferredCouponId(0=不用券)与 ticketRedeemCount,服务端原子核销。
 */
const OrderConfirm: React.FC<OrderConfirmProps> = () => {
  usePageViewTracking()

  const [orderItems, setOrderItems] = useState<OrderItem[]>([])
  const [selectedAddress, setSelectedAddress] = useState<Address | null>(null)
  const [preview, setPreview] = useState<OrderPreviewVo | null>(null)
  const [previewLoading, setPreviewLoading] = useState<boolean>(false)
  const [previewError, setPreviewError] = useState<string | null>(null)
  const [previewRetryTick, setPreviewRetryTick] = useState<number>(0)
  // 选中的优惠券 ID(null = 不用券;初始由引擎 isBest 自动选中)
  const [selectedCouponId, setSelectedCouponId] = useState<string | null>(null)
  const [ticketRedeemCount, setTicketRedeemCount] = useState<number>(0)
  // 商品价格信息(熟客价划线展示,R-MKT-01)
  const [productPriceInfo, setProductPriceInfo] = useState<ProductPriceInfo | null>(null)
  const [paymentMethod, setPaymentMethod] = useState<string>('wechat')
  const [deliveryTime, setDeliveryTime] = useState<string>('immediate')
  const [remark, setRemark] = useState<string>('')
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [agreeTerms, setAgreeTerms] = useState<boolean[]>([false])

  // 请求序号:丢弃过期计价响应(水票/优惠券连续调整时只采纳最后一次)
  const previewRequestSeq = useRef<number>(0)
  // 最优券自动勾选只执行一次
  const autoSelectedCouponRef = useRef<boolean>(false)

  const addressService = AddressService.getInstance()

  const firstItem = orderItems.length > 0 ? orderItems[0] : null
  const productId = firstItem?.id ?? ''
  const quantity = firstItem?.quantity ?? 0
  const fallbackUnitPrice = normalizeCents(firstItem?.price) ?? 0

  // 加载订单数据(本地待支付订单,单个商品)
  const loadOrderData = useCallback((): void => {
    try {
      // 从本地存储获取订单数据
      const pendingOrder = Taro.getStorageSync('pendingOrder')

      if (pendingOrder && pendingOrder.products && pendingOrder.products.length > 0) {
        setOrderItems(pendingOrder.products)
      } else {
        // 如果没有订单数据，跳转到首页
        Taro.showToast({ title: '订单数据异常，请重新选择商品', icon: 'none' })
        setTimeout(() => {
          Taro.switchTab({
            url: '/pages/home/index'
          })
        }, 1500)
      }
    } catch (error) {
      console.error('加载订单数据失败:', error)
      Taro.showToast({ title: '加载订单数据失败', icon: 'none' })
    }
  }, [])

  // 加载地址列表并选中默认地址(地址变更后计价随 selectedAddress 变化自动刷新)
  const loadAddresses = useCallback(async (): Promise<void> => {
    try {
      // 首先检查是否有从地址列表页面选中的地址
      const selectedAddr = Taro.getStorageSync('selectedAddress') as Address | null
      if (selectedAddr) {
        setSelectedAddress(selectedAddr)
        // 清除缓存的选中地址
        Taro.removeStorageSync('selectedAddress')
      }

      // 获取用户地址列表
      const result = await addressService.getUserAddresses()

      if (result.success && result.data) {
        const addressList = result.data

        // 如果没有选中地址，选择默认地址
        if (!selectedAddress && !selectedAddr) {
          const defaultAddress = addressList.find(addr => addr.isDefault) || addressList[0]
          if (defaultAddress) {
            // PrimaryId 传输层为字符串,本地类型沿用 number 与既有页面保持一致
            setSelectedAddress(defaultAddress as unknown as Address)
          }
        }
      } else {
        console.error('获取地址列表失败:', result.error)
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
    }
  }, [addressService, selectedAddress])

  // Load initial data
  useReady(() => {
    loadOrderData()
    loadAddresses()
  })

  // Refresh addresses when page is shown(用户从地址列表选择后返回)
  useDidShow(() => {
    loadAddresses()
  })

  // 服务端计价预览:商品/地址/优惠券/水票任一变化即重新计价
  useEffect(() => {
    if (!productId || !selectedAddress || quantity <= 0) return

    const seq = ++previewRequestSeq.current
    setPreviewLoading(true)
    setPreviewError(null)

    const run = async (): Promise<void> => {
      try {
        const result = await OrderService.getInstance().getOrderPreview({
          productId,
          quantity,
          addressId: selectedAddress.id,
          // null = 不用券(0);引擎最优券自动勾选后传入券 ID
          preferredCouponId: selectedCouponId ?? 0,
          ticketRedeemCount
        })

        if (seq !== previewRequestSeq.current) return

        if (!result.success || !result.data) {
          const message = result.error?.message || '计价失败，请重试'
          setPreviewError(message)
          Taro.showToast({ title: message, icon: 'none' })
          return
        }

        setPreview(result.data)

        // 引擎最优券自动勾选(仅首次,用户改选后不再干预)
        if (!autoSelectedCouponRef.current && selectedCouponId === null) {
          const best = (result.data.couponOptions ?? []).find(option => option.isBest)
          if (best) {
            autoSelectedCouponRef.current = true
            setSelectedCouponId(best.couponId)
          }
        }
      } catch (error) {
        console.error('获取订单计价失败:', error)
        if (seq !== previewRequestSeq.current) return
        setPreviewError('计价失败，请检查网络后重试')
        Taro.showToast({ title: '计价失败，请检查网络后重试', icon: 'none' })
      } finally {
        if (seq === previewRequestSeq.current) {
          setPreviewLoading(false)
        }
      }
    }

    run()
  }, [productId, quantity, selectedAddress, selectedCouponId, ticketRedeemCount, previewRetryTick])

  // 商品价格信息(熟客价/目录价,划线展示用)
  useEffect(() => {
    if (!productId) return

    let cancelled = false
    ProductService.getInstance().getProductPrice(productId)
      .then(result => {
        if (!cancelled && result.success && result.data) {
          setProductPriceInfo(result.data)
        }
      })
      .catch(error => {
        console.error('获取商品价格信息失败:', error)
      })

    return () => {
      cancelled = true
    }
  }, [productId])

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

  const handleRemarkChange = (value: string): void => {
    setRemark(value)
  }

  const handleTermsChange = (value: boolean[]): void => {
    setAgreeTerms(value)
  }

  const handleCouponSelect = useCallback((couponId: string | null): void => {
    setSelectedCouponId(couponId)
  }, [])

  const handleTicketRedeemChange = useCallback((value: number): void => {
    setTicketRedeemCount(value)
  }, [])

  const handleRetryPreview = useCallback((): void => {
    setPreviewRetryTick(tick => tick + 1)
  }, [])

  const handleSubmitOrder = useCallback(async (): Promise<void> => {
    if (!selectedAddress) {
      Taro.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }

    if (!firstItem) {
      Taro.showToast({ title: '订单中没有商品', icon: 'none' })
      return
    }

    if (!agreeTerms[0]) {
      Taro.showToast({ title: '请同意服务条款', icon: 'none' })
      return
    }

    if (!preview || previewError || previewLoading) {
      Taro.showToast({ title: '计价失败，请刷新后重试', icon: 'none' })
      return
    }

    setSubmitting(true)

    try {
      const result = await OrderService.getInstance().createOrder({
        productId,
        addressId: selectedAddress.id,
        quantity,
        // 0 = 用户选择不用券/无券可选;否则回传选中的券 ID
        preferredCouponId: selectedCouponId ?? 0,
        ticketRedeemCount,
        remark: remark || undefined
      })

      if (!result.success || !result.data) {
        throw new Error(result.error?.message || '创建订单失败')
      }

      const createdOrder = result.data

      console.log('Order created successfully:', createdOrder)

      // 清除缓存的订单数据
      Taro.removeStorageSync('pendingOrder')

      // 发起支付(F-ORD-005):测试环境假支付自动完成(autoCompleted),
      // 真实环境拉起微信支付;失败/取消均保留订单(待支付)可再次发起
      await doPay(createdOrder.id)

      Taro.showToast({ title: '订单创建成功', icon: 'success' })

      // 跳转到订单详情页面
      setTimeout(() => {
        Taro.redirectTo({
          url: `/pages/order-detail/index?orderId=${createdOrder.id}`
        })
      }, 1500)
    } catch (error) {
      console.error('创建订单失败:', error)
      const message = error instanceof Error ? error.message : '创建订单失败，请重试'
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }, [
    selectedAddress,
    firstItem,
    agreeTerms,
    preview,
    previewError,
    previewLoading,
    productId,
    quantity,
    selectedCouponId,
    ticketRedeemCount,
    remark
  ])

  /**
   * 发起订单支付(F-ORD-005):
   * - autoCompleted=true:假支付已自动完成(测试环境),无需调起微信支付;
   * - requiresSimulation=true:支付未接入,提示后返回;
   * - 否则调起 Taro.requestPayment,用户取消/失败保留「待支付」可再次发起。
   */
  const doPay = useCallback(async (orderId: string): Promise<void> => {
    const payResult = await OrderService.getInstance().createOrderPayment(orderId)
    if (!payResult.success || !payResult.data) {
      Taro.showToast({ title: payResult.error?.message || '支付发起失败', icon: 'none' })
      return
    }
    const { autoCompleted, requiresSimulation, paymentParams } = payResult.data
    if (autoCompleted) {
      Taro.showToast({ title: '支付成功（模拟支付自动完成）', icon: 'success' })
      return
    }
    if (requiresSimulation || !paymentParams) {
      Taro.showToast({ title: '支付功能待接入', icon: 'none' })
      return
    }
    try {
      await Taro.requestPayment({
        timeStamp: paymentParams.timeStamp as string,
        nonceStr: paymentParams.nonceStr as string,
        package: paymentParams.package as string,
        signType: paymentParams.signType as 'MD5' | 'HMAC-SHA256' | 'RSA',
        paySign: paymentParams.paySign as string
      })
      Taro.showToast({ title: '支付成功', icon: 'success' })
    } catch (e) {
      console.error('微信支付未完成:', e)
      Taro.showToast({ title: '支付未完成，可在订单详情继续支付', icon: 'none' })
    }
  }, [])

  // 计价展示数据(服务端为准,未就绪时回退本地兜底价)
  const unitPriceCents = preview ? normalizeCents(preview.unitPriceCents) ?? fallbackUnitPrice : fallbackUnitPrice
  const finalAmount = preview ? normalizeCents(preview.payAmountCents) ?? 0 : fallbackUnitPrice * quantity

  return (
    <View className='order-confirm-page'>
      {/* 收货地址 */}
      <AddressDisplay
        selectedAddress={selectedAddress}
        onAddressSelect={handleAddressSelect}
      />

      {/* 商品列表(单价以服务端计价为准) */}
      <ProductList
        name={firstItem?.name ?? ''}
        image={firstItem?.image ?? ''}
        specification={firstItem?.specification}
        quantity={quantity}
        unitPriceCents={unitPriceCents}
        catalogPriceCents={productPriceInfo?.catalogPriceCents}
        hasGroupPrice={productPriceInfo?.hasGroupPrice}
        priceSource={preview?.priceSource}
      />

      {/* 计价状态提示 */}
      {previewError ? (
        <View className='preview-status preview-error' onClick={handleRetryPreview}>
          <Text className='preview-status-text'>计价失败：{previewError}，点击重试</Text>
        </View>
      ) : !preview ? (
        <View className='preview-status'>
          <Text className='preview-status-text'>价格计算中...</Text>
        </View>
      ) : (
        <View className='preview-status preview-loading'>
          <Text className='preview-status-text'>{previewLoading ? '价格更新中...' : ''}</Text>
        </View>
      )}

      {/* 配送时间 */}
      <DeliveryTimeSelector
        selectedTime={deliveryTime}
        onTimeChange={handleDeliveryTimeChange}
      />

      {/* 水票抵扣(无可用水票时隐藏) */}
      {preview && preview.availableTicketCount > 0 && (
        <TicketRedeemSection
          availableTicketCount={preview.availableTicketCount}
          quantity={quantity}
          value={ticketRedeemCount}
          onChange={handleTicketRedeemChange}
        />
      )}

      {/* 优惠券选择(最优默认勾选,可改选/取消) */}
      {preview && (
        <CouponSelector
          coupons={preview.availableCoupons ?? []}
          options={preview.couponOptions ?? []}
          selectedCouponId={selectedCouponId}
          onSelect={handleCouponSelect}
        />
      )}

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

      {/* 费用明细(服务端计价) */}
      {preview ? (
        <CostSummary
          subtotalCents={normalizeCents(preview.subtotalCents) ?? 0}
          ticketCoveredQuantity={preview.ticketCoveredQuantity}
          promotionDiscountCents={normalizeCents(preview.promotionDiscountCents) ?? 0}
          promotionActivityName={preview.promotionActivityName}
          firstOrderDiscountCents={normalizeCents(preview.firstOrderDiscountCents) ?? 0}
          firstOrderActivityName={preview.firstOrderActivityName}
          couponDiscountCents={normalizeCents(preview.couponDiscountCents) ?? 0}
          selectedCouponName={preview.selectedCouponName}
          giftItems={preview.giftItems}
          deliveryFeeCents={normalizeCents(preview.deliveryFeeCents) ?? 0}
          payAmountCents={normalizeCents(preview.payAmountCents) ?? 0}
        />
      ) : (
        !previewError && (
          <View className='cost-section'>
            <View className='cost-item'>
              <Text className='cost-label'>费用明细</Text>
              <Text className='cost-value'>计算中...</Text>
            </View>
          </View>
        )
      )}

      {/* 服务条款 */}
      <TermsAgreement
        agreeTerms={agreeTerms}
        onTermsChange={handleTermsChange}
      />

      {/* 底部操作栏(计价失败时禁用提交) */}
      <BottomActions
        totalAmount={finalAmount}
        loading={submitting}
        disabled={!preview || !!previewError || previewLoading}
        onSubmit={handleSubmitOrder}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderConfirm