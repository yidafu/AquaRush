# 04 · 前端实现设计

> pnpm workspace 四包：`user-weapp`（Taro 4 微信小程序）、`delivery-app`（Expo RN 独立 App）、`admin-client`（React+Vite+AntD）、`@aquarush/common`（共享 money/status 工具 + tracking SDK）。页面清单以 PRD 附录 A 为准。

## 0. 跨端通用约定

| 项 | 约定 |
| --- | --- |
| 金额 | 一律经 `@aquarush/common/utils/money`：分→元（两位小数）展示、元→分提交；禁止组件内手写 ÷100/×100（R-MON-01） |
| 状态 | 订单状态等枚举文案/色彩映射进 common，拆为两个模块：`utils/order-status`（状态枚举→文案/Tab 归类）+ `utils/status-tone`（状态→色彩 tone），三端口径一致（7 态、日报 4 态、对账 4 态文案「待对账/匹配/有差异/已复核」） |
| 请求 | GraphQL 客户端 + codegen 生成 TS 类型（生成物不手改）；**单页渲染=单请求**（一次 query 携带全部所需字段，别名/嵌套取回；禁止页面内串行多次请求拼装） |
| 时间 | 展示本地化 zh-CN；**提交本地格式 `YYYY-MM-DDTHH:mm:ss`**（禁 `.toISOString()` UTC 偏移，G40 教训） |
| 测试 | jest（ colocated `src/**/*.test.ts`），无 vitest/jest30/babel8 |
| 埋点 | `@aquarush/common/tracking`：平台无关核心 + 各端 Adapter（队列缓冲/定时 flush/PAGE_VIEW 去重/无 token 暂存/失败静默重入队）→ `POST /api/logs/user-actions/batch`；切后台/登录成功触发 flush。weapp 另有编译期插桩：workspace 包 `babel-plugin-aquarush-track`（Vite 插件形态）把 JSX 上的 `track-*` props 编译为 `<Track>` 包裹，业务代码不手写埋点调用 |
| 组件 | React 函数组件 + TypeScript 注解（禁 class 组件） |

---

## 1. 用户端小程序（user-weapp / Taro 4）

### 1.1 页面清单与路由

| 页面 | 路径 | 要点 |
| --- | --- | --- |
| 首页 | `home` | 顶部轮播（activeBanners 配置驱动，未配置隐藏）+ 公告栏（最近已发布 1 条）+ 商品网格（activeProducts 20/页，下拉刷新重置/触底追加）；价格区按 priceSource 渲染优惠价+划线目录价 |
| 商品列表 | `product-list` | 搜索/排序/筛选（首页搜索入口跳转至此） |
| 商品详情 | `product-detail?id=` | 图集画廊/全屏预览；价格按登录用户解析（分组价/特价/新人价+划线）；库存/销量；收藏；立即购买（BuyModal 1~库存 → pendingOrder 本地缓存 → order-confirm）；售罄/非在售禁购；加载失败重试 3 次间隔 1s |
| 购物车 | `cart` | 本地存储（同商品合并数量）；勾选/步进/删除/全选/合计实时；超库存拦截；部分缺货整体拦截提示缺货项；去结算写 pendingOrder |
| 确认订单 | `order-confirm` | **单请求 orderPreview**：地址卡（默认回显/切换，范围校验标红+「更换地址/联系客服」）、商品卡、配送时段 2×2 芯片（deliverySlots 配置驱动，停用回退立即配送）、备注 ≤200、费用明细（商品/优惠/水票抵扣/赠品行/配送费/押金/合计）、券选择、水票核销数、积分抵扣区（预留；**无余额抵扣——用户储值 2026-10-09 决策不做**）、服务条款；**起送门槛**（R-OPS-03 规划中 G96）：orderPreview 返回 belowMinimum 时禁提交+「未达起送金额 ¥X」提示；提交前 `Taro.requestSubscribeMessage` 订阅授权 |
| 订单列表 | `order-list?tab=` | Tab 全部/待付款/待配送/配送中/已完成（10/页）；按状态操作集（取消+立即支付/查看详情/再次购买） |
| 订单详情 | `order-detail?id=` | 状态步骤条（下单→支付→派单→配送→完成）、地址/商品/配送信息卡（送水工电话可拨、实时位置入口）、费用明细、时间线（order_operations）、操作（催单 5 分钟防抖提示、评价、再来一单、联系客服=拨店铺电话） |
| 评价 | `order-review?id=` | 1-5 星 + ≤500 字 + 匿名开关 |
| 配送跟踪 | `delivery-tracking?orderId=` | 状态门控；双标记地图（概略位置+地址）、ETA；送达「已送达」停止刷新 |
| 周期订购 | `subscriptions` | **用户端入口本期不开发（继续暂缓，2026-10-11 新方向：入口仅管理后台）**；后台侧见 §3.1 |
| 企业下单 | `enterprise-order`（新增） | 多地址多商品批量提交；拆单结果回显；月结标识（**[暂缓]** 随 F-ORD-027，不实现） |
| 地址列表/编辑 | `address-list?select=true` / `address-edit` | 选择模式回填；三级联动；地图选点（GCJ-02→WGS-84 转换后提交，失败可手填坐标可空）；默认唯一切换 |
| 我的 | `my` | 头像昵称（编辑弹层）、订单角标（myOrderStatistics 单请求，>99 显 99+）、资产入口（收藏/地址/水桶账户/会员/积分/水票/优惠券）、消息中心角标、客服拨号、反馈、关于 |
| 消息中心 | `message-center` | 双行筛选（类型×已读）；点击先标已读再跳转；全部已读 |
| 水桶账户 | `barrel-mine`（新增） | 按规格在租/欠桶/押金合计 + 押金记录 + 申请退桶退押（文案统一押金桶口径） |
| 水票 | `ticket-shop` / `ticket-list` | 购票（1~10）/票包（票号/状态/有效期）；G97 运营策略：商城「拉新专享」角标与复购激活明示、票夹待激活置灰 |
| 优惠券 | `coupon-center` / `coupon-list` | 领券/券包（状态筛选） |
| 会员/积分 | `member-center` / `points`（新增） | 等级成长进度与权益/积分明细与兑换；**储值/充值页 `recharge` 不设**（用户储值 2026-10-09 决策不做，G79 收敛为套餐卡 `package-plans`） |
| 反馈 | `feedback` / `feedback-records` | 类型胶囊/10-500 字/图≤3；记录页回传处理结果 |
| 资料编辑/关于 | `profile-edit` / `about` | 昵称 2-20 校验/头像上传；关于页动态读 store_profile |

### 1.2 关键实现

- **静默登录**：app 启动 `initSilentLogin`（Taro.login → code 换 token，openid 首现自动建用户）；onShow `checkLoginStatus`；网络层 401 拦截 → refreshToken 刷新重试（幂等防风暴，失败清态游客）；**单飞 Promise** 防并发重复登录；游客执行需登录操作 → 引导登录后回跳原页面；
- **pendingOrder 本地缓存**：立即购买/购物车结算/再来一单统一写 pendingOrder（商品+数量+来源），order-confirm 读取并服务端预览计价；
- **再来一单**（R-ORD-12）：reorder 查询返回重建的待下单数据（失效剔除+提示、地址回填/默认、价格当前）→ 写 pendingOrder → order-confirm；
- **主题**：固定品牌主色 `#3A9BD9`，禁主题切换逻辑（F-USR-004 移除口径）。

## 2. 送水工端 App（delivery-app / Expo RN，dev build）

### 2.1 页面清单

| 页面 | 路由 | 要点 |
| --- | --- | --- |
| 登录 | `login` | 账号密码（`delivery_{id}`）；角色分流：WORKER→任务首页 / ADMIN·SUPER_ADMIN→`/manage`（看板/全部订单/送水工 深链） |
| 工作台 | `(tabs)/index` | 今日四卡 + 近 7 天双折线（`todayStatistics`+`weekStatistics` 单请求双字段取回，30s 轮询；折线为自绘组件 `WeekLineChart`，victory-native 绘制，非共享图表库）+ 快捷入口九宫格（经营看板[兼送水]/每日收款/对账记录/历史订单/消息中心…） |
| 任务列表 | 任务首页 | 三栏 未接单/已接单/配送中（workerTasks）；WS 实时刷新 + 轮询降级 |
| 订单详情 | `order/[orderNo]` | 收货地址卡（内嵌目的地地图 MapLibre+天地图 WGS-84；无坐标/未配 Key 降级文字坐标）、「导航」按钮唤起第三方地图（微信/高德/百度 URI，WGS-84 终点；无坐标降级复制地址）、修改地址（updateOrderAddress）、转单入口、完成配送表单（照片≥1 上传、收款方式三选、空桶回收数、备注）、配送地图卡（地址标记+当前位置蓝点）；**号码保护**（F-ORD-032/R-ORD-17 规划中 G95）：收货人电话按接口返回渲染——非配送阶段（待支付/待派单/已完成/已取消/已退款）服务端只下发脱敏串，前端无完整号码即不显示拨号按钮（`tel:` 仅完整号码阶段渲染） |
| 代客下单 | `create-order` | 商品搜索（防抖 300ms）/多选/行内步进；选用户地址；自提开关；提交进待派单 |
| 历史订单 | `history-orders` | 状态 Tab；工作台统计卡跳转 |
| 每日收款 | `daily-collection` | 系统预计算三栏回显 + 填报 + 差异实时计算；超容差弹差异确认（说明必填）；状态卡四态 |
| 对账记录 | `reconciliation-records` | 10/页；状态色（待对账黄/匹配绿/有差异红/已复核蓝）；下钻当日订单流水 |
| 消息中心 | `messages` | 任务/系统 × 已读双行筛选 |
| 我的 | `my` | 头像姓名手机号 + 关于/协议/退出 |
| 管理看板 | `/manage`（ADMIN） | 复用 admin 接口同权限（看板/全部订单/送水工） |

### 2.2 关键实现

- **WebSocket 客户端**：登录后全局监听 `/ws/notifications`；断线指数退避重连；按 idempotencyKey 去重；30s 轮询兜底；新订单广播 → 任务列表置顶提示；
- **系统通知**：expo-notifications 本地通知（前台横幅/通知栏/声音/点击跳订单详情）；web 端静默跳过；远程推送（APNs/FCM）为目标形态【G69】；
- **后台轨迹上报**：原生 GPS + SQLite 持久化 SDK（锁屏/后台不丢点，需 dev build，Expo Go 不支持）；5s 节流；弱网 AsyncStorage 缓存补传；服务端拒绝（非本人/非配送中）→ 停止该订单上报并丢弃缓存；App 重启/重登录 `resumeActive` 校验后恢复；iOS Always + UIBackgroundModes location / Android ACCESS_BACKGROUND_LOCATION + FOREGROUND_SERVICE(_LOCATION) 权限声明；
- **路由守卫**：Stack.Protected 未登录拦截；workerInfo 为空重定向登录。

## 3. 管理后台（admin-client / React + Vite + Ant Design）

### 3.1 菜单与页面清单（权限驱动渲染，无权限的菜单不显示且接口 FORBIDDEN）

| 菜单 | 页面 | 权限码 |
| --- | --- | --- |
| 仪表盘 | `dashboard`（四卡+双图+时间范围预设） | STATISTICS_READ |
| 数据统计 | `statistics`（营收）/ `statistics/users` / `statistics/delivery-workers` / `statistics/orders` / `statistics/products` / `statistics/marketing` | STATISTICS_READ |
| 商品管理 | `products/list`（筛选/多条件/着色/快速操作：导出+批量导入）/ `products/new·:id/edit`（分组表单+小程序实时预览）/ `products/options`（规格/水源地字典）/ `group-prices`（分组价） | PRODUCT_* / MKT_* |
| 押金管理 | `barrel-ledger`（桶账台账+导出）/ 退押申请处理 / 押金价目 | PRODUCT_* |
| 订单管理 | `orders`（筛选/多选批量派单/导出）/ `orders/detail`（时间线/交付照片核验/取消/地址修正入口） | ORDER_* |
| 周期订购计划 | `subscriptions`（菜单「订单管理 → 周期订购计划」） | SUBSCRIPTION_READ / SUBSCRIPTION_WRITE（G77 后台侧已实现 2026-10-11：列表筛选分页、新建（绑定用户/地址/可选送水工）、暂停/恢复/终止、lastError 展示） |
| 企业客户 | `enterprise-customers`（档案/账单/开票） | ORDER_*（**[暂缓]** G83，不实现） |
| 收银对账 | `daily-collection`（每日收款记录 Tab + 对账记录 Tab）/ `payroll`（工资结算） | RECONCILIATION_* |
| 用户管理 | `users/customers`（列表+分组列）/ `users/:id`（信息卡+统计卡+7 Tab）/ `users/accounts`（账号列表，用户类型下拉）/ `users/accounts/:id`（统一账号详情：管理员 8 Tab / 送水工 6 Tab） | USER_* / ADMIN_USER_* / DELIVERY_WORKER_* |
| 客户分组 | `customer-groups`（成员覆盖式管理+价格策略入口） | MKT_* |
| 营销管理 | `coupons` / `promotion-full-reduce` / `flash-sale` / `buy-gift` / `newcomer` / `water-tickets`（+`:id` 票种统计；票种表单含运营策略区【引流票/复购激活+需复购单数/单次核销上限，G97 已实现 2026-10】）/ `member-levels` / `package-plans` / `points-rules`（新增页随 F-MKT-010~012） | MKT_READ / MKT_WRITE |
| 地址管理 | `addresses`（筛选分页/编辑/导入弹窗）/ `addresses/map`（热力图+片区维度视图） | ADDRESS_* |
| 配送管理 | `trajectories`（实时位置+回放）/ `areas`（配送片区） | DELIVERY_WORKER_READ / OPS_CONFIG_* |
| 运营配置 | `banners` / `notices` / `feedbacks` / `store-profile`（含门店经纬度【R-OPS-03 距离计算输入，G96 规划中】）/ `delivery-fee-rule`（楼层分档已落地；表单扩展起送门槛+距离阶梯档位表【R-OPS-03 规划中 G96】） | OPS_CONFIG_* / FEEDBACK_* |
| 系统设置 | `system-config/parameters·order·stock·security·notifications·data`（仅超管可见） | SYSTEM_CONFIG_* |
| 消息通知 | `notifications/messages`（筛选/详情/失败重发[仅超管]） | NOTIFICATION_READ / WRITE |
| 日志管理 | `logs/api` / `logs/user-actions` / `logs/business`（表格/时间线双视图） | LOG_READ |

### 3.2 关键实现

- **权限**：登录返回 authorities → 菜单/按钮按权限码渲染；`@PreAuthorize` 为最终防线（前端隐藏不作为隔离手段）；
- **标题**：react-helmet-async 按路由设置页面标题；
- **图表**：折线/柱状/环形/直方图（统计域各图）；双 Y 轴（订单数/营收）；
- **地图组件**：MapLibre + 天地图底图（vec+cva，WGS-84）——轨迹页（实时+折线+时间轴回放加速）、热力图页（标记+热力+Top 列表+汇总+「N 个地址缺少坐标」提示）、送水工详情轨迹 Tab 复用；
- **账号详情聚合**：`accountDetail` 单请求按角色渲染 Tab；管理员视图含权限信息（超管全权限/管理员配送能力范围/兼送水 DELIVERY_TASK）、删除/重置密码仅超管可见；
- **导出/导入交互**：导出按钮 → export_tasks 异步任务（进度/完成后下载链接）；导入五步（模板→上传→校验→预览[行级原因/失败行下载]→确认），无权限禁用；
- **危险操作**：Popconfirm 二次确认（删除商品/取消订单/执行对账[「重复执行会更新已有数据」]/删除账号等）；
- **类型检查**：`tsc -b` 入 build；eslint。

---

下一册：[05-security-and-nfr.md](05-security-and-nfr.md) —— 安全、非功能与运维。
