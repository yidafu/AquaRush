import React, { useState, useCallback, useEffect } from 'react'
import { View, Text, ScrollView, Image } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { Money } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import EmptyState from '../../components/EmptyState'
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

import './index.scss'

/**
 * 购物车页(F-PRO-004,本地存储;设计稿 01-product/cart.html WeUI 视觉重排):
 * .weui-cells 内 .weui-media-box 商品行(勾选 .weui-checkbox + 缩略图 + 名称/规格 +
 * .weui-price 单价 + .weui-stepper 数量 + 小计 + 删除)、.mp-footbar 底部结算栏
 * (全选 + 合计 .weui-price--lg + 去结算)。
 * 数据结构与增删改逻辑走 utils/cart,原样保留;库存冲突提示为规划中功能,本页不做。
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

  // tabBar 角标:购物车数据变化时同步总件数(上限 99+),空购物车移除角标(F-PRO-004)
  useEffect(() => {
  }, [cartItems])

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
  const selectedCount = getSelectedCartItems().length

  return (
    <View className='cart-page'>
      {cartItems.length === 0 ? (
        <View className='cart-empty'>
          <EmptyState
            icon={<Text className='cart-empty-emoji'>🛒</Text>}
            title='购物车还是空的'
            description='去挑几桶好水,加入购物车一起下单'
            actionText='去逛逛'
            onAction={handleGoShopping}
          />
        </View>
      ) : (
        <>
          <ScrollView scrollY className='cart-scroll'>
            <View className='weui-cells__title'>全部商品</View>
            <View className='weui-cells cart-cells'>
              {cartItems.map(item => (
                <View key={item.id} className='weui-media-box cart-item'>
                  {/* 勾选 */}
                  <View
                    className='weui-media-box__hd cart-check-hd'
                    onClick={() => handleSelect(item.id, !item.selected)}
                  >
                    <View className={`weui-checkbox${item.selected ? ' on' : ''}`} />
                  </View>
                  {/* 商品图 */}
                  <View className='weui-media-box__hd'>
                    <Image
                      src={item.image || '/assets/default-product.png'}
                      mode='aspectFill'
                      className='weui-media-box__thumb cart-item-image'
                    />
                  </View>
                  {/* 商品信息 */}
                  <View className='weui-media-box__bd'>
                    <Text className='weui-media-box__title'>{item.name}</Text>
                    {item.specification && (
                      <Text className='weui-media-box__desc'>规格:{item.specification}</Text>
                    )}
                    <View className='cart-row'>
                      <Text className='weui-price weui-price--md'>
                        <Money cents={item.priceCents} />
                      </Text>
                      <View className='weui-stepper'>
                        <View
                          className={`weui-stepper__btn${item.quantity <= 1 ? ' disabled' : ''}`}
                          onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                        >
                          <Text>−</Text>
                        </View>
                        <Text className='weui-stepper__num'>{item.quantity}</Text>
                        <View
                          className={`weui-stepper__btn${item.quantity >= 999 ? ' disabled' : ''}`}
                          onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                        >
                          <Text>+</Text>
                        </View>
                      </View>
                    </View>
                    <View className='cart-row'>
                      <Text className='cart-subtotal'>
                        小计 <Text className='weui-price weui-price--sm'><Money cents={item.priceCents * item.quantity} /></Text>
                      </Text>
                      <Text className='cart-delete' onClick={() => handleRemove(item.id)}>删除</Text>
                    </View>
                  </View>
                </View>
              ))}
            </View>
          </ScrollView>

          {/* 底部结算栏(WeUI .mp-footbar) */}
          <View className='mp-footbar hair-t cart-footbar'>
            <View className='cart-select-all' onClick={() => handleSelectAll(!allSelected)}>
              <View className={`weui-checkbox${allSelected ? ' on' : ''}`} />
              <Text className='select-all-lbl'>全选</Text>
            </View>
            <View className='mp-footbar__info cart-total'>
              <View className='mp-footbar__lbl'>合计</View>
              <Text className='weui-price weui-price--lg'>
                <Money cents={totalCents} />
              </Text>
            </View>
            <View
              className={`weui-btn weui-btn_primary${selectedCount === 0 ? ' weui-btn_disabled' : ''}`}
              onClick={handleCheckout}
            >
              去结算({selectedCount})
            </View>
          </View>
        </>
      )}
    </View>
  )
}

export default CartPage
