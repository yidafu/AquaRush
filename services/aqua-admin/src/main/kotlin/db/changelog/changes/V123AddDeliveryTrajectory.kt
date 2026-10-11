// 转换自 changes/v1.2.3-add-delivery-trajectory.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V123AddDeliveryTrajectory :
  KotlinCompiledDatabaseChangeLog({
    // 配送轨迹表(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03,G22 落地):
    //     送水工在订单「配送中」期间按设定频率(默认 5s)上报的实时位置点,
    //     delivery_worker_id 即账号 id(送水员档案并入 aqua_admins 单表,迁移 v1.2.1)。
    changeSet(author = "aqua-rush", id = "1.2.3-add-delivery-trajectory") {
      comment("Add aqua_delivery_trajectories table for real-time trajectory reporting")
      createTable(tableName = "aqua_delivery_trajectories") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "delivery_worker_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "longitude", type = "double") {
          constraints(nullable = false)
        }
        column(name = "latitude", type = "double") {
          constraints(nullable = false)
        }
        column(name = "accuracy", type = "double")
        column(name = "reported_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_delivery_trajectories", indexName = "idx_trajectory_worker_order") {
        column(name = "delivery_worker_id")
        column(name = "order_id")
        column(name = "reported_at")
      }
      createIndex(tableName = "aqua_delivery_trajectories", indexName = "idx_trajectory_worker_reported") {
        column(name = "delivery_worker_id")
        column(name = "reported_at")
      }
      createIndex(tableName = "aqua_delivery_trajectories", indexName = "idx_trajectory_order") {
        column(name = "order_id")
      }
    }
  })
