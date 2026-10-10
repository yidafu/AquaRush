// Outbox 替代 Artemis MQ(docs/compose/spec/outbox-messaging.md):重建 V100 遗留的 outbox_events。
// 旧表从未被读写(Artemis 切换后搁置),直接 DROP 重建为点对点/广播双语义事件表。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V320RecreateOutboxEvents :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.2.0-recreate-outbox-events") {
      comment("Recreate outbox_events for transactional outbox (replaces Artemis MQ)")
      sql("DROP TABLE IF EXISTS outbox_events")
      createTable(tableName = "outbox_events") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "event_type", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "destination", type = "varchar(50)") {
          constraints(nullable = false)
        }
        // NULL = 竞争消费(任一服务认领);非空 = 指定服务独享(广播扇出行)
        column(name = "subscriber", type = "varchar(20)")
        column(name = "payload", type = "text") {
          constraints(nullable = false)
        }
        // 可认领时刻;延迟投递写未来时间(取代 Artemis _AMQ_SCHED_DELIVERY)
        column(name = "next_run_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)", defaultValue = "PENDING") {
          constraints(nullable = false)
        }
        column(name = "retry_count", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "error_message", type = "text")
        column(name = "locked_by", type = "varchar(64)")
        column(name = "locked_at", type = "timestamp")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "outbox_events", indexName = "idx_outbox_status_next_run") {
        column(name = "status")
        column(name = "next_run_at")
      }
      createIndex(tableName = "outbox_events", indexName = "idx_outbox_destination") {
        column(name = "destination")
      }
      createIndex(tableName = "outbox_events", indexName = "idx_outbox_subscriber_status") {
        column(name = "subscriber")
        column(name = "status")
      }
    }
  })
