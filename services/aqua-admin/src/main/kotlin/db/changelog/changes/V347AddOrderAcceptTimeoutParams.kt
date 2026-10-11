// 转换自 changes/v3.4.7-add-order-accept-timeout-params.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V347AddOrderAcceptTimeoutParams :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.7-add-order-accept-timeout-params") {
      comment(
        "G94 接单超时预警(F-NTF-008/R-NTF-07): two accept/delivery-start timeout threshold params in F-SYS-002 ORDER_AND_FULFILLMENT group",
      )
      // ==================== 订单与履约参数组(F-SYS-002)·接单超时预警阈值(G94) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "47")
        column(name = "setting_key", value = "order_accept_timeout_minutes")
        column(name = "setting_value", value = "30")
        column(name = "description", value = "接单超时预警阈值(分钟),「待派单」超阈值无人接单预警管理员(R-NTF-07/F-NTF-008)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "30")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "48")
        column(name = "setting_key", value = "delivery_start_timeout_minutes")
        column(name = "setting_value", value = "60")
        column(name = "description", value = "开始配送超时预警阈值(分钟),「待配送」超阈值未开始配送预警管理员(R-NTF-07/F-NTF-008)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "60")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
