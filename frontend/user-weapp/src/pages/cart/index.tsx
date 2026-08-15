import React, { useState, useCallback } from 'react'
import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { displayCents } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import {
  CartItem,
  getCart,
  updateCartQuantity,
  removeCartItem,
  setCartSelected,
  setCartAllSelected,
  getSelectedCartItems,
  getSelectedTotalCents
} from '../../utils/cart'

import "taro-ui/dist/style/components/button.scss"
import './index.scss'

/**
 * 购物车页(F-PRO-004,本地存储):
 * 多商品列表(勾选/数量步进/删除)、合计实时汇总(分→元)、「去结算」携带勾选商品进入确认订单页(F-ORD-004)。
 */
const CartPage: React.FC = () => {
  usePageViewTracking()

  const [cartItems, setCartItems] = useState<CartItem[]>([])

  const refreshCart = useCallback(() => {
    setCartItems(getCart())
  }, [])

  useDidShow(() => {
    refreshCart()
  })

  const handleQuantityChange = useCallback((id: string, quantity: number) => {
    setCartItems(updateCartQuantity(id, quantity))
  }, [])

  const handleRemove = useCallback((id: string) => {
    Taro.showModal({
      title: '删除商品',
      content: '确定要删除该商品吗?',
      success: (res) => {
        if (res.confirm) {
          setCartItems(removeCartItem(id))
          Taro.showToast({ title: '已删除', icon: 'success' })
        }
      }
    })
  }, [])

  const handleSelect = useCallback((id: string, selected: boolean) => {
    setCartItems(setCartSelected(id, selected))
  }, [])

  const handleSelectAll = useCallback((selected: boolean) => {
    setCartItems(setCartAllSelected(selected))
  }, [])

  const handleCheckout = useCallback(() => {
    const selected = getSelectedCartItems()
    if (selected.length === 0) {
      Taro.showToast({ title: '请选择要结算的商品', icon: 'none' })
      return
    }

    // 组装多商品明细进入确认订单页(F-ORD-004)
    const orderData = {
      products: selected.map(item => ({
        id: item.id,
        name: item.name,
        price: item.priceCents,
        quantity: item.quantity,
        image: item.image,
        specification: item.specification
      })),
      totalAmount: getSelectedTotalCents()
    }
    Taro.setStorageSync('pendingOrder', orderData)
    // 结算来源标记:下单成功后回写购物车移除已购商品
    Taro.setStorageSync('cartCheckout', true)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }, [])

  const handleGoShopping = useCallback(() => {
    Taro.switchTab({
      url: '/pages/home/index'
    })
  }, [])

  const allSelected = cartItems.length > 0 && cartItems.every(it => it.selected)
  const totalCents = getSelectedTotalCents()

  return (
    <View className='cart-page'>
      {cartItems.length === 0 ? (
        <View className='cart-empty'>
          <View className='cart-empty-icon'>
            <Text className='cart-empty-emoji'>🛒</Text>
          </View>
          <Text className='cart-empty-text'>购物车空空如也,去逛逛吧~</Text>
          <View className='cart-empty-btn' onClick={handleGoShopping}>
            <Text className='cart-empty-btn-text'>去逛逛</Text>
          </View>
        </View>
      ) : (
        <>
          <ScrollView scrollY className='cart-list'>
            {cartItems.map(item => (
              <View key={item.id} className='cart-item'>
                {/* 勾选框 */}
                <View
                  className={`cart-check ${item.selected ? 'cart-check-on' : ''}`}
                  onClick={() => handleSelect(item.id, !item.selected)}
                >
                  <Text className='cart-check-icon'>{item.selected ? '✓' : ''}</Text>
                </View>
                {/* 商品图 */}
                <Image
                  src={item.image || '/assets/default-product.png'}
                  mode='aspectFill'
                  className='cart-item-image'
                />
                {/* 商品信息 */}
                <View className='cart-item-info'>
                  <Text className='cart-item-name'>{item.name}</Text>
                  {item.specification && (
                    <Text className='cart-item-spec'>规格:{item.specification}</Text>
                  )}
                  <View className='cart-item-bottom'>
                    <Text className='cart-item-price'>{displayCents(item.priceCents)}</Text>
                    <View className='cart-stepper'>
                      <View
                        className={`cart-stepper-btn ${item.quantity <= 1 ? 'cart-stepper-disabled' : ''}`}
                        onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                      >
                        <Text className='cart-stepper-icon'>−</Text>
                      </View>
                      <Text className='cart-stepper-value'>{item.quantity}</Text>
                      <View
                        className={`cart-stepper-btn ${item.quantity >= 999 ? 'cart-stepper-disabled' : ''}`}
                        onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                      >
                        <Text className='cart-stepper-icon'>+</Text>
                      </View>
                    </View>
                  </View>
                </View>
                {/* 删除 */}
                <View className='cart-item-delete' onClick={() => handleRemove(item.id)}>
                  <Text className='cart-item-delete-icon'>×</Text>
                </View>
              </View>
            ))}
          </ScrollView>

          {/* 底部结算栏 */}
          <View className='cart-footer'>
            <View className='cart-select-all' onClick={() => handleSelectAll(!allSelected)}>
              <View className={`cart-check ${allSelected ? 'cart-check-on' : ''}`}>
                <Text className='cart-check-icon'>{allSelected ? '✓' : ''}</Text>
              </View>
              <Text className='cart-select-all-text'>全选</Text>
            </View>
            <View className='cart-total'>
              <Text className='cart-total-label'>合计:</Text>
              <Text className='cart-total-price'>{displayCents(totalCents)}</Text>
            </View>
            <View className='cart-checkout-btn' onClick={handleCheckout}>
              <Text className='cart-checkout-text'>去结算({getSelectedCartItems().length})</Text>
            </View>
          </View>
        </>
      )}
    </View>
  )
}

export default CartPage