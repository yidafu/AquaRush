import React, { useState, useCallback, useEffect, useRef } from 'react'
import { View } from '@tarojs/components'
import Taro, {
  useReady,
  useDidShow
} from '@tarojs/taro'
import AddressService from '../../services/AddressService'
import OrderService, { CreateOrderRequest, OrderItemInput } from '../../services/OrderService'
import NotificationService from '../../services/NotificationService'
import ConfigService from '../../services/ConfigService'
import { Address } from '../../types/address'
import {
  DeliverySlotVo,
  DeliveryTimeSlot,
  normalizeCents,
  OrderPreviewVo,
  maxTicketRedeem
} from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { removeSelectedCartItems, syncCartTabBadge } from '../../utils/cart'
import {
  DEFAULT_DELIVERY_SLOTS,
  enabledDeliverySlots,
  resolveSelectedSlot
} from '../../utils/deliverySlots'

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
  specification?: string
}

/**
 * 确认订单页(F-ORD-004 多商品下单 / F-ORD-023 计价预览)
 * 商品明细(items)为唯一下单口径:单商品「立即购买」=单元素,购物车结算=多元素;
 * 计价全部由服务端 orderPreview 计算(R-MON-01,促销引擎 R-MKT-02/07/08 全链路):
 * 逐商品成交单价/水票抵扣(优先:有可用票默认核销最多张数,可改选/取消)/满减/优惠券(最优默认勾选,可改选/取消)/配送费/实付;
 * 提交订单时回传 items 与 preferredCouponId(0=不用券)/ticketRedeemCount,服务端原子核销。
 */
const OrderConfirm: React.FC = () => {
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
  const [paymentMethod, setPaymentMethod] = useState<string>('wechat')
  // 配送时段(F-SYS-002):默认立即配送;选项由后台「订单与履约参数」配置驱动(请求失败/离线回退兜底)
  const [deliveryTime, setDeliveryTime] = useState<string>(DeliveryTimeSlot.IMMEDIATE)
  // 全量配送时段配置(含已停用;渲染时仅取 enabled 项,订单详情需全量映射)
  const [deliverySlots, setDeliverySlots] = useState<DeliverySlotVo[]>(DEFAULT_DELIVERY_SLOTS)
  const [remark, setRemark] = useState<string>('')
  const [submitting, setSubmitting] = useState<boolean>(false)
  // 服务条款勾选:AtCheckbox 的 selectedList 为已选 value 数组(string[]),
  // 初始为空数组(未勾选),勾选后为 ['agree'] —— 勿用 boolean 数组,否则
  // onChange 回传的 Set 首位残留 false,校验永远判定未勾选
  const [agreeTerms, setAgreeTerms] = useState<string[]>([])

  // 请求序号:丢弃过期计价响应(水票/优惠券连续调整时只采纳最后一次)
  const previewRequestSeq = useRef<number>(0)
  // 最优券自动勾选只执行一次
  const autoSelectedCouponRef = useRef<boolean>(false)
  // 水票优先抵扣自动核销只执行一次(默认按最多可用张数核销,用户改选/取消后不再干预 —— R-MKT-08)
  const autoSelectedTicketsRef = useRef<boolean>(false)
  // 服务端下单幂等键(R-ORD-06):进入确认页生成一次,提交失败重试沿用同一值,
  // 服务端按 (userId, clientRequestId) 去重,重复提交不产生重复订单
  const clientRequestIdRef = useRef<string>(`${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`)

  const addressService = AddressService.getInstance()

  // 多商品明细(F-ORD-004):items 为唯一下单口径
  const items: OrderItemInput[] = orderItems.map(item => ({
    productId: item.id,
    quantity: item.quantity
  }))
  const totalQuantity = orderItems.reduce((sum, item) => sum + item.quantity, 0)

  // 加载订单数据(本地 pendingOrder:立即购买单元素 / 购物车结算多元素)
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

  // 加载配送时段配置(F-SYS-002):后台「订单与履约参数」配置,热生效;
  // 与地址加载并行发出(非串行,单次轻量查询);失败/离线时保持兜底默认,不阻塞下单流程
  const loadDeliverySlots = useCallback(async (): Promise<void> => {
    try {
      const result = await ConfigService.getDeliverySlots()
      if (result.success && result.data && result.data.length > 0) {
        setDeliverySlots(result.data)
      }
    } catch (error) {
      console.error('获取配送时段配置失败:', error)
    }
  }, [])

  // Load initial data
  useReady(() => {
    loadOrderData()
    loadAddresses()
    loadDeliverySlots()
  })

  // 配送时段配置生效后校准选中态:后台停用当前时段时回退第一个启用时段(AC-SYS-002-03)
  useEffect(() => {
    setDeliveryTime(current => resolveSelectedSlot(deliverySlots, current))
  }, [deliverySlots])

  // Refresh addresses when page is shown(用户从地址列表选择后返回)
  useDidShow(() => {
    loadAddresses()
  })

  // 服务端计价预览:商品明细/地址/优惠券/水票任一变化即重新计价
  useEffect(() => {
    if (items.length === 0 || !selectedAddress) return

    const seq = ++previewRequestSeq.current
    setPreviewLoading(true)
    setPreviewError(null)

    const run = async (): Promise<void> => {
      try {
        const result = await OrderService.getInstance().getOrderPreview({
          items,
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

        // 水票优先抵扣(仅首次,用户改选/取消后不再干预):
        // 票包有可用水票时默认核销最多可用张数 min(总桶数, 可用张数)(R-MKT-08 一票抵一桶),
        // 自动核销后触发重计价,实付=抵扣后应付;用户仍可步进改选/取消(可选支付)。
        if (!autoSelectedTicketsRef.current && (result.data.availableTicketCount ?? 0) > 0) {
          const maxRedeem = maxTicketRedeem(totalQuantity, result.data.availableTicketCount ?? 0)
          if (maxRedeem > 0) {
            autoSelectedTicketsRef.current = true
            setTicketRedeemCount(maxRedeem)
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(items), selectedAddress, selectedCouponId, ticketRedeemCount, previewRetryTick])

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

  const handleTermsChange = (value: string[]): void => {
    setAgreeTerms(value)
  }

  const handleCouponSelect = useCallback((couponId: string | null): void => {
    setSelectedCouponId(couponId)
  }, [])

  const handleTicketRedeemChange = useCallback((value: number): void => {
    // 用户手动改选/取消后,水票优先抵扣不再自动干预(可选支付)
    autoSelectedTicketsRef.current = true
    setTicketRedeemCount(value)
  }, [])

  const handleRetryPreview = useCallback((): void => {
    setPreviewRetryTick(tick => tick + 1)
  }, [])

  /**
   * 提交订单时申请微信订阅消息授权(R-NTF-01,F-NTF-001):
   * 仅记录用户接受的模板(一次性模板授权一次可发一次);用户拒绝/接口异常不阻塞下单,
   * 关键节点降级为站内消息中心查看。
   */
  const requestSubscribeAuth = useCallback(async (): Promise<void> => {
    try {
      const templates = await NotificationService.getSubscribeTemplates()
      if (templates.length === 0) {
        return
      }
      const result = await Taro.requestSubscribeMessage({
        tmplIds: templates.map(t => t.templateId)
      })
      const accepted = templates
        .filter(t => (result as Record<string, string>)[t.templateId] === 'accept')
        .map(t => t.templateId)
      if (accepted.length > 0) {
        await NotificationService.recordSubscribeAuth(accepted)
      }
    } catch (e) {
      // 已拒绝/达上限/接口异常:不打扰、不阻塞下单,降级站内查看
      console.warn('订阅消息授权跳过(降级站内):', e)
    }
  }, [])

  const handleSubmitOrder = useCallback(async (): Promise<void> => {
    if (!selectedAddress) {
      Taro.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }

    if (orderItems.length === 0) {
      Taro.showToast({ title: '订单中没有商品', icon: 'none' })
      return
    }

    if (!agreeTerms.includes('agree')) {
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
        items,
        addressId: selectedAddress.id,
        // 幂等键(R-ORD-06):同会话重复提交返回同一订单
        clientRequestId: clientRequestIdRef.current,
        // 配送时段(AC-ORD-001-05):id 与后端 DeliveryTimeSlot 枚举对应,大写随单快照落库
        deliveryTime: deliveryTime.toUpperCase() as CreateOrderRequest['deliveryTime'],
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

      // 提交订单时申请订阅消息授权(F-NTF-001,失败不影响下单流程)
      await requestSubscribeAuth()

      // 清除缓存的订单数据
      Taro.removeStorageSync('pendingOrder')

      // 购物车结算来源:移除已下单的勾选商品(F-PRO-004)
      const fromCart = Taro.getStorageSync('cartCheckout')
      if (fromCart) {
        removeSelectedCartItems()
        Taro.removeStorageSync('cartCheckout')
        // 下单后同步购物车 tabBar 角标
        syncCartTabBadge()
      }

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
    orderItems,
    agreeTerms,
    preview,
    previewError,
    previewLoading,
    items,
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
  const finalAmount = preview ? normalizeCents(preview.payAmountCents) ?? 0 : 0

  // 逐商品计价信息(服务端 preview.items 为准,兜底本地价)
  const previewItemsById = new Map<string, OrderPreviewVo['items'][number]>()
  ;(preview?.items ?? []).forEach(item => {
    previewItemsById.set(String(item.productId), item)
  })

  return (
    <View className='order-confirm-page'>
      {/* 收货地址 */}
      <AddressDisplay
        selectedAddress={selectedAddress}
        onAddressSelect={handleAddressSelect}
      />

      {/* 商品列表(多商品 F-ORD-004,单价以服务端计价为准) */}
      <ProductList
        items={orderItems.map(item => ({
          id: item.id,
          name: item.name,
          image: item.image,
          specification: item.specification,
          quantity: item.quantity,
          unitPriceCents: previewItemsById.get(item.id) ? normalizeCents(previewItemsById.get(item.id)!.unitPriceCents) ?? item.price : item.price,
          catalogPriceCents: previewItemsById.get(item.id) ? normalizeCents(previewItemsById.get(item.id)!.catalogPriceCents) ?? undefined : undefined,
          priceSource: previewItemsById.get(item.id)?.priceSource
        }))}
      />

      {/* 配送时间(选项由后台 F-SYS-002 配置驱动,仅渲染启用时段) */}
      <DeliveryTimeSelector
        slots={enabledDeliverySlots(deliverySlots)}
        selectedTime={deliveryTime}
        onTimeChange={handleDeliveryTimeChange}
      />

      {/* 水票抵扣(无可用水票时隐藏;核销上限=订单总桶数 Σquantity) */}
      {preview && preview.availableTicketCount > 0 && (
        <TicketRedeemSection
          availableTicketCount={preview.availableTicketCount}
          quantity={totalQuantity}
          value={ticketRedeemCount}
          onChange={handleTicketRedeemChange}
        />
      )}

      {/* 优惠券选择(无可用券时区块整体隐藏;最优默认勾选,可改选/取消) */}
      {preview && preview.availableCoupons && preview.availableCoupons.length > 0 && (
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

      {/* 费用明细(服务端计价;失败/未就绪/更新中态内联展示) */}
      <CostSummary
        preview={preview}
        error={previewError}
        loading={previewLoading}
        onRetry={handleRetryPreview}
      />

      {/* 服务条款 */}
      <TermsAgreement
        agreeTerms={agreeTerms}
        onTermsChange={handleTermsChange}
      />

      {/* 底部操作栏(计价失败时禁用提交;展示已优惠金额) */}
      <BottomActions
        totalAmount={finalAmount}
        loading={submitting}
        disabled={!preview || !!previewError || previewLoading}
        discountCents={preview ? preview.totalDiscountCents : 0}
        onSubmit={handleSubmitOrder}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderConfirm