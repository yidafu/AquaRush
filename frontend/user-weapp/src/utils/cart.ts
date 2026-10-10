import Taro from '@tarojs/taro'

/**
 * 购物车工具(F-PRO-004,本地存储):
 * 商品详情「加入购物车」累积多商品(同商品按 productId 合并数量),购物车页调整/删除/勾选,
 * 结算时携带勾选项进入确认订单页(F-ORD-004 多商品下单)。
 * 金额一律以分存储与计算(R-MON-01),展示层经 displayCents 换算。
 */

export interface CartItem {
  id: string
  name: string
  priceCents: number
  quantity: number
  image: string
  specification?: string
  /** 结算勾选状态(本地购物车交互态) */
  selected?: boolean
}

const CART_KEY = 'cart'

export function getCart(): CartItem[] {
  try {
    const cart = Taro.getStorageSync(CART_KEY)
    return Array.isArray(cart) ? cart : []
  } catch (error) {
    console.error('读取购物车失败:', error)
    return []
  }
}

function saveCart(cart: CartItem[]): void {
  Taro.setStorageSync(CART_KEY, cart)
}

/**
 * 加入购物车:同商品合并数量(AC-PRO-004-01/02)
 */
export function addToCart(item: CartItem): CartItem[] {
  const cart = getCart()
  const existing = cart.find(it => it.id === item.id)
  if (existing) {
    existing.quantity += item.quantity
  } else {
    cart.push({ ...item, selected: true })
  }
  saveCart(cart)
  return cart
}

/**
 * 更新数量(≤0 时移除;上限 999 与后端一致)
 */
export function updateCartQuantity(productId: string, quantity: number): CartItem[] {
  let cart = getCart()
  if (quantity <= 0) {
    cart = cart.filter(it => it.id !== productId)
  } else {
    const item = cart.find(it => it.id === productId)
    if (item) {
      item.quantity = Math.min(quantity, 999)
    }
  }
  saveCart(cart)
  return cart
}

export function removeCartItem(productId: string): CartItem[] {
  const cart = getCart().filter(it => it.id !== productId)
  saveCart(cart)
  return cart
}

export function setCartSelected(productId: string, selected: boolean): CartItem[] {
  const cart = getCart()
  const item = cart.find(it => it.id === productId)
  if (item) {
    item.selected = selected
  }
  saveCart(cart)
  return cart
}

export function setCartAllSelected(selected: boolean): CartItem[] {
  const cart = getCart().map(it => ({ ...it, selected }))
  saveCart(cart)
  return cart
}

/** 勾选商品(结算用) */
export function getSelectedCartItems(): CartItem[] {
  return getCart().filter(it => it.selected)
}

/** 结算成功后移除已下单的勾选商品 */
export function removeSelectedCartItems(): CartItem[] {
  const cart = getCart().filter(it => !it.selected)
  saveCart(cart)
  return cart
}

/** 勾选商品合计(分) */
export function getSelectedTotalCents(): number {
  return getSelectedCartItems().reduce((sum, it) => sum + it.priceCents * it.quantity, 0)
}

export function clearCart(): void {
  Taro.removeStorageSync(CART_KEY)
}
