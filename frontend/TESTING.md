# AquaRush 前端单元测试与覆盖率基线

> 本文件记录前端各包的单测配置、运行方式与覆盖率基线。
> 原则:先覆盖**非 UI 的纯业务逻辑**(utils / SDK / 网络层),UI 组件与页面测试后续再补。

## 测试框架

- **统一使用 jest(不用 vitest)**:weapp(Taro 4.1.9)与 delivery-app(Expo SDK 57 / RN)的
  生态工具链(测试 preset、babel 转换、mock)都围绕 jest 构建,vitest 对这两者支持不足
  (如 Taro 官方 preset 来自 `@tarojs/test-utils-react`、RN 走 `@react-native/jest-preset` +
  `jest-expo`),故四个前端包统一 jest 29;
- 禁止引入 vitest / jest 30 / babel 8(与 Taro 4.1.9 生态不兼容,见文末「已知事项」);
- TS 源码经 **babel-jest**(`@babel/preset-env` + `@babel/preset-typescript`)转换;
- 覆盖率收集器:jest 内置 istanbul(`--coverage`)。

## 运行方式

```bash
# 单包
cd frontend/common && pnpm test          # 或 npm run test
cd frontend/admin-client && pnpm test
cd frontend/user-weapp && pnpm test
cd frontend/delivery-app && pnpm test    # jest-expo(SDK 57)

# 带覆盖率(生成 coverage/ 目录,已 gitignore)
cd frontend/common && pnpm test:coverage
```

| 包 | 命令 | 说明 |
| --- | --- | --- |
| `@aquarush/common` | `test` / `test:coverage` | 纯业务 utils + tracking SDK |
| `@aquarush/admin-client` | `test` / `test:coverage` | 非 UI 工具(debounce/http 封装) |
| `user-weapp` | `test` / `test:coverage` | 非 UI 业务 utils(cart/marketing/feedback 等) |
| `delivery-app` | `test` / `test:coverage` | jest-expo 57 预设;非 UI 纯业务逻辑(constants / url / storage / tracking 适配器) |

## 测试文件位置与覆盖范围

测试与源码同目录放置(`*.test.ts`),jest `testMatch` 只匹配 `src/**/*.test.ts`。

### 已覆盖的业务逻辑(本阶段)

| 包 | 文件 | 覆盖内容 |
| --- | --- | --- |
| common | `src/utils/money.ts` | 分↔元换算、金额格式化、解析、合计、校验(100%) |
| common | `src/utils/date.ts` | 日期时间格式化(100%) |
| common | `src/utils/md5.ts` | RFC 1321 向量 + 中文/emoji UTF-8(98.6%) |
| common | `src/utils/image.ts` | buildUrl、identicon 默认头像(100%) |
| common | `src/utils/network.ts` | NetworkManager:认证头、401 重试、GraphQL/REST/上传、错误封装(83.5%) |
| common | `src/tracking/tracker.ts` | 埋点队列、去重、token 门控、失败重入队、dispose(94.9%) |
| admin-client | `src/utils/debounce.ts` | 防抖合并、计时重置、参数透传(100%) |
| admin-client | `src/utils/http.ts` | 拦截器:token 注入、success 解包、业务失败抛错、错误提示(94.4%) |
| user-weapp | `src/utils/cart.ts` | 购物车增删改、合并、勾选、合计(95.9%) |
| user-weapp | `src/utils/feedback.ts` | 反馈状态色(CSS 变量,100%) |
| user-weapp | `src/styles/tokens.test.ts` | theme.scss `--status-*` ↔ common STATUS_TONE_COLORS 同步守卫 |
| delivery-app | `src/constants/order.ts` | 订单状态/进度步骤/收款方式文案与工具(100%;状态文案与步骤改自 common re-export) |
| delivery-app | `src/constants/collection.ts` | 每日收款/对账状态文案与 tone 映射(100%;tone 改自 common re-export) |
| delivery-app | `src/utils/url.ts` | resolveAssetUrl:相对路径拼接、localhost 重写、非法 URL 透传(100%) |
| delivery-app | `src/services/storage.ts` | AsyncStorage 封装:token/用户信息存取、损坏 JSON、clearAll(100%) |
| delivery-app | `src/utils/tracking.ts` | RN 适配器:send 上报(带 token 头)、路由、认证上下文解析(100%) |

> 2026 复用改造后迁移说明:user-weapp 的 `orderCost`/`marketing`(券/票标签)与
> delivery-app 的 `orderCost` 已合并进 common `utils/order-cost`/`utils/marketing`(测试随迁,
> 见 common 表);user-weapp `network.ts`(共享 NetworkManager 的本地副本)已删除,
> `auth.ts` 改接共享单例;hook 层(`useOrderDetail`/`usePagedList`/`order-view`)测试在 common。

### 尚未覆盖(下一阶段候选)

- **user-weapp**:`auth.ts` / `auth.h5.ts` / `networkManager.ts` / `tracking.ts` / `clear-auth.ts`(Taro API 耦合较多,需 mock `@tarojs/taro` 后补测);
- **admin-client**:`utils/tracking.ts`(需 jsdom + fetch mock);
- **delivery-app**:`services/*`(GraphQL 查询)、`utils/network-manager.ts`(依赖 common NetworkManager + fetch)、`utils/upload.ts`(依赖 networkManager)、`utils/trajectory-*`(原生模块耦合)— 下一阶段补测;
- **common**:`utils/index.ts`(仅 re-export,无需测)、`hooks/useOrderDetail.ts`/`usePagedList.ts` 的 renderHook 测试已随 hooks 目录纳入 coverage;
- **UI 组件与页面**:admin-client 的 `components/`、user-weapp 的 `pages/` 与共享组件(StatusTag/OrderItemRow 等)— 按约定本阶段不做。

## 覆盖率基线(2026 年 08 月记录)

> 基线口径:仅统计上表「已覆盖业务逻辑」所在的源码目录(`common/src/utils/**` + `tracker.ts`、
> `admin-client/src/utils/**`、`user-weapp/src/utils/**`),UI/pages/components 未纳入。
> 用 `pnpm test:coverage` 复现;后续每轮新增测试后更新此表。

### @aquarush/common — 143 tests(2026 复用改造后新增 status-tone / order-status / order-cost / marketing / tianditu / hooks 等)

> 覆盖率数值以 `pnpm test:coverage` 实测为准(本表为迁移前基线,待下次覆盖运行回写)。

| 文件 | Stmts | Branch | Funcs | Lines |
| --- | --- | --- | --- | --- |
| **All files** | **93.70%** | **80.88%** | **91.04%** | **93.08%** |
| utils/money.ts | 100% | 100% | 100% | 100% |
| utils/date.ts | 100% | 100% | 100% | 100% |
| utils/image.ts | 100% | 100% | 100% | 100% |
| utils/md5.ts | 98.59% | 92.85% | 100% | 98.36% |
| utils/network.ts | 83.47% | 67.59% | 81.48% | 83.33% |
| tracking/tracker.ts | 94.87% | 86.79% | 92.30% | 94.59% |
| utils/index.ts | 0% | 0% | 0% | 0% |
| utils/status-tone.ts | 新增:值=§2.2、域映射覆盖枚举、getStatusTone 兜底 | | | |
| utils/order-status.ts | 新增:文案全覆盖、步骤、ORDER_TABS 口径、getStatusView | | | |
| utils/order-cost.ts | 新增:全优惠栈/单优惠/赠品/免运费/全零/水桶费/小计推导/fromPreview | | | |
| utils/marketing.ts | 新增:券/票/反馈标签、折扣率 ÷1000(8500‰→8.5折) | | | |
| utils/tianditu.ts | 新增:瓦片 URL 模板、style 结构、attribution | | | |
| hooks/order-view.ts | 新增:items 归一/营销价判定、时间轴、地址、flags、费用快照 | | | |
| hooks/useOrderDetail.ts | 新增:renderHook 加载/失败/刷新/无 orderNo | | | |
| hooks/usePagedList.ts | 新增:renderHook 首屏/分页/重置/失败保持 | | | |

### @aquarush/admin-client — 14 tests

| 文件 | Stmts | Branch | Funcs | Lines |
| --- | --- | --- | --- | --- |
| **All files** | **48.93%** | **29.62%** | **64.70%** | **50.00%** |
| utils/debounce.ts | 100% | 100% | 100% | 100% |
| utils/http.ts | 94.44% | 100% | 88.88% | 94.44% |
| utils/tracking.ts | 0% | 0% | 0% | 0% |

### user-weapp — 55 tests(2026 复用改造后;orderCost/marketing 测试已迁 common)

| 文件 | Stmts | Branch | Funcs | Lines |
| --- | --- | --- | --- | --- |
| **All files** | **16.96%** | **24.46%** | **22.13%** | **15.93%** |
| utils/cart.ts | 95.91% | 90.00% | 100% | 95.00% |
| utils/feedback.ts | 100% | 100% | 100% | 100% |
| styles/tokens.test.ts | 守卫:theme.scss `--status-*` ↔ common STATUS_TONE_COLORS | | | |
| utils/auth.ts / auth.h5.ts / networkManager.ts / tracking.ts / clear-auth.ts | 0% | 0% | 0% | 0% |

### delivery-app — 31 tests(2026 复用改造后;orderCost 测试已迁 common)

> 口径:`jest.config.js` 的 `collectCoverageFrom` 覆盖 `src/constants|utils|services/**`
> (UI 组件与页面未纳入),与其它包一致;`api.ts` 的未覆盖分支为 Android 平台分支,`theme.ts`
> 仅在常量文件中被 `import type` 引用(运行时未加载)。

| 文件 | Stmts | Branch | Funcs | Lines |
| --- | --- | --- | --- | --- |
| **All files** | **19.49%** | **19.27%** | **16.50%** | **18.97%** |
| constants/order.ts | 100% | 100% | 100% | 100% |
| constants/collection.ts | 100% | 100% | 100% | 100% |
| constants/api.ts | 87.50% | 55.55% | 100% | 87.50% |
| services/storage.ts | 100% | 100% | 100% | 100% |
| utils/url.ts | 100% | 100% | 100% | 100% |
| utils/tracking.ts | 100% | 81.81% | 100% | 100% |
| 其余 services/utils(未覆盖) | 0% | 0% | 0% | 0% |

## 已知事项

1. **user-weapp 的 Taro UI 测试未启用**:`__tests__/index.test.js`(Taro 模板自带快照测试)依赖
   `@tarojs/test-utils-react`,`@tarojs/router` 在 jsdom 下模块加载即访问 `window.history` 报错,
   属 Taro UI 测试基建问题。当前 `jest.config.js` 的 `testMatch` 只匹配 `src/**/*.test.ts`,
   待修复后把 `__tests__` 加回。
2. **admin-client `tsc -b`**:`src/utils/**/*.test.ts` 参与 `tsconfig.app.json` 编译
   (`include: ["src"]`),测试文件类型必须保持正确(`@jest/globals` 已加入 devDependencies)。
   `tsc -b` 当前存在大量 pages/components 的**既有**类型错误,与本阶段改动无关。
3. **依赖**:jest 29 + babel-jest 29 + `@babel/*` 7.x + `@types/jest` 29 + `@jest/globals` 29,
   三个包版本已对齐;不要用 vitest/jest 30/babel 8(与 Taro 4.1.9 生态不兼容)。
