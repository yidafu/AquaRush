// 转换自 changes/v1.7.0-seed-nanshan-demo-data.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog
import java.math.BigDecimal

class V170SeedNanshanDemoData :
  KotlinCompiledDatabaseChangeLog({
    // ============================================================================
    //     南山牌桶装水演示数据(v1.7.0)
    //     ============================================================================
    //     以「南山牌桶装水」为核心重建全套演示数据,替换旧 v1.0.2 通用饮料数据:
    //       - 12 款南山牌商品(含 1 缺货 / 1 下架)
    //       - 3 名送水员(admins 单表 role=DELIVERY_WORKER,送水员档案已于 v1.2.1 并入)
    //       - 12 名杭州市西湖区用户 + 17 条收货地址(默认西湖区 330106)
    //       - 51 条最近 30 天历史订单(多商品/营销快照/各状态) + 明细/赠品/评价/轨迹
    //       - 营销配置:客户分组/分组价/优惠券/满减活动/水票/秒杀/买赠/首单活动
    //       - 运营配置:店铺信息/配送费规则/轮播图/公告/意见反馈/每日收款/每日对账
    //     ============================================================================
    //     时间一律使用相对表达式(CURRENT_TIMESTAMP - INTERVAL 'N days'),
    //     保证「最近一个月」的统计窗口在任意时刻重建数据库后都成立。
    //     本文件在 db.changelog-master.xml 末尾(全部 schema 变更之后)执行,
    //     可引用最终 schema 的列与表;必须在重建数据库后运行(旧测试数据会冲突)。
    //     ============================================================================
    changeSet(author = "aqua-rush", id = "1.7.0-seed-nanshan-basic") {
      comment("Seed 南山牌 demo base data: customer groups, users, admins (3 delivery workers), addresses, products, favorites")
      // ==================== 客户分组(F-MKT-001) ====================
      insert(tableName = "customer_groups") {
        column(name = "id", valueNumeric = 7000000000000000001)
        column(name = "name", value = "企业客户")
        column(name = "description", value = "写字楼/餐饮等高频批量客户,享专属价格")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
        column(name = "is_deleted", valueBoolean = false)
      }
      insert(tableName = "customer_groups") {
        column(name = "id", valueNumeric = 7000000000000000002)
        column(name = "name", value = "社区会员")
        column(name = "description", value = "小区家庭长期订购用户")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '180 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '180 days')")
        column(name = "is_deleted", valueBoolean = false)
      }
      insert(tableName = "customer_groups") {
        column(name = "id", valueNumeric = 7000000000000000003)
        column(name = "name", value = "普通客户")
        column(name = "description", value = "默认分组,未归属其他分组的新用户")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '160 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '160 days')")
        column(name = "is_deleted", valueBoolean = false)
      }
      // ==================== 用户(12 名,杭州市西湖区) ====================
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000001)
        column(name = "wechat_openid", value = "openid_nanshan_001")
        column(name = "wechat_unionid", value = "unionid_nanshan_001")
        column(name = "nickname", value = "王建国")
        column(name = "phone", value = "13800138001")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_001.png")
        column(name = "email", value = "wangjianguo@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 2500)
        column(name = "total_spent_cents", valueNumeric = 15200)
        column(name = "customer_group_id", valueNumeric = 7000000000000000002)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '420 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000002)
        column(name = "wechat_openid", value = "openid_nanshan_002")
        column(name = "wechat_unionid", value = "unionid_nanshan_002")
        column(name = "nickname", value = "李秀英")
        column(name = "phone", value = "13800138002")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_002.png")
        column(name = "email", value = "lixiuying@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 800)
        column(name = "total_spent_cents", valueNumeric = 9800)
        column(name = "customer_group_id", valueNumeric = 7000000000000000002)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '350 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000003)
        column(name = "wechat_openid", value = "openid_nanshan_003")
        column(name = "wechat_unionid", value = "unionid_nanshan_003")
        column(name = "nickname", value = "张伟")
        column(name = "phone", value = "13800138003")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_003.png")
        column(name = "email", value = "zhangwei@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 0)
        column(name = "total_spent_cents", valueNumeric = 7600)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000004)
        column(name = "wechat_openid", value = "openid_nanshan_004")
        column(name = "wechat_unionid", value = "unionid_nanshan_004")
        column(name = "nickname", value = "陈晓东")
        column(name = "phone", value = "13800138004")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_004.png")
        column(name = "email", value = "chenxiaodong@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 5000)
        column(name = "total_spent_cents", valueNumeric = 38600)
        column(name = "customer_group_id", valueNumeric = 7000000000000000001)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '600 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000005)
        column(name = "wechat_openid", value = "openid_nanshan_005")
        column(name = "wechat_unionid", value = "unionid_nanshan_005")
        column(name = "nickname", value = "林芳")
        column(name = "phone", value = "13800138005")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_005.png")
        column(name = "email", value = "linfang@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 1500)
        column(name = "total_spent_cents", valueNumeric = 6300)
        column(name = "customer_group_id", valueNumeric = 7000000000000000002)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000006)
        column(name = "wechat_openid", value = "openid_nanshan_006")
        column(name = "wechat_unionid", value = "unionid_nanshan_006")
        column(name = "nickname", value = "赵强")
        column(name = "phone", value = "13800138006")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_006.png")
        column(name = "email", value = "zhaoqiang@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 12000)
        column(name = "total_spent_cents", valueNumeric = 52400)
        column(name = "customer_group_id", valueNumeric = 7000000000000000001)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '720 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000007)
        column(name = "wechat_openid", value = "openid_nanshan_007")
        column(name = "wechat_unionid", value = "unionid_nanshan_007")
        column(name = "nickname", value = "孙志远")
        column(name = "phone", value = "13800138007")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_007.png")
        column(name = "email", value = "sunzhiyuan@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 300)
        column(name = "total_spent_cents", valueNumeric = 4100)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000008)
        column(name = "wechat_openid", value = "openid_nanshan_008")
        column(name = "wechat_unionid", value = "unionid_nanshan_008")
        column(name = "nickname", value = "周丽娟")
        column(name = "phone", value = "13800138008")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_008.png")
        column(name = "email", value = "zhoulijuan@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 600)
        column(name = "total_spent_cents", valueNumeric = 5200)
        column(name = "customer_group_id", valueNumeric = 7000000000000000002)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '260 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000009)
        column(name = "wechat_openid", value = "openid_nanshan_009")
        column(name = "wechat_unionid", value = "unionid_nanshan_009")
        column(name = "nickname", value = "吴刚")
        column(name = "phone", value = "13800138009")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_009.png")
        column(name = "email", value = "wugang@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 8000)
        column(name = "total_spent_cents", valueNumeric = 29800)
        column(name = "customer_group_id", valueNumeric = 7000000000000000001)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '500 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000010)
        column(name = "wechat_openid", value = "openid_nanshan_010")
        column(name = "wechat_unionid", value = "unionid_nanshan_010")
        column(name = "nickname", value = "郑秀兰")
        column(name = "phone", value = "13800138010")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_010.png")
        column(name = "email", value = "zhengxiulan@example.com")
        column(name = "status", value = "SUSPENDED")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 0)
        column(name = "total_spent_cents", valueNumeric = 3600)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '180 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000011)
        column(name = "wechat_openid", value = "openid_nanshan_011")
        column(name = "wechat_unionid", value = "unionid_nanshan_011")
        column(name = "nickname", value = "何俊杰")
        column(name = "phone", value = "13800138011")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_011.png")
        column(name = "email", value = "hejunjie@example.com")
        column(name = "status", value = "INACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 0)
        column(name = "total_spent_cents", valueNumeric = 0)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      insert(tableName = "users") {
        column(name = "id", valueNumeric = 1000000000000000012)
        column(name = "wechat_openid", value = "openid_nanshan_012")
        column(name = "wechat_unionid", value = "unionid_nanshan_012")
        column(name = "nickname", value = "高敏")
        column(name = "phone", value = "13800138012")
        column(name = "avatar_url", value = "https://example.com/avatars/nanshan_012.png")
        column(name = "email", value = "gaomin@example.com")
        column(name = "status", value = "ACTIVE")
        column(name = "role", value = "USER")
        column(name = "balance_cents", valueNumeric = 200)
        column(name = "total_spent_cents", valueNumeric = 2800)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '120 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 后台账号(superadmin/admin 自增 1/2,送水员显式 id) ====================
      insert(tableName = "admins") {
        column(name = "username", value = "superadmin")
        column(
          name = "password_hash",
          value = "${'$'}2a${'$'}10${'$'}7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        )
        column(name = "real_name", value = "超级管理员")
        column(name = "phone", value = "13800138000")
        column(name = "role", value = "SUPER_ADMIN")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '900 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "admins") {
        column(name = "username", value = "admin")
        column(
          name = "password_hash",
          value = "${'$'}2a${'$'}10${'$'}7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        )
        column(name = "real_name", value = "系统管理员")
        column(name = "phone", value = "13800138999")
        column(name = "role", value = "ADMIN")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '800 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 送水员 1:陈永强(在线,评分最高)
      insert(tableName = "admins") {
        column(name = "id", valueNumeric = 9000000000000000011)
        column(name = "username", value = "delivery_worker_001")
        column(
          name = "password_hash",
          value = "${'$'}2a${'$'}10${'$'}7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        )
        column(name = "real_name", value = "陈永强")
        column(name = "phone", value = "13800138101")
        column(name = "role", value = "DELIVERY_WORKER")
        column(name = "avatar_url", value = "https://example.com/avatars/delivery_worker_001.png")
        column(name = "online_status", value = "ONLINE")
        column(name = "rating", valueNumeric = BigDecimal("4.9"))
        column(name = "total_orders", valueNumeric = 342)
        column(name = "completed_orders", valueNumeric = 331)
        column(name = "average_rating", valueNumeric = BigDecimal("4.9"))
        column(name = "earning_cents", valueNumeric = 268500)
        column(name = "is_available", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '730 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 送水员 2:刘志刚(在线)
      insert(tableName = "admins") {
        column(name = "id", valueNumeric = 9000000000000000012)
        column(name = "username", value = "delivery_worker_002")
        column(
          name = "password_hash",
          value = "${'$'}2a${'$'}10${'$'}7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        )
        column(name = "real_name", value = "刘志刚")
        column(name = "phone", value = "13800138102")
        column(name = "role", value = "DELIVERY_WORKER")
        column(name = "avatar_url", value = "https://example.com/avatars/delivery_worker_002.png")
        column(name = "online_status", value = "ONLINE")
        column(name = "rating", valueNumeric = BigDecimal("4.8"))
        column(name = "total_orders", valueNumeric = 288)
        column(name = "completed_orders", valueNumeric = 276)
        column(name = "average_rating", valueNumeric = BigDecimal("4.8"))
        column(name = "earning_cents", valueNumeric = 214000)
        column(name = "is_available", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '650 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 送水员 3:黄海涛(离线,演示下线状态)
      insert(tableName = "admins") {
        column(name = "id", valueNumeric = 9000000000000000013)
        column(name = "username", value = "delivery_worker_003")
        column(
          name = "password_hash",
          value = "${'$'}2a${'$'}10${'$'}7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        )
        column(name = "real_name", value = "黄海涛")
        column(name = "phone", value = "13800138103")
        column(name = "role", value = "DELIVERY_WORKER")
        column(name = "avatar_url", value = "https://example.com/avatars/delivery_worker_003.png")
        column(name = "online_status", value = "OFFLINE")
        column(name = "rating", valueNumeric = BigDecimal("4.6"))
        column(name = "total_orders", valueNumeric = 196)
        column(name = "completed_orders", valueNumeric = 183)
        column(name = "average_rating", valueNumeric = BigDecimal("4.6"))
        column(name = "earning_cents", valueNumeric = 152000)
        column(name = "is_available", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '500 days')")
        column(name = "last_login_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      // ==================== 送水员统计(账号 id 即 delivery_worker_id) ====================
      insert(tableName = "delivery_worker_statistics") {
        column(name = "id", valueNumeric = 8500000000000000001)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "average_rating", valueNumeric = BigDecimal("4.90"))
        column(name = "total_reviews", valueNumeric = 42)
        column(name = "one_star_reviews", valueNumeric = 1)
        column(name = "two_star_reviews", valueNumeric = 1)
        column(name = "three_star_reviews", valueNumeric = 2)
        column(name = "four_star_reviews", valueNumeric = 8)
        column(name = "five_star_reviews", valueNumeric = 30)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '730 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_updated", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "delivery_worker_statistics") {
        column(name = "id", valueNumeric = 8500000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "average_rating", valueNumeric = BigDecimal("4.83"))
        column(name = "total_reviews", valueNumeric = 36)
        column(name = "one_star_reviews", valueNumeric = 0)
        column(name = "two_star_reviews", valueNumeric = 1)
        column(name = "three_star_reviews", valueNumeric = 2)
        column(name = "four_star_reviews", valueNumeric = 8)
        column(name = "five_star_reviews", valueNumeric = 25)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '650 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "last_updated", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "delivery_worker_statistics") {
        column(name = "id", valueNumeric = 8500000000000000003)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "average_rating", valueNumeric = BigDecimal("4.60"))
        column(name = "total_reviews", valueNumeric = 21)
        column(name = "one_star_reviews", valueNumeric = 0)
        column(name = "two_star_reviews", valueNumeric = 1)
        column(name = "three_star_reviews", valueNumeric = 1)
        column(name = "four_star_reviews", valueNumeric = 6)
        column(name = "five_star_reviews", valueNumeric = 13)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '500 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "last_updated", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      // ==================== 收货地址(默认杭州市西湖区 330106,WGS-84 坐标,floor 用于配送费) ====================
      // 王建国(2 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000001)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "receiver_name", value = "王建国")
        column(name = "phone", value = "13800138001")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "古墩路808号 三墩颐景园3幢2单元601室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1089"))
        column(name = "latitude", valueNumeric = BigDecimal("30.3096"))
        column(name = "floor", valueNumeric = 6)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
      }
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000002)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "receiver_name", value = "王建国")
        column(name = "phone", value = "13800138001")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文一西路588号 西溪诚园2期5幢1201室")
        column(name = "longitude", valueNumeric = BigDecimal("120.0854"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2841"))
        column(name = "floor", valueNumeric = 12)
        column(name = "is_default", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
      }
      // 李秀英(2 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000003)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "receiver_name", value = "李秀英")
        column(name = "phone", value = "13800138002")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "教工路223号 文教社区4幢1单元302室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1426"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2839"))
        column(name = "floor", valueNumeric = 3)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '330 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '330 days')")
      }
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000004)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "receiver_name", value = "李秀英")
        column(name = "phone", value = "13800138002")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文二路328号 富丽科技大厦5楼")
        column(name = "longitude", valueNumeric = BigDecimal("120.1452"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2876"))
        column(name = "floor", valueNumeric = 5)
        column(name = "is_default", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '250 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '250 days')")
      }
      // 张伟(1 条,写字楼)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000005)
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "receiver_name", value = "张伟")
        column(name = "phone", value = "13800138003")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文三路138号 东部软件园8号楼1203室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1339"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2793"))
        column(name = "floor", valueNumeric = 12)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '280 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '280 days')")
      }
      // 陈晓东(3 条,餐饮店)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000006)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "receiver_name", value = "陈晓东")
        column(name = "phone", value = "13800138004")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "学院路77号 黄龙万科中心美食街16号铺")
        column(name = "longitude", valueNumeric = BigDecimal("120.1323"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2739"))
        column(name = "floor", valueNumeric = 1)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '580 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '580 days')")
      }
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000007)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "receiver_name", value = "陈晓东")
        column(name = "phone", value = "13800138004")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文三路90号 东部软件园食堂2楼")
        column(name = "longitude", valueNumeric = BigDecimal("120.1345"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2801"))
        column(name = "floor", valueNumeric = 2)
        column(name = "is_default", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
      }
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000008)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "receiver_name", value = "陈晓东")
        column(name = "phone", value = "13800138004")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "拱墅区")
        column(name = "district_code", value = "330105")
        column(name = "detail_address", value = "莫干山路132号 耀江国际大厦1楼餐厅")
        column(name = "longitude", valueNumeric = BigDecimal("120.1486"))
        column(name = "latitude", valueNumeric = BigDecimal("30.3001"))
        column(name = "floor", valueNumeric = 1)
        column(name = "is_default", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
      }
      // 林芳(1 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000009)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "receiver_name", value = "林芳")
        column(name = "phone", value = "13800138005")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "西溪路550号 西溪花园翠竹苑12幢3单元502室")
        column(name = "longitude", valueNumeric = BigDecimal("120.0711"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2672"))
        column(name = "floor", valueNumeric = 5)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '190 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '190 days')")
      }
      // 赵强(1 条,奶茶店)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000010)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "receiver_name", value = "赵强")
        column(name = "phone", value = "13800138006")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "紫荆花路2号 世纪联华超市旁奶茶店")
        column(name = "longitude", valueNumeric = BigDecimal("120.1143"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2920"))
        column(name = "floor", valueNumeric = 1)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '700 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '700 days')")
      }
      // 孙志远(1 条,写字楼)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000011)
        column(name = "user_id", valueNumeric = 1000000000000000007)
        column(name = "receiver_name", value = "孙志远")
        column(name = "phone", value = "13800138007")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文一西路969号 西溪银泰城A座写字楼15楼")
        column(name = "longitude", valueNumeric = BigDecimal("120.0613"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2796"))
        column(name = "floor", valueNumeric = 15)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '140 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '140 days')")
      }
      // 周丽娟(1 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000012)
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "receiver_name", value = "周丽娟")
        column(name = "phone", value = "13800138008")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "古翠路60号 康新花园2幢1801室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1206"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2892"))
        column(name = "floor", valueNumeric = 18)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '250 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '250 days')")
      }
      // 吴刚(2 条,企业)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000013)
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "receiver_name", value = "吴刚")
        column(name = "phone", value = "13800138009")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "黄龙路1号 黄龙世纪广场B座9楼")
        column(name = "longitude", valueNumeric = BigDecimal("120.1271"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2707"))
        column(name = "floor", valueNumeric = 9)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '480 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '480 days')")
      }
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000014)
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "receiver_name", value = "吴刚")
        column(name = "phone", value = "13800138009")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "天目山路160号 国际花园写字楼7楼")
        column(name = "longitude", valueNumeric = BigDecimal("120.1352"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2730"))
        column(name = "floor", valueNumeric = 7)
        column(name = "is_default", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
      }
      // 郑秀兰(1 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000015)
        column(name = "user_id", valueNumeric = 1000000000000000010)
        column(name = "receiver_name", value = "郑秀兰")
        column(name = "phone", value = "13800138010")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "文二西路5号 竞舟路小区3幢402室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1118"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2874"))
        column(name = "floor", valueNumeric = 4)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '170 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '170 days')")
      }
      // 何俊杰(1 条,新用户)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000016)
        column(name = "user_id", valueNumeric = 1000000000000000011)
        column(name = "receiver_name", value = "何俊杰")
        column(name = "phone", value = "13800138011")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "古墩路与余杭塘路交叉口 城西银泰城1号楼2301室")
        column(name = "longitude", valueNumeric = BigDecimal("120.0952"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2951"))
        column(name = "floor", valueNumeric = 23)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      // 高敏(1 条)
      insert(tableName = "addresses") {
        column(name = "id", valueNumeric = 2000000000000000017)
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "receiver_name", value = "高敏")
        column(name = "phone", value = "13800138012")
        column(name = "province", value = "浙江省")
        column(name = "province_code", value = "330000")
        column(name = "city", value = "杭州市")
        column(name = "city_code", value = "330100")
        column(name = "district", value = "西湖区")
        column(name = "district_code", value = "330106")
        column(name = "detail_address", value = "莲花街109号 莲花新村1幢2单元201室")
        column(name = "longitude", valueNumeric = BigDecimal("120.1040"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2822"))
        column(name = "floor", valueNumeric = 2)
        column(name = "is_default", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '110 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '110 days')")
      }
      // ==================== 商品(12 款南山牌) ====================
      // 商品 1:旗舰 18.9L 桶装水
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000001)
        column(name = "name", value = "南山牌 天然山泉水 18.9L 桶装水")
        column(name = "subtitle", value = "天目山泉 弱碱性 甘甜清冽")
        column(name = "price_cents", valueNumeric = 2500)
        column(name = "original_price_cents", valueNumeric = 2800)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(
          name = "detail_images",
          value = "[\"/static/default-product.jpeg\", \"/static/default-product.jpeg\"]",
        )
        column(name = "specification", value = "18.9L/桶")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,锶 0.6mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 200)
        column(name = "sales_volume", valueNumeric = 1286)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 1)
        column(name = "tags", value = "[\"热销\", \"山泉水\", \"家庭装\"]")
        column(
          name = "description",
          value = "南山牌天然山泉水,源自临安天目山脉深层山泉,富含偏硅酸等矿物质,弱碱性水质甘甜清冽。18.9L 大桶装满足家庭与办公饮水需求,首次订水需购买塑料水桶(水桶费见「买水桶」)。",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 2:11.3L 小桶
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000002)
        column(name = "name", value = "南山牌 天然山泉水 11.3L 桶装水")
        column(name = "subtitle", value = "小桶轻量 适合小家庭")
        column(name = "price_cents", valueNumeric = 1800)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "11.3L/桶")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 150)
        column(name = "sales_volume", valueNumeric = 634)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 2)
        column(name = "tags", value = "[\"山泉水\"]")
        column(name = "description", value = "南山牌 11.3L 小桶装天然山泉水,水源同 18.9L 大桶,小规格更适合小家庭与办公室,搬运更轻便。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '380 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 3:5L 便携装
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000003)
        column(name = "name", value = "南山牌 天然山泉水 5L 便携装")
        column(name = "subtitle", value = "出行露营 即开即饮")
        column(name = "price_cents", valueNumeric = 1200)
        column(name = "original_price_cents", valueNumeric = 1500)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "5L/袋装")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 500)
        column(name = "sales_volume", valueNumeric = 892)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 3)
        column(name = "tags", value = "[\"便携\", \"露营\"]")
        column(name = "description", value = "南山牌 5L 便携袋装山泉水,适合露营、出行与办公室小场景,无需水桶,即开即饮。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '300 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 4:4.5L×2 礼盒装
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000004)
        column(name = "name", value = "南山牌 天然矿泉水 4.5L×2 礼盒装")
        column(name = "subtitle", value = "高端礼盒 商务馈赠")
        column(name = "price_cents", valueNumeric = 2800)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "4.5L×2瓶")
        column(name = "water_source", value = "临安天目山脉深层泉眼")
        column(name = "mineral_content", value = "锶 0.6mg/L,偏硅酸 30mg/L")
        column(name = "stock_quantity", valueNumeric = 80)
        column(name = "sales_volume", valueNumeric = 156)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 4)
        column(name = "tags", value = "[\"礼盒\", \"矿泉水\"]")
        column(name = "description", value = "南山牌天然矿泉水礼盒装,选用天目山脉深层泉眼水,富锶弱碱,包装精致,适合商务馈赠与家庭待客。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '250 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 5:平价纯净水 18.9L
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000005)
        column(name = "name", value = "南山牌 饮用纯净水 18.9L 桶装水")
        column(name = "subtitle", value = "经济实惠 泡茶煮饭皆宜")
        column(name = "price_cents", valueNumeric = 1800)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "18.9L/桶")
        column(name = "water_source", value = "城市管网深度净化")
        column(name = "mineral_content", value = "纯净水 低矿化")
        column(name = "stock_quantity", valueNumeric = 120)
        column(name = "sales_volume", valueNumeric = 453)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 5)
        column(name = "tags", value = "[\"纯净水\", \"实惠\"]")
        column(name = "description", value = "南山牌饮用纯净水,多级反渗透深度净化,口感纯净,价格实惠,适合餐饮后厨与日常饮用。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '220 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 6:550ml×24 整箱
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000006)
        column(name = "name", value = "南山牌 天然山泉水 550ml×24 整箱")
        column(name = "subtitle", value = "整箱装 办公常备")
        column(name = "price_cents", valueNumeric = 3600)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "550ml×24瓶")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 300)
        column(name = "sales_volume", valueNumeric = 421)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 6)
        column(name = "tags", value = "[\"整箱\", \"办公\"]")
        column(name = "description", value = "南山牌天然山泉水瓶装整箱,550ml×24 瓶,会议、前台、健身房常备,单箱更划算。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '200 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 7:1.5L×12 整箱
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000007)
        column(name = "name", value = "南山牌 天然山泉水 1.5L×12 整箱")
        column(name = "subtitle", value = "大瓶装 厨房泡茶")
        column(name = "price_cents", valueNumeric = 2400)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "1.5L×12瓶")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 260)
        column(name = "sales_volume", valueNumeric = 378)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 7)
        column(name = "tags", value = "[\"整箱\"]")
        column(name = "description", value = "南山牌天然山泉水 1.5L 大瓶整箱,适合家庭厨房、泡茶与多人口场景。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '190 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 8:饮水机
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000008)
        column(name = "name", value = "南山牌 冷热双用饮水机")
        column(name = "subtitle", value = "下置式 冷热双温")
        column(name = "price_cents", valueNumeric = 29900)
        column(name = "original_price_cents", valueNumeric = 35900)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "下置式冷热双温")
        column(name = "stock_quantity", valueNumeric = 50)
        column(name = "sales_volume", valueNumeric = 89)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 8)
        column(name = "tags", value = "[\"设备\"]")
        column(name = "description", value = "南山牌下置式冷热双用饮水机,适配 18.9L 标准桶,加热快、噪音低,购机免费上门安装。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '180 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 9:电动抽水泵
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000009)
        column(name = "name", value = "南山牌 电动抽水泵")
        column(name = "subtitle", value = "USB 充电 一键出水")
        column(name = "price_cents", valueNumeric = 2500)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "通用型")
        column(name = "stock_quantity", valueNumeric = 200)
        column(name = "sales_volume", valueNumeric = 310)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 9)
        column(name = "tags", value = "[\"配件\"]")
        column(name = "description", value = "南山牌 USB 充电电动抽水泵,适配 18.9L/11.3L 标准桶,一键出水,免安装。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '160 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 10:桶盖
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000010)
        column(name = "name", value = "南山牌 桶装水桶盖(10只装)")
        column(name = "subtitle", value = "密封防尘 加厚耐用")
        column(name = "price_cents", valueNumeric = 1500)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "10只/包")
        column(name = "stock_quantity", valueNumeric = 400)
        column(name = "sales_volume", valueNumeric = 265)
        column(name = "status", value = "ONLINE")
        column(name = "sort_order", valueNumeric = 10)
        column(name = "tags", value = "[\"配件\"]")
        column(name = "description", value = "南山牌标准桶盖,加厚密封,防止二次污染,10 只装家庭常备。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 商品 11:7.5L 家用装(缺货)
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000011)
        column(name = "name", value = "南山牌 天然山泉水 7.5L 家用装")
        column(name = "subtitle", value = "中桶装 家庭优选")
        column(name = "price_cents", valueNumeric = 1600)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "7.5L/桶")
        column(name = "water_source", value = "临安天目山脉深层山泉")
        column(name = "mineral_content", value = "偏硅酸 28mg/L,弱碱性 pH 7.2")
        column(name = "stock_quantity", valueNumeric = 0)
        column(name = "sales_volume", valueNumeric = 198)
        column(name = "status", value = "OUT_OF_STOCK")
        column(name = "sort_order", valueNumeric = 11)
        column(name = "tags", value = "[\"山泉水\"]")
        column(name = "description", value = "南山牌 7.5L 中桶装天然山泉水,补货中,敬请期待。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '140 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      // 商品 12:泡茶山泉水(下架)
      insert(tableName = "products") {
        column(name = "id", valueNumeric = 3000000000000000012)
        column(name = "name", value = "南山牌 高端泡茶山泉水 4L")
        column(name = "subtitle", value = "茶道专用 小分子团")
        column(name = "price_cents", valueNumeric = 3200)
        column(name = "thumbnail_url", value = "/static/default-product.jpeg")
        column(name = "detail_images", value = "[\"/static/default-product.jpeg\"]")
        column(name = "specification", value = "4L/瓶")
        column(name = "water_source", value = "临安天目山脉深层泉眼")
        column(name = "mineral_content", value = "小分子团水,低钠淡矿")
        column(name = "stock_quantity", valueNumeric = 60)
        column(name = "sales_volume", valueNumeric = 45)
        column(name = "status", value = "OFFLINE")
        column(name = "sort_order", valueNumeric = 12)
        column(name = "tags", value = "[\"泡茶\", \"高端\"]")
        column(name = "description", value = "南山牌高端泡茶山泉水,小分子团水更易析出茶香,已下架,老客户可联系客服咨询。")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '130 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
      }
      // ==================== 水桶费价目(G06 一次性塑料水桶费,独立价目表) ====================
      insert(tableName = "barrel_fees") {
        column(name = "id", valueNumeric = 3100000000000000001)
        column(name = "specification", value = "18.9L/桶")
        column(name = "price_cents", valueNumeric = 4000)
        column(name = "status", value = "ENABLED")
        column(name = "sort_order", valueNumeric = 1)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "barrel_fees") {
        column(name = "id", valueNumeric = 3100000000000000002)
        column(name = "specification", value = "11.3L/桶")
        column(name = "price_cents", valueNumeric = 3000)
        column(name = "status", value = "ENABLED")
        column(name = "sort_order", valueNumeric = 2)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 商品收藏(F-PRO 收藏演示) ====================
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000001)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000002)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000003)
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "product_id", valueNumeric = 3000000000000000006)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000004)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000005)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "product_id", valueNumeric = 3000000000000000003)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000006)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "product_id", valueNumeric = 3000000000000000008)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000007)
        column(name = "user_id", valueNumeric = 1000000000000000007)
        column(name = "product_id", valueNumeric = 3000000000000000002)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000008)
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000009)
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      insert(tableName = "product_favorites") {
        column(name = "id", valueNumeric = 8800000000000000010)
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "product_id", valueNumeric = 3000000000000000003)
        column(name = "enable", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
    }
    changeSet(author = "aqua-rush", id = "1.7.0-seed-nanshan-marketing") {
      comment(
        "Seed 南山牌 demo marketing configs: group prices, coupons, promotions, water tickets, flash sale, gift, first-order (PRD 10-marketing.md)",
      )
      // ==================== 分组专属价(F-MKT-002) ====================
      insert(tableName = "group_product_prices") {
        column(name = "id", valueNumeric = 7000000000000000101)
        column(name = "customer_group_id", valueNumeric = 7000000000000000001)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "price_cents", valueNumeric = 2300)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
      }
      insert(tableName = "group_product_prices") {
        column(name = "id", valueNumeric = 7000000000000000102)
        column(name = "customer_group_id", valueNumeric = 7000000000000000001)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "price_cents", valueNumeric = 1600)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '150 days')")
      }
      insert(tableName = "group_product_prices") {
        column(name = "id", valueNumeric = 7000000000000000103)
        column(name = "customer_group_id", valueNumeric = 7000000000000000002)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "price_cents", valueNumeric = 2400)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '120 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '120 days')")
      }
      // ==================== 优惠券模板(F-MKT-003) ====================
      // 券 1:新人见面礼 5 元无门槛券
      insert(tableName = "coupon_templates") {
        column(name = "id", valueNumeric = 7100000000000000001)
        column(name = "name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_type", value = "NO_THRESHOLD_CASH")
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "ALL")
        column(name = "validity_type", value = "DAYS_AFTER_CLAIM")
        column(name = "valid_days", valueNumeric = 30)
        column(name = "issuance_type", value = "AUTO_CLAIM")
        column(name = "per_user_limit", valueNumeric = 1)
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '60 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 券 2:满 100 减 10
      insert(tableName = "coupon_templates") {
        column(name = "id", valueNumeric = 7100000000000000002)
        column(name = "name", value = "全场满100减10元券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 10000)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "validity_type", value = "FIXED_RANGE")
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '30 days')")
        column(name = "issuance_type", value = "AUTO_CLAIM")
        column(name = "per_user_limit", valueNumeric = 2)
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 券 3:桶装水专享 满 50 减 5
      insert(tableName = "coupon_templates") {
        column(name = "id", valueNumeric = 7100000000000000003)
        column(name = "name", value = "桶装水专享 满50减5券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 5000)
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "validity_type", value = "FIXED_RANGE")
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '30 days')")
        column(name = "issuance_type", value = "TARGETED")
        column(name = "per_user_limit", valueNumeric = 1)
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 券 4:老客回馈 9 折券(已停用,演示停用态)
      insert(tableName = "coupon_templates") {
        column(name = "id", valueNumeric = 7100000000000000004)
        column(name = "name", value = "老客回馈 9折券")
        column(name = "coupon_type", value = "DISCOUNT")
        column(name = "discount_rate_permille", valueNumeric = 900)
        column(name = "scope", value = "ALL")
        column(name = "validity_type", value = "DAYS_AFTER_CLAIM")
        column(name = "valid_days", valueNumeric = 15)
        column(name = "issuance_type", value = "TARGETED")
        column(name = "per_user_limit", valueNumeric = 1)
        column(name = "status", value = "DISABLED")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '40 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      // ==================== 用户优惠券实例 ====================
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000101)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000002)
        column(name = "status", value = "RECEIVED")
        column(name = "name", value = "全场满100减10元券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 10000)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '20 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000102)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000001)
        column(name = "status", value = "USED")
        column(name = "name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_type", value = "NO_THRESHOLD_CASH")
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '1 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
        column(name = "used_order_id", valueNumeric = 4000000000000000045)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000103)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000002)
        column(name = "status", value = "RECEIVED")
        column(name = "name", value = "全场满100减10元券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 10000)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '25 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000104)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000003)
        column(name = "status", value = "USED")
        column(name = "name", value = "桶装水专享 满50减5券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 5000)
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '15 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "used_order_id", valueNumeric = 4000000000000000020)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000105)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000003)
        column(name = "status", value = "USED")
        column(name = "name", value = "桶装水专享 满50减5券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 5000)
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '18 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "used_order_id", valueNumeric = 4000000000000000023)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000106)
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000002)
        column(name = "status", value = "RECEIVED")
        column(name = "name", value = "全场满100减10元券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 10000)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '22 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000107)
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000002)
        column(name = "status", value = "USED")
        column(name = "name", value = "全场满100减10元券")
        column(name = "coupon_type", value = "CASH")
        column(name = "threshold_cents", valueNumeric = 10000)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '10 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "used_order_id", valueNumeric = 4000000000000000013)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000108)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000001)
        column(name = "status", value = "RECEIVED")
        column(name = "name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_type", value = "NO_THRESHOLD_CASH")
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '3 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000109)
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000001)
        column(name = "status", value = "EXPIRED")
        column(name = "name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_type", value = "NO_THRESHOLD_CASH")
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '32 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '32 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "user_coupons") {
        column(name = "id", valueNumeric = 7100000000000000110)
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "coupon_template_id", valueNumeric = 7100000000000000001)
        column(name = "status", value = "USED")
        column(name = "name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_type", value = "NO_THRESHOLD_CASH")
        column(name = "discount_amount_cents", valueNumeric = 500)
        column(name = "scope", value = "ALL")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '2 days')")
        column(name = "claim_time", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(name = "used_order_id", valueNumeric = 4000000000000000035)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
      }
      // ==================== 满减/折扣活动(F-MKT-004) ====================
      // 活动 1:全场满 200 减 20
      insert(tableName = "promotion_activities") {
        column(name = "id", valueNumeric = 7200000000000000001)
        column(name = "name", value = "全场满200减20")
        column(name = "activity_type", value = "FULL_REDUCE")
        column(name = "scope", value = "ALL")
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '50 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 活动 2:桶装水 9 折
      insert(tableName = "promotion_activities") {
        column(name = "id", valueNumeric = 7200000000000000002)
        column(name = "name", value = "南山桶装水 9折优惠")
        column(name = "activity_type", value = "FULL_DISCOUNT")
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '25 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 活动 1 档位:满 200 减 20 / 满 500 减 60
      insert(tableName = "promotion_activity_tiers") {
        column(name = "id", valueNumeric = 7200000000000000101)
        column(name = "activity_id", valueNumeric = 7200000000000000001)
        column(name = "threshold_cents", valueNumeric = 20000)
        column(name = "discount_amount_cents", valueNumeric = 2000)
        column(name = "sort", valueNumeric = 1)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "promotion_activity_tiers") {
        column(name = "id", valueNumeric = 7200000000000000102)
        column(name = "activity_id", valueNumeric = 7200000000000000001)
        column(name = "threshold_cents", valueNumeric = 50000)
        column(name = "discount_amount_cents", valueNumeric = 6000)
        column(name = "sort", valueNumeric = 2)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 活动 2 档位:9 折
      insert(tableName = "promotion_activity_tiers") {
        column(name = "id", valueNumeric = 7200000000000000103)
        column(name = "activity_id", valueNumeric = 7200000000000000002)
        column(name = "threshold_cents", valueNumeric = 100)
        column(name = "discount_rate_permille", valueNumeric = 900)
        column(name = "sort", valueNumeric = 1)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 水票(F-MKT-007) ====================
      // 票种 1:18.9L 桶水票 10 张装(买 10 送 1)
      insert(tableName = "ticket_types") {
        column(name = "id", valueNumeric = 7300000000000000001)
        column(name = "name", value = "南山山泉水 18.9L 水票(10张装)")
        column(name = "ticket_type", value = "SPECIFIC_PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "specification", value = "18.9L")
        column(name = "package_count", valueNumeric = 10)
        column(name = "package_price_cents", valueNumeric = 20000)
        column(name = "gift_count", valueNumeric = 1)
        column(name = "validity_type", value = "DAYS_AFTER_PURCHASE")
        column(name = "valid_days", valueNumeric = 180)
        column(name = "per_user_limit", valueNumeric = 5)
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '90 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 票种 2:通用水票 5 张装
      insert(tableName = "ticket_types") {
        column(name = "id", valueNumeric = 7300000000000000002)
        column(name = "name", value = "通用水票(5张装)")
        column(name = "ticket_type", value = "UNIVERSAL")
        column(name = "specification", value = "通用")
        column(name = "package_count", valueNumeric = 5)
        column(name = "package_price_cents", valueNumeric = 10000)
        column(name = "gift_count", valueNumeric = 0)
        column(name = "validity_type", value = "FIXED_RANGE")
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '45 days')")
        column(name = "per_user_limit", valueNumeric = 10)
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '60 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 购票单(PAID)
      insert(tableName = "ticket_purchases") {
        column(name = "id", valueNumeric = 7300000000000000101)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "order_no", value = "TKN2024000001")
        column(name = "package_count", valueNumeric = 1)
        column(name = "gift_count", valueNumeric = 1)
        column(name = "total_count", valueNumeric = 11)
        column(name = "package_price_cents", valueNumeric = 20000)
        column(name = "payment_status", value = "PAID")
        column(name = "transaction_id", value = "WXTXN-TK-101")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "ticket_purchases") {
        column(name = "id", valueNumeric = 7300000000000000102)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000002)
        column(name = "order_no", value = "TKN2024000002")
        column(name = "package_count", valueNumeric = 2)
        column(name = "gift_count", valueNumeric = 0)
        column(name = "total_count", valueNumeric = 10)
        column(name = "package_price_cents", valueNumeric = 20000)
        column(name = "payment_status", value = "PAID")
        column(name = "transaction_id", value = "WXTXN-TK-102")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      insert(tableName = "ticket_purchases") {
        column(name = "id", valueNumeric = 7300000000000000103)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "order_no", value = "TKN2024000003")
        column(name = "package_count", valueNumeric = 1)
        column(name = "gift_count", valueNumeric = 1)
        column(name = "total_count", valueNumeric = 11)
        column(name = "package_price_cents", valueNumeric = 20000)
        column(name = "payment_status", value = "PAID")
        column(name = "transaction_id", value = "WXTXN-TK-103")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
      }
      // 用户水票实例(王建国 7 张)
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000201)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000202)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000203)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000204)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000205)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000206)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = true)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000207)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000101)
        column(name = "status", value = "USED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '168 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "used_order_id", valueNumeric = 4000000000000000018)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      // 用户水票实例(赵强 4 张)
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000208)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000002)
        column(name = "purchase_id", valueNumeric = 7300000000000000102)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '40 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000209)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000002)
        column(name = "purchase_id", valueNumeric = 7300000000000000102)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '40 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000210)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000002)
        column(name = "purchase_id", valueNumeric = 7300000000000000102)
        column(name = "status", value = "USED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '40 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "used_order_id", valueNumeric = 4000000000000000030)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000211)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000002)
        column(name = "purchase_id", valueNumeric = 7300000000000000102)
        column(name = "status", value = "USED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '40 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "used_order_id", valueNumeric = 4000000000000000004)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
      }
      // 用户水票实例(陈晓东 2 张)
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000212)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000103)
        column(name = "status", value = "UNUSED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '170 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
      }
      insert(tableName = "user_tickets") {
        column(name = "id", valueNumeric = 7300000000000000213)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "ticket_type_id", valueNumeric = 7300000000000000001)
        column(name = "purchase_id", valueNumeric = 7300000000000000103)
        column(name = "status", value = "USED")
        column(name = "expire_time", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '170 days')")
        column(name = "is_gift", valueBoolean = false)
        column(name = "used_order_id", valueNumeric = 4000000000000000014)
        column(name = "used_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      // ==================== 限时特价(F-MKT-005) ====================
      insert(tableName = "flash_sale_activities") {
        column(name = "id", valueNumeric = 7400000000000000001)
        column(name = "name", value = "5L便携装 9.9元限时特价")
        column(name = "product_id", valueNumeric = 3000000000000000003)
        column(name = "sale_price_cents", valueNumeric = 990)
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '7 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 买赠/满赠(F-MKT-008) ====================
      // 活动 1:买 3 桶 18.9L 送 1 桶
      insert(tableName = "gift_activities") {
        column(name = "id", valueNumeric = 7500000000000000001)
        column(name = "name", value = "买3桶送1桶")
        column(name = "gift_type", value = "BUY_GIFT")
        column(name = "threshold_quantity", valueNumeric = 3)
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "gift_product_id", valueNumeric = 3000000000000000001)
        column(name = "gift_quantity", valueNumeric = 1)
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '30 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 活动 2:满 200 送 5L 便携装 1 桶
      insert(tableName = "gift_activities") {
        column(name = "id", valueNumeric = 7500000000000000002)
        column(name = "name", value = "满200送5L便携装")
        column(name = "gift_type", value = "FULL_GIFT")
        column(name = "threshold_cents", valueNumeric = 20000)
        column(name = "scope", value = "ALL")
        column(name = "gift_product_id", valueNumeric = 3000000000000000003)
        column(name = "gift_quantity", valueNumeric = 1)
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '20 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 首单活动(F-MKT-009) ====================
      // 活动 1:新人首单立减 10 元
      insert(tableName = "first_order_activities") {
        column(name = "id", valueNumeric = 7600000000000000001)
        column(name = "name", value = "新人首单立减10元")
        column(name = "activity_type", value = "FIRST_ORDER_REDUCE")
        column(name = "threshold_cents", valueNumeric = 0)
        column(name = "discount_amount_cents", valueNumeric = 1000)
        column(name = "scope", value = "ALL")
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '60 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '90 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '60 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 活动 2:新人专享 18.9L 桶 19.9 元
      insert(tableName = "first_order_activities") {
        column(name = "id", valueNumeric = 7600000000000000002)
        column(name = "name", value = "新人专享 18.9L桶 19.9元")
        column(name = "activity_type", value = "NEWCOMER_PRICE")
        column(name = "scope", value = "PRODUCT")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "newcomer_price_cents", valueNumeric = 1990)
        column(name = "valid_from", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "valid_to", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '60 days')")
        column(name = "status", value = "ACTIVE")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
    }
    changeSet(author = "aqua-rush", id = "1.7.0-seed-nanshan-orders") {
      comment(
        "Seed 南山牌 demo orders (last 30 days), order items, gifts, reviews, delivery trajectories (PRD 04-order.md / 05-address-delivery.md)",
      )
      // ======================================================================
      //         订单 1:王建国 2×18.9L(COMPLETED,陈永强)
      //     ======================================================================
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000001)
        column(name = "order_no", value = "NS00000001")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0001")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(
          name = "delivery_photos",
          value = "[\"https://example.com/delivery/ns00000001_1.jpg\", \"https://example.com/delivery/ns00000001_2.jpg\"]",
        )
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "空桶放门口即可")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000001)
        column(name = "order_id", valueNumeric = 4000000000000000001)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
      }
      // 订单 2:李秀英 1×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000002)
        column(name = "order_no", value = "NS00000002")
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000003)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0002")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "教工路223号 文教社区4幢1单元302室 李秀英 13800138002 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000002)
        column(name = "order_id", valueNumeric = 4000000000000000002)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
      }
      // 订单 3:陈晓东 3×18.9L 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000003)
        column(name = "order_no", value = "NS00000003")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 6900)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0003")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "delivery_photos", value = "[\"https://example.com/delivery/ns00000003_1.jpg\"]")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '35 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '45 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "送餐厅后厨,空桶收走")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '45 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000003)
        column(name = "order_id", valueNumeric = 4000000000000000003)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 6900)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
      }
      // 订单 4:赵强 2×18.9L+1×抽水泵,核销水票1张(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000004)
        column(name = "order_no", value = "NS00000004")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 7500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 1)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "CASH_ON_DELIVERY")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "WATER_TICKET")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '20 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(name = "remark", value = "核销通用水票1张")
        column(
          name = "search_content",
          value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水 南山牌 电动抽水泵",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000004)
        column(name = "order_id", valueNumeric = 4000000000000000004)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000005)
        column(name = "order_id", valueNumeric = 4000000000000000004)
        column(name = "product_id", valueNumeric = 3000000000000000009)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000009,\"name\":\"南山牌 电动抽水泵\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
      }
      // 订单 5:吴刚 4×18.9L 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000005)
        column(name = "order_no", value = "NS00000005")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 9200)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0005")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "送到前台即可,自取上楼")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000006)
        column(name = "order_id", valueNumeric = 4000000000000000005)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 4)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 9200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
      }
      // 订单 6:王建国 1×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000006)
        column(name = "order_no", value = "NS00000006")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0006")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000007)
        column(name = "order_id", valueNumeric = 4000000000000000006)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
      }
      // 订单 7:林芳 1×5L 便携装(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000007)
        column(name = "order_no", value = "NS00000007")
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "total_amount_cents", valueNumeric = 1200)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 1200)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000009)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0007")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "西溪路550号 西溪花园翠竹苑12幢3单元502室 林芳 13800138005 南山牌 天然山泉水 5L 便携装",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000008)
        column(name = "order_id", valueNumeric = 4000000000000000007)
        column(name = "product_id", valueNumeric = 3000000000000000003)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1200)
        column(name = "total_price_cents", valueNumeric = 1200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000003,\"name\":\"南山牌 天然山泉水 5L 便携装\",\"price_cents\":1200,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '26 days')")
      }
      // 订单 8:张伟 2×550ml整箱(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000008)
        column(name = "order_no", value = "NS00000008")
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "total_amount_cents", valueNumeric = 7200)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 3600)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000005)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0008")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "放前台快递架")
        column(
          name = "search_content",
          value = "文三路138号 东部软件园8号楼1203室 张伟 13800138003 南山牌 天然山泉水 550ml×24 整箱",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000009)
        column(name = "order_id", valueNumeric = 4000000000000000008)
        column(name = "product_id", valueNumeric = 3000000000000000006)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 3600)
        column(name = "total_price_cents", valueNumeric = 7200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000006,\"name\":\"南山牌 天然山泉水 550ml×24 整箱\",\"price_cents\":3600,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '25 days')")
      }
      // 订单 9:周丽娟 1×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000009)
        column(name = "order_no", value = "NS00000009")
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000012)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "CASH_ON_DELIVERY")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "CASH")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '20 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '55 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "古翠路60号 康新花园2幢1801室 周丽娟 13800138008 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000010)
        column(name = "order_id", valueNumeric = 4000000000000000009)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days')")
      }
      // 订单 10:陈晓东 8×18.9L 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000010)
        column(name = "order_no", value = "NS00000010")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 18400)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000007)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0010")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '55 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "文三路90号 东部软件园食堂2楼 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000010)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 8)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 18400)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '24 days')")
      }
      // 订单 11:赵强 3×18.9L 触发买3送1(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000011)
        column(name = "order_no", value = "NS00000011")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 7500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0011")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(
          name = "delivery_photos",
          value = "[\"https://example.com/delivery/ns00000011_1.jpg\", \"https://example.com/delivery/ns00000011_2.jpg\"]",
        )
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "买3送1活动,赠1桶")
        column(name = "search_content", value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000011)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 7500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
      }
      // 订单 12:王建国 2×18.9L(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000012)
        column(name = "order_no", value = "NS00000012")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000002)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0012")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "文一西路588号 西溪诚园2期5幢1201室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000013)
        column(name = "order_id", valueNumeric = 4000000000000000012)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
      }
      // 订单 13:吴刚 6×18.9L 企业价+满100减10券(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000013)
        column(name = "order_no", value = "NS00000013")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 13800)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "coupon_id", valueNumeric = 7100000000000000107)
        column(name = "coupon_name", value = "全场满100减10元券")
        column(name = "coupon_discount_cents", valueNumeric = 1000)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0013")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "使用满100减10券")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000014)
        column(name = "order_id", valueNumeric = 4000000000000000013)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 6)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 13800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      // 订单 14:陈晓东 2×18.9L+2×纯净水,核销水票1张(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000014)
        column(name = "order_no", value = "NS00000014")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 7800)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 1)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0014")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "WATER_TICKET")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "核销18.9L水票1张")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水 南山牌 饮用纯净水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000015)
        column(name = "order_id", valueNumeric = 4000000000000000014)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000016)
        column(name = "order_id", valueNumeric = 4000000000000000014)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 1600)
        column(name = "total_price_cents", valueNumeric = 3200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000005,\"name\":\"南山牌 饮用纯净水 18.9L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      // 订单 15:孙志远 1×11.3L 首单立减(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000015)
        column(name = "order_no", value = "NS00000015")
        column(name = "user_id", valueNumeric = 1000000000000000007)
        column(name = "total_amount_cents", valueNumeric = 1800)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "first_order_activity_id", valueNumeric = 7600000000000000001)
        column(name = "first_order_activity_name", value = "新人首单立减10元")
        column(name = "first_order_discount_cents", valueNumeric = 1000)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000011)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0015")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(name = "remark", value = "新用户首单立减10元")
        column(
          name = "search_content",
          value = "文一西路969号 西溪银泰城A座写字楼15楼 孙志远 13800138007 南山牌 天然山泉水 11.3L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000017)
        column(name = "order_id", valueNumeric = 4000000000000000015)
        column(name = "product_id", valueNumeric = 3000000000000000002)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "total_price_cents", valueNumeric = 1800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000002,\"name\":\"南山牌 天然山泉水 11.3L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
      }
      // 订单 16:李秀英 2×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000016)
        column(name = "order_no", value = "NS00000016")
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000003)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0016")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "教工路223号 文教社区4幢1单元302室 李秀英 13800138002 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000018)
        column(name = "order_id", valueNumeric = 4000000000000000016)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '21 days')")
      }
      // 订单 17:赵强 10×18.9L 满200减20(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000017)
        column(name = "order_no", value = "NS00000017")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 25000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "promotion_activity_id", valueNumeric = 7200000000000000001)
        column(name = "promotion_activity_name", value = "全场满200减20")
        column(name = "promotion_discount_cents", valueNumeric = 2000)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0017")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "delivery_photos", value = "[\"https://example.com/delivery/ns00000017_1.jpg\"]")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '20 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '1 hours')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '1 hours' + INTERVAL '10 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "满200减20活动已优惠")
        column(name = "search_content", value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '1 hours' + INTERVAL '10 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000019)
        column(name = "order_id", valueNumeric = 4000000000000000017)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 10)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 25000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      // 订单 18:王建国 1×18.9L 水票核销(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000018)
        column(name = "order_no", value = "NS00000018")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 1)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0018")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "WATER_TICKET")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "核销18.9L水票1张")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000020)
        column(name = "order_id", valueNumeric = 4000000000000000018)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      // 订单 19:吴刚 3×纯净水(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000019)
        column(name = "order_no", value = "NS00000019")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 5400)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000014)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0019")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "天目山路160号 国际花园写字楼7楼 吴刚 13800138009 南山牌 饮用纯净水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000021)
        column(name = "order_id", valueNumeric = 4000000000000000019)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "total_price_cents", valueNumeric = 5400)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000005,\"name\":\"南山牌 饮用纯净水 18.9L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
      }
      // 订单 20:陈晓东 2×18.9L+饮水机,满200减20+专享券(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000020)
        column(name = "order_no", value = "NS00000020")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 34500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "promotion_activity_id", valueNumeric = 7200000000000000001)
        column(name = "promotion_activity_name", value = "全场满200减20")
        column(name = "promotion_discount_cents", valueNumeric = 2000)
        column(name = "coupon_id", valueNumeric = 7100000000000000104)
        column(name = "coupon_name", value = "桶装水专享 满50减5券")
        column(name = "coupon_discount_cents", valueNumeric = 500)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0020")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(
          name = "delivery_photos",
          value = "[\"https://example.com/delivery/ns00000020_1.jpg\", \"https://example.com/delivery/ns00000020_2.jpg\"]",
        )
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '20 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '1 hours')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '1 hours' + INTERVAL '15 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(name = "remark", value = "送饮水机请提前电话联系")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水 南山牌 冷热双用饮水机",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '1 hours' + INTERVAL '15 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000022)
        column(name = "order_id", valueNumeric = 4000000000000000020)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000023)
        column(name = "order_id", valueNumeric = 4000000000000000020)
        column(name = "product_id", valueNumeric = 3000000000000000008)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 29900)
        column(name = "total_price_cents", valueNumeric = 29900)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000008,\"name\":\"南山牌 冷热双用饮水机\",\"price_cents\":29900,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
      }
      // 订单 21:林芳 1×18.9L(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000021)
        column(name = "order_no", value = "NS00000021")
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000009)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0021")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "西溪路550号 西溪花园翠竹苑12幢3单元502室 林芳 13800138005 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000024)
        column(name = "order_id", valueNumeric = 4000000000000000021)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      // 订单 22:周丽娟 2×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000022)
        column(name = "order_no", value = "NS00000022")
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000012)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0022")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "古翠路60号 康新花园2幢1801室 周丽娟 13800138008 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000025)
        column(name = "order_id", valueNumeric = 4000000000000000022)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      // 订单 23:赵强 2×18.9L+2×抽水泵,专享券(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000023)
        column(name = "order_no", value = "NS00000023")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 10000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "coupon_id", valueNumeric = 7100000000000000105)
        column(name = "coupon_name", value = "桶装水专享 满50减5券")
        column(name = "coupon_discount_cents", valueNumeric = 500)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0023")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水 南山牌 电动抽水泵",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000026)
        column(name = "order_id", valueNumeric = 4000000000000000023)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000027)
        column(name = "order_id", valueNumeric = 4000000000000000023)
        column(name = "product_id", valueNumeric = 3000000000000000009)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000009,\"name\":\"南山牌 电动抽水泵\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
      }
      // 订单 24:王建国 2×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000024)
        column(name = "order_no", value = "NS00000024")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000002)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0024")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "文一西路588号 西溪诚园2期5幢1201室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000028)
        column(name = "order_id", valueNumeric = 4000000000000000024)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
      }
      // 订单 25:张伟 1×18.9L+1×桶盖(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000025)
        column(name = "order_no", value = "NS00000025")
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "total_amount_cents", valueNumeric = 4000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000005)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0025")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "文三路138号 东部软件园8号楼1203室 张伟 13800138003 南山牌 天然山泉水 18.9L 桶装水 南山牌 桶装水桶盖",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000029)
        column(name = "order_id", valueNumeric = 4000000000000000025)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000030)
        column(name = "order_id", valueNumeric = 4000000000000000025)
        column(name = "product_id", valueNumeric = 3000000000000000010)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1500)
        column(name = "total_price_cents", valueNumeric = 1500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000010,\"name\":\"南山牌 桶装水桶盖(10只装)\",\"price_cents\":1500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
      }
      // 订单 26:孙志远 2×18.9L(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000026)
        column(name = "order_no", value = "NS00000026")
        column(name = "user_id", valueNumeric = 1000000000000000007)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000011)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0026")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "文一西路969号 西溪银泰城A座写字楼15楼 孙志远 13800138007 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000031)
        column(name = "order_id", valueNumeric = 4000000000000000026)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '16 days')")
      }
      // 订单 27:李秀英 1×纯净水(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000027)
        column(name = "order_no", value = "NS00000027")
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "total_amount_cents", valueNumeric = 1800)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000003)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0027")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '35 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '45 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "教工路223号 文教社区4幢1单元302室 李秀英 13800138002 南山牌 饮用纯净水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '45 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000032)
        column(name = "order_id", valueNumeric = 4000000000000000027)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "total_price_cents", valueNumeric = 1800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000005,\"name\":\"南山牌 饮用纯净水 18.9L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
      }
      // 订单 28:陈晓东 3×18.9L 企业价(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000028)
        column(name = "order_no", value = "NS00000028")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 6900)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0028")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '20 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000033)
        column(name = "order_id", valueNumeric = 4000000000000000028)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 6900)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
      }
      // 订单 29:吴刚 8×18.9L 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000029)
        column(name = "order_no", value = "NS00000029")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 18400)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0029")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '55 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000034)
        column(name = "order_id", valueNumeric = 4000000000000000029)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 8)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 18400)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
      }
      // 订单 30:赵强 1×11.3L+1×550ml整箱,水票核销(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000030)
        column(name = "order_no", value = "NS00000030")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 5400)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 1)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0030")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "WATER_TICKET")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(name = "remark", value = "核销通用水票1张")
        column(
          name = "search_content",
          value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 11.3L 桶装水 南山牌 天然山泉水 550ml×24 整箱",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000035)
        column(name = "order_id", valueNumeric = 4000000000000000030)
        column(name = "product_id", valueNumeric = 3000000000000000002)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "total_price_cents", valueNumeric = 1800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000002,\"name\":\"南山牌 天然山泉水 11.3L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000036)
        column(name = "order_id", valueNumeric = 4000000000000000030)
        column(name = "product_id", valueNumeric = 3000000000000000006)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 3600)
        column(name = "total_price_cents", valueNumeric = 3600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000006,\"name\":\"南山牌 天然山泉水 550ml×24 整箱\",\"price_cents\":3600,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
      }
      // 订单 31:林芳 2×18.9L(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000031)
        column(name = "order_no", value = "NS00000031")
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000009)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0031")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "西溪路550号 西溪花园翠竹苑12幢3单元502室 林芳 13800138005 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000037)
        column(name = "order_id", valueNumeric = 4000000000000000031)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
      }
      // 订单 32:周丽娟 1×18.9L(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000032)
        column(name = "order_no", value = "NS00000032")
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000012)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0032")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "古翠路60号 康新花园2幢1801室 周丽娟 13800138008 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000038)
        column(name = "order_id", valueNumeric = 4000000000000000032)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '13 days')")
      }
      // 订单 33:王建国 2×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000033)
        column(name = "order_no", value = "NS00000033")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0033")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000039)
        column(name = "order_id", valueNumeric = 4000000000000000033)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      // 订单 34:陈晓东 2×18.9L+2×纯净水 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000034)
        column(name = "order_no", value = "NS00000034")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 7800)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000008)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0034")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "拱墅店送餐,空桶回收")
        column(
          name = "search_content",
          value = "莫干山路132号 耀江国际大厦1楼餐厅 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水 南山牌 饮用纯净水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000040)
        column(name = "order_id", valueNumeric = 4000000000000000034)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000041)
        column(name = "order_id", valueNumeric = 4000000000000000034)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 1600)
        column(name = "total_price_cents", valueNumeric = 3200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000005,\"name\":\"南山牌 饮用纯净水 18.9L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      // 订单 35:张伟 2×18.9L,新人券(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000035)
        column(name = "order_no", value = "NS00000035")
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "coupon_id", valueNumeric = 7100000000000000110)
        column(name = "coupon_name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_discount_cents", valueNumeric = 500)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000005)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0035")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "文三路138号 东部软件园8号楼1203室 张伟 13800138003 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000042)
        column(name = "order_id", valueNumeric = 4000000000000000035)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
      }
      // 订单 36:吴刚 6×18.9L 企业价(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000036)
        column(name = "order_no", value = "NS00000036")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 13800)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0036")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '50 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000043)
        column(name = "order_id", valueNumeric = 4000000000000000036)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 6)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 13800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '11 days')")
      }
      // 订单 37:赵强 2×18.9L+1×桶盖(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000037)
        column(name = "order_no", value = "NS00000037")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 6500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0037")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水 南山牌 桶装水桶盖",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000044)
        column(name = "order_id", valueNumeric = 4000000000000000037)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000045)
        column(name = "order_id", valueNumeric = 4000000000000000037)
        column(name = "product_id", valueNumeric = 3000000000000000010)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1500)
        column(name = "total_price_cents", valueNumeric = 1500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000010,\"name\":\"南山牌 桶装水桶盖(10只装)\",\"price_cents\":1500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      // 订单 38:王建国 1×18.9L(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000038)
        column(name = "order_no", value = "NS00000038")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0038")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000046)
        column(name = "order_id", valueNumeric = 4000000000000000038)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
      }
      // 订单 39:陈晓东 4×18.9L 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000039)
        column(name = "order_no", value = "NS00000039")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 9200)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0039")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000047)
        column(name = "order_id", valueNumeric = 4000000000000000039)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 4)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 9200)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      // 订单 40:高敏 2×18.9L(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000040)
        column(name = "order_no", value = "NS00000040")
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000017)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0040")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "莲花街109号 莲花新村1幢2单元201室 高敏 13800138012 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000048)
        column(name = "order_id", valueNumeric = 4000000000000000040)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
      }
      // 订单 41:林芳 1×18.9L(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000041)
        column(name = "order_no", value = "NS00000041")
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000009)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0041")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "西溪路550号 西溪花园翠竹苑12幢3单元502室 林芳 13800138005 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000049)
        column(name = "order_id", valueNumeric = 4000000000000000041)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '7 days')")
      }
      // 订单 42:周丽娟 1×18.9L(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000042)
        column(name = "order_no", value = "NS00000042")
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000012)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0042")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "EVENING")
        column(
          name = "search_content",
          value = "古翠路60号 康新花园2幢1801室 周丽娟 13800138008 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000050)
        column(name = "order_id", valueNumeric = 4000000000000000042)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days')")
      }
      // 订单 43:赵强 3×18.9L 买3送1(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000043)
        column(name = "order_no", value = "NS00000043")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 7500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0043")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(
          name = "delivery_photos",
          value = "[\"https://example.com/delivery/ns00000043_1.jpg\", \"https://example.com/delivery/ns00000043_2.jpg\"]",
        )
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "买3送1活动,赠1桶")
        column(name = "search_content", value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000051)
        column(name = "order_id", valueNumeric = 4000000000000000043)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 7500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      // 订单 44:吴刚 2×18.9L+2×抽水泵 企业价(COMPLETED,刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000044)
        column(name = "order_no", value = "NS00000044")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 9600)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0044")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水 南山牌 电动抽水泵",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000052)
        column(name = "order_id", valueNumeric = 4000000000000000044)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000053)
        column(name = "order_id", valueNumeric = 4000000000000000044)
        column(name = "product_id", valueNumeric = 3000000000000000009)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000009,\"name\":\"南山牌 电动抽水泵\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      // 订单 45:王建国 2×18.9L,新人券(COMPLETED,黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000045)
        column(name = "order_no", value = "NS00000045")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "coupon_id", valueNumeric = 7100000000000000102)
        column(name = "coupon_name", value = "新人见面礼 5元无门槛券")
        column(name = "coupon_discount_cents", valueNumeric = 500)
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000001)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0045")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "delivery_photos", value = "[\"https://example.com/delivery/ns00000045_1.jpg\"]")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '10 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '40 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '50 minutes')",
        )
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "古墩路808号 三墩颐景园3幢2单元601室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days' + INTERVAL '50 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000054)
        column(name = "order_id", valueNumeric = 4000000000000000045)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
      }
      // 订单 46:陈晓东 3×18.9L+1×抽水泵 企业价(COMPLETED,陈永强)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000046)
        column(name = "order_no", value = "NS00000046")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 9400)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000008)
        column(name = "status", value = "COMPLETED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0046")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '15 minutes')",
        )
        column(
          name = "delivery_confirmed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '45 minutes')",
        )
        column(
          name = "completed_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '55 minutes')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(
          name = "search_content",
          value = "莫干山路132号 耀江国际大厦1楼餐厅 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水 南山牌 电动抽水泵",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '55 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000055)
        column(name = "order_id", valueNumeric = 4000000000000000046)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 3)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 6900)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000056)
        column(name = "order_id", valueNumeric = 4000000000000000046)
        column(name = "product_id", valueNumeric = 3000000000000000009)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000009,\"name\":\"南山牌 电动抽水泵\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      // 订单 47:赵强 2×18.9L(配送中,陈永强,带轨迹)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000047)
        column(name = "order_no", value = "NS00000047")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "DELIVERING")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0047")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hours')",
        )
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000057)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      // 订单 48:李秀英 1×18.9L(配送中,刘志刚,带轨迹)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000048)
        column(name = "order_no", value = "NS00000048")
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000003)
        column(name = "status", value = "DELIVERING")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0048")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(
          name = "delivery_started_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '30 minutes')",
        )
        column(name = "delivery_time_slot", value = "AFTERNOON")
        column(
          name = "search_content",
          value = "教工路223号 文教社区4幢1单元302室 李秀英 13800138002 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(
          name = "updated_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '30 minutes')",
        )
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000058)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 订单 49:吴刚 2×18.9L 企业价(待配送,已派单刘志刚)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000049)
        column(name = "order_no", value = "NS00000049")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 4600)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "PENDING_DELIVERY")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0049")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000059)
        column(name = "order_id", valueNumeric = 4000000000000000049)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // 订单 50:周丽娟 1×5L 便携装 秒杀价(待配送,已派单黄海涛)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000050)
        column(name = "order_no", value = "NS00000050")
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "total_amount_cents", valueNumeric = 990)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 990)
        column(name = "price_source", value = "FLASH_SALE_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000012)
        column(name = "status", value = "PENDING_DELIVERY")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0050")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 hours')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "5L便携装 9.9元限时特价")
        column(name = "search_content", value = "古翠路60号 康新花园2幢1801室 周丽娟 13800138008 南山牌 天然山泉水 5L 便携装")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 hours')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 hours')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000060)
        column(name = "order_id", valueNumeric = 4000000000000000050)
        column(name = "product_id", valueNumeric = 3000000000000000003)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 990)
        column(name = "total_price_cents", valueNumeric = 990)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000003,\"name\":\"南山牌 天然山泉水 5L 便携装\",\"price_cents\":1200,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 hours')")
      }
      // 订单 51:张伟 1×18.9L(待分配,已支付)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000051)
        column(name = "order_no", value = "NS00000051")
        column(name = "user_id", valueNumeric = 1000000000000000003)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000005)
        column(name = "status", value = "PENDING_DISPATCH")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0051")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 hours')")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "文三路138号 东部软件园8号楼1203室 张伟 13800138003 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 hours')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 hours')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000061)
        column(name = "order_id", valueNumeric = 4000000000000000051)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 hours')")
      }
      // 订单 52:孙志远 2×18.9L(待分配,已支付)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000052)
        column(name = "order_no", value = "NS00000052")
        column(name = "user_id", valueNumeric = 1000000000000000007)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000011)
        column(name = "status", value = "PENDING_DISPATCH")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0052")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 hours')")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "文一西路969号 西溪银泰城A座写字楼15楼 孙志远 13800138007 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 hours')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 hours')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000062)
        column(name = "order_id", valueNumeric = 4000000000000000052)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 hours')")
      }
      // 订单 53:高敏 1×18.9L(待支付)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000053)
        column(name = "order_no", value = "NS00000053")
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000017)
        column(name = "status", value = "PENDING_PAYMENT")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "等待用户支付")
        column(
          name = "search_content",
          value = "莲花街109号 莲花新村1幢2单元201室 高敏 13800138012 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 hours')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 hours')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000063)
        column(name = "order_id", valueNumeric = 4000000000000000053)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 hours')")
      }
      // 订单 54:林芳 1×18.9L(待支付)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000054)
        column(name = "order_no", value = "NS00000054")
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000009)
        column(name = "status", value = "PENDING_PAYMENT")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(
          name = "search_content",
          value = "西溪路550号 西溪花园翠竹苑12幢3单元502室 林芳 13800138005 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 hours')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 hours')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000064)
        column(name = "order_id", valueNumeric = 4000000000000000054)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 hours')")
      }
      // 订单 55:陈晓东 2×18.9L 企业价(已取消)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000055)
        column(name = "order_no", value = "NS00000055")
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "total_amount_cents", valueNumeric = 4600)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000006)
        column(name = "status", value = "CANCELLED")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "用户下单后取消")
        column(
          name = "search_content",
          value = "学院路77号 黄龙万科中心美食街16号铺 陈晓东 13800138004 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000065)
        column(name = "order_id", valueNumeric = 4000000000000000055)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      // 订单 56:王建国 1×18.9L(超时未支付取消)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000056)
        column(name = "order_no", value = "NS00000056")
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "total_amount_cents", valueNumeric = 2500)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000002)
        column(name = "status", value = "CANCELLED")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "delivery_time_slot", value = "IMMEDIATE")
        column(name = "remark", value = "超时未支付,系统自动取消")
        column(
          name = "search_content",
          value = "文一西路588号 西溪诚园2期5幢1201室 王建国 13800138001 南山牌 天然山泉水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000066)
        column(name = "order_id", valueNumeric = 4000000000000000056)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 2500)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      // 订单 57:赵强 2×18.9L(已取消)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000057)
        column(name = "order_no", value = "NS00000057")
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "total_amount_cents", valueNumeric = 5000)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000010)
        column(name = "status", value = "CANCELLED")
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "用户下单后取消")
        column(name = "search_content", value = "紫荆花路2号 世纪联华超市旁奶茶店 赵强 13800138006 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000067)
        column(name = "order_id", valueNumeric = 4000000000000000057)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2500)
        column(name = "total_price_cents", valueNumeric = 5000)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '10 days')")
      }
      // 订单 58:李秀英 1×纯净水(已退款)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000058)
        column(name = "order_no", value = "NS00000058")
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "total_amount_cents", valueNumeric = 1800)
        column(name = "delivery_fee_cents", valueNumeric = 0)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "price_source", value = "CATALOG_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000003)
        column(name = "status", value = "REFUNDED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0058")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "用户退款:水质不满意")
        column(
          name = "search_content",
          value = "教工路223号 文教社区4幢1单元302室 李秀英 13800138002 南山牌 饮用纯净水 18.9L 桶装水",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000068)
        column(name = "order_id", valueNumeric = 4000000000000000058)
        column(name = "product_id", valueNumeric = 3000000000000000005)
        column(name = "quantity", valueNumeric = 1)
        column(name = "unit_price_cents", valueNumeric = 1800)
        column(name = "total_price_cents", valueNumeric = 1800)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000005,\"name\":\"南山牌 饮用纯净水 18.9L 桶装水\",\"price_cents\":1800,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
      }
      // 订单 59:吴刚 2×18.9L 企业价(已退款)
      insert(tableName = "orders") {
        column(name = "id", valueNumeric = 4000000000000000059)
        column(name = "order_no", value = "NS00000059")
        column(name = "user_id", valueNumeric = 1000000000000000009)
        column(name = "total_amount_cents", valueNumeric = 4600)
        column(name = "delivery_fee_cents", valueNumeric = 200)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "price_source", value = "GROUP_PRICE")
        column(name = "ticket_redeem_count", valueNumeric = 0)
        column(name = "address_id", valueNumeric = 2000000000000000013)
        column(name = "status", value = "REFUNDED")
        column(name = "payment_method", value = "WECHAT_PAY")
        column(name = "payment_transaction_id", value = "WXTXN-NS-0059")
        column(name = "paid_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "is_self_collect", valueBoolean = false)
        column(name = "payment_type", value = "QR_CODE")
        column(name = "delivery_time_slot", value = "MORNING")
        column(name = "remark", value = "退款:送错地址")
        column(name = "search_content", value = "黄龙路1号 黄龙世纪广场B座9楼 吴刚 13800138009 南山牌 天然山泉水 18.9L 桶装水")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      insert(tableName = "order_items") {
        column(name = "id", valueNumeric = 5000000000000000069)
        column(name = "order_id", valueNumeric = 4000000000000000059)
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "quantity", valueNumeric = 2)
        column(name = "unit_price_cents", valueNumeric = 2300)
        column(name = "total_price_cents", valueNumeric = 4600)
        column(
          name = "product_snapshot",
          value = "{\"id\":3000000000000000001,\"name\":\"南山牌 天然山泉水 18.9L 桶装水\",\"price_cents\":2500,\"thumbnail_url\":\"/static/default-product.jpeg\",\"status\":\"ONLINE\"}",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      // ==================== 订单赠品行(买3送1活动触发) ====================
      insert(tableName = "order_gifts") {
        column(name = "id", valueNumeric = 8100000000000000001)
        column(name = "order_id", valueNumeric = 4000000000000000011)
        column(name = "gift_activity_id", valueNumeric = 7500000000000000001)
        column(name = "gift_activity_name", value = "买3桶送1桶")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "product_name", value = "南山牌 天然山泉水 18.9L 桶装水")
        column(name = "quantity", valueNumeric = 1)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
      }
      insert(tableName = "order_gifts") {
        column(name = "id", valueNumeric = 8100000000000000002)
        column(name = "order_id", valueNumeric = 4000000000000000043)
        column(name = "gift_activity_id", valueNumeric = 7500000000000000001)
        column(name = "gift_activity_name", value = "买3桶送1桶")
        column(name = "product_id", valueNumeric = 3000000000000000001)
        column(name = "product_name", value = "南山牌 天然山泉水 18.9L 桶装水")
        column(name = "quantity", valueNumeric = 1)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      // ==================== 订单评价(已完成订单) ====================
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000001)
        column(name = "order_id", valueNumeric = 4000000000000000001)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "师傅准时送到,还帮忙把空桶带走了,服务很好!")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '29 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000002)
        column(name = "order_id", valueNumeric = 4000000000000000002)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "水很甘甜,家里人都说好喝,下次还订这家")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '28 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000003)
        column(name = "order_id", valueNumeric = 4000000000000000003)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "餐厅用水量大,一直订南山,送货从不耽误营业")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '27 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000004)
        column(name = "order_id", valueNumeric = 4000000000000000009)
        column(name = "user_id", valueNumeric = 1000000000000000008)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "rating", valueNumeric = 4)
        column(name = "comment", value = "水不错,就是昨天送得稍微有点晚,师傅态度很好")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '23 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000005)
        column(name = "order_id", valueNumeric = 4000000000000000011)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "买3送1很划算,赠的水也一起送到了")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '22 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000006)
        column(name = "order_id", valueNumeric = 4000000000000000016)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "小区老客户了,送水师傅都认识,服务贴心")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '20 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000007)
        column(name = "order_id", valueNumeric = 4000000000000000017)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "一口气送10桶,师傅辛苦了,满减活动很实惠")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '19 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000008)
        column(name = "order_id", valueNumeric = 4000000000000000020)
        column(name = "user_id", valueNumeric = 1000000000000000004)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "饮水机安装很快,师傅还教了使用方法,很好")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '18 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000009)
        column(name = "order_id", valueNumeric = 4000000000000000021)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "水质干净,桶装水换水也很方便")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '17 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000010)
        column(name = "order_id", valueNumeric = 4000000000000000027)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "纯净水煮饭泡茶都不错,价格实惠")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '14 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000031)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 4)
        column(name = "comment", value = "水很好喝,就是桶盖有点紧,其他都满意")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '12 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000037)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "补桶盖很及时,服务周到")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '9 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000013)
        column(name = "order_id", valueNumeric = 4000000000000000040)
        column(name = "user_id", valueNumeric = 1000000000000000012)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "第一次订南山,送水很快,水质不错")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '6 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000014)
        column(name = "order_id", valueNumeric = 4000000000000000043)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "买3送1又囤了一批,生意兴隆的秘诀之一")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '4 days')")
      }
      insert(tableName = "reviews") {
        column(name = "id", valueNumeric = 6000000000000000015)
        column(name = "order_id", valueNumeric = 4000000000000000045)
        column(name = "user_id", valueNumeric = 1000000000000000001)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "rating", valueNumeric = 5)
        column(name = "comment", value = "用券下单很划算,师傅送货上门还帮搬进厨房")
        column(name = "is_anonymous", valueBoolean = false)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      // ==================== 配送轨迹(配送中订单) ====================
      // 订单 47:陈永强沿紫荆花路配送
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000001)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "longitude", valueNumeric = BigDecimal("120.1210"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2880"))
        column(name = "accuracy", valueNumeric = 8)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "longitude", valueNumeric = BigDecimal("120.1188"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2905"))
        column(name = "accuracy", valueNumeric = 10)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '5 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '5 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000003)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "longitude", valueNumeric = BigDecimal("120.1164"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2912"))
        column(name = "accuracy", valueNumeric = 9)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '10 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '10 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000004)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "longitude", valueNumeric = BigDecimal("120.1149"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2918"))
        column(name = "accuracy", valueNumeric = 11)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '15 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '15 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000005)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "order_id", valueNumeric = 4000000000000000047)
        column(name = "longitude", valueNumeric = BigDecimal("120.1143"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2920"))
        column(name = "accuracy", valueNumeric = 8)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '20 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours' + INTERVAL '20 minutes')",
        )
      }
      // 订单 48:刘志刚沿教工路配送
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "longitude", valueNumeric = BigDecimal("120.1500"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2860"))
        column(name = "accuracy", valueNumeric = 9)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000007)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "longitude", valueNumeric = BigDecimal("120.1472"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2851"))
        column(name = "accuracy", valueNumeric = 10)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '5 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '5 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000008)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "longitude", valueNumeric = BigDecimal("120.1450"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2844"))
        column(name = "accuracy", valueNumeric = 8)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '10 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '10 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000009)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "longitude", valueNumeric = BigDecimal("120.1436"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2841"))
        column(name = "accuracy", valueNumeric = 9)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '15 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '15 minutes')",
        )
      }
      insert(tableName = "delivery_trajectories") {
        column(name = "id", valueNumeric = 8200000000000000010)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "order_id", valueNumeric = 4000000000000000048)
        column(name = "longitude", valueNumeric = BigDecimal("120.1426"))
        column(name = "latitude", valueNumeric = BigDecimal("30.2839"))
        column(name = "accuracy", valueNumeric = 8)
        column(
          name = "reported_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '20 minutes')",
        )
        column(
          name = "created_at",
          valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days' + INTERVAL '1 hours' + INTERVAL '20 minutes')",
        )
      }
    }
    changeSet(author = "aqua-rush", id = "1.7.0-seed-nanshan-ops") {
      comment(
        "Seed 南山牌 demo ops configs: store profile, delivery fee rule, banners, notices, feedbacks, daily collections & reconciliations (PRD 12-operations-config.md / 04-order.md)",
      )
      // ==================== 店铺信息(F-OPS-004,单行 id=1) ====================
      insert(tableName = "store_profiles") {
        column(name = "id", valueNumeric = 1)
        column(name = "shop_name", value = "南山送水站")
        column(name = "logo_url", value = "/static/default-product.jpeg")
        column(name = "service_hotline", value = "0571-86543210")
        column(name = "business_hours_start", value = "08:00")
        column(name = "business_hours_end", value = "20:00")
        column(name = "shop_intro", value = "杭州西湖区桶装水配送,南山牌天然山泉水厂家直供")
        column(
          name = "about_us",
          value = "南山送水站深耕杭州桶装水配送二十余年,水源来自临安天目山脉深层山泉,坚持当日下单当日送达,覆盖西湖区全境,为超 3000 户家庭与 200 余家企事业单位提供优质饮用水服务。",
        )
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1000 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
      }
      // ==================== 配送费规则(F-OPS-005,单行 id=1) ====================
      insert(tableName = "delivery_fee_rules") {
        column(name = "id", valueNumeric = 1)
        column(name = "free_floor_limit", valueNumeric = 4)
        column(name = "surcharge_cents", valueNumeric = 200)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '400 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '30 days')")
      }
      // ==================== 轮播图(F-OPS-001) ====================
      insert(tableName = "banners") {
        column(name = "id", valueNumeric = 8000000000000000001)
        column(name = "image_url", value = "/static/default-product.jpeg")
        column(name = "title", value = "新品上市 5L便携装")
        column(name = "subtitle", value = "出行露营 即开即饮")
        column(name = "link_type", value = "PRODUCT")
        column(name = "link_target", value = "3000000000000000003")
        column(name = "sort_order", valueNumeric = 1)
        column(name = "enabled", valueBoolean = true)
        column(name = "start_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "end_at", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '25 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
      }
      insert(tableName = "banners") {
        column(name = "id", valueNumeric = 8000000000000000002)
        column(name = "image_url", value = "/static/default-product.jpeg")
        column(name = "title", value = "夏日特惠 买3桶送1桶")
        column(name = "subtitle", value = "活动期间下单即赠")
        column(name = "link_type", value = "CUSTOM_PAGE")
        column(name = "link_target", value = "/pages/order-confirm/index")
        column(name = "sort_order", valueNumeric = 2)
        column(name = "enabled", valueBoolean = true)
        column(name = "start_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "end_at", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '30 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '8 days')")
      }
      insert(tableName = "banners") {
        column(name = "id", valueNumeric = 8000000000000000003)
        column(name = "image_url", value = "/static/default-product.jpeg")
        column(name = "title", value = "水票充值 多买多送")
        column(name = "subtitle", value = "10张装水票限时优惠")
        column(name = "link_type", value = "CUSTOM_PAGE")
        column(name = "link_target", value = "/pages/ticket-shop/index")
        column(name = "sort_order", valueNumeric = 3)
        column(name = "enabled", valueBoolean = true)
        column(name = "start_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "end_at", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '45 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '15 days')")
      }
      // ==================== 公告(F-OPS-002) ====================
      insert(tableName = "notices") {
        column(name = "id", valueNumeric = 8000000000000000011)
        column(name = "title", value = "停水通知")
        column(
          name = "content",
          value = "因杭州水务集团管网改造,本周六 09:00-17:00 西湖区文三路/教工路沿线片区将停水,请提前备水。送水站将优先保障该片区配送,如有不便敬请谅解。",
        )
        column(name = "type", value = "WATER_OUTAGE")
        column(name = "pinned", valueBoolean = true)
        column(name = "start_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "end_at", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '5 days')")
        column(name = "status", value = "PUBLISHED")
        column(name = "published_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "notices") {
        column(name = "id", valueNumeric = 8000000000000000012)
        column(name = "title", value = "活动预告:买水票送水票")
        column(name = "content", value = "水票充值活动将于下周一开启:购买 18.9L 桶装水水票 10 张装即送 1 张,多买多送,老客户专享,敬请关注。")
        column(name = "type", value = "ACTIVITY_PREVIEW")
        column(name = "pinned", valueBoolean = false)
        column(name = "start_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "end_at", valueComputed = "(CURRENT_TIMESTAMP + INTERVAL '7 days')")
        column(name = "status", value = "PUBLISHED")
        column(name = "published_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "notices") {
        column(name = "id", valueNumeric = 8000000000000000013)
        column(name = "title", value = "节假日配送安排(草稿)")
        column(name = "content", value = "端午节当天配送时间调整为 09:00-18:00,请提前一天下单,感谢理解。")
        column(name = "type", value = "PLATFORM_NOTICE")
        column(name = "pinned", valueBoolean = false)
        column(name = "status", value = "DRAFT")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 意见反馈(F-OPS-003) ====================
      insert(tableName = "feedbacks") {
        column(name = "id", valueNumeric = 8000000000000000021)
        column(name = "user_id", valueNumeric = 1000000000000000002)
        column(name = "feedback_type", value = "DELIVERY_SERVICE")
        column(name = "content", value = "今天送的桶装水放在门口没提前打电话,天气热水放在楼道怕影响水质,希望下次送货前先电话确认。")
        column(name = "contact_info", value = "13800138002")
        column(name = "status", value = "PENDING")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "feedbacks") {
        column(name = "id", valueNumeric = 8000000000000000022)
        column(name = "user_id", valueNumeric = 1000000000000000006)
        column(name = "feedback_type", value = "SUGGESTION")
        column(name = "content", value = "奶茶店每周固定要 6 桶水,希望能支持「定时配送」功能,设置每周一自动下单,省得每次手动操作。")
        column(name = "contact_info", value = "13800138006")
        column(name = "status", value = "RESOLVED")
        column(name = "handle_result", value = "已收到建议,定时配送已列入下季度产品规划,当前可联系客服电话下单约定固定配送周期。")
        column(name = "handled_by_id", valueNumeric = 2)
        column(name = "handled_by_name", value = "系统管理员")
        column(name = "handled_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '5 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "feedbacks") {
        column(name = "id", valueNumeric = 8000000000000000023)
        column(name = "user_id", valueNumeric = 1000000000000000005)
        column(name = "feedback_type", value = "PRODUCT_QUALITY")
        column(name = "content", value = "这批次水桶的桶盖密封圈有点松,换水时留意到,其他都正常。")
        column(name = "contact_info", value = "13800138005")
        column(name = "status", value = "PENDING")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      // ==================== 每日收款记录(近 3 天) ====================
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000001)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '1 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 12500)
        column(name = "water_ticket_count", valueNumeric = 2)
        column(name = "qr_code_amount_cents", valueNumeric = 6800)
        column(name = "order_count", valueNumeric = 8)
        column(name = "order_amount_cents", valueNumeric = 19800)
        column(name = "calculated_cash_amount_cents", valueNumeric = 12500)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 6800)
        column(name = "calculated_water_ticket_count", valueNumeric = 2)
        column(name = "calculated_order_amount_cents", valueNumeric = 19800)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "CONFIRMED")
        column(name = "confirmed_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "confirmed_by", valueNumeric = 2)
        column(name = "notes", value = "无差异")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000002)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '1 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 9800)
        column(name = "water_ticket_count", valueNumeric = 1)
        column(name = "qr_code_amount_cents", valueNumeric = 5200)
        column(name = "order_count", valueNumeric = 6)
        column(name = "order_amount_cents", valueNumeric = 15000)
        column(name = "calculated_cash_amount_cents", valueNumeric = 9800)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 5200)
        column(name = "calculated_water_ticket_count", valueNumeric = 1)
        column(name = "calculated_order_amount_cents", valueNumeric = 15000)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "CONFIRMED")
        column(name = "confirmed_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "confirmed_by", valueNumeric = 2)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000003)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '1 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 3200)
        column(name = "water_ticket_count", valueNumeric = 0)
        column(name = "qr_code_amount_cents", valueNumeric = 4600)
        column(name = "order_count", valueNumeric = 4)
        column(name = "order_amount_cents", valueNumeric = 7800)
        column(name = "calculated_cash_amount_cents", valueNumeric = 3200)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 4600)
        column(name = "calculated_water_ticket_count", valueNumeric = 0)
        column(name = "calculated_order_amount_cents", valueNumeric = 7800)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "SUBMITTED")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000004)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000011)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '2 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 15200)
        column(name = "water_ticket_count", valueNumeric = 3)
        column(name = "qr_code_amount_cents", valueNumeric = 8600)
        column(name = "order_count", valueNumeric = 10)
        column(name = "order_amount_cents", valueNumeric = 23800)
        column(name = "calculated_cash_amount_cents", valueNumeric = 15200)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 8600)
        column(name = "calculated_water_ticket_count", valueNumeric = 3)
        column(name = "calculated_order_amount_cents", valueNumeric = 23800)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "CONFIRMED")
        column(name = "confirmed_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "confirmed_by", valueNumeric = 2)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000005)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000012)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '2 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 7600)
        column(name = "water_ticket_count", valueNumeric = 2)
        column(name = "qr_code_amount_cents", valueNumeric = 4100)
        column(name = "order_count", valueNumeric = 5)
        column(name = "order_amount_cents", valueNumeric = 11700)
        column(name = "calculated_cash_amount_cents", valueNumeric = 7600)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 4100)
        column(name = "calculated_water_ticket_count", valueNumeric = 2)
        column(name = "calculated_order_amount_cents", valueNumeric = 11700)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "CONFIRMED")
        column(name = "confirmed_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "confirmed_by", valueNumeric = 2)
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
      insert(tableName = "daily_collection_records") {
        column(name = "id", valueNumeric = 8600000000000000006)
        column(name = "delivery_worker_id", valueNumeric = 9000000000000000013)
        column(name = "collection_date", valueComputed = "(CURRENT_DATE - INTERVAL '3 days')::date")
        column(name = "cash_amount_cents", valueNumeric = 0)
        column(name = "water_ticket_count", valueNumeric = 0)
        column(name = "qr_code_amount_cents", valueNumeric = 5200)
        column(name = "order_count", valueNumeric = 3)
        column(name = "order_amount_cents", valueNumeric = 5200)
        column(name = "calculated_cash_amount_cents", valueNumeric = 0)
        column(name = "calculated_qr_code_amount_cents", valueNumeric = 5200)
        column(name = "calculated_water_ticket_count", valueNumeric = 0)
        column(name = "calculated_order_amount_cents", valueNumeric = 5200)
        column(name = "difference_amount_cents", valueNumeric = 0)
        column(name = "status", value = "DRAFT")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '3 days')")
      }
      // ==================== 每日对账记录(近 2 天) ====================
      insert(tableName = "daily_reconciliations") {
        column(name = "id", valueNumeric = 8700000000000000001)
        column(name = "reconciliation_date", valueComputed = "(CURRENT_DATE - INTERVAL '1 days')::date")
        column(name = "total_cash_amount_cents", valueNumeric = 25500)
        column(name = "total_water_ticket_count", valueNumeric = 3)
        column(name = "total_qr_code_amount_cents", valueNumeric = 16600)
        column(name = "total_order_count", valueNumeric = 18)
        column(name = "total_order_amount_cents", valueNumeric = 42600)
        column(name = "total_collection_amount_cents", valueNumeric = 42100)
        column(name = "discrepancy_amount_cents", valueNumeric = 500)
        column(name = "status", value = "MATCHED")
        column(name = "review_notes", value = "差异在容差范围内")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '1 days')")
      }
      insert(tableName = "daily_reconciliations") {
        column(name = "id", valueNumeric = 8700000000000000002)
        column(name = "reconciliation_date", valueComputed = "(CURRENT_DATE - INTERVAL '2 days')::date")
        column(name = "total_cash_amount_cents", valueNumeric = 22800)
        column(name = "total_water_ticket_count", valueNumeric = 5)
        column(name = "total_qr_code_amount_cents", valueNumeric = 12700)
        column(name = "total_order_count", valueNumeric = 15)
        column(name = "total_order_amount_cents", valueNumeric = 35500)
        column(name = "total_collection_amount_cents", valueNumeric = 35500)
        column(name = "discrepancy_amount_cents", valueNumeric = 0)
        column(name = "status", value = "REVIEWED")
        column(name = "reviewed_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "reviewed_by", valueNumeric = 2)
        column(name = "review_notes", value = "人工复核通过")
        column(name = "created_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
        column(name = "updated_at", valueComputed = "(CURRENT_TIMESTAMP - INTERVAL '2 days')")
      }
    }
  })
