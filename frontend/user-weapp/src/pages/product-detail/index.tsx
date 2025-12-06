import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image, Button, Swiper, SwiperItem } from '@tarojs/components'
import { AtButton, AtCard, AtInputNumber, AtToast, AtModal, AtModalHeader, AtModalContent, AtModalAction } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/input-number.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/modal.scss"
import './index.scss'
import { CONTACT_INFO } from '@/constants'

interface Product {
  id: string
  name: string
  price: number
  originalPrice?: number
  image: string
  images?: string[]
  description: string
  stock: number
  category: string
  specifications?: string[]
  brand?: string
  deliveryTime?: string
}

const ProductDetail: React.FC = () => {
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [currentImageIndex, setCurrentImageIndex] = useState(0)
  const [showToast, setShowToast] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [productId, setProductId] = useState<string>('')
  const [showBuyModal, setShowBuyModal] = useState(false)
  const [buyModalQuantity, setBuyModalQuantity] = useState(1)
  const [buyModalSelectedSpec, setBuyModalSelectedSpec] = useState<string>('')

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  const loadProductDetail = useCallback(async () => {
    try {
      setLoading(true)

      // TODO: 实际项目中这里应该调用获取产品详情的API
      // const product = await getProductDetail(productId)

      // 模拟产品详情数据
      const mockProduct: Product = {
        id: productId,
        name: '农夫山泉 天然矿泉水 550ml',
        price: 2.00,
        originalPrice: 2.50,
        image: 'https://images.unsplash.com/photo-1548839140-29a74921eb34?w=800&h=600&fit=crop',
        images: [
          'https://images.unsplash.com/photo-1548839140-29a74921eb34?w=800&h=600&fit=crop',
          'https://images.unsplash.com/photo-1596484989008-0a4938d2b5c7?w=800&h=600&fit=crop',
          'https://images.unsplash.com/photo-1549496903-b551a9c4d80f?w=800&h=600&fit=crop',
          'https://images.unsplash.com/photo-1580957419298-3176325b1324?w=800&h=600&fit=crop',
          'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=600&fit=crop'
        ],
        description: '农夫山泉天然矿泉水，源自深层地下水源，经过天然过滤，富含多种矿物质和微量元素。水质清冽甘甜，是您日常补水的理想选择。\n\n产品特点：\n• 天然矿物质，有益健康\n• pH值7.3±0.5，天然弱碱性\n• 源自千岛湖深层水源\n• 经过严格质量检测\n• 环保包装，绿色健康',
        stock: 100,
        category: '矿泉水',
        specifications: ['550ml', '24瓶装', '36瓶装'],
        brand: '农夫山泉',
        deliveryTime: '30分钟内送达'
      }

      setProduct(mockProduct)
    } catch (error) {
      console.error('获取产品详情失败:', error)
      showToastMessage('加载失败，请重试', 'error')
    } finally {
      setLoading(false)
    }
  }, [productId, showToastMessage])

  const handleImageChange = useCallback((index: number) => {
    setCurrentImageIndex(index)
  }, [])

  
  const handleBuyNow = useCallback(() => {
    if (!product) return

    // 显示购买弹窗
    setShowBuyModal(true)
    setBuyModalQuantity(1) // 默认数量为1

    // 如果有规格，默认选中第一个
    if (product.specifications && product.specifications.length > 0) {
      setBuyModalSelectedSpec(product.specifications[0])
    }
  }, [product])

  const handleBuyModalConfirm = useCallback(() => {
    if (!product) return

    if (buyModalQuantity > product.stock) {
      showToastMessage('库存不足', 'error')
      return
    }

    // 检查是否需要选择规格
    if (product.specifications && product.specifications.length > 0 && !buyModalSelectedSpec) {
      showToastMessage('请选择产品规格', 'error')
      return
    }

    // 跳转到订单确认页面
    const orderData = {
      products: [{
        id: product.id,
        name: product.name,
        price: product.price,
        quantity: buyModalQuantity,
        image: product.image,
        specification: buyModalSelectedSpec
      }],
      totalAmount: product.price * buyModalQuantity,
      specification: buyModalSelectedSpec
    }

    // 将订单数据缓存到本地存储
    Taro.setStorageSync('pendingOrder', orderData)

    setShowBuyModal(false)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [product, buyModalQuantity, buyModalSelectedSpec, showToastMessage])

  const handleBuyModalCancel = useCallback(() => {
    setShowBuyModal(false)
  }, [])

  const handleBuyModalQuantityChange = useCallback((value: number) => {
    if (product && value <= product.stock && value > 0) {
      setBuyModalQuantity(value)
    }
  }, [product])

  const handleBuyModalSpecSelect = useCallback((spec: string) => {
    setBuyModalSelectedSpec(spec)
  }, [])

  const handleContactService = useCallback(() => {
    // 联系客服
    Taro.makePhoneCall({
      phoneNumber: CONTACT_INFO.COMPLAINT_HOTLINE
    })
  }, [])

  const handleShare = useCallback(() => {
    // 分享功能
    Taro.showShareMenu({
      withShareTicket: true
    })
  }, [])

  const handlePullDownRefresh = useCallback(() => {
    loadProductDetail().finally(() => {
      Taro.stopPullDownRefresh()
    })
  }, [loadProductDetail])

  // 初始化
  useEffect(() => {
    // 获取页面参数
    const instance = Taro.getCurrentInstance()
    const { id } = instance.router?.params || {}

    if (!id) {
      Taro.showToast({
        title: '产品ID缺失',
        icon: 'none'
      })
      Taro.navigateBack()
      return
    }

    setProductId(id)
  }, [])

  // 加载产品详情
  useEffect(() => {
    if (productId) {
      loadProductDetail()
    }
  }, [productId, loadProductDetail])

  // 页面显示时刷新产品信息
  useEffect(() => {
    if (productId && !loading) {
      loadProductDetail()
    }
  }, [productId, loading, loadProductDetail])

  // 注册下拉刷新
  Taro.usePullDownRefresh(handlePullDownRefresh)

  if (loading) {
    return (
      <View className='product-detail-page'>
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!product) {
    return (
      <View className='product-detail-page'>
        <View className='error-container'>
          <Text>产品不存在</Text>
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

  const currentImages = product.images || [product.image]
  const hasDiscount = product.originalPrice && product.originalPrice > product.price

  return (
    <View className='product-detail-page'>
      {/* 产品图片轮播 */}
      <View className='image-section'>
        <Swiper
          className='product-swiper'
          indicatorDots
          autoplay
          interval={3000}
          duration={500}
          indicatorColor='rgba(255, 255, 255, 0.5)'
          indicatorActiveColor='#667eea'
          circular
        >
          {currentImages.map((image, index) => (
            <SwiperItem key={index}>
              <View className='swiper-item'>
                <Image
                  src={image}
                  mode='aspectFit'
                  className='product-image'
                />
              </View>
            </SwiperItem>
          ))}
        </Swiper>

        {/* 缩略图导航 */}
        {currentImages.length > 1 && (
          <View className='image-thumbnails'>
            {currentImages.map((image, index) => (
              <View
                key={index}
                className={`thumbnail ${currentImageIndex === index ? 'active' : ''}`}
                onClick={() => handleImageChange(index)}
              >
                <Image
                  src={image}
                  mode='aspectFill'
                  className='thumbnail-image'
                />
              </View>
            ))}
          </View>
        )}
      </View>

      {/* 产品信息 */}
      <View className='product-info'>
        <View className='product-header'>
          <Text className='product-name'>{product.name}</Text>

          {product.brand && (
            <Text className='product-brand'>{product.brand}</Text>
          )}
        </View>

        <View className='price-section'>
          <View className='price-row'>
            <Text className='current-price'>¥{product.price.toFixed(2)}</Text>
            {hasDiscount && (
              <Text className='original-price'>¥{product.originalPrice!.toFixed(2)}</Text>
            )}
          </View>

          {hasDiscount && (
            <View className='discount-tag'>
              <Text>限时特价</Text>
            </View>
          )}
        </View>

        <View className='product-meta'>
          <View className='meta-item'>
            <Text className='meta-label'>库存</Text>
            <Text className='meta-value'>{product.stock}</Text>
          </View>

          {product.deliveryTime && (
            <View className='meta-item'>
              <Text className='meta-label'>配送</Text>
              <Text className='meta-value'>{product.deliveryTime}</Text>
            </View>
          )}
        </View>
      </View>

      {/* 产品详情 */}
      <AtCard
        title='产品详情'
        className='detail-card'
      >
        <View className='detail-content'>
          {/* 产品图片和文字描述 */}
          <View className='detail-section'>
            <Image
              src='https://images.unsplash.com/photo-1549496903-b551a9c4d80f?w=400&h=300&fit=crop'
              mode='aspectFill'
              className='detail-image'
            />
            <View className='detail-text-section'>
              <Text className='detail-title'>优质水源</Text>
              <Text className='detail-description'>
                农夫山泉天然矿泉水源自千岛湖深层地下水源，经过多层天然过滤，确保水质纯净清冽。
              </Text>
            </View>
          </View>

          <View className='detail-section'>
            <View className='detail-text-section'>
              <Text className='detail-title'>富含矿物质</Text>
              <Text className='detail-description'>
                含有钙、镁、钾、钠等多种天然矿物质和微量元素，有助于维持身体机能平衡。
              </Text>
            </View>
            <Image
              src='https://images.unsplash.com/photo-1596484989008-0a4938d2b5c7?w=400&h=300&fit=crop'
              mode='aspectFill'
              className='detail-image'
            />
          </View>

          <View className='detail-section'>
            <Image
              src='https://images.unsplash.com/photo-1580957419298-3176325b1324?w=400&h=300&fit=crop'
              mode='aspectFill'
              className='detail-image'
            />
            <View className='detail-text-section'>
              <Text className='detail-title'>天然弱碱性</Text>
              <Text className='detail-description'>
                pH值7.3±0.5的天然弱碱性水质，有助于维持体内酸碱平衡，促进新陈代谢。
              </Text>
            </View>
          </View>

          <View className='detail-section'>
            <View className='detail-text-section'>
              <Text className='detail-title'>环保包装</Text>
              <Text className='detail-description'>
                采用可回收环保材料，减少塑料污染，为环境保护贡献一份力量。
              </Text>
            </View>
            <Image
              src='https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&h=300&fit=crop'
              mode='aspectFill'
              className='detail-image'
            />
          </View>

          {/* 产品特点列表 */}
          <View className='detail-features'>
            <Text className='detail-subtitle'>产品特点</Text>
            {product.description.split('\n').map((paragraph, index) => (
              paragraph.startsWith('•') ? (
                <View key={index} className='feature-list-item'>
                  <Text className='feature-bullet'>•</Text>
                  <Text className='feature-text'>{paragraph.slice(1).trim()}</Text>
                </View>
              ) : paragraph.trim() && !paragraph.startsWith('产品特点') ? (
                <Text key={index} className='detail-paragraph'>
                  {paragraph}
                </Text>
              ) : null
            ))}
          </View>
        </View>
      </AtCard>

      {/* 操作按钮 */}
      <View className='action-section'>
        <View className='action-buttons'>
          <View className='button-row'>
            <AtButton
              type='primary'
              size='normal'
              onClick={handleBuyNow}
              className='buy-button full-width'
            >
              立即购买
            </AtButton>
          </View>

          <View className='service-buttons'>
            <AtButton
              size='small'
              type='secondary'
              onClick={handleContactService}
              className='service-button'
            >
              联系客服
            </AtButton>

            <AtButton
              size='small'
              type='secondary'
              onClick={handleShare}
              className='share-button'
            >
              分享
            </AtButton>
          </View>
        </View>
      </View>

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 购买确认弹窗 */}
      <AtModal
        isOpened={showBuyModal}
        onClose={handleBuyModalCancel}
        className='buy-modal'
      >
        <AtModalHeader>确认购买</AtModalHeader>
        <AtModalContent>
          <View className='buy-modal-content'>
            {/* 产品信息 */}
            <View className='modal-product-info'>
              <Image
                src={product?.image}
                mode='aspectFill'
                className='modal-product-image'
              />
              <View className='modal-product-details'>
                <Text className='modal-product-name'>{product?.name}</Text>
                <Text className='modal-product-price'>¥{product?.price.toFixed(2)}</Text>
                <Text className='modal-product-stock'>库存: {product?.stock}</Text>
              </View>
            </View>

            {/* 规格选择 */}
            {product?.specifications && product.specifications.length > 0 && (
              <View className='modal-spec-section'>
                <Text className='modal-section-title'>选择规格</Text>
                <View className='modal-spec-list'>
                  {product.specifications.map((spec, index) => (
                    <View
                      key={index}
                      className={`modal-spec-item ${buyModalSelectedSpec === spec ? 'active' : ''}`}
                      onClick={() => handleBuyModalSpecSelect(spec)}
                    >
                      <Text>{spec}</Text>
                    </View>
                  ))}
                </View>
              </View>
            )}

            {/* 数量选择 */}
            <View className='modal-quantity-section'>
              <Text className='modal-section-title'>购买数量</Text>
              <AtInputNumber
                value={buyModalQuantity}
                min={1}
                max={product?.stock || 1}
                onChange={handleBuyModalQuantityChange}
                size='large'
                className='modal-quantity-input'
                type='number'
              />
            </View>

            {/* 总价 */}
            <View className='modal-total-section'>
              <Text className='modal-total-label'>合计：</Text>
              <Text className='modal-total-price'>
                ¥{product ? (product.price * buyModalQuantity).toFixed(2) : '0.00'}
              </Text>
            </View>
          </View>
        </AtModalContent>
        <AtModalAction>
          <Button onClick={handleBuyModalCancel}>取消</Button>
          <Button type='primary' onClick={handleBuyModalConfirm}>确认购买</Button>
        </AtModalAction>
      </AtModal>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default ProductDetail
