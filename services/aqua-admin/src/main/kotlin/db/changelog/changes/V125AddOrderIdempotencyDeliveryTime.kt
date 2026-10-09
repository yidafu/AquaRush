// 转换自 changes/v1.2.5-add-order-idempotency-delivery-time.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V125AddOrderIdempotencyDeliveryTime :
  KotlinCompiledDatabaseChangeLog({
    // 下单支付链路完善:
    //     1. R-ORD-06 服务端下单幂等:orders 新增 client_request_id,并建 (user_id, client_request_id) 唯一索引,
    //        同一用户同一 clientRequestId 重复提交不产生重复订单(并发重复由唯一约束兜底);
    //        可空列,NULL 不参与唯一约束(未携带幂等键的旧请求维持原行为)。
    //     2. AC-ORD-001-05 配送时间随单落库:orders 新增 delivery_time_slot varchar(20)(可空,默认立即配送 IMMEDIATE)。
    changeSet(author = "aqua-rush", id = "1.2.5-add-order-idempotency-delivery-time") {
      // 修复存量漂移(该缺陷在 XML 时代就存在,全新库上必失败):orders.client_request_id/delivery_time_slot/uk_orders_user_client_request 已被就地并入 v1.0.0
      preConditions(onFail = "MARK_RAN", onErrorMessage = "client_request_id 已存在,跳过") {
        not {
          columnExists(tableName = "orders", columnName = "client_request_id")
        }
      }
      comment("Add client_request_id (idempotency R-ORD-06) and delivery_time_slot (AC-ORD-001-05) to orders")
      addColumn(tableName = "orders") {
        column(name = "client_request_id", type = "varchar(64)") {
          constraints(nullable = true)
        }
      }
      addColumn(tableName = "orders") {
        column(name = "delivery_time_slot", type = "varchar(20)") {
          constraints(nullable = true)
        }
      }
      // 服务端幂等最终防线(R-ORD-06):同一用户同一 clientRequestId 全局唯一
      createIndex(indexName = "uk_orders_user_client_request", tableName = "orders", unique = true) {
        column(name = "user_id")
        column(name = "client_request_id")
      }
    }
  })
