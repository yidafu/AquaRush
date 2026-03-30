import React, { useState } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { AtButton } from 'taro-ui'
import 'taro-ui/dist/style/components/button.scss'
import 'taro-ui/dist/style/components/icon.scss'
import './index.scss'
import { createOrder } from '../../services/delivery'
import { PageContainer } from '../../components/PageContainer'
import AddressDisplay from '../../components/AddressDisplay'
import ProductSelector, { Product } from '../../components/ProductSelector'
import OrderSummaryCard, { OrderInfo } from './components/OrderSummaryCard'

const CreateTaskPage: React.FC = () => {
  const [selectedAddressId, setSelectedAddressId] = useState<string>('')
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [orderInfo, setOrderInfo] = useState<OrderInfo>({
    quantity: 1,
    isSelfCollect: false,
    remark: '',
  })
  const [submitting, setSubmitting] = useState(false)

  // 提交创建订单
  const handleSubmit = async () => {
    if (!selectedAddressId) {
      Taro.showToast({ title: '请选择地址', icon: 'none' })
      return
    }

    if (!selectedProduct) {
      Taro.showToast({ title: '请选择商品', icon: 'none' })
      return
    }

    if (orderInfo.quantity < 1) {
      Taro.showToast({ title: '数量不能少于1', icon: 'none' })
      return
    }

    try {
      setSubmitting(true)
      Taro.showLoading({ title: '创建中...' })

      await createOrder(
        selectedProduct.id,
        selectedAddressId,
        orderInfo.quantity,
        orderInfo.isSelfCollect,
        orderInfo.remark || undefined
      )

      Taro.hideLoading()
      Taro.showToast({ title: '创建成功', icon: 'success' })

      // 返回订单列表页
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } catch (error: any) {
      Taro.hideLoading()
      Taro.showToast({ title: error.message || '创建失败', icon: 'none' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <PageContainer title='创建送水订单'>
      <View className='create-task-page'>
        {/* 地址选择 */}
        <View className='section'>
          <Text className='section-title'>选择地址</Text>
          <AddressDisplay
            selectedId={selectedAddressId}
            onSelect={(address) => setSelectedAddressId(address.id)}
          />
        </View>

        {/* 商品选择 */}
        <View className='section'>
          <Text className='section-title'>选择商品</Text>
          <ProductSelector
            selectedProductId={selectedProduct?.id}
            onSelect={(product) => setSelectedProduct(product)}
          />
        </View>

        <View className='section'>
          <Text className='section-title'>订单详情</Text>
          <OrderSummaryCard
            product={selectedProduct}
            orderInfo={orderInfo}
            onOrderInfoChange={setOrderInfo}
          />
        </View>

        {/* 提交按钮 */}
        <View className='submit-section'>
          <AtButton
            type='primary'
            loading={submitting}
            disabled={submitting || !selectedAddressId || !selectedProduct || orderInfo.quantity < 1}
            onClick={handleSubmit}
            className='submit-button'
          >
            {submitting ? '创建中...' : '创建送水订单'}
          </AtButton>
        </View>

        {/* 底部安全区域 */}
        <View className='safe-bottom' />
      </View>
    </PageContainer>
  )
}

export default CreateTaskPage
