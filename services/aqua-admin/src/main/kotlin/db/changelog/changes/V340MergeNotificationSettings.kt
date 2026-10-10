// B1b 缺陷修复(裁决清单 §2.7):通知设置双表合一。
// notification_settings(V310,写方 aqua-user 用户创建默认行)与 user_notification_settings
// (V100,读方 aqua-platform 订阅消息门控)语义重复、写读分家——默认行写了没人读,读方恒按全开降级。
// 按设计 01 分册 §1.6 与 PRD 13-notification.md 命名保留 user_notification_settings,
// 迁移存量数据后删除 notification_settings;新写入口为 NotificationMutationApiService.createDefaultUserSettings。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V340MergeNotificationSettings :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.0-merge-notification-settings") {
      comment(
        "Merge active rows from notification_settings into user_notification_settings, then drop the duplicate table",
      )
      // 迁数据:user_id 唯一(user_notification_settings.user_id UNIQUE),已存在则以读方表为准;
      // id 不迁移,由目标表序列生成,避免两侧 id 空间冲突。
      sql(
        sql = """
        INSERT INTO user_notification_settings
          (user_id, order_updates, payment_notifications, delivery_notifications,
           promotional_notifications, is_deleted, deleted_at, deleted_by, created_at, updated_at)
        SELECT ns.user_id, ns.order_updates, ns.payment_notifications, ns.delivery_notifications,
               ns.promotional_notifications, ns.is_deleted, ns.deleted_at, ns.deleted_by,
               ns.created_at, ns.updated_at
        FROM notification_settings ns
        WHERE ns.is_deleted = false
          AND NOT EXISTS (
            SELECT 1 FROM user_notification_settings uns WHERE uns.user_id = ns.user_id
          );
        """,
      )
      sql(sql = "DROP TABLE IF EXISTS notification_settings")
    }
  })
