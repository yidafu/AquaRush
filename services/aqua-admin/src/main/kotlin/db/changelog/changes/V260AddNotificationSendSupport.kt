// 转换自 changes/v2.6.0-add-notification-send-support.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V260AddNotificationSendSupport :
  KotlinCompiledDatabaseChangeLog({
    // 消息发送补齐(PRD 13-notification.md):
    //        1) aqua_notifications 持久化微信订阅消息模板上下文(template_scene/template_page/template_fields),
    //           支撑失败消息手动重发(F-NTF-006 开放问题关闭);
    //        2) 新增系统参数:优惠券即将过期提醒窗口(coupon_expiring_notify_days)、配送超时阈值(delivery_timeout_minutes)。
    changeSet(author = "aqua-rush", id = "2.6.0-add-notification-send-support") {
      comment("Notification send support params: coupon-expiring / delivery-timeout (模板上下文列已并入 v1.5.0 建表)")
      // ==================== 优惠券即将过期提醒窗口(默认 3 天) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "45")
        column(name = "setting_key", value = "coupon_expiring_notify_days")
        column(name = "setting_value", value = "3")
        column(name = "description", value = "优惠券即将过期提醒窗口(天),每日调度扫描该窗口内到期的已领取未使用优惠券并站内提醒用户")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "3")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "30")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      // ==================== 配送超时阈值(默认 120 分钟,触发送水工超时推送) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "46")
        column(name = "setting_key", value = "delivery_timeout_minutes")
        column(name = "setting_value", value = "120")
        column(name = "description", value = "配送超时阈值(分钟),配送中订单超过该时长未送达时向送水工推送超时提醒")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "ORDER_AND_FULFILLMENT")
        column(name = "default_value", value = "120")
        column(name = "min_value", value = "10")
        column(name = "max_value", value = "1440")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
