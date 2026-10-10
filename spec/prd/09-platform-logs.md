# 平台与日志域(LOG)

> 面向管理后台的日志管理子域:三类日志(API 日志 / 用户操作日志 / 业务日志)的记录、查询、筛选与详情追溯,供异常排查与审计。本域范围:F-LOG-001~003(均为管理后台端);规则:本域无自有 R-* 规则,遵循 01-overview.md §非功能(日志审计)与 14-misc.md §安全与合规(交叉引用见 §4)。

## 1. 域概述

### 1.1 域范围与功能清单

| F-ID | 名称 | 状态 | 端 |
| --- | --- | --- | --- |
| F-LOG-001 | API 日志 | [已实现] | 管理后台 |
| F-LOG-002 | 用户操作日志 | [已实现] | 管理后台 |
| F-LOG-003 | 业务日志 | [已实现] | 管理后台 |

### 1.2 域内角色与入口

- 使用角色:管理员(后台账号登录),对应 01-overview.md §角色权限矩阵中「数据统计与日志」列(管理员 ✅)。
- 入口:登录管理后台后,经左侧导航「日志管理」展开子菜单,分别进入「API日志」(`/logs/api`)、「用户操作」(`/logs/user-actions`)、「业务日志」(`/logs/business`)。
- 数据来源:API/用户操作日志由前端通过 REST 接口查询(`/logs/api`、`/logs/user-actions`,见 `frontend/admin-client/src/services/logs.ts`);业务日志(F-LOG-003)经 GraphQL `businessLogs` 查询(admin 端,`frontend/admin-client/src/services/log-graphql.ts`),REST `/logs/business` 控制器保留。三张日志表(api_logs/user_action_logs/business_logs)按月 RANGE 分区,超期分区经「系统设置-数据维护」离线备份(转储冷表 + 导出文件)后不再在线查询(见 11-system-settings.md §F-SYS-006)。

### 1.3 域级验收清单

三类日志持续记录,筛选可用,关键操作可追溯。(迁移自原 PRD 第 11 章 11.1「日志」验收行)

## 2. 域内业务规则

本域无自有 R-* 规则。日志记录、保留与审计的合规性要求引用:

- 01-overview.md §非功能(日志审计):接口调用、用户操作、业务事件须留痕且可追溯;
- 14-misc.md §安全与合规:日志审计须满足合规要求。

各功能在 §3「业务规则扩充」中对上述要求做本域口径的细化说明。

## 3. 功能需求详述

### 3.1 管理后台:日志管理

#### F-LOG-001 API 日志 [已实现]
**需求描述**: 记录后台关键接口的调用日志,含调用时间、路径、方法、状态码、耗时与调用方(用户/IP/UserAgent),支持按时间范围与多种条件筛选,用于接口排障与审计。现状:已实现——「API 日志」页通过 REST `/logs/api` 查询(`queryApiLogs`,见 `frontend/admin-client/src/pages/logs/api/index.tsx`),筛选区含请求类型(HTTP/GraphQL)、请求方法、用户 ID、时间范围,列表分页(默认 20/页),行内「详情」弹窗展示完整请求体与错误信息;状态列按 2xx/4xx/5xx 着色。验收要点:关键接口均有记录;筛选正确。
**页面与入口**: API 日志页(logs/api)| 管理后台 | 入口:左侧导航「日志管理」→「API日志」。
**UI与交互**:
- **页面布局**:
  1. 筛选区(Card):请求类型 Select(HTTP/GraphQL)、请求方法 Select(GET/POST/PUT/DELETE)、用户 ID Input、时间范围 RangePicker(含时间)、「搜索」主按钮、「重置」按钮,均支持清除(allowClear)。
  2. 日志表格(Table):列依次为 时间(本地化 zh-CN)、类型(Tag:HTTP 蓝 / GraphQL 紫)、方法、状态(Tag:2xx 绿 / 4xx 橙 / 5xx 红)、URI(超长省略)、耗时(毫秒)、用户(用户名或 ID)、IP、操作(「详情」文字按钮);横向滚动 x=1200。
  3. 分页:默认 20/页,支持切换每页条数、快速跳页,页脚文案「共 N 条记录」。
  4. 详情弹窗(Modal「请求详情」):Descriptions 两列,含 请求类型/方法/URI/Query(等宽可滚动+复制按钮)/状态码/耗时/用户ID/用户名/IP/UserAgent/关联ID/操作类型/操作名称/请求体(JsonViewer:格式化、可折叠、可复制)/错误信息(红色、可滚动+复制)/时间。
- **组件清单**: Card、Space、Select、Input、DatePicker.RangePicker、Button、Table、Tag、Modal、Descriptions(均 antd;页面 `logs/api/index.tsx`)。
- **关键交互流程**:
  1. 进入页面 → 按默认第 1 页/20 条请求 `/logs/api` → 渲染列表与总数。
  2. 设置任一筛选条件(可组合)→ 点击「搜索」→ 回到第 1 页重新请求。
  3. 点击「重置」→ 清空全部筛选并回到第 1 页重新加载。
  4. 点击某行「详情」→ 打开弹窗展示该条完整请求信息。
  5. 翻页 / 切换每页条数 → 按新分页参数重新请求。
- **状态呈现**:
  - 空态:无匹配记录时表格展示 antd 默认空态;
  - 加载态:请求期间 Table 置 loading(转圈)并复用旧数据;
  - 错误态:请求失败仅 `console.error`,无前端用户提示(现状缺陷,登记缺口 G74);
  - 禁用态:请求期间「搜索」/「重置」无显式禁用(现状)。
- **校验与文案**:
  - 筛选条件均为可选,无必填与格式校验;时间范围缺省不限;
  - 分页页脚「共 N 条记录」;耗时列无值显示「-」;
  - 状态码着色规则:2xx 绿、4xx 橙、5xx 红、其余 default。
**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-LOG-001-01 | 已登录后台且存在 API 日志 | 进入「日志管理-API日志」 | 默认加载第 1 页(20/页),表格展示 时间/类型/方法/状态/URI/耗时/用户/IP,页脚显示「共 N 条记录」 |
| AC-LOG-001-02 | 存在 HTTP 与 GraphQL 记录 | 请求类型选「HTTP」→ 点击「搜索」 | 仅返回 HTTP 记录,类型列显示蓝色 HTTP Tag |
| AC-LOG-001-03 | 存在不同状态码记录 | 查看状态列渲染 | 2xx 显示绿 Tag、4xx 橙 Tag、5xx 红 Tag |
| AC-LOG-001-04 | 日志总数 > 20 | 点击分页第 2 页 | 请求携带 page=1,展示下一页数据,页脚总数不变 |
| AC-LOG-001-05 | 存在含 requestBody 的记录 | 点击某行「详情」 | 弹窗「请求详情」展示 请求体/错误信息/关联ID 等完整字段,请求体可滚动 |
| AC-LOG-001-06 | 已进入页面 | 选择最近 1 天时间范围 → 点击「搜索」 | 仅返回该时间窗内的记录 |
| AC-LOG-001-07 | 存在 4xx/5xx 记录 | 按用户 ID 输入后「搜索」 | 仅返回该用户发起且匹配的记录,状态着色正确 |
| AC-LOG-001-08 | 已设置多个筛选条件 | 点击「重置」 | 全部筛选清空,回到第 1 页重新加载全部日志 |

**规则引用**: —(本域无自有规则,遵循 01-overview.md §非功能(日志审计))。
**业务规则扩充**:
- 记录范围:需求为「关键接口均有记录」;现状按请求类型(HTTP/GraphQL)统一记录,不区分业务优先级;
- 耗时口径:durationMs 为服务端请求处理耗时,列表以毫秒展示;
- 时间准确:createdAt 以服务端时间为准,前端仅做本地化展示(zh-CN),不换算时区;
- 开放问题——状态筛选(见 §4):需求「按时间与状态筛选」中状态维度当前查询参数层已定义 `responseStatus`,但页面筛选区未暴露,无法按 4xx/5xx 快速定位异常接口。
**缺口关联**: —(切片声明本域无新增缺口;疑似 UI 口径差异见本文件 §4 开放问题,不占用 G 编号)。

#### F-LOG-002 用户操作日志 [已实现]
**需求描述**: 记录用户关键操作(登录/下单/取消/支付/地址变更),含操作人、时间、操作对象,供行为追溯与审计。现状:已实现——「用户操作」页通过 REST `/logs/user-actions` 查询(`queryUserActionLogs`,见 `frontend/admin-client/src/pages/logs/user-actions/index.tsx`),按操作类型、用户 ID/用户名/目标、时间范围筛选;表格展示 时间/操作类型/用户/页面/目标/元素ID/IP,详情弹窗含元素类型/元素文本/附加属性(格式化 JSON 视图,`components/JsonViewer`,可折叠/着色/复制)。上报侧:用户操作上报能力已封装为跨端 SDK `@aquarush/common/tracking`(平台无关核心 + 各端注入 `TrackingAdapter`,队列缓冲/定时 flush/PAGE_VIEW 去重/无 token 暂存/失败静默重入队均由 SDK 承担),三端统一经 `POST /api/logs/user-actions/batch` 批量上报:
- **user-weapp(小程序,已接入)**:页面浏览经 `usePageViewTracking`(19 页全覆盖,页面名映射 `src/config/tracking.ts`)上报 PAGE_VIEW;按钮/元素经 `data-track-*` attr 标记(或 `Track` 包装组件)上报 CLICK/FORM_SUBMIT/SEARCH;适配器注入 Taro 存储/路由,无 token 暂存,登录成功/切后台(useDidHide)flush;
- **admin-client(管理后台 PC 端,已接入)**:页面浏览经 `usePageViewTracking`(Layout 内路由变化上报)上报 PAGE_VIEW;登录成功/退出登录上报 CLICK(login-submit/logout);切后台(visibilitychange)flush;
- **delivery-app(配送 RN 端,已接入)**:页面浏览经 `usePageViewTracking`(expo-router 路由变化上报)上报 PAGE_VIEW;登录/登出、订单接单/开始配送/确认送达上报 CLICK(login-submit/logout/order-accept/order-start/order-complete);切后台(AppState)flush;后端 `UserActionEventService.saveToDatabase` 支持显式字段(pageUrl/elementId/elementType/elementText)与 properties 回退提取,properties pretty-print 落库并按配置剔除敏感键。说明:当前埋点枚举以页面浏览/点击等前端行为为主(PAGE_VIEW/CLICK/FORM_SUBMIT/SEARCH/SHARE/SCROLL/FILE_OPERATION),与需求所列业务操作(登录/下单/取消/支付/地址变更)存在口径差异,见 §4 开放问题。验收要点:操作可追溯;时间准确。
**页面与入口**: 用户操作日志页(logs/user-actions)| 管理后台 | 入口:左侧导航「日志管理」→「用户操作」。
**UI与交互**:
- **页面布局**:
  1. 筛选区(Card):操作类型 Select(页面浏览/点击/表单提交/搜索/分享/滚动/文件操作,各带色 Tag;含用户登录/登出、系统配置保存/恢复等后端自定义类型)、用户 ID Input、用户名 Input、目标 Input(匹配 target 列)、时间范围 RangePicker(含时间)、「搜索」/「重置」按钮。
  2. 操作表格(Table):列依次为 时间(zh-CN)、操作类型(Tag:页面浏览蓝/点击绿/表单提交橙/搜索紫/分享青/滚动品红/文件操作红)、用户(用户名或 ID)、页面(pageUrl 超长省略)、目标(target 省略)、元素 ID、IP、操作(「详情」文字按钮)。
  3. 分页:默认 20/页,支持切换每页条数、快速跳页,页脚「共 N 条记录」。
  4. 详情弹窗(Modal「操作详情」):Descriptions 两列,含 操作类型/用户ID/用户名/IP/页面URL/目标/元素ID/元素类型/元素文本/UserAgent(可滚动)/附加属性(JsonViewer:格式化、可折叠、语法着色、可复制)/时间。
- **组件清单**: Card、Space、Select、Input、DatePicker.RangePicker、Button、Table、Tag、Modal、Descriptions(均 antd;页面 `logs/user-actions/index.tsx`)。
- **关键交互流程**:
  1. 进入页面 → 按默认第 1 页/20 条请求 `/logs/user-actions` → 渲染列表与总数。
  2. 选择操作类型 / 输入用户 ID、用户名 / 选择时间范围(可组合)→ 点击「搜索」→ 回到第 1 页重新请求。
  3. 点击「重置」→ 清空全部筛选并回到第 1 页。
  4. 点击某行「详情」→ 弹窗展示该条操作的元素级信息与附加属性。
  5. 翻页 / 切换每页条数 → 按新分页重新请求。
- **状态呈现**:
  - 空态:无匹配记录时表格展示 antd 默认空态;
  - 加载态:请求期间 Table 置 loading;
  - 错误态:请求失败仅 `console.error`,无前端用户提示(现状缺陷,登记缺口 G74);
  - 禁用态:请求期间「搜索」/「重置」无显式禁用(现状)。
- **校验与文案**:
  - 筛选条件均为可选,无必填校验;时间范围缺省不限;
  - 分页页脚「共 N 条记录」;用户列无值显示「-」;
  - 操作类型枚举映射见上文组件清单,未命中枚举显示原值 + default 色。
**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-LOG-002-01 | 已登录后台且存在用户操作日志 | 进入「日志管理-用户操作」 | 默认加载第 1 页,表格展示 时间/操作类型/用户/页面/目标/元素ID/IP,页脚显示总数 |
| AC-LOG-002-02 | 存在多种操作类型记录 | 操作类型选「表单提交」→ 点击「搜索」 | 仅返回 FORM_SUBMIT 记录,操作类型列显示橙色「表单提交」Tag |
| AC-LOG-002-03 | 存在指定用户名记录 | 用户名输入关键字 → 点击「搜索」 | 仅返回该用户名的记录 |
| AC-LOG-002-04 | 日志总数 > 20 | 切换每页条数或翻页 | 分页正确请求,页脚总数一致 |
| AC-LOG-002-05 | 存在含元素信息的记录 | 点击某行「详情」 | 弹窗「操作详情」展示 页面URL/目标/元素ID/元素类型/元素文本/附加属性 |
| AC-LOG-002-06 | 已进入页面 | 选择最近 1 天时间范围 → 点击「搜索」 | 仅返回该时间窗内的记录 |
| AC-LOG-002-07 | 已进入页面 | 点击「重置」 | 全部筛选清空,回到第 1 页重新加载 |
| AC-LOG-002-08 | 存在记录 | 查看某条记录时间列 | 时间为本地化 zh-CN 格式,与记录产生时刻一致 |

**规则引用**: —(本域无自有规则,遵循 01-overview.md §非功能(日志审计))。
**业务规则扩充**:
- 追溯口径:操作人取 username 或 userId 兜底,对象取 target/页面 URL/元素 ID 三要素,保证「操作人、时间、对象」可追溯;
- 时间准确:createdAt 以服务端时间为准,前端仅本地化展示,不做时区换算;
- 枚举口径差异:需求所列「登录/下单/取消/支付/地址变更」为业务操作维度,当前埋点枚举为前端行为维度(PAGE_VIEW/CLICK 等),二者未建立映射,是后续对齐项(见 §4 开放问题),不影响现有「操作可追溯」验收。
**缺口关联**: —(切片声明本域无新增缺口;疑似枚举口径差异与分页字段问题见 §4 开放问题,不占用 G 编号)。

#### F-LOG-003 业务日志 [已实现]
**需求描述**: 记录业务事件(订单状态流转/派单/对账/退款申请),供异常排查与审计,须与订单操作记录时间线一致;以用户为视角可查看该用户全部业务操作记录,时间线形式展示。现状:已实现——「业务日志」页经 GraphQL `businessLogs` 查询(`frontend/admin-client/src/pages/logs/business/index.tsx`,服务层 `services/log-graphql.ts`),按业务域、操作类型、日志级别(TRACE/DEBUG/INFO/WARN/ERROR)、用户 ID、时间范围筛选;操作类型已结构化落库(`business_logs.operation` 列,BizLogger 写入);页面支持「表格 / 时间线」视图切换,时间线以用户为视角展示其全部业务操作记录(恒 createdAt 倒序、分页 10/页);表格展示 时间/级别/操作类型/Logger/消息/用户/关联ID(correlationId),详情弹窗含完整消息与堆栈跟踪;REST `/logs/business` 控制器保留。F-LOG-003 与订单操作记录时间线一致(facts.md §3)。验收要点:状态流转均有日志;与订单操作时间线一致。
**页面与入口**: 业务日志页(logs/business)| 管理后台 | 入口:左侧导航「日志管理」→「业务日志」。业务操作记录仅在日志管理页查看,用户详情页不提供操作记录。
**UI与交互**:
- **页面布局**:
  1. 筛选区(Card):业务域 Select(订单/支付/配送/用户/认证,映射自后端 loggerName)、操作类型 Select(与业务域联动,登录/提交订单/查看产品等,未选域时展示全部)、日志级别 Select(TRACE default/DEBUG 绿/INFO 蓝/WARN 橙/ERROR 红)、用户 ID Input、时间范围 RangePicker(含时间)、「搜索」/「重置」按钮。
  2. 视图切换:Segmented「表格 / 时间线」,切换保留筛选,分页回第 1 页并按视图调整每页条数(表格 20/时间线 10)。
  3. 业务日志表格(Table):列依次为 时间(zh-CN)、级别(Tag 带色)、操作类型(中文文案,老数据回退解析 message 前缀)、Logger(省略)、消息(省略)、用户(用户名或 ID)、关联ID(correlationId,省略)、操作(「详情」文字按钮)。
  4. 时间线视图:AntD Timeline(mode="left",`components/BusinessLogTimeline`),条目含 操作类型(图标+色+中文文案)、级别 Tag、用户 Tag、消息(省略)、关联ID、本地化时间;恒按 createdAt 倒序;分页默认 10/页。
  5. 分页:表格默认 20/页,支持切换每页条数、快速跳页,页脚「共 N 条记录」;时间线固定 10/页。
  6. 详情弹窗(Modal「日志详情」):Descriptions 两列,含 日志级别(Tag)/时间/Logger/关联ID/用户ID/用户名/消息(以 `{`/`[` 开头且可解析时用 JsonViewer 格式化展示,否则保留原文 pre)/堆栈跟踪(pre、可滚动、可复制)。
- **组件清单**: Card、Space、Select、Input、DatePicker.RangePicker、Button、Table、Tag、Modal、Descriptions、Segmented、Pagination、Timeline、Empty(均 antd);`BusinessLogTimeline` 组件(`components/BusinessLogTimeline`,含域→操作类型映射 `DOMAIN_OPERATIONS` 与操作文案映射 `OPERATION_CONFIG`);GraphQL 查询文档与 Hook(`graphql/queries/log.graphql.ts`、`services/log-graphql.ts`)。
- **关键交互流程**:
  1. 进入页面 → 经 GraphQL `businessLogs` 按默认第 1 页/20 条请求 → 渲染列表与总数。
  2. 选择业务域 → 操作类型下拉联动为该域类型(未选域展示全部);再选操作类型/日志级别 / 输入用户 ID / 选择时间范围(可组合)→ 点击「搜索」→ 回到第 1 页重新请求。
  3. 点击「重置」→ 清空全部筛选并回到第 1 页。
  4. 点击某行「详情」→ 弹窗展示消息全文与堆栈跟踪。
  5. 切换「时间线」视图 → 以时间线展示当前筛选结果(恒 createdAt 倒序);按用户 ID 筛选后,即以该用户为视角查看其全部业务操作轨迹。
  6. 翻页 / 切换每页条数 → 按新分页参数重新请求。
- **状态呈现**:
  - 空态:无匹配记录时表格展示 antd 默认空态;时间线视图展示 Empty「暂无操作记录」;
  - 加载态:请求期间 Table/Timeline 置 loading(转圈)并复用旧数据;
  - 错误态:请求失败仅 `console.error`,无前端用户提示(现状缺陷,登记缺口 G74);
  - 禁用态:请求期间「搜索」/「重置」无显式禁用(现状)。
- **校验与文案**:
  - 筛选条件均为可选,无必填校验;时间范围缺省不限;
  - 分页页脚「共 N 条记录」;级别 Tag 色:TRACE default / DEBUG 绿 / INFO 蓝 / WARN 橙 / ERROR 红;
  - 关联ID 列用于将业务日志与订单操作时间线、请求链(correlationId)关联;
  - 时间范围提交格式 `YYYY-MM-DDTHH:mm:ss`(本地时间,弃用 `.toISOString()` 的 UTC 偏移,消除与后端 LocalDateTime 解析不兼容的隐患,关联 G40);
  - 时间线恒按 createdAt 倒序。
**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-LOG-003-01 | 已登录后台且存在业务日志 | 进入「日志管理-业务日志」 | 默认加载第 1 页,表格展示 时间/级别/Logger/消息/用户/关联ID,页脚显示总数 |
| AC-LOG-003-02 | 存在 ERROR 级记录 | 日志级别选「ERROR」→ 点击「搜索」 | 仅返回 ERROR 记录,级别列显示红色 ERROR Tag |
| AC-LOG-003-03 | 已知 Logger 名称 | Logger 名称输入关键字 → 点击「搜索」 | 仅返回名称匹配该关键字的记录 |
| AC-LOG-003-04 | 某订单发生过状态流转 | 按该订单的关联ID筛选并查看 | 可依据关联ID(correlationId)将该单流转日志与订单操作时间线对应,流转均有日志 |
| AC-LOG-003-05 | 存在含异常堆栈的记录 | 点击某条 ERROR 记录「详情」 | 弹窗「日志详情」展示完整消息与堆栈跟踪(可滚动) |
| AC-LOG-003-06 | 已进入页面 | 选择最近 1 天时间范围 → 点击「搜索」 | 仅返回该时间窗内的记录 |
| AC-LOG-003-07 | 日志总数 > 20 | 翻页或切换每页条数 | 分页正确请求,页脚总数一致 |
| AC-LOG-003-08 | 已设置筛选条件 | 点击「重置」 | 全部筛选清空,回到第 1 页重新加载 |
| AC-LOG-003-09 | 已登录后台且存在业务日志 | 进入「日志管理-业务日志」→ 切换「时间线」视图 | 以时间线展示当前筛选结果,条目按 createdAt 倒序,含 级别、操作类型图标/文案、用户、时间;分页正确 |
| AC-LOG-003-10 | 存在多个业务域记录 | 业务域选「订单」→ 点击「搜索」 | 仅返回订单域记录(module=ORDER),操作类型下拉联动显示该域类型 |
| AC-LOG-003-11 | 存在订单域多类型记录 | 业务域「订单」+ 操作类型选「提交订单」→ 点击「搜索」 | 仅返回该操作类型的记录 |
| AC-LOG-003-12 | 存在指定用户的多条记录 | 时间线视图下输入用户 ID → 点击「搜索」 | 时间线仅展示该用户全部业务操作轨迹,结果与表格视图一致 |
| AC-LOG-003-13 | 已进入页面 | 时间范围选择后提交 | 请求参数为 `YYYY-MM-DDTHH:mm:ss` 格式,仅返回该时间窗内记录 |

**规则引用**: —(本域无自有规则,遵循 01-overview.md §非功能(日志审计);时间线一致性引用 02-business-flows.md §配送履约流程)。
**业务规则扩充**:
- 状态流转留痕:每笔订单状态变更均写入业务日志,经 correlationId 可与订单操作记录时间线(后台订单详情时间线)对齐,满足「状态流转均有日志、与订单操作时间线一致」;
- 排查与审计:ERROR 级日志携带堆栈跟踪,可据 Logger 名称与关联ID 定位异常来源;
- 级别口径:日志级别由后端日志框架写入,前端仅做筛选与着色展示,不参与级别判定;
- 业务域与操作类型口径:业务日志按 业务域 + 操作类型 两维度组织——业务域映射自后端 module(`BizLogger.logOperation` 的 module 参数,落库为 loggerName 列,含 ORDER/PAYMENT/DELIVERY/USER/AUTH/PRODUCT/MARKETING/OPS/RECONCILIATION);操作类型映射自 operation 参数(BizLogger 已封装 logOrderOperation/logPaymentOperation/logDeliveryOperation/logUserOperation/logProductOperation/logMarketingOperation/logOpsOperation/logLogin 等,并在各业务 Service 的写操作埋点:订单创建/取消/状态变更/支付成功、支付发起/完成/失败/退款/超时、派单/接单/开始/完成配送、配送员与配送区域管理、用户/地址/管理员管理、产品管理、营销活动/券/水票/分组定价、运营配置(OPS_*,经 OpsConfigAuditService 与 user_action_logs 审计并存)、对账(每日收款提交/确认/复核/执行),登录区分 USER_LOGIN/ADMIN_LOGIN/LOGIN_FAILED),operation 结构化落库(`business_logs.operation` 列,迁移 v1.1.1 新增;老数据为 NULL,前端时间线回退解析 message 前缀 `Operation: X`);枚举可扩展;
- 数据源:业务日志经 GraphQL `businessLogs`(admin 角色,PERMISSION_LOG_READ)查询,输入含 domain(业务域)/operationType(操作类型)维度,后端按域映射 loggerName 过滤、按 operationType 精确匹配 operation 列,恒按 createdAt 倒序,PageInfo 分页;REST `/logs/business` 控制器保留;
- 分页口径:业务日志页经 GraphQL 按 PageInfo 字段读取(修复 REST 分页字段不一致,见 G40,F-LOG-003 侧消除,用户操作日志侧保留)。
**缺口关联**: —(G43 已解决:时间线视图与业务域/操作类型筛选已实现)。

## 4. 交叉引用

- 状态机:见 02-business-flows.md §订单状态机、§配送履约流程(F-LOG-003 记录订单状态流转与履约事件,时间线与订单操作记录一致)。
- 缺口:见 14-misc.md §G01-G17(本域无关联缺口)。
- 实体:见 14-misc.md §核心实体(操作日志)。
- 异常:见 14-misc.md(异常信息经业务日志 ERROR 级记录的堆栈跟踪落库)。
- 全局金额精度:见 01-overview.md §全局约束(本域不涉及金额)。
- 非功能审计:见 14-misc.md §安全与合规(日志审计要求)。
