/**
 * Http 请求封装单测
 * mock axios 实例与 @/ui message,验证:
 * - 请求拦截器:token 注入 Authorization;
 * - 响应拦截器:success 解包 / 业务失败抛错 / 网络错误提示;
 * - get/post/put/delete 透传到 axios 实例。
 * 注意:mock 工厂被 jest 提升到文件顶部,工厂内不能引用外部变量,
 * 共享实例统一经 jest.requireMock 取用。
 */
import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals'

// ---- mock 环境(localStorage)与 axios/@/ui ----
const storage = new Map<string, string>()

Object.defineProperty(globalThis, 'localStorage', {
  value: {
    getItem: (key: string) => (storage.has(key) ? storage.get(key)! : null),
    setItem: (key: string, value: string) => void storage.set(key, value),
    removeItem: (key: string) => void storage.delete(key),
  },
  configurable: true,
})

jest.mock('axios', () => {
  const instance = {
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  }
  return { create: jest.fn(() => instance) }
})

jest.mock('@/ui', () => ({
  message: { error: jest.fn() },
}))

import http from './http'

interface MockAxiosInstance {
  interceptors: {
    request: { use: jest.Mock }
    response: { use: jest.Mock }
  }
  get: jest.Mock
  post: jest.Mock
  put: jest.Mock
  delete: jest.Mock
}

// Http 单例构造时经 axios.create() 拿到同一个 mock 实例
const instance = (
  jest.requireMock('axios') as { create: jest.Mock<() => MockAxiosInstance> }
).create()
const messageError = (jest.requireMock('@/ui') as { message: { error: jest.Mock } }).message.error

const requestInterceptor = (
  instance.interceptors.request.use as jest.Mock
).mock.calls[0][0] as (config: { headers: Record<string, string> }) => {
  headers: Record<string, string>
}
const responseOnFulfilled = (
  instance.interceptors.response.use as jest.Mock
).mock.calls[0][0] as (response: {
  data: { success: boolean; data?: unknown; message: string; code?: string }
}) => unknown
const responseOnRejected = (
  instance.interceptors.response.use as jest.Mock
).mock.calls[0][1] as (error: Error) => Promise<Error>

beforeEach(() => {
  storage.clear()
  // 只清方法级 mock;构造期(import 时)的 create/拦截器注册记录需保留用于断言
  instance.get.mockClear()
  instance.post.mockClear()
  instance.put.mockClear()
  instance.delete.mockClear()
  messageError.mockClear()
})

afterEach(() => {
  storage.clear()
})

describe('axios 实例初始化', () => {
  it('以 /api 为 baseURL、10s 超时创建实例', () => {
    const { create } = jest.requireMock('axios') as { create: jest.Mock }
    expect(create).toHaveBeenCalledWith({ baseURL: '/api', timeout: 10000 })
  })

  it('注册了请求与响应拦截器', () => {
    expect(instance.interceptors.request.use).toHaveBeenCalled()
    expect(instance.interceptors.response.use).toHaveBeenCalled()
  })
})

describe('请求拦截器: token 注入', () => {
  it('有 token 时注入 Authorization: Bearer <token>', () => {
    storage.set('token', 'abc123')
    const config: { headers: Record<string, string> } = { headers: {} }

    const result = requestInterceptor(config) as { headers: Record<string, string> }

    expect(result.headers.Authorization).toBe('Bearer abc123')
  })

  it('无 token 时不注入 Authorization', () => {
    const config: { headers: Record<string, string> } = { headers: {} }

    requestInterceptor(config)

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('请求拦截器同时注册了错误处理回调', () => {
    expect(typeof (instance.interceptors.request.use as jest.Mock).mock.calls[0][1]).toBe(
      'function'
    )
  })
})

describe('响应拦截器: 成功解包与业务失败', () => {
  it('success: true 时解包返回 data', () => {
    const result = responseOnFulfilled({
      data: { success: true, data: { id: 1 }, message: 'ok', code: '0' },
    })
    expect(result).toEqual({ id: 1 })
  })

  it('success: false 时抛出业务错误信息', () => {
    expect(() =>
      responseOnFulfilled({
        data: { success: false, data: null, message: '库存不足', code: 'E1' },
      })
    ).toThrow('库存不足')
  })

  it('网络/HTTP 错误时提示 antd message 并 reject', () => {
    const error = new Error('Network Error')

    expect(responseOnRejected(error)).rejects.toBe(error)
    expect(messageError).toHaveBeenCalledWith('Network Error')
  })

  it('无 message 的网络错误使用兜底文案', async () => {
    await expect(responseOnRejected(new Error())).rejects.toBeInstanceOf(Error)

    expect(messageError).toHaveBeenCalledWith('网络错误')
  })
})

describe('HTTP 方法透传', () => {
  it('get/delete 透传 url 与 config', () => {
    http.get('/logs/api', { params: { page: 1 } })
    http.delete('/logs/1', { timeout: 5000 })

    expect(instance.get).toHaveBeenCalledWith('/logs/api', { params: { page: 1 } })
    expect(instance.delete).toHaveBeenCalledWith('/logs/1', { timeout: 5000 })
  })

  it('post/put 透传 url、data 与 config', () => {
    http.post('/orders', { a: 1 }, { headers: { 'X-1': '1' } })
    http.put('/orders/1', { b: 2 })

    expect(instance.post).toHaveBeenCalledWith('/orders', { a: 1 }, { headers: { 'X-1': '1' } })
    // put 未传 config 时,Http.put 会透传 undefined 作为第三个参数
    expect(instance.put).toHaveBeenCalledWith('/orders/1', { b: 2 }, undefined)
  })
})
