# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Version Requirements

**IMPORTANT**: This project uses Spring Boot 4.0.0 and requires this exact version. When resolving dependency conflicts, never downgrade Spring Boot versions as this will break the application. The project's BOM and all dependencies are configured specifically for Spring Boot 4.0.0.

## Project Overview

AquaRush is a WeChat Mini Program-based bottled water ordering and delivery management system built with a Kotlin Spring Boot 4.0.0 backend and React-based frontends. The project uses a multi-module architecture with domain-driven design principles.

## Build & Development Commands

### Backend Development

```bash
# Build all modules
./gradlew build

# Run the main application (development)
./gradlew :services:aqua-admin:bootRun

# Alternative: Run client application
./gradlew :services:aqua-client:bootRun

# Run tests
./gradlew test

# Run specific module tests
./gradlew :modules:aqua-common:test
./gradlew :modules:aqua-order:test

# Update database schema (Liquibase)
./gradlew :services:aqua-admin:update

# Ktlint code formatting
./gradlew ktlintFormat

# Ktlint code checking
./gradlew ktlintCheck

# Git commit with Angular format (when prompted "生成commit msg")
git commit -m "$(cat <<'EOF'
feat: type commit message here following Angular format

Detailed description of changes made...
EOF
)"

# Generate commit message interactively
./scripts/generate-commit.sh
```

### Frontend Development

```bash
# User WeChat Mini Program (Taro)
cd frontend/user-weapp
npm install
npm run dev:weapp          # Development with WeChat Studio
npm run build:weapp        # Production build for WeChat Mini Program

# Delivery Mini Program (Taro)
cd frontend/delivery-client
npm install --legacy-peer-deps
npm run dev

# Admin Dashboard (React + Vite)
cd frontend/admin-client
npm install
npm run dev  # Runs on http://localhost:5173

# Additional WeChat Mini Program build commands
cd frontend/user-weapp
npm run build:h5           # H5 web build
npm run dev:h5             # H5 development build
```

## Architecture Overview

### Backend Module Structure

The project follows a clean multi-module architecture where each module has a specific domain responsibility:

- **aqua-common**: Shared utilities, caching system (MapDB), GraphQL configuration, messaging infrastructure
- **aqua-api**: API interfaces and DTOs for external communication
- **aqua-logging**: Structured logging with correlation IDs and user action tracking
- **aqua-user**: User management, authentication (JWT), addresses, WeChat integration
- **aqua-product**: Product catalog and inventory management
- **aqua-order**: Order processing, domain events, and business logic
- **aqua-delivery**: Delivery worker management and task assignment
- **aqua-payment**: WeChat Pay integration and refund processing
- **aqua-analytics**: Business analytics — financial reconciliation with external systems (e.g. WeChat Pay) and business statistics (revenue, delivery workers, products, users)
- **aqua-storage**: File storage service for product images and delivery photos
- **aqua-notice**: WeChat notification system and template management
- **aqua-review**: User reviews and delivery worker ratings
- **aqua-entry**: Main Spring Boot application entry point with global configurations

### Key Design Patterns

**Module Dependency Rule**:
- **IMPORTANT**: Domain modules (aqua-user, aqua-product, aqua-order, etc.) **MUST NOT** directly call Repository interfaces from other modules
- All cross-module data access must go through **aqua-api** services
- Each domain module should only expose its functionality via Service interfaces in aqua-api
- This ensures clean module boundaries and proper encapsulation

**Event-Driven Architecture with Artemis MQ**:
- Domain events are published via ActiveMQ Artemis
- Embedded Artemis broker for simplified deployment
- Message persistence for reliability
- Configurable retry mechanism with exponential backoff

**Caching System**:
- Spring Cache + MapDB integration for high-performance local caching
- Namespace-based cache isolation with configurable TTL
- Full compatibility with Spring Cache annotations (@Cacheable, @CacheEvict, @CachePut)

### Database Configuration

- **Primary Database**: PostgreSQL (configurable to MySQL)
- **Migration**: Liquibase with XML-based changelogs in `modules/aqua-entry/src/main/resources/db/changelog/`
- **Schema Management**: Automatic on startup, manual via `./gradlew :modules:aqua-entry:update`
- **ORM**: Spring Data JPA with Hibernate

### Key Database Tables

- `users`, `addresses`: User management and delivery addresses
- `products`: Product catalog with inventory tracking
- `orders`, `order_items`: Order management and line items
- `delivery_workers`, `delivery_areas`: Delivery workforce management
- `domain_events`: Domain events for event-driven architecture
- `payments`, `payment_refunds`: Financial transactions
- `reviews`, `delivery_worker_statistics`: Quality and performance tracking

### Primary Key Strategy

**IMPORTANT**: All database entities use `Long` type primary keys generated by Snowflake algorithm for distributed ID generation. This ensures:
- Unique IDs across all tables and environments
- Sortable by generation time
- High performance for distributed systems
- Avoids UUID performance overhead and storage costs

### Monetary Values Strategy

**IMPORTANT**: All monetary values (amounts, prices, totals) are stored as `Long` type representing cents/分. This approach:

- Avoids floating-point precision issues with financial calculations
- Provides exact arithmetic operations for money
- Stores values in the smallest currency unit (1 = ¥0.01)
- Converts to decimal display format only in presentation layer
- Ensures consistency across all financial-related tables

### GraphQL Schema Configuration

**IMPORTANT**: GraphQL schema files are located in `graphql-schema/schema.graphqls`. The project uses:
- `Long` types for all entity IDs (not UUID)
- Generated GraphQL types in `modules/aqua-common/src/main/graphql-gen/schema.kt`
- Schema consistency between GraphQL schema and resolver implementations
- Spring Boot auto-configuration for GraphQL endpoints at `/graphql`

**Type Naming Conventions**:
- Input types: `XxxxInput` (e.g., `CreateOrderInput`, `UpdateProductInput`)
- Response types (root): `XxxxResponse` (e.g., `OrderResponse`, `ProductListResponse`)
- pagination type: `XxxVoPage`(e.g., `ProductVoPage`, `OrderVoPage`)
- Other types: `XxxxVO` (e.g., `OrderVO`, `ProductVO`)

### Configuration Management

Primary configuration in `modules/aqua-entry/src/main/resources/application.yml`:

- Database connection with HikariCP pooling
- WeChat Mini Program integration (app-id, app-secret)
- WeChat Pay v3 configuration with certificate management
- JWT authentication settings
- Messaging system configuration (Artemis/Memory/Outbox strategies)
- GraphQL endpoint configuration
- Jackson serialization settings

### Security Architecture

- **Authentication**: JWT tokens with configurable expiration
- **Authorization**: Role-based access control (USER, ADMIN, DELIVERY_WORKER)
- **WeChat Integration**: code2session flow for Mini Program login
- **Payment Security**: WeChat Pay v3 signature verification and certificate handling

## Frontend Architecture

### WeChat Mini Programs (Remax + React)

**User Mini Program**: Product browsing, order placement, payment, address management, order history
**Delivery Mini Program**: Task management, navigation, photo verification, status management

Both use:
- Remax framework for cross-platform Mini Program development
- TypeScript for type safety
- Axios for API communication with interceptors
- Custom TabBar icons and navigation

### Admin Dashboard (React + Vite + Ant Design)

- Modern React 18 with TypeScript
- Ant Design 5 component library
- Vite for fast development and building
- Role-based dashboard with different views for admin users
- Real-time order management and analytics

## Commit Message Generation

This project includes automated commit message generation tools to maintain consistent commit history following Angular convention.

### Available Tools

1. **Python Scripts (Default)**: `./scripts/*.py`
   - **Default choice for all automation tasks**
   - Use `./scripts/analyze_code.py` for code analysis and pattern detection
   - Use `./scripts/check_models.py` to verify model consistency
   - Use `./scripts/generate_commit.py` for automated commit messages
   - Python scripts provide better error handling and cross-platform compatibility

2. **Interactive Script**: `./scripts/generate-commit.sh`
   - Analyzes staged changes automatically
   - Suggests appropriate commit type and scope
   - Validates Angular commit format
   - Includes proper attribution
   - Use when Python scripts are not available

3. **Slash Command**: `/commit` (when available)
   - Generates commit messages based on changes
   - Follows project-specific patterns
   - Includes co-authorship information

### Commit Types Used

- `feat`: New features and functionality
- `fix`: Bug fixes and error corrections
- `docs`: Documentation and README changes
- `style`: Code formatting without logic changes
- `refactor`: Code restructuring without feature changes
- `test`: Adding or modifying tests
- `chore`: Build, dependency, or configuration changes

### Common Scopes

- `user`: User management and authentication modules
- `order`: Order processing and management
- `delivery`: Delivery worker and task management
- `product`: Product catalog and inventory
- `payment`: Payment processing and WeChat Pay integration
- `common`: Shared utilities and infrastructure
- `admin`: Admin dashboard functionality
- `schema`: Database schema and migrations
- `frontend`: Client-side applications (Mini Programs, Admin Dashboard)

### Usage Example

```bash
# Stage your changes
git add modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/service/AuthService.kt

# Preferred: Use Python script for commit generation
./scripts/generate_commit.py

# Alternative: Use shell script
./scripts/generate-commit.sh

# Or use the traditional way with template
git commit -m "$(cat <<'EOF'
feat(user): implement JWT authentication with WeChat integration

Add JWT token generation and validation with WeChat Mini Program
OAuth2 flow for secure user authentication.
EOF
)"
```

### Script Preference Guidelines

**Always prefer Python scripts over shell scripts when available:**

- **Python scripts** (`*.py`) are the default choice for:
  - Code analysis and pattern detection
  - Model consistency checks
  - Automated commit message generation
  - File structure validation
  - Cross-platform compatibility

- **Shell scripts** (`*.sh`) are used for:
  - Simple build and deployment tasks
  - System service management
  - When Python is not available in the environment

## Automation and Scripting

**IMPORTANT**: This project prioritizes Python scripts for all automation and analysis tasks. Python scripts provide better error handling, cross-platform compatibility, and more maintainable code.

### Python Script Standards

- **Default choice**: Always use Python scripts (`*.py`) over shell scripts (`*.sh`) when both are available
- **Location**: All Python scripts are located in `./scripts/` directory
- **Python version**: Compatible with Python 3.8+
- **Dependencies**: Use minimal external dependencies, prefer standard library
- **Error handling**: Include proper exception handling and user-friendly error messages
- **Documentation**: Include docstrings and usage examples

### Common Python Scripts

1. **Code Analysis**: `./scripts/analyze_code.py`
   - Analyzes code patterns and structure
   - Detects inconsistencies and missing implementations
   - Generates reports on code quality

2. **Model Validation**: `./scripts/check_models.py`
   - Validates entity model consistency
   - Checks for missing required fields and annotations
   - Ensures proper implementation of interfaces

3. **Commit Generation**: `./scripts/generate_commit.py`
   - Automatically generates Angular-style commit messages
   - Analyzes git changes to suggest appropriate commit types
   - Includes co-authorship and attribution information

4. **File Structure**: `./scripts/validate_structure.py`
   - Validates project structure and organization
   - Checks for missing required files and directories
   - Ensures consistency across modules

### When to Use Shell Scripts

Shell scripts are still used for specific scenarios:

- System service management (systemctl, service commands)
- Simple build and deployment pipelines
- Environment setup and configuration
- When Python dependencies are not available

## Development Guidelines

### Module Dependencies

Modules are structured with clear dependency hierarchy:
- Domain modules (user, product, order, etc.) depend on aqua-common
- aqua-entry depends on all domain modules
- Avoid circular dependencies between business modules

### GraphQL Resolver Development

This project uses Spring GraphQL with annotation-based resolvers. All resolvers should follow these patterns.

**Resolver Structure**:

```kotlin
@Controller("clientOrderQueryResolver")  // Spring GraphQL controller
class OrderQueryResolver(
  private val orderQueryService: OrderQueryService,
) {
  // Query methods
}
```

**Available Annotations**:

| Annotation | Purpose |
|------------|---------|
| `@QueryMapping` | Marks a method as GraphQL query (read operation) |
| `@MutationMapping` | Marks a method as GraphQL mutation (write operation) |
| `@Argument` | Injects GraphQL input argument |
| `@AuthenticationPrincipal` | Injects authenticated user (UserPrincipal) |
| `@PreAuthorize` | Method-level security (e.g., `isAuthenticated()`) |
| `@Valid` | Enables Jakarta validation on input DTOs |

**Query Method Example**:

```kotlin
@QueryMapping
@PreAuthorize("isAuthenticated()")
fun myOrders(
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): List<OrderModel> = orderQueryService.findOrdersByUserId(userPrincipal.id)

@QueryMapping
fun order(
  @Argument orderId: Long,
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): OrderModel? = orderQueryService.findOrderByIdAndUserId(orderId, userPrincipal.id)
```

**Mutation Method Example**:

```kotlin
@MutationMapping
@PreAuthorize("isAuthenticated()")
fun createOrder(
  @Argument @Valid input: CreateOrderInput,
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): OrderModel {
  val request = CreateOrderInputMapper.map(input).copy(userId = userPrincipal.id)
  return orderMutationService.createOrder(request)
}
```

**Working with Input DTOs**:

```kotlin
// Simple argument
@Argument orderId: Long

// With default value
@Argument id: Long = 0

// Nullable argument
@Argument status: OrderStatus?

// With validation
@Argument @Valid input: CreateProductInput
```

**UserPrincipal Usage**:

The `UserPrincipal` class provides authenticated user information:

```kotlin
data class UserPrincipal(
  val id: Long,
  private val _username: String,
  val userType: String,  // USER, WORKER, ADMIN
  private val _authorities: Collection<GrantedAuthority>,
) : UserDetails {
  // Methods
  fun hasRole(role: String): Boolean
  fun hasAuthority(authority: String): Boolean
}
```

Common usage patterns:
- `userPrincipal.id` - Get current user ID
- `userPrincipal.userType` - Get user type (USER/ADMIN/WORKER)
- `userPrincipal.hasRole("ADMIN")` - Check role

**BaseGraphQLResolver**:

Extend `BaseGraphQLResolver` for common validation logic:

```kotlin
@Controller
class MyResolver(
  private val service: MyService,
) : BaseGraphQLResolver() {

  @QueryMapping
  fun myQuery(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Result {
    // Use helper methods
    checkPermission(userPrincipal, "myQuery")
    return service.doSomething(userPrincipal.id)
  }
}
```

**Common Patterns**:

1. **Client vs Admin Resolvers**: Use `@ClientService` for client-facing APIs, `@AdminService` for admin APIs
2. **Naming Convention**: Resolver name should match GraphQL schema type name
3. **Error Handling**: Throw `IllegalArgumentException` for not found, handle authorization via `@PreAuthorize`
4. **Validation**: Use `@Valid` with input DTOs for automatic validation

**File Location**: Resolvers are placed in each module's `resolvers/` directory:
- `modules/aqua-order/src/main/kotlin/dev/yidafu/aqua/client/order/resolvers/`
- `modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/admin/user/resolvers/`

### Object Mapping with Mappie

This project uses [Mappie](https://mappie.tech/) for type-safe object-to-object mapping between DTOs, domain models, and API representations. Mappie is a Kotlin compiler plugin that generates mapper code at compile-time (no reflection at runtime).

**Why Mappie**:
- Compile-time code generation for better performance
- Type-safe mappings with compile-time verification
- Kotlin-first design with native Kotlin feature support
- No runtime dependencies

**Installation**:

Add to `build.gradle.kts`:

```kt
plugins {
    id("tech.mappie.plugin") version("版本号")
}
```

When using mappie version below 1.0.0 or when you want to add the mappie-api dependency manually:

```kt
dependencies {
    implementation("tech.mappie:mappie-api:版本号")
}
```

**Configuration**:

Mappie can be configured via Gradle or per Mapper:

```kt
mappie {
    useDefaultArguments = false // Disable using default arguments in implicit mappings
    strictness {
        enums = false // Do not report an error if not all enum sources are mapped
        platformTypeNullability = true // Enable strict nullability checks for platform types
        visibility = true // Allow calling constructors not visible from the calling scope
    }
    reporting {
        enabled = true // Enable report generation
    }
}
```

Local configuration can be applied via annotations on the mapper class, overriding global settings:

| Gradle Option | Annotation | Default |
| --------------- | ------------ | --------- |
| useDefaultArguments | @UseDefaultArguments | true |
| strictness.enums | @UseStrictEnums | true |
| strictness.platformTypeNullability | @UseStrictPlatformTypeNullabilityValidation | true |
| strictness.visibility | @UseStrictVisibility | false |

**Basic Usage**:


**Key Features**:
- Automatic field mapping by name
- Enum mapping support
- Custom mapping functions
- Constructor-based mapping

**Enum Mapping**:

Mappie supports mapping enum classes by extending from `EnumMappie`. If both source and target are enum classes with identical entries, Mappie resolves names automatically:

```kotlin
enum class Color { RED, GREEN, BLUE }
enum class Colour { RED, GREEN, BLUE }

// Simple enum mapper - automatic mapping by name
object ColorMapper : EnumMappie<Color, Colour>()
```

For enums with different entries, use `fromEnumEntry` to explicitly map source entries to target:

```kotlin
enum class Color { RED, GREEN, BLUE, ORANGE }
enum class Colour { RED, GREEN, BLUE, OTHER }

// Explicit enum mapping with fromEnumEntry
object ColorMapper : EnumMappie<Color, Colour>() {
    override fun map(from: Color): Colour = mapping {
        Colour.OTHER fromEnumEntry Color.ORANGE
    }
}
```

**Inferring Implicit Mappings**:

Mappie infers implicit mappings by name, type, default arguments, getter- and setter methods, and other mappers that are defined. An implicit mapping for a target property is inferred automatically if it has the same name as a source property, and it is assignable from that source property. If it is not assignable, Mappie will check if there is a single mapper defined that can map the source type to the target type, and will automatically apply it. Mappie comes with several mappers out of the box. See Built-in Mappers.

For example, suppose we have a data class Person and a data class PersonDto:

```kotlin
data class Person(val name: String, val age: Int)

data class PersonDto(val name: String, val age: Int)
```

The properties of Person match the parameters of the primary constructor of PersonDto, and as such, no explicit mappings have to be defined. We can simply construct such a mapper by writing:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>()
```

which will generate a mapper which calls the primary constructor of PersonDto assigned to the fields of Person.

**Mapper Generation**:

Mappie can also generate mappers automatically. When a source type and a target type do not have an existing mapper, and one can be written without any explicit mappings, it will be generated automatically.

For example, suppose we have the data classes Person and PersonDto containing Gender and GenderDto enum classes:

```kotlin
data class Person(val name: String, val gender: Gender)
enum class Gender { MALE, FEMALE, OTHER }

data class PersonDto(val name: String, val gender: GenderDto)
enum class GenderDto { MALE, FEMALE, OTHER }
```

We can generate a mapper from Person to PersonDto by writing:

```kotlin
class PersonMapper : ObjectMappie<Person, PersonDto>()
```

and the nested mapper from Gender to GenderDto will be generated automatically as they both contain the same enum entries.

**Default Arguments**:

Mappie also considers default arguments as a possibility.

For example, suppose PersonDto is defined as:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    val hasChildren: Boolean = false,
)
```

Mappie will use the default argument `false` for `hasChildren` if no explicit mapping is defined. This is enabled by default and can be disabled by setting the configuration option `useDefaultArguments` to false.

**Constructing Explicit Mappings**:

Not all classes one wants to map are equivalent. Mappie supports defining explicit mappings for those which cannot be resolved automatically. This can be done via properties, values, or expressions as described in the coming sections.

Suppose we have a data class Person, and we have the data class PersonDto which has the property description which is not defined in Person:

```kotlin
data class Person(
    val name: String,
    val age: Int,
)

data class PersonDto(
    val name: String,
    val age: Int,
    val description: String,
)
```

If one would define a mapper without an explicit mapping for description, Mappie will give a compile-time error stating that the target description has no source defined. The target property can be assigned in different ways:

- mapping via a source property
- mapping via a value; or
- mapping via an expression

**Mapping via a Source Property**:

Targets can be set via the operator `fromProperty`. This will set the target to the given source property.

For example, the following snippet will construct a mapper where PersonDto.description is set to Person.name:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromProperty from::name
    }
}
```

The target type is not always assignable from the source type. There are several ways to handle this. One way is to define a mapper from the source type to the target type. This can be applied explicitly using The Via Operator, or be implicitly applied by Mappie.

It is also possible to transform the property. For example to tweak the value, handle nullability, or transform the source in some other way. See The Transform Operator for some guidelines.

**Nullability**:

When mapping from a nullable type to a non-nullable type, one has several options. The most flexible option is to use the transform operator.

When the transformation logic is applying a simple non-null assertion operator, or a requireNotNull function call, `to::x fromPropertyNotNull from::y` steps in as an equivalent alternative to:

```kotlin
to::x fromProperty from::y transform { it!! }
```

**Mapping via a Value**:

Targets can be set via the operator `fromValue`. This will set the target to the given value.

For example, the following snippet will construct a mapper where PersonDto.description is set to "unknown":

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromValue "unknown"
    }
}
```

**Mapping via an Expression**:

Targets can be set via the operator `fromExpression`. This will set the target to the given lambda result.

The difference between fromExpression and fromValue is that fromExpression will take a lambda function as a parameter, which takes the original source as a parameter. Allowing for more flexibility.

For example, the following snippet will construct a mapper where PersonDto.description is set to "Description: ${from.name}":

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromExpression { from ->
            "Description: ${from.name}"
        }
    }
}
```

All mappings can be defined using fromExpression, but to keep the mappings clean and give Mappie the most information to suggest improvements to your code, fromProperty combined with either via or transform is preferred.

**Handling non-referenceable Targets**:

We can use the `to` function to refer to constructor parameters which do not have a property or to refer to a setter method.

For example, suppose that we use the same example as above, but PersonDto.description does not declare a backing property:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    description: String,
)
```

We cannot reference description via a property reference Person::description. To target the constructor parameter, we can use `to("description")` to reference the constructor parameter:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        to("description") fromValue "a constant"
    }
}
```

**Using a Specific Constructor**:

We can force Mappie to select a specific constructor using the different overloads of mapping. We can force a specific constructor by passing the types of the constructor parameters as type arguments to the call of mapping and passing a constructor reference. For example, suppose PersonDto is defined as:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    val description: String,
) {
    constructor(name: String, age: Int) : this(name, age, "description")
}
```

we can reference the primary constructor via:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person) =
        mapping<String, String, Int>(::PersonDto)
}
```

and we can reference the secondary constructor via:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person) =
        mapping<String, Int>(::PersonDto)
}
```

**The to Alias**:

We can access the target properties via the target type of the mapper. This can clutter the mapping definition when many explicit mappings are defined. Mappie defines a special `to` property which can be used instead of the target type.

For example, we can use to refer to the property streetname of PersonDto:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        to::streetname fromProperty from.address::street
    }
}
```

where `to::streetname` is equivalent to `PersonDto::streetname`.

**When to Use**:
- DTO to Domain model conversion
- API response to internal model transformation
- Entity toVO/VO to Entity conversions
- Any object structure mapping between layers

See [Mappie Documentation](https://mappie.tech/) for more details.

### Event Processing

When adding new domain events:
1. Define event in the appropriate domain module
2. Publish event via SimplifiedEventPublishService using Artemis MQ
3. Create event handler to consume from Artemis queue
4. Add retry logic for idempotent processing

### Cache Usage

Leverage the built-in caching system:
```kt
@Cacheable(value = ["orders"], key = "#orderId")
fun getOrderById(orderId: UUID): Order? {
    return orderRepository.findById(orderId).orElse(null)
}
```

Use namespaces for cache isolation and configure appropriate TTL based on data volatility.

### WeChat Integration Notes

- WeChat Mini Program login requires proper app-id and app-secret configuration
- WeChat Pay v3 needs merchant private key and platform certificate
- Template messages require pre-approved template IDs from WeChat
- All WeChat API calls should include proper error handling and retry logic

### React/TypeScript Development Requirements

**IMPORTANT**: All React components in frontend projects must use functional components with TypeScript annotations, not class components. This ensures:

- Better type safety and developer experience
- Compatibility with modern React patterns (hooks, suspense, etc.)
- Improved code maintainability and testing
- Consistency across all frontend applications

**Functional Component Pattern**:

```typescript
import React, { useState, useEffect } from 'react'

interface Props {
  title: string
  onSubmit: (data: FormData) => void
}

const MyComponent: React.FC<Props> = ({ title, onSubmit }) => {
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // Component logic
  }, [])

  const handleSubmit = (data: FormData) => {
    setLoading(true)
    onSubmit(data)
  }

  return (
    <div>
      <h1>{title}</h1>
      {/* Component JSX */}
    </div>
  )
}

export default MyComponent
```

**Taro Page Component Pattern**:

```typescript
import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import {
  useReady,
  useDidShow,
  useDidHide,
  usePullDownRefresh,
  useReachBottom,
  usePageScroll,
  useShareAppMessage,
  useShareTimeline,
  useAddToFavorites,
  useTitleClick,
  useOptionMenuClick,
  useResize,
  useTabItemTap,
  useBeforeLeave,
  useSaveExitState,
  useRestoreExitState
} from '@tarojs/taro'

interface Props {
  // Page props (rarely used in Taro pages)
}

const MyPage: React.FC<Props> = () => {
  const [data, setData] = useState<any>(null)
  const [loading, setLoading] = useState<boolean>(false)

  // Standard React hooks
  useEffect(() => {
    // Component mounted logic
    console.log('Component mounted')

    return () => {
      // Cleanup logic
      console.log('Component unmounted')
    }
  }, [])

  // Taro page lifecycle hooks
  useReady(() => {
    // 对应类组件的 componentDidMount 和 componentDidReady
    console.log('Page ready')
    // 页面准备就绪时的逻辑
  })

  useDidShow(() => {
    // 对应类组件的 componentDidShow
    console.log('Page shown')
    // 页面显示时的逻辑（每次页面显示都会调用）
    loadData()
  })

  useDidHide(() => {
    // 对应类组件的 componentDidHide
    console.log('Page hidden')
    // 页面隐藏时的逻辑
  })

  usePullDownRefresh(() => {
    // 对应类组件的 onPullDownRefresh
    console.log('Pull down refresh')
    handleRefresh()
  })

  useReachBottom(() => {
    // 对应类组件的 onReachBottom
    console.log('Reach bottom')
    loadMore()
  })

  usePageScroll((event) => {
    // 对应类组件的 onPageScroll
    console.log('Page scroll:', event.scrollTop)
  })

  useShareAppMessage((res) => {
    // 对应类组件的 onShareAppMessage
    return {
      title: '分享标题',
      path: '/pages/index/index'
    }
  })

  useShareTimeline(() => {
    // 对应类组件的 onShareTimeline
    return {
      title: '朋友圈分享标题'
    }
  })

  // 自定义方法
  const loadData = async () => {
    try {
      setLoading(true)
      // 加载数据逻辑
      const result = await Taro.request({
        url: 'your-api-url',
        method: 'GET'
      })
      setData(result.data)
    } catch (error) {
      console.error('Load data failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    try {
      await loadData()
      Taro.stopPullDownRefresh()
    } catch (error) {
      console.error('Refresh failed:', error)
      Taro.stopPullDownRefresh()
    }
  }

  const loadMore = async () => {
    // 加载更多逻辑
    console.log('Loading more data...')
  }

  return (
    <View className='page'>
      {loading ? (
        <Text>加载中...</Text>
      ) : (
        <View>
          {/* 页面内容 */}
          <Text>Page Content</Text>
        </View>
      )}
    </View>
  )
}

export default MyPage
```

**TypeScript Requirements**:

- All components must have explicit `Props` interfaces
- Use `React.FC<Props>` type for functional components
- All state variables should be typed explicitly
- All functions must have parameter and return type annotations
- Use proper TypeScript event handling (e.g., `React.MouseEvent`, `React.ChangeEvent`)

### UI Design and Layout Guidelines

**IMPORTANT**: All UI components should be designed for 1x scale (实际像素尺寸) to ensure proper display and usability on devices. This ensures:

- Better readability and accessibility
- Larger touch targets for mobile users
- Improved user experience on smaller screens
- Consistent visual hierarchy across all pages

**Page Layout Standards**:

```scss
// 页面容器
.page {
  min-height: 100vh;
  padding: 60px 32px 32px; // 1x 倍尺寸，充足的间距
  box-sizing: border-box;
}

// 标题样式
.title {
  font-size: 40px; // 大标题使用 40px
  font-weight: bold;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 20px; // 副标题使用 20px
  margin-bottom: 12px;
}

// 表单元素
.form-input {
  height: 64px; // 输入框高度 64px
  font-size: 20px; // 输入文字大小 20px
  padding: 16px 20px;
  border-radius: 12px;
}

.form-textarea {
  min-height: 180px; // 文本域最小高度 180px
  font-size: 20px; // 文本大小 20px
  padding: 20px;
  line-height: 1.6;
}

// 按钮样式
.primary-button {
  height: 64px; // 按钮高度 64px
  font-size: 20px; // 按钮文字大小 20px
  border-radius: 16px;
  font-weight: 600;
}

// 卡片和容器
.card {
  padding: 32px; // 卡片内边距 32px
  border-radius: 20px;
  margin-bottom: 32px;
}

// 图片上传区域
.image-upload-item {
  width: 140px; // 图片预览尺寸 140px x 140px
  height: 140px;
  border-radius: 12px;
}

// 选择器按钮
.selector-option {
  padding: 12px 24px;
  border-radius: 28px;
  font-size: 16px;
  min-height: 48px;
}
```

**Touch Target Guidelines**:

- **Minimum touch target**: 48px × 48px (符合无障碍标准)
- **Recommended button height**: 64px for primary actions
- **Input field height**: 64px for better touch accuracy
- **Icon buttons**: Minimum 32px × 32px with adequate padding

**Typography Scale**:

- **Page titles**: 40px, bold
- **Section headers**: 24px, semibold
- **Body text**: 16px, regular
- **Small text**: 14px, regular
- **Form labels**: 20px, semibold

**Spacing System**:

- **Page margins**: 32px (left/right), 60px (top)
- **Section spacing**: 32px - 48px
- **Element spacing**: 16px - 24px
- **Component padding**: 20px - 32px

**Accessibility Considerations**:

- All interactive elements must meet WCAG AA contrast standards
- Focus states should be clearly visible with 4px minimum outline
- Text should be readable at 1x scale without requiring zoom
- Touch targets should have adequate spacing between them

### Testing Strategy

- Unit tests for repositories and services
- Integration tests for controllers and event processing
- Database migrations should be tested with different database versions
- WeChat integration should be tested with sandbox environments
- React components should have unit tests with React Testing Library

## Production Deployment

### Single-Server Deployment

The project is designed for single-server deployment:
- systemd service configuration in `scripts/aquarush.service`
- Database backup scripts in `scripts/backup.sh`
- Caddy or Nginx for reverse proxy with automatic HTTPS
- Built-in monitoring via Spring Boot Actuator endpoints

### Environment Variables

Key environment variables for production:
- `DB_USERNAME`, `DB_PASSWORD`: Database credentials
- `WECHAT_APP_ID`, `WECHAT_APP_SECRET`: WeChat Mini Program credentials
- `WECHAT_PAY_MCH_ID`, `WECHAT_PAY_API_V3_KEY`: WeChat Pay credentials
- `JWT_SECRET`: JWT signing secret
- `SNOWFLAKE_MACHINE_ID`: Unique ID for distributed ID generation

## License

This project is licensed under AGPL-3.0. Any modifications must be released under the same license, and users accessing the service over network have the right to obtain the source code.
