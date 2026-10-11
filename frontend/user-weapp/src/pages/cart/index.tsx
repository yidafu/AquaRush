import React, { useState, useCallback, useEffect } from 'react'
import { View, Text, ScrollView, Image } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { Money } from '@aquarush/common'
import { pageTheme } from '../../styles/theme.stylex'
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

/** 购物车样式(原 index.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用) */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box'
  },
  scroll: {
    flexGrow: 1,
    height: 'calc(100vh - 128rpx - env(safe-area-inset-bottom))',
    boxSizing: 'border-box'
  },
  // 原 .cart-cells 后代选择器拍平到子元素
  checkHd: {
    alignSelf: 'center',
    marginRight: '20rpx'
  },
  itemImage: {
    overflow: 'hidden'
  },
  row: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: '16rpx'
  },
  subtotal: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)'
  },
  delete: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-RED)'
  },
  empty: {
    flexGrow: 1
  },
  emptyEmoji: {
    fontSize: '72rpx'
  },
  // 底部结算栏:固定 + 安全区(原 .cart-footbar 后代选择器拍平)
  footbar: {
    position: 'fixed',
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 100,
    paddingBottom: 'calc(16rpx + env(safe-area-inset-bottom))'
  },
  selectAll: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '12rpx',
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto'
  },
  selectAllLbl: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)'
  },
  total: {
    textAlign: 'right'
  }
})

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
    <View {...stylex.props(pageTheme, styles.page)}>
      {cartItems.length === 0 ? (
        <View {...stylex.props(styles.empty)}>
          <EmptyState
            icon={<Text {...stylex.props(styles.emptyEmoji)}>🛒</Text>}
            title='购物车还是空的'
            description='去挑几桶好水,加入购物车一起下单'
            actionText='去逛逛'
            onAction={handleGoShopping}
          />
        </View>
      ) : (
        <>
          <ScrollView scrollY {...stylex.props(styles.scroll)}>
            <View className='weui-cells__title'>全部商品</View>
            <View className='weui-cells'>
              {cartItems.map(item => (
                <View key={item.id} className='weui-media-box'>
                  {/* 勾选 */}
                  <View
                    {...props('weui-media-box__hd', styles.checkHd)}
                    onClick={() => handleSelect(item.id, !item.selected)}
                  >
                    <View className={`weui-checkbox${item.selected ? ' on' : ''}`} />
                  </View>
                  {/* 商品图 */}
                  <View className='weui-media-box__hd'>
                    <Image
                      src={item.image || '/assets/default-product.png'}
                      mode='aspectFill'
                      {...props('weui-media-box__thumb', styles.itemImage)}
                    />
                  </View>
                  {/* 商品信息 */}
                  <View className='weui-media-box__bd'>
                    <Text className='weui-media-box__title'>{item.name}</Text>
                    {item.specification && (
                      <Text className='weui-media-box__desc'>规格:{item.specification}</Text>
                    )}
                    <View {...stylex.props(styles.row)}>
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
                    <View {...stylex.props(styles.row)}>
                      <Text {...stylex.props(styles.subtotal)}>
                        小计 <Text className='weui-price weui-price--sm'><Money cents={item.priceCents * item.quantity} /></Text>
                      </Text>
                      <Text {...stylex.props(styles.delete)} onClick={() => handleRemove(item.id)}>删除</Text>
                    </View>
                  </View>
                </View>
              ))}
            </View>
          </ScrollView>

          {/* 底部结算栏(WeUI .mp-footbar) */}
          <View {...props('mp-footbar', 'hair-t', styles.footbar)}>
            <View {...props(styles.selectAll)} onClick={() => handleSelectAll(!allSelected)}>
              <View className={`weui-checkbox${allSelected ? ' on' : ''}`} />
              <Text {...stylex.props(styles.selectAllLbl)}>全选</Text>
            </View>
            <View {...props('mp-footbar__info', styles.total)}>
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
