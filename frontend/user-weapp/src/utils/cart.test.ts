/**
 * 购物车工具单测(F-PRO-004,本地存储)
 * mock 掉 @tarojs/taro 的 storage API,验证合并/增删改/勾选/合计等纯逻辑。
 */
import { beforeEach, describe, expect, it, jest } from '@jest/globals'
import type { CartItem } from './cart'
import {
  addToCart,
  clearCart,
  getCart,
  getSelectedCartItems,
  getSelectedTotalCents,
  removeCartItem,
  removeSelectedCartItems,
  setCartAllSelected,
  setCartSelected,
  updateCartQuantity,
} from './cart'

jest.mock(
  '@tarojs/taro',
  () => {
    const store = new Map<string, any>()
    return {
      __esModule: true,
      default: {
        getStorageSync: jest.fn((key: string) => (store.has(key) ? store.get(key) : '')),
        setStorageSync: jest.fn((key: string, value: any) => void store.set(key, value)),
        removeStorageSync: jest.fn((key: string) => void store.delete(key)),
      },
      __store: store,
    }
  },
  { virtual: true }
)

const makeItem = (overrides: Partial<CartItem> = {}): CartItem => ({
  id: 'p1',
  name: '农夫山泉 550ml',
  priceCents: 200,
  quantity: 1,
  image: 'http://x/a.png',
  ...overrides,
})

beforeEach(() => {
  const taroMock = jest.requireMock('@tarojs/taro') as { __store: Map<string, any> }
  taroMock.__store.clear()
  jest.clearAllMocks()
})

describe('getCart', () => {
  it('无存储或非数组时返回空数组', () => {
    expect(getCart()).toEqual([])

    const taroMock = jest.requireMock('@tarojs/taro') as { __store: Map<string, any> }
    taroMock.__store.set('cart', 'not-an-array')
    expect(getCart()).toEqual([])
  })

  it('返回已存储的购物车', () => {
    const items = [makeItem()]
    const taroMock = jest.requireMock('@tarojs/taro') as { __store: Map<string, any> }
    taroMock.__store.set('cart', items)
    expect(getCart()).toEqual(items)
  })
})

describe('addToCart', () => {
  it('新商品加入并默认勾选', () => {
    const cart = addToCart(makeItem({ id: 'p1', quantity: 2 }))

    expect(cart).toHaveLength(1)
    expect(cart[0]).toMatchObject({ id: 'p1', quantity: 2, selected: true })
  })

  it('同商品合并数量而非新增条目', () => {
    addToCart(makeItem({ id: 'p1', quantity: 1 }))
    const cart = addToCart(makeItem({ id: 'p1', quantity: 3 }))

    expect(cart).toHaveLength(1)
    expect(cart[0].quantity).toBe(4)
  })
})

describe('updateCartQuantity', () => {
  it('数量 > 0 时更新,并受 999 上限约束', () => {
    addToCart(makeItem({ id: 'p1', quantity: 1 }))

    expect(updateCartQuantity('p1', 5)[0].quantity).toBe(5)
    expect(updateCartQuantity('p1', 10000)[0].quantity).toBe(999)
  })

  it('数量 ≤ 0 时移除该商品', () => {
    addToCart(makeItem({ id: 'p1' }))
    addToCart(makeItem({ id: 'p2' }))

    const cart = updateCartQuantity('p1', 0)
    expect(cart.map((it) => it.id)).toEqual(['p2'])
  })

  it('不存在的商品保持不变', () => {
    addToCart(makeItem({ id: 'p1' }))
    expect(updateCartQuantity('nope', 3)).toHaveLength(1)
  })
})

describe('removeCartItem / setCartSelected / setCartAllSelected', () => {
  it('移除指定商品', () => {
    addToCart(makeItem({ id: 'p1' }))
    addToCart(makeItem({ id: 'p2' }))

    expect(removeCartItem('p1').map((it) => it.id)).toEqual(['p2'])
  })

  it('切换单个商品勾选状态', () => {
    addToCart(makeItem({ id: 'p1' }))

    expect(setCartSelected('p1', false)[0].selected).toBe(false)
    expect(setCartSelected('p1', true)[0].selected).toBe(true)
  })

  it('全选/全不选', () => {
    addToCart(makeItem({ id: 'p1' }))
    addToCart(makeItem({ id: 'p2' }))

    expect(setCartAllSelected(false).every((it) => it.selected === false)).toBe(true)
    expect(setCartAllSelected(true).every((it) => it.selected === true)).toBe(true)
  })
})

describe('结算相关', () => {
  it('getSelectedCartItems 只返回勾选项', () => {
    addToCart(makeItem({ id: 'p1' }))
    addToCart(makeItem({ id: 'p2' }))
    setCartSelected('p2', false)

    expect(getSelectedCartItems().map((it) => it.id)).toEqual(['p1'])
  })

  it('getSelectedTotalCents 累加勾选项(分)', () => {
    addToCart(makeItem({ id: 'p1', priceCents: 200, quantity: 2 }))
    addToCart(makeItem({ id: 'p2', priceCents: 150, quantity: 1 }))
    setCartSelected('p2', false)

    expect(getSelectedTotalCents()).toBe(400)
  })

  it('removeSelectedCartItems 结算后移除已下单项', () => {
    addToCart(makeItem({ id: 'p1' }))
    addToCart(makeItem({ id: 'p2' }))
    setCartSelected('p2', false)

    const cart = removeSelectedCartItems()
    expect(cart.map((it) => it.id)).toEqual(['p2'])
  })
})

describe('clearCart', () => {
  it('清空本地存储', () => {
    addToCart(makeItem({ id: 'p1' }))
    expect(getCart()).toHaveLength(1)

    clearCart()
    expect(getCart()).toEqual([])
  })
})
