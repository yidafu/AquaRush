// 转换自 changes/v1.2.1-merge-delivery-worker-into-admins.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V121MergeDeliveryWorkerIntoAdmins :
  KotlinCompiledDatabaseChangeLog({
    // 送水员档案并入 admin 账号表单表(spec/prd/07-user.md §F-USR-021 业务规则扩充):
    //     删除 delivery_workers 档案表,配送业务字段并入 admins 单表(role=DELIVERY_WORKER 区分身份);
    //     orders/reviews/delivery_worker_statistics/daily_collection_records 的 delivery_worker_id
    //     由档案 id 数据值迁移为账号 id;删除 admins.delivery_worker_id 冗余关联列;
    //     存量 delivery_{档案ID} 用户名归一为 delivery_{账号ID}。
    changeSet(author = "aqua-rush", id = "1.2.1-merge-delivery-worker-into-admins") {
      // 档案表已不存在(已合并)时跳过
      preConditions(onFail = "CONTINUE", onErrorMessage = "delivery_workers 表已不存在,跳过合并迁移") {
        tableExists(tableName = "delivery_workers")
      }
      comment("Merge delivery_workers profile into admins single table")
      // 1. admins 增加配送业务字段
      addColumn(tableName = "admins") {
        column(name = "avatar_url", type = "varchar(512)")
        column(name = "online_status", type = "varchar(20)", defaultValue = "OFFLINE") {
          constraints(nullable = false)
        }
        column(name = "coordinates", type = "json")
        column(name = "current_location", type = "json")
        column(name = "rating", type = "double")
        column(name = "total_orders", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "completed_orders", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "average_rating", type = "double")
        column(name = "earning_cents", type = "bigint")
        column(name = "is_available", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
      }
      // 2. 档案字段回填到关联账号行(delivery_workers.admin_id ↔ admins.id)
      sql(
        sql = """
      UPDATE admins a
      SET avatar_url = dw.avatar_url,
          online_status = dw.online_status,
          coordinates = dw.coordinates,
          current_location = dw.current_location,
          rating = dw.rating,
          total_orders = dw.total_orders,
          completed_orders = dw.completed_orders,
          average_rating = dw.average_rating,
          earning_cents = dw.earning_cents,
          is_available = dw.is_available
      FROM delivery_workers dw
      WHERE dw.admin_id = a.id
    
            """,
      )
      // 3. 引用表 delivery_worker_id 由档案 id 重映射为账号 id
      sql(
        sql = """
      UPDATE orders o
      SET delivery_worker_id = a.id
      FROM delivery_workers dw
      JOIN admins a ON a.id = dw.admin_id
      WHERE dw.id = o.delivery_worker_id
    
            """,
      )
      sql(
        sql = """
      UPDATE reviews r
      SET delivery_worker_id = a.id
      FROM delivery_workers dw
      JOIN admins a ON a.id = dw.admin_id
      WHERE dw.id = r.delivery_worker_id
    
            """,
      )
      sql(
        sql = """
      UPDATE delivery_worker_statistics s
      SET delivery_worker_id = a.id
      FROM delivery_workers dw
      JOIN admins a ON a.id = dw.admin_id
      WHERE dw.id = s.delivery_worker_id
    
            """,
      )
      sql(
        sql = """
      UPDATE daily_collection_records d
      SET delivery_worker_id = a.id
      FROM delivery_workers dw
      JOIN admins a ON a.id = dw.admin_id
      WHERE dw.id = d.delivery_worker_id
    
            """,
      )
      // 4. 存量 delivery_{档案ID} 用户名归一为 delivery_{账号ID}(冲突行保留原名)
      sql(
        sql = """
      UPDATE admins a
      SET username = 'delivery_' || a.id
      WHERE a.username LIKE 'delivery\_%' ESCAPE '\'
        AND NOT EXISTS (
          SELECT 1 FROM admins a2 WHERE a2.username = 'delivery_' || a.id
        )
    
            """,
      )
      // 5. 清理:先删引用 FK,再删档案表与 admins.delivery_worker_id 冗余列
      dropForeignKeyConstraint(baseTableName = "reviews", constraintName = "fk_reviews_delivery_worker")
      dropTable(tableName = "delivery_workers")
      dropUniqueConstraint(tableName = "admins", constraintName = "uq_admins_delivery_worker_id")
      dropColumn(tableName = "admins", columnName = "delivery_worker_id")
    }
  })
