# spec/ — 规格、设计稿与技术文档

本目录只保留**最终版本**的文档。改动历史与过时内容不入库,见文末存档约定。

## 目录

| 目录 | 内容 |
| --- | --- |
| [`prd/`](prd/00-PRD.md) | 产品需求(PRD)——唯一需求事实来源。`00-PRD.md` 为总纲,`01`~`14` 为编号分册;`_ref/` 为写作参考(本地,不入库) |
| [`design/`](design/index.html) | UI 设计稿——`ui-design-system.md` 设计规范、`_spec/` 页面规格(从 PRD 提取)、`admin/` `user-weapp/` `delivery/` 三端 HTML 原型(浏览器打开 `index.html` 总览) |
| [`tech/`](tech/README.md) | 技术文档——`design/` 技术设计 7 分卷与特性设计、`conventions/` 开发约定与实现指南、`operations/` 构建部署运维 |

## 协作约定

- 改业务行为:先查 `prd/` 对应分册,保持 **代码 + GraphQL schema + PRD** 同步;改页面:先对照 `design/ui-design-system.md` 与 `design/_spec/` 对应规格。
- 代码注释引用本文档一律写仓库根相对路径(如 `spec/prd/04-order.md`)。

## 存档约定(均 gitignore,不入库)

- `changelog/` — 改动历史:PRD 修订说明/修订记录、审查报告、行业调研报告、已完成的重构计划与技术报告。
- `outdate/` — 过时文档:描述已移除机制(Mappie、REST API 等)的旧指南。

历史追溯需求一律写进 `changelog/`,不要回填到 `spec/`。
