import Taro from '@tarojs/taro'

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
  filePath: string
  name: string
  formData?: Record<string, string>
  headers?: Record<string, string>
  skipAuth?: boolean
}

export interface NetworkConfig {
  baseURL: string
  timeout?: number
  headers?: Record<string, string>
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
  private static instance: NetworkManager

  constructor(config: NetworkConfig) {
    this.config = config
  }

  static getInstance(config?: NetworkConfig): NetworkManager {
    if (!NetworkManager.instance) {
      if (!config) {
        throw new Error('NetworkManager config is required for first initialization')
      }
      NetworkManager.instance = new NetworkManager(config)
    }
    return NetworkManager.instance
  }

  /**
   * 获取当前存储的认证 token
   */
  getAuthToken(): string | null {
    try {
      return Taro.getStorageSync('auth_token')
    } catch (error) {
      console.error('Failed to get auth token:', error)
      return null
    }
  }

  /**
   * 异步获取认证 token，如果没有则尝试获取新 token
   */
  async getAuthTokenAsync(): Promise<string | null> {
    try {
      // 首先尝试从存储获取 token
      let token = Taro.getStorageSync('auth_token')

      if (token) {
        return token
      }

      // 如果没有 token，尝试静默登录获取新 token
      const { authService } = await import('./auth')
      if (await authService.silentLogin()) {
        token = Taro.getStorageSync('auth_token')
        return token
      }

      return null
    } catch (error) {
      console.error('Failed to get auth token asynchronously:', error)
      return null
    }
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

    // 添加认证头（如果没有明确跳过）
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

    // 添加认证头（如果没有明确跳过）
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

    // 添加认证头（如果没有明确跳过）
    if (!options.skipAuth) {
      const token = await this.getAuthTokenAsync()
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  /**
   * 处理认证失败：清理登录态并尝试重新登录
   */
  private async handleAuthFailure(): Promise<boolean> {
    try {
      console.log('🔐 检测到认证失败，清理登录态并尝试重新登录...')

      // 清理认证数据
      this.clearAuthToken()

      // 尝试静默重新登录
      const { authService } = await import('./auth')
      const loginSuccess = await authService.silentLogin()

      if (loginSuccess) {
        console.log('✅ 自动重新登录成功')
        return true
      } else {
        console.log('⚠️ 自动重新登录失败，需要用户手动登录')
        return false
      }
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
        const response = await Taro.request({
          url: this.config.baseURL,
          method: 'POST',
          timeout: this.config.timeout || 10000,
          header: await this.buildGraphQLHeaders(options),
          data: {
            query,
            variables
          }
        })

        // 检查 HTTP 状态码
        if (response.statusCode !== 200) {
          // 处理认证失败 (401)
          if (response.statusCode === 401 && !options.skipAuth && !retryAfterAuth) {
            const authSuccess = await this.handleAuthFailure()
            if (authSuccess) {
              console.log('🔄 重新登录成功，重试原始请求...')
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

        // 检查 GraphQL 错误
        if (result.errors && result.errors.length > 0) {
          // 检查是否包含认证错误
          const hasAuthError = result.errors.some(err =>
            err.message.includes('认证失败') ||
            err.message.includes('UNAUTHORIZED') ||
            err.message.includes('请先登录')
          )

          if (hasAuthError && !options.skipAuth && !retryAfterAuth) {
            const authSuccess = await this.handleAuthFailure()
            if (authSuccess) {
              console.log('🔄 重新登录成功，重试原始请求...')
              return executeRequest(true)
            }
          }

          const errorMessages = result.errors.map(err => err.message).join('; ')
          throw new GraphQLError(
            `GraphQL Error: ${errorMessages}`,
            result.errors
          )
        }

        // 检查是否有返回数据
        if (!result.data) {
          throw new GraphQLError('No data returned from GraphQL query')
        }

        return result.data

      } catch (error) {
        console.error('Network request failed:', error)

        if (error instanceof NetworkError || error instanceof GraphQLError) {
          throw error
        }

        // 处理网络超时错误
        if (error.errMsg && error.errMsg.includes('timeout')) {
          throw new NetworkError('Request timeout', undefined, error)
        }

        // 处理其他网络错误
        throw new NetworkError(
          error.errMsg || 'Network request failed',
          undefined,
          error
        )
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
    return this.request<T>({
      query,
      variables,
      ...options
    })
  }

  /**
   * GraphQL 变更
   */
  async mutate<T = any>(
    mutation: string,
    variables?: Record<string, any>,
    options: Partial<GraphQLRequestOptions> = {}
  ): Promise<T> {
    return this.request<T>({
      query: mutation,
      variables,
      ...options
    })
  }

  /**
   * REST API 请求
   */
  async restRequest<T = any>(options: RestRequestOptions): Promise<T> {
    const { url, method = 'GET', data } = options

    try {
      const response = await Taro.request({
        url: url.startsWith('http') ? url : `${this.config.baseURL.replace('/graphql', '')}${url}`,
        method,
        timeout: this.config.timeout || 10000,
        header: await this.buildRestHeaders(options),
        data
      })

      // 检查 HTTP 状态码
      if (response.statusCode < 200 || response.statusCode >= 300) {
        // 处理认证失败 (401)
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

    } catch (error) {
      console.error('REST request failed:', error)

      if (error instanceof NetworkError) {
        throw error
      }

      // 处理网络超时错误
      if (error.errMsg && error.errMsg.includes('timeout')) {
        throw new NetworkError('Request timeout', undefined, error)
      }

      // 处理其他网络错误
      throw new NetworkError(
        error.errMsg || 'REST request failed',
        undefined,
        error
      )
    }
  }

  /**
   * 文件上传
   */
  async uploadFile<T = any>(options: FileUploadOptions): Promise<T> {
    const { url, filePath, name, formData } = options

    try {
      const response = await Taro.uploadFile({
        url: url.startsWith('http') ? url : `${this.config.baseURL.replace('/graphql', '')}${url}`,
        filePath,
        name,
        formData,
        header: await this.buildUploadHeaders(options),
        timeout: this.config.timeout || 30000 // 文件上传超时时间更长
      })

      // 检查 HTTP 状态码
      if (response.statusCode < 200 || response.statusCode >= 300) {
        // 处理认证失败 (401)
        if (response.statusCode === 401 && !options.skipAuth) {
          await this.handleAuthFailure()
        }
        throw new NetworkError(
          `Upload failed with status: ${response.statusCode}`,
          response.statusCode,
          response.data
        )
      }

      // 尝试解析JSON响应
      try {
        return JSON.parse(response.data)
      } catch (parseError) {
        // 如果不是JSON，直接返回原始数据
        return response.data as any
      }

    } catch (error) {
      console.error('File upload failed:', error)

      if (error instanceof NetworkError) {
        throw error
      }

      // 处理网络超时错误
      if (error.errMsg && error.errMsg.includes('timeout')) {
        throw new NetworkError('Upload timeout', undefined, error)
      }

      // 处理其他网络错误
      throw new NetworkError(
        error.errMsg || 'File upload failed',
        undefined,
        error
      )
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
  setAuthToken(token: string): void {
    try {
      Taro.setStorageSync('auth_token', token)
    } catch (error) {
      console.error('Failed to store auth token:', error)
    }
  }

  /**
   * 清除认证 token
   */
  clearAuthToken(): void {
    try {
      Taro.removeStorageSync('auth_token')
    } catch (error) {
      console.error('Failed to clear auth token:', error)
    }
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