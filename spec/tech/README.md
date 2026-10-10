# spec/tech/ — 技术文档

按「设计 → 约定」两层组织,只保留最终版本。

## design/ — 技术设计(系统怎么设计)

七分卷实现技术文档(总览见 [README](design/README.md),基于 `spec/prd/` 推导,不引用现有代码):

| 分卷 | 内容 |
| --- | --- |
| [01-data-model](design/01-data-model.md) | 数据库全量设计(60+ 表、状态机、分区归档) |
| [02-api-design](design/02-api-design.md) | GraphQL 双端 Schema、REST 端点、WS 协议、权限接线 |
| [03-core-flows](design/03-core-flows.md) | 16 个核心流程时序与并发实现 |
| [04-frontend-design](design/04-frontend-design.md) | 三端一包前端架构与页面要点 |
| [05-security-and-nfr](design/05-security-and-nfr.md) | 认证授权、数据隔离、异常矩阵、非功能 |
| [06-testing-and-roadmap](design/06-testing-and-roadmap.md) | 测试策略、验收追溯、路线图 |
| [07-payment-reconciliation](design/07-payment-reconciliation.md) | 微信支付对账详细设计 |

特性级设计:[outbox-messaging](design/outbox-messaging.md)(Transactional Outbox 事件传输,被多处代码注释引用)、[address-management](design/address-management.md)(地址管理实现说明)。

## conventions/ — 开发约定(代码怎么写的规范)

只放**约定与契约**,不放用法说明(用法/技术细节类文档在根 `agents/`):

- [soft-delete-guide](conventions/soft-delete-guide.md) — 软删表查询约定(配合根 `AGENTS.md` 规则 13)
- [error-response-format](conventions/error-response-format.md) — 统一错误响应格式

相关用法参考(在 `agents/`):[graphql-validation](../../agents/graphql-validation.md)(GraphQL 输入验证)、[logging](../../agents/logging.md)(日志模块)、[debug-jwt-endpoint](../../agents/debug-jwt-endpoint.md)(JWT 调试端点)。

构建部署运维不单设目录:部署步骤、环境变量、时区与日志备份恢复见 `agents/deployment.md`,开发环境搭建与常用命令见根 `AGENTS.md`。

## 存档

改动历史与过时文档不入库:重构/特性计划与技术报告在 `changelog/tech/`,已移除机制(如 Mappie、旧 REST API、旧 entry-module 架构)的指南在 `outdate/`。
