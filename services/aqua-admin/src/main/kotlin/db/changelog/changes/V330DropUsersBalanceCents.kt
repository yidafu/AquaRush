// B1b 缺陷修复(裁决清单 §1.6):删除 users.balance_cents 遗留列。
// PRD 2026-10-09 决策「不做用户储值」(10-marketing.md §1.1 合规决策),储值三件套不落地,
// 该列为储值模型残留,代码(UserModel/Exposed 表/仓储/GraphQL UserVo)同步移除。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V330DropUsersBalanceCents :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.3.0-drop-users-balance-cents") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "users.balance_cents 已不存在,跳过") {
        columnExists(tableName = "users", columnName = "balance_cents")
      }
      comment("Drop users.balance_cents (stored-value not implemented, PRD 2026-10-09 decision)")
      dropColumn(tableName = "users", columnName = "balance_cents")
    }
  })
