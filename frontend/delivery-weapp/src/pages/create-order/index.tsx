import React, { useState } from 'react'
import { View, Text, Textarea, Switch } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { AtButton, AtInputNumber } from 'taro-ui'
import 'taro-ui/dist/style/components/button.scss'
import 'taro-ui/dist/style/components/input-number.scss'
import 'taro-ui/dist/style/components/icon.scss'
import './index.scss'
import { createOrder } from '../../services/delivery'
import { useAuth } from '../../hooks/useAuth'
import { PageContainer } from '../../components/PageContainer'
import AddressDisplay from '../../components/AddressDisplay'
import ProductSelector, { Product } from '../../components/ProductSelector'

const CreateTaskPage: React.FC = () => {
  const { workerInfo } = useAuth()
  const [selectedAddressId, setSelectedAddressId] = useState<string>('')
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [quantity, setQuantity] = useState<number>(1)
  const [isSelfCollect, setIsSelfCollect] = useState<boolean>(false)
  const [remark, setRemark] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)

  // 格式化价格（分转元）
  const formatPrice = (cents: number): string => {
    return (cents / 100).toFixed(2)
  }

  // 计算总价
  const totalAmount = selectedProduct ? selectedProduct.price * quantity : 0

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

    if (quantity < 1) {
      Taro.showToast({ title: '数量不能少于1', icon: 'none' })
      return
    }

    try {
      setSubmitting(true)
      Taro.showLoading({ title: '创建中...' })

      await createOrder(
        selectedProduct.id,
        selectedAddressId,
        quantity,
        isSelfCollect,
        remark || undefined
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

        {/* 数量选择 */}
        <View className='section'>
          <Text className='section-title'>数量</Text>
          <View className='card quantity-card'>
            <AtInputNumber
              min={1}
              max={99}
              step={1}
              value={quantity}
              type='digit'
              onChange={(value: number) => setQuantity(value)}
            />
          </View>
        </View>

        {/* 费用明细 */}
          <View className='section'>
            <Text className='section-title'>费用明细</Text>
            <View className='card summary-card'>
              <View className='summary-row'>
                <Text className='summary-label'>商品单价</Text>
                <Text className='summary-value'>¥{formatPrice(selectedProduct?.price ?? 0)}</Text>
              </View>
              <View className='summary-row'>
                <Text className='summary-label'>数量</Text>
                <Text className='summary-value'>× {quantity}</Text>
              </View>
              <View className='summary-row total'>
                <Text className='summary-label'>合计</Text>
                <Text className='summary-value'>¥{formatPrice(totalAmount)}</Text>
              </View>
            </View>
          </View>

        {/* 是否已收款 */}
        <View className='section'>
          <Text className='section-title'>是否已收款</Text>
          <View className='card switch-card'>
            <Text className='switch-label'>{isSelfCollect ? '已收款' : '未收款'}</Text>
            <Switch
              checked={isSelfCollect}
              onChange={(e: any) => setIsSelfCollect(e.detail.value)}
              color='#1890ff'
            />
          </View>
        </View>

        {/* 备注 */}
        <View className='section'>
          <Text className='section-title'>备注</Text>
          <View className='card'>
            <Textarea
              className='remark-input'
              value={remark}
              onInput={(e) => setRemark(e.detail.value)}
              placeholder='请输入订单备注（可选）'
              maxlength={500}
              autoHeight
            />
          </View>
        </View>

        {/* 提交按钮 */}
        <View className='submit-section'>
          <AtButton
            type='primary'
            loading={submitting}
            disabled={submitting || !selectedAddressId || !selectedProduct || quantity < 1}
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
