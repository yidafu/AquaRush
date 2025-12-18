/**
 * API Configuration Management
 *
 * Centralized configuration for all API endpoints and network settings.
 * Supports environment-specific configuration with fallback defaults.
 */

interface ApiConfig {
  // GraphQL Configuration
  graphqlUrl: string

  // REST API Configuration
  restApiBaseUrl: string

  // Network Configuration
  timeout: number
  headers: Record<string, string>

  // Environment
  environment: 'development' | 'production' | 'test'

  // Feature flags
  enableMockData: boolean
  enableLogging: boolean
}

/**
 * Default configuration values
 */
const DEFAULT_CONFIG: ApiConfig = {
  graphqlUrl: 'http://localhost:8080/graphql',
  restApiBaseUrl: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  environment: 'development',
  enableMockData: false,
  enableLogging: true
}

/**
 * Production configuration overrides
 */
const PRODUCTION_CONFIG: Partial<ApiConfig> = {
  timeout: 15000,
  enableMockData: false,
  enableLogging: false
}

/**
 * Development configuration overrides
 */
const DEVELOPMENT_CONFIG: Partial<ApiConfig> = {
  timeout: 10000,
  enableMockData: true,
  enableLogging: true
}

/**
 * Get environment-specific configuration
 */
function getEnvironmentConfig(): Partial<ApiConfig> {
  const env = process.env.NODE_ENV || 'development'

  switch (env) {
    case 'production':
      return PRODUCTION_CONFIG
    case 'development':
      return DEVELOPMENT_CONFIG
    case 'test':
      return {
        timeout: 5000,
        enableMockData: true,
        enableLogging: false
      }
    default:
      return {}
  }
}

/**
 * Get configuration from environment variables
 */
function getEnvironmentVariableConfig(): Partial<ApiConfig> {
  const config: Partial<ApiConfig> = {}

  // GraphQL endpoint configuration
  if (process.env.TARO_APP_GRAPHQL_URL) {
    config.graphqlUrl = process.env.TARO_APP_GRAPHQL_URL
  }

  // REST API base URL configuration
  if (process.env.TARO_APP_API_BASE_URL) {
    config.restApiBaseUrl = process.env.TARO_APP_API_BASE_URL
  } else if (process.env.VITE_API_BASE_URL) {
    config.restApiBaseUrl = process.env.VITE_API_BASE_URL
  }

  // Timeout configuration
  if (process.env.TARO_APP_API_TIMEOUT) {
    const timeout = parseInt(process.env.TARO_APP_API_TIMEOUT, 10)
    if (!isNaN(timeout) && timeout > 0) {
      config.timeout = timeout
    }
  }

  // Feature flags from environment variables
  if (process.env.TARO_APP_ENABLE_MOCK_DATA) {
    config.enableMockData = process.env.TARO_APP_ENABLE_MOCK_DATA === 'true'
  }

  if (process.env.TARO_APP_ENABLE_API_LOGGING) {
    config.enableLogging = process.env.TARO_APP_ENABLE_API_LOGGING === 'true'
  }

  return config
}

/**
 * Create final API configuration by merging all configuration sources
 */
function createApiConfig(): ApiConfig {
  const environmentConfig = getEnvironmentConfig()
  const envVarConfig = getEnvironmentVariableConfig()

  // Merge configurations in order of precedence:
  // 1. Environment variables (highest precedence)
  // 2. Environment-specific config
  // 3. Default config (lowest precedence)
  const finalConfig: ApiConfig = {
    ...DEFAULT_CONFIG,
    ...environmentConfig,
    ...envVarConfig,
    // Ensure graphqlUrl is derived from restApiBaseUrl if not explicitly set
    graphqlUrl: envVarConfig.graphqlUrl ||
                environmentConfig.graphqlUrl ||
                `${envVarConfig.restApiBaseUrl || environmentConfig.restApiBaseUrl || DEFAULT_CONFIG.restApiBaseUrl}/graphql`,
    // Override environment detection
    environment: (process.env.NODE_ENV as any) || 'development'
  }

  return finalConfig
}

/**
 * API Configuration instance
 */
const apiConfig = createApiConfig()

/**
 * API Configuration Manager Class
 */
class ApiConfigManager {
  private config: ApiConfig

  constructor() {
    this.config = { ...apiConfig }
  }

  /**
   * Get the current API configuration
   */
  getConfig(): ApiConfig {
    return { ...this.config }
  }

  /**
   * Get GraphQL endpoint URL
   */
  getGraphqlUrl(): string {
    return this.config.graphqlUrl
  }

  /**
   * Get REST API base URL
   */
  getRestApiBaseUrl(): string {
    return this.config.restApiBaseUrl
  }

  /**
   * Get network timeout setting
   */
  getTimeout(): number {
    return this.config.timeout
  }

  /**
   * Get default headers
   */
  getHeaders(): Record<string, string> {
    return { ...this.config.headers }
  }

  /**
   * Get current environment
   */
  getEnvironment(): string {
    return this.config.environment
  }

  /**
   * Check if mock data is enabled
   */
  isMockDataEnabled(): boolean {
    return this.config.enableMockData
  }

  /**
   * Check if API logging is enabled
   */
  isLoggingEnabled(): boolean {
    return this.config.enableLogging
  }

  /**
   * Update configuration (for runtime updates)
   */
  updateConfig(updates: Partial<ApiConfig>): void {
    this.config = { ...this.config, ...updates }
  }

  /**
   * Reset configuration to defaults
   */
  resetConfig(): void {
    this.config = { ...apiConfig }
  }

  /**
   * Get configuration summary for debugging
   */
  getConfigSummary(): {
    environment: string
    graphqlUrl: string
    restApiBaseUrl: string
    timeout: number
    enableMockData: boolean
    enableLogging: boolean
  } {
    return {
      environment: this.config.environment,
      graphqlUrl: this.config.graphqlUrl,
      restApiBaseUrl: this.config.restApiBaseUrl,
      timeout: this.config.timeout,
      enableMockData: this.config.enableMockData,
      enableLogging: this.config.enableLogging
    }
  }
}

// Create singleton instance
const apiConfigManager = new ApiConfigManager()

// Export the configuration manager instance and types
export default apiConfigManager
export { ApiConfig, ApiConfigManager }

// Export convenience functions for backward compatibility
export const getApiConfig = () => apiConfigManager.getConfig()
export const getGraphqlUrl = () => apiConfigManager.getGraphqlUrl()
export const getRestApiBaseUrl = () => apiConfigManager.getRestApiBaseUrl()
export const getApiTimeout = () => apiConfigManager.getTimeout()
export const getApiHeaders = () => apiConfigManager.getHeaders()