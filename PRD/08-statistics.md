# 统计与数据域(STA)

> 本域覆盖管理后台数据统计能力:仪表盘与营收/用户/送水工/订单/商品五个统计子页,为运营提供订单、营收、用户、送水工业绩与商品维度的可视化分析,全部为管理员只读查看,无写操作。本域范围:F-STA-001~F-STA-007;规则引用:R-MON-01(01-overview.md §全局约束)、R-PRO-02(03-product.md)、STATISTICS_READ/STATISTICS_EXPORT(01-overview.md §权限码目录)(交叉引用见 §4)。

## 1. 域概述

### 1.1 域范围与功能清单

| F-ID | 名称 | 状态 | 端 |
| --- | --- | --- | --- |
| F-STA-001 | 仪表盘 | [已实现] | 管理后台 |
| F-STA-002 | 营收统计 | [已实现] | 管理后台 |
| F-STA-003 | 用户统计 | [已实现] | 管理后台 |
| F-STA-004 | 送水工统计 | [已实现] | 管理后台 |
| F-STA-005 | 订单统计 | [已实现] | 管理后台 |
| F-STA-006 | 商品统计 | [已实现] | 管理后台 |
| F-STA-007 | 营销统计 | [已实现] | 管理后台 |

> 状态标记以 facts.md §1 校准表为准(2026-08 复查校准):F-STA-002 由「[部分实现]」校准回「[已实现]」(2026-08 营收统计页重建:日/周/月粒度切换 + 时间范围筛选 + 三张汇总卡(营收总额/日均营收/最大单日营收)+「订单趋势」「营收趋势」双图表 + 退款/净营收 G01 说明区,`orderStatistics`/`weeklyStatistics`/`monthlyStatistics` 已接线 `AdminOrderStatisticsQueryResolver`,G12 与 G42 剩余部分解决;多商品订单营收按订单只计一次,P1-3 修复;**2026-08 再补充:退款/净营收维度随支付链路(G01)落地兑现**——退款记录(`payment_refunds` status=COMPLETED)由退款流程落库并接入聚合,营收统计页新增 退款金额/净营收/退款率 汇总卡与「营收/净营收/退款趋势」三线图,净营收 = 已完成营收 − 退款金额);F-STA-001 由原「[部分实现]」校准回「[已实现]」(2026-08-09 仪表盘重建:预设/自定义时间范围、待处理订单卡、环比、错误/空/加载态齐备,`dailyStatistics`/`pendingOrderBreakdown` 已接线 resolver,G33/G34 解决、G42 仪表盘部分解决);F-STA-005 由「[部分实现]」校准回「[已实现]」(2026-08:G37 状态分布/客单价分布/高峰时段分析三分析卡随 `orderStatistics` 查询端到端可用,`OrderStatistics` 增三分布字段,仓储/服务/Resolver 三层聚合补零,前端三列分析卡;G38/G42/P1-5 均已解决,周/月营收口径已对齐已完成订单金额,「已完成订单」卡取 `completedOrders`);其余三项保持原标记。送水工端「当日/周统计」属收银对账域(F-COL-003,独立「统计」Tab 已于 2026 去重调整并入工作台首页),见 06-collection.md。

### 1.2 域内角色与入口

- 使用角色:管理员(对应 01-overview.md §角色权限矩阵「数据统计与日志 → 管理员 ✅」);
- 入口:管理后台左侧菜单「仪表盘」与「数据统计 → 营收统计 / 用户统计 / 送水工统计 / 订单统计 / 商品统计 / 营销统计」六个子菜单(Layout.tsx menuItems);
- 本域全部为只读统计分析,管理员无写权限以外的操作集合;统计页路由:statistics、statistics/users、statistics/delivery-workers、statistics/orders、statistics/products、statistics/marketing。

### 1.3 域级验收清单

> 迁移自拆分前 PRD §11.1「统计」行,一行/域;2026-08 补全权限与时区两条。

- 仪表盘与 5 个统计页数据与订单/商品/送水工数据一致;金额展示精度正确(分→元,两位小数);
- 全部统计查询受 `STATISTICS_READ` 权限码方法级强制(对应 01-overview.md §角色权限矩阵,与 F-STA 全部条目适用);
- 统计按日/周/月聚合时区统一 Asia/Shanghai,口径与订单/商品/用户源数据一致(见 14-misc.md §数据一致性要求)。

## 2. 域内业务规则

本域无自有规则,不定义新 R-;仅引用跨文件规则,适用关系如下:

- **R-MON-01 金额精度**(定义于 01-overview.md §全局约束):适用 F-STA-001、F-STA-002、F-STA-005、F-STA-006;统计聚合金额以"分"存储与计算,展示换算为元、两位小数。
- **R-PRO-02 库存预警阈值**(定义于 03-product.md):适用 F-STA-006;低库存预警计数口径与商品域阈值配置一致。
- **STATISTICS_READ / STATISTICS_EXPORT 权限码**(定义于 01-overview.md §权限码目录 / §角色权限矩阵):适用全部 F-STA;`STATISTICS_READ` 已随仪表盘与用户/送水工/商品统计接线(2026-08,`PERMISSION_STATISTICS_READ` 方法级强制,见 §3 各 F- 条目);`STATISTICS_EXPORT` 为预留码,当前无统计导出功能。
- **域级口径(2026-08 定稿,适用全部 F-STA;2026-10 随 R-ORD-11 送达即完成修订)**:营收口径全域统一 = 已完成订单金额合计(仅 `COMPLETED` 状态,依据 02-business-flows.md §订单状态机「完成配送→已完成→计入业绩」),**按完成时间(`completed_at`)归属日/周/月窗口**——跨日完成订单(下单日 ≠ 完成日)营收计入完成日,与「日完成订单数 = 当日进入已完成状态的订单数」(01-overview.md §全局约束)对齐;**「已完成」在完成配送时直接达成(2026-10 按行业惯例送达即完成,订单域 R-ORD-11):`completed_at` = 完成配送时间,营收即时计入,无「待确认收货」延迟**;订单数口径仍按下单时间(`created_at`)归属窗口(订单数 = 当日全部订单);退款口径(2026-08 补充,F-STA-002)= `payment_refunds` status=COMPLETED 按退款成功时间(processed_at)归属窗口,与退款记录一致;净营收 = 已完成营收 − 退款金额;统计时间聚合时区统一 Asia/Shanghai,且**显式钉死不依赖部署环境默认时区(P2-8):应用入口 `TimeZone.setDefault("Asia/Shanghai")`、JDBC 连接 `TimeZone=Asia/Shanghai`(PostgreSQL 会话时区)、systemd `-Duser.timezone=Asia/Shanghai` 三处一致,实体默认时间戳(LocalDateTime.now())与统计日界均以该时区为准**;统计数字与订单/商品/用户/送水工源数据一致(见 14-misc.md §数据一致性要求)。

## 3. 功能需求详述

### 3.1 管理后台:数据统计(六个功能均为管理后台端)

#### F-STA-001 仪表盘 [已实现]

**需求描述**: 管理后台首页仪表盘集中展示核心经营指标:每日订单量趋势图、每日营收趋势图;今日订单数/今日营收/本月累计营收/待处理订单等关键数字,关键指标带环比;图表数据与订单数据同源一致。
现状:dashboard 真实页面已实现(2026-08-09 重建),关键数字用 StatisticsCard(antd Statistic)卡、趋势图用 recharts(营收折线/订单柱状),数据取自 `dailyStatistics` + `pendingOrderBreakdown` 两个 GraphQL 内联查询,均已有后端 resolver(`DailyStatisticsQueryResolver`,`PERMISSION_STATISTICS_READ`);时间范围支持 Segmented 预设「近7天/近30天/本月」+ RangePicker 自定义,默认近30天;四张数字卡含「待处理订单」(tooltip 三态细分);今日订单/今日营收/本月累计营收均带环比(较昨日/较上月同期);错误态 Alert+重试、空态 Empty、加载态 Spin 齐备;G33/G34 已解决,G42 仪表盘部分已解决,端到端可用。

**页面与入口**: 仪表盘(dashboard)| 管理后台 | 入口:登录后台默认首页,或左侧菜单「仪表盘」。

**UI与交互**:
- **页面布局**:
  1. 筛选区:时间范围切换控件,Segmented 预设「近7天/近30天/本月」+ RangePicker 自定义,默认「近30天」;RangePicker 禁用未来日期。
  2. 关键数字卡区:四张卡(今日订单/今日营收/本月累计营收/待处理订单),今日营收与本月累计营收带「元」后缀、两位小数,卡头配 ShoppingCartOutlined/DollarOutlined/ClockCircleOutlined 图标;今日订单/今日营收带环比标识「较昨日」,本月累计营收带「较上月同期」;待处理订单卡 tooltip 显示「待支付 n · 待派单 n · 待配送 n」三态细分。
  3. 营收趋势卡:标题随所选范围动态显示(如「最近30天营收趋势」),折线图(营收,¥ 刻度)。
  4. 订单趋势卡:标题随所选范围动态显示(如「最近30天订单数」),柱状图(订单数)。
- **组件清单**: Statistic、Card、Row、Col、Spin、Segmented、DatePicker、Alert、Button、Tooltip、Empty(antd)、DailyRevenueChart、DailyOrderChart(均见 dashboard/components/,recharts LineChart/BarChart)、StatisticsCard(components/StatisticsCard,已支持 trend/trendValue/trendLabel 环比呈现,trendLabel 默认「较昨日」可自定义)、useQuery(gql)。
- **关键交互流程**:
  1. 页面挂载即发起 GET_DAILY_STATISTICS(dailyStatistics)查询,日期范围默认近 30 天;查询窗口向前扩展覆盖「昨日」(较昨日环比)与「上月同期」(较上月同期环比)基准,图表/卡片按所选范围切片展示。
  2. 切换「近7天/近30天/本月」预设或 RangePicker 自定义范围 → 重新发起 dailyStatistics 查询 → 两张趋势图与数字卡按所选范围联动刷新。
  3. 数据返回后:取切片最后一条日数据回填「今日订单」「今日营收」,累加所选范围营收回填「本月累计营收」;今日订单/今日营收按切片前一日的昨日同期数据计算环比,本月累计营收按所选范围平移一个月(近30天→前30天、本月→上月1日至同日、近7天→前7天)计算环比;待处理订单由 pendingOrderBreakdown 存量快照查询(仅挂载时取一次,与日期窗口无关)。
  4. 分别渲染两张趋势图(x 轴 MM-DD,营收分→元)。
  5. 加载中两张图表卡内居中显示 Spin。
- **状态呈现**:
  - 空态:所选范围无订单数据时,数字卡值显示 0,图表卡显示 Empty「所选范围暂无数据」,环比显示「—」;
  - 加载态:两张趋势图卡内居中 Spin,数字卡跟随 loading;
  - 错误态:整页 Alert「数据加载失败」+ error.message + 重试按钮,点击重发当前查询;
  - 禁用态:查询期间切换控件仍可操作,由 loading 呈现数据未更新。
- **校验与文案**: 无表单输入;金额统一「元」两位小数;营收 tooltip「日期: xx」「¥x.xx」;订单 tooltip「日期: xx」;环比文案「+x.x% 较昨日」「-x.x% 较上月同期」,上期无数据时显示「—」。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-001-01 | 有近30天订单数据 | 打开仪表盘 | 四张数字卡显示今日订单/今日营收/本月累计营收/待处理订单,营收两位小数带「元」 |
| AC-STA-001-02 | 有近30天数据 | 查看营收趋势卡 | 折线图按日展示营收,悬停 tooltip 显示「日期: xx」「¥x.xx」 |
| AC-STA-001-03 | 有近30天数据 | 查看订单数卡 | 柱状图按日展示订单数,与订单数据一致 |
| AC-STA-001-04 | 今日有完成/在途订单 | 打开仪表盘 | 「今日订单」卡值 = 今日订单总数 |
| AC-STA-001-05 | 数据加载中 | 打开仪表盘 | 两图表卡内显示 Spin 加载态 |
| AC-STA-001-06 | 近30天无数据 | 打开仪表盘 | 数字卡显示 0,图表卡 Empty,环比显示「—」 |
| AC-STA-001-07 | 已进入仪表盘 | 切换时间范围为「近7天」 | 两张趋势图与数字卡按近7天刷新 |
| AC-STA-001-08 | 已进入仪表盘 | RangePicker 选择自定义区间 | 图表与数字卡按所选区间刷新 |
| AC-STA-001-09 | 存在待支付/待派单/待配送订单 | 查看「待处理订单」卡 | 卡值 = 三态订单合计,tooltip 显示三态细分 |
| AC-STA-001-10 | 今日订单数较昨日上升 | 查看「今日订单」卡 | 绿色上箭头 +「+x.x% 较昨日」 |
| AC-STA-001-11 | 今日营收较昨日下降 | 查看「今日营收」卡 | 红色下箭头 +「-x.x% 较昨日」 |
| AC-STA-001-12 | 本月累计营收较上月同期上升 | 查看「本月累计营收」卡 | 绿色上箭头 +「+x.x% 较上月同期」 |
| AC-STA-001-13 | 昨日无数据(补零日) | 查看「今日营收」卡环比 | 环比显示「—」,不报错 |
| AC-STA-001-14 | 有真实订单数据 | 打开仪表盘 | 数字卡与图表正常渲染真实数据,无 GraphQL 报错 |
| AC-STA-001-15 | 后端统计查询异常 | 打开仪表盘 | 显示「数据加载失败」Alert 与重试按钮,点击重试可恢复 |

**规则引用**: R-MON-01(金额精度,01-overview.md)、STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- R-MON-01 扩充:营收聚合以"分"为单位(dailyStatistics.revenueCents),展示统一除 100 转元、两位小数,仅展示层换算。
- 环比口径(已实现):环比增长率 = (本期值 − 上期值) / 上期值 × 100%,保留 1 位小数;上升显示绿色(#3f8600)上箭头、下降显示红色(#cf1322)下箭头、持平显示中性;上期值为 0 且本期 > 0 时显示「—」(避免除零与无穷大误导)。基准:今日订单/今日营收 → 切片前一日(较昨日);本月累计营收 → 所选范围平移一个月(较上月同期);待处理订单为存量指标,不设环比。
- 待处理订单口径(已实现):= 待支付 + 待派单 + 待配送 三态订单合计(pendingOrderBreakdown 存量快照,未进入履约执行中的未完结订单),tooltip 细分三态;若运营仅关注派单,可收窄为仅待派单。
- 营收口径:dailyStatistics.revenueCents = 当日已完成订单金额合计(仅 COMPLETED 状态),按完成时间(`completed_at`)归属当日——跨日完成订单计入完成日,不再随下单日丢失(2026-08 修正:此前按 created_at 归属,导致「今天完成但前几天下单」的订单营收在营收趋势中永久缺失);订单数 orderCount = 当日全部订单(按下单日),两口径分离聚合(与 F-STA-005 统一)。
- 开放问题:仪表盘「今日订单/今日营收」取切片最后一条数据(非当日实时汇总),自定义区间不含今日时显示区间末日数据,口径须与订单统计统一(见 §4 14-misc.md)。
**缺口关联**: G33(时间范围不可切换)、G34(缺「待处理订单」关键数字)均已解决(2026-08-09 仪表盘重建);G42 仪表盘部分已解决(dailyStatistics/pendingOrderBreakdown 已接线),剩余部分关联 F-STA-005。

#### F-STA-002 营收统计 [已实现]

**需求描述**: 按日/周/月维度展示营收(订单金额,已完成口径),支持时间范围筛选与汇总;金额精度正确。退款/净营收维度已随支付链路(G01)落地兑现:退款金额与退款记录(`payment_refunds` status=COMPLETED)一致,净营收 = 营收 − 退款金额。
现状:statistics/index.tsx 真实页面(2026-08 重建),日期粒度 Select(按日/按周/按月)+ RangePicker 时间范围筛选(粒度切换自动重置默认范围:日=近30天、周=近12周、月=近1年,RangePicker 禁用未来日期),汇总卡两行——第一行 营收总额/日均营收/最大单日(周/月)营收,第二行 退款金额/净营收/退款率(退款率 = 退款金额÷营收总额,营收为 0 显示「—」);「订单趋势」(柱状,订单数)+「营收/净营收/退款趋势」(折线,营收绿/净营收蓝虚/退款红,分→元)两图表卡;数据来自 orderStatistics + daily/weekly/monthly Statistics 单查询(按粒度 skip 只启用当前粒度一个查询,CLAUDE.md 规则 10),营收口径 = 已完成订单金额合计(与 F-STA-005 统一,G38),退款口径 = payment_refunds status=COMPLETED 按退款成功时间归属窗口;错误态 Alert+重试、空态 Empty、加载态 Spin 齐备;G12 解决、G42 剩余部分已接线(`AdminOrderStatisticsQueryResolver`,`PERMISSION_STATISTICS_READ`),端到端可用。

**页面与入口**: 数据统计-营收(statistics)| 管理后台 | 入口:左侧菜单「数据统计 → 营收统计」。

**UI与交互**:
- **页面布局**:
  1. 筛选区:日期粒度 Select(按日/按周/按月)+ RangePicker 时间范围选择;切换粒度重置默认范围(日=近30天、周=近12周、月=近1年),时间格式随粒度变化(日/周=YYYY-MM-DD、月=YYYY-MM),RangePicker 禁用未来日期。
  2. 汇总卡区(第一行):营收总额/日均营收/最大单日营收(StatisticsCard,金额「¥」前缀两位小数);日均营收 = 营收总额 ÷ 覆盖天数;周/月粒度时第三卡标题为「最大周营收/最大月营收」,值为该粒度最大周期营收。
  3. 图表区:两列 Card「订单趋势」(柱状,订单数)与「营收/净营收/退款趋势」(折线,营收绿/净营收蓝虚/退款红,分→元),x 轴标签按粒度(日/周=MM-DD、月=年-月)。
  4. 退款/净营收区(2026-08,G01 落地后补充):第二行三张汇总卡 退款金额(¥,退款记录 status=COMPLETED)/净营收(¥ = 营收 − 退款金额)/退款率(退款金额÷营收总额×100%,营收=0 显示「—」);营收趋势卡叠加 退款 与 净营收 两条线,不再展示「依赖支付链路」占位说明。
- **组件清单**: StatisticsCard、Select、DatePicker(RangePicker)、Card、Row、Col、Spin、Alert、Button、Empty(antd)、BarChart、LineChart、XAxis、YAxis、CartesianGrid、Tooltip、ResponsiveContainer(recharts)、useQuery(gql)、dayjs。
- **关键交互流程**:
  1. 页面挂载即发起当前粒度查询(默认按日 + 近30天):orderStatistics(汇总)+ daily/weekly/monthly Statistics(趋势,按粒度 skip 只启用一个查询)。
  2. 切换日/周/月粒度 → 重置默认时间范围并 refetch 对应粒度查询。
  3. 选择时间范围 → 格式化 startDate/endDate 传入 DateRangeInput 并 refetch。
  4. 返回后回填三张汇总卡、聚合趋势数据渲染两图表;营收按已完成订单金额聚合。
  5. 查询失败整页 Alert「数据加载失败」+ error.message + 重试按钮,点击重发当前查询。
- **状态呈现**:
  - 空态:所选范围无数据(无日期行)时汇总卡为 0、图表卡 Empty「所选范围暂无数据」;
  - 加载态:查询期间汇总卡 loading、图表 Spin;
  - 错误态:整页 Alert「数据加载失败」+ error.message + 重试按钮;
  - 禁用态:查询期间粒度/时间控件仍可操作,由 loading 呈现数据未更新。
- **校验与文案**: 时间范围必选且开始≤结束(RangePicker 内置);退款/净营收维度展示真实数据(退款金额/净营收/退款率,与退款记录一致),退款率在营收为 0 时显示「—」;金额统一「元」两位小数(R-MON-01);图表 tooltip「日期: xx」「n 单」「¥x.xx」。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-002-01 | 进入营收统计页 | 打开 /statistics | 页面含粒度选择器、时间范围选择器与「订单趋势」「营收/净营收/退款趋势」两卡片 |
| AC-STA-002-02 | 有已完成订单数据 | 打开 /statistics | 图表区渲染真实趋势数据(订单数/营收),营收两位小数、精度正确(R-MON-01) |
| AC-STA-002-03 | 有已完成订单数据 | 选择日粒度+近30天 | 展示每日营收趋势,金额两位小数、精度正确(R-MON-01) |
| AC-STA-002-04 | 已进入页面 | 查看退款/净营收维度 | 显示 退款金额/净营收/退款率 汇总卡,退款金额与退款记录(payment_refunds status=COMPLETED)一致,净营收 = 营收 − 退款金额,营收趋势图叠加退款/净营收线 |
| AC-STA-002-05 | 已进入页面 | 切换「按周」粒度 | 时间范围重置为近12周,趋势按周聚合展示 |
| AC-STA-002-06 | 已进入页面 | 切换「按月」粒度 | 时间范围重置为近1年,趋势按月聚合展示 |
| AC-STA-002-07 | 已进入页面 | 选择时间范围 | 图表与汇总按所选范围刷新 |
| AC-STA-002-08 | 范围内存在非已完成订单 | 查看营收总额 | 营收总额 = 已完成订单金额合计,已取消/已退款/未完成不计入,两位小数(R-MON-01) |
| AC-STA-002-09 | 存在多商品已完成订单 | 查看营收总额/营收趋势 | 多商品订单金额按订单只计一次,不按明细行数重复求和(P1-3) |
| AC-STA-002-10 | 存在退款记录(COMPLETED) | 查看退款金额/净营收 | 退款金额 = 窗口内退款记录合计(两位小数),净营收 = 营收 − 退款金额 |
| AC-STA-002-11 | 范围内无退款记录 | 查看退款率 | 退款金额显示 0,退款率显示「—」(营收=0 时同样「—」,不报错) |
| AC-STA-002-12 | 退款发生在营收窗口外 | 查看净营收 | 跨窗口退款导致净营收为负时如实展示(不钳 0),口径见业务规则扩充 |

**规则引用**: R-MON-01(金额精度,01-overview.md)、STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- R-MON-01 扩充:营收按"分"聚合、以"元"展示(两位小数);**营收口径定稿 = 已完成订单金额合计**(依据 02-business-flows.md §订单状态机「完成配送→已完成→计入业绩」,已取消/已退款/未完成订单不计入),与 F-STA-005 订单统计统一,解决既有营收图表口径重叠问题。
- 汇总口径(已实现):营收总额 = orderStatistics.totalRevenueCents(已完成口径,G38 映射 completedAmountCents);日均营收 = 营收总额 ÷ 覆盖天数;最大单日营收 = 按日趋势 revenueCents 最大值(周/月粒度展示最大周期营收)。
- 退款/净营收口径(2026-08 定稿,G01 落地后兑现;2026-08-19 增补;2026-10 押金桶恢复):退款金额 = 窗口内 `payment_refunds.status=COMPLETED` 且 `refund_type=ORDER_REFUND` 的 refund_amount_cents 合计,按退款成功时间(processed_at)归属窗口(与服务层 `sumRefundedAmountCents` 一致);净营收 = 已完成营收 − 退款金额(`netRevenueCents`);退款率 = 退款金额 ÷ 营收总额 ×100%,营收为 0 时显示「—」;退款金额与退款记录一致(同源于 payment_refunds),部分退款按 sum 聚合天然支持。**水桶押金退还(DEPOSIT_REFUND)不计入退款/净营收统计**——押金不计入订单营收,退还不产生净营收扭曲(G06);水票退票款(2026-10 未核销可退)计入退款统计。跨窗口边界:退款成功时间在窗口内、订单完成在窗口外时,净营收如实为负,不钳 0(PRD 如实展示口径)。
- 实现要点(2026-08):`OrderStatistics`/`DailyStatistic`/`WeeklyStatistic`/`MonthlyStatistic` 增 refundAmountCents/netRevenueCents/refundCents 字段;`PaymentQueryService`(aqua-foundation)增 sumRefundedAmountCents/getDailyRefundedAmountCents,实现于 aqua-trade(`PaymentRefundRepository`/Impl,QueryDSL,DATE_TRUNC('day') + status=COMPLETED + refund_type=ORDER_REFUND);`StatisticsServiceImpl` 注入 paymentQueryService,日/周/月趋势按退款成功时间归属(周=ISO 周首、月=月首,与订单趋势同桶);退款记录由 `PaymentServiceImpl.refund()` 落库(status=COMPLETED,04-order.md §R-PAY-02)。
- 开放问题:退款统计覆盖在线支付订单退款记录与**水票退票款**(2026-10 未核销可退,G58);现金/线下收款无退款记录,不计入;退款结果异步查询/微信退款回调对账为后续增强。水桶押金及退押(DEPOSIT_REFUND)为独立押金账本,不计入订单营收/退款统计,是否单独统计押金流量待运营确认(G06)。
**缺口关联**: G12(营收统计页为占位)已解决(2026-08 重建);G01(支付链路)已解决(2026-08,退款/净营收维度随之落地);G42 剩余部分(orderStatistics/weeklyStatistics/monthlyStatistics)已接线(`AdminOrderStatisticsQueryResolver`);P1-3(多商品订单营收虚增)已修复(仓储层按订单聚合金额、明细单独聚合件数后合并)。

#### F-STA-003 用户统计 [已实现]

**需求描述**: 用户增长趋势、活跃用户、下单用户分布等维度统计。现状:statistics/users 真实页面,展示 总用户数/今日新增/本月新增/活跃用户/今日登录/累计登录 六张关键数字卡 + 「用户增长趋势(近30天)」「登录趋势(近30天)」两张折线图 + 「下单用户分布(近30天)」环形图与三指标(G35 已落地,2026-08-18),数据来自 userStatistics GraphQL 内联查询(单请求,规则 #10)。

**页面与入口**: 数据统计-用户(statistics/users)| 管理后台 | 入口:左侧菜单「数据统计 → 用户统计」。

**UI与交互**:
- **页面布局**:
  1. 关键数字卡区:两行 StatisticsCard(总用户数/今日新增/本月新增/活跃用户;今日登录/累计登录),活跃用户带「人」后缀、今日新增>0 时带上升 trend 标识。
  2. 增长趋势卡:「用户增长趋势(近30天)」折线图(新增用户,蓝)。
  3. 登录趋势卡:「登录趋势(近30天)」折线图(登录用户,绿)。
  4. 下单用户分布卡(近30天,G35 已实现):左环形图按累计下单次数分层(1次/2~3次/4次以上),右指标列:下单用户数、下单用户占比(下单用户/总用户)、复购率(下单≥2次用户/下单用户)。
- **组件清单**: StatisticsCard(components/StatisticsCard)、Card、Row、Col、Spin(antd)、LineChart、Line、XAxis、YAxis、CartesianGrid、Tooltip、ResponsiveContainer(recharts)、PieChart、Pie、Cell(recharts,G35)、Statistic(antd,G35)、useQuery(gql)。
- **关键交互流程**:
  1. 页面挂载发起 GET_USER_STATISTICS 查询 userStatistics。
  2. 返回后回填六张数字卡;dailyNewUsers 按 date.slice(5) 格式化渲染增长趋势图,dailyLogins 渲染登录趋势图。
  3. 下单用户分布(2026-08-18 落地):后端按近30天下单用户聚合(orders 按 userId 去重计数,`countOrdersPerUserInRange`),下发 `orderDistribution.orderingUsers/orderingUserRatio/repurchaseRate/tiers`;前端环形图 tooltip「n 人」,占比/复购率两位小数百分比。
  4. 查询失败整页显示「加载失败: {error.message}」;加载中数字卡与图表 loading。
- **状态呈现**:
  - 空态:无数据时卡片值 0,图表无数据点,下单用户分布图空、指标 0;
  - 加载态:StatisticsCard 与图表 Spin 加载;
  - 错误态:整页「加载失败: {error.message}」;
  - 禁用态:无交互元素,不适用。
- **校验与文案**: 无表单;日期轴 MM-DD;tooltip 文案「日期: xx」+「n 人 新增用户/登录用户」;下单用户分布 tooltip「n 人」,复购率百分比两位小数。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-003-01 | 有用户数据 | 打开用户统计页 | 六张卡显示总用户数/今日新增/本月新增/活跃用户/今日登录/累计登录 |
| AC-STA-003-02 | 有近30天新增数据 | 查看「用户增长趋势」 | 折线图按日展示新增用户,tooltip「日期: xx」「n 人 新增用户」 |
| AC-STA-003-03 | 有近30天登录数据 | 查看「登录趋势」 | 折线图按日展示登录用户数 |
| AC-STA-003-04 | 今日新增>0 | 查看「今日新增」卡 | 显示今日新增用户数并带上升 trend 标识 |
| AC-STA-003-05 | 后端异常 | 打开用户统计页 | 页面显示「加载失败: {错误信息}」 |
| AC-STA-003-06 | 无用户数据 | 打开用户统计页 | 卡片值 0,图表空态 |
| AC-STA-003-07 | 近30天有下单用户 | 查看「下单用户分布」 | 环形图按次数分层展示,下单用户数与占比正确(G35) |
| AC-STA-003-08 | 存在复购用户 | 查看复购率指标 | 复购率 = 下单≥2次用户数 / 下单用户数 |
| AC-STA-003-09 | 有订单数据 | 核对下单用户分布 | 分层计数与订单数据按 userId 聚合结果一致 |

**规则引用**: STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- 活跃用户口径(已实现,2026-08 定稿):近30天内有下单的用户去重(后端 `countActiveUsersSince` 按订单 createdAt 聚合);如需纳入「登录活跃」口径需后端扩展,当前不纳入。
- 登录口径(已实现):今日登录/累计登录 = 当日/近30天 USER_LOGIN 行为日志按 userId 去重计数;「累计登录」非历史全量累计,与登录趋势图同为近30天窗口。
- 下单用户分布口径(已实现,2026-08-18 定稿):按近30天下单用户去重计数,按累计下单次数分层(1次/2~3次/4次以上);下单用户占比 = 下单用户数/总用户数;复购率 = 下单≥2次用户数/下单用户数;总用户数为 0 或下单用户数为 0 时对应比率为 0,不报错。实现:后端 `StatisticsOrderRepositoryImpl.countOrdersPerUserInRange`(orders 按 userId 分组计数,与活跃用户同窗口同源)+ `UserStatisticsService.getOrderUserDistribution`;`UserStatistics` 增 `orderDistribution` 字段,随 `userStatistics` 单查询下发。
**缺口关联**: G35(用户统计缺「下单用户分布」维度)已解决(2026-08-18)。

#### F-STA-004 送水工统计 [已实现]

**需求描述**: 送水工业绩排行(完成单量/好评率/收入),人均单量、平均履约时长。现状:statistics/delivery-workers 真实页面,展示 送水工总数/今日在岗/配送中订单/今日完成 四张数字卡 + 「送水工工作量排行」柱状图 + 「送水工排行榜」表格(排名/姓名/今日完成订单/好评率/累计收入),数据来自 deliveryWorkerStatistics 与 deliveryWorkerRanking(limit:10) 查询;「人均单量」「平均履约时长」维度当前未实现,为目标补充(G36)。好评率依赖评价数据,评价链路未接线(G05),好评率可能为空或失真。

**页面与入口**: 数据统计-送水工(statistics/delivery-workers)| 管理后台 | 入口:左侧菜单「数据统计 → 送水工统计」。

**UI与交互**:
- **页面布局**:
  1. 关键数字卡区:送水工总数/今日在岗/配送中订单/今日完成 + 「人均单量」「平均履约时长」两张卡(G36 已实现,2026-08-18;人均单量 = 今日完成订单总数/今日在岗送水工数,「单/人」后缀;平均履约时长 = Σ(completedAt − 派单时间)/今日完成订单数,「分钟」后缀;无数据时显示「—」)。
  2. 排行图区:左「送水工工作量排行」柱状图(Top10 今日完成订单),右「送水工排行榜」表格(排名/姓名/今日完成订单/好评率/累计收入),分页关闭、size=small;目标形态:排行榜下方或表头标注口径说明「排行默认按今日完成订单排序,好评率/累计收入为展示列,支持列头切换排序」。
- **组件清单**: StatisticsCard、Card、Row、Col、Table、Spin(antd)、BarChart、Bar、XAxis、YAxis、CartesianGrid、Tooltip、ResponsiveContainer(recharts)、useQuery(gql)。
- **关键交互流程**:
  1. 页面挂载发起 GET_DELIVERY_WORKER_STATISTICS(deliveryWorkerStatistics + deliveryWorkerRanking limit:10)。
  2. ranking 取前 10 生成柱状图与表格;好评率按 rating×100 百分比显示、累计收入按分转元(¥x.xx)。
  3. 履约效率指标(2026-08-18 落地):人均单量 = 今日完成订单总数/今日在岗送水工数(在岗为 0 时为 null → 「—」);平均履约时长 = Σ(completedAt − 派单时间)/今日完成订单数(派单时间 = 最早 DELIVERY_ASSIGNED 订单操作时间,回退 deliveryStartedAt,再回退 createdAt;无完成订单时为 null → 「—」)。
  4. 查询失败显示「加载失败: {error.message}」。
- **状态呈现**:
  - 空态:无排行数据时柱状图空、表格空,人均单量/平均履约时长显示「—」;
  - 加载态:图表 Spin、表格 loading;
  - 错误态:「加载失败: {error.message}」;
  - 禁用态:无交互元素,不适用。
- **校验与文案**: 好评率列百分比显示(如 95.0%);收入列「¥」前缀两位小数;柱状图 tooltip「送水工: 姓名」「n 单 今日完成订单」;人均单量「n 单/人」、平均履约时长「n 分钟」。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-004-01 | 有送水工数据 | 打开送水工统计页 | 四张卡显示送水工总数/今日在岗/配送中订单/今日完成 |
| AC-STA-004-02 | 有排行数据 | 查看「送水工工作量排行」 | 柱状图展示 Top10 今日完成订单 |
| AC-STA-004-03 | 有排行数据 | 查看「送水工排行榜」表格 | 按排名展示姓名/今日完成订单/好评率/累计收入,好评率百分比、收入「¥」两位小数 |
| AC-STA-004-04 | 有评价数据 | 查看好评率列 | 好评率 = 该送水工好评数/评价总数 |
| AC-STA-004-05 | 无评价数据(G05) | 查看好评率列 | 好评率显示 0.0% 或空,说明评价数据源缺失 |
| AC-STA-004-06 | 后端异常 | 打开送水工统计页 | 页面显示「加载失败: {错误信息}」 |
| AC-STA-004-07 | 今日有完成订单与在岗送水工 | 查看「人均单量」卡 | 人均单量 = 今日完成订单总数/今日在岗送水工数,口径标注(G36) |
| AC-STA-004-08 | 有已完成订单 | 查看「平均履约时长」卡 | 平均履约时长按 completedAt − 派单时间聚合展示(分钟) |

**规则引用**: STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- 好评率口径:rating 以 0~1 小数存储,展示乘 100 转百分比(如 95.0%);当前评价链路无写入(G05),好评率依赖评价功能(F-ORD-007)落地,无评价数据时显示 0.0%。
- 排行实现(已实现):`deliveryWorkerRanking(limit:10)` 按「今日完成订单」降序返回 Top10,柱状图与表格同源;好评率/累计收入为展示列;需求所述业绩综合排行(完成单量/好评率/收入)排序口径待明确,目标形态以默认排序 + 列头切换排序承载。
- 人均单量口径(已实现,2026-08-18 定稿):今日完成订单总数 / 今日在岗送水工数(在岗口径);今日在岗为 0 时返回 null,前端显示「—」。
- 平均履约时长口径(已实现,2026-08-18 定稿):Σ(completedAt − 派单时间)/ 今日完成订单数,单位分钟,保留 1 位小数;「派单时间」= order_operations 中该订单最早 DELIVERY_ASSIGNED 操作时间,未命中回退 `deliveryStartedAt`(出发时间),再回退 `createdAt`;无今日完成订单时返回 null,前端显示「—」。实现:`StatisticsDeliveryWorkerRepositoryImpl.findCompletedOrderTimingsSince`/`findEarliestAssignedAtByOrderIds` + `DeliveryWorkerStatisticsServiceImpl.getDeliveryWorkerStatistics`;`DeliveryWorkerOverview` 增 `averageOrdersPerWorker`/`averageFulfillmentMinutes`(可空)。
- **送水工业绩口径(R-ORD-11,2026-10 修订)**:「今日完成订单」= 当日「已完成」订单数(2026-10 送达即完成,已无「待确认收货」态),时间锚点 = 完成配送时间(送达时间,`completed_at`)——送水工业绩按完成配送即时计入;排行/人均单量/平均履约时长同口径(履约时长终点 = 完成配送时间)。
- 开放问题:排名当前固定按「今日完成订单」口径。
**缺口关联**: G36(送水工统计缺人均单量/平均履约时长)已解决(2026-08-18);G05(评论功能未接线——好评率数据源缺失)。

#### F-STA-005 订单统计 [已实现]

**需求描述**: 订单量趋势、状态分布、客单价分布、高峰时段分析。现状:statistics/orders 真实页面,支持 按日/按周/按月 粒度切换 + 时间范围筛选,展示 订单总数/营收总额/平均客单价/已完成订单 四张数字卡 + 「订单与营收趋势」折线图(订单数/送水桶数/营收,双 Y 轴);「订单状态分布」(环形图,7 态,合计=订单总数)、「客单价分布」(直方图,0-20/20-50/50-100/100-200/200+ 元,合计=订单总数)、「高峰时段分析」(柱状图,0-23 时,标注高峰小时)三张分析卡已实现(G37 已解决,2026-08,数据随 `orderStatistics` 查询下发,状态/客单价/高峰三分布均按所选范围全状态聚合、合计恒等于订单总数);「已完成订单」卡已改取 `completedOrders`,与「订单总数」区分(G38 已修复,2026-08);营收口径统一为已完成订单金额(G38/P1-5 已修复);`dailyStatistics`/`orderStatistics`/`weeklyStatistics`/`monthlyStatistics` 均已接线 resolver(2026-08,`DailyStatisticsQueryResolver` + `AdminOrderStatisticsQueryResolver`),页面端到端可用(G42 剩余部分已解决)。

**页面与入口**: 数据统计-订单(statistics/orders)| 管理后台 | 入口:左侧菜单「数据统计 → 订单统计」。

**UI与交互**:
- **页面布局**:
  1. 筛选区:「日期粒度」Select(按日/按周/按月)+ RangePicker 时间范围;切换粒度自动重置默认范围(日=近30天、周=近12周、月=近1年),时间格式随粒度变化(日/周=YYYY-MM-DD、月=YYYY-MM)。
  2. 关键数字卡区:订单总数/营收总额/平均客单价/已完成订单(营收与客单价带「¥」前缀);「已完成订单」卡口径 = 状态为「已完成」的订单数,与「订单总数」区分(G38)。
  3. 趋势图卡:「订单与营收趋势」折线图,双 Y 轴(左=订单数/送水桶数,右=营收 ¥)。
  4. 分析卡区(已实现,G37):三列卡「订单状态分布」「客单价分布」「高峰时段分析」——状态分布环形图按 7 态聚合所选范围订单数,tooltip 数量与占比、卡内合计=订单总数;客单价分布直方图按金额区间(0-20/20-50/50-100/100-200/200+ 元,区间可配置)聚合订单数,卡内注明区间合计=订单总数;高峰时段柱状图按小时(0-23)聚合所选范围订单创建量(createdAt),订单量最大小时柱高亮并标注高峰小时;三卡始终渲染图表展示真实数值,无订单时展示 0 值数据(状态图例 7 态计数 0、客单价 5 档区间柱高 0、高峰 24 小时柱高 0),不使用占位空态。
- **组件清单**: StatisticsCard、Card、Row、Col、Select、Spin、Space、DatePicker(antd)、LineChart、Line、XAxis、YAxis、Tooltip、Legend、ResponsiveContainer、PieChart、Pie、Cell、BarChart、Bar(recharts)、useQuery(gql)、dayjs。
- **关键交互流程**:
  1. 默认「按日」+ 近30天;按粒度发起对应查询(daily/weekly/monthly Statistics,以 skip 控制只发当前粒度)。
  2. 切换粒度:重置时间范围默认值并 refetch 对应查询。
  3. 选择时间范围:格式化 startDate/endDate 传入 DateRangeInput 并 refetch。
  4. 返回后回填数字卡、聚合趋势数据渲染折线图;「已完成订单」取后端 completedOrders 字段(区别于 totalOrders)(G38);失败显示「加载失败: {error.message}」。
  5. 已实现:按所选范围聚合订单状态/客单价/创建小时,分别渲染状态分布环形图、客单价直方图、高峰时段柱状图(时区统一 Asia/Shanghai);三分布随 `orderStatistics` 查询一并下发(单页面单请求,CLAUDE.md 规则 10),随粒度/时间范围筛选联动刷新。
- **状态呈现**:
  - 空态:无数据卡片为 0、折线无数据点;三分析卡仍渲染图表展示 0 值(状态分布图例 7 态计数 0、客单价分布 5 档区间柱高 0、高峰时段 24 小时柱高 0,卡内合计/区间合计显示 0);
  - 加载态:当前粒度查询 loading,数字卡与图表 Spin;
  - 错误态:「加载失败: {error.message}」;
  - 禁用态:查询期间粒度/时间控件仍可操作,由 loading 呈现数据未更新(现状说明)。
- **校验与文案**: 粒度选项「按日/按周/按月」;tooltip:订单数/送水桶数「n 单」、营收「¥x.xx」;金额统一「元」两位小数(R-MON-01);状态分布 tooltip「状态名: n 单 (x.x%)」;客单价区间「0-20元」等;高峰时段「n 时: n 单」。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-005-01 | 有订单数据 | 打开订单统计页(默认按日近30天) | 四张卡显示订单总数/营收总额/平均客单价/已完成订单,营收与客单价带「¥」 |
| AC-STA-005-02 | 有订单数据 | 查看「订单与营收趋势」 | 折线图展示订单数/送水桶数/营收三线,双 Y 轴刻度正确 |
| AC-STA-005-03 | 已进入页面 | 切换粒度为「按周」 | 时间范围重置为近12周,趋势按周聚合展示 |
| AC-STA-005-04 | 已进入页面 | 切换粒度为「按月」 | 时间范围重置为近1年,趋势按月聚合展示 |
| AC-STA-005-05 | 已进入页面 | 选择自定义时间范围 | 图表与数字卡按所选范围刷新 |
| AC-STA-005-06 | 有营收数据 | 查看营收总额 | 金额两位小数、精度正确(R-MON-01) |
| AC-STA-005-07 | 后端异常 | 打开订单统计页 | 页面显示「加载失败: {错误信息}」 |
| AC-STA-005-08 | 无订单数据 | 打开订单统计页 | 卡片为 0,趋势图无数据点 |
| AC-STA-005-09 | 存在已取消/已退款订单 | 查看「已完成订单」卡 | 卡值 = 状态为「已完成」的订单数,与「订单总数」不同(G38) |
| AC-STA-005-10 | 存在非已完成订单 | 查看营收总额 | 营收总额仅计已完成订单金额,已取消/已退款/未完成不计入,与 F-STA-002 口径一致(G38) |
| AC-STA-005-11 | 有订单数据 | 查看「订单状态分布」 | 环形图按所选范围各状态数量与占比,合计 = 订单总数(G37 ✅) |
| AC-STA-005-12 | 有订单数据 | 查看「客单价分布」 | 直方图按金额区间聚合,各区间合计与订单数据一致(G37 ✅) |
| AC-STA-005-13 | 有订单数据 | 查看「高峰时段分析」 | 柱状图按小时展示订单创建量,高峰小时 = 订单量最大小时(G37 ✅) |
| AC-STA-005-14 | orderStatistics/weeklyStatistics/monthlyStatistics 已接线 | 打开订单统计页 | 数字卡与图表正常加载真实数据,无 GraphQL 报错(G42 剩余部分解决,2026-08 ✅) |

**规则引用**: R-MON-01(金额精度,01-overview.md)、STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- R-MON-01 扩充:营收/客单价按"分"聚合、以"元"展示(两位小数)。
- G38 口径修正定稿:「已完成订单」卡 = 状态为「已完成」的订单数(订单状态机 7 态中唯一计入口径,依据 02-business-flows.md §订单状态机「完成配送→已完成→计入业绩」),不得复用 totalOrders;营收总额 = 已完成订单金额合计,与 F-STA-002 统一。
- G38 修复要点(2026-08 补充):当前生成类型 `OrderStatistics`(shared-types.graphqls)仅有 totalOrders/totalRevenueCents/averageOrderValueCents/dateRange,**缺 `completedOrders` 字段**;修复需 (a) shared-types.graphqls 扩展 `completedOrders: Int` (b) resolver 以 `OrderStatisticsDTO.completedOrders/completedAmountCents` 映射(服务端已分别计算全状态金额与已完成金额,营收总额须映射 `completedAmountCents`,不得混用) (c) 前端「已完成订单」卡改用该字段。**(2026-08 已实施**:`OrderStatistics` 增 `completedOrders: Int!`;`AdminOrderStatisticsQueryResolver.orderStatistics` 映射 `completedOrders/completedAmountCents`;前端卡改取 `completedOrders`。)
- 客单价分布区间(已实现):0-20/20-50/50-100/100-200/200+ 元,区间边界为服务层常量(`StatisticsServiceImpl.defaultOrderValueBuckets`),可配置扩展;金额按订单 `amountCents` 分桶只计一次(与 P1-3 口径一致);聚合范围为所选时间窗口全部订单,各区间合计 = 订单总数。
- 高峰时段分析(已实现):按 createdAt 小时聚合(0-23),时区统一 Asia/Shanghai;订单量最大小时柱高亮并展示「高峰时段: X时 (n 单)」。
- 状态分布口径(已实现):按所选范围全部订单按 7 态聚合(`OrderModelStatus.entries` 补零),合计 = 订单总数;零值状态不入图,避免零扇区。
- G37 实现要点(2026-08):`shared-types.graphqls` 的 `OrderStatistics` 增 `statusDistribution`/`orderValueDistribution`/`peakHourDistribution` 三字段;`StatisticsOrderRepositoryImpl` 增 `countOrdersGroupByStatus`/`countOrdersGroupByHour`/`countOrdersByAmountBucket`(QueryDSL,EXTRACT(HOUR)/CASE WHEN);`StatisticsServiceImpl` 增三分布服务方法(补零);`AdminOrderStatisticsQueryResolver.orderStatistics` 映射三分布(仍 `PERMISSION_STATISTICS_READ`);前端订单统计页新增三列分析卡。
- 开放问题:当前用户下单直落「待派单」(G09/G01),状态分布呈现以实际数据为准。
**缺口关联**: G37(缺状态分布/客单价分布/高峰时段分析)已解决(2026-08,三分析卡随 `orderStatistics` 查询端到端可用);G38/G42 已解决(2026-08:`orderStatistics`/`weeklyStatistics`/`monthlyStatistics` 已接线 `AdminOrderStatisticsQueryResolver`,`dailyStatistics`/`pendingOrderBreakdown` 维持已接线,订单统计页端到端可用;营收口径 = 已完成订单金额,P1-5 周/月分两次聚合已对齐)。

#### F-STA-006 商品统计 [已实现]

**需求描述**: 商品销量排行、销售额贡献、库存预警列表。现状:statistics/products 真实页面,展示 商品总数/在售商品/低库存预警/总销量/总收藏数 五张数字卡 + 「日销量趋势」折线图(支持按商品筛选+时间范围)+ 「商品销量排行 TOP10」(销量/营收双 Y 轴柱状图)+ 「商品收藏排行 TOP10」柱状图;需求所列「库存预警列表」当前仅以「低库存预警」计数卡呈现,未提供明细列表,为目标补充(G39);库存预警阈值可配置依赖 R-PRO-02(03-product.md),当前无阈值配置 UI,为目标补充。

**页面与入口**: 数据统计-商品(statistics/products)| 管理后台 | 入口:左侧菜单「数据统计 → 商品统计」。

**UI与交互**:
- **页面布局**:
  1. 关键数字卡区:商品总数/在售商品/低库存预警(计数>0 时带上升 trend 标识)/总销量(「桶」后缀)/总收藏数。
  2. 日销量趋势卡:标题「日销量趋势」,右上角 商品选择 Select(占位「选择商品(可选)」,可清除)+ RangePicker;未选商品展示全商品按日聚合趋势,选中商品展示该商品日销量/营收。
  3. 排行卡区:左「商品销量排行 TOP10」(销量/营收双系列柱状图,商品名超 10 字截断加「...」),右「商品收藏排行 TOP10」(收藏数柱状图)。
  4. 库存预警明细卡(G39 已实现,2026-08-18):「库存预警明细」表格,列 = 商品名/规格/当前库存/阈值/状态(预警:0 < 库存 ≤ 阈值;缺货:库存 = 0),按库存升序、缺货置顶;表头「阈值配置」入口跳转系统设置-库存预警阈值(F-SYS-003,system-config/stock)。
- **组件清单**: StatisticsCard、Card、Row、Col、Spin、Select、Space、DatePicker、Table、Tag、Button(antd)、LineChart、Line、BarChart、Bar、XAxis、YAxis、Tooltip、Legend、ResponsiveContainer(recharts)、useQuery(gql)、useNavigate(react-router-dom)、dayjs。
- **关键交互流程**:
  1. 页面挂载并发查询 productStatistics、productDailySales(默认近30天)、productsPaginated(商品下拉数据源)。
  2. 选择商品或时间范围 → refetch productDailySales → 刷新趋势图(选中商品则按 productId 过滤)。
  3. 销量排行/收藏排行取 productStatistics.productRanking / favoriteStatistics 渲染 TOP10 柱状图。
  4. 库存预警明细(G39,2026-08-18 落地):取 `productStatistics.stockWarningItems`(库存≤阈值,缺货置顶、按库存升序),渲染「库存预警明细」表;状态列 缺货红/预警橙;「阈值配置」按钮跳转 `/system-config/stock`。
  5. 查询失败显示「加载失败: {statsError.message}」。
- **状态呈现**:
  - 空态:无数据卡片为 0、图表空、预警明细表空;
  - 加载态:数字卡与图表 Spin、表格 loading;
  - 错误态:「加载失败: {错误信息}」;
  - 禁用态:无交互元素,不适用。
- **校验与文案**: 销量单位「桶」;营收「¥」两位小数(分转元);商品名超 10 字截断加「...」;tooltip:销量「n 桶」、营收「¥x.xx」、收藏「n 人」;预警明细状态列:预警橙、缺货红。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-006-01 | 有商品数据 | 打开商品统计页 | 五张卡显示商品总数/在售商品/低库存预警/总销量/总收藏数,总销量带「桶」后缀 |
| AC-STA-006-02 | 有销量数据 | 查看「商品销量排行 TOP10」 | 柱状图展示销量与营收双系列,与商品销量数据一致 |
| AC-STA-006-03 | 有收藏数据 | 查看「商品收藏排行 TOP10」 | 柱状图按收藏数排序展示 |
| AC-STA-006-04 | 已进入页面 | 选择时间范围 | 日销量趋势按所选范围刷新 |
| AC-STA-006-05 | 已进入页面 | 在商品下拉选择某商品 | 趋势图切换为该商品日销量/营收 |
| AC-STA-006-06 | 有营收数据 | 查看营收显示 | 营收两位小数、精度正确(R-MON-01) |
| AC-STA-006-07 | 存在库存低于阈值商品 | 查看「低库存预警」卡 | 显示低库存商品数,口径与 R-PRO-02 阈值一致 |
| AC-STA-006-08 | 后端异常 | 打开商品统计页 | 页面显示「加载失败: {错误信息}」 |
| AC-STA-006-09 | 存在库存≤阈值商品 | 查看「库存预警明细」表 | 列出商品名/当前库存/阈值,按库存升序(G39) |
| AC-STA-006-10 | 存在库存=0 商品 | 查看「库存预警明细」表 | 库存=0 商品标记「缺货」并置顶 |
| AC-STA-006-11 | 阈值已配置(R-PRO-02) | 查看「库存预警明细」表 | 明细口径与阈值一致,阈值配置后即时生效 |

**规则引用**: R-MON-01(金额精度,01-overview.md)、R-PRO-02(库存预警阈值,03-product.md)、STATISTICS_READ(权限码,01-overview.md §权限码目录)。
**业务规则扩充**:
- R-PRO-02 扩充(引用):库存预警阈值默认值与配置入口见 03-product.md;本页「低库存预警」计数与「库存预警明细」表格须与该阈值一致,阈值配置后即时生效。
- R-MON-01 扩充:营收按"分"聚合、以"元"展示;排行图营收同样分→元换算。
- 库存预警明细口径(已实现,2026-08-18 定稿):库存 ≤ 阈值(默认 10)进入预警,库存 = 0 标记「缺货」并置顶;明细列表与「低库存预警」计数卡同源同口径(`ProductStatisticsServiceImpl.getProductStatistics` 同一次聚合生成 `stockWarningItems`,阈值实时读 R-PRO-02 配置)。
- 现状确认(2026-08):`productStatistics`/`productDailySales` 已接线 resolver(`ProductStatisticsQueryResolver`,`PERMISSION_STATISTICS_READ`);日销量趋势按商品筛选 + 时间范围刷新、全商品按日聚合为前端聚合逻辑,数据与商品销量一致。
**缺口关联**: G39(商品统计缺库存预警明细与阈值配置 UI)已解决(2026-08-18;阈值配置 UI 归 F-SYS-003,已于 2026-08 落地 system-config/stock)。

#### F-STA-007 营销统计 [已实现]

**需求描述**: 营销效果时间维度分析(营销订单/让利/用券/水票/赠品)+ 营销方式使用明细,为运营评估营销投入产出提供数据支撑。现状(2026-08 落地):基于**第 3 层日汇总中间表**(`daily_marketing_facts`/`daily_marketing_usage_facts`,v2.5.0 迁移,夜间快照任务与既有四张事实表同事务 delete-then-insert)查询侧两段合并,与 F-STA-001~006 同构;页面「效果统计 + 明细表」:时间范围筛选(默认近 30 天,日粒度)+ 六张汇总卡(营销订单数/让利总额/用券数/水票销售金额/水票核销/赠品数量)+ 四张图表(营销优惠趋势[券/满减/首单立减三线]、营销订单与用券趋势、水票销售与核销趋势、营销方式构成环形图)+ 营销方式使用明细表(Segmented 按 券/满减/首单立减/买赠满赠/桶水票 切换,行操作跳详情);数据来自 `marketingStatistics` 单查询(`AdminMarketingStatisticsQueryResolver`,`PERMISSION_STATISTICS_READ`),单页面单请求(规则 #10),端到端可用。

**页面与入口**: 数据统计-营销(statistics/marketing)| 管理后台 | 入口:左侧菜单「数据统计 → 营销统计」。

**UI与交互**:
- **页面布局**:
  1. 筛选区:时间范围 RangePicker(默认近 30 天,禁用未来日期,日粒度,含手动刷新/定时刷新);
  2. 汇总卡区:营销订单数(单)/让利总额(¥)/用券数(张)/水票销售金额(¥)/水票核销(张)/赠品数量(件);
  3. 图表区:左上「营销优惠趋势」折线(券/满减/首单立减 优惠金额三线,¥)、右上「营销方式构成」环形(让利金额占比)、左下「营销订单与用券趋势」柱+线、右下「水票销售与核销趋势」双线(销售/核销张数);
  4. 明细表区:「营销方式使用明细」Segmented 切换(优惠券/满减折扣/首单立减/买赠满赠/桶水票),列随类型变化——券:券名/使用订单数/优惠金额/核销张数;满减/首单:活动名/使用订单数/优惠金额/使用数量;赠品:活动名/使用订单数/优惠金额(估算)/赠品件数;票种:票种名/销售张数/销售金额/核销张数;行操作「详情」跳对应活动详情页(营销管理)。
- **组件清单**: StatisticsCard、Card、Row、Col、Spin、Alert、Button、Empty、Segmented、Table、Tooltip、DatePicker(RangePicker)(antd)、LineChart/ComposedChart/PieChart 等(recharts)、useQuery(gql)、dayjs、RefreshControls/useAutoRefresh。
- **关键交互流程**:
  1. 页面挂载发起 `marketingStatistics` 查询(默认近 30 天);
  2. 切换时间范围 → refetch → 汇总卡/图表/明细表联动刷新;
  3. 明细表切换 Segmented → 前端按 kind 过滤展示该营销方式区间使用记录;
  4. 行「详情」→ 跳转 /marketing/{coupons|promotions|newcomer|buy-gift|water-tickets}/:id。
- **状态呈现**:
  - 空态:无数据汇总卡 0、图表 Empty、明细表 Empty;
  - 加载态:汇总卡/图表/表格 loading;
  - 错误态:整页 Alert「数据加载失败」+ error.message + 重试按钮;
  - 禁用态:查询期间筛选/切换仍可操作,由 loading 呈现数据未更新。
- **校验与文案**: 金额统一「元」两位小数(R-MON-01);图表 tooltip「日期: xx」+「¥x.xx」/「n 张」/「n 单」;明细表优惠金额列 tooltip 口径说明(券/满减/首单=快照精确,赠品=估算);让利总额不含赠品估算与桶水票抵扣(预收抵扣非让利),页面/PRD 明示。

**验收测试用例**:

| AC编号 | 前置条件 | 操作步骤 | 预期结果 |
| --- | --- | --- | --- |
| AC-STA-007-01 | 有营销数据 | 打开营销统计页 | 六张汇总卡显示营销订单数/让利总额/用券数/水票销售金额/水票核销/赠品数量,金额两位小数带「¥」 |
| AC-STA-007-02 | 有券订单 | 查看「营销优惠趋势」 | 折线图按日展示券/满减/首单立减 优惠金额(元) |
| AC-STA-007-03 | 有营销订单 | 查看「营销订单与用券趋势」 | 柱状图展示营销订单数 + 用券数折线,与订单/券数据一致 |
| AC-STA-007-04 | 有水票数据 | 查看「水票销售与核销趋势」 | 双线图按日展示销售/核销张数 |
| AC-STA-007-05 | 有让利数据 | 查看「营销方式构成」 | 环形图按 券/满减/首单立减 让利金额占比展示 |
| AC-STA-007-06 | 有券模板使用 | 明细表默认「优惠券」 | 展示券名称/使用订单数/优惠金额/核销张数,「详情」跳转券详情 |
| AC-STA-007-07 | 有满减活动使用 | 明细表切「满减/折扣」 | 展示活动区间使用记录,优惠金额=快照列合计 |
| AC-STA-007-08 | 有票种数据 | 明细表切「桶水票」 | 展示票种销售张数/销售金额/核销张数,「详情」跳转票种统计页 |
| AC-STA-007-09 | 有赠品订单 | 明细表切「买赠/满赠」 | 优惠金额列展示估算金额(件数×目录价)并带口径说明 |
| AC-STA-007-10 | 区间无营销数据 | 打开营销统计页 | 汇总卡 0、图表/明细表空态,不报错 |
| AC-STA-007-11 | 后端统计查询异常 | 打开营销统计页 | 「数据加载失败」Alert + 重试按钮,点击重试可恢复 |
| AC-STA-007-12 | 已进入页面 | 切换时间范围 | 汇总卡/图表/明细表按所选范围刷新 |
| AC-STA-007-13 | 夜间快照已运行且今日有新数据 | 打开营销统计页 | 昨日及以前读中间表、今日实时兜底,两段合并数据与源数据一致 |

**规则引用**: STATISTICS_READ(权限码,01-overview.md §权限码目录)、R-MON-01(金额精度,01-overview.md §全局约束)。
**业务规则扩充**:
- **中间表机制(与 F-STA-001~006 同构)**:`daily_marketing_facts`(单日一行,无数据日全 0 保证覆盖连续)与 `daily_marketing_usage_facts`(日 × 营销方式 kind × 活动 ref_id)由夜间快照任务(`StatisticsSnapshotServiceImpl.rebuildDate`,六表同一事务 delete-then-insert)写入;查询侧两段合并——覆盖历史段读中间表,未覆盖日期(今日/缺口)实时聚合兜底(与快照写同口径),`StatisticsSnapshotCoverageService` 覆盖区间判定不变。
- **营销订单口径(已实现,定稿)**:区间内订单含 券/满减/首单立减 任一快照优惠列 > 0(coupon_discount_cents/promotion_discount_cents/first_order_discount_cents),按订单去重(券与满减可同单只计 1);营销订单数由每日事实独立聚合,不能由明细求和得出。
- **让利总额口径(已实现,定稿)**:= 券 + 满减 + 首单立减 三快照列合计(精确,与 10-marketing.md 营销方式详情口径一致);赠品让利估算(件数 × 赠品当前目录价,估算)仅在明细表展示并标注,不并入总额;桶水票抵扣为预收抵扣(被核销桶按单价 0 计入,非让利)不并入总额。
- **用券数口径**:user_coupons status=USED 按 used_at 归属区间(与营销方式详情「已核销券张数」同口径)。
- **水票口径**:销售 = ticket_purchases paymentStatus=PAID 按 paidAt(回退 createdAt)归属,张数含赠送(N+M);核销 = user_tickets USED 按 usedAt 归属;与 F-MKT-007 票种统计同口径。
- **赠品件数口径**:order_gifts 按 created_at 归属区间,Σ quantity。
- **明细表口径**:券行按 orders.coupon_id → user_coupons.coupon_template_id 归并(含已退回券订单——订单快照保留 coupon_id);满减/首单按订单快照活动列;赠品按 gift_activity_id(usage_count=赠品件数,让利估算);票种按 ticket_type_id(销售/核销);活动名查询侧批量解析(中间表不落 name 列,活动改名即时生效)。
- **开放问题**:限时特价/新人专享价因 order_items 无活动 ID 快照,本页汇总/构成/明细均不纳入(估算口径与查询成本),由各活动详情页承接(与 10-marketing.md §营销方式详情口径一致);后续 order_items 落活动 ID 快照后可补充。
**缺口关联**: 无(2026-08 新增功能)。

## 4. 交叉引用
- 状态机 → 02-business-flows.md §订单状态机(F-STA-005 订单统计口径依赖订单状态;营收口径 = 已完成订单金额,依据「完成配送→已完成→计入业绩」)。
- 缺口 → 14-misc.md(G33/G34 已解决;G12 营收统计页为占位已解决(2026-08 重建);G35~G39 为统计维度补全、G42 剩余部分(orderStatistics/weeklyStatistics/monthlyStatistics)已接线、G38「已完成订单」卡口径已修正;G01 已解决(2026-08,支付链路落地,退款/净营收维度随之兑现,见 F-STA-002);现状登记见 facts.md §2;G05 影响好评率)。
- 权限码 → 01-overview.md §权限码目录/§角色权限矩阵(STATISTICS_READ 已接线、STATISTICS_EXPORT 为预留码)。
- 实体与数据一致性 → 14-misc.md §数据一致性要求(送水工统计 ↔ 订单、商品统计 ↔ 销量、用户统计活跃口径 = 近30天下单用户)。
- 营销口径 → 10-marketing.md §营销方式详情口径与 R-MKT-02/07(F-STA-007 让利总额/用券数/水票/明细表口径与营销域一致;限时特价/新人专享价不纳入见开放问题)。
- 异常 → 14-misc.md(统计查询异常处理)。
- 全局金额精度 → 01-overview.md §全局约束 R-MON-01。
