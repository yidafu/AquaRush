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
import { displayCents, calculateAndFormatTotal } from '@/utils/money'
import NetworkManager, { GraphQLError } from '@/utils/network'
import { Product } from '../../types/graphql'

interface ProductDetailState {
  product: Product | null
  loading: boolean
  currentImageIndex: number
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  productId: string
  showBuyModal: boolean
  buyModalQuantity: number
  buyModalSelectedSpec: string
  isPreviewMode: boolean
}

const GET_PRODUCT_QUERY = `
  query GetProduct($id: Long!) {
    product(id: $id) {
      id
      name
      subtitle
      price
      originalPrice
      depositPrice
      coverImageUrl
      imageGallery
      specification
      waterSource
      phValue
      mineralContent
      stock
      salesVolume
      status
      sortOrder
      tags
      detailContent
      certificateImages
      deliverySettings
      isDeleted
      createdAt
      updatedAt
    }
  }
`

const ProductDetail: React.FC = () => {
  const [state, setState] = useState<ProductDetailState>({
    product: null,
    loading: true,
    currentImageIndex: 0,
    showToast: false,
    toastText: '',
    toastType: 'success',
    productId: '',
    showBuyModal: false,
    buyModalQuantity: 1,
    buyModalSelectedSpec: '',
    isPreviewMode: false
  })

  const updateState = useCallback((updates: Partial<ProductDetailState>) => {
    setState(prev => ({ ...prev, ...updates }))
  }, [])

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    updateState({
      showToast: true,
      toastText: text,
      toastType: type
    })
  }, [updateState])

  const hideToast = useCallback(() => {
    updateState({ showToast: false })
  }, [updateState])

  const loadProductDetail = useCallback(async () => {
    if (!state.productId) return

    try {
      updateState({ loading: true })

      const network = NetworkManager.getInstance({
        baseURL: process.env.TARO_APP_GRAPHQL_URL || 'http://localhost:8080/graphql'
      })

      const response = await network.query<{ product: Product }>(
        GET_PRODUCT_QUERY,
        { id: state.productId }
      )

      if (response.product) {
        updateState({ product: response.product })
      } else {
        showToastMessage('产品不存在', 'error')
      }
    } catch (error) {
      console.error('获取产品详情失败:', error)

      if (error instanceof GraphQLError) {
        showToastMessage(`加载失败: ${error.message}`, 'error')
      } else {
        showToastMessage('加载失败，请重试', 'error')
      }

      // 如果是预览模式或API不可用，使用模拟数据
      if (state.isPreviewMode || process.env.NODE_ENV === 'development') {
        const mockProduct: Product = {
          id: state.productId,
          name: '农夫山泉 天然矿泉水 550ml',
          subtitle: '深层天然矿泉水，富含矿物质',
          price: '2.00',
          originalPrice: '2.50',
          depositPrice: undefined,
          coverImageUrl: 'https://images.unsplash.com/photo-1548839140-29a74921eb34?w=800&h=600&fit=crop',
          imageGallery: [
            'https://images.unsplash.com/photo-1548839140-29a74921eb34?w=800&h=600&fit=crop',
            'https://images.unsplash.com/photo-1596484989008-0a4938d2b5c7?w=800&h=600&fit=crop',
            'https://images.unsplash.com/photo-1549496903-b551a9c4d80f?w=800&h=600&fit=crop',
            'https://images.unsplash.com/photo-1580957419298-3176325b1324?w=800&h=600&fit=crop',
            'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=600&fit=crop'
          ],
          specification: '550ml*24瓶',
          waterSource: '千岛湖深层水源',
          phValue: '7.3',
          mineralContent: '钙≥4mg/L，镁≥0.5mg/L，钾≥0.35mg/L，钠≥8mg/L，偏硅酸≥18mg/L',
          stock: 100,
          salesVolume: 1250,
          status: 'ONLINE',
          sortOrder: 1,
          tags: ['天然矿泉水', '弱碱性', '富含矿物质'],
          detailContent: '农夫山泉天然矿泉水，源自深层地下水源，经过天然过滤，富含多种矿物质和微量元素。水质清冽甘甜，是您日常补水的理想选择。\n\n产品特点：\n• 天然矿物质，有益健康\n• pH值7.3±0.5，天然弱碱性\n• 源自千岛湖深层水源\n• 经过严格质量检测\n• 环保包装，绿色健康',
          certificateImages: [],
          deliverySettings: {},
          isDeleted: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }

        updateState({ product: mockProduct })
      }
    } finally {
      updateState({ loading: false })
    }
  }, [state.productId, state.isPreviewMode, showToastMessage, updateState])

  const handleImageChange = useCallback((index: number) => {
    updateState({ currentImageIndex: index })
  }, [updateState])

  const handleBuyNow = useCallback(() => {
    if (!state.product) return

    updateState({
      showBuyModal: true,
      buyModalQuantity: 1,
      buyModalSelectedSpec: state.product.specification
    })
  }, [state.product, updateState])

  const handleBuyModalConfirm = useCallback(() => {
    if (!state.product) return

    if (state.buyModalQuantity > state.product.stock) {
      showToastMessage('库存不足', 'error')
      return
    }

    // 跳转到订单确认页面
    const orderData = {
      products: [{
        id: state.product.id.toString(),
        name: state.product.name,
        price: parseFloat(state.product.price),
        quantity: state.buyModalQuantity,
        image: state.product.coverImageUrl,
        specification: state.buyModalSelectedSpec
      }],
      totalAmount: parseFloat(state.product.price) * state.buyModalQuantity,
      specification: state.buyModalSelectedSpec
    }

    // 将订单数据缓存到本地存储
    Taro.setStorageSync('pendingOrder', orderData)

    updateState({ showBuyModal: false })

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [state.product, state.buyModalQuantity, state.buyModalSelectedSpec, showToastMessage, updateState])

  const handleBuyModalCancel = useCallback(() => {
    updateState({ showBuyModal: false })
  }, [updateState])

  const handleBuyModalQuantityChange = useCallback((value: number) => {
    if (state.product && value <= state.product.stock && value > 0) {
      updateState({ buyModalQuantity: value })
    }
  }, [state.product, updateState])

  const handleContactService = useCallback(() => {
    Taro.makePhoneCall({
      phoneNumber: CONTACT_INFO.COMPLAINT_HOTLINE
    })
  }, [])

  const handleShare = useCallback(() => {
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
    const instance = Taro.getCurrentInstance()
    const { id, preview, from } = instance.router?.params || {}

    const isPreview = preview === 'true' || from === 'admin' ||
                     (typeof window !== 'undefined' && window !== window.parent)

    if (!id) {
      Taro.showToast({
        title: '产品ID缺失',
        icon: 'none'
      })
      Taro.navigateBack()
      return
    }

    updateState({
      productId: id,
      isPreviewMode: isPreview
    })

    if (isPreview) {
      console.log('Product preview mode activated for product:', id)
    }
  }, [updateState])

  // 加载产品详情
  useEffect(() => {
    if (state.productId) {
      loadProductDetail()
    }
  }, [state.productId, loadProductDetail])

  // 注册下拉刷新
  Taro.usePullDownRefresh(handlePullDownRefresh)

  if (state.loading) {
    return (
      <View className='product-detail-page'>
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!state.product) {
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

  const currentImages = state.product.imageGallery && state.product.imageGallery.length > 0
    ? state.product.imageGallery as string[]
    : [state.product.coverImageUrl]

  const hasDiscount = state.product.originalPrice && parseFloat(state.product.originalPrice) > parseFloat(state.product.price)
  const hasDeposit = state.product.depositPrice && parseFloat(state.product.depositPrice) > 0
  const tags = state.product.tags as string[] || []

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
                className={`thumbnail ${state.currentImageIndex === index ? 'active' : ''}`}
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
          <Text className='product-name'>{state.product.name}</Text>

          {state.product.subtitle && (
            <Text className='product-subtitle'>{state.product.subtitle}</Text>
          )}

          {/* 标签 */}
          {tags.length > 0 && (
            <View className='product-tags'>
              {tags.map((tag, index) => (
                <View key={index} className='product-tag'>
                  <Text>{tag}</Text>
                </View>
              ))}
            </View>
          )}
        </View>

        <View className='price-section'>
          <View className='price-row'>
            <Text className='current-price'>{displayCents(parseFloat(state.product.price) * 100)}</Text>
            {hasDiscount && (
              <Text className='original-price'>{displayCents(parseFloat(state.product.originalPrice!) * 100)}</Text>
            )}
          </View>

          {hasDeposit && (
            <View className='deposit-info'>
              <Text className='deposit-price'>押金: {displayCents(parseFloat(state.product.depositPrice!) * 100)}</Text>
            </View>
          )}

          {hasDiscount && (
            <View className='discount-tag'>
              <Text>限时特价</Text>
            </View>
          )}
        </View>

        {/* 产品规格 */}
        <View className='product-specs'>
          <View className='spec-item'>
            <Text className='spec-label'>规格</Text>
            <Text className='spec-value'>{state.product.specification}</Text>
          </View>

          {state.product.waterSource && (
            <View className='spec-item'>
              <Text className='spec-label'>水源</Text>
              <Text className='spec-value'>{state.product.waterSource}</Text>
            </View>
          )}

          {state.product.phValue && (
            <View className='spec-item'>
              <Text className='spec-label'>pH值</Text>
              <Text className='spec-value'>{state.product.phValue}</Text>
            </View>
          )}
        </View>

        <View className='product-meta'>
          <View className='meta-item'>
            <Text className='meta-label'>库存</Text>
            <Text className='meta-value'>{state.product.stock}</Text>
          </View>

          <View className='meta-item'>
            <Text className='meta-label'>销量</Text>
            <Text className='meta-value'>{state.product.salesVolume}</Text>
          </View>
        </View>

        {/* 矿物质含量 */}
        {state.product.mineralContent && (
          <View className='mineral-content'>
            <Text className='mineral-title'>矿物质含量</Text>
            <Text className='mineral-text'>{state.product.mineralContent}</Text>
          </View>
        )}
      </View>

      {/* 产品详情 */}
      {state.product.detailContent && (
        <AtCard
          title='产品详情'
          className='detail-card'
        >
          <View className='detail-content'>
            {state.product.detailContent.split('\n').map((paragraph: string, index: number) => (
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
        </AtCard>
      )}

      {/* 操作按钮 */}
      <View className='action-section'>
        <View className='action-buttons'>
          {/* 预览模式标识 */}
          {state.isPreviewMode && (
            <View className='preview-indicator'>
              <Text className='preview-text'>预览模式</Text>
            </View>
          )}

          {/* 非预览模式显示购买按钮 */}
          {!state.isPreviewMode && state.product.status === 'ONLINE' && (
            <View className='button-row'>
              <AtButton
                type='primary'
                size='normal'
                onClick={handleBuyNow}
                className='buy-button full-width'
                disabled={state.product.stock <= 0}
              >
                {state.product.stock <= 0 ? '暂时缺货' : '立即购买'}
              </AtButton>
            </View>
          )}

          {/* 服务按钮 */}
          <View className='service-buttons'>
            {!state.isPreviewMode && (
              <AtButton
                size='small'
                type='secondary'
                onClick={handleContactService}
                className='service-button'
              >
                联系客服
              </AtButton>
            )}

            <AtButton
              size='small'
              type='secondary'
              onClick={handleShare}
              className='share-button'
              disabled={state.isPreviewMode}
            >
              {state.isPreviewMode ? '预览中' : '分享'}
            </AtButton>
          </View>
        </View>
      </View>

      {/* Toast 提示 */}
      <AtToast
        isOpened={state.showToast}
        text={state.toastText}
        status={state.toastType}
        onClose={hideToast}
      />

      {/* 购买确认弹窗 */}
      <AtModal
        isOpened={state.showBuyModal}
        onClose={handleBuyModalCancel}
        className='buy-modal'
      >
        <AtModalHeader>确认购买</AtModalHeader>
        <AtModalContent>
          <View className='buy-modal-content'>
            {/* 产品信息 */}
            <View className='modal-product-info'>
              <Image
                src={state.product?.coverImageUrl}
                mode='aspectFill'
                className='modal-product-image'
              />
              <View className='modal-product-details'>
                <Text className='modal-product-name'>{state.product?.name}</Text>
                <Text className='modal-product-price'>{displayCents(parseFloat(state.product?.price || '0') * 100)}</Text>
                <Text className='modal-product-spec'>规格: {state.product?.specification}</Text>
                <Text className='modal-product-stock'>库存: {state.product?.stock}</Text>
              </View>
            </View>

            {/* 数量选择 */}
            <View className='modal-quantity-section'>
              <Text className='modal-section-title'>购买数量</Text>
              <AtInputNumber
                value={state.buyModalQuantity}
                min={1}
                max={state.product?.stock || 1}
                onChange={handleBuyModalQuantityChange}
                size='large'
                className='modal-quantity-input'
                type='number'
              />
            </View>

            {/* 押金信息 */}
            {hasDeposit && (
              <View className='modal-deposit-section'>
                <Text className='modal-deposit-info'>
                  押金: {displayCents(parseFloat(state.product.depositPrice!) * 100)}
                </Text>
              </View>
            )}

            {/* 总价 */}
            <View className='modal-total-section'>
              <Text className='modal-total-label'>合计：</Text>
              <Text className='modal-total-price'>
                {calculateAndFormatTotal(state.buyModalQuantity, parseFloat(state.product?.price || '0') * 100)}
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