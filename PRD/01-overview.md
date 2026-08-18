# 产品概述、目标与角色

> 全局概述、目标指标、角色权限与全局约束在此集中定义,各域文件通过 `01-overview.md §小节` 文本引用。

## 产品概述

### 背景

桶装水订购与配送是高频、刚需的本地生活服务。传统经营模式依赖电话、微信群人工接单,存在以下痛点:

- **用户侧**:下单流程繁琐、无法随时查看订单进度、支付不便;
- **配送侧**:送水工接单靠人工分配、配送过程无记录、收款与对账依赖纸质记账;
- **经营者侧**:订单、库存、营收、送水工业绩缺乏可视化管理,日报对账耗时且容易出错。

AquaRush 以微信小程序为载体,为小型桶装水门店提供"订购 → 配送 → 收款 → 对账"全链路数字化方案,面向单服务器、轻量化运营场景。

### 产品定位

微信生态内桶装水**订购 + 配送履约 + 运营管理**一体化系统,由三个端协同构成:

| 端             | 形态            | 定位     | 核心诉求                       |
| -------------- | --------------- | -------- | ------------------------------ |
| 用户端小程序   | Taro 微信小程序 | 订购入口 | 像点外卖一样方便地买水         |
| 送水工端小程序 | Taro 微信小程序 | 履约工具 | 接单、配送、收款一气呵成       |
| 管理后台       | Web(React)      | 运营中枢 | 订单、库存、对账、营收可视可控 |

### 业务闭环

业务主链路(跨三端):

```text
用户端:浏览商品 → 下单支付 → 等待配送 → 查看跟踪 → 确认收货 → 评价
送水工端:接单/被派单 → 配送 → 拍照验证 → 收款 → 提交每日收款日报
管理后台:维护商品/地址 → 派单 → 核验配送照片 → 确认日报 → 对账审核 → 查看统计
```

### 当前阶段说明

系统主体框架与主要业务链路已搭建完成:商品展示、地址管理、下单、送水工接单配送收款、日报对账、后台管理均已具备可用雏形。**支付链路、用户端订单数据真实性、评论、押金、订阅消息通知**等环节存在缺口(详见 14-misc.md),是后续迭代重点。

## 产品目标与成功指标

### 产品目标

1. **打通全链路闭环**:实现"用户下单 → 在线支付 → 派单 → 配送 → 拍照验证 → 收款 → 日报对账"端到端可运营;
2. **订单数据真实化**:用户端订单列表、详情、配送跟踪全部对接真实业务数据,替代当前模拟数据;
3. **收款对账零差错**:通过系统预计算与人工填报的比对、差异申诉、管理员审核,将日报对账差异率降到最低;
4. **体验对标外卖平台**:下单流程 ≤ 3 步,订单进度全程可见,支付安全可靠。

### 分端目标

| 端       | 目标                                              |
| -------- | ------------------------------------------------- |
| 用户端   | 下单付得了款、订单看得见、进度追得到、送达有通知  |
| 送水工端 | 接单配送收款一气呵成,日报提交不费心,业绩一目了然  |
| 管理后台 | 商品/订单/派单/对账/统计全维可控,经营状况实时可见 |

### 成功指标(建议)

| 指标         | 说明                             | 目标值(建议)              |
| ------------ | -------------------------------- | ------------------------- |
| 日完成订单数 | 当日进入「已完成」状态的订单数   | 上线 3 个月后日均 ≥ 50 单 |
| 支付成功率   | 支付成功订单 / 发起的支付订单    | ≥ 95%                     |
| 对账差异率   | 存在差异的日报数 / 当日日报总数  | ≤ 5%                      |
| 订单履约时长 | 派单至「已完成」的平均耗时       | ≤ 4 小时                  |
| 库存准确率   | 盘点库存与系统库存一致率         | ≥ 99%                     |
| 用户留存     | 月内下单 ≥ 2 次的用户 / 活跃用户 | ≥ 30%                     |

### 阶段目标

| 阶段     | 主题                  | 关键交付                                                     |
| -------- | --------------------- | ------------------------------------------------------------ |
| 一期(P0) | 支付闭环 + 订单真实化 | 微信支付全链路、用户端订单列表/详情/跟踪真实数据、状态机对齐 |
| 二期(P1) | 体验完善              | 多商品购物车下单、订单评价、订阅消息通知、意见反馈提交       |
| 三期(P2) | 增值业务              | 水桶费购买(一次性,2026-08 已实现)、配送范围校验启用、营销能力                 |

## 用户角色与权限

### 角色定义

| 角色       | 代码枚举                         | 登录方式              | 使用端         | 核心诉求                                         |
| ---------- | -------------------------------- | --------------------- | -------------- | ------------------------------------------------ |
| 普通用户   | —                                | 微信静默登录          | 用户端小程序   | 浏览商品、下单、支付、查看订单、管理地址         |
| 送水工     | `AdminRoleModel.DELIVERY_WORKER` | 后台创建的登录账号(用户名/密码,表数据由管理员创建,不关联微信 openId) | 送水工端独立 App | 接单、配送、收款、提交日报、查看业绩             |
| 管理员     | `AdminRoleModel.ADMIN`           | 后台账号(用户名/密码) | 管理后台 + 送水工端(兼送水) | 业务域管理:商品/订单/用户/送水员/对账,统计与日志;兼送水(F-USR-023) |
| 超级管理员 | `AdminRoleModel.SUPER_ADMIN`     | 后台账号(用户名/密码) | 管理后台       | 全部权限,含管理员账号管理与系统配置              |

### 权限码目录(40 码)

后台权限模型为**代码级静态矩阵 + 方法级强制生效**:单一来源 `AdminPermission`(规划 36 码,营销域新增 2 码、通知域新增 2 码后共 40 码)与 `RolePermissionMapping`(角色→权限矩阵),不建数据库 RBAC 表。权限码按域(模块)+操作拆分,查询(READ)/管理(WRITE)分离,关键操作(派单、核验、确认、审核、执行、导出、退款、删除、重置密码、反馈处理)独立成码。

| 权限码                         | 说明                         | 权限码                 | 说明                        |
| ------------------------------ | ---------------------------- | ---------------------- | --------------------------- |
| PRODUCT_READ                   | 商品查询                     | USER_READ              | 用户查询                    |
| PRODUCT_WRITE                  | 商品管理(增改删·上下架)      | USER_WRITE             | 用户管理(增改)              |
| ORDER_READ                     | 订单查询                     | USER_DELETE            | 删除用户                    |
| ORDER_WRITE                    | 订单管理(改状态·取消)        | USER_RESET_PASSWORD    | 重置用户密码                |
| ORDER_CREATE                   | 代客下单                     | ADDRESS_READ           | 地址查询                    |
| ORDER_ASSIGN                   | 派单/批量派单                | ADDRESS_WRITE          | 地址维护(增删·批量导入)     |
| ORDER_REVIEW                   | 配送照片核验                 | ADMIN_USER_READ        | 管理员查询                  |
| DELIVERY_TASK                  | 配送任务执行(接单·开始·完成) | ADMIN_USER_WRITE       | 管理员管理(增改删·重置密码) |
| DELIVERY_WORKER_READ           | 送水工查询                   | RECONCILIATION_READ    | 对账/日报查询               |
| DELIVERY_WORKER_WRITE          | 送水工管理(增改·停用)        | RECONCILIATION_SUBMIT  | 日报提交                    |
| DELIVERY_WORKER_DELETE         | 删除送水工                   | RECONCILIATION_CONFIRM | 日报确认                    |
| DELIVERY_WORKER_RESET_PASSWORD | 重置送水工密码               | RECONCILIATION_REVIEW  | 对账审核                    |
| PAYMENT_READ                   | 支付查询                     | RECONCILIATION_EXECUTE | 执行对账                    |
| PAYMENT_REFUND                 | 退款                         | STATISTICS_READ        | 统计查询                    |
| LOG_READ                       | 日志查询                     | STATISTICS_EXPORT      | 统计导出                    |
| SYSTEM_CONFIG_READ             | 系统配置查询                 | SYSTEM_CONFIG_WRITE    | 系统配置修改                |
| OPS_CONFIG_READ                | 运营配置查询                 | OPS_CONFIG_WRITE       | 运营配置修改                |
| FEEDBACK_READ                  | 意见反馈查询                 | FEEDBACK_REVIEW        | 意见反馈处理(回复·标记)     |
| MKT_READ                       | 营销查询                     | MKT_WRITE              | 营销配置修改                |
| NOTIFICATION_READ              | 通知与消息查询(消息列表)     | NOTIFICATION_WRITE     | 推送与消息配置(模板/参数)   |

### 角色权限矩阵(后台三角色 × 权限码)

| 权限码                           | 超级管理员 | 管理员 | 送水工 | 权限码                         | 超级管理员 | 管理员 | 送水工 |
| -------------------------------- | ---------- | ------ | ------ | ------------------------------ | ---------- | ------ | ------ |
| PRODUCT_READ                     | ✅          | ✅      | ✅      | DELIVERY_WORKER_DELETE         | ✅          | ✅      | —      |
| PRODUCT_WRITE                    | ✅          | ✅      | —      | DELIVERY_WORKER_RESET_PASSWORD | ✅          | ✅      | —      |
| ORDER_READ                       | ✅          | ✅      | ✅      | USER_READ                      | ✅          | ✅      | —      |
| ORDER_WRITE                      | ✅          | ✅      | —      | USER_WRITE                     | ✅          | ✅      | —      |
| ORDER_CREATE(代客下单)           | ✅          | —      | ✅      | USER_DELETE                    | ✅          | ✅      | —      |
| ORDER_ASSIGN(派单)               | ✅          | ✅      | —      | USER_RESET_PASSWORD            | ✅          | ✅      | —      |
| ORDER_REVIEW(照片核验)           | ✅          | ✅      | —      | ADDRESS_READ                   | ✅          | ✅      | ✅      |
| DELIVERY_TASK(接单/开始/完成)    | ✅          | ✅      | ✅      | ADDRESS_WRITE                  | ✅          | ✅      | —      |
| DELIVERY_WORKER_READ             | ✅          | ✅      | —      | ADMIN_USER_READ                | ✅          | —      | —      |
| DELIVERY_WORKER_WRITE            | ✅          | ✅      | —      | ADMIN_USER_WRITE               | ✅          | —      | —      |
| RECONCILIATION_READ              | ✅          | ✅      | ✅      | SYSTEM_CONFIG_READ             | ✅          | —      | —      |
| RECONCILIATION_SUBMIT(日报提交)  | ✅          | —      | ✅      | SYSTEM_CONFIG_WRITE            | ✅          | —      | —      |
| OPS_CONFIG_READ                  | ✅          | ✅      | —      | FEEDBACK_READ                  | ✅          | ✅      | —      |
| OPS_CONFIG_WRITE                 | ✅          | ✅      | —      | FEEDBACK_REVIEW(反馈处理)      | ✅          | ✅      | —      |
| RECONCILIATION_CONFIRM(日报确认) | ✅          | ✅      | —      | PAYMENT_READ                   | ✅          | ✅      | —      |
| RECONCILIATION_REVIEW(对账审核)  | ✅          | ✅      | —      | PAYMENT_REFUND(退款)           | ✅          | ✅      | —      |
| RECONCILIATION_EXECUTE(执行对账) | ✅          | ✅      | —      | STATISTICS_READ                | ✅          | ✅      | ✅      |
| LOG_READ                         | ✅          | ✅      | —      | STATISTICS_EXPORT              | ✅          | ✅      | —      |
| MKT_READ                         | ✅          | ✅      | —      | MKT_WRITE                      | ✅          | ✅      | —      |
| NOTIFICATION_READ                | ✅          | ✅      | —      | NOTIFICATION_WRITE             | ✅          | —      | —      |

> 权限模型说明:
> - 超级管理员全 40 码;管理员 33 码(缺 ADMIN_USER_*/SYSTEM_CONFIG_*/ORDER_CREATE/RECONCILIATION_SUBMIT/NOTIFICATION_WRITE,共 7 码);送水工 8 码(ORDER_READ/PRODUCT_READ/ADDRESS_READ/ORDER_CREATE/DELIVERY_TASK/RECONCILIATION_READ/RECONCILIATION_SUBMIT/STATISTICS_READ)。
> - **普通用户(USER)独立于本矩阵**:走 `isAuthenticated()` + 本人数据隔离(R-AUTH-02),不授予后台权限码;client 侧以 `ROLE_USER` 标记身份。
> - 超级管理员额外携带 `ROLE_ADMIN`(兼容 `hasRole('ADMIN')` 表达式);`users.role=WORKER` 对应 `ROLE_DELIVERY_WORKER`。
> - `SYSTEM_CONFIG_*` 当前无对应接口(系统设置域为规划中,见 11-system-settings.md,G64);`OPS_CONFIG_*`/`FEEDBACK_*` 为 v0.12 新增规划码,**已随运营配置域 F-OPS-001/F-OPS-003 接线(2026-08)**:banners/feedbacks 查询与变更分别以 `PERMISSION_OPS_CONFIG_READ/WRITE`、`PERMISSION_FEEDBACK_READ/REVIEW` 方法级强制(见 12-operations-config.md);`PAYMENT_*` 同理(G01 支付 stub)。均保留在矩阵待接线。
> - `MKT_READ`/`MKT_WRITE` 为营销域新增码,**已随营销域 F-MKT-001/002/003/004/007 接线(2026-08)**:客户分组/分组价/优惠券/满减折扣/桶水票的查询与配置分别以 `PERMISSION_MKT_READ/WRITE` 方法级强制(见 10-marketing.md)。
> - `NOTIFICATION_READ`/`NOTIFICATION_WRITE` 为通知与消息域(v0.20)新增规划码,见 13-notification.md:消息列表查询(F-NTF-006)已以 `PERMISSION_NOTIFICATION_READ` 接线(管理员/超管,2026 通知域落地);推送与消息配置(F-NTF-005)现状复用 `SYSTEM_CONFIG_WRITE`(system-config/notifications 页已接线 2026-08),`NOTIFICATION_WRITE` 为规划码待接线。
> - `STATISTICS_READ` **已随统计域接线(2026-08)**:仪表盘 `dailyStatistics`/`pendingOrderBreakdown` 与用户/送水员/商品统计查询均以 `PERMISSION_STATISTICS_READ` 方法级强制(见 08-statistics.md);`STATISTICS_EXPORT` 为预留码,暂无统计导出功能。
> - `DELIVERY_TASK` **已授予管理员(2026-08)**:支持「管理员兼送水」——管理员账号可登录送水工端执行接单/开始配送/完成配送,业绩按账号关联的送水员档案归集(见 07-user.md §F-USR-023);`ORDER_CREATE`/`RECONCILIATION_SUBMIT` 对管理员仍未授予,兼送水是否覆盖代客下单/日报提交为开放问题。
> - 强制链路与方法级校验见 **R-AUTH-04**(定义于 07-user.md)。后台「照片核验」当前仅为照片展示(F-ORD-021,部分实现),核验/通过/驳回流程缺失(G23)。

### 角色层级关系

- **超级管理员 > 管理员**:超管可管理管理员账号(ADMIN_USER_*)与系统配置(SYSTEM_CONFIG_*),普通管理员不可;
- **管理员 > 送水员**:管理员创建/停用送水员账号,查看送水员订单与业绩,向送水员派单;
- **管理员 > 普通用户**:管理员可查看用户信息与订单,处理售后问题;
- 送水员与普通用户之间**无直接管理关系**,送水员仅在配送履约(接单后)看到用户配送信息。

## 全局约束

> 跨域全局规则在此定义,域文件只引用不复制。

- **R-MON-01 金额精度**:所有金额以"分"为单位存储与计算,展示时换算为元;服务端校验订单金额 = 商品单价 × 数量(多商品为 Σ 明细),防止前端篡改。
