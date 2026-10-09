// 转换自 changes/v1.0.8-add-system-config-params.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
// 存量漂移修复:冗余 addColumn(已被就地并入 v1.0.0)已移除,原因:system_settings 配置参数列已被就地并入 v1.0.0 建表
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V108AddSystemConfigParams :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.8-add-system-config-params") {
      comment("Extend system_settings with type/group/default columns and seed system config params for F-SYS-001/F-SYS-002/F-SYS-003")
      // 孤儿种子清理(G06):bucket_deposit_amount 随押桶功能移除成为孤儿数据,须先于 NOT NULL addColumn 删除
      delete(tableName = "system_settings") {
        where("setting_key = 'bucket_deposit_amount'")
      }
      // ==================== 订单与履约参数组(F-SYS-002) ====================
      insert(tableName = "system_settings") {
        column(name = "id", value = "1")
        column(name = "setting_key", value = "payment_timeout_minutes")
        column(name = "setting_value", value = "30")
        column(name = "description", value = "支付超时(分钟),超时自动取消订单并恢复库存(R-ORD-04)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "30")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "2")
        column(name = "setting_key", value = "delivery_slot_immediate_minutes")
        column(name = "setting_value", value = "30")
        column(name = "description", value = "立即配送时限(分钟)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "30")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "3")
        column(name = "setting_key", value = "delivery_slot_morning_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "上午配送时段启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "4")
        column(name = "setting_key", value = "delivery_slot_morning_start")
        column(name = "setting_value", value = "09:00")
        column(name = "description", value = "上午配送时段开始时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "09:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "5")
        column(name = "setting_key", value = "delivery_slot_morning_end")
        column(name = "setting_value", value = "12:00")
        column(name = "description", value = "上午配送时段结束时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "12:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "6")
        column(name = "setting_key", value = "delivery_slot_afternoon_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "下午配送时段启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "7")
        column(name = "setting_key", value = "delivery_slot_afternoon_start")
        column(name = "setting_value", value = "14:00")
        column(name = "description", value = "下午配送时段开始时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "14:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "8")
        column(name = "setting_key", value = "delivery_slot_afternoon_end")
        column(name = "setting_value", value = "18:00")
        column(name = "description", value = "下午配送时段结束时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "18:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "9")
        column(name = "setting_key", value = "delivery_slot_evening_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "晚上配送时段启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "10")
        column(name = "setting_key", value = "delivery_slot_evening_start")
        column(name = "setting_value", value = "18:00")
        column(name = "description", value = "晚上配送时段开始时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "18:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "11")
        column(name = "setting_key", value = "delivery_slot_evening_end")
        column(name = "setting_value", value = "21:00")
        column(name = "description", value = "晚上配送时段结束时间")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "21:00")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "12")
        column(name = "setting_key", value = "dispatch_timeout_reminder_minutes")
        column(name = "setting_value", value = "15")
        column(name = "description", value = "派单超时提醒(分钟),无 PRD 默认值,暂定 15(开放问题,见 PRD F-SYS-002)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "15")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "13")
        column(name = "setting_key", value = "auto_confirm_receipt_days")
        column(name = "setting_value", value = "7")
        column(name = "description", value = "自动确认收货(天),无 PRD 默认值,暂定 7(开放问题,见 PRD F-SYS-002)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "7")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      // ==================== 库存预警参数组(F-SYS-003,R-PRO-02) ====================
      insert(tableName = "system_settings") {
        column(name = "id", value = "14")
        column(name = "setting_key", value = "low_stock_threshold")
        column(name = "setting_value", value = "10")
        column(name = "description", value = "低库存阈值(件),库存低于该值显示红色预警(F-PRO-006/F-STA-006)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "INVENTORY")
        column(name = "default_value", value = "10")
        column(name = "min_value", value = "0")
        column(name = "max_value", value = "99999")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "system_settings") {
        column(name = "id", value = "15")
        column(name = "setting_key", value = "stock_warning_tier_mid")
        column(name = "setting_value", value = "50")
        column(name = "description", value = "中档预警阈值(件),库存低于该值显示橙色预警;须大于低库存阈值")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "INVENTORY")
        column(name = "default_value", value = "50")
        column(name = "min_value", value = "0")
        column(name = "max_value", value = "99999")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
