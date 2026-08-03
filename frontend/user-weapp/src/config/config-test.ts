/**
 * Configuration Verification Utility
 *
 * This file can be used to test and verify that the API configuration
 * is working correctly in different environments.
 */

import apiConfig from './api'

/**
 * Configuration verification result
 */
interface ConfigVerificationResult {
  success: boolean
  environment: string
  graphqlUrl: string
  restApiBaseUrl: string
  timeout: number
  enableMockData: boolean
  enableLogging: boolean
  errors?: string[]
}

/**
 * Verify API configuration
 */
export function verifyApiConfig(): ConfigVerificationResult {
  const errors: string[] = []

  try {
    const config = apiConfig.getConfig()

    // Check required configuration values
    if (!config.graphqlUrl) {
      errors.push('GraphQL URL is not configured')
    }

    if (!config.restApiBaseUrl) {
      errors.push('REST API base URL is not configured')
    }

    if (config.timeout <= 0) {
      errors.push('Timeout must be greater than 0')
    }

    // Check URL formats
    try {
      new URL(config.graphqlUrl)
    } catch {
      errors.push('GraphQL URL is not a valid URL')
    }

    try {
      new URL(config.restApiBaseUrl)
    } catch {
      errors.push('REST API base URL is not a valid URL')
    }

    const result: ConfigVerificationResult = {
      success: errors.length === 0,
      environment: config.environment,
      graphqlUrl: config.graphqlUrl,
      restApiBaseUrl: config.restApiBaseUrl,
      timeout: config.timeout,
      enableMockData: config.enableMockData,
      enableLogging: config.enableLogging,
      errors: errors.length > 0 ? errors : undefined
    }

    return result

  } catch (error) {
    return {
      success: false,
      environment: 'unknown',
      graphqlUrl: '',
      restApiBaseUrl: '',
      timeout: 0,
      enableMockData: false,
      enableLogging: false,
      errors: [`Failed to load configuration: ${error instanceof Error ? error.message : 'Unknown error'}`]
    }
  }
}

/**
 * Log configuration summary for debugging
 */
export function logConfigurationSummary(): void {
  const summary = apiConfig.getConfigSummary()

  console.group('🔧 API Configuration Summary')
  console.log('Environment:', summary.environment)
  console.log('GraphQL URL:', summary.graphqlUrl)
  console.log('REST API Base URL:', summary.restApiBaseUrl)
  console.log('Timeout:', `${summary.timeout}ms`)
  console.log('Mock Data Enabled:', summary.enableMockData)
  console.log('API Logging Enabled:', summary.enableLogging)
  console.groupEnd()
}

/**
 * Test configuration in development mode
 */
export function testConfigurationInDevelopment(): boolean {
  if (process.env.NODE_ENV === 'development') {
    const verification = verifyApiConfig()

    if (!verification.success) {
      console.error('❌ API Configuration Verification Failed:')
      verification.errors?.forEach(error => console.error(`  - ${error}`))
      return false
    }

    console.log('✅ API Configuration Verified Successfully')
    logConfigurationSummary()
    return true
  }

  return true // Skip verification in non-development environments
}

// Export the main verification function as default
export default verifyApiConfig