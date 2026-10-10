# Address Management System

This document describes the comprehensive address management system implemented for AquaRush, including geolocation services, regional data management, and address validation.

## Recent Updates

### Schema Changes (v2.0)

- **Removed `postalCode` field**: Address schema no longer includes postal code
- **Added `receiverName` and `phone`**: Essential contact information now stored with addresses
- **Extended code field lengths**: Province/city/district codes now support up to 20 characters
- **Client-side mutations**: Added comprehensive CRUD operations for client use
- **Enhanced validation**: Stricter validation rules for address data

### GraphQL Type Generation

- **Automated generation**: Use `npx graphql-codegen --config graphql-codegen.yml` to regenerate types
- **Type consistency**: Ensures frontend and backend types stay synchronized
- **Multi-language support**: Generates both Kotlin (backend) and TypeScript (frontend) types

## Overview

The address management system provides:
1. **User Address Management**: CRUD operations for user addresses with validation
2. **Regional Data Support**: Hierarchical administrative divisions (Provinces, Cities, Districts)
3. **Geolocation Services**: Address-to-coordinate conversion and vice versa
4. **Address Validation**: Format standardization and duplicate detection
5. **Nearby Address Search**: Location-based address discovery

## Architecture

### Core Components

#### 1. Domain Models
- **Address**: User delivery address with geolocation support
- **Region**: Hierarchical administrative regions (3-level hierarchy)

#### 2. Services
- **AddressService**: Core address business logic and CRUD operations
- **GeolocationService**: Geographic coordinate handling and validation
- **ExternalGeocodingService**: External API integration (Google Maps)

#### 3. Repositories
- **AddressRepository**: Data access with geospatial queries
- **RegionRepository**: Regional data management

#### 4. GraphQL Resolvers
- **AddressQueryResolver**: Read operations and searches
- **AddressMutationResolver**: Write operations and transformations

## Database Schema

### Address Table
```sql
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    province_code VARCHAR(20),
    city VARCHAR(50) NOT NULL,
    city_code VARCHAR(20),
    district VARCHAR(50) NOT NULL,
    district_code VARCHAR(20),
    detail_address VARCHAR(500) NOT NULL,
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_address_user_id (user_id),
    INDEX idx_addresses_user_default (user_id, is_default),
    INDEX idx_addresses_location (longitude, latitude)
);
```

### Regions Table
```sql
CREATE TABLE regions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(12) NOT NULL UNIQUE,
    parent_code VARCHAR(12),
    level TINYINT NOT NULL COMMENT '1=Province, 2=City, 3=District',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_regions_parent_code (parent_code),
    INDEX idx_regions_level (level),
    INDEX idx_regions_code (code)
);
```

## GraphQL API

### Core Types

#### Address Type
```graphql
type Address {
  id: Long!
  userId: Long!
  receiverName: String!
  phone: String!
  province: String!
  provinceCode: String
  city: String!
  cityCode: String
  district: String!
  districtCode: String
  detailAddress: String!
  longitude: Float
  latitude: Float
  isDefault: Boolean!
  createdAt: LocalDateTime!
  updatedAt: LocalDateTime!
}
```

### Queries

#### Address Management
```graphql
# Get user's all addresses
userAddresses: [Address!]!

# Get user's default address
userDefaultAddress: Address

# Get address by ID
address(addressId: Long!): Address
```

#### Regional Data
```graphql
# Get regions by criteria
regions(level: Int, parentCode: String): [Region!]!

# Get specific region by code
region(code: String!): Region

# Search regions by name
searchRegions(keyword: String!, level: Int): [Region!]!

# Get region by coordinates
regionByCoordinates(
  longitude: BigDecimal!,
  latitude: BigDecimal!
): RegionCoordinatesPayload
```

#### Geolocation Services
```graphql
# Find nearby addresses
nearbyAddresses(
  longitude: BigDecimal!,
  latitude: BigDecimal!,
  radiusKm: Float
): NearbyAddressesPayload

# Calculate distance between coordinates
calculateDistance(
  lng1: BigDecimal!,
  lat1: BigDecimal!,
  lng2: BigDecimal!,
  lat2: BigDecimal!
): DistancePayload

# Check if coordinates are in service area
isInServiceArea(
  longitude: BigDecimal!,
  latitude: BigDecimal!
): ServiceAreaPayload
```

### Mutations

#### Address CRUD (Client-side)
```graphql
# Create new address
createAddress(input: AddressInput!): Address!

# Update existing address
updateUserAddress(
  id: Long!,
  input: UpdateAddressInput!
): Address!

# Delete address
deleteUserAddress(id: Long!): Boolean!

# Set default address
setDefaultUserAddress(id: Long!): Boolean!

# Copy address
copyUserAddress(
  id: Long!,
  newName: String!
): Address!

# Batch delete addresses
deleteUserAddresses(ids: [Long!]!): Boolean!
```

#### Geolocation Operations
```graphql
# Convert address to coordinates
geocodeAddress(address: GeocodeInput!): GeocodePayload!

# Convert coordinates to address
reverseGeocode(
  longitude: BigDecimal!,
  latitude: BigDecimal!
): ReverseGeocodePayload!

# Validate address
validateAddress(input: AddressValidationInput!): AddressValidationPayload!
```

## Input Types

### AddressInput (Client-side)
```graphql
input AddressInput {
  receiverName: String!
  phone: String!
  province: String!
  provinceCode: String!
  city: String!
  cityCode: String!
  district: String!
  districtCode: String!
  detailAddress: String!
  longitude: Float
  latitude: Float
  isDefault: Boolean! = false
}
```

### UpdateAddressInput
```graphql
input UpdateAddressInput {
  receiverName: String
  phone: String
  province: String
  provinceCode: String
  city: String
  cityCode: String
  district: String
  districtCode: String
  detailAddress: String
  longitude: Float
  latitude: Float
  isDefault: Boolean
}
```

### AddressValidationInput
```graphql
input AddressValidationInput {
  province: String!
  provinceCode: String
  city: String!
  cityCode: String
  district: String!
  districtCode: String
  detailAddress: String!
  longitude: BigDecimal
  latitude: BigDecimal
}
```

## Response Payloads

### AddressPayload
```graphql
type AddressPayload {
  success: Boolean!
  message: String!
  address: Address
}
```

### AddressValidationPayload
```graphql
type AddressValidationPayload {
  success: Boolean!
  message: String!
  isValid: Boolean!
  isDuplicate: Boolean!
  coordinatesValid: Boolean!
  normalizedAddress: NormalizedAddress
  errors: [String!]!
}
```

## Geolocation Features

### Geocoding (Address → Coordinates)
- **Input**: Full address components (province, city, district, detail)
- **Output**: Longitude and latitude coordinates
- **Service**: Google Maps API with fallback to mock service

### Reverse Geocoding (Coordinates → Address)
- **Input**: Longitude and latitude
- **Output**: Formatted address with region information
- **Service**: Google Maps API with administrative region mapping

### Distance Calculation
- **Algorithm**: Haversine formula for great-circle distance
- **Output**: Distance in kilometers with high precision

### Nearby Search
- **Radius**: Configurable (default 5km)
- **Limit**: Maximum 20 results
- **Sorting**: Distance-based ascending order

## Validation Rules

### Address Validation
1. **Required Fields**: Receiver name, phone, province, city, district, detail address
2. **Length Limits**:
   - Receiver name: 2-20 characters
   - Phone: Valid Chinese mobile number format
   - Province/City/District: Max 50 characters each
   - Detail address: Max 500 characters
3. **Coordinate Range**: Longitude (-180 to 180), Latitude (-90 to 90)
4. **Administrative Code**: Validates against regions table (if provided)
5. **Hierarchy**: Validates parent-child relationships in regions
6. **Duplicate Detection**: Prevents identical addresses per user
7. **Address Limits**: Maximum 20 addresses per user

### Regional Data Validation
1. **Code Format**: Standard administrative division codes
2. **Level Hierarchy**: Enforces 3-level administrative structure
3. **Parent Relations**: Validates parent-child relationships

## Caching Strategy

### Cache Keys
- `user_addresses:{userId}`: User's address list
- `user_default_address:{userId}`: User's default address
- `address:{addressId}`: Individual address details
- `region_by_coords:{lng}_{lat}`: Region lookup by coordinates

### Cache Invalidation
- **Create/Update/Delete**: Clear user-related cache entries
- **Default Address Change**: Clear default address cache
- **Region Changes**: Clear coordinate-based region cache

## Performance Optimizations

### Database Indexes
1. **User-based**: Fast address retrieval by user
2. **Default Address**: Quick default address lookup
3. **Geospatial**: Efficient nearby address queries
4. **Regional**: Fast region hierarchy traversal

### Query Optimization
1. **Batch Operations**: Process multiple addresses efficiently
2. **Lazy Loading**: Load addresses only when needed
3. **Result Limiting**: Prevent excessive data transfer

## Security Considerations

### Access Control
1. **User Isolation**: Users can only access their own addresses
2. **Ownership Validation**: Strict user ID verification
3. **Permission Checking**: Role-based operation access

### Data Privacy
1. **Address Encryption**: Sensitive address data protection
2. **Coordinate Precision**: Limited coordinate precision for privacy
3. **Audit Logging**: Track address modifications

## Integration Points

### External Services
1. **Google Maps API**: Geocoding and reverse geocoding
2. **Administrative Data**: Official regional division codes

### Internal Services
1. **User Service**: User authentication and context
2. **Order Service**: Address selection for orders
3. **Delivery Service**: Address-based delivery routing

## GraphQL Schema Generation

The GraphQL types are generated from schema files to ensure consistency between frontend and backend:

```bash
# Generate both Kotlin and TypeScript types
npx graphql-codegen --config graphql-codegen.yml
```

### Schema Files Location

- **Shared Types**: `shared-config/graphql/shared-types.graphqls`
- **Admin Schema**: `services/aqua-admin/src/main/resources/graphql/admin-schema.graphqls`
- **Client Schema**: `services/aqua-client/src/main/resources/graphql/client-schema.graphqls`

### Generated Files

- **Backend Types**: `modules/aqua-foundation/src/main/graphql-gen/schema.kt`
- **Frontend Types**: `frontend/common/src/types/graphql.ts`

## Configuration

### Google Maps Integration
```yaml
geocoding:
  google:
    api-key: ${GOOGLE_MAPS_API_KEY}
    base-url: https://maps.googleapis.com/maps/api
```

### Database Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

## Testing

### Unit Tests
- Address CRUD operations
- Geocoding accuracy
- Validation rules
- Regional data integrity

### Integration Tests
- Google Maps API integration
- Database performance
- Cache behavior
- Transaction management

### Performance Tests
- Bulk address operations
- Nearby address queries
- Concurrent user access

## Monitoring and Logging

### Key Metrics
- Address creation/update/delete rates
- Geocoding success rates
- Query response times
- Cache hit ratios

### Logging Strategy
- Structured logging with correlation IDs
- Performance metric tracking
- Error classification and alerting
- Audit trail for address changes

## Future Enhancements

### Planned Features
1. **Smart Address Suggestions**: AI-powered address completion
2. **Delivery Zone Management**: Dynamic delivery area definitions
3. **Address Analytics**: Usage patterns and optimization
4. **Multi-language Support**: Address localization

### Scalability Improvements
1. **Read Replicas**: Separate read/write database instances
2. **Microservices**: Independent address service deployment
3. **Event Sourcing**: Address change event tracking
4. **GraphQL Federation**: Distributed address data access

## Troubleshooting

### Common Issues
1. **Geocoding Failures**: Check API keys and rate limits
2. **Performance Issues**: Verify database indexes and cache hit rates
3. **Validation Errors**: Review input format and regional data
4. **Permission Errors**: Verify user authentication and ownership

### Debug Tools
1. **GraphQL Playground**: Test queries and mutations
2. **Database Queries**: Validate data integrity
3. **Cache Inspection**: Monitor cache effectiveness
4. **API Monitoring**: Track external service health
