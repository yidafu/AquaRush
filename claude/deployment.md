# Testing, Production Deployment & Environment

> Imported by the root `CLAUDE.md`. Testing strategy, single-server deployment, and production environment variables.

## Testing Strategy

**IMPORTANT**: Developing or modifying any backend API (GraphQL resolvers, REST controllers, or cross-module service interfaces) requires accompanying integration tests, and the integration test suite MUST pass before the work is considered complete. Backend API code must not be committed without passing integration tests. Run them with `./gradlew test` (or `./gradlew :services:aqua-admin:test` / `./gradlew :services:aqua-client:test` for service-level integration tests).

- Unit tests for repositories and services
- Integration tests for controllers and event processing
- Database migrations should be tested with different database versions
- WeChat integration should be tested with sandbox environments
- React components should have unit tests with React Testing Library

### Test Database (Testcontainers PostgreSQL)

**H2 has been removed from the test stack.** Repository impl tests and Spring integration tests run against a **real PostgreSQL** container via Testcontainers (default image `postgres:17-alpine`) — H2's `MODE=PostgreSQL` does not reproduce `INSERT ... ON CONFLICT` upsert semantics, `jsonString` binding, varchar length checks, or row-lock behaviour.

- Shared container bootstrap: `PostgresTestContainer` (test fixture of `aqua-foundation`, consumed via `testImplementation(testFixtures(project(":modules:aqua-foundation")))`).
- Gradle wiring: the `aqua.testcontainers` convention plugin (container deps + `DOCKER_HOST`/ryuk setup).
- `services/aqua-admin` and `services/aqua-client` inject the container into Spring through a tracked `ApplicationContextInitializer` registered in `src/test/resources/META-INF/spring.factories` — **not** through `application-test.yml`, which `.gitignore` excludes from version control (`application-*.yml`).
- **A container runtime is required** to run backend tests: Docker, or podman on machines without Docker (the convention plugin falls back to the podman machine socket and disables ryuk). CI needs a working Docker daemon.
- Image override: `AQUA_TEST_PG_IMAGE` (default `postgres:17-alpine`). The container is tuned for speed and a small footprint (`fsync=off`, `shared_buffers=16MB`, `max_connections=200`).
- **`./gradlew check` takes ~20 min**, dominated by `services/aqua-admin` booting ~30 cached Spring contexts (test bodies themselves total ~13 s). Runtime is the same order as before the migration — iterate with module-scoped `--tests` and `--no-build-cache` when you need a trustworthy result (the test task result otherwise often comes `FROM-CACHE`).

### Coverage Standards (Kover)

The build enforces per-module line-coverage gates via **Kover** (configured in root `build.gradle.kts`, `koverModuleBounds`). `koverVerify` runs on `check`, so `./gradlew build` fails when a module is below its bound. The standard:

| Tier | Line coverage | Modules | Rationale |
|---|---|---|---|
| S | ≥ 80% | `aqua-trade`, `aqua-marketing` | Money & state machines (orders, payment, pricing engine) — highest priority |
| A | ≥ 70% | `aqua-ops`, `aqua-user`, `aqua-product`, `aqua-analytics` | Business orchestration services |
| B | ≥ 60% | `aqua-foundation`, `aqua-platform` | Shared infra / storage / messaging |
| C | ≥ 55% | `aqua-admin`, `aqua-client` | Thin GraphQL resolvers; covered mainly by integration tests |
| Total | ≥ 60% | whole codebase (merged) | Aggregate gate |

The following are **excluded** from measurement (no business logic or generated code — `*Mapper*` conversions are handwritten, not generated): `dev.yidafu.aqua.api.dto` DTOs, `dev.yidafu.aqua.common.graphql.generated` codegen, `*Mapper*` conversion functions in `*Mapper*.kt` (mechanical translation, covered by integration tests), `*Exception`/`*Exceptions`, `*Application` entry classes, and `@Configuration` classes.

**Commands**: `./gradlew koverVerify` (whole build), `./gradlew :modules:aqua-trade:koverVerify` (one module), `./gradlew koverHtmlReport` (per-module HTML reports under each `build/reports/kover/`), `./gradlew :modules:aqua-trade:koverHtmlReport`.

**Which classes need unit tests** (priority order — see the tier list in the Kover gate config and root `build.gradle.kts`):

1. **Tier 1 — business core, MUST have unit tests** (MockK + JUnit 5, no Spring context):
   - `aqua-trade`: `OrderMutationServiceImpl`, `PaymentServiceImpl`, `SdkWechatPayGateway`, `OrderOperationServiceImpl`, `PaymentQueryServiceImpl`
   - `aqua-marketing`: `PromotionEngineImpl` + all `engine/*Rule` classes (money calculation core)
   - `aqua-analytics`: `ReconciliationServiceImpl`, `StatisticsServiceImpl` family, both schedulers
   - `aqua-ops`: `DeliveryFeeRuleMutationServiceImpl` (fee tiers), `OpsConfigAuditService`
2. **Tier 2 — business orchestration** (all `*MutationServiceImpl` / `*QueryServiceImpl` not yet covered in `aqua-marketing`, `aqua-product`, `aqua-user`, `aqua-platform`, `aqua-analytics`)
3. **Tier 3 — cheap & valuable**: utils (`JsonHelper`, `SearchContentBuilder`, `PaginationUtils`), domain-model methods with real logic (money fields, status transitions), repository custom impls (Testcontainers PostgreSQL integration — see `SoftDeleteRepositoryTest` / `MonetaryFieldsIntegrationTest`)

**No unit tests needed** for: DTOs, conversion functions in `*Mapper*.kt` (mechanical translation, excluded from coverage by class name and covered by integration tests — do not write conversion unit tests), GraphQL resolvers/controllers (integration-tested), `@Configuration`/exception classes.

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
  - 本地开发:`./gradlew :services:aqua-client:bootRun` 会自动加载 `services/aqua-client/.env`(若存在),无需手写 export;小程序项目 `project.config.json` 的 `appid` 必须与后端 `WECHAT_APP_ID` 一致(不能用 `touristappid`,否则 `wx.login` 的 code 无法换取 session,登录必然失败并报 40013 invalid appid)
- 微信支付 V3(PRD 04-order.md §F-ORD-005):
  - `WECHAT_PAY_MOCK_ENABLED`: 测试环境默认 `true`(假支付,发起支付即自动完成);生产必须置 `false`
  - `WECHAT_PAY_APPID` / `WECHAT_PAY_MCHID`: 小程序 AppID / 商户号
  - `WECHAT_PAY_PRIVATE_KEY_PATH`: 商户 API 私钥文件路径(apiclient_key.pem,服务器本地文件)
  - `WECHAT_PAY_MERCHANT_SERIAL_NUMBER`: 商户证书序列号
  - `WECHAT_PAY_API_V3_KEY`: APIv3 密钥
  - `WECHAT_PAY_NOTIFY_URL`: 回调地址(公网 HTTPS,如 `https://api.example.com/api/pay/notify`)
- `JWT_SECRET`: JWT signing secret
- `SNOWFLAKE_MACHINE_ID`: Unique ID for distributed ID generation

### Timezone (统计聚合时区)

统计按日/周/月聚合时区统一 **Asia/Shanghai**(PRD 08-statistics.md §域级口径,P2-8)。为避免依赖部署环境默认时区,时区在以下三处显式钉死,新增部署方式时须保持一致:

1. **应用入口**:两个服务 `main()` 开头调用 `TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))`(`AquaAdminApplication.kt` / `AquaClientApplication.kt`),覆盖所有启动方式;
2. **JDBC 连接**:`application.yml` 数据源 URL 携带 `TimeZone=Asia/Shanghai`(PostgreSQL 会话时区);
3. **systemd**:`scripts/aquarush.service` 的 `ExecStart` 显式 `-Duser.timezone=Asia/Shanghai`。

直接用 `java -jar` 部署时,同样建议追加 `-Duser.timezone=Asia/Shanghai`。