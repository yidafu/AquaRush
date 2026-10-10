# AquaRush 送水小程序产品需求文档(PRD)

| 文档信息 | 内容 |
| --- | --- |
| 文档名称 | AquaRush 送水小程序产品需求文档 |
| 版本 | v0.31(合规决策:明确不做用户储值,F-MKT-010 收敛为套餐卡) |
| 编写日期 | 2026-08-09 |
| 文档状态 | 草稿(待评审) |
| 覆盖范围 | 用户端小程序 / 送水工端独立 App(delivery-app,React Native + Expo) / 管理后台(三端) |
| 文档性质 | 产品视角需求文档,以最优用户体验为目标设计;参考当前代码现状,区分已实现与规划中功能 |

### 修订记录

| 日期 | 版本 | 变更内容 | 作者 |
| --- | --- | --- | --- |
| 2026-08-07 | v0.1 | 初稿,覆盖三端完整产品需求 | - |
| 2026-08-08 | v0.2 | 按功能域拆分为多文件;细化 UI/交互细节、验收测试用例、业务规则;基于代码现状校准 11 处实现状态标记;新增缺口 G10-G41 | - |
| 2026-08-08 | v0.3 | F-LOG-003 业务日志增强:新增 GraphQL 查询、业务域/操作类型筛选(登录/提交订单/查看产品等)与按用户时间线视图(表格/时间线切换);新增缺口 G43 | - |
| 2026-08-09 | v0.4 | 将 10-data/11-exceptions/12-gaps-roadmap/13-nfr/14-acceptance/15-appendices 六个文件合并为 11-misc.md,目录导航同步更新 | - |
| 2026-08-09 | v0.5 | 新增送水工端实时消息推送(F-ORD-014,R-ORD-10,G44)与实时轨迹上报及后台轨迹查看(F-ADR-012,R-ADR-03,G22 扩展);同步更新核心实体(推送消息/配送轨迹)、异常处理、阶段路线图、页面清单与角色权限矩阵 | - |
| 2026-08-09 | v0.6 | 新增熟客分组价:客户分组管理(F-USR-023)、分组商品价格配置(F-PRO-009)、订单计价与熟客分组价(跨端 F-ORD-023,R-ORD-11,自动应用 + 代客下单手动兜底);登记缺口 G45-G48;同步更新订单/用户/商品三域功能清单与规则、核心实体、流程、路线图、页面清单与术语表 | - |
| 2026-08-09 | v0.7 | 后台权限模型细化:角色权限矩阵由功能域级升级为 32 权限码 × 三角色矩阵(SUPER_ADMIN/ADMIN/DELIVERY_WORKER);新增规则 R-AUTH-04(权限矩阵与强制生效);新增缺口 G49(后台角色/权限管理未生效,P0) | - |
| 2026-08-09 | v0.8 | 新增营销域(10-marketing.md):客户分组管理(F-MKT-001,原 F-USR-023 迁入)与价格策略·分组商品专属价(F-MKT-002,原 F-PRO-009 迁入)集中为营销玩法归属;营销计价规则 R-MKT-01(原 R-ORD-11 迁入);订单域保留 F-ORD-023 应用层;熟客价缺口重排为 G50-G53(权限缺口为 G49);同步更新订单/用户/商品三域功能清单与规则、交叉引用、核心实体、流程、路线图、页面清单与术语表 | - |
| 2026-08-09 | v0.9 | 营销域扩展营销玩法:新增优惠券(F-MKT-003)、满减/折扣(F-MKT-004)、限时特价(F-MKT-005)、会员折扣(F-MKT-006)与营销计价链路 R-MKT-02~07(成交单价解析「手动实收价→分组价→特价→目录价」+ 订单层「满减→会员折扣→优惠券」叠加与封顶);登记缺口 G54-G57;产品页与订单(列表/详情/确认)补「划线价 + 优惠价」展示要求;同步更新订单/商品/用户三域交叉引用、核心实体、术语表、后台菜单与路线图 | - |
| 2026-08-09 | v0.10 | 营销域新增桶水票/水票制(F-MKT-007,R-MKT-08):预购「桶数凭证」、一票抵一桶,用户端购票/下单核销,被核销桶收款方式=水票并计入送水工日报(06-collection.md §R-COL-04);登记缺口 G58;同步更新营销域功能清单与规则、交叉引用、核心实体、路线图与目录导航 | - |
| 2026-08-09 | v0.11 | 营销域扩展买赠/满赠(F-MKT-008,R-MKT-09)与首单立减/新人专享价(F-MKT-009,R-MKT-10):赠品为附加 0 价行随单配送,新人价进单价解析④、首单立减进叠加①(与满减互斥取最大);桶水票补充决策(通兑票按规格限定、支持买 N 送 M);登记缺口 G59-G60;同步更新营销域功能清单与规则、交叉引用、核心实体、路线图与目录导航 | - |
| 2026-08-09 | v0.12 | 新增系统设置域(12-system-settings.md):系统参数管理(F-SYS-001)、订单与履约参数(F-SYS-002)、库存预警阈值(F-SYS-003)、账号与安全策略(F-SYS-004)、通知与订阅消息配置(F-SYS-005)、数据维护(F-SYS-006),规则 R-SYS-01~03;新增运营配置域(13-operations-config.md):首页轮播图(F-OPS-001)、公告管理(F-OPS-002)、意见反馈管理(F-OPS-003)、店铺信息配置(F-OPS-004)、配送费规则配置(F-OPS-005),规则 R-OPS-01~02;权限码目录新增 OPS_CONFIG_READ/WRITE、FEEDBACK_READ/FEEDBACK_REVIEW(32→36 码,后台角色权限矩阵同步);登记缺口 G61-G64;同步更新核心实体、术语表、后台菜单、路线图与目录导航 | - |
| 2026-08-09 | v0.13 | 编号调整:11-misc.md(公共规范与附录)移至 13-misc.md 置于文档末尾,系统设置域顺延为 11-system-settings.md、运营配置域顺延为 12-operations-config.md;同步更新目录导航与全部跨文件引用(历史修订记录保留原文件名) | - |
| 2026-08-15 | v0.14 | 意见反馈闭环完成(2026-08):F-USR-005 由「[部分实现]」校准为「[已实现]」——提交接口接通(client `submitFeedback` 落库,G04 已解决)+ 处理结果回传落地(站内「我的-反馈记录」feedback-records 页展示本人反馈状态与处理结果,后台处理流转后即时可见,`myFeedbacks` 查询仅本人数据);F-OPS-003「处理结果回传」开放问题关闭(订阅消息为备选通道,暂未启用);同步更新 07-user.md §F-USR-005(页面/交互/验收 AC-USR-005-08~09)、12-operations-config.md §F-OPS-003(闭环与页面入口)与 13-misc.md §G04 | - |
| 2026-08-16 | v0.15 | 用户域管理后台展示优化(2026-08):07-user.md §3.3 补充「展示约定」——账号/用户列表与详情不使用 Tab 页切换展示,账号列表以「用户类型」过滤参数(下拉)替代角色 Tab;补充管理员详情页设计(详情弹窗升级为独立详情页 users/accounts/:id 统一账号详情页,按角色渲染:管理账号=基本信息/账号安全/配送能力(兼送水)/操作,送水工=基本信息/统计/配送记录);送水工详情订单区由 Tab 改为「订单状态」筛选参数+单表格;用户详情收货地址/订单记录由 Tab 改为纵向区块;新增验收用例 AC-USR-020-11~13、AC-USR-021-09,更新 AC-USR-020-01/04、AC-USR-021-06、AC-USR-022-02~04 | - |
| 2026-08-16 | v0.16 | 轨迹上报闭环完成(2026-08):F-ADR-012 实时轨迹上报与后台轨迹查看由「规划中」校准为「已实现」——送水工端 App 配送中每 5s 上报(定位 SDK react-native-fitness-geolocation,原生 GPS + SQLite 持久化,弱网缓存补传)、后端 `delivery_trajectories` 轨迹表与 `reportTrajectory`(仅本人 DELIVERING 订单)、后台「配送轨迹」页地图实时位置+轨迹回放(仅 PERMISSION_DELIVERY_WORKER_READ);R-ADR-03 现状与目标更新;G22 关闭(13-misc.md),残余(用户端配送跟踪未复用轨迹数据源、上报参数不可后台配置)登记为 G65;同步更新 05-address-delivery.md §1.1/§1.2/§1.3/§3.4/§4 与 13-misc.md §G22/§G65 | - |
| 2026-08-16 | v0.17 | 用户详情页恢复 Tab 布局(2026-08-16 设计调整):07-user.md §3.3 展示约定更新——账号详情仍纵向排布,用户详情(users/:id)由纵向区块改为 Tab 切换(基本信息/统计/收货地址/订单记录,默认「基本信息」),替代原纵向区块(2026-08 v0.15 将收货地址/订单记录由 Tab 改纵向,现恢复为全页 Tab);同步更新 F-USR-022(现状/页面布局/组件清单/关键交互流程),新增 AC-USR-022-09,更新 AC-USR-022-02~04;前端 users/detail 页实现落地 | - |
| 2026-09 | v0.18 | 用户详情页内容增强(2026-09):F-USR-022 详情页由「基本信息/统计/收货地址/订单记录」调整为**顶部基本信息卡+统计卡 + 7 个 Tabs**(收货地址/订单记录/优惠券/水票/意见反馈/评价记录/收藏商品,默认「收货地址」)——基本信息卡补齐角色/邮箱/所属客户分组/账户余额/总消费金额;统计卡改为全量口径(总订单取 orders.totalElements、总消费取 user.totalSpentCents、已完成订单按状态计数、平均=总消费/总订单),不再按当前分页计算;新增 5 个 Tab 数据源:userCoupons/userTickets(营销域复用 CouponQueryApiService/TicketQueryApiService)、feedbacks(userId 过滤,FeedbackListInput 新增)、reviewsByUser(评价按用户分页,ReviewQueryService 新增)、favoriteProducts(复用 ProductFavoriteService),新查询均以 PERMISSION_USER_READ 强制;UserCouponVO/UserTicketVO 及状态枚举上移 shared-config 双端共用;新增验收用例 AC-USR-022-10~16;同步更新 07-user.md §F-USR-022/§展示约定、10-marketing.md(F-MKT-003/007 后台入口)、12-operations-config.md(F-OPS-003 userId 过滤) | - |
| 2026-09 | v0.19 | 账号详情页改 Tab 布局并补充内容(2026-09):07-user.md §3.3 展示约定与 F-USR-020/F-USR-021 更新——账号详情(users/accounts/:id)由纵向区块改为 **Tab 切换**(管理员视图:基本信息(含权限信息)/账号安全/配送能力(含配送统计)/配送记录/配送轨迹/每日收款/评价记录/操作;送水工视图:基本信息(含当前任务数)/统计/配送记录/配送轨迹/每日收款/评价记录,默认「基本信息」),配送记录 Tab 内保留「订单状态」筛选参数;新增后端查询 `reviewsByDeliveryWorker`(ReviewQueryService.findByDeliveryWorkerIdPaginated 复用 findReviewsWithFilters,PERMISSION_DELIVERY_WORKER_READ)与 `deliveryWorkerRatingStatistics`(DeliveryWorkerStatisticsQueryApiService,delivery_worker_statistics 表 1~5 星分布,无记录全零);前端 AdminAccountDetail/WorkerAccountDetail 改 Tabs,新增 sections/(TrajectorySection/DailyCollectionSection/WorkerReviewSection)复用轨迹/日报查询与地图组件;新增验收用例 AC-USR-020-14~17、AC-USR-021-10~13,更新 AC-USR-020-11、AC-USR-021-05/06/09 | - |
| 2026-09 | v0.20 | 新增通知与消息域(13-notification.md):订阅消息通知(F-NTF-001,承接原 F-ADR-011,编号保留)、用户端消息中心(F-NTF-002)、送水工端实时推送(F-NTF-003,承接原 F-ORD-014,编号保留)、送水工端消息中心(F-NTF-004)、后台推送与消息配置(F-NTF-005,承接原 F-SYS-005,状态校准为 [部分实现]——模板 ID/启用配置已落地,保留时长与 RN 凭证配置为目标形态)、后台消息列表页(F-NTF-006);规则 R-NTF-01~05(订阅授权与降级/消息幂等去重/离线弱网降级/已读保留/配置与密钥保护);权限码目录新增 NOTIFICATION_READ/WRITE(38→40 码,后台角色权限矩阵同步);登记缺口 G66-G69(用户端/送水工端消息中心、后台消息列表页、RN 推送凭证);同步更新 05-address-delivery.md §F-ADR-011、04-order.md §F-ORD-014/R-ORD-10、11-system-settings.md §F-SYS-005、12-operations-config.md(公告与消息中心)、07-user.md(消息中心入口)、01-overview.md(权限码/矩阵)、14-misc.md(核心实体/异常处理/缺口)与目录导航 | - |
| 2026-09 | v0.21 | 序号调整:通知与消息域由 14-notification.md 调整为 13-notification.md(紧随 12-operations-config.md),公共规范与附录由 13-misc.md 顺延为 14-misc.md 置于文档末尾;同步更新目录导航与全部跨文件引用(历史修订记录 v0.13 及以前保留当时文件名) | - |
| 2026-09 | v0.22 | 通知与消息域落地(2026-09):F-NTF-001/002/003/004/006 由 [规划中] 校准为 [已实现]——订阅消息全链路(access-token 服务/发送服务/授权记录 `user_subscription_auths`)、消息网关统一落库(`notifications` 表,幂等键 R-NTF-02)、WebSocket 实时推送(F-NTF-003 通道,`/ws/notifications` + broadcast-events 主题分发)、用户端 message-center / 送水工端 messages / 后台 notifications-messages 三端消息中心、保留期清理(R-NTF-04,notification_retention_days);同步更新 13-notification.md 各条目现状、04-order.md R-ORD-10(WebSocket 直连落地)、01-overview.md §权限码目录(NOTIFICATION_READ 已接线)、14-misc.md 缺口 G08/G44/G66/G67/G68 登记已解决 | - |
| 2026-09 | v0.23 | 送水工端订单页地址信息修正(F-ORD-024):配送员/管理员在订单详情修正收货人/电话/详细地址/楼层/定位(WGS-84 经纬度,「使用当前位置」expo-location + 手动输入),后端 `updateOrderAddress` mutation(PERMISSION_DELIVERY_TASK,终态订单拦截,同步地址簿并留痕 `ADDRESS_UPDATED`),配送费不重算(下单快照 R-MON-02);同步更新 04-order.md(域范围/功能清单/§3.2) | - |
| 2026-09 | v0.24 | 配送时段配置化闭环(2026-09):用户端确认订单页配送时间选项不再写死——后台「订单与履约参数」配置(F-SYS-002 配送时段)经 client 新增 `deliverySlots` 查询(ClientOpsQueryResolver,读 `system_settings` ORDER_AND_FULFILLMENT 组,立即配送恒启用,上午/下午/晚上按启停与时间窗下发,未配置按种子默认兜底)下发,`DeliveryTimeSelector` 改配置驱动(仅渲染启用时段,停用当前已选时段自动回退立即配送),订单详情时段文案同步按配置渲染(请求失败回退兜底文案);F-SYS-002 状态校准为 [部分实现](配送时段链路已闭环,支付超时消费仍规划中),AC-SYS-002-01/03 标注已实现;同步更新 04-order.md(F-ORD-001 现状与配送时间区)、11-system-settings.md(F-SYS-002) | - |
| 2026 | v0.25 | 支付超时优化(R-ORD-04,仅 admin 处理):创建/重置待支付时发布 `PAYMENT_EXPIRY_CHECK` 延迟消息(Artemis `_AMQ_SCHED_DELIVERY`=expiredAt → 独立队列 `payment-expiry-events`)为主触发,到期秒级自动取消;`PaymentExpiryScheduler` 降级为兜底安全网(`@AdminService`,默认 5 分钟,分页游标分批扫描);幂等与互斥由 `payments.version` 乐观锁 CAS 认领(`claimExpired`:PENDING 且到期→EXPIRED,version+1)保证;用户手动取消与支付超时并发防重复恢复库存:订单级乐观锁 CAS 认领(`OrderRepository.cancelAtomically`,取消路径先认领再恢复库存);同步更新 04-order.md(R-ORD-04/F-ORD-005)、14-misc.md(G01)、claude/messaging-cache-wechat.md | - |
| 2026-09 | v0.26 | 送水工端轨迹上报恢复实现(2026-09):react-native-fitness-geolocation 曾因 Android 编译问题随 484b02b 移除、轨迹上报置空,现恢复——重新引入 SDK(原生 GPS + 原生 SQLite 后台持续定位,锁屏/后台不丢点),app.config.js 恢复后台定位/前台服务权限(iOS Always + UIBackgroundModes location;Android ACCESS_BACKGROUND_LOCATION + FOREGROUND_SERVICE + FOREGROUND_SERVICE_LOCATION),reporter 恢复并改进(5s 节流上报;弱网上报失败 AsyncStorage 缓存补传;服务端拒绝[非本人/非配送中]停止该订单并丢弃缓存点;App 重启/重新登录 `resumeActive` 校验订单仍 DELIVERING 后恢复);需 dev build 运行(Expo Go 不支持后台定位),Android 构建已验证;同步更新 05-address-delivery.md(§F-ADR-012 现状/组件清单)与 14-misc.md(§G22) | - |
| 2026-10 | v0.27 | **按行业惯例修订**(产品评审结论,逐项见下):① 订单状态机 **8→7 态**,取消「待确认收货」中间态,**非自提订单送达即完成**并即时计入营收(原「送达 N 天自动确认」及参数 `auto_confirm_receipt_days`/`ReceiptAutoConfirmScheduler` 废弃),重写 R-ORD-11,同步 01/02/04/08/11/14;② **派单目标态由「配送中」改「待配送」**(与抢单口径统一,送水工显式「开始配送」才进入配送中,R-ORD-01/08、02 流程);③ **水桶押金恢复**(R-MON-03 由「一次性水桶费」回退为**可退押金桶 + 空桶回收 + 退桶退押**,G06,同步 03/04/08/14);④ **水票购票未核销可退**(R-MKT-08,原「预收不退/一经购买不支持退款」作废,同步 08/10);⑤ **配送费楼层分档**(R-MON-02/F-OPS-005,避免 7 楼与 27 楼同价,同步 04/12);⑥ **订单地址修正仅改订单快照、不回写用户地址簿**(F-ORD-024);⑦ **明确会员折扣叠加边界**(R-MKT-06/07,按商品维度与分组价/特价/新人价互斥)。本次仅调整规则口径与商业模型,不新增功能编号 | - |
| 2026-10-08 | v0.28 | **按行业惯例差异分析新增功能**(依据《送水小程序行业惯例功能对比与差异分析》Top 建议项,除在线客服 IM 外 7 项补入 PRD):① 一键复购「再来一单」(F-ORD-025/R-ORD-12);② 周期订购(定期配送)(F-ORD-026/R-ORD-13);③ 配送片区与范围校验(F-ADR-003 扩充 + F-ADR-013 配送片区管理 + R-ADR-04;承接 G07);④ 用户端真实配送追踪(F-ADR-010 本期目标接入真实轨迹;收敛 G65);⑤ 水桶账户与退押(F-PRO-010/R-PRO-04);⑥ 储值与套餐(F-MKT-010/R-MKT-11,资金类,幂等+对账+资损监控三件套 + 预付卡合规);⑦ 会员等级与权益(F-MKT-011/R-MKT-12,并入 F-MKT-006 会员折扣,承接 G57)+ 积分体系(F-MKT-012/R-MKT-13);⑧ 补水与续购提醒(F-NTF-007/R-NTF-06)。新增缺口 G75-G82;同步更新 01/02/14(核心实体/异常处理/缺口清单/路线图/页面清单/术语表) | - |
| 2026-10-08 | v0.29 | **按行业惯例差异分析剩余项补充**(依据同一差异分析的 Top 8 之外剩余项):① 企业客户下单(批量下单/月结/开票)(F-ORD-027/R-ORD-14,月结资金类);② 送水工转单(F-ORD-028/R-ORD-15);③ 送水工地图导航(F-ORD-029/R-ORD-16);④ 订单导出(F-ORD-030);⑤ 商品导出与批量导入(F-PRO-011,承接 G20);⑥ 桶账台账与押金对账导出(F-PRO-012/R-PRO-05);⑦ 送水工工资结算(F-COL-012/R-COL-05,资金类);⑧ 配送区域热力图/区域维度(F-ADR-022 扩充,联动 F-ADR-013);⑨ 水票核销点位修订与票号唯一性/防重核销(R-MKT-08 修订);⑩ 公告管理现状校准为「用户端展示已具备、后台维护链路未闭环」(F-OPS-002 状态改 [部分实现])。新增缺口 G83-G92(G90 多门店、G92 在线客服 IM 为**本期不做**);同步更新 01/02/03/05/06/12/14(阶段目标/流程/核心实体/数据一致性/缺口清单/路线图/页面清单/术语表/代码对应) | - |
| 2026-10-08 | v0.30 | **代码核验回填**(对 v0.29 两项 ❓ 待核验项的收敛):① **G91 水票**——票号全局唯一(`user_tickets.ticket_no` 唯一索引 + `ticket_no_sequences` 按月原子取号)与防重复核销(`redeemAtomically` 原子条件更新 + 幂等 + 核销不足回滚)**均已具备**(2026-08),残余仅为**核销点位**(现状下单侧 → 目标送达侧扫码,增强);② **G85 送水工导航**——订单详情**已内嵌目的地地图**(MapLibre + 天地图,`destination-map.tsx`),F-ORD-029 状态 [规划中]→**[部分实现]**,残余为**一键唤起第三方导航未集成**(`Linking` 仅用于拨号)。同步更新 04-order(F-ORD-029 现状/缺口关联/域级验收)、10-marketing(R-MKT-08 现状、§1.3 验收)、14-misc(G85/G91 缺口行与说明) | - |
| 2026-10-09 | v0.31 | **明确本项目不做用户储值(合规决策)**:储值余额属单用途预付卡资金类业务,需预付卡备案/资金存管资质,公司无相应资质,小程序上架与运营存在合规风险——余额充值/余额支付/余额抵扣/余额+微信组合支付均不实现,预收资金需求由水票(F-MKT-007)与套餐卡(预购货物凭证)承接;F-MKT-010「储值与套餐」收敛为「套餐卡(次卡/月卡/季卡)」,R-MKT-11 重写为套餐卡规则(核销/退款对齐 R-MKT-08),G79 同步收敛;移除项:储值账户/储值流水实体、储值↔渠道对账一致性、储值类异常场景、储值渗透率指标、recharge(用户端充值页)/recharge-plans(后台储值配置)页面、F-NTF-007 ④储值余额不足提醒;前端同步移除确认页「余额支付(即将上线)」占位与后台用户详情「账户余额」展示(`users.balance_cents` 为遗留恒 0 字段,不暴露入口);同步更新 01/02/04/07/10/13/14 与 design/_spec、原型页 | - |

### 阅读约定

- 每个功能条目带实现状态标记:`[已实现]`(功能完整可用)、`[部分实现]`(界面或部分链路已通,数据为模拟或链路未闭合)、`[规划中]`(当前无实现,按目标形态设计,UI 标注「目标形态」)。
- 功能条目编号 `F-域-序号`,业务规则编号 `R-域-序号`,验收用例编号 `AC-域-F序号-序号`,缺口编号 `G-序号`;可在开发排期、测试用例中引用。
- 页面标题(浏览器标签页)由前端统一管理(admin-client 用 react-helmet-async 按路由设置),属实现细节,PRD 不在页面布局中逐页描述「标题区」。
- 状态标记反映当前 `refactor/unify-module-structure` 分支工作区状态,重构完成后需复查校准。
- 规划中/部分实现功能的缺口登记见 [14-misc.md](14-misc.md) §缺口清单,不虚构需求。
- `_ref/` 为内部工作目录(代码现状基线 facts.md、编写规范 conventions.md、域切片),**不属于正式 PRD 内容**,不进入目录导航;重构完成复查标记时可参考 `_ref/facts.md`。

---

## 目录导航

> PRD 已按功能域拆分为多个文件,本章节为导航索引。跨文件引用采用「文件 §小节标题」文本形式(禁用锚点链接)。

### 上下文与流程

| 文档 | 内容 | 对应原章节 |
| --- | --- | --- |
| [01-overview.md](01-overview.md) | 产品概述、目标与成功指标、用户角色与权限矩阵、全局约束 R-MON-01 | 第 1-3 章 |
| [02-business-flows.md](02-business-flows.md) | 核心业务流程、订单状态机(全局唯一事实来源)、派单/履约/对账/支付退款/代客下单流程、现状与目标差异 | 第 4 章 |

### 功能域(按域拆分)

| 文档 | 域范围 | 功能编号 | 业务规则 |
| --- | --- | --- | --- |
| [03-product.md](03-product.md) | 商品域 | F-PRO-001~012 | R-PRO-01~05、R-MON-03 |
| [04-order.md](04-order.md) | 订单域 | F-ORD-001~014、020~030 | R-ORD-01~16、R-PAY-01~03、R-MON-02、R-EVL-01 |
| [05-address-delivery.md](05-address-delivery.md) | 地址与配送域 | F-ADR-001~003、010~013、020~022 | R-ADR-01~04 |
| [06-collection.md](06-collection.md) | 收银与对账域 | F-COL-001~003、010~012 | R-COL-01~05 |
| [07-user.md](07-user.md) | 用户与账户域 | F-USR-001~006、010~011、020~023 | R-AUTH-01~03 |
| [08-statistics.md](08-statistics.md) | 统计与数据域 | F-STA-001~007 | — |
| [09-platform-logs.md](09-platform-logs.md) | 平台与日志域 | F-LOG-001~003 | — |
| [10-marketing.md](10-marketing.md) | 营销域 | F-MKT-001~012(预留 013+) | R-MKT-01~13 |
| [11-system-settings.md](11-system-settings.md) | 系统设置域 | F-SYS-001~006(预留 007+) | R-SYS-01~03 |
| [12-operations-config.md](12-operations-config.md) | 运营配置域 | F-OPS-001~005(预留 006+) | R-OPS-01~02 |
| [13-notification.md](13-notification.md) | 通知与消息域 | F-NTF-001~007(预留 008+) | R-NTF-01~06 |

### 公共规范与附录

| 文档 | 内容 | 对应原章节 |
| --- | --- | --- |
| [14-misc.md](14-misc.md) | 数据需求要点、异常处理与边界场景、现状缺口登记(G01-G92)与阶段路线图、非功能需求、通用/端到端验收(AC-E2E-01~03)、附录(页面清单/术语表/代码对应) | 第 7-11 章、附录 A/C/D |

---

## 编号规则与文档约定

### 编号体系

| 前缀 | 含义 | 示例 |
| --- | --- | --- |
| F-域-序号 | 功能条目 | F-ORD-001 |
| R-域-序号 | 业务规则(唯一归属一个文件,他处只引用) | R-ORD-02 |
| AC-域-F序号-序号 | 功能级验收用例(定义于各域文件) | AC-ORD-001-01 |
| AC-E2E-序号 | 跨域端到端用例 | AC-E2E-01 |
| G-序号 | 缺口登记(唯一归属 14-misc.md) | G01 |

### 域缩写

- 功能域:PRO 商品 / ORD 订单 / ADR 地址配送 / COL 收银 / USR 用户 / STA 统计 / LOG 日志 / MKT 营销 / OPS 运营配置 / SYS 系统设置 / NTF 通知消息;
- 规则域:PRO/ORD/ADR/COL/EVL/MON/AUTH/PAY/MKT/OPS/SYS/NTF。

### 约定

- 状态标记仅三种:`[已实现]` / `[部分实现]` / `[规划中]`;
- 新增功能须登记编号与状态,不得跳过编号直接编写;新增缺口按序追加至 14-misc.md(当前已至 G92,新增从 G93 起),并在对应域文件补记关联;
- 开发在提交信息、测试用例中引用功能编号,形成"PRD → 开发 → 测试"追踪链;
- PRD 管业务规则与验收,技术文档管实现细节,两者通过 14-misc.md §附录 C(代码对应)的路径互引,不互相复制。
