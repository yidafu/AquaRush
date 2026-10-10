# Backend Architecture Reference

> Imported by the root `CLAUDE.md`. Backend module structure, conventions, key design patterns, database, GraphQL schema configuration, and security.

## Backend Module Structure

The project follows a clean multi-module architecture. Modules (`modules/`) hold domain logic, services (`services/`) are the deployable application entry points:

- **aqua-foundation**: Shared infrastructure — `common/` (caching MapDB, GraphQL config, messaging/Artemis, security, id generation, exceptions), `api/` (cross-module service interfaces and DTOs), `logging/` (structured logging with correlation IDs; API/user-action/business log models and mappers live here)
- **aqua-user**: User management, authentication (JWT), addresses, WeChat integration
- **aqua-product**: Product catalog and inventory management
- **aqua-trade**: Order processing, delivery worker management, payment (WeChat Pay), reviews — each as a sub-domain (`order/`, `delivery/`, `payment/`, `review/`)
- **aqua-analytics**: Reconciliation with external systems (e.g. WeChat Pay) and business statistics (revenue, delivery workers, products, users)
- **aqua-platform**: Storage (file upload/images), notice (WeChat notifications)
- **aqua-ops**: Operations configuration — banner, notice, feedback, store profile, delivery fee rule, system config parameters. Sub-domains: `banner/`, `notice/`, `feedback/`, `storeprofile/`, `deliveryfeerule/`, `systemconfig/`, plus `audit/` (config-change audit). Maps to `PRD/12-operations-config.md` and `PRD/11-system-settings.md`.
- **aqua-marketing**: Marketing domain — customer groups, group product pricing, coupons, promotion activities (满减/折扣), water tickets (桶水票), flash sale, buy-gift, first-order promotions, and a shared pricing engine. Also hosts the `barrel/` sub-domain (G06 水桶费/购桶/历史押金, merged from the former aqua-deposit module). Sub-domains: `coupon/`, `customer/`, `promotion/`, `ticket/`, `barrel/`, `flashsale/`, `gift/`, `firstorder/`, `pricing/`, plus `engine/` (`PromotionEngineImpl`, `CouponRule`, `TicketRule`, `GiftRule`, `UnitPriceRule`, ...). Maps to `PRD/10-marketing.md` (and `PRD/14-misc.md §G06`).

Services (deployable Spring Boot applications):

- **aqua-admin**: Admin dashboard backend — GraphQL resolvers under `admin/<domain>/resolvers/` (e.g. `order/`, `marketing/`, `ops/`, `logging/`), REST controllers, security
- **aqua-client**: Mini Program backend — client-facing resolvers under `client/<domain>/resolvers/` (e.g. `marketing/`, `ops/`) and controllers

## Module Directory Conventions

Every domain module follows the same package layout:

```text
dev/yidafu/aqua/<domain>/
├── config/           # module-local configuration
├── controller/       # REST controllers (only when needed)
├── domain/
│   ├── model/        # module-private models (+ enums/)
│   ├── repository/   # Exposed DSL repositories (+ Repository interface in same package)
│   └── exception/    # domain exceptions
├── dto/              # module-private DTOs
├── event/            # domain events and handlers
├── mapper/           # 手写扩展函数转换（toGraphQL/toDomain/toRequest，valueOf 枚举互转）
├── resolvers/        # GraphQL resolvers that are thin module adapters
└── service/
    └── impl/         # implementations (interfaces live in aqua-foundation api/service)
```

Services use `<admin|client>/{config, <domain>/{resolvers, controller, dto}}`.

**Core rules**:

1. **Domain models are centralized** in `modules/aqua-foundation/.../common/domain/model/` (plain Kotlin classes — JPA/Hibernate annotations were removed in the P5 migration). Exposed `Table` mappings live in `modules/aqua-foundation/.../common/domain/table/`. Domain modules keep only private non-model types.
2. **Cross-module DTOs live in `aqua-foundation` `api/dto`** (one class per file). Module-private DTOs stay in the module `dto/`; service-local DTOs in `<domain>/dto/`. Do not create `api/query` or `common/dto`.
3. **Resolver placement**: pure module adapters may live in the module (`analytics/reconciliation/resolvers`); resolvers that depend on service-layer DTOs/security live in the service `<domain>/resolvers/` (always plural).
4. **Resolver input types are never inlined** (no `data class XxxInput` in a resolver `companion object`) — service-local GraphQL input DTOs go in `<admin|client>/<domain>/dto/` (e.g. `admin/user/dto/AdminRequestDtos.kt`, mapped from the generated GraphQL input via a `fromInput(...)` factory); cross-module DTOs in `aqua-foundation` `api/dto`.

Prohibited: new `*ApiService` interface suffixes (use `*QueryService`/`*MutationService`); `util`/`utils` dual packages (use `util`); module-root `exception/` (use `domain/exception/`); `.imports` files outside `META-INF/spring/`.

> 其中 util/utils、exception 位置、api.query/common.dto、实体集中、resolvers 复数、Input 归位由 ArchUnit 强制执行：`services/aqua-admin` 测试 `dev.yidafu.aqua.architecture.DirectoryStructureTest`（规则 D1–D6）；`*ApiService` 新后缀与 `.imports` 位置等其余约定仍靠评审。

## Module Dependencies

Modules are structured with clear dependency hierarchy:
- All modules depend on aqua-foundation (common/api/logging)
- aqua-trade depends on aqua-product/aqua-user (compileOnly)
- aqua-ops and aqua-marketing depend only on aqua-foundation
- Both services (aqua-admin, aqua-client) depend on all modules
- Avoid circular dependencies between business modules

## Key Design Patterns

**Module Dependency Rule**:

- **IMPORTANT**: Domain modules **MUST NOT** directly call Repository interfaces from other modules（历史债务已于 2026-10 清理，P1 已强制）
- All cross-module data access must go through the service interfaces in `aqua-foundation` `api/service/**`
- Each domain module only exposes its functionality via Service interfaces in `api/service`
- This ensures clean module boundaries and proper encapsulation

> 本节的跨模块 repository 直连、ops/marketing 依赖隔离、foundation 反向依赖与模块循环依赖由 ArchUnit 强制执行：`services/aqua-admin` 测试 `dev.yidafu.aqua.architecture.ModuleDependencyTest`（规则 P1–P4）。正向约定（跨模块数据访问须经 `api/service` 等）与 admin/client 豁免范围仍靠评审（admin/client 直连模块 repository 属计划内豁免，services 非领域模块）。

**Event-Driven Architecture with Artemis MQ**:
- Domain events are published via ActiveMQ Artemis
- Embedded Artemis broker for simplified deployment
- Message persistence for reliability
- Configurable retry mechanism with exponential backoff

**Caching System**:
- Spring Cache + MapDB integration for high-performance local caching
- Namespace-based cache isolation with configurable TTL
- Full compatibility with Spring Cache annotations (@Cacheable, @CacheEvict, @CachePut)

**Query Bounds(禁止全量查询)**:

- 列表/统计查询必须分页 + 条件过滤,禁止 `findAll()` / 无 `LIMIT` 的查询一次性拉取整张表(数据量增长后会造成慢查询、内存膨胀并拖垮接口响应)
- 面向 GraphQL/REST 接口的 Repository 方法一律返回有界结果(Page / Limit);确需全量数据的场景(导出、定时任务)改为分批(keyset/游标)或流式处理,禁止无界 `findAll()` 后整表进内存

## Database Configuration

- **Primary Database**: PostgreSQL (configurable to MySQL)
- **Migration**: Liquibase with **compiled Kotlin DSL changelogs** in `services/aqua-admin/src/main/kotlin/db/changelog/` (`DbChangelogMaster` + `changes/V*.kt`; the path is the class FQCN). There is no `liquibase update` Gradle task — migrations run at application startup.
- **Schema Management**: Automatic on startup (Liquibase, admin service only; liquibase is disabled on aqua-client)
- **Persistence**: JetBrains Exposed 1.x DSL (`org.jetbrains.exposed.v1.*`) over the Spring-managed `DataSource`; repositories open transactions with `transaction(database) { }`. No JPA/Hibernate/QueryDSL on the classpath (P5 收口).

## Key Database Tables

- `users`, `addresses`: User management and delivery addresses
- `products`: Product catalog with inventory tracking
- `orders`, `order_items`, `order_gifts`: Order management and line items
- `admins`, `delivery_areas`: 后台账号(送水员档案并入 admins 单表,role=DELIVERY_WORKER 区分身份,见 PRD/07-user.md §F-USR-021)与配送区域
- `domain_events`: Domain events for event-driven architecture
- `payments`, `payment_refunds`: Financial transactions
- `reviews`, `delivery_worker_statistics`: Quality and performance tracking
- Marketing: `customer_groups`, `group_product_prices`, `coupon_templates`, `user_coupons`, `promotion_activities`, `promotion_activity_tiers`, `ticket_types`, `user_tickets`, `ticket_purchases`, `flash_sale_activities`, `gift_activities`, `first_order_activities`
- Ops: `banners`, `notices`, `feedbacks`, `store_profiles`, `delivery_fee_rules`, `system_settings`
- Logs: `api_logs`, `user_action_logs`, `business_logs`

## Primary Key Strategy

**IMPORTANT**: All database entities use `Long` type primary keys generated by Snowflake algorithm for distributed ID generation. This ensures:
- Unique IDs across all tables and environments
- Sortable by generation time
- High performance for distributed systems
- Avoids UUID performance overhead and storage costs

## Monetary Values Strategy

**IMPORTANT**: All monetary values (amounts, prices, totals) are stored as `Long` type representing cents/分. This approach:

- Avoids floating-point precision issues with financial calculations
- Provides exact arithmetic operations for money
- Stores values in the smallest currency unit (1 = ¥0.01)
- Converts to decimal display format only in presentation layer
- Ensures consistency across all financial-related tables

## GraphQL Schema Configuration

**IMPORTANT**: GraphQL schema files are located in `shared-config/graphql/` (shared types) plus per-service `src/main/resources/graphql/` directories. The admin service organizes per-domain schemas under `graphql/modules/` (one file per domain: `user.graphqls`, `order.graphqls`, `product.graphqls`, `marketing.graphqls`, `ops.graphqls`, `system.graphqls`, `log.graphqls`, ...) alongside `graphql/inputs/` and `graphql/extensions/`. The project uses:

**Shared types single source of truth**: `shared-config/graphql/shared-types.graphqls` is the only real copy of the shared type definitions. The same-named files under `services/aqua-admin/src/main/resources/graphql/`, `services/aqua-client/src/main/resources/graphql/` and `services/aqua-admin/src/test/resources/graphql/` are **git-tracked symlinks** pointing to it (`../../../../../../shared-config/graphql/shared-types.graphqls`). Editing through any of those paths edits the single source. Never replace the symlinks with regular copies or add copy tasks — `processResources` follows the symlinks (Gradle default `followSymlinks=true`) and materializes the content into the build output, so runtime classpath loading is unaffected. Windows contributors need Developer Mode (or admin) and `git config core.symlinks true` for the symlinks to check out correctly.

- `Long` types for all entity IDs (not UUID)
- Generated GraphQL types in `modules/aqua-foundation/src/main/graphql-gen/schema.kt` (package `dev.yidafu.aqua.common.graphql.generated` — do not change; regenerated via `pnpm codegen` with `graphql-codegen.yml`)
- Schema consistency between GraphQL schema and resolver implementations
- Spring Boot auto-configuration for GraphQL endpoints at `/graphql`

**Type Naming Conventions**:
- Input types: `XxxxInput` (e.g., `CreateOrderInput`, `UpdateProductInput`)
- Response types (root): `XxxxResponse` (e.g., `OrderResponse`, `ProductListResponse`)
- pagination type: `XxxVoPage`(e.g., `ProductVoPage`, `OrderVoPage`)
- Other types: `XxxxVO` (e.g., `OrderVO`, `ProductVO`)

## Configuration Management

Primary configuration in `services/aqua-admin/src/main/resources/application.yml` (client counterpart in `services/aqua-client/src/main/resources/application.yml`):

- Database connection with HikariCP pooling
- WeChat Mini Program integration (app-id, app-secret)
- WeChat Pay v3 configuration with certificate management
- JWT authentication settings
- Messaging system configuration (Artemis/Memory/Outbox strategies)
- GraphQL endpoint configuration
- Jackson serialization settings

## Security Architecture

- **Authentication**: JWT tokens with configurable expiration
- **Authorization**: Granular permission-based access control. Roles are `SUPER_ADMIN`, `ADMIN`, `DELIVERY_WORKER` (admin side) and `USER`/WeChat openid (client side). `AdminPermission` (in `aqua-foundation` `common/domain/model/enums`) defines 38 fine-grained codes (READ/WRITE split plus key ops like `ORDER_ASSIGN`, `ORDER_REVIEW`, `PAYMENT_REFUND`, `RECONCILIATION_EXECUTE`, and per-domain `SYSTEM_CONFIG_*`, `OPS_CONFIG_*`, `FEEDBACK_*`, `MKT_*` codes). `RolePermissionMapping` maps each role to its permission set; authorities are emitted as `PERMISSION_<CODE>` (plus `ROLE_<ROLE>`, and `SUPER_ADMIN` also implies `ROLE_ADMIN`).
- **Enforcement**: Resolvers guard admin mutations/queries with `@PreAuthorize("hasAuthority('PERMISSION_<CODE>')")` (see `PermissionExtensions.kt` for the authority conversion). The mapping in `RolePermissionMapping` and the codes in `AdminPermission` must stay in sync with `PRD/01-overview.md §权限码目录 / §角色权限矩阵`.
- **WeChat Integration**: code2session flow for Mini Program login
- **Payment Security**: WeChat Pay v3 signature verification and certificate handling
