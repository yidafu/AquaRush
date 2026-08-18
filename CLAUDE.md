# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. It is intentionally concise — detailed reference docs live in `claude/` and are listed (and imported) at the bottom of this file.

## Version Requirements

**IMPORTANT**: This project uses Spring Boot 4.0.0 and requires this exact version. When resolving dependency conflicts, never downgrade Spring Boot versions as this will break the application. The project's BOM and all dependencies are configured specifically for Spring Boot 4.0.0.

## Project Overview

AquaRush is a WeChat Mini Program-based bottled water ordering and delivery management system built with a Kotlin Spring Boot 4.0.0 backend and React-based frontends. The project uses a multi-module architecture with domain-driven design principles.

- **Backend modules** (`modules/`): `aqua-foundation` (shared infra), `aqua-user`, `aqua-product`, `aqua-trade`, `aqua-analytics`, `aqua-platform`, `aqua-ops` (运营配置), `aqua-marketing` (营销)
- **Services** (`services/`): `aqua-admin` (admin dashboard backend), `aqua-client` (Mini Program backend)
- **Frontends** (`frontend/`): `user-weapp` (Taro Mini Program), `delivery-app` (Expo React Native), `admin-client` (React + Vite + Ant Design), sharing `@aquarush/common`

## PRD (Requirements Source of Truth)

The `PRD/` directory holds the product requirements as numbered markdown docs (`01-overview.md`, `03-product.md`, `04-order.md`, `09-platform-logs.md`, `10-marketing.md`, `11-system-settings.md`, `12-operations-config.md`, etc.). Domain code comments reference it directly (e.g. `AdminPermission` ↔ `PRD/01-overview.md §权限码目录`). When implementing or changing business behavior, consult the relevant `PRD/` doc first and keep code, GraphQL schema, and PRD in sync.

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
./gradlew :modules:aqua-foundation:test
./gradlew :modules:aqua-trade:test

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
# Install workspace dependencies (user-weapp, delivery-app, admin-client, common)
# The frontends (user-weapp, delivery-app, admin-client) consume @aquarush/common
# via pnpm workspace (see pnpm-workspace.yaml) — `workspace:*` deps require pnpm, not npm.
pnpm install   # from repo root

# User WeChat Mini Program (Taro)
cd frontend/user-weapp
npm run dev:weapp          # Development with WeChat Studio
npm run build:weapp        # Production build for WeChat Mini Program

# Delivery App (Expo React Native) — part of the pnpm workspace
cd frontend/delivery-app
pnpm install
npx expo start             # i = iOS sim, a = Android sim, w = web, or scan with Expo Go

# Admin Dashboard (React + Vite)
cd frontend/admin-client
npm run dev  # Runs on http://localhost:5173

# Additional WeChat Mini Program build commands
cd frontend/user-weapp
npm run build:h5           # H5 web build
npm run dev:h5             # H5 development build
```

## Architecture at a Glance

- **8 backend modules + 2 services**: full module layout, directory conventions, and design patterns in `claude/architecture.md`
- **`aqua-ops`** covers 运营配置 (banner, notice, feedback, store profile, delivery fee rule, system config) → `PRD/12-operations-config.md`, `PRD/11-system-settings.md`
- **`aqua-marketing`** covers 营销 (customer groups, group prices, coupons, promotions, water tickets, flash sale, buy-gift, first-order) plus a shared pricing engine → `PRD/10-marketing.md`
- **Frontends**: detailed patterns in `claude/frontend.md`
- **GraphQL**: resolver development patterns in `claude/resolvers.md`; per-domain admin schemas under `services/aqua-admin/src/main/resources/graphql/modules/`

## Critical Development Rules

These invariants must always hold — see `claude/architecture.md` for the full rationale.

1. **Spring Boot 4.0.0 must never be downgraded** (see Version Requirements above).
2. **All entity IDs are `Long` Snowflake**; **all monetary values are `Long` cents (分)** — money is formatted for display only in the presentation layer.
3. **JPA entities are centralized** in `modules/aqua-foundation/.../common/domain/model/`; cross-module DTOs live in `aqua-foundation` `api/dto` (one class per file).
4. **Module dependency rule**: domain modules MUST NOT call other modules' repositories directly — cross-module data access goes through `aqua-foundation` `api/service/**`.
5. **Prohibited**: new `*ApiService` interface suffixes (use `*QueryService`/`*MutationService`); `util`/`utils` dual packages; module-root `exception/`; `.imports` files outside `META-INF/spring/`.
6. **Admin resolvers** annotate each method with `@PreAuthorize("hasAuthority('PERMISSION_<CODE>')")` using codes from `AdminPermission`; client resolvers use `isAuthenticated()`. Keep `AdminPermission` / `RolePermissionMapping` in sync with `PRD/01-overview.md §权限码目录 / §角色权限矩阵`.
7. **Backend API changes require accompanying integration tests, and the suite must pass** before the work is considered complete — run `./gradlew test` (see `claude/deployment.md`).
8. **Frontends use `@aquarush/common`** via pnpm workspace (`workspace:*` deps require pnpm, not npm); all money formatting/parsing goes through `@aquarush/common/utils/money`.
9. **React components are functional components with TypeScript annotations**, never class components.
10. **单页面的 GraphQL 请求必须合并**: 一个页面的一次渲染只发起一个 GraphQL 请求,通过字段选择一次取回该页所需全部数据 — 禁止为同一页面串行发起多个独立请求(这会增加网络往返、拖慢页面加载)。
11. **Resolver input DTOs are never inlined in resolvers** (no `data class XxxInput` in a resolver `companion object`) — service-local GraphQL input DTOs live in `<admin|client>/<domain>/dto/`, cross-module DTOs in `aqua-foundation` `api/dto`.
12. **允许不兼容修改(开发期间)**: 开发期间遇到运行不兼容的修改,以最优实现为准 — 不因迁就旧约定、旧文档或既有行为而牺牲实现质量;允许破坏性变更(如模块结构、DTO/模型、行为调整),并同步更新代码、GraphQL schema 与 PRD。

## Reference Documentation

Detailed guides are split into `claude/` files and imported here — consult the relevant one when working in that area. The `@`-imports below are loaded into context when this file is read (do not wrap them in backticks).

- @claude/architecture.md — Backend module structure, directory conventions, key design patterns, database & key tables, GraphQL schema config, security architecture
- @claude/resolvers.md — GraphQL resolver development (annotations, examples, UserPrincipal, BaseGraphQLResolver, patterns)
- @claude/mappie.md — Object Mapping with Mappie (compile-time type-safe mappers)
- @claude/frontend.md — Frontend architecture, React/TypeScript & Taro page patterns, UI design guidelines
- @claude/commit-automation.md — Commit message generation (Angular convention) and Python-first automation standards
- @claude/messaging-cache-wechat.md — Event processing (Artemis), cache usage, WeChat integration notes
- @claude/deployment.md — Testing strategy, single-server production deployment, environment variables

## License

This project is licensed under AGPL-3.0. Any modifications must be released under the same license, and users accessing the service over network have the right to obtain the source code.
