# AGENTS.md

Instructions for AI coding agents (OpenCode, Claude Code, …) working in this repository. Keep this file compact — only guidance an agent would otherwise get wrong. Detailed reference docs live in `agents/` and are imported at the bottom.

## Version requirements

**Spring Boot 4.0.0 must never be downgraded.** The root `build.gradle.kts` BOM and all dependencies are pinned to 4.0.0; do not resolve dependency conflicts by lowering Spring versions.

## Project overview

AquaRush — WeChat Mini Program bottled-water ordering & delivery system. Kotlin Spring Boot 4 backend, React frontends.

- **Backend modules** (`modules/`): `aqua-foundation` (shared infra: domain models, Exposed table mappings, cross-module DTOs/services, GraphQL config, messaging, security), `aqua-user`, `aqua-product`, `aqua-trade` (order/delivery/payment/review), `aqua-analytics`, `aqua-platform`, `aqua-ops` (运营配置), `aqua-marketing` (营销 + `barrel/` 水桶费 — merged from former `aqua-deposit`)
- **Services** (the only two deployable entrypoints): `services/aqua-admin` (admin dashboard backend), `services/aqua-client` (Mini Program backend)
- **Frontends** (pnpm workspace): `frontend/user-weapp` (Taro 4 Mini Program; npm package name `user-miniapp`), `frontend/delivery-app` (Expo SDK 57 React Native), `frontend/admin-client` (React + Vite;内部 `src/ui` 组件库 = Radix + StyleX,已移除 antd), `frontend/common` (`@aquarush/common` — shared money/status utils, tracking SDK)
- Root `package.json` scripts: `pnpm dev:admin` / `pnpm dev:client` (Gradle bootRun), `pnpm dev:weapp`, `pnpm dev:pc`.

## spec/ — 规格、设计稿与技术文档

`spec/` 只保留**最终版本**的文档,分三区:

- **`spec/prd/`** — 需求(唯一事实来源):编号分册(`01-overview.md`, `04-order.md`, `10-marketing.md`, `12-operations-config.md`, …)。Domain code comments reference them directly (e.g. `AdminPermission` ↔ `spec/prd/01-overview.md §权限码目录`). When changing business behavior: consult the relevant PRD first, then keep **code + GraphQL schema + PRD** in sync. `spec/prd/_ref/` 是写作参考(本地,不入库)。
- **`spec/design/`** — UI 设计稿:`ui-design-system.md`(设计规范)、`_spec/`(从 PRD 提取的页面规格)、`admin|user-weapp|delivery/`(HTML 原型)。改页面先对照对应规范。
- **`spec/tech/`** — 技术文档(见 `spec/tech/README.md`):`design/`(技术设计 7 分卷 + 特性设计)、`conventions/`(开发约定与实现指南)。构建部署运维见 `agents/deployment.md`。

本地存档目录(均 gitignore,不入库):`changelog/` 放改动历史(PRD 修订说明、审查报告、已完成重构计划等),`outdate/` 放过时文档。不要把这两类内容写进 `spec/`。

## Build, test & verification

### Backend (Gradle)

```bash
./gradlew check        # FULL verification: ktlint + tests + Kover coverage gates
./gradlew test                                   # all tests
./gradlew :modules:aqua-trade:test               # one module
./gradlew :modules:aqua-trade:test --tests '*OrderServiceTest*'   # one test class
./gradlew ktlintFormat                           # auto-format Kotlin
./gradlew :services:aqua-admin:bootRun           # = pnpm dev:admin
./gradlew :services:aqua-client:bootRun          # = pnpm dev:client
```

- Backend tests are **Kotest 6** (`FunSpec` + MockK + kotest-matchers), run via the JUnit Platform. Root `build.gradle.kts` **globally excludes JUnit Jupiter/Vintage** — never write `@Test`/`@ExtendWith`-style tests; they won't run.
- `koverVerify` is wired into `check`: per-module coverage gates (`koverModuleBounds` in root `build.gradle.kts` — aqua-trade/aqua-marketing ≥ 80%, whole repo ≥ 60%). `./gradlew build` fails when a module is under its bound; new business code in a module needs tests to keep the gate green. Standard: `agents/deployment.md` §Testing Strategy.
- There is **no `./gradlew …:update` Liquibase task** (plugin declared in the version catalog but never applied). Migrations run automatically at application startup.
- `services/aqua-client/.env` is auto-loaded by aqua-client bootRun if present (WeChat creds, `WECHAT_PAY_MOCK_ENABLED`, etc. — see `agents/deployment.md` §Environment Variables). `aqua-admin` has no dotenv loading.
- Integration tests boot Spring + a **Testcontainers PostgreSQL** container with `maxHeapSize = 1g` and are slow — iterate with module-scoped `--tests`, run the full `./gradlew check` before considering work done (required for any API change). **H2 is gone from the test stack**: a container runtime (podman) is required to run backend tests — see `agents/deployment.md` §Test Database.
- Statistics/aggregation timezone is pinned to **Asia/Shanghai** in three places (app `main()`, JDBC URL, systemd `-Duser.timezone`). Keep any new deployment path consistent — `agents/deployment.md` §Timezone.

### Frontend (pnpm workspace)

```bash
pnpm install                              # repo root — npm install breaks workspace:* deps
pnpm test:frontend                        # all 4 packages (jest)
pnpm --filter @aquarush/common test       # one package
pnpm --filter user-miniapp test -- cart    # one test file (jest filename pattern)
pnpm codegen                              # regenerate GraphQL types after editing *.graphqls
```

- Root `pnpm test` **always exits 1** ("no test specified") — use `pnpm test:frontend`.
- Tests are **jest only** — no vitest, no jest 30, no babel 8 (Taro 4.1.9 / Expo 57 ecosystems depend on jest; `frontend/TESTING.md`). Tests are colocated: `src/**/*.test.ts`.
- Lint is per-package: `pnpm --filter @aquarush/admin-client lint` (eslint), `pnpm --filter delivery-app lint` (expo lint). Only admin-client has a real typecheck (`tsc -b`, part of its `build` script).
- `pnpm codegen` (config: `graphql-codegen.yml`) regenerates `modules/aqua-foundation/src/main/graphql-gen/schema.kt` and `frontend/common/src/types/graphql.ts` from the `*.graphqls` schemas. Both are **generated — never hand-edit**; change the schema and re-run codegen.
- Money display/parsing goes through `@aquarush/common/utils/money`; delivery-app targets Expo SDK 57 (`frontend/delivery-app/AGENTS.md`).

## Critical development rules

These invariants must always hold — see `agents/architecture.md` for rationale:

1. All entity IDs are `Long` Snowflake; **all monetary values are `Long` cents (分)** — format only at the presentation layer.
2. Domain models (plain Kotlin data classes, no JPA annotations) are centralized in `modules/aqua-foundation/.../common/domain/model/`; Exposed table mappings in `modules/aqua-foundation/.../common/domain/table/`; cross-module DTOs live in `aqua-foundation` `api/dto` (one class per file).
3. Domain modules MUST NOT call other modules' repositories directly — cross-module data access goes through `aqua-foundation` `api/service/**`.
4. Prohibited: new `*ApiService` interface suffixes (use `*QueryService`/`*MutationService`); `util`/`utils` dual packages; module-root `exception/`; `.imports` files outside `META-INF/spring/`.
5. Admin resolvers annotate each method with `@PreAuthorize("hasAuthority('PERMISSION_<CODE>')")` using codes from `AdminPermission`; client resolvers use `isAuthenticated()`. Keep `AdminPermission` / `RolePermissionMapping` in sync with `spec/prd/01-overview.md`.
6. Backend API changes require passing integration tests (`./gradlew test` / `check`) before the work is complete.
7. **单页面的 GraphQL 请求必须合并**: one render → one GraphQL request with all needed fields; never chain multiple sequential requests for one page.
8. Resolver input DTOs are never inlined in resolvers — service-local inputs go in `<admin|client>/<domain>/dto/`, cross-module DTOs in `aqua-foundation` `api/dto`.
9. **允许不兼容修改(开发期间)**: prefer the best implementation over legacy conventions; breaking changes are allowed as long as code, GraphQL schema, and PRD are updated together.
10. **禁止数据表的全量查询**: every list/statistics query for an API must paginate + filter; no `findAll()` / unbounded `SELECT` — use batch/cursor for exports and jobs.
11. React components are functional with TypeScript annotations, never class components.
12. Every new `.kt` file needs the AGPL copyright header (ktlint `header-rule` is enforced by `./gradlew check`). Add it with `python3 scripts/add-copyright.py --dirs <dir>` (idempotent) or copy an existing header.
13. **软删查询走入口**:软删表(`SoftDeleteFlagTable`/`SoftDeletableTable`)的 `selectAll()`/`select()` 已自动带 `is_deleted = false`,追加条件用 `andWhere {}`(再 `.where {}` 会抛 `WHERE clause is specified twice`);读已删行只能走 `selectAllIncludingDeleted()`;join/聚合用 `activeOnly {}` 并逐表过滤;软删用 `softDelete {}`。守卫测试 `SoftDeleteQueryConventionTest`。

## Commits

Angular convention, `<type>(<scope>): <description>` (history uses Chinese descriptions). No co-author/attribution trailers. Helper: `./scripts/generate-commit.sh`. See `agents/commit-automation.md`.

## Reference documentation (read when working in that area)

- `agents/architecture.md` — module structure, directory conventions, DB tables, GraphQL schema config, security
- `agents/resolvers.md` — GraphQL resolver patterns, UserPrincipal, BaseGraphQLResolver
- `agents/graphql-validation.md` — schema 验证指令 → Jakarta 注解(codegen 插件)→ 运行时校验链路
- `agents/logging.md` — 日志模块使用(结构化/业务/审计日志)
- `agents/frontend.md` — frontend architecture, Taro/React page patterns, UI guidelines
- `agents/deployment.md` — testing strategy & coverage tiers, production deploy, env vars, timezone
- `agents/debug-jwt-endpoint.md` — JWT 调试端点(开发环境)
- `agents/messaging-cache-wechat.md` — outbox domain events, cache, WeChat integration
- `agents/commit-automation.md` — commit message tooling
