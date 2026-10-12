---
feature: admin-radix-ui
status: delivered
updated: 2026-10-12
branch: feat/admin-radix-ui
commits: 613eb544..d1d1d789
---

# admin-client 全量迁移 Radix + StyleX（移除 antd）

## Report

**What was built** — admin-client 从 antd 6 全量迁移到自研 `src/ui/` 内部组件库(60+ 文件):
Radix headless 行为 + StyleX 外观 + antd 同形 API,含 TanStack v9 表壳、react-hook-form
表单门面(Form.useForm 元组/validateFields reject 形状)、自研 message toast(组件树外可用)、
react-day-picker 日期/区间选择(DatePicker.RangePicker/TimePicker.RangePicker 静态)、
77 枚本地 SVG 图标(路径数据固化,零运行时依赖)。187 个调用文件改走 `@/ui` / `@/ui/icons`,
antd 与 @ant-design/icons 从依赖、main.tsx(ConfigProvider/locale/ANTD_THEME_TOKEN)、
index.css(.ant-*)、@aquarush/common/theme/antd.ts 中全部移除。迁移按波次并行执行
(基建 → 非页面模块 → 4 波页面批次 → 移除),独立评审发现并修复了 2 个运行时 CRITICAL
(Form.Item 链式回调子 onChange、Table 非受控分页),随后 jest 升级 jsdom+tsx+stylex 补齐
DOM 级回归与验收测试。并行会话贡献的 ArchUnitTS 组件行数约束(fc640bbd)经用户确认纳入本
feature。已知非阻塞偏差(经评审归档):preserve=false 近似、toast 同屏上限 3 条、
Modal 关闭即卸载(destroyOnClose 语义名保留)、group-prices 尺寸切换为既有死控件。

**Verification** — 命令与结果(head d1d1d789):
- `pnpm --filter @aquarush/admin-client build`(tsc -b + vite + StyleX 抽取)→ PASS
- `npx tsc --noEmit -p tsconfig.app.json` → 0 errors
- `npx jest`(admin-client)→ 14 suites / 151 tests PASS(含 icons 77 渲染 smoke、
  Table DOM onRow/ellipsis/scroll/fixed/受控与非受控分页、Form.Item 链式 onChange DOM
  回归、架构行数约束、订单/系统设置/统计三页挂载走查)
- `pnpm test:frontend` → 4 包共 433 tests PASS
- `npx eslint .` → 95 problems(90 errors / 5 warnings),规则类别恰为基线 9 类
  (基线 96 / 91E / 5W / 9 类),无新增类别
- antd 残留:src 内仅注释;package.json 与 lockfile 无 antd/@ant-design/icons
- 运行时冒烟:vite preview `/login` 渲染、React 挂载、0 console 错误;像素级视觉走查
  因环境限制(IAB compositor 空帧、evaluate 只读无法伪造登录态)降级为 jsdom 三页挂载
  走查 + `/login` 浏览器冒烟,**像素级样式核对留作人工 QA**
- 评审:两轮独立评审。Round1(613eb544..d1e66fb4)报 2 CRITICAL → 修复提交 d1d1d789;
  复评(d1e66fb4..d1d1d789)逐项核验 = PASS,剩余 CRITICAL 0

**Journey log** —
1. spec 初稿断言「现网无 RangePicker」有误(实际约 20 处)→ S2.7 勘误并新增 T8b,
   提示:范围断言必须先 grep 再写死。
2. 图标清单曾被双引号 import 漏采(JsonEditor 的 FormatPainterOutlined)→ 全量双引号
   重扫,76→77 枚补齐;清单类 AC 要双向 diff(用集 ⊆ 导出集)。
3. 并行会话在共享 worktree 落了 ArchUnitTS 行数约束 + 15 组件拆分(fc640bbd);
   暂停上报后用户裁决「纳入本 feature」——共享 worktree 冲突先问再动。
4. 独立评审抓到 tsc/jest 全绿仍漏掉的两个 antd 语义回归(rc-field-form 链式回调
   originTriggerFunc、antd Table 非受控 current 内部态)→ 修复 + jest 升级
   jsdom/tsx/stylex 补 DOM 回归;纯逻辑单测无法覆盖 cloneElement/渲染期行为。
5. IAB 只读 evaluate + compositor 空帧:无法写 localStorage/截图 → 视觉走查降级为
   `/login` 冒烟 + jsdom 三页挂载;此类环境限制应在 spec 设计期就预设替代验证。

## [S1] Problem

`frontend/admin-client` 全站依赖 antd 6（187 个文件、约 301 处 `from 'antd'`、71 个文件用
`@ant-design/icons`，77 个不重复图标）。antd 是带样式的重库，与仓库已定的 StyleX 样式体系
（`agents/frontend.md` §StyleX 约定）双轨并行：主题经 `ANTD_THEME_TOKEN` 手工对齐、
`index.css` 里残留 `.ant-*` 覆盖、设计规范 `spec/design/ui-design-system.md` 无法约束
antd 内部结构。目标是换成 headless 的 Radix UI + StyleX 单一样式体系，并最终从依赖中
移除 antd 与 @ant-design/icons。

## [S2] Design

Grill 已定决策（用户确认）：

1. **范围**：一个 feature 内全量替换 187 个文件，最终移除 `antd` 与 `@ant-design/icons` 依赖。
2. **架构**：内部 primitives 库 `frontend/admin-client/src/ui/`，每个组件 = Radix headless
   行为（如适用）+ StyleX 外观；页面统一从 `@/ui` 导入。
3. **重型组件**：Table → TanStack Table；Form → react-hook-form；message → 自研 toast；
   DatePicker/TimePicker → react-day-picker + Radix Popover（+ 已有 dayjs）；图标 → 自建
   SVG 图标集（不引第三方图标库）。
4. **视觉**：以 `spec/design/ui-design-system.md` + `@aquarush/common/theme/tokens.stylex`
   为唯一基准（token 唯一导入路径、StyleX 全量 longhand 规则见 `agents/frontend.md`），
   布局与信息结构保持现状，观感允许向设计规范微调。

### [S2.1] 依赖变更

`frontend/admin-client/package.json`：

- 新增（具体包以实现期实际需要裁剪，但至少覆盖）：
  - `@radix-ui/react-dialog`（Modal / 图片预览灯箱）、`react-popover`（DatePicker 弹层）、
    `react-select`、`react-dropdown-menu`、`react-tooltip`、`react-tabs`、`react-switch`、
    `react-checkbox`、`react-radio-group`、`react-accordion`（Collapse）、`react-slider`、
    `react-progress`、`react-label`（FormItem label）、`react-slot`（asChild 组合）、
    `react-avatar`（头像 fallback）
  - `@tanstack/react-table`、`react-hook-form`、`react-day-picker`
- 移除（最后一步）：`antd`、`@ant-design/icons`
- 纯展示组件（Button/Card/Tag/Alert/Empty/Spin/Statistic/Descriptions/List/Breadcrumb/
  Divider/Grid 等）不引 Radix，StyleX + tokens 直接实现；侧边栏菜单（Layout.tsx 的
  Menu）自研 nav，不用 Radix NavigationMenu。

### [S2.2] 目录与导出契约

```
frontend/admin-client/src/ui/
  index.ts          # barrel，页面只从此导入
  icons/index.tsx   # 77 个自建 SVG 图标组件
  toast.ts(x)       # message API
  ...每个组件一个文件
```

- 图标：**保留 antd 图标原名**（`SearchOutlined` 等，含别名 `DeliveryCarOutlined`），
  import 路径改为 `@/ui/icons`；props 支持 `style`/`className`/`spin`，默认尺寸 `1em`、
  `currentColor`。图标集清单以迁移期 `rg "from '@ant-design/icons'"` 全量导出名为准
  （基准审计：77 个唯一名，见实现期生成的 `src/ui/icons/` 目录）。
- 页面侧 import 改写规则：`from 'antd'` → `from '@/ui'`；
  `from '@ant-design/icons'` → `from '@/ui/icons'`。

### [S2.3] 组件 API 形状（对齐 antd 以保证迁移机械性）

`src/ui` 组件对外 props 尽量保持 antd 同名同义，降低 187 个文件的改造风险：

- **Button**：`type="primary|default|dashed|text|link"`、`danger`、`loading`、`icon`、
  `size="small|middle|large"`、`onClick`。外观（高度/圆角/色板）来自 tokens.stylex。
- **Card**：`title`、`extra`、`actions`、`bordered`、`headStyle/bodyStyle`。
- **Tag**：`color`（antd 语义色名 + 自定义色）、`closable`、`onClose`。
- **Space**：`align`、`direction`、`size`、`wrap` → flex gap 实现。
- **Grid（Row/Col）**：`gutter`（数字|[水平,垂直]）、Col `span`、响应式
  `xs/sm/md/lg`（StyleX media query，断点同 antd：576/768/992/1200）。
- **Typography**：`Title/Text/Paragraph`（`type="secondary|danger"`、`strong`、`code`、
  `copyable`）。
- **Alert**：`type="success|info|warning|error"`、`message`、`description`、`showIcon`、
  `closable`。
- **Input / InputNumber / Textarea**：受控 `value/onChange`；InputNumber `min/max/
  precision/addonBefore/addonAfter`；均支持透传。
- **Select**：`options={[{value,label,disabled}]}`、`allowClear`、`placeholder`、
  `mode="multiple"`、`disabled`、`onChange`（值语义与 antd 一致）。
- **Switch / Checkbox / Radio(Radio.Group) / Segmented / Slider / Progress / Rate**：
  同名 props，行为 Radix（或自研）headless，外观 StyleX。
- **Modal**：`open`、`onOk`、`onCancel`、`confirmLoading`、`title`、`footer`、
  `width`、`destroyOnClose`、`maskClosable`；基于 Radix Dialog。
- **Popconfirm**：`title`、`description`、`onConfirm`、`onCancel`、`okText/cancelText`、
  `disabled`；Radix Popover + 内部按钮。
- **Tooltip / Dropdown / Tabs / Collapse(Accordion) / Breadcrumb / Pagination**：
  同名 props；Tabs `items=[{key,label,children}]` 与 antd v5+ 形状一致。
- **Upload**：迁移期仅 2 处（`components/ImageUpload`、`pages/addresses/
  components/AddressImport`），按 file-input + Radix 实现其用到的子集
  （`beforeUpload`/`onChange(info)` 返回 `status` 的调用形状保持，或在该两处直接
  改写为自定义上传 hook——以文件实际用法为准，保证行为等价）。
- **Empty / Spin / Divider / Avatar / Image（预览）/ List / Timeline / Descriptions /
  Statistic / Layout（Sider/Header/Content 布局）**：同名同 props 子集；Image 预览用
  Radix Dialog 灯箱。
- **Cascader**：仅 `components/RegionCascader` 一处，实现地区级联（选项树 + 多级
  Popover/Select），props 按该调用点实际需要定，不追求 antd 兼容。

### [S2.4] Form 契约（react-hook-form 之上的 antd 形状门面）

- `useForm<T>()`（`@/ui` 导出，替代 `Form.useForm()`）返回门面，**必须支持**现有调用
  面（审计）：`getFieldsValue()`、`setFieldsValue(v)`、`setFieldValue(name, v)`、
  `resetFields()`、`validateFields(): Promise<T>`（校验失败 reject，形状与 antd 一致，
  页面 `try/catch` 逻辑不改）。内部为 react-hook-form `useForm`。
- `<Form form={form} layout="horizontal|vertical" initialValues onFinish onValuesChange>`
  与 `<Form.Item name label rules required extra>`（无 `name` 时为纯布局容器）。
- `rules` 支持现有子集：`{ required, message, min, max, len, pattern, type:
  'string'|'number'|'email'|'url' }`，错误文案中文（与现网 antd zh-CN 默认一致或
  更贴近语境）；`Form.Item` 负责 label 关联、错误展示、`required` 星号。
- 校验时机：默认 blur/change（同 antd `validateTrigger="onChange"` 近似），
  `validateFields` 强制全量校验。
- 值类型：InputNumber/Radio 等数值字段在 form state 中保持 `number`（RHF
  `valueAsNumber` 或门面转换），与 antd 一致，避免页面比较逻辑被改成字符串。
- `type FormInstance` 导入点（`LogRetentionForm.tsx` 等）改从 `@/ui` 导入门面类型。

### [S2.5] Table 契约（TanStack 核心 + antd 形状表壳）

```ts
interface Column<T> {
  title: ReactNode; dataIndex?: keyof T & string; key?: string;
  render?: (value: any, record: T, index: number) => ReactNode;
  sorter?: ((a: T, b: T) => number) | boolean;
  sortOrder?: 'ascend' | 'descend' | null;
  align?: 'left' | 'right' | 'center'; width?: number | string;
  fixed?: 'left' | 'right'; ellipsis?: boolean;
}
<Table<T>
  columns dataSource rowKey? loading? bordered? size? title? footer?
  pagination={{ current, pageSize, total, onChange, showTotal?, pageSizeOptions?, disabled? }}
  onRow? rowSelection={{ selectedRowKeys, onChange, preserveSelectedRowKeys? }}
  scroll={{ x?: number|string, y?: number|string }} locale?  // locale.emptyText
/>
```

- 内部用 `@tanstack/react-table`：本地 `sorter`（函数型）在表壳内排序；服务端分页
  经 `pagination.onChange` 回调（现状即如此），表壳不做数据获取。
- `rowSelection`（仅 orders 2 处）与 `onRow`（3 处）、`ellipsis`、`scroll.x/y`、
  `fixed` 列为必支持子集；不实现 expandable/sticky/virtual（现网未用）。
- `TableProps` type 导入点（4 个文件）改从 `@/ui` 导入 `TableProps<T>`。
- 空表格展示 `Empty`；`loading` 用 Spin 覆盖层。
- 行点击高亮等现走 `.ant-table-row.clickable-row` 的样式（index.css）改为 ui Table
  的 `onRow` + 内部 stylex class，`index.css` 中 `.ant-*` 规则删除。

### [S2.6] message → 自研 toast 契约

- 模块：`@/ui` 导出 `message` 对象，**纯函数调用、不要求 React 上下文**（审计：大量
  用于 `src/services/*.ts`、`lib/apollo-client.ts`、`utils/*`——必须在组件树外可用）。
- API 与 antd 对齐：`message.success/error/warning/info(content, duration?)` 返回
  `key`；`message.open({type,content,duration,...})`；`message.destroy(key?)`。
- 实现：模块内部维护队列 + `ReactDOM.createRoot` 懒挂载容器（或全局
  `createPortal` 到 body），自上而下堆叠、自动消退、可手动关闭；样式 StyleX +
  tokens；图标用 `@/ui/icons`。
- 主 App 树内挂载点不需要（模块自挂载），`main.tsx` 不为 toast 增加 Provider。

### [S2.7] DatePicker / TimePicker 契约

- `<DatePicker value={Dayjs|null} onChange={(d: Dayjs|null, str?) => void} format
  placeholder? disabled? disabledDate? allowClear? showTime? />`；内部
  react-day-picker（zh-CN locale 内置）+ Radix Popover，值域 dayjs（依赖已有）。
- `<TimePicker value format="HH:mm" ...>`：小时/分钟选择（day-picker 的时间模式或
  受控 time input，以调用点 `format` 子集为准）。
- 现网同时存在 **RangePicker**（勘误：初稿"现网无 RangePicker"错误 —— `DatePicker.RangePicker`
  约 18 处 + `TimePicker.RangePicker` 2 处，含 `showTime`、`format='YYYY-MM'`、
  `placeholder=['开始日期','结束日期']`、`disabledDate`、`allowClear={false}`）。
  `RangePicker` 作为 `DatePicker.RangePicker` / `TimePicker.RangePicker` 静态成员实现，
  值类型 `[Dayjs|null, Dayjs|null]`，`onChange(dates, dateStrings)`，内置 [开始,结束]
  与「开始 ≤ 结束」归一（BannerFormModal/NoticeFormModal 等调用点自行归一到秒，行为保持）。
- `antd/locale/zh_CN` 仅服务 ConfigProvider，移除后中文文案（"今天/上一月/请选择"等）
  在 ui 内置。

### [S2.8] 迁移批次与移除

页面按目录分批迁移，每批验收：该批目录 `rg "from 'antd'"` 归零 + `pnpm --filter
@aquarush/admin-client build` 通过。批次顺序：

1. 基建（依赖、icons、toast、ui 展示类、overlay、Form、Table、picker 杂项）
2. `src/components` + `src/hooks` + `src/lib` + `src/services` + `src/utils` + `main.tsx/App`
3. `login` `dashboard` `addresses`
4. `orders` `payments`
5. `users`（30 文件，最大批）
6. `marketing`（24）
7. `statistics`（21）
8. `system` `ops` `products`
9. `logs` `notifications` `subscriptions` `barrels` `trajectories` `daily-collection`

**移除阶段**（最后）：

- `main.tsx`：删 `ConfigProvider`/`antd/locale`/`ANTD_THEME_TOKEN` 引入。
- `index.css`：删全部 `.ant-*` 规则（clickable-row、card 阴影、`.num`——`.num`
  语义由 ui Table/样式承接）。
- `@aquarush/common`：删 `theme/antd.ts` 与 barrel 导出 `ANTD_THEME_TOKEN`，
  同步删 `tokens.test.ts` 中 antd themeToken 对照 describe（COLORS 对照保留并改注释）；
  `colors.ts`/`stylex/props.ts`/`tracking/types.ts` 注释中的 antd 字样改为中性描述。
- `package.json` 移除 `antd`、`@ant-design/icons`；全仓 `rg "from 'antd'"` 在
  `frontend/admin-client` 下归零。

### [S2.9] 验证边界

- `pnpm --filter @aquarush/admin-client build`（tsc -b + vite build，StyleX 编译）
  必须通过。
- `pnpm --filter @aquarush/admin-client test`：现有 4 套件保持通过；为 ui 层补
  关键单测（toast 队列、Form 门面 validateFields/setFieldsValue、Table 排序/分页回调、
  icons smoke 渲染）。
- `pnpm --filter @aquarush/admin-client lint`：基线 96 problems（迁移前即存在，
  记 PRE-EXISTING），交付时不得新增错误类别/数量显著上升。
- `pnpm test:frontend`：全 workspace 回归。
- 视觉走查：dev server 起 admin 登录页 + 2~3 个代表页（订单列表/系统设置/统计）
  人工核对样式无破损（交付报告中记录；无法自动化）。

## [S3] Out of Scope

- `frontend/user-weapp`、`frontend/delivery-app`（无 antd；已用 StyleX/RN StyleSheet）。
- `spec/design/ui-design-system.md` 本身的修订（只消费不改写）。
- GraphQL schema / 后端任何改动。
- 页面信息架构、交互流程的重设计（迁移保持现状结构）。
- 引入第三方图标库或第三方 form/table 封装库（TanStack/RHF/react-day-picker 除外）。
- antd 相关但仓库其他位置的注释级清理可在移除阶段顺手做，但不追求全仓注释无 antd 字样。

## Tasks

- [x] T1: 依赖接线 — acceptance: admin-client package.json 含 Radix 子集 + TanStack +
  RHF + react-day-picker，`pnpm install` 后 `build`/`test` 通过且无行为变化 (covers: S2.1)
- [x] T2: 图标集 `src/ui/icons` — acceptance: 77 个图标组件齐备（名称与 antd 一致含
  别名），示例页 import 编译通过，svg 以 currentColor 渲染 (covers: S2.2)
- [x] T3: toast（message 门面）— acceptance: 组件树外 `message.success()` 可用，
  队列堆叠/自动消退/destroy 单测通过 (covers: S2.6; depends: T1)
- [x] T4: ui 展示类 primitives（Button/Card/Tag/Space/Grid/Typography/Alert/Empty/
  Spin/Divider/Avatar/List/Statistic/Descriptions/Breadcrumb/Pagination/Image）—
  acceptance: `@/ui` 导出可编译，样式全 StyleX + tokens，组件单测/构建通过 (covers:
  S2.1, S2.3; depends: T1, T2)
- [x] T5: ui 交互类（Modal/Popconfirm/Tooltip/Dropdown/Tabs/Collapse/Menu 导航/
  Segmented/Switch/Checkbox/Radio/Rate/Slider/Progress/Upload 子集）— acceptance:
  Radix headless 行为 + StyleX 外观，键盘/焦点可达，构建通过 (covers: S2.1, S2.3;
  depends: T4)
- [x] T6: Form 门面 + Input/InputNumber/Select/Cascader(RegionCascader 所需) —
  acceptance: S2.4 全部 API 有单测（validateFields reject、setFieldsValue、resetFields、
  数值类型保持 number），构建通过 (covers: S2.4; depends: T4)
- [x] T7: Table（TanStack 表壳）— acceptance: 列渲染/本地 sorter/服务端分页回调/
  rowSelection/onRow/ellipsis/scroll/fixed 子集有单测，4 个 `TableProps` 导入点类型
  可替换 (covers: S2.5; depends: T4)
- [x] T8: DatePicker/TimePicker — acceptance: zh-CN 日历、受控 dayjs 值、format 输出
  正确，构建通过 (covers: S2.7; depends: T5)
- [x] T8b: RangePicker（DatePicker.RangePicker + TimePicker.RangePicker 静态成员）—
  acceptance: ~20 处 RangePicker 调用点所需 props 全覆盖（showTime/YYYY-MM 格式/
  placeholder 数组/disabledDate/allowClear），picker-core 增补区间逻辑单测，构建通过
  (covers: S2.7; depends: T8)
- [x] T9: 迁移非页面模块（components/hooks/lib/services/utils/main/App）— acceptance:
  该集合 `rg "from 'antd'"` 归零，build/test 通过 (covers: S2.2, S2.8; depends:
  T3,T5,T6,T7,T8)
- [x] T10: 迁移 login/dashboard/addresses — acceptance: 目录 antd import 归零，
  build 通过 (covers: S2.8; depends: T9)
- [x] T11: 迁移 orders/payments — acceptance: 同上 (covers: S2.8; depends: T9;
  与 T10 同波并行)
- [x] T12: 迁移 users（30 文件）— acceptance: 同上 (covers: S2.8; depends: T10,T11)
- [x] T13: 迁移 marketing（24 文件）— acceptance: 同上 (covers: S2.8; depends: T10,T11;
  与 T12 同波并行)
- [x] T14: 迁移 statistics（21 文件）— acceptance: 同上 (covers: S2.8; depends: T12,T13)
- [x] T15: 迁移 system/ops/products — acceptance: 同上 (covers: S2.8; depends: T12,T13;
  与 T14 同波并行)
- [x] T16: 迁移 logs/notifications/subscriptions/barrels/trajectories/daily-collection
  — acceptance: 全 src `rg "from 'antd'|from '@ant-design/icons'"` 归零（main.tsx 除外，
  其由 T17 处理）(covers: S2.8; depends: T14,T15)
- [x] T17: 移除 antd — acceptance: package.json 无 antd/@ant-design/icons；main.tsx 无
  ConfigProvider；index.css 无 .ant-*；common 的 ANTD_THEME_TOKEN 及其测试删除且
  `pnpm test:frontend` 通过 (covers: S2.8; depends: T16)
- [x] T18: 全量验证 — acceptance: build/test/lint/test:frontend 命令与结果记录，
  lint 无新增问题类别，ui 层新增单测通过，视觉走查记录在 Report (covers: S2.9;
  depends: T17)
