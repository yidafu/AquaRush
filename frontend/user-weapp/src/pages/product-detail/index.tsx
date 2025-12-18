import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, RichText } from '@tarojs/components'
import { AtButton, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import { CONTACT_INFO } from '@/constants'
import ProductService from '../../services/ProductService'
import { Product } from '../../types/product'

// Import extracted components
import ProductImageGallery from './components/ProductImageGallery'
import ProductInfo from './components/ProductInfo'
import ActionButtons from './components/ActionButtons'
import BuyModal from './components/BuyModal'
import LoadingSkeleton from './components/LoadingSkeleton'
import { ProductProvider } from './context/ProductContext'

interface ProductDetailState {
  product: Product | null
  loading: boolean
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  productId: string
  showBuyModal: boolean
  buyModalQuantity: number
  imagesLoaded: boolean[]
  imagesWithError: boolean[]
  imageLoadingStates: boolean[]
  isFavorite: boolean
}

// Retry configuration
const MAX_RETRIES = 3
const RETRY_DELAY = 1000 // 1 second

const ProductDetail: React.FC = () => {
  const [state, setState] = useState<ProductDetailState>({
    product: null,
    loading: true,
    showToast: false,
    toastText: '',
    toastType: 'success',
    productId: '',
    showBuyModal: false,
    buyModalQuantity: 1,
    imagesLoaded: [],
    imagesWithError: [],
    imageLoadingStates: [],
    isFavorite: false
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

  const loadProductDetail = useCallback(async (retryCount = 0) => {
    if (!state.productId) return

    try {
      updateState({ loading: true })

      const productService = ProductService.getInstance()
      console.log(`Loading product detail for ID: ${state.productId} (attempt ${retryCount + 1}/${MAX_RETRIES})`)

      const result = await productService.getProductDetail(state.productId)

      if (result.success && result.data) {
        updateState({ product: result.data })
        const imageCount = result.data.imageGallery?.length || 1
        initializeImageStates(imageCount)
        console.log('Product loaded successfully:', result.data)
      } else {
        // Handle specific error cases
        const errorMessage = result.error?.message || '产品不存在'

        if (result.error?.code === 'PRODUCT_NOT_FOUND') {
          showToastMessage('产品不存在', 'error')
        } else if (result.error?.code === 'NETWORK_ERROR' && retryCount < MAX_RETRIES - 1) {
          // Retry on network errors
          console.log(`Network error, retrying in ${RETRY_DELAY}ms...`)
          setTimeout(() => {
            loadProductDetail(retryCount + 1)
          }, RETRY_DELAY)
          return
        } else {
          showToastMessage(errorMessage, 'error')
        }
      }
    } catch (error) {
      console.error('Unexpected error loading product:', error)

      // Retry on unexpected errors
      if (retryCount < MAX_RETRIES - 1) {
        console.log(`Unexpected error, retrying in ${RETRY_DELAY}ms...`)
        setTimeout(() => {
          loadProductDetail(retryCount + 1)
        }, RETRY_DELAY)
        return
      }

      showToastMessage('加载失败，请检查网络后重试', 'error')
    } finally {
      updateState({ loading: false })
    }
  }, [state.productId, showToastMessage, updateState])


  const handleBuyNow = useCallback(() => {
    if (!state.product) return

    updateState({
      showBuyModal: true,
      buyModalQuantity: 1
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
        specification: state.product.specification
      }],
      totalAmount: parseFloat(state.product.price) * state.buyModalQuantity,
      specification: state.product.specification
    }

    // 将订单数据缓存到本地存储
    Taro.setStorageSync('pendingOrder', orderData)

    updateState({ showBuyModal: false })

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [state.product, state.buyModalQuantity, showToastMessage, updateState])

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

  const handleToggleFavorite = useCallback(() => {
    setState(prev => ({ ...prev, isFavorite: !prev.isFavorite }))
    showToastMessage(
      state.isFavorite ? '已取消收藏' : '已添加收藏',
      'success'
    )
  }, [state.isFavorite, showToastMessage])

  const handleAddToCart = useCallback(() => {
    if (!state.product) return

    // 简单的购物车添加逻辑（实际项目中应该调用购物车API）
    const cartItem = {
      id: state.product.id,
      name: state.product.name,
      price: parseFloat(state.product.price),
      quantity: 1,
      image: state.product.coverImageUrl,
      specification: state.product.specification
    }

    // 获取现有购物车数据
    const existingCart = Taro.getStorageSync('cart') || []
    const updatedCart = [...existingCart, cartItem]

    // 保存到本地存储
    Taro.setStorageSync('cart', updatedCart)

    showToastMessage('已加入购物车', 'success')
  }, [state.product, showToastMessage])

  const handleImageLoad = useCallback((index: number) => {
    setState(prev => ({
      ...prev,
      imagesLoaded: [...prev.imagesLoaded.slice(0, index), true, ...prev.imagesLoaded.slice(index + 1)],
      imagesWithError: [...prev.imagesWithError.slice(0, index), false, ...prev.imagesWithError.slice(index + 1)],
      imageLoadingStates: [...prev.imageLoadingStates.slice(0, index), false, ...prev.imageLoadingStates.slice(index + 1)]
    }))
  }, [])

  const handleImageError = useCallback((index: number) => {
    setState(prev => ({
      ...prev,
      imagesWithError: [...prev.imagesWithError.slice(0, index), true, ...prev.imagesWithError.slice(index + 1)],
      imageLoadingStates: [...prev.imageLoadingStates.slice(0, index), false, ...prev.imageLoadingStates.slice(index + 1)]
    }))
  }, [])

  const initializeImageStates = useCallback((imageCount: number) => {
    setState(prev => ({
      ...prev,
      imagesLoaded: new Array(imageCount).fill(false),
      imagesWithError: new Array(imageCount).fill(false),
      imageLoadingStates: new Array(imageCount).fill(true)
    }))
  }, [])

  const handleImagePress = useCallback((index: number) => {
    // 预览大图功能
    const currentImages = state.product?.imageGallery && state.product.imageGallery.length > 0
      ? state.product.imageGallery as string[]
      : [state.product?.coverImageUrl || '']

    Taro.previewImage({
      current: currentImages[index],
      urls: currentImages
    })
  }, [state.product])

  const handlePullDownRefresh = useCallback(() => {
    loadProductDetail().finally(() => {
      Taro.stopPullDownRefresh()
    })
  }, [loadProductDetail])

  // 初始化
  useEffect(() => {
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

    updateState({
      productId: id
    })
  }, [updateState])

  // 加载产品详情
  useEffect(() => {
    if (state.productId) {
      loadProductDetail()
    }
  }, [state.productId, loadProductDetail])

  // 注册下拉刷新
  Taro.usePullDownRefresh(handlePullDownRefresh)

  // Loading state - use skeleton component
  if (state.loading) {
    return <LoadingSkeleton />
  }

  // Error state
  if (!state.product) {
    return (
      <View className='min-h-screen pb-40 bg-gray-50'>
        <View className='flex flex-col items-center justify-center gap-20 min-h-96'>
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

  return (
    <View className='min-h-screen pb-10 bg-gray-100 md:max-w-md md:mx-auto md:shadow-xl md:rounded-3xl md:overflow-hidden'>
      {/* 产品图片轮播 */}
      <ProductImageGallery
        images={currentImages}
        onImagePress={handleImagePress}
        imagesLoaded={state.imagesLoaded}
        imagesWithError={state.imagesWithError}
        imageLoadingStates={state.imageLoadingStates}
        onImageLoad={handleImageLoad}
        onImageError={handleImageError}
      />

      {/* 产品信息 */}
      <ProductProvider
        product={state.product}
      >
        <ProductInfo />
      </ProductProvider>

      {/* 产品详情 */}
      {state.product.detailContent && (
        <View className='mt-4 mb-5 bg-white rounded animate-fade-in'>
          <View className='p-4'>
            <RichText nodes={state.product.detailContent} />
          </View>
        </View>
      )}

      {/* 操作按钮 */}
      <ActionButtons
        isFavorite={state.isFavorite}
        onToggleFavorite={handleToggleFavorite}
        onAddToCart={handleAddToCart}
        onBuyNow={handleBuyNow}
        onContactService={handleContactService}
        onShare={handleShare}
        stock={state.product.stock}
        status={state.product.status}
      />

      {/* Toast 提示 */}
      <AtToast
        isOpened={state.showToast}
        text={state.toastText}
        status={state.toastType}
        onClose={hideToast}
      />

      {/* 购买确认弹窗 */}
      <BuyModal
        isOpened={state.showBuyModal}
        product={state.product}
        quantity={state.buyModalQuantity}
        onQuantityChange={handleBuyModalQuantityChange}
        onConfirm={handleBuyModalConfirm}
        onCancel={handleBuyModalCancel}
      />

      {/* 底部安全区域 */}
      <View className='h-40' />
    </View>
  )
}

export default ProductDetail
