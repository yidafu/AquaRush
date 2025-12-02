import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtButton, AtCard, AtInputNumber, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/input-number.scss"
import "taro-ui/dist/style/components/toast.scss"
import './index.scss'

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
  const [quantity, setQuantity] = useState(1)
  const [showSpecDialog, setShowSpecDialog] = useState(false)
  const [selectedSpec, setSelectedSpec] = useState<Record<string, string>>({})
  const [currentImageIndex, setCurrentImageIndex] = useState(0)
  const [showToast, setShowToast] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [productId, setProductId] = useState<string>('')

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
        image: '/assets/product-water.jpg',
        images: [
          '/assets/product-water-1.jpg',
          '/assets/product-water-2.jpg',
          '/assets/product-water-3.jpg'
        ],
        description: '农夫山泉天然矿泉水，源自深层地下水源，经过天然过滤，富含多种矿物质和微量元素。水质清冽甘甜，是您日常补水的理想选择。',
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

  const handleQuantityChange = useCallback((value: number) => {
    if (product && value <= product.stock && value > 0) {
      setQuantity(value)
    }
  }, [product])

  const handleImageChange = useCallback((index: number) => {
    setCurrentImageIndex(index)
  }, [])

  const handleAddToCart = useCallback(() => {
    if (!product) return

    if (quantity > product.stock) {
      showToastMessage('库存不足', 'error')
      return
    }

    // TODO: 实际项目中这里应该调用添加到购物车的API
    // await addToCart({
    //   productId: product.id,
    //   quantity: quantity,
    //   specifications: selectedSpec
    // })

    showToastMessage('已添加到购物车', 'success')
  }, [product, quantity, selectedSpec, showToastMessage])

  const handleBuyNow = useCallback(() => {
    if (!product) return

    if (quantity > product.stock) {
      showToastMessage('库存不足', 'error')
      return
    }

    // 跳转到订单确认页面
    const orderData = {
      products: [{
        id: product.id,
        name: product.name,
        price: product.price,
        quantity: quantity,
        image: product.image
      }],
      totalAmount: product.price * quantity
    }

    // 将订单数据缓存到本地存储
    Taro.setStorageSync('pendingOrder', orderData)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [product, quantity, showToastMessage])

  const handleContactService = useCallback(() => {
    // 联系客服
    Taro.makePhoneCall({
      phoneNumber: '400-888-8888'
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
        <View className='main-image'>
          <Image
            src={currentImages[currentImageIndex]}
            mode='aspectFit'
            className='product-image'
          />
        </View>

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
        content={product.description}
        className='detail-card'
      />

      {/* 规格选择 */}
      {product.specifications && product.specifications.length > 0 && (
        <AtCard
          title='选择规格'
          className='spec-card'
        >
          <View className='spec-list'>
            {product.specifications.map((spec, index) => (
              <View
                key={index}
                className={`spec-item ${selectedSpec.spec === spec ? 'active' : ''}`}
                onClick={() => setSelectedSpec({ ...selectedSpec, spec })}
              >
                <Text>{spec}</Text>
              </View>
            ))}
          </View>
        </AtCard>
      )}

      {/* 数量选择和操作按钮 */}
      <View className='action-section'>
        <View className='quantity-selector'>
          <Text className='quantity-label'>数量</Text>
          <AtInputNumber
            value={quantity}
            min={1}
            max={product.stock}
            onChange={handleQuantityChange}
            size='large'
          />
        </View>

        <View className='action-buttons'>
          <View className='button-row'>
            <AtButton
              type='secondary'
              size='normal'
              onClick={handleAddToCart}
              className='cart-button'
            >
              加入购物车
            </AtButton>

            <AtButton
              type='primary'
              size='normal'
              onClick={handleBuyNow}
              className='buy-button'
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

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default ProductDetail