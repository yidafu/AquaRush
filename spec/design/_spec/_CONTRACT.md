# 页面编写契约（UI 设计稿生成规范）

> 所有 `spec/design/**/*.html` 必须遵守本契约。目标是 90+ 张稿子视觉同源、可整组评审。
> 设计依据：`spec/design/ui-design-system.md` V1.1 + `spec/design/_spec/*.md`（从 PRD 提取的页面规格）。
> **禁止参考代码**：只依据 PRD 与规格文件，不得阅读 `frontend/`、`modules/`、`services/`。

---

## 0. 用户端小程序：WeUI 组件体系（最高优先级，与下文冲突时以本节为准）

`spec/design/user-weapp/**` 一律采用**微信官方 WeUI 2.x 组件体系**，样式来自 `../../assets/weui.css`。
样板页：`spec/design/user-weapp/01-product/home.html`（**照它写**）。

### 0.1 引入顺序（三端外壳 + WeUI）

```html
<link rel="stylesheet" href="../../assets/tokens.css">
<link rel="stylesheet" href="../../assets/ui.css">     <!-- 仅供 .stage/.notes/.phone/.hero/.banner/.map 等外壳 -->
<link rel="stylesheet" href="../../assets/weui.css">    <!-- 组件一律用这里的 .weui-* / .mp-* -->
```

### 0.2 禁止使用 ui.css 里的 Web 风组件

user-weapp 页面**不得**使用：`.card` `.card-title` `.list` `.list-item` `.btn` `.tag`（除 `.tag-goal` 外）
`.price` `.price-orig` `.stepper` `.switch` `.check` `.empty` `.bottom-bar` `.fab` `.sheet` `.overlay` `.progress` `.timeline` `.divider` `.grid2` `.prod-card` `.stat-card`。
全部替换为下表的 WeUI 等价类。

### 0.3 设备外壳（小程序）

```html
<div class="phone mp">
  <div class="phone-screen is-brand">   <!-- 顶部是品牌渐变时才加 is-brand（胶囊转为白色） -->
    <div class="hero">                  <!-- 无渐变头的页面：删掉 .hero，把 .mp-status/.mp-nav 直接放 .phone-screen 里 -->
      <div class="mp-status">
        <span>9:41</span>
        <span class="sb-icons"><i class="sb-signal"></i><i class="sb-wifi"></i><i class="sb-batt"></i></span>
      </div>
      <div class="mp-nav is-brand">     <!-- 白底导航：去掉 is-brand -->
        <span class="mp-nav__back"></span>          <!-- 二级页才有 -->
        <span class="mp-nav__title">页面标题</span>
        <span class="mp-capsule"><i class="dots"></i><i class="target"></i></span>
      </div>
      <div class="hero-inner">…定位行 / 搜索框…</div>
    </div>
    <div class="app">
      <div class="app-scroll">…页面主体…</div>
      <div class="mp-footbar hair-t">…底部操作栏（可选）…</div>
      <div class="mp-tabbar hair-t">…原生 TabBar（仅一级页）…</div>
      <div class="home-indicator"></div>
    </div>
  </div>
</div>
```

- 原生 TabBar 固定三项：**首页 / 订单 / 我的**（当前项 `.mp-tab.on`，未选中 `#7F7F7F`，选中品牌蓝）。
- 二级页不要 TabBar，保留 `.home-indicator`。

### 0.4 WeUI 组件对照表

| 用途 | 类名 |
| --- | --- |
| 列表组 | `.weui-cells`（白底 + 上下发丝线）；组标题 `.weui-cells__title`；组说明 `.weui-cells__tips` |
| 单元格 | `.weui-cell` + `.weui-cell__hd` / `.weui-cell__bd` / `.weui-cell__ft`；带箭头 `.weui-cell__ft_in-access`；可点链接 `.weui-cell_link`；图标 `.weui-cell__icon`（`.weui-cell__icon--brand`） |
| 图文项 | `.weui-media-box` + `__hd` / `__thumb`（`.weui-media-box__thumb--sm`）/ `__bd` / `__title`（`--2` 两行）/ `__desc`（`--2`）/ `__info` |
| 面板 | `.weui-panel` + `__hd` / `__bd` / `__ft` |
| 九宫格 | `.weui-grids` + `.weui-grid` + `.weui-grid__icon`（内联 SVG 26px）/ `.weui-grid__label` |
| 加载更多 | `.weui-loadmore` + `.weui-loadmore__line`；单点 `.weui-loadmore_dot` + `__dot`；加载中 `.weui-loadmore_loading` + `.spinner` |
| 按钮 | `.weui-btn`（+ `_primary` / `_warn` / `_default` / `_plain-primary` / `_plain-default` / `_disabled` / `_mini` / `_xs` / `.flex1`）；按钮区 `.weui-btn_area` |
| 底部操作栏 | `.mp-footbar`（`hair-t`）+ `.mp-footbar__info` / `__lbl` |
| 输入 | `.weui-input`（透明，嵌在 `.weui-cell` 里）；灰底框 `.weui-input-box` + `.ph`；`.weui-textarea` + `.weui-textarea__counter`；`.weui-label`（必填 `.weui-label--req`） |
| 开关/选择 | `.weui-switch`（`.on`）；`.weui-radio` / `.weui-checkbox`（`.on`，圆形加 `--circle`）；`.weui-stepper` + `__btn`（`.disabled`）/ `__num` |
| 搜索 | `.weui-searchbar__box`（`.start` 左对齐 / `.on-brand` 渐变头内用）；`.weui-searchbar__cancel` |
| 分段/Tabs | `.weui-segmented` + `__item`（`.on`）；`.weui-tabs` + `.weui-tabs__item`（`.on`，选中带品牌蓝短下划线）+ `.cnt` |
| 弹窗 | 遮罩 `.weui-mask` + `.weui-dialog` + `__hd` / `__title` / `__bd` / `__ft` / `__btn`（`_primary` 品牌蓝 / `_warn` 红 / `_default`） |
| 底部菜单 | `.weui-mask` + `.weui-actionsheet` + `__title` / `__menu` / `__cell`（`__cell_warn` 红色危险项）/ `__action` |
| 半屏弹层 | `.weui-mask` + `.weui-half` + `__hd` / `__title` / `__close` / `__bd`（规格/地址/优惠券选择用） |
| 轻提示 | `.weui-toast`（`_text` 纯文字）+ `__icon` / `__text`；顶部提示 `.weui-toptips` |
| 标签 | `.weui-tag`（`--md` 大号）+ `--brand` / `--red` / `--orange` / `--green` / `--outline` / `--goal` |
| 订单状态 | `.weui-tag--pay`（待付款橙）/ `--assign`（待派单·待配送蓝）/ `--deliver`（配送中绿）/ `--done`（已完成灰）/ `--cancel`（已取消·已退款红） |
| 价格 | `.weui-price`（微信电商红 `#FA5151`）+ `__symbol` / `__int` / `__dec`；尺寸 `--lg` / `--md` / `--sm`；灰价 `--gray`；划线原价 `.weui-price--origin`；免运费 `.weui-price--free` |
| 徽标 | `.weui-badge`（`.weui-badge_dot`） |
| 进度条 | `.weui-progress` + `.weui-progress__bar` |
| 步骤条 | `.weui-steps` + `.weui-step`（`.done`）+ `__dot` / `__label` |
| 通告栏 | `.weui-notice`（`--brand` / `--red`） |
| 图片上传 | `.weui-uploader` + `__file` / `__input`（加号） |
| 商品卡 | `.mp-grid2` + `.mp-prod` + `__cover` / `__body` / `__name` / `__desc` / `__foot` / `__cart` |
| 数据格 | `.mp-statgrid` + `__item` + `__num`（含 `.unit`）+ `__lbl` |
| 空态 | `.weui-empty` + `__icon`（内联 SVG 36px）+ `__text` |
| 左右留白 | `.mp-pad`（8px）/ `.mp-pad-16`（16px） |
| 发丝线 | `.hair-t` / `.hair-b` |
| 通用排版 | 沿用 `.row` `.col` `.grow` `.gap1~4` `.mt2~4` `.mb2~3` `.ellipsis` `.clamp2` `.f-note` `.f-aux` `.f-body` `.bold` `.t-sub` `.t-weak` `.t-err` `.ico` `.chips` `.chip` |

### 0.5 颜色使用规则（user-weapp 专属）

| 用途 | 用色 |
| --- | --- |
| 交互主色（导航、TabBar 选中、Grid 图标、主按钮、开关、单选复选、进度） | `var(--weui-BRAND)` 品牌蓝 |
| **所有金额（价格 / 优惠 / 合计）** | `var(--weui-price)` 微信电商红 `#FA5151` |
| 链接 / 「查看更多」「查看全部」 | `var(--weui-LINK)` `#576B95` |
| 危险 / 删除 / 警示 | `var(--weui-RED)` |
| 提示 / 待办 | `var(--weui-ORANGE)` |
| 成功 / 配送中 | `var(--weui-GREEN)` |
| 强调橙 `var(--action)` | **仅**优惠券品牌元素与最高优先级 CTA，每屏 ≤ 3 处 |

> 金额一律 `.weui-price`，**不得手拼 `¥` 字符串**。禁止裸 hex。

---

## 1. 文件与路径

| 端 | 目录 | 路径深度 |
| --- | --- | --- |
| 用户端小程序 | `spec/design/user-weapp/<模块号>-<模块名>/<page>.html` | 2 层 |
| 管理后台 | `spec/design/admin/<模块号>-<模块名>/<page>.html` | 2 层 |
| 送水工端 App | `spec/design/delivery/<模块号>-<模块名>/<page>.html` | 2 层 |

一个页面 = 一个 HTML 文件。**弹窗/抽屉/底部弹层不单独成页**，作为父页面的第二种状态在同一文件内用「状态切换」区块表达（见 §5）。

## 2. HTML 骨架（逐字照抄，只改注释处）

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>用户端 · 首页｜奔泉 UI 设计稿</title>
<link rel="stylesheet" href="../../assets/tokens.css">
<link rel="stylesheet" href="../../assets/ui.css">
</head>
<body class="end-weapp">   <!-- end-weapp | end-admin | end-delivery -->
<div class="stage">

  <header class="stage-head">
    <div>
      <h1>用户端小程序 · 首页</h1>
      <p class="sub">功能编号 F-PRO-001 ｜ 商品域 ｜ 下游：商品详情 / 地址选择</p>
    </div>
    <div class="chips">
      <span class="chip">F-PRO-001</span>
      <span class="chip chip-ok">已实现</span>   <!-- chip-ok 已实现 / chip-partial 部分实现 / chip-plan 规划中 / chip-defer 暂缓 -->
      <span class="chip chip-plain">user-weapp</span>
    </div>
  </header>

  <div class="stage-body">
    <!-- ① 设备外框 + 页面内容（见 §3） -->
    <!-- ② 右侧标注面板（见 §4） -->
  </div>

</div>
</body>
</html>
```

## 3. 设备外框

### 3.1 用户端小程序（375 宽，高度自适应）

> `.phone` 默认高度随内容增长，可完整展示长页面；需要表现「视口态 / 弹层遮挡」时改用 `<div class="phone viewport">`（固定 812 高，内容滚动）。

```html
<div class="phone">
  <div class="phone-screen">
    <div class="statusbar on-brand">
      <span>9:41</span>
      <span class="sb-icons"><i class="sb-signal"></i><i class="sb-wifi"></i><i class="sb-batt"></i></span>
    </div>
    <div class="app">
      <div class="navbar on-brand">
        <span class="nav-back">‹</span>
        <span>页面标题</span>
        <span class="capsule"></span>
      </div>
      <div class="app-scroll">…页面主体…</div>
      <div class="tabbar">
        <div class="tab active"><svg class="ico">…</svg>首页</div>
        …
      </div>
      <div class="home-indicator"></div>
    </div>
  </div>
</div>
```

- 渐变头图页（首页/登录/个人中心）：给 `.app` 或首个区块加 `style="background:var(--grad)"`，导航栏与状态栏用 `.on-brand`（白字）。
- 仅当该页在小程序内有 TabBar 时渲染 `.tabbar`（首页 / 订单 / 我的）。
- 二级页不要 TabBar，保留 `.home-indicator`。

### 3.2 管理后台（自适应高度，完整表格）

```html
<div class="browser">              <!-- 常规页面：高度随内容增长 -->
```
含弹窗/抽屉的「状态图」用固定视口态，遮罩才能正确铺满：`<div class="browser viewport">`。

```html
<div class="browser">
  <div class="browser-bar">
    <span class="lights"><i></i><i></i><i></i></span>
    <span class="url">admin.aquarush.cn/products</span>
  </div>
  <div class="browser-body">
    <aside class="admin-side">
      <div class="logo"><span class="mark">A</span>奔泉运营后台</div>
      <div class="admin-menu">…菜单项，当前项加 .on…</div>
    </aside>
    <div class="admin-main">
      <div class="admin-header">
        <span class="breadcrumb">商品管理 <span class="crumb-sep">/</span> <b>商品列表</b></span>
        <span class="user"><span class="avatar">超</span>超级管理员</span>
      </div>
      <div class="admin-content">…页面主体…</div>
    </div>
  </div>
</div>
```

- 侧边栏菜单**每页都要渲染完整菜单**（便于评审导航一致性），当前页加 `.on`。
- 内容区固定从「页面标题区 `.page-title`」开始。

### 3.3 送水工端 App（亮色，375 宽，高度自适应）

> 送水工端（delivery-app）为 **Expo React Native 应用，默认亮色**（与设计系统 `spec/design/ui-design-system.md` §2 的「亮/暗双模式」一致，稿子出亮色默认态）。
> `ui.css` 里保留了 `.phone.dark` 夜间模式作用域作为可选能力，**页面默认不使用**。

```html
<div class="phone">
  <div class="phone-screen">
    <div class="statusbar">
      <span>9:41</span>
      <span class="sb-icons"><i class="sb-signal"></i><i class="sb-wifi"></i><i class="sb-batt"></i></span>
    </div>
    <div class="app">
      <div class="navbar"><span>页面标题</span></div>
      <div class="app-scroll">…</div>
      <div class="tabbar">…</div>
      <div class="home-indicator"></div>
    </div>
  </div>
</div>
```

## 4. 右侧标注面板（每页必写）

```html
<div class="notes">
  <div class="note-card">
    <h3>页面说明</h3>
    <p><strong>所属端：</strong>用户端小程序</p>
    <p><strong>入口：</strong>…</p>
    <p><strong>跳转：</strong>…</p>
  </div>
  <div class="note-card">
    <h3>布局结构</h3>
    <ol><li>…</li></ol>
  </div>
  <div class="note-card">
    <h3>关键交互</h3>
    <ol><li>…</li></ol>
  </div>
  <div class="note-card">
    <h3>状态与规则</h3>
    <p>…含空态/加载态/禁用态/校验规则…</p>
  </div>
  <div class="note-card todo-note">
    <h3>设计要点 / 禁忌</h3>
    <p>…强调橙仅限消费者端 CTA；金额分转元；真实摆拍图…</p>
  </div>
</div>
```

- 规划中功能在「设计要点」卡里加 `.chip-goal` 样式说明，并在画布内对目标形态分区块加 `<span class="tag tag-goal"></span>`（自动显示「目标形态」）。
- 若该页含弹层状态，追加一张卡片说明弹层内容，并在设备内**并排**渲染第二个 `.phone` / `.browser` 展示弹层打开态。

## 5. 弹层/多状态表达

同一页面有多个状态（如「正常态 / 空态」「列表 / 弹窗」）时，在 `.stage-body` 内**横向并排**多个设备框，每个上方加一行小标题：

```html
<div class="col gap3">
  <span class="f-note t-sub bold">状态一 · 正常列表</span>
  <div class="phone">…</div>
</div>
<div class="col gap3">
  <span class="f-note t-sub bold">状态二 · 空态</span>
  <div class="phone">…</div>
</div>
```

设备内的弹层用 `.overlay`（底部弹层）或 `.overlay.center`（居中弹窗）绝对定位在 `.phone-screen` 内。

## 6. 可用类名清单（只用这些，不要自造 CSS）

| 分类 | 类名 |
| --- | --- |
| 卡片 | `.card` `.card.flush` `.card.tint` `.card-head` `.card-title` `.card-title.sm` `.card-more` `.section-title` `.divider` |
| 布局 | `.row` `.row.between` `.row.top` `.col` `.grow` `.wrap` `.gap1~.gap4` `.mt2/.mt3/.mt4` `.mb0/.mb2/.mb3` `.grid2` `.grid3` `.grid4` `span/.col.gap3` |
| 文字 | `.f-note` `.f-aux` `.f-body` `.f-blk` `.t-main` `.t-sub` `.t-weak` `.t-brand` `.t-action` `.t-ok` `.t-warn` `.t-err` `.bold` `.ellipsis` `.clamp2` `.strike` |
| 价格 | `.price`（`.cur` `.dec` `.big` `.plain`）`.price-orig` `.price-line` |
| 标签 | `.tag` `.tag-lg` + `.tag-pay/.tag-assign/.tag-deliver/.tag-done/.tag-cancel/.tag-brand/.tag-action/.tag-ok/.tag-warn/.tag-err/.tag-neutral/.tag-outline/.tag-goal` |
| 按钮 | `.btn`（`.sm` `.xs` `.block` `.secondary` `.ghost` `.cta` `.danger` `.text` `.disabled`） |
| 列表 | `.list` `.list-item` `.li-ico` `.arrow` |
| 缩略图 | `.thumb` + `.sq/.sq-sm/.sq-lg/.wide`；商品卡 `.prod-card`（`.cover` `.cover.ratio-43` `.info` `.name` `.desc`） |
| 表单 | `.field`（`label`，必填加 `.req`）`.input` `.textarea` `.select` `.ph` `.disabled` `.switch` `.radio-row` `.radio` `.stepper`（`.num`）`.check` |
| 金额 | `.cost-row`（`.minus` `.total`）`.amt` `.lbl` |
| 底部栏 | `.bottom-bar` `.bb-info` |
| 空/加载 | `.empty` `.e-ico` `.e-title` `.e-desc` `.skeleton` |
| 进度 | `.progress` `.dot` `.bar`；时间线 `.timeline` `.tl-item`（`.done`）`.tl-dot` `.tl-time` |
| 悬浮 | `.fab` |
| 弹层 | `.overlay` `.overlay.center` `.sheet` `.sheet-title` |
| 后台 | `.browser` `.browser-bar` `.url` `.admin-side` `.logo` `.mark` `.admin-menu` `.mi`（`.on`）`.badge` `.group-lbl` `.admin-main` `.admin-header` `.breadcrumb` `.crumb-sep` `.user` `.avatar` `.admin-content` `.page-title` `.filter-card` `.fitem` `.spacer` `.stat-card` `.stat-grid`（`.c3`）`.up` `.down` `.ant-table` `.num` `.ops` `.sub-line` `.table-foot` `.pager` `.tabs` `.tab-item`（`.on`）`.chart-box` `.cb-head` `.cb-title` `.chart-legend` `.bars` `.bar-col` `.bar`（`.alt`）`.x` `.hbar` `.hr` `.nm` `.track` `.fill` `.vl` `.modal-mask` `.modal`（`.wide`）`.modal-head` `.modal-body` `.modal-foot` `.drawer` `.desc-grid`（`.two`）`.desc-item` `.k` `.v` |
| 送水工端 | `.task-card` `.tc-head` `.addr` `.meta` `.foot` `.seg` `.s`（`.on`）`.cnt` |
| 地图 | `.map` `.water` `.road` `.pin`（`.pickup` `.cur`）`.map-label` `.map-ctrl` |
| 状态栏 | `.statusbar`（`.on-brand`）`.sb-icons` `.sb-signal` `.sb-wifi` `.sb-batt` |
| 头图 | `.hero` `.hero-inner` `.hero-loc` `.hero-h` `.hero-sub` `.searchbar`（`.plain`）`.banner` `.b-kicker/.b-title/.b-desc/.b-cta` `.banner-dots` `.quick-actions` `.qa` `.notice`（`.brand` `.action`） |
| 工具 | `.ico` `.ico-sm` `.ico-lg` `.chip` `.chips` |

## 7. 图标

一律内联 SVG，使用 `class="ico"` 或 `class="ico ico-sm"`，`stroke="currentColor"`，`fill="none"`。常用路径（24×24 viewBox）：

```html
<!-- 首页 --><svg class="ico" viewBox="0 0 24 24"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.8V20h14V9.8"/><path d="M10 20v-5h4v5"/></svg>
<!-- 订单 --><svg class="ico" viewBox="0 0 24 24"><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>
<!-- 我的 --><svg class="ico" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.6"/><path d="M4.5 20c1.3-3.6 4-5.4 7.5-5.4s6.2 1.8 7.5 5.4"/></svg>
<!-- 购物车 --><svg class="ico" viewBox="0 0 24 24"><path d="M3 4h2l2.4 11h10.2L20 7H6"/><circle cx="9" cy="19" r="1.6"/><circle cx="17" cy="19" r="1.6"/></svg>
<!-- 定位 --><svg class="ico" viewBox="0 0 24 24"><path d="M12 21s6.5-5.6 6.5-10.4A6.5 6.5 0 0 0 5.5 10.6C5.5 15.4 12 21 12 21z"/><circle cx="12" cy="10.5" r="2.4"/></svg>
<!-- 电话 --><svg class="ico" viewBox="0 0 24 24"><path d="M5 4h3.5l1.8 4.2-2.1 1.6a12 12 0 0 0 5.9 5.9l1.6-2.1L20 15.5V19a1 1 0 0 1-1 1A15 15 0 0 1 4 5a1 1 0 0 1 1-1z"/></svg>
<!-- 右箭头 --><svg class="ico ico-sm" viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"/></svg>
<!-- 左箭头 --><svg class="ico" viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg>
<!-- 加号 --><svg class="ico" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>
<!-- 搜索 --><svg class="ico" viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5"/><path d="M16 16l4 4"/></svg>
<!-- 相机 --><svg class="ico" viewBox="0 0 24 24"><path d="M4 8h3l1.6-2h6.8L17 8h3v11H4z"/><circle cx="12" cy="13.2" r="3.4"/></svg>
<!-- 消息 --><svg class="ico" viewBox="0 0 24 24"><path d="M4 5h16v11H9l-5 4z"/></svg>
<!-- 图表 --><svg class="ico" viewBox="0 0 24 24"><path d="M4 20V9M10 20V4M16 20v-7M22 20H2"/></svg>
<!-- 商品/盒子 --><svg class="ico" viewBox="0 0 24 24"><path d="M12 3 3 7.5v9L12 21l9-4.5v-9z"/><path d="M3 7.5 12 12l9-4.5M12 12v9"/></svg>
<!-- 用户组 --><svg class="ico" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.2"/><path d="M3 19c1-3 3.4-4.6 6-4.6s5 1.6 6 4.6"/><path d="M16 5.4a3 3 0 0 1 0 5.6M18 14.6c1.6.7 2.7 2.2 3 4.4"/></svg>
<!-- 设置 --><svg class="ico" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3.2"/><path d="M12 3v2.4M12 18.6V21M4.2 7.2l2 1.2M17.8 15.6l2 1.2M4.2 16.8l2-1.2M17.8 8.4l2-1.2"/></svg>
<!-- 票/优惠券 --><svg class="ico" viewBox="0 0 24 24"><path d="M3 8h18v3a2 2 0 0 0 0 4v3H3v-3a2 2 0 0 0 0-4z"/><path d="M12 8v10" stroke-dasharray="2 2.5"/></svg>
<!-- 对勾 --><svg class="ico" viewBox="0 0 24 24"><path d="M5 12.5 10 17.5 19 7"/></svg>
<!-- 关闭 --><svg class="ico" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6 6 18"/></svg>
```

## 8. 硬性红线（评审会逐条查）

1. 颜色只用 `var(--*)`，**页面内不得出现裸 hex**（`#fff`、`#3c87f7` 等一律禁止）。
2. 强调橙 `var(--action)` 仅用于：消费者端最高优先级 CTA 按钮、优惠券品牌元素（券卡金额/类型、领券按钮、券选择器、券图标）。**每屏 ≤ 3 处**。管理后台与送水工端**完全不出现**强调橙。
3. 卡片圆角 ≥ 16px（用 `.card` / `.prod-card` / `.task-card`）；小元素 8px；按钮 24px。
4. 金额一律 `¥12.00` 形式（分转元、两位小数），用 `.price` / `.cost-row`，**不得手拼 `¥` 字符串**。
5. 订单/业务状态一律用「10% tint 底 + 状态色文字」的 `.tag-*`（映射见 §9）。
6. 移动端触达高度 ≥ 44px，管理端控件 ≥ 32px。
7. 商品图一律用 `.thumb` / `.prod-card .cover` 占位块并标注「实拍图占位」——**严禁卡通插画风**。
8. 消费者端不做无限滚动长列表：首页只保留「定位 / 时效 / 高频购买」≤3 个核心信息 + 2~3 个补充商品。
9. 状态标记必须与 PRD 一致：`[已实现]` → `chip-ok`，`[部分实现]` → `chip-partial`，`[规划中]` → `chip-plan` + 页内 `.tag-goal` 标注，`[暂缓]`（2026-10-11 新增：已有决策明确缓做，保留编号与设计、不进当期排期）→ `chip-defer` + 页内 `.tag-goal` 标注 + 「设计要点」卡注明暂缓决策与日期。
10. 页内文案尽量逐字使用 PRD 原文（规格文件的「页面文案」一栏）。

## 9. 订单状态标签映射（唯一口径）

| 状态 | 类名 | 观感 |
| --- | --- | --- |
| 待付款 / 待支付 | `.tag-pay` | 橙 `#FA8C16` |
| 待派单 / 待配送 | `.tag-assign` | 蓝 `#3A9BD9` |
| 配送中 | `.tag-deliver` | 绿 `#389E0D` |
| 已完成 | `.tag-done` | 灰 `#8C8C8C` |
| 已取消 / 已退款 | `.tag-cancel` | 红 `#CF1322` |

管理后台侧边栏菜单（所有后台页统一）：

```
工作台 / 数据统计 / 商品管理 / 订单管理 / 派单中心(订单管理内) / 地址与配送 / 收银对账
支付管理(支付对账 / 退款管理,2026-10 新增) / 用户管理 / 营销管理 / 运营配置 / 通知消息 / 系统设置 / 日志管理
```
