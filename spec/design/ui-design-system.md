# 奔泉（AquaRush）三端 UI 设计系统（Design System）

> 文档版本：V1.1 ｜ 适用范围：user-weapp（小程序）、admin-client（管理后台）、delivery-app（配送端）
>
> **单一事实来源**：本文件（权威路径 `spec/design/ui-design-system.md`）是品牌级设计系统的唯一权威定义（色彩 / 字体 / 间距 / 圆角 / 阴影 / 通用组件 / 禁忌清单）。三端各自的页面级规范见：
> - `frontend/user-weapp/docs/ui-spec.md`
> - `frontend/admin-client/docs/ui-spec.md`
> - `frontend/delivery-app/docs/ui-spec.md`
>
> 设计稿体系（90+ 张 HTML 稿）以本文件为视觉同源依据：`spec/design/index.html` + `spec/design/_spec/_CONTRACT.md`，token 镜像见 `spec/design/assets/tokens.css`。
>
> 关联 PRD：`spec/prd/00-PRD.md`、`spec/prd/03-product.md`（商品）、`spec/prd/04-order.md`（订单）、`spec/prd/07-user.md`（用户端）、`spec/prd/12-operations-config.md`（运营配置）。

---

## 1. 品牌与设计语言

| 维度 | 规范 |
|------|------|
| **整体调性** | 清水蓝 + 卡片式 + 便民电商，传递「干净、专业、快」的品牌感知 |
| **视觉语言** | 扁平化设计，全局大圆角卡片（圆角 ≥ 16px） |
| **分区方式** | 弱化线条边框，依靠**留白**（卡片间距 ≥ 12px）和**浅投影**（`0 4px 16px rgba(0,0,0,0.04)`）区分内容区块 |
| **商品图规范** | ⚠️ **严禁**使用卡通插画/扁平图标。必须使用**真实场景摆拍图**（高档办公室角落、现代家庭客厅、水站实拍），传递「实体水站、真实现货」的信任感。图片风格统一为明亮自然光、高清晰度 |

### 1.1 三端定位差异

| 端 | 产品定位 | 设计侧重 | 模式 |
| --- | --- | --- | --- |
| **user-weapp** | 消费者下单小程序 | 首屏最短购买路径、信任感（真实水站/实拍） | 仅亮色 |
| **admin-client** | 管理后台（Ant Design 5） | 高密度数据表格、表单、状态流转；深色侧边栏 + 浅色内容区 | 仅亮色 |
| **delivery-app** | 送水员作业端（Expo RN） | 信息密度优先、触达目标 ≥48、亮色为主 + 暗色可读（夜间作业） | 亮/暗双模式（默认亮色） |

> 三端共享品牌 token；**强调橙 `#FF5436` 仅限消费者端最高优先级 CTA**，管理端/配送端主操作走品牌蓝、危险操作走语义 error 色（见 §2.1 红线）。
>
> **小程序端（user-weapp）组件体系**：严格按微信官方 **WeUI 2.x** 实现，不自造组件。44px 导航栏 + 87×32 微信胶囊、原生 TabBar（50px / 图标 27px / 文字 10px / 底 `#F7F7F7`）、页面底 `#EDEDED`、单元格 17px 主文字与 0.5px 内缩 16px 发丝线，官方 Cells / Grids / Loadmore / Actionsheet / HalfScreen / Dialog / Toast。主题色经 `--weui-BRAND` 注入品牌蓝 `#3A9BD9`（不提供主题切换，见 F-USR-004，`spec/prd/07-user.md` 已标 [已移除]）。

---

## 2. 色彩 Token

### 2.1 品牌色板（三端共享）

| 角色 | 色值 | 使用场景 |
|------|------|----------|
| **主色** | `#4DB6E5`（浅蓝） / `#3A9BD9`（湖蓝） | 顶部导航背景、选中态、轻量图标、标签底色、空状态插图、主按钮（湖蓝） |
| **渐变** | `#4DB6E5 → #3A9BD9`（135°） | 品牌 Banner、登录/页头渐变背景 |
| **基底色-灰** | `#F5F7FA` | 页面整体背景 |
| **基底色-白** | `#FFFFFF` | 卡片背景、弹窗背景 |
| **文字-主** | `#1A1A1A` | 标题、核心信息 |
| **文字-辅** | `#8C8C8C` | 描述文字、次要信息 |
| **文字-弱** | `#B8B8B8` | 占位符、禁用态文字 |
| **强调色** | `#FF5436`（橙红） | ⚠️ 消费者端最高优先级 CTA 按钮：`[立即订水]` `[立即购买]` `[催单]` `[提交订单]`；**优惠券品牌色**（券卡金额/类型、领券按钮、券选择器、券图标）同值 |
| **金额色（小程序端专用）** | `#FA5151`（微信电商红） | 小程序端**金额专用**：商品价、划线原价、优惠/减免金额、实付合计、券面额、账户余额。仅用于金额数字，**不用于按钮与图标** |
| **语义色** | `#52C41A` success / `#FAAD14` warning / `#FF4D4F` error | 状态、校验、危险操作 |

> **强调色使用红线**：禁止用于装饰元素、普通图标、分割线、非按钮文字。全页面强调色出现次数 ≤ 3 处/屏（消费者端）。**例外：优惠券**——消费者端优惠券为品牌橙红（券卡金额/类型标签、领券按钮、券选择器选中态/最优标记/金额、券入口图标），不受「仅限 CTA」限制。
>
> **两个红色不要混用**：`#FF5436` 强调橙是「**行为色**」（CTA 按钮 / 券元素），`#FA5151` 微信电商红是「**数据色**」（金额数字）。小程序端金额一律用 `#FA5151`（`.weui-price`），CTA 一律用 `#FF5436`；管理端与配送端两者都不出现（金额走品牌蓝加粗）。

### 2.2 语义色与状态标签（StatusTone 体系）

- 订单/业务状态一律用「10% tint 底 + 状态色文字」的标签表达。
- **唯一事实来源**：`frontend/common/src/utils/status-tone.ts` —— 与平台无关的语义 tone（6 种）+ `STATUS_TONE_COLORS` 色对，以及各业务域的「状态 → tone」映射（`ORDER_STATUS_TONE`、`FEEDBACK_STATUS_TONE`、`COUPON_STATUS_TONE`、`TICKET_STATUS_TONE`）。**新增业务状态必须先在该模块增加映射，页面不得自配色**。
- 三端渲染通道：admin-client 经 `toneToAntDColor`（AntD preset 色名）；user-weapp 经 `StatusTag` 组件 + `--status-*` CSS 变量（守卫测试锁定）；delivery-app 经 `StatusTints`（light 取自 `STATUS_TONE_COLORS`，dark 本地适配）。

| tone | text / bg | 典型状态 |
| --- | --- | --- |
| info | `#3A9BD9` / `rgba(58,155,217,0.10)` | 待派单 / 待配送、券/票未使用 |
| success | `#389E0D` / `rgba(82,196,26,0.10)` | 配送中、已处理 |
| warning | `#FA8C16` / `rgba(250,173,20,0.10)` | 待付款、待处理、已退回 |
| neutral | `#8C8C8C` / `rgba(140,140,140,0.10)` | 已完成、已过期 |
| error | `#CF1322` / `rgba(255,77,79,0.10)` | 已取消 / 已退款 |
| accent | `#FF5436` / `rgba(255,84,54,0.10)` | 消费者端强调态（订单时间线「支付成功」等）；**管理端/配送端禁用**（§2.1 红线） |

订单状态 → tone（状态机见 `spec/prd/02-business-flows.md`）：待付款 warning ｜ 待派单·待配送 info ｜ 配送中 success ｜ 已完成 neutral ｜ 已取消·已退款 error。

- 标签尺寸：圆角 8，内边距 4×8，字号 12（sm 档，对应 weapp `.theme-tag` / tokens.css `--st-*`）。
- 亮色端 tint 统一 10%；delivery-app 暗色模式用 16% tint + 提亮文字保证夜间可读（tone 语义不变）。

### 2.3 三端 Token 落地（V1.1 迁移已完成）

#### user-weapp（CSS 变量，载体 `src/styles/theme.scss`）

| Token | 值 | 用途 |
| --- | --- | --- |
| `--brand-primary` | `#3A9BD9` | 主按钮、选中态、链接 |
| `--brand-primary-light` | `rgba(58,155,217,0.10)` | 浅蓝 tint 底 |
| `--brand-primary-dark` | `#2E7FB8` | 按压态 |
| `--brand-gradient-start` / `--brand-gradient-end` | `#4DB6E5` / `#3A9BD9` | Banner / 页头渐变（135°） |
| `--action` | `#FF5436` | 消费者端 CTA（仅此，§2.1 红线） |
| `--coupon-primary` / `--coupon-primary-light` | `#FF5436` / `rgba(255,84,54,0.10)` | 优惠券品牌色 |
| `--success` / `--warning` / `--error` | `#52C41A` / `#FAAD14` / `#FF4D4F` | 语义色 |
| `--text-main` / `--text-sub` / `--text-weak` | `#1A1A1A` / `#8C8C8C` / `#B8B8B8` | 主 / 辅 / 弱文字 |
| `--bg-card` / `--bg` | `#FFFFFF` / `#F5F7FA` | 卡片 / 页面背景 |
| `--border` | `rgba(0,0,0,0.06)` | 发丝线 / 弱边框 |
| `--status-*`（10 对） | §2.2 | 状态标签文字 / 底色 |
| `--spacing-*` / `--radius-*` / `--shadow-*` | §3 | 间距 / 圆角 / 阴影 |

> 旧 `--theme-*` 名称以**别名**形式保留（指向品牌值），迁移期**勿再新增引用**；存量引用替换完成后删除别名块。主题切换（原 aqua/blue/green/purple 预设 + ThemeProvider）已移除（F-USR-004 → `spec/prd/07-user.md` [已移除]）。

#### admin-client（AntD 5 `ConfigProvider theme`，载体 `src/main.tsx` `themeToken`）

| AntD token | 值 | 说明 |
| --- | --- | --- |
| `colorPrimary` | `#3A9BD9` | 主按钮、链接、选中态 |
| `colorLink` | `#3A9BD9` | 链接 |
| `colorBgLayout` | `#F5F7FA` | 页面背景 |
| `colorBgContainer` | `#FFFFFF` | 卡片/容器背景 |
| `colorText` | `#1A1A1A` | 主文字 |
| `colorTextSecondary` | `#8C8C8C` | 次要文字 |
| `borderRadius` | `8` | 控件圆角（AntD 默认 6 → 8） |
| `colorError` | `#FF4D4F` | 危险操作/删除 |
| `colorSuccess` / `colorWarning` | `#52C41A` / `#FAAD14` | 状态色 |
| 侧边栏 | 深色 `#001529`（保持现状） | 菜单高亮色随 `colorPrimary` |

> 硬编码色（如 `index.css` 的 `#e6f4ff` hover 行）已清理；新样式一律走 token。

#### delivery-app（载体 `src/constants/theme.ts`，亮/暗全表）

| Token | 亮 | 暗 |
| --- | --- | --- |
| `primary` / `primaryDark` | `#3A9BD9` / `#2E7FB8` | `#4DB6E5` / `#6FC3EC` |
| `primaryBg` | `rgba(58,155,217,0.10)` | `rgba(77,182,229,0.16)` |
| `accent`（自收/自提业务色） | `#FA8C16` | `#FA8C16` |
| `pageBackground` / `card` | `#F5F7FA` / `#FFFFFF` | `#101418` / `#1C1E21` |
| `text` / `textSecondary` | `#1A1A1A` / `#8C8C8C` | `#EAEAEA` / `#A6ACB4` |
| `headerGradientStart` / `headerGradientEnd` | `#4DB6E5` / `#3A9BD9` | `#3A9BD9` / `#2E7FB8` |

> `accent` 已定案：自收/自提业务色 = warning 橙 `#FA8C16`，**非消费者 CTA**（不使用 `#FF5436`），见 delivery-app 页面规范 §1/§2。订单状态 tint 接入 common `STATUS_TONE_COLORS`（dark 本地适配，§2.2）。新增 token 必须在亮/暗两套中都存在（`ThemeColor` 取二者交集）。

---

## 3. 字体 / 间距 / 圆角 / 阴影 / 层级

### 3.1 字体排版刻度

| 层级 | 字号/行高 | 字重 | 用途 |
| --- | --- | --- | --- |
| 页面大标题 | 20 / 28 | 600 | 页面主标题（管理端 20、移动端 18-20） |
| 区块标题 | 16 / 24 | 600 | 卡片/分区标题 |
| 正文 | 15 / 22 | 400 | 正文内容 |
| 辅助 | 14 / 20 | 400 | 描述、次要信息 |
| 说明 | 12 / 16 | 400 | 说明文字、占位符、版本号 |

> 三端均按各自 1x 尺寸体系执行（weapp 与 admin 的 CSS px、delivery 的 RN pt 一致）；替换现状各处零散字号（如 weapp 首页 20-48px 的不一致值）。

### 3.2 间距刻度

```
--spacing-xs: 4px;  --spacing-sm: 8px;  --spacing-md: 12px;
--spacing-lg: 16px; --spacing-xl: 24px;
```

- 卡片间距 ≥ `--spacing-md`（12px）；页面左右边距 `--spacing-lg`（16px）；区块间距 `--spacing-xl`（24px）。

### 3.3 圆角

| 场景 | 值 |
| --- | --- |
| 卡片 | ≥ 16px |
| 按钮（管理端 / 配送端，全圆角） | 24px |
| 按钮（小程序端，WeUI 官方） | 8px |
| 小元素（标签/缩略图/输入框） | 8px（小程序端输入框 / 单元格沿用 WeUI 官方值） |

### 3.4 阴影

```css
--shadow-card: 0 4px 16px rgba(0,0,0,0.04);           /* 卡片 */
--shadow-btn:  0 2px 8px rgba(255,84,54,0.3);         /* 消费者端 CTA 按钮 */
--shadow-modal: 0 8px 32px rgba(0,0,0,0.12);          /* 弹窗 */
```

### 3.5 层级

```
页面背景(#F5F7FA) → 卡片(#FFFFFF) → 浮层遮罩(rgba(0,0,0,0.4))
```

delivery-app 亮色为主（默认）；可选「夜间模式」（设计系统允许亮/暗双模式）：`#101418` 页面 → `#1C1E21` 卡片 → 遮罩加深（`rgba(0,0,0,0.6)`）。

---

## 4. 通用组件规范

| 组件 | 规范 |
| --- | --- |
| **按钮** | 主（品牌蓝底白字）/ 次（浅蓝 tint 底品牌蓝字）/ 幽灵（透明+品牌蓝边框字）/ **强调 CTA（消费者端橙红 `#FF5436` 底白字 + `--shadow-btn`）** / 危险（error 底，管理端）/ 禁用（灰底 `--text-weak`）；触达高度 ≥ 44（移动端）/ 48（管理端）；按压 `scale(0.98)` |
| **卡片** | 白底、圆角 ≥16、`--shadow-card`、padding 16；可选标题（区块标题 16/600）+ 右侧插槽 |
| **状态标签** | 10% tint 底 + 状态色文字，圆角 8，尺寸 sm（§2.2） |
| **价格展示** | 一律经 `@aquarush/common` `displayCents` / `<Money cents={...}/>`，**禁止手拼 `¥` 字符串**；**小程序端金额用微信电商红 `#FA5151`**（`.weui-price`，符号与小数位按比例缩放），管理端 / 配送端用主色或主文字色加粗（**均不用强调橙**） |
| **空态/加载/错误态** | 统一组件（BasePageLayout / EmptyState / LoadingSkeleton）；空态图标用品牌蓝 tint 圆底，不用插画 |
| **Toast/Modal** | 统一系统级组件；确认框文案「确定要…?」 |
| **列表项** | 白卡内列表，行高 ≥ 44（移动端）；分隔用留白，不用粗线框 |
| **底部操作栏** | 白底 + 顶部发丝线 + 安全区 padding；主操作按钮靠右或通栏 |
| **TabBar**（weapp） | 选中色 `#3A9BD9`，未选中 `#999999`；PNG 图标选中色随主色 |
| **表单输入** | 高 ≥ 44（移动端）/ 32（管理端默认），focus 品牌蓝描边 |
| **金额高亮** | 实付 / 合计金额：小程序端用微信电商红 `#FA5151`；管理端 / 配送端用品牌蓝或主文字色加粗。均不用强调橙 |

---

## 5. 禁忌清单（全局红线）

| ❌ 禁止 | ✅ 替代方案 |
|---------|------------|
| 长信息流/无限滚动（消费者端） | 首屏展示核心 SKU，下方仅 2-3 个补充商品或折叠入口 |
| 插画风商品图 / 扁平图标代替商品图 | 真实场景摆拍，统一明亮自然光 |
| 强调色（橙红）用于装饰/图标/分割线/非按钮文字 | 仅用于消费者端最高优先级 CTA 按钮与优惠券品牌元素（券卡/领券按钮/券选择器/券图标，§2.1） |
| 小程序端自造组件 / 偏离 WeUI 官方度量（自绘卡片、宫格、弹层、TabBar） | 用微信官方 WeUI 2.x 等价组件（§1.1 注） |
| 两个红色混用（金额用 `#FF5436`、CTA 用 `#FA5151`） | 行为色 `#FF5436` 只给 CTA / 券元素；数据色 `#FA5151` 只给金额数字（§2.1） |
| 细线边框分隔 | 用留白 + 浅投影分区 |
| 首屏超过 3 个核心信息 | 严格只保留：定位、时效、高频购买 |
| 复杂导航/多级分类（消费者端首页） | 首页不做分类导航，直接购买 |
| 硬编码颜色（`#fff`、`#3c87f7`、`#ff6b35` 等散落值） | 全部走设计 token（§2） |
| 手拼金额字符串 | 统一 `@aquarush/common` 金额工具 |
| 强调橙在管理端/配送端出现 | 主操作用品牌蓝，危险操作用 error 色 |
| 页面内自配状态色 / 绕过 status-tone 新增状态映射 | 先在 `frontend/common/src/utils/status-tone.ts` 增加 tone 映射（§2.2） |

---

## 6. 三端落地状态与持续约束

品牌级 token 迁移已于 2026-10 完成，本节从「实施计划」转为「落地状态」：

### 6.1 user-weapp ✅

`theme.scss` 新 token 落地、旧 `--theme-*` 别名化（存量引用逐步替换）、主题切换移除（F-USR-004，见 §6.5）、首页重构见 user-weapp 页面规范。

### 6.2 admin-client ✅

`main.tsx` `ConfigProvider` 注入 `themeToken`、`index.css` 硬编码色清理，详见 §2.3。

### 6.3 delivery-app ✅

`theme.ts` 品牌色映射（亮/暗全表）、`accent` 定案 `#FA8C16`、状态 tint 接入 common `STATUS_TONE_COLORS`，详见 §2.3。

### 6.4 运营素材规范（持续约束）

轮播图（建议 750×360 实拍/实景图）、公告、商品图（真实摆拍，明亮自然光）——后台素材上传与运营配置持续按本规范执行。

### 6.5 PRD 同步 ✅

`spec/prd/07-user.md` F-USR-004 已标 [已移除]（仓库规则 #12：允许不兼容修改，须同步 PRD）。

### 6.6 设计稿体系

`spec/design/`（`index.html` + `_spec/_CONTRACT.md` 编写契约 + `assets/tokens.css` token 镜像）以本文件为唯一视觉同源依据；稿子新增/修改按 `_CONTRACT.md` 执行，token 变更须先改本文件再同步 `tokens.css`。

---

## 7. 验收清单（逐屏通用）

- [ ] 全部颜色来自设计 token，无残留硬编码 hex
- [ ] 强调橙仅出现于消费者端 CTA 与优惠券品牌元素，每屏 ≤ 3 处
- [ ] 状态标签经 common status-tone / 各端 token 渲染，页面不自配状态色（§2.2）
- [ ] 卡片圆角 ≥16、投影/间距符合 §3
- [ ] 商品图均为真实实拍（后台素材达标）
- [ ] 金额全部经 `@aquarush/common` 格式化
- [ ] 触达目标 ≥ 44px（移动端）/ ≥ 32px（管理端控件）
- [ ] 消费者端无新增无限滚动长列表

---

## 变更记录

- **V1.1（2026-10-11）**：对齐已落地实现 —— PRD 引用路径迁移至 `spec/prd/`；§2.2 收敛为 StatusTone 体系（`@aquarush/common` status-tone 为唯一来源）；§2.3 由「现状→目标」迁移表改为已落地 token 表；delivery-app `accent` 定案 `#FA8C16`；§6 由实施计划改为落地状态（新增 6.1-6.6 子节）。
- **V1.0**：初版 —— 设计系统定义 + 三端迁移计划。
