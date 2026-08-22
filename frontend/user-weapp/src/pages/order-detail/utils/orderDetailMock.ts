import type { OrderDetail } from '../components/constants'

/**
 * 模拟订单详情数据(离线/演示场景兜底,与真实数据同构)
 */
export const buildMockOrder = (orderId: string): OrderDetail => ({
  id: orderId,
  orderNo: 'ORD202412020001',
  status: 'delivering',
  items: [
    {
      id: '1',
      name: '农夫山泉 天然矿泉水 550ml',
      image: '/assets/product-water.jpg',
      priceCents: 200,
      quantity: 2,
      specifications: ['24瓶装']
    },
    {
      id: '2',
      name: '怡宝 纯净水 380ml',
      image: '/assets/product-water.jpg',
      priceCents: 150,
      quantity: 3,
      specifications: ['36瓶装']
    }
  ],
  totalAmount: 8700,
  deliveryFee: 0,
  finalAmount: 8700,
  address: {
    receiverName: '张三',
    phone: '13800138000',
    province: '广东省',
    city: '深圳市',
    district: '南山区',
    detailAddress: '科技园南区深南大道9988号'
  },
  paymentMethod: 'wechat',
  deliveryTime: 'immediate',
  createTime: '2024-12-02 10:30:00',
  paymentTime: '2024-12-02 10:35:00',
  acceptTime: '2024-12-02 10:45:00',
  remark: '请送到前台',
  deliveryInfo: {
    deliveryWorkerId: 'DW001',
    deliveryWorkerName: '配送员小李',
    deliveryWorkerPhone: '13600136000',
    deliveryWorkerAvatar: '/assets/delivery-worker.jpg',
    estimatedDeliveryTime: '2024-12-02 11:15:00',
    currentLocation: '科技园北区',
    deliveryDistance: '1.2km'
  },
  timeline: [
    {
      time: '2024-12-02 10:30:00',
      title: '订单创建',
      content: '您的订单已创建',
      icon: 'bookmark',
      color: 'var(--brand-primary)'
    },
    {
      time: '2024-12-02 10:35:00',
      title: '支付成功',
      content: '使用微信支付成功',
      icon: 'credit-card',
      color: 'var(--success)'
    },
    {
      time: '2024-12-02 10:45:00',
      title: '商家接单',
      content: '商家已接单，正在准备商品',
      icon: 'shopping-bag',
      color: 'var(--brand-primary)'
    },
    {
      time: '2024-12-02 10:50:00',
      title: '配送员取货',
      content: '配送员小李已取货，正在配送中',
      icon: 'truck',
      color: 'var(--success)'
    }
  ]
})
