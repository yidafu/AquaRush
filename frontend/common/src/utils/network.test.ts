/**
 * NetworkManager 单测
 * 用内存 mock 的 RequestHandler/AuthHandler 验证:
 * 请求头构建、GraphQL/REST/上传流程、401 认证失败重试、超时与错误封装。
 * 注意:直接 new NetworkManager(...) 构造,避免污染全局单例。
 */
import { afterEach, describe, expect, it, jest } from '@jest/globals'
import NetworkManager, {
  GraphQLError,
  NetworkError,
  type AuthHandler,
  type RequestHandler,
} from './network'

const BASE_URL = 'http://localhost:9090/graphql'

const makeAuth = (overrides: Partial<AuthHandler> = {}): AuthHandler => ({
  getToken: () => null,
  getTokenAsync: async () => null,
  clearToken: jest.fn(),
  silentLogin: async () => false,
  ...overrides,
})

/** 记录每次底层请求以便断言 */
const makeRequestHandler = (): RequestHandler & { calls: any[] } => {
  const calls: any[] = []
  const handler: RequestHandler & { calls: any[] } = {
    calls,
    request: jest.fn(async (options) => {
      calls.push(options)
      return { statusCode: 200, data: { data: { ok: true } } }
    }),
  }
  return handler
}

afterEach(() => {
  jest.restoreAllMocks()
})

describe('GraphQL request', () => {
  it('POST 到 baseURL,携带 query/variables 与 Content-Type', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    const data = await manager.request({ query: '{ me { id } }', variables: { a: 1 } })

    expect(data).toEqual({ ok: true })
    expect(handler.calls).toHaveLength(1)
    expect(handler.calls[0]).toMatchObject({
      url: BASE_URL,
      method: 'POST',
      timeout: 10000,
      header: { 'Content-Type': 'application/json' },
      data: { query: '{ me { id } }', variables: { a: 1 } },
    })
  })

  it('有 token 时自动携带 Authorization Bearer 头', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ getTokenAsync: async () => 'token-123' }),
      handler
    )

    await manager.query('{ me { id } }')

    expect(handler.calls[0].header.Authorization).toBe('Bearer token-123')
  })

  it('skipAuth 时不携带 Authorization 头', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ getTokenAsync: async () => 'token-123' }),
      handler
    )

    await manager.query('{ me { id } }', undefined, { skipAuth: true })

    expect(handler.calls[0].header.Authorization).toBeUndefined()
  })

  it('配置级与请求级 headers 合并(请求级优先)', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager(
      { baseURL: BASE_URL, headers: { 'X-Config': '1' } },
      makeAuth(),
      handler
    )

    await manager.query('{ me { id } }', undefined, { headers: { 'X-Config': '2', 'X-Req': 'y' } })

    expect(handler.calls[0].header).toMatchObject({
      'X-Config': '2',
      'X-Req': 'y',
      'Content-Type': 'application/json',
    })
  })
})

describe('GraphQL 错误处理', () => {
  it('HTTP 非 200 抛出 NetworkError 且带状态码', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 500, data: 'boom' }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(manager.query('{ me { id } }')).rejects.toMatchObject({
      name: 'NetworkError',
      statusCode: 500,
    })
  })

  it('响应体含 errors 抛出 GraphQLError 并聚合消息', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({
      statusCode: 200,
      data: { errors: [{ message: '权限不足' }, { message: '参数错误' }] },
    }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(manager.query('{ me { id } }')).rejects.toMatchObject({
      name: 'GraphQLError',
      message: 'GraphQL Error: 权限不足; 参数错误',
    })
  })

  it('响应体无 data 抛出 GraphQLError', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 200, data: { errors: undefined } }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(manager.query('{ me { id } }')).rejects.toMatchObject({
      name: 'GraphQLError',
      message: 'No data returned from GraphQL query',
    })
  })

  it('底层 errMsg 含 timeout 时转成 NetworkError 超时信息', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => {
      throw { errMsg: 'request:fail timeout' }
    })
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(manager.query('{ me { id } }')).rejects.toMatchObject({
      name: 'NetworkError',
      message: 'Request timeout',
    })
  })
})

describe('401 认证失败与自动重试', () => {
  it('401 + silentLogin 成功 → 清 token、重试一次并成功', async () => {
    const handler = makeRequestHandler()
    handler.request = jest
      .fn()
      .mockResolvedValueOnce({ statusCode: 401, data: null })
      .mockResolvedValueOnce({ statusCode: 200, data: { data: { ok: true } } })
    const clearToken = jest.fn()
    const silentLogin = jest.fn(async () => true)
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ clearToken, silentLogin }),
      handler
    )

    const data = await manager.query('{ me { id } }')

    expect(data).toEqual({ ok: true })
    expect(clearToken).toHaveBeenCalledTimes(1)
    expect(silentLogin).toHaveBeenCalledTimes(1)
    expect(handler.request).toHaveBeenCalledTimes(2)
  })

  it('401 + silentLogin 失败 → 抛 NetworkError,不重试', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 401, data: null }))
    const silentLogin = jest.fn(async () => false)
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ silentLogin }),
      handler
    )

    await expect(manager.query('{ me { id } }')).rejects.toMatchObject({
      name: 'NetworkError',
      statusCode: 401,
    })
    expect(handler.request).toHaveBeenCalledTimes(1)
  })

  it('GraphQL errors 含认证失败文案时触发重试', async () => {
    const handler = makeRequestHandler()
    handler.request = jest
      .fn()
      .mockResolvedValueOnce({ statusCode: 200, data: { errors: [{ message: '认证失败' }] } })
      .mockResolvedValueOnce({ statusCode: 200, data: { data: { ok: true } } })
    const silentLogin = jest.fn(async () => true)
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ silentLogin }),
      handler
    )

    const data = await manager.query('{ me { id } }')

    expect(data).toEqual({ ok: true })
    expect(handler.request).toHaveBeenCalledTimes(2)
  })

  it('skipAuth 时不触发认证重试,直接抛错', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 401, data: null }))
    const silentLogin = jest.fn(async () => true)
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ silentLogin }),
      handler
    )

    await expect(
      manager.query('{ me { id } }', undefined, { skipAuth: true })
    ).rejects.toMatchObject({ name: 'NetworkError' })
    expect(silentLogin).not.toHaveBeenCalled()
    expect(handler.request).toHaveBeenCalledTimes(1)
  })
})

describe('REST 请求', () => {
  it('相对路径拼接 baseURL(去掉 /graphql 后缀)', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 200, data: { list: [] } }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    const data = await manager.get('/api/orders')

    expect(data).toEqual({ list: [] })
    expect(handler.request).toHaveBeenCalledWith(
      expect.objectContaining({ url: 'http://localhost:9090/api/orders', method: 'GET' })
    )
  })

  it('完整 URL 不拼接', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await manager.post('https://other.example.com/x', { a: 1 })

    expect(handler.calls[0].url).toBe('https://other.example.com/x')
    expect(handler.calls[0].data).toEqual({ a: 1 })
  })

  it('非 2xx 抛出 NetworkError,401 时触发认证处理', async () => {
    const handler = makeRequestHandler()
    handler.request = jest.fn(async () => ({ statusCode: 401, data: null }))
    const clearToken = jest.fn()
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ clearToken }),
      handler
    )

    await expect(manager.delete('/api/x')).rejects.toMatchObject({
      name: 'NetworkError',
      statusCode: 401,
    })
    expect(clearToken).toHaveBeenCalled()
  })
})

describe('文件上传', () => {
  it('解析 JSON 响应返回对象', async () => {
    const handler = makeRequestHandler()
    handler.uploadFile = jest.fn(async (options) => ({
      statusCode: 200,
      data: JSON.stringify({ url: 'http://cdn/x.png' }),
    }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    const data = await manager.uploadFile({
      url: '/api/upload',
      filePath: '/tmp/a.png',
      name: 'file',
    })

    expect(data).toEqual({ url: 'http://cdn/x.png' })
    expect(handler.uploadFile).toHaveBeenCalledWith(
      expect.objectContaining({
        url: 'http://localhost:9090/api/upload',
        name: 'file',
        timeout: 30000,
      })
    )
  })

  it('非 JSON 响应原样返回字符串', async () => {
    const handler = makeRequestHandler()
    handler.uploadFile = jest.fn(async () => ({ statusCode: 200, data: 'plain' }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    expect(await manager.uploadFile({ url: '/u', filePath: '/f', name: 'f' })).toBe('plain')
  })

  it('未实现 uploadFile 时抛出明确错误', async () => {
    const handler = makeRequestHandler()
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(
      manager.uploadFile({ url: '/u', filePath: '/f', name: 'f' })
    ).rejects.toThrow('uploadFile is not implemented')
  })

  it('上传失败(非 2xx)抛出 NetworkError', async () => {
    const handler = makeRequestHandler()
    handler.uploadFile = jest.fn(async () => ({ statusCode: 500, data: 'err' }))
    const manager = new NetworkManager({ baseURL: BASE_URL }, makeAuth(), handler)

    await expect(
      manager.uploadFile({ url: '/u', filePath: '/f', name: 'f' })
    ).rejects.toMatchObject({ name: 'NetworkError', statusCode: 500 })
  })
})

describe('配置与认证辅助方法', () => {
  it('getConfig / updateConfig', () => {
    const manager = new NetworkManager({ baseURL: BASE_URL, timeout: 5000 }, makeAuth(), makeRequestHandler())
    expect(manager.getConfig()).toEqual({ baseURL: BASE_URL, timeout: 5000 })

    manager.updateConfig({ timeout: 8000 })
    expect(manager.getConfig()).toEqual({ baseURL: BASE_URL, timeout: 8000 })
  })

  it('getAuthToken 同步取 token,clearAuthToken 清理', () => {
    const clearToken = jest.fn()
    const manager = new NetworkManager(
      { baseURL: BASE_URL },
      makeAuth({ getToken: () => 'sync-token', clearToken }),
      makeRequestHandler()
    )
    expect(manager.getAuthToken()).toBe('sync-token')
    expect(manager.getAuthTokenAsync()).resolves.toBeNull()

    manager.clearAuthToken()
    expect(clearToken).toHaveBeenCalled()
  })
})
