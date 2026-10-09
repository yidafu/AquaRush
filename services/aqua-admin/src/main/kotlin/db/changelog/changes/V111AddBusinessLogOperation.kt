// 转换自 changes/v1.1.1-add-business-log-operation.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V111AddBusinessLogOperation :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.1.1-add-business-log-operation") {
      // 修复存量漂移(该缺陷在 XML 时代就存在,全新库上必失败):business_logs 由 v1.0.0 的 raw SQL 建表,operation 列已被就地并入其中
      preConditions(onFail = "MARK_RAN", onErrorMessage = "operation 已存在,跳过") {
        not {
          columnExists(tableName = "business_logs", columnName = "operation")
        }
      }
      comment("Add operation column to business_logs for F-LOG-003 (PRD 09-platform-logs.md): 操作类型结构化落库,老数据为 NULL(前端回退解析 message 前缀)")
      addColumn(tableName = "business_logs") {
        column(name = "operation", type = "varchar(100)")
      }
    }
  })
