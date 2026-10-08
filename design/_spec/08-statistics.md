# 08 统计与数据域 页面规格

> 来源：`PRD/08-statistics.md`（F-STA-001~007，全部 [已实现]），仅管理后台端，只读统计分析，无写操作。
> 权限码：`STATISTICS_READ`（方法级强制，全部 F-STA 适用）；`STATISTICS_EXPORT` 为**预留码**，当前无统计导出功能——本文所有「导出」均标「目标形态」。
> 全局口径（2026-08 定稿，2026-10 随 R-ORD-11 送达即完成修订）：
> - 营收 = 已完成订单金额合计（仅 `COMPLETED`），按完成时间 `completed_at` 归属日/周/月窗口，送达即完成即时计入；跨日完成订单计入完成日（下单日 ≠ 完成日）。
> - 订单数 = 当日/窗口全部订单，按下单时间 `created_at` 归属。
> - 退款 = `payment_refunds` status=COMPLETED 且 refund_type=ORDER_REFUND 的 refund_amount_cents 合计，按退款成功时间 processed_at 归属；水桶押金退还（DEPOSIT_REFUND）不计入；净营收 = 已完成营收 − 退款金额（跨窗口可如实为负，不钳 0）。
> - 时间聚合时区统一 Asia/Shanghai；金额以「分」存储/聚合，展示换算「元」两位小数（R-MON-01）。

## 页面 1：管理后台 · 仪表盘（建议文件名 dashboard.html）
- 功能编号：F-STA-001 [已实现]
- 所属端：管理后台
- 页面入口与跳转：登录后台默认首页，或左侧菜单「仪表盘」；路由 `dashboard`；无子页跳转
- 布局结构（从上到下）：
  1. 筛选区：时间范围切换控件（Segmented 预设 + RangePicker 自定义）
  2. 关键数字卡区（一行四卡）：今日订单 / 今日营收 / 本月累计营收 / 待处理订单
  3. 营收趋势卡：折线图（营收）
  4. 订单趋势卡：柱状图（订单数）
- 组件清单：
  - 卡片：StatisticsCard（antd Statistic 封装，支持 trend / trendValue / trendLabel 环比呈现）、Card、Row、Col
  - 筛选：Segmented（近7天/近30天/本月）、DatePicker.RangePicker（禁用未来日期）
  - 反馈：Spin、Alert（错误态）、Empty（空态）、Button（重试）
  - 提示：Tooltip（待处理订单三态细分）
  - 图表：DailyRevenueChart（recharts LineChart）、DailyOrderChart（recharts BarChart）
  - 卡头图标：ShoppingCartOutlined（今日订单）、DollarOutlined（营收类）、ClockCircleOutlined（待处理订单）
- 关键交互：
  1. 页面挂载即发起 `dailyStatistics` 查询，默认近 30 天；窗口向前扩展以覆盖「昨日」与「上月同期」环比基准。
  2. 切换 Segmented 预设或 RangePicker 自定义范围 → 重新查询 → 两张趋势图与数字卡联动刷新。
  3. 数据返回后：取切片最后一条日数据回填「今日订单/今日营收」；累加所选范围营收回填「本月累计营收」。
  4. 待处理订单由 `pendingOrderBreakdown` 存量快照查询，仅挂载取一次，与日期窗口无关。
  5. 渲染两张趋势图（x 轴 MM-DD，营收分→元）。
- 状态与标签：
  - 空态：所选范围无数据 → 数字卡值 0，图表卡 Empty「所选范围暂无数据」，环比「—」
  - 加载态：图表卡内居中 Spin，数字卡跟随 loading
  - 错误态：整页 Alert「数据加载失败」+ error.message + 重试按钮
  - 禁用态：查询期间筛选控件仍可操作，由 loading 呈现数据未更新
  - 环比色：上升绿色 #3f8600 上箭头；下降红色 #cf1322 下箭头；持平中性
- 关键数据字段：
  - `dailyStatistics.revenueCents`（当日已完成营收，分）、`dailyStatistics.orderCount`（当日全部订单数，按下单日）
  - `pendingOrderBreakdown`（待支付 / 待派单 / 待配送 三态存量）
  - 派生：今日订单、今日营收、本月累计营收、待处理订单合计、环比增长率（保留 1 位小数）
- 图表规格：
  - 营收趋势：折线图（LineChart）；X 轴 = 日期 MM-DD；Y 轴 = 营收（¥ 元，分→元两位小数）；指标口径 = 当日已完成订单金额合计（按 `completed_at` 归属）；标题随所选范围动态显示（如「最近30天营收趋势」）；tooltip「日期: xx」「¥x.xx」；时间范围切换 = Segmented 近7天/近30天/本月 + RangePicker 自定义（禁用未来日期，默认近30天）；导出按钮 = 无（目标形态，`STATISTICS_EXPORT` 预留）。
  - 订单趋势：柱状图（BarChart）；X 轴 = 日期 MM-DD；Y 轴 = 订单数（n 单）；指标口径 = 当日全部订单数（按 `created_at` 归属）；标题「最近30天订单数」动态；tooltip「日期: xx」；时间范围同上；导出同上。
- 页面文案：
  - 卡标题：「今日订单」「今日营收」「本月累计营收」「待处理订单」
  - 环比：「+x.x% 较昨日」「-x.x% 较上月同期」；上期无数据显示「—」
  - 待处理订单 tooltip：「待支付 n · 待派单 n · 待配送 n」
  - 空态：「所选范围暂无数据」；错误态：「数据加载失败」+「重试」
  - 图表标题示例：「最近30天营收趋势」「最近30天订单数」
- 设计要点/禁忌：
  - 营收一律带「元」后缀与两位小数；订单数为整数，不混用单位。
  - 待处理订单为存量指标，**不设环比**，避免误导。
  - 环比上期为 0 且本期 > 0 时必须显示「—」，不得显示无穷大或除零错误。
  - 时间范围默认近 30 天，RangePicker 禁用未来日期。
- 备注：目标形态——统计导出（`STATISTICS_EXPORT`）。开放问题：今日订单/今日营收取切片最后一条数据（非当日实时汇总），自定义区间不含今日时显示区间末日数据，口径须与 F-STA-005 统一。缺口 G33/G34 已解决，G42 仪表盘部分已解决。

## 页面 2：管理后台 · 营收统计（建议文件名 revenue-statistics.html）
- 功能编号：F-STA-002 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 营收统计」；路由 `statistics`
- 布局结构（从上到下）：
  1. 筛选区：日期粒度 Select（按日/按周/按月）+ RangePicker 时间范围
  2. 汇总卡区第一行（三卡）：营收总额 / 日均营收 / 最大单日（周/月）营收
  3. 汇总卡区第二行（三卡）：退款金额 / 净营收 / 退款率
  4. 图表区（两列）：左「订单趋势」柱状图；右「营收/净营收/退款趋势」折线图
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 筛选：Select（粒度）、DatePicker.RangePicker（禁用未来日期）
  - 反馈：Spin、Alert、Button（重试）、Empty
  - 图表：recharts BarChart / LineChart / XAxis / YAxis / CartesianGrid / Tooltip / ResponsiveContainer
  - 工具：dayjs
- 关键交互：
  1. 挂载即发起当前粒度查询（默认按日 + 近30天）：`orderStatistics`（汇总）+ daily/weekly/monthly Statistics（趋势，按粒度 skip 只启用一个查询）。
  2. 切换日/周/月粒度 → 重置默认时间范围并 refetch 对应粒度查询。
  3. 选择时间范围 → 格式化 startDate/endDate 传入 `DateRangeInput` 并 refetch。
  4. 返回后回填六张汇总卡、聚合趋势渲染两图表；营收按已完成订单金额聚合。
  5. 失败整页 Alert + 重试按钮重发当前查询。
- 状态与标签：
  - 空态：无日期行 → 汇总卡 0、图表 Empty「所选范围暂无数据」
  - 加载态：汇总卡 loading、图表 Spin
  - 错误态：整页 Alert「数据加载失败」+ error.message + 重试
  - 禁用态：查询期间粒度/时间控件仍可操作
  - 退款率：营收为 0 时显示「—」
- 关键数据字段：
  - `orderStatistics.totalRevenueCents`（营收总额，映射 `completedAmountCents`）
  - `DailyStatistic/WeeklyStatistic/MonthlyStatistic` 的 `refundAmountCents` / `netRevenueCents` / `refundCents`（及营收、订单数）
  - 派生：日均营收 = 营收总额 ÷ 覆盖天数；最大单日（周/月）营收 = 趋势 revenueCents 最大值；退款率 = 退款金额 ÷ 营收总额 ×100%
- 图表规格：
  - 订单趋势：柱状图（BarChart）；X 轴 = 日/周 MM-DD、月 YYYY-MM；Y 轴 = 订单数（n 单）；指标口径 = 窗口内全部订单数（按下单时间）；时间范围切换 = 粒度切换自动重置（日=近30天、周=近12周、月=近1年）；tooltip「日期: xx」「n 单」；导出按钮 = 无（目标形态）。
  - 营收/净营收/退款趋势：折线图（LineChart，三线）；X 轴 = 日/周 MM-DD、月 YYYY-MM；Y 轴 = 金额（¥ 元，分→元两位小数）；指标口径 = 营收（绿，已完成订单金额）、净营收（蓝虚线 = 营收 − 退款）、退款（红，`payment_refunds` COMPLETED 按 processed_at）；tooltip「日期: xx」「¥x.xx」；时间范围同上；导出同上。
- 页面文案：
  - 卡标题：「营收总额」「日均营收」「最大单日营收 / 最大周营收 / 最大月营收」「退款金额」「净营收」「退款率」
  - 粒度选项：「按日」「按周」「按月」；tooltip：「日期: xx」「n 单」「¥x.xx」
  - 空态：「所选范围暂无数据」；错误态：「数据加载失败」+「重试」
- 设计要点/禁忌：
  - 营收总额 = 已完成订单金额合计，已取消/已退款/未完成不计入；多商品订单金额按订单只计一次（不按明细行重复求和）。
  - 净营收可如实为负，**禁止钳 0**。
  - 退款率在营收为 0 时显示「—」，不报错；押金退还（DEPOSIT_REFUND）不计入退款/净营收。
  - 金额统一「元」两位小数（R-MON-01）。
- 备注：目标形态——统计导出（`STATISTICS_EXPORT`）。退款/净营收随支付链路 G01 落地兑现；缺口 G12 已解决、G01 已解决、G42 剩余部分已接线；P1-3 已修复。

## 页面 3：管理后台 · 用户统计（建议文件名 user-statistics.html）
- 功能编号：F-STA-003 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 用户统计」；路由 `statistics/users`
- 布局结构（从上到下）：
  1. 关键数字卡区第一行：总用户数 / 今日新增 / 本月新增 / 活跃用户
  2. 关键数字卡区第二行：今日登录 / 累计登录
  3. 增长趋势卡：「用户增长趋势(近30天)」折线图（新增用户，蓝）
  4. 登录趋势卡：「登录趋势(近30天)」折线图（登录用户，绿）
  5. 下单用户分布卡（近30天）：左环形图（1次/2~3次/4次以上）+ 右指标列（下单用户数 / 下单用户占比 / 复购率）
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 反馈：Spin
  - 图表：recharts LineChart / Line / XAxis / YAxis / CartesianGrid / Tooltip / ResponsiveContainer；PieChart / Pie / Cell（环形图）；antd Statistic（右侧指标列）
- 关键交互：
  1. 挂载发起 `userStatistics` 单查询（GET_USER_STATISTICS）。
  2. 返回后回填六张数字卡；`dailyNewUsers`（date.slice(5) 格式化）渲染增长趋势图，`dailyLogins` 渲染登录趋势图。
  3. 下单用户分布按近 30 天下单用户聚合（按 userId 去重计数），下发 `orderDistribution.orderingUsers/orderingUserRatio/repurchaseRate/tiers`。
  4. 查询失败整页「加载失败: {error.message}」。
- 状态与标签：
  - 空态：卡片值 0、图表无数据点、下单用户分布图空、指标 0
  - 加载态：StatisticsCard 与图表 Spin
  - 错误态：整页「加载失败: {error.message}」
  - 禁用态：无交互元素，不适用
  - 今日新增 > 0：带上升 trend 标识
- 关键数据字段：
  - 总用户数、今日新增、本月新增、活跃用户（「人」后缀）、今日登录、累计登录
  - `dailyNewUsers[]`（新增用户趋势）、`dailyLogins[]`（登录趋势）
  - `orderDistribution.orderingUsers / orderingUserRatio / repurchaseRate / tiers`（下单用户分布）
- 图表规格：
  - 用户增长趋势：折线图；X 轴 = 日期 MM-DD（近30天固定）；Y 轴 = 新增用户数（人）；指标口径 = 当日新增用户；tooltip「日期: xx」「n 人 新增用户」；时间范围切换 = 无（固定近30天）；导出按钮 = 无（目标形态）。
  - 登录趋势：折线图；X 轴 = 日期 MM-DD（近30天）；Y 轴 = 登录用户数（人）；指标口径 = 当日 USER_LOGIN 行为日志按 userId 去重；tooltip「日期: xx」「n 人 登录用户」；时间范围 = 固定近30天；导出 = 无（目标形态）。
  - 下单用户分布：环形图（PieChart）；维度 = 累计下单次数分层（1次 / 2~3次 / 4次以上）；指标口径 = 近30天下单用户去重按次数分层；tooltip「n 人」；右列指标 = 下单用户数、下单用户占比（下单用户/总用户）、复购率（下单≥2次/下单用户），百分比两位小数；时间范围 = 固定近30天；导出 = 无（目标形态）。
- 页面文案：
  - 卡标题：「总用户数」「今日新增」「本月新增」「活跃用户」「今日登录」「累计登录」
  - 图标题：「用户增长趋势(近30天)」「登录趋势(近30天)」「下单用户分布(近30天)」
  - 指标文案：下单用户数、下单用户占比、复购率
  - tooltip：「日期: xx」「n 人 新增用户 / 登录用户」；分布「n 人」
  - 错误态：「加载失败: {error.message}」
- 设计要点/禁忌：
  - 活跃用户口径 = 近30天内下单用户去重（不含登录活跃）。
  - 「累计登录」非历史全量，与登录趋势同为近30天窗口，文案须避免「历史累计」歧义。
  - 占比/复购率两位小数；总用户或下单用户为 0 时比率显示 0，不报错。
- 备注：缺口 G35 已解决（下单用户分布，2026-08-18）。本页无时间范围切换与导出（目标形态）。

## 页面 4：管理后台 · 送水工统计（建议文件名 delivery-worker-statistics.html）
- 功能编号：F-STA-004 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 送水工统计」；路由 `statistics/delivery-workers`
- 布局结构（从上到下）：
  1. 关键数字卡区：送水工总数 / 今日在岗 / 配送中订单 / 今日完成
  2. 效率卡区：人均单量 / 平均履约时长
  3. 排行图区左：「送水工工作量排行」柱状图（Top10 今日完成订单）
  4. 排行图区右：「送水工排行榜」表格（排名/姓名/今日完成订单/好评率/累计收入）
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 反馈：Spin、Table loading
  - 表格：Table（分页关闭、size=small）
  - 图表：recharts BarChart / Bar / XAxis / YAxis / CartesianGrid / Tooltip / ResponsiveContainer
- 关键交互：
  1. 挂载发起 `deliveryWorkerStatistics` + `deliveryWorkerRanking(limit:10)`。
  2. ranking 取前 10 生成柱状图与表格；好评率按 rating×100 百分比显示、累计收入分→元（¥x.xx）。
  3. 效率指标：人均单量 = 今日完成订单总数 / 今日在岗送水工数（在岗 0 → null 显示「—」）；平均履约时长 = Σ(completedAt − 派单时间) / 今日完成订单数（派单时间 = 最早 DELIVERY_ASSIGNED，回退 deliveryStartedAt，再回退 createdAt；无完成订单 → null 显示「—」）。
  4. 查询失败显示「加载失败: {error.message}」。
- 状态与标签：
  - 空态：柱状图空、表格空，人均单量/平均履约时长「—」
  - 加载态：图表 Spin、表格 loading
  - 错误态：「加载失败: {error.message}」
  - 禁用态：无交互元素，不适用
  - 效率卡：无数据时显示「—」（非 0）
- 关键数据字段：
  - 送水工总数、今日在岗、配送中订单、今日完成
  - `DeliveryWorkerOverview.averageOrdersPerWorker`（人均单量，可空）、`averageFulfillmentMinutes`（平均履约时长，可空）
  - ranking 字段：排名、姓名、今日完成订单、好评率、累计收入
- 图表规格：
  - 送水工工作量排行：柱状图（BarChart）；X 轴 = 送水工姓名（Top10）；Y 轴 = 今日完成订单数（n 单）；指标口径 = 当日已完成订单数（完成配送时间 `completed_at`，送达即完成）；tooltip「送水工: 姓名」「n 单 今日完成订单」；时间范围切换 = 无（固定今日）；导出按钮 = 无（目标形态）。
  - 送水工排行榜：表格（非图表）；列 = 排名/姓名/今日完成订单/好评率/累计收入；排序口径 = 默认按今日完成订单降序（目标形态：支持列头切换排序）；好评率 = rating×100%（如 95.0%）；累计收入「¥」两位小数；分页关闭。
- 页面文案：
  - 卡标题：「送水工总数」「今日在岗」「配送中订单」「今日完成」「人均单量」「平均履约时长」
  - 效率卡后缀：人均单量「n 单/人」、平均履约时长「n 分钟」（保留 1 位小数）
  - 表格列头：排名 / 姓名 / 今日完成订单 / 好评率 / 累计收入
  - 错误态：「加载失败: {error.message}」
- 设计要点/禁忌：
  - 好评率依赖评价链路（G05 未接线），无评价数据时显示 0.0% 或空，须有数据源缺失说明。
  - 人均单量/平均履约时长无数据时显示「—」而非 0。
  - 排序当前固定按「今日完成订单」，列头切换排序标目标形态。
- 备注：目标形态——排行榜列头切换排序、业绩综合排行排序口径。缺口 G36 已解决（人均单量/平均履约时长，2026-08-18）；G05 影响好评率。

## 页面 5：管理后台 · 订单统计（建议文件名 order-statistics.html）
- 功能编号：F-STA-005 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 订单统计」；路由 `statistics/orders`
- 布局结构（从上到下）：
  1. 筛选区：日期粒度 Select（日/周/月）+ RangePicker
  2. 关键数字卡区：订单总数 / 营收总额 / 平均客单价 / 已完成订单
  3. 趋势图卡：「订单与营收趋势」折线图（双 Y 轴）
  4. 分析卡区（三列）：订单状态分布（环形）/ 客单价分布（直方图）/ 高峰时段分析（柱状）
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 筛选：Select、DatePicker.RangePicker
  - 反馈：Spin、Space
  - 图表：recharts LineChart / Line / XAxis / YAxis / Tooltip / Legend / ResponsiveContainer；PieChart / Pie / Cell；BarChart / Bar
  - 工具：dayjs
- 关键交互：
  1. 默认「按日」+ 近30天；按粒度发起对应查询（daily/weekly/monthly Statistics，以 skip 控制只发当前粒度）。
  2. 切换粒度：重置时间范围默认值并 refetch。
  3. 选择时间范围：格式化 startDate/endDate 传入 `DateRangeInput` 并 refetch。
  4. 返回后回填数字卡、聚合趋势渲染折线图；「已完成订单」取 `completedOrders`（区别于 totalOrders）。
  5. 三分布（状态/客单价/高峰）随 `orderStatistics` 一并下发，随粒度/时间范围联动刷新。
- 状态与标签：
  - 空态：卡片 0、折线无数据点；三分析卡仍渲染 0 值图表（状态 7 态计数 0、客单价 5 档柱高 0、高峰 24 小时柱高 0），不使用占位空态
  - 加载态：当前粒度查询 loading，数字卡与图表 Spin
  - 错误态：「加载失败: {error.message}」
  - 禁用态：查询期间粒度/时间控件仍可操作
  - 高峰标注：订单量最大小时柱高亮并标注「高峰时段: X时 (n 单)」
- 关键数据字段：
  - 订单总数（totalOrders）、营收总额（已完成订单金额）、平均客单价（averageOrderValueCents）、已完成订单（completedOrders）
  - `statusDistribution`（7 态）、`orderValueDistribution`（客单价 5 档）、`peakHourDistribution`（0-23 时）
- 图表规格：
  - 订单与营收趋势：折线图（LineChart，双 Y 轴）；X 轴 = 日/周 MM-DD、月 YYYY-MM；左 Y 轴 = 订单数/送水桶数（n），右 Y 轴 = 营收（¥）；指标口径 = 订单数按 `created_at`、营收按已完成订单金额（`completed_at`）；tooltip：订单数/送水桶数「n 单」、营收「¥x.xx」；时间范围切换 = 粒度联动（日=近30天、周=近12周、月=近1年）；导出按钮 = 无（目标形态）。
  - 订单状态分布：环形图（PieChart，7 态）；指标口径 = 所选范围全部订单按 7 态聚合（`OrderModelStatus.entries` 补零，零值不入图），合计 = 订单总数；tooltip「状态名: n 单 (x.x%)」；时间范围 = 随粒度/范围联动；导出 = 无（目标形态）。
  - 客单价分布：直方图（BarChart）；X 轴 = 金额区间「0-20元 / 20-50元 / 50-100元 / 100-200元 / 200+元」；Y 轴 = 订单数；指标口径 = 订单 `amountCents` 分桶只计一次（区间为服务层常量，可配置），各区间合计 = 订单总数；时间范围 = 联动；导出 = 无（目标形态）。
  - 高峰时段分析：柱状图（BarChart，0-23 时）；X 轴 = 小时 0-23；Y 轴 = 订单创建量；指标口径 = 按 `created_at` 小时聚合（时区 Asia/Shanghai）；tooltip「n 时: n 单」；时间范围 = 联动；导出 = 无（目标形态）。
- 页面文案：
  - 卡标题：「订单总数」「营收总额」「平均客单价」「已完成订单」
  - 粒度选项：「按日」「按周」「按月」
  - 分析卡标题：「订单状态分布」「客单价分布」「高峰时段分析」
  - tooltip：订单数/送水桶数「n 单」、营收「¥x.xx」、状态「状态名: n 单 (x.x%)」、客单价「0-20元」等、高峰「n 时: n 单」
  - 错误态：「加载失败: {error.message}」
- 设计要点/禁忌：
  - 「已完成订单」卡必须取 `completedOrders`，不得复用 `totalOrders`（G38）。
  - 营收总额仅计已完成订单，已取消/已退款/未完成不计入，与 F-STA-002 口径一致。
  - 三分析卡无数据时仍渲染真实 0 值图表，不使用空态占位。
  - 金额统一「元」两位小数。
- 备注：缺口 G37 已解决（状态/客单价/高峰三分析卡，2026-08）、G38/G42 已解决。开放问题：用户下单直落「待派单」（G09/G01），状态分布以实际数据为准。

## 页面 6：管理后台 · 商品统计（建议文件名 product-statistics.html）
- 功能编号：F-STA-006 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 商品统计」；路由 `statistics/products`；跳转：库存预警明细卡「阈值配置」入口 → 系统设置-库存预警阈值（F-SYS-003，`/system-config/stock`）
- 布局结构（从上到下）：
  1. 关键数字卡区：商品总数 / 在售商品 / 低库存预警 / 总销量 / 总收藏数
  2. 日销量趋势卡（右上角 商品选择 Select + RangePicker）
  3. 排行卡区（两列）：左「商品销量排行 TOP10」柱状图（销量/营收双系列）；右「商品收藏排行 TOP10」柱状图
  4. 库存预警明细卡：「库存预警明细」表格（表头含「阈值配置」入口）
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 筛选：Select（商品，占位「选择商品(可选)」，可清除）、DatePicker.RangePicker、Space、Button（阈值配置跳转）
  - 表格：Table、Tag（预警橙 / 缺货红）
  - 反馈：Spin
  - 图表：recharts LineChart / Line / BarChart / Bar / XAxis / YAxis / Tooltip / Legend / ResponsiveContainer
  - 路由：useNavigate
- 关键交互：
  1. 挂载并发查询 `productStatistics`、`productDailySales`（默认近30天）、商品下拉数据源。
  2. 选择商品或时间范围 → refetch `productDailySales` → 刷新趋势图（选中商品按 productId 过滤）。
  3. 销量排行/收藏排行取 `productStatistics.productRanking` / `favoriteStatistics` 渲染 TOP10 柱状图。
  4. 库存预警明细取 `productStatistics.stockWarningItems`（库存 ≤ 阈值，缺货置顶、按库存升序）渲染表格；状态列 缺货红/预警橙；「阈值配置」按钮跳转 `/system-config/stock`。
  5. 查询失败显示「加载失败: {statsError.message}」。
- 状态与标签：
  - 空态：卡片 0、图表空、预警明细表空
  - 加载态：数字卡与图表 Spin、表格 loading
  - 错误态：「加载失败: {error.message}」
  - 禁用态：无交互元素，不适用
  - 预警状态标签：预警（黄橙，0 < 库存 ≤ 阈值）；缺货（红，库存 = 0，置顶）
  - 低库存预警计数 > 0：带上升 trend 标识
- 关键数据字段：
  - 商品总数、在售商品、低库存预警、总销量（「桶」后缀）、总收藏数
  - `productStatistics.stockWarningItems`（商品名/规格/当前库存/阈值/状态）
  - `productRanking`（销量/营收）、`favoriteStatistics`（收藏数）
  - `productDailySales`（日销量/营收）
- 图表规格：
  - 日销量趋势：折线图（LineChart）；X 轴 = 日期 MM-DD；Y 轴 = 销量（桶）；指标口径 = 未选商品按全商品按日聚合，选中商品按 productId 过滤展示其日销量/营收；tooltip 销量「n 桶」、营收「¥x.xx」；时间范围切换 = RangePicker（默认近30天）；导出按钮 = 无（目标形态）。
  - 商品销量排行 TOP10：柱状图（BarChart，双系列 = 销量 + 营收）；X 轴 = 商品名（超 10 字截断加「...」）；Y 轴 = 销量（桶）/ 营收（¥）；指标口径 = 商品销量与营收排行（营收分→元）；tooltip 销量「n 桶」、营收「¥x.xx」；时间范围 = 随筛选联动；导出 = 无（目标形态）。
  - 商品收藏排行 TOP10：柱状图（BarChart）；X 轴 = 商品名；Y 轴 = 收藏数（「人」）；指标口径 = 商品收藏数排行；tooltip「n 人」；时间范围 = 联动；导出 = 无（目标形态）。
- 页面文案：
  - 卡标题：「商品总数」「在售商品」「低库存预警」「总销量（桶）」「总收藏数」
  - 图标题：「日销量趋势」「商品销量排行 TOP10」「商品收藏排行 TOP10」「库存预警明细」
  - 表格列头：商品名 / 规格 / 当前库存 / 阈值 / 状态；状态值「预警」「缺货」
  - 商品下拉占位：「选择商品(可选)」；按钮「阈值配置」；tooltip「n 桶」「¥x.xx」「n 人」
  - 错误态：「加载失败: {error.message}」
- 设计要点/禁忌：
  - 低库存预警计数与库存预警明细表必须同源同口径（阈值默认 10），阈值配置后即时生效。
  - 库存 = 0 归「缺货」并置顶；0 < 库存 ≤ 阈值 归「预警」。
  - 商品名超 10 字截断加「...」；营收两位小数（R-MON-01）。
  - 库存着色仅作用于在售/库存维度，不影响下架商品（与 F-SYS-003 AC 一致）。
- 备注：缺口 G39 已解决（库存预警明细与阈值配置 UI，2026-08-18；阈值配置 UI 归 F-SYS-003）。目标形态——统计导出（`STATISTICS_EXPORT`）。

## 页面 7：管理后台 · 营销统计（建议文件名 marketing-statistics.html）
- 功能编号：F-STA-007 [已实现]
- 所属端：管理后台
- 页面入口与跳转：左侧菜单「数据统计 → 营销统计」；路由 `statistics/marketing`；跳转：明细表行「详情」→ 对应营销活动详情页 `/marketing/{coupons|promotions|newcomer|buy-gift|water-tickets}/:id`
- 布局结构（从上到下）：
  1. 筛选区：RangePicker 时间范围（默认近30天，禁用未来日期，日粒度，含手动刷新 / 定时刷新）
  2. 汇总卡区（六卡）：营销订单数（单）/ 让利总额（¥）/ 用券数（张）/ 水票销售金额（¥）/ 水票核销（张）/ 赠品数量（件）
  3. 图表区（2×2）：
     - 左上「营销优惠趋势」折线（券/满减/首单立减 优惠金额三线，¥）
     - 右上「营销方式构成」环形（让利金额占比）
     - 左下「营销订单与用券趋势」柱 + 线
     - 右下「水票销售与核销趋势」双线（销售/核销张数）
  4. 明细表区：「营销方式使用明细」Segmented 切换（优惠券/满减折扣/首单立减/买赠满赠/桶水票）+ 表格
- 组件清单：
  - 卡片：StatisticsCard、Card、Row、Col
  - 筛选：DatePicker.RangePicker、Segmented（明细类型）、Button；刷新：RefreshControls / useAutoRefresh
  - 反馈：Spin、Alert、Empty
  - 表格：Table、Tooltip（优惠金额口径说明）
  - 图表：recharts LineChart / ComposedChart / PieChart 等
  - 工具：dayjs
- 关键交互：
  1. 挂载发起 `marketingStatistics` 单查询（默认近30天）。
  2. 切换时间范围 → refetch → 汇总卡/图表/明细表联动刷新。
  3. 明细表切换 Segmented → 前端按 kind 过滤展示该营销方式区间使用记录。
  4. 行「详情」→ 跳转对应活动详情页（营销管理）。
  5. 失败整页 Alert + 重试按钮。
- 状态与标签：
  - 空态：无数据汇总卡 0、图表 Empty、明细表 Empty
  - 加载态：汇总卡/图表/表格 loading
  - 错误态：整页 Alert「数据加载失败」+ error.message + 重试
  - 禁用态：查询期间筛选/切换仍可操作
  - 明细类型：Segmented 当前项高亮
- 关键数据字段：
  - 营销订单数、让利总额、用券数、水票销售金额、水票核销、赠品数量
  - 趋势：券/满减/首单立减 优惠金额、营销订单数、用券数、水票销售/核销张数
  - 明细行（随类型）：券 = 券名/使用订单数/优惠金额/核销张数；满减/首单 = 活动名/使用订单数/优惠金额/使用数量；赠品 = 活动名/使用订单数/优惠金额(估算)/赠品件数；票种 = 票种名/销售张数/销售金额/核销张数
- 图表规格：
  - 营销优惠趋势：折线图（三线：券/满减/首单立减）；X 轴 = 日期 MM-DD；Y 轴 = 优惠金额（¥）；指标口径 = 券 + 满减 + 首单立减 三快照优惠列金额（精确）；tooltip「日期: xx」「¥x.xx」；时间范围切换 = RangePicker（默认近30天，日粒度）；导出按钮 = 无（目标形态）。
  - 营销方式构成：环形图（PieChart）；维度 = 券 / 满减 / 首单立减；指标口径 = 各方式让利金额占比（不含赠品估算与桶水票抵扣）；tooltip 占比与金额；时间范围 = 联动；导出 = 无（目标形态）。
  - 营销订单与用券趋势：柱 + 线（ComposedChart）；柱 = 营销订单数（单），线 = 用券数（张）；X 轴 = 日期 MM-DD；指标口径 = 营销订单（含券/满减/首单立减任一快照列 > 0 按订单去重）+ 用券数（user_coupons USED 按 used_at）；tooltip「n 单」「n 张」；时间范围 = 联动；导出 = 无（目标形态）。
  - 水票销售与核销趋势：双线图（LineChart）；线 = 销售张数 / 核销张数；X 轴 = 日期 MM-DD；指标口径 = 销售（ticket_purchases PAID 按 paidAt，含赠送 N+M）/ 核销（user_tickets USED 按 usedAt）；tooltip「n 张」；时间范围 = 联动；导出 = 无（目标形态）。
- 页面文案：
  - 卡标题：「营销订单数（单）」「让利总额（¥）」「用券数（张）」「水票销售金额（¥）」「水票核销（张）」「赠品数量（件）」
  - 图标题：「营销优惠趋势」「营销方式构成」「营销订单与用券趋势」「水票销售与核销趋势」
  - 明细 Segmented：「优惠券」「满减折扣」「首单立减」「买赠满赠」「桶水票」
  - tooltip：「日期: xx」「¥x.xx」「n 张」「n 单」；赠品优惠金额列口径说明「赠品估算 = 件数 × 当前目录价」
  - 让利总额说明：「让利总额不含赠品估算与桶水票抵扣（预收抵扣非让利）」
  - 错误态：「数据加载失败」+「重试」
- 设计要点/禁忌：
  - 让利总额 = 券 + 满减 + 首单立减 三快照列合计（精确），不含赠品估算与桶水票抵扣，页面须明示。
  - 营销订单数由每日事实独立聚合，不能由明细求和得出。
  - 明细表优惠金额列须区分口径：券/满减/首单 = 快照精确；赠品 = 估算（带 Tooltip 说明）。
  - 金额统一「元」两位小数（R-MON-01）。
- 备注：2026-08 新增功能（无缺口）。底层为第 3 层日汇总中间表（`daily_marketing_facts` / `daily_marketing_usage_facts`），查询侧两段合并（历史读中间表、今日/缺口实时兜底）。开放问题：限时特价/新人专享价因 order_items 无活动 ID 快照，本页不纳入。目标形态——统计导出（`STATISTICS_EXPORT`）。
