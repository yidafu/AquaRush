import React, { useState, useEffect, useRef, useCallback } from 'react'
import { View, Text, Textarea, ScrollView, Image } from '@tarojs/components'
import { AtFloatLayout, AtIcon } from 'taro-ui'
import 'taro-ui/dist/style/components/float-layout.scss'
import 'taro-ui/dist/style/components/icon.scss'
import { searchProducts } from '../../services/delivery'
import './index.scss'

export interface Product {
  id: string
  name: string
  price: number
  image: string
  stock: number
  status: string
}

interface ProductSelectorProps {
  selectedProductId?: string
  onSelect: (product: Product) => void
}

// 格式化价格（分转元）
const formatPrice = (cents: number): string => {
  return (cents / 100).toFixed(2)
}

const ProductSelector: React.FC<ProductSelectorProps> = ({
  selectedProductId,
  onSelect
}) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [products, setProducts] = useState<Product[]>([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [initialLoadDone, setInitialLoadDone] = useState(false)
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Load initial products when modal opens
  useEffect(() => {
    if (modalVisible && !initialLoadDone) {
      loadProducts('')
    }
  }, [modalVisible, initialLoadDone])

  const loadProducts = (keyword: string) => {
    setSearchLoading(true)
    searchProducts(keyword, 20)
      .then((res) => {
        if (res?.data?.activeProducts?.list) {
          setProducts(res.data.activeProducts.list)
          setInitialLoadDone(true)
        }
      })
      .catch((err) => {
        console.error('Load products failed:', err)
      })
      .finally(() => {
        setSearchLoading(false)
      })
  }

  // Debounced search function
  const performSearch = useCallback((keyword: string) => {
    loadProducts(keyword)
  }, [])

  // Debounced search when keyword changes
  useEffect(() => {
    if (searchTimerRef.current) {
      clearTimeout(searchTimerRef.current)
    }

    searchTimerRef.current = setTimeout(() => {
      performSearch(searchKeyword)
    }, 300)

    return () => {
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current)
      }
    }
  }, [searchKeyword, performSearch])

  const handleClick = () => {
    setModalVisible(true)
  }

  const handleSelect = (product: Product) => {
    onSelect?.(product)
    setModalVisible(false)
    setSearchKeyword('')
  }

  const handleClose = () => {
    setModalVisible(false)
    setSearchKeyword('')
  }

  // Get the selected product for display
  const selectedProduct = products.find(p => p.id === selectedProductId)

  return (
    <>
      <View
        className='card product-card'
        onClick={handleClick}
      >
        {searchLoading ? (
          <Text className='loading-text'>加载中...</Text>
        ) : selectedProductId && selectedProduct ? (
          <View className='selected-product'>
            <Image
              className='product-thumb'
              src={selectedProduct.image || 'https://via.placeholder.com/100'}
              mode='aspectFill'
            />
            <View className='product-info'>
              <Text className='product-name'>{selectedProduct.name}</Text>
              <Text className='product-price'>¥{formatPrice(selectedProduct.price)}</Text>
              <Text className='product-stock'>库存: {selectedProduct.stock}</Text>
            </View>
            <View className='chevron'>
              <Text className='chevron-text'>›</Text>
            </View>
          </View>
        ) : (
          <Text className='empty-text'>点击选择商品</Text>
        )}
      </View>

      <AtFloatLayout
        isOpened={modalVisible}
        title='选择商品'
        onClose={handleClose}
      >
        {/* Search Bar */}
        <View className='modal-search-bar'>
          <View className='search-input-wrap'>
            <AtIcon value='search' size='16' color='#999' />
            <Textarea
              className='search-input'
              placeholder='搜索商品名称'
              value={searchKeyword}
              onInput={(e) => setSearchKeyword(e.detail.value)}
              adjustPosition
            />
            {searchKeyword && (
              <View className='clear-btn' onClick={() => setSearchKeyword('')}>
                <AtIcon value='close-circle' size='16' color='#999' />
              </View>
            )}
          </View>
        </View>

        {/* Product List */}
        <ScrollView className='modal-product-list' scrollY>
          {searchLoading && products?.length === 0 ? (
            <View className='empty-state'>
              <Text className='loading-text'>加载中...</Text>
            </View>
          ) : products?.length === 0 ? (
            <View className='empty-state'>
              <Text className='empty-text'>
                {initialLoadDone ? '暂无可用商品' : '没有匹配的商品'}
              </Text>
            </View>
          ) : (
            products?.map(product => (
              <View
                key={product.id}
                className={`product-item ${selectedProductId === product.id ? 'selected' : ''}`}
                onClick={() => handleSelect(product)}
              >
                <Image
                  className='product-image'
                  src={product.image || 'https://via.placeholder.com/100'}
                  mode='aspectFill'
                />
                <View className='product-info'>
                  <Text className='product-name'>{product.name}</Text>
                  <Text className='product-price'>¥{formatPrice(product.price)}</Text>
                  <Text className='product-stock'>库存: {product.stock}</Text>
                </View>
                {selectedProductId === product.id && (
                  <View className='check-icon'>
                    <AtIcon value='check' size='20' color='#1890ff' />
                  </View>
                )}
              </View>
            ))
          )}
        </ScrollView>
      </AtFloatLayout>
    </>
  )
}

export default ProductSelector
