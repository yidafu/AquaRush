export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  timeout?: number
  header?: Record<string, string>
  data?: any
}

/**
 * 上传文件源(平台相关):
 * - Taro 小程序:`string` 本地文件路径
 * - React Native / Expo:浏览器风格 `Blob`(推荐,配合 `filename`),或兼容 RN 旧版 fetch 的 `{ uri, name, type }`
 *   (expo/fetch 只接受 string/Blob/File 部件,旧式 `{ uri }` 对象会抛 "Unsupported FormDataPart implementation",
 *   由各端 request handler 负责归一化)
 * - 不要传 React Native 的 `File`:expo/fetch 的 FormData 补丁会写部件的 `name` 属性,
 *   而 RN File 把 `name` 定义为原型上只读 getter,会抛 "Cannot assign to property 'name' which has only a getter"。
 *   统一传 `Blob` + `filename` 即可携带文件名。
 */
export type UploadFilePath = string | File | Blob | { uri: string; name?: string; type?: string }

export interface UploadOptions {
  url: string
  filePath: UploadFilePath
  name: string
  /** multipart 部件文件名(Blob 无 name 属性,需显式传入以生成 Content-Disposition filename)。 */
  filename?: string
  formData?: Record<string, string>
  header?: Record<string, string>
  timeout?: number
}

export interface RequestHandler {
  request: (options: RequestOptions) => Promise<{
    statusCode: number
    data: any
    errMsg?: string
  }>
  uploadFile?: (options: UploadOptions) => Promise<{
    statusCode: number
    data: string
    errMsg?: string
  }>
}

export interface GraphQLResponse<T = any> {
  data?: T
  errors?: Array<{
    message: string
    locations?: Array<{
      line: number
      column: number
    }>
    path?: Array<string | number>
    extensions?: Record<string, any>
  }>
}

/**
 * graphql-java 在「非空字段解析失败」时追加的空值冒泡机制错误
 * (如非空 Mutation 字段抛业务异常后附带的
 * "The field at path '/assignDeliveryWorker' was declared as a non null type ...")。
 * 这类信息对用户无意义,聚合展示错误消息时应过滤,只保留业务/校验错误。
 */
const NULL_BUBBLE_ERROR_PATTERN =
  /was declared as a non null type|non-nullable type|bubble up null|cannot return null for non-nullable/i

/** 是否为 graphql-java 非空字段空值冒泡机制错误 */
export function isGraphQLNullBubbleError(error: { message: string }): boolean {
  return NULL_BUBBLE_ERROR_PATTERN.test(error.message)
}

/**
 * 过滤掉空值冒泡机制错误;若全部被过滤(极端情况)则原样返回,避免吞掉错误。
 */
export function extractMeaningfulGraphQLErrors<T extends { message: string }>(errors: T[]): T[] {
  const meaningful = errors.filter(error => !isGraphQLNullBubbleError(error))
  return meaningful.length > 0 ? meaningful : errors
}

/** 聚合 GraphQL 错误消息(已过滤机制错误),用于展示给用户 */
export function graphQLErrorMessage(errors: Array<{ message: string }>): string {
  return extractMeaningfulGraphQLErrors(errors)
    .map(error => error.message)
    .join('; ')
}

/**
 * 从任意错误对象提取面向用户的错误消息。
 * 兼容:
 * - ApolloError(graphQLErrors 数组,含 graphql-java 空值冒泡机制错误)
 * - NetworkManager 的 GraphQLError(message 已聚合清洗)
 * - 普通 Error / 其它对象
 */
export function extractErrorMessage(error: any, fallback = '操作失败'): string {
  if (!error) return fallback
  if (Array.isArray(error.graphQLErrors) && error.graphQLErrors.length > 0) {
    const message = graphQLErrorMessage(error.graphQLErrors)
    if (message) return message
  }
  const message = typeof error?.message === 'string' ? error.message.trim() : ''
  return message || fallback
}

export interface GraphQLRequestOptions {
  query: string
  variables?: Record<string, any>
  headers?: Record<string, string>
  skipAuth?: boolean
}

export interface RestRequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: any
  headers?: Record<string, string>
  skipAuth?: boolean
}

export interface FileUploadOptions {
  url: string
  filePath: UploadFilePath
  name: string
  /** multipart 部件文件名(透传给 request handler 的 UploadOptions.filename)。 */
  filename?: string
  formData?: Record<string, string>
  headers?: Record<string, string>
  skipAuth?: boolean
}

export interface NetworkConfig {
  baseURL: string
  timeout?: number
  headers?: Record<string, string>
}

export interface AuthHandler {
  getToken: () => string | null
  getTokenAsync: () => Promise<string | null>
  clearToken: () => void
  silentLogin?: () => Promise<boolean>
}

class NetworkError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public response?: any
  ) {
    super(message)
    this.name = 'NetworkError'
  }
}

class GraphQLError extends Error {
  constructor(
    message: string,
    public errors?: Array<{
      message: string
      locations?: Array<{
        line: number
        column: number
      }>
      path?: Array<string | number>
      extensions?: Record<string, any>
    }>
  ) {
    super(message)
    this.name = 'GraphQLError'
  }
}

class NetworkManager {
  private config: NetworkConfig
  private authHandler: AuthHandler
  private requestHandler: RequestHandler
  private static instance: NetworkManager

  constructor(config: NetworkConfig, authHandler: AuthHandler, requestHandler: RequestHandler) {
    this.config = config
    this.authHandler = authHandler
    this.requestHandler = requestHandler
  }

  static getInstance(config?: NetworkConfig, authHandler?: AuthHandler, requestHandler?: RequestHandler): NetworkManager {
    if (!NetworkManager.instance) {
      if (!config || !authHandler || !requestHandler) {
        throw new Error('NetworkManager config, authHandler and requestHandler are required for first initialization')
      }
      NetworkManager.instance = new NetworkManager(config, authHandler, requestHandler)
    }
    return NetworkManager.instance
  }

  static setRequestHandler(requestHandler: RequestHandler): void {
    if (NetworkManager.instance) {
      NetworkManager.instance.requestHandler = requestHandler
    }
  }

  /**
   * 更新认证处理器(单例已初始化后)
   * 用于运行时接入真实的 token 读取/清除与静默登录,使 401/UNAUTHORIZED 时能自动拉起登录验证
   */
  static setAuthHandler(authHandler: AuthHandler): void {
    if (!NetworkManager.instance) {
      throw new Error('NetworkManager must be initialized before setAuthHandler')
    }
    NetworkManager.instance.authHandler = authHandler
  }

  /**
   * 获取当前存储的认证 token
   */
  getAuthToken(): string | null {
    return this.authHandler.getToken()
  }

  /**
   * 异步获取认证 token
   */
  async getAuthTokenAsync(): Promise<string | null> {
    return this.authHandler.getTokenAsync()
  }

  /**
   * 构建GraphQL请求头
   */
  private async buildGraphQLHeaders(options: GraphQLRequestOptions): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...this.config.headers,
      ...options.headers
    }

    if (!options.skipAuth) {
      const token = await this.getAuthTokenAsync()
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  /**
   * 构建REST API请求头
   */
  private async buildRestHeaders(options: RestRequestOptions): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...this.config.headers,
      ...options.headers
    }

    if (!options.skipAuth) {
      const token = await this.getAuthTokenAsync()
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  /**
   * 构建文件上传请求头
   */
  private async buildUploadHeaders(options: FileUploadOptions): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      ...this.config.headers,
      ...options.headers
    }

    if (!options.skipAuth) {
      const token = await this.getAuthTokenAsync()
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  /**
   * 处理认证失败
   */
  private async handleAuthFailure(): Promise<boolean> {
    try {
      console.log('🔐 检测到认证失败，清理登录态并尝试重新登录...')
      this.authHandler.clearToken()

      if (this.authHandler.silentLogin) {
        const loginSuccess = await this.authHandler.silentLogin()
        if (loginSuccess) {
          console.log('✅ 自动重新登录成功')
          return true
        }
      }
      return false
    } catch (error) {
      console.error('❌ 处理认证失败时出错:', error)
      return false
    }
  }

  /**
   * 发起 GraphQL 请求
   */
  async request<T = any>(options: GraphQLRequestOptions): Promise<T> {
    const { query, variables } = options

    const executeRequest = async (retryAfterAuth = false): Promise<T> => {
      try {
        const response = await this.requestHandler.request({
          url: this.config.baseURL,
          method: 'POST',
          timeout: this.config.timeout || 10000,
          header: await this.buildGraphQLHeaders(options),
          data: { query, variables }
        })

        if (response.statusCode !== 200) {
          if (response.statusCode === 401 && !options.skipAuth && !retryAfterAuth) {
            const authSuccess = await this.handleAuthFailure()
            if (authSuccess) {
              return executeRequest(true)
            }
          }
          throw new NetworkError(
            `HTTP Error: ${response.statusCode}`,
            response.statusCode,
            response.data
          )
        }

        const result = response.data as GraphQLResponse<T>

        if (result.errors && result.errors.length > 0) {
          const hasAuthError = result.errors.some(err =>
            err.message.includes('认证失败') ||
            err.message.includes('UNAUTHORIZED') ||
            err.message.includes('请先登录')
          )

          if (hasAuthError && !options.skipAuth && !retryAfterAuth) {
            const authSuccess = await this.handleAuthFailure()
            if (authSuccess) {
              return executeRequest(true)
            }
          }

          // 过滤 graphql-java 非空字段空值冒泡机制错误,避免用户看到
          // "The field at path ... was declared as a non null type ..." 这类内部噪音
          const meaningfulErrors = extractMeaningfulGraphQLErrors(result.errors)
          const errorMessages = meaningfulErrors.map(err => err.message).join('; ')
          throw new GraphQLError(`GraphQL Error: ${errorMessages}`, meaningfulErrors)
        }

        if (!result.data) {
          throw new GraphQLError('No data returned from GraphQL query')
        }

        return result.data
      } catch (error: any) {
        console.error('Network request failed:', error)
        if (error instanceof NetworkError || error instanceof GraphQLError) {
          throw error
        }
        if (error.errMsg && error.errMsg.includes('timeout')) {
          throw new NetworkError('Request timeout', undefined, error)
        }
        // RN/Expo 中 AbortController 超时触发取消时,底层会抛出
        // "fetch failed: Fetch request has been canceled" 之类的取消/中止错误,
        // 与 timeout 一并归类为「请求超时」,避免展示误导性的 "Network request failed"。
        const message = String(error?.message ?? '')
        if (/cancel(?:l)?ed|aborted|timeout|timed out/i.test(message)) {
          throw new NetworkError('Request timeout', undefined, error)
        }
        throw new NetworkError(error.errMsg || 'Network request failed', undefined, error)
      }
    }

    return executeRequest()
  }

  /**
   * GraphQL 查询
   */
  async query<T = any>(
    query: string,
    variables?: Record<string, any>,
    options: Partial<GraphQLRequestOptions> = {}
  ): Promise<T> {
    return this.request<T>({ query, variables, ...options })
  }

  /**
   * GraphQL 变更
   */
  async mutate<T = any>(
    mutation: string,
    variables?: Record<string, any>,
    options: Partial<GraphQLRequestOptions> = {}
  ): Promise<T> {
    return this.request<T>({ query: mutation, variables, ...options })
  }

  /**
   * REST API 请求
   */
  async restRequest<T = any>(options: RestRequestOptions): Promise<T> {
    const { url, method = 'GET', data } = options

    try {
      const response = await this.requestHandler.request({
        url: url.startsWith('http') ? url : `${this.config.baseURL.replace('/graphql', '')}${url}`,
        method,
        timeout: this.config.timeout || 10000,
        header: await this.buildRestHeaders(options),
        data
      })

      if (response.statusCode < 200 || response.statusCode >= 300) {
        if (response.statusCode === 401 && !options.skipAuth) {
          await this.handleAuthFailure()
        }
        throw new NetworkError(
          `HTTP Error: ${response.statusCode}`,
          response.statusCode,
          response.data
        )
      }

      return response.data
    } catch (error: any) {
      console.error('REST request failed:', error)
      if (error instanceof NetworkError) {
        throw error
      }
      if (error.errMsg && error.errMsg.includes('timeout')) {
        throw new NetworkError('Request timeout', undefined, error)
      }
      throw new NetworkError(error.errMsg || 'REST request failed', undefined, error)
    }
  }

  /**
   * 文件上传
   */
  async uploadFile<T = any>(options: FileUploadOptions): Promise<T> {
    if (!this.requestHandler.uploadFile) {
      throw new Error('uploadFile is not implemented in the request handler')
    }

    const { url, filePath, name, filename, formData } = options

    try {
      const response = await this.requestHandler.uploadFile({
        url: url.startsWith('http') ? url : `${this.config.baseURL.replace('/graphql', '')}${url}`,
        filePath,
        name,
        filename,
        formData,
        header: await this.buildUploadHeaders(options),
        timeout: this.config.timeout || 30000
      })

      if (response.statusCode < 200 || response.statusCode >= 300) {
        if (response.statusCode === 401 && !options.skipAuth) {
          await this.handleAuthFailure()
        }
        throw new NetworkError(
          `Upload failed with status: ${response.statusCode}`,
          response.statusCode,
          response.data
        )
      }

      try {
        return JSON.parse(response.data)
      } catch {
        return response.data as any
      }
    } catch (error: any) {
      console.error('File upload failed:', error)
      if (error instanceof NetworkError) {
        throw error
      }
      throw new NetworkError(error.errMsg || 'File upload failed', undefined, error)
    }
  }

  /**
   * REST API GET 请求
   */
  async get<T = any>(url: string, options: Partial<RestRequestOptions> = {}): Promise<T> {
    return this.restRequest<T>({ url, method: 'GET', ...options })
  }

  /**
   * REST API POST 请求
   */
  async post<T = any>(url: string, data?: any, options: Partial<RestRequestOptions> = {}): Promise<T> {
    return this.restRequest<T>({ url, method: 'POST', data, ...options })
  }

  /**
   * REST API PUT 请求
   */
  async put<T = any>(url: string, data?: any, options: Partial<RestRequestOptions> = {}): Promise<T> {
    return this.restRequest<T>({ url, method: 'PUT', data, ...options })
  }

  /**
   * REST API DELETE 请求
   */
  async delete<T = any>(url: string, options: Partial<RestRequestOptions> = {}): Promise<T> {
    return this.restRequest<T>({ url, method: 'DELETE', ...options })
  }

  /**
   * 设置认证 token
   */
  setAuthToken(_token: string): void {
    // This should be handled by the auth handler
  }

  /**
   * 清除认证 token
   */
  clearAuthToken(): void {
    this.authHandler.clearToken()
  }

  /**
   * 更新网络配置
   */
  updateConfig(config: Partial<NetworkConfig>): void {
    this.config = { ...this.config, ...config }
  }

  /**
   * 获取当前配置
   */
  getConfig(): NetworkConfig {
    return { ...this.config }
  }
}

export default NetworkManager
export { NetworkError, GraphQLError }
