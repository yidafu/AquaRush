# Mock Data for AquaRush Admin Dashboard

This directory contains mock data and GraphQL resolvers for development and testing purposes.

## Overview

The mock system provides:
- **10 Sample Products**: Various beverage types (矿泉水, 纯净水, 茶饮料, etc.)
- **GraphQL Resolvers**: Full CRUD operations for product management
- **Apollo Link Integration**: Seamless mock data injection in development

## Features

### Supported Operations
- `GetProducts` - Retrieve all products with pagination
- `GetProductDetail` - Get single product by ID
- `CreateProduct` - Create new product with auto-generated ID
- `UpdateProduct` - Update existing product
- `DeleteProduct` - Remove product from mock data
- `UpdateProductInventory` - Update product stock levels

### Data Structure
All products include:
- Basic info: id, name, description, price (in cents)
- Inventory: stock, status (ONLINE/OFFLINE)
- Metadata: category, brand, specifications
- Timestamps: createdAt, updatedAt

## Usage

### Environment Configuration
Set environment variable to enable mock data:
```bash
VITE_USE_MOCK_DATA=true
```

### Mock Data Files
- `data/products.ts` - Product data and GraphQL resolvers
- `apolloMockLink.ts` - Apollo Client link for mock responses

### Integration
The mock system automatically integrates with Apollo Client when enabled:
```typescript
// In apollo-client.ts
createMockApolloLink() || httpLink
```

## Product Categories Available

1. **矿泉水** - 农夫山泉, 百岁山
2. **纯净水** - 怡宝, 娃哈哈
3. **茶饮料** - 东方树叶, 康师傅冰红茶
4. **功能饮料** - 红牛, 脉动
5. **苏打水** - 屈臣氏

## Testing

The mock system includes realistic data for testing:
- Prices in cent format (e.g., 200 = ¥2.00)
- Various stock levels and statuses
- Proper product specifications arrays
- Realistic timestamps and metadata

## Development Benefits

- **Offline Development**: No backend required
- **Consistent Data**: Same data structure across tests
- **Fast Performance**: 500ms simulated network delay
- **Type Safety**: Full TypeScript support
- **Easy Reset**: Data resets on application restart