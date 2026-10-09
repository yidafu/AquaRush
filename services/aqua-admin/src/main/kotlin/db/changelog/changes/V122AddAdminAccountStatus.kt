// 转换自 changes/v1.2.2-add-admin-account-status.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V122AddAdminAccountStatus :
  KotlinCompiledDatabaseChangeLog({
    // 后台账号状态列(PRD/07-user.md §F-USR-020/021):
    //     admins.account_status ACTIVE/DISABLED,停用账号不可登录/不可派单接单(AC-USR-020-07/021-07)。
    changeSet(author = "aqua-rush", id = "1.2.2-add-admin-account-status") {
      preConditions(onFail = "MARK_RAN") {
        not {
          columnExists(tableName = "admins", columnName = "account_status")
        }
      }
      comment("Add account_status column to admins (account disable support)")
      addColumn(tableName = "admins") {
        column(name = "account_status", type = "varchar(20)", defaultValue = "ACTIVE") {
          constraints(nullable = false)
        }
      }
    }
  })
