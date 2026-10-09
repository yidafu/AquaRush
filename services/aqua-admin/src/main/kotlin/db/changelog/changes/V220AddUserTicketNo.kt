// 转换自 changes/v2.2.0-add-user-ticket-no.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V220AddUserTicketNo :
  KotlinCompiledDatabaseChangeLog({
    // F-MKT-007 水票唯一递增票号(2026-08 优化):
    //     - user_tickets.ticket_no:用户可见水票编号,格式 SP + 入账年月(YYYYMM) + 8 位零填充序号,
    //       每月从 00000001 重新递增(发票式编号),(月份, 序号) 组合唯一;
    //     - ticket_no_sequences:按月计数表,INSERT ... ON CONFLICT DO UPDATE ... RETURNING 原子取号,
    //       跨月自动重置、重启不丢(不依赖定时任务/DB sequence 手动重置)。
    //     历史数据按 created_at 月份分组、id 序回填,计数表初始值接续最大序号。
    changeSet(author = "aqua-rush", id = "2.2.0-add-user-ticket-no-column") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "user_tickets.ticket_no 已存在,跳过") {
        not {
          columnExists(tableName = "user_tickets", columnName = "ticket_no")
        }
      }
      comment("Add user_tickets.ticket_no (水票唯一递增票号)")
      addColumn(tableName = "user_tickets") {
        column(name = "ticket_no", type = "varchar(32)")
      }
      // 回填:按创建月份分组、id(发放入账顺序)编号
      sql(
        sql = """
      UPDATE user_tickets t
      SET ticket_no = 'SP' || to_char(t.created_at, 'YYYYMM') || lpad(rn::text, 8, '0')
      FROM (
        SELECT id,
               to_char(created_at, 'YYYYMM') AS mk,
               row_number() OVER (PARTITION BY to_char(created_at, 'YYYYMM') ORDER BY id) AS rn
        FROM user_tickets
      ) s
      WHERE t.id = s.id
    
            """,
      )
      createIndex(tableName = "user_tickets", indexName = "uk_user_tickets_ticket_no", unique = true) {
        column(name = "ticket_no")
      }
      addNotNullConstraint(tableName = "user_tickets", columnName = "ticket_no", columnDataType = "varchar(32)")
    }
    changeSet(author = "aqua-rush", id = "2.2.0-create-ticket-no-sequences-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "ticket_no_sequences 表已存在,跳过") {
        not {
          tableExists(tableName = "ticket_no_sequences")
        }
      }
      comment("Create ticket_no_sequences (按月水票票号计数表,原子自增)")
      createTable(tableName = "ticket_no_sequences") {
        column(name = "month_key", type = "varchar(6)") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "last_seq", type = "bigint") {
          constraints(nullable = false)
        }
      }
      // 初始值接续回填最大序号:本月已有票则从 max 继续,否则空表等待首次取号(insert 分支置 1)
      sql(
        sql = """
      INSERT INTO ticket_no_sequences (month_key, last_seq)
      SELECT mk, MAX(rn)
      FROM (
        SELECT to_char(created_at, 'YYYYMM') AS mk,
               row_number() OVER (PARTITION BY to_char(created_at, 'YYYYMM') ORDER BY id) AS rn
        FROM user_tickets
      ) x
      GROUP BY mk
    
            """,
      )
    }
  })
