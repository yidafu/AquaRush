export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  timeout?: number
  header?: Record<string, string>
  data?: any
}

export interface UploadOptions {
  url: string
  filePath: string
  name: string
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

          const errorMessages = result.errors.map(err => err.message).join('; ')
          throw new GraphQLError(`GraphQL Error: ${errorMessages}`, result.errors)
        }

        if (!result.data) {
          throw new GraphQLError('No data returned from GraphQL query')
        }

        return result.data
      } catch (error) {
        console.error('Network request failed:', error)
        if (error instanceof NetworkError || error instanceof GraphQLError) {
          throw error
        }
        if (error.errMsg && error.errMsg.includes('timeout')) {
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
    } catch (error) {
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

    const { url, filePath, name, formData } = options

    try {
      const response = await this.requestHandler.uploadFile({
        url: url.startsWith('http') ? url : `${this.config.baseURL.replace('/graphql', '')}${url}`,
        filePath,
        name,
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
    } catch (error) {
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
  setAuthToken(token: string): void {
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
