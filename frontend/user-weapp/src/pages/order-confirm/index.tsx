import { Component } from 'react'
import { View, Text, Image, Radio, RadioGroup, Textarea } from '@tarojs/components'
import { AtButton, AtCard, AtToast, AtIcon, AtCheckbox } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/checkbox.scss"
import './index.scss'
import { displayCents, calculateAndFormatTotal } from '@/utils/money'

interface OrderItem {
  id: string
  name: string
  price: number
  quantity: number
  image: string
  specifications?: string[]
}

interface Address {
  id: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
}

interface PaymentMethod {
  id: string
  name: string
  icon: string
  description: string
}

interface OrderConfirmState {
  orderItems: OrderItem[]
  totalAmount: number
  selectedAddress: Address | null
  addresses: Address[]
  paymentMethod: string
  deliveryTime: string
  remark: string
  loading: boolean
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  agreeTerms: boolean[]
}

export default class OrderConfirm extends Component<{}, OrderConfirmState> {
  constructor(props) {
    super(props)
    this.state = {
      orderItems: [],
      totalAmount: 0,
      selectedAddress: null,
      addresses: [],
      paymentMethod: 'wechat',
      deliveryTime: 'immediate',
      remark: '',
      loading: false,
      showToast: false,
      toastText: '',
      toastType: 'success',
      agreeTerms: [false]
    }
  }

  componentDidMount() {
    this.loadOrderData()
    this.loadAddresses()
  }

  componentDidShow() {
    // 页面显示时刷新地址列表（从地址选择页面返回时）
    this.loadAddresses()
  }

  loadOrderData = () => {
    try {
      // 从本地存储获取订单数据
      const pendingOrder = Taro.getStorageSync('pendingOrder')

      if (pendingOrder && pendingOrder.products) {
        this.setState({
          orderItems: pendingOrder.products,
          totalAmount: pendingOrder.totalAmount || 0
        })
      } else {
        // 如果没有订单数据，跳转到首页
        this.showToast('订单数据异常，请重新选择商品', 'error')
        setTimeout(() => {
          Taro.switchTab({
            url: '/pages/home/index'
          })
        }, 1500)
      }
    } catch (error) {
      console.error('加载订单数据失败:', error)
      this.showToast('加载订单数据失败', 'error')
    }
  }

  loadAddresses = async () => {
    try {
      // TODO: 实际项目中这里应该调用获取地址列表的API
      // const addresses = await getUserAddresses()

      // 模拟地址数据
      const mockAddresses: Address[] = [
        {
          id: '1',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号',
          isDefault: true
        },
        {
          id: '2',
          province: '广东省',
          city: '深圳市',
          district: '福田区',
          detailAddress: '华强北电子世界1栋',
          isDefault: false
        }
      ]

      this.setState({ addresses: mockAddresses })

      // 如果已选择的地址不在列表中，重新选择默认地址
      const { selectedAddress } = this.state
      if (!selectedAddress || !mockAddresses.find(addr => addr.id === selectedAddress.id)) {
        const defaultAddress = mockAddresses.find(addr => addr.isDefault) || mockAddresses[0]
        if (defaultAddress) {
          this.setState({ selectedAddress: defaultAddress })
        }
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
    }
  }

  showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    this.setState({
      showToast: true,
      toastText: text,
      toastType: type
    })
  }

  hideToast = () => {
    this.setState({ showToast: false })
  }

  handleAddressSelect = () => {
    Taro.navigateTo({
      url: '/pages/address-list/index?select=true'
    })
  }

  handleAddressChange = (address: Address) => {
    this.setState({ selectedAddress: address })
  }

  handlePaymentMethodChange = (e: any) => {
    this.setState({ paymentMethod: e.detail.value })
  }

  handleDeliveryTimeChange = (e: any) => {
    this.setState({ deliveryTime: e.detail.value })
  }

  handleRemarkChange = (e: any) => {
    this.setState({ remark: e.detail.value })
  }

  handleTermsChange = (value: boolean[]) => {
    this.setState({ agreeTerms: value })
  }

  calculateTotalAmount = () => {
    const { orderItems } = this.state
    return orderItems.reduce((total, item) => total + (item.price * item.quantity), 0)
  }

  calculateDeliveryFee = () => {
    // 配送费计算逻辑
    const totalAmount = this.calculateTotalAmount()
    return totalAmount >= 30 ? 0 : 5 // 满30元免配送费
  }

  handleSubmitOrder = async () => {
    const {
      selectedAddress,
      paymentMethod,
      deliveryTime,
      remark,
      agreeTerms,
      orderItems
    } = this.state

    if (!selectedAddress) {
      this.showToast('请选择收货地址', 'error')
      return
    }

    if (orderItems.length === 0) {
      this.showToast('订单中没有商品', 'error')
      return
    }

    if (!agreeTerms[0]) {
      this.showToast('请同意服务条款', 'error')
      return
    }

    this.setState({ loading: true })

    try {
      const orderData = {
        items: orderItems,
        address: selectedAddress,
        paymentMethod,
        deliveryTime,
        remark,
        totalAmount: this.calculateTotalAmount(),
        deliveryFee: this.calculateDeliveryFee(),
        finalAmount: this.calculateTotalAmount() + this.calculateDeliveryFee(),
        status: 'pending_payment'
      }

      // TODO: 实际项目中这里应该调用创建订单的API
      // const order = await createOrder(orderData)

      // 模拟创建订单
      await new Promise(resolve => setTimeout(resolve, 1000))

      // 清除缓存的订单数据
      Taro.removeStorageSync('pendingOrder')

      this.showToast('订单创建成功', 'success')

      // 跳转到支付页面或订单详情页面
      setTimeout(() => {
        Taro.redirectTo({
          url: '/pages/order-detail/index?orderId=new_order_id'
        })
      }, 1500)
    } catch (error) {
      console.error('创建订单失败:', error)
      this.showToast('创建订单失败，请重试', 'error')
    } finally {
      this.setState({ loading: false })
    }
  }

  renderPaymentMethods = () => {
    const paymentMethods: PaymentMethod[] = [
      {
        id: 'wechat',
        name: '微信支付',
        icon: '/assets/wechat-pay.png',
        description: '推荐使用微信支付'
      },
      {
        id: 'balance',
        name: '余额支付',
        icon: '/assets/balance-pay.png',
        description: '使用账户余额支付'
      }
    ]

    const { paymentMethod } = this.state

    return (
      <RadioGroup onChange={this.handlePaymentMethodChange}>
        {paymentMethods.map(method => (
          <View key={method.id} className='payment-method'>
            <Radio
              value={method.id}
              checked={paymentMethod === method.id}
              color='#667eea'
            />
            <View className='payment-info'>
              <Text className='payment-name'>{method.name}</Text>
              <Text className='payment-desc'>{method.description}</Text>
            </View>
          </View>
        ))}
      </RadioGroup>
    )
  }

  renderDeliveryTimes = () => {
    const deliveryTimes = [
      { id: 'immediate', name: '立即配送（30分钟内）' },
      { id: 'morning', name: '上午配送（9:00-12:00）' },
      { id: 'afternoon', name: '下午配送（14:00-18:00）' },
      { id: 'evening', name: '晚上配送（18:00-21:00）' }
    ]

    const { deliveryTime } = this.state

    return (
      <RadioGroup onChange={this.handleDeliveryTimeChange}>
        {deliveryTimes.map(time => (
          <View key={time.id} className='delivery-time'>
            <Radio
              value={time.id}
              checked={deliveryTime === time.id}
              color='#667eea'
            />
            <Text className='time-name'>{time.name}</Text>
          </View>
        ))}
      </RadioGroup>
    )
  }

  render() {
    const {
      orderItems,
      selectedAddress,
      loading,
      showToast,
      toastText,
      toastType,
      remark,
      agreeTerms
    } = this.state

    const totalAmount = this.calculateTotalAmount()
    const deliveryFee = this.calculateDeliveryFee()
    const finalAmount = totalAmount + deliveryFee

    return (
      <View className='order-confirm-page'>
        {/* 收货地址 */}
        <AtCard
          title='收货地址'
          className='address-card'
        >
          {selectedAddress ? (
            <View className='address-content' onClick={this.handleAddressSelect}>
              <View className='address-header'>
                <Text className='receiver-name'>{selectedAddress.receiverName}</Text>
                <Text className='receiver-phone'>{selectedAddress.phone}</Text>
                {selectedAddress.isDefault && (
                  <View className='default-tag'>
                    <Text>默认</Text>
                  </View>
                )}
              </View>
              <Text className='address-detail'>
                {selectedAddress.province} {selectedAddress.city} {selectedAddress.district} {selectedAddress.detailAddress}
              </Text>
              <AtIcon value='chevron-right' size='16' color='#999' />
            </View>
          ) : (
            <View className='address-empty' onClick={this.handleAddressSelect}>
              <Text className='empty-text'>请选择收货地址</Text>
              <AtIcon value='chevron-right' size='16' color='#999' />
            </View>
          )}
        </AtCard>

        {/* 商品列表 */}
        <AtCard
          title='商品信息'
          className='product-card'
        >
          <View className='product-list'>
            {orderItems.map((item, index) => (
              <View key={`${item.id}-${index}`} className='product-item'>
                <Image
                  src={item.image}
                  mode='aspectFill'
                  className='product-image'
                />
                <View className='product-info'>
                  <Text className='product-name'>{item.name}</Text>
                  {item.specifications && item.specifications.length > 0 && (
                    <Text className='product-spec'>
                      规格：{item.specifications.join(', ')}
                    </Text>
                  )}
                  <View className='product-bottom'>
                    <Text className='product-price'>{displayCents(item.price)}</Text>
                    <Text className='product-quantity'>x{item.quantity}</Text>
                  </View>
                </View>
              </View>
            ))}
          </View>
        </AtCard>

        {/* 配送时间 */}
        <AtCard
          title='配送时间'
          className='delivery-card'
        >
          {this.renderDeliveryTimes()}
        </AtCard>

        {/* 支付方式 */}
        <AtCard
          title='支付方式'
          className='payment-card'
        >
          {this.renderPaymentMethods()}
        </AtCard>

        {/* 订单备注 */}
        <AtCard
          title='订单备注'
          className='remark-card'
        >
          <Textarea
            placeholder='请输入订单备注（选填）'
            value={remark}
            onInput={this.handleRemarkChange}
            maxlength={200}
            className='remark-input'
          />
          <Text className='remark-count'>{remark.length}/200</Text>
        </AtCard>

        {/* 费用明细 */}
        <View className='cost-section'>
          <View className='cost-item'>
            <Text className='cost-label'>商品总价</Text>
            <Text className='cost-value'>¥{totalAmount.toFixed(2)}</Text>
          </View>
          <View className='cost-item'>
            <Text className='cost-label'>配送费</Text>
            <Text className='cost-value'>
              {deliveryFee === 0 ? '免配送费' : `¥${deliveryFee.toFixed(2)}`}
            </Text>
          </View>
          <View className='cost-item total'>
            <Text className='cost-label'>实付金额</Text>
            <Text className='cost-value total-amount'>¥{finalAmount.toFixed(2)}</Text>
          </View>
        </View>

        {/* 服务条款 */}
        <View className='terms-section'>
          <AtCheckbox
            options={[{
              value: 'agree',
              label: '我已阅读并同意《用户服务协议》和《隐私政策》'
            }]}
            selectedList={agreeTerms}
            onChange={this.handleTermsChange}
          />
        </View>

        {/* 底部操作栏 */}
        <View className='bottom-actions'>
          <View className='total-info'>
            <Text className='total-label'>合计：</Text>
            <Text className='total-amount'>¥{finalAmount.toFixed(2)}</Text>
          </View>
          <AtButton
            type='primary'
            size='normal'
            loading={loading}
            disabled={loading}
            onClick={this.handleSubmitOrder}
            className='submit-button'
          >
            {loading ? '提交中...' : '提交订单'}
          </AtButton>
        </View>

        {/* Toast 提示 */}
        <AtToast
          isOpened={showToast}
          text={toastText}
          status={toastType}
          onClose={this.hideToast}
        />

        {/* 底部安全区域 */}
        <View className='safe-bottom' />
      </View>
    )
  }
}
