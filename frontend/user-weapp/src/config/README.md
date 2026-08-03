# API Configuration Management

This directory contains the centralized API configuration management system for the user-weapp frontend.

## Files

- `api.ts` - Main API configuration manager
- `config-test.ts` - Configuration verification utilities
- `README.md` - This documentation file

## Overview

The API configuration system provides:

- **Centralized Configuration**: All API endpoints and network settings in one place
- **Environment Support**: Different configurations for development, production, and test environments
- **Type Safety**: Full TypeScript support with proper interfaces
- **Flexible Configuration**: Support for environment variables and runtime configuration updates
- **Built-in Validation**: Automatic verification of configuration values

## Usage

### Basic Usage

```typescript
import apiConfig from '@/config/api'

// Get GraphQL URL
const graphqlUrl = apiConfig.getGraphqlUrl()

// Get REST API base URL
const restApiUrl = apiConfig.getRestApiBaseUrl()

// Get timeout setting
const timeout = apiConfig.getTimeout()

// Check if mock data is enabled
const useMockData = apiConfig.isMockDataEnabled()
```

### Advanced Usage

```typescript
import apiConfig, { getApiConfig, getGraphqlUrl } from '@/config/api'

// Get full configuration object
const config = getApiConfig()

// Get specific values using convenience functions
const url = getGraphqlUrl()

// Update configuration at runtime
apiConfig.updateConfig({
  timeout: 15000,
  enableLogging: false
})

// Reset to defaults
apiConfig.resetConfig()
```

### Service Integration

Services should import and use the configuration:

```typescript
import NetworkManager from '../utils/network'
import apiConfig from '../config/api'

class MyService {
  private networkManager: NetworkManager

  constructor() {
    this.networkManager = NetworkManager.getInstance({
      baseURL: apiConfig.getGraphqlUrl(),
      timeout: apiConfig.getTimeout(),
      headers: apiConfig.getHeaders()
    })
  }
}
```

## Environment Variables

The configuration system reads the following environment variables:

### API Configuration

- `TARO_APP_GRAPHQL_URL` - GraphQL endpoint URL
- `TARO_APP_API_BASE_URL` - REST API base URL
- `VITE_API_BASE_URL` - Alternative REST API base URL (for Vite builds)
- `TARO_APP_API_TIMEOUT` - Network request timeout in milliseconds

### Feature Flags

- `TARO_APP_ENABLE_MOCK_DATA` - Enable/disable mock data (true/false)
- `TARO_APP_ENABLE_API_LOGGING` - Enable/disable API logging (true/false)

### Environment

- `NODE_ENV` - Environment (development/production/test)

## Configuration Precedence

Configuration values are applied in the following order (highest to lowest precedence):

1. **Environment Variables** - Values from `.env` files or system environment
2. **Environment-Specific Config** - Overrides based on `NODE_ENV`
3. **Default Configuration** - Built-in default values

## Environment-Specific Settings

### Development
- Default GraphQL URL: `http://localhost:8080/graphql`
- Timeout: 10 seconds
- Mock data: Enabled
- API logging: Enabled

### Production
- Default GraphQL URL: (from environment variables)
- Timeout: 15 seconds
- Mock data: Disabled
- API logging: Disabled

### Test
- Default GraphQL URL: (from environment variables)
- Timeout: 5 seconds
- Mock data: Enabled
- API logging: Disabled

## Testing and Verification

Use the built-in verification utilities:

```typescript
import { verifyApiConfig, logConfigurationSummary } from '@/config/config-test'

// Verify configuration
const result = verifyApiConfig()
if (!result.success) {
  console.error('Configuration errors:', result.errors)
}

// Log configuration summary
logConfigurationSummary()
```

## Updating Services

When updating existing services to use the centralized configuration:

1. Import the configuration manager
2. Replace hardcoded values with configuration calls
3. Use the same NetworkManager initialization pattern

Example before:
```typescript
this.networkManager = NetworkManager.getInstance({
  baseURL: 'http://localhost:8080/graphql',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})
```

Example after:
```typescript
import apiConfig from '../config/api'

this.networkManager = NetworkManager.getInstance({
  baseURL: apiConfig.getGraphqlUrl(),
  timeout: apiConfig.getTimeout(),
  headers: apiConfig.getHeaders()
})
```

## Best Practices

1. **Always use the configuration manager** - Don't hardcode URLs or timeouts
2. **Test configuration in development** - The app automatically verifies configuration on startup
3. **Use environment variables** - Configure endpoints for different deployment environments
4. **Keep defaults sensible** - The defaults should work for most development scenarios
5. **Validate URLs** - The configuration system automatically validates URL formats

## Troubleshooting

### Configuration Not Loading

1. Check that environment variables are properly set in `.env` files
2. Verify the configuration file imports are correct
3. Check the browser/devtools console for configuration verification messages

### Services Not Working

1. Ensure services are importing and using `apiConfig`
2. Verify the NetworkManager initialization uses configuration values
3. Check that the GraphQL/REST endpoints are accessible

### Environment Issues

1. Verify `NODE_ENV` is set correctly
2. Check environment-specific overrides in the configuration file
3. Ensure `.env` files are loaded properly by the build system