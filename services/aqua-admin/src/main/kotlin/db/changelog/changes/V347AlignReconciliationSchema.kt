/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

/**
 * G93 微信支付对账收尾(F-ORD-031/R-PAY-04,对齐 spec/tech/design/07-payment-reconciliation.md):
 * 1) reconciliation_tasks 增触发方式/执行人与微信/本地两侧汇总列(三方校验落列,§3.1);
 * 2) chk_task_status 增 SKIPPED(未配置真实商户参数时任务跳过,§6-2);
 * 3) chk_discrepancy_status 增 IGNORED(差异「忽略」处置:跨日边界等可解释差异,R-PAY-04)。
 */
class V347AlignReconciliationSchema :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.7-add-reconciliation-trigger-totals") {
      comment("Add reconciliation task trigger/executed-by and wechat/local totals columns")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS trigger_type varchar(20) NOT NULL DEFAULT 'SCHEDULED'")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS executed_by bigint")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS wechat_total_count int NOT NULL DEFAULT 0")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS wechat_total_amount_cents bigint NOT NULL DEFAULT 0")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS local_total_count int NOT NULL DEFAULT 0")
      sql(sql = "ALTER TABLE reconciliation_tasks ADD COLUMN IF NOT EXISTS local_total_amount_cents bigint NOT NULL DEFAULT 0")
      sql(
        sql = """
        ALTER TABLE reconciliation_tasks DROP CONSTRAINT IF EXISTS chk_recon_trigger_type;
        ALTER TABLE reconciliation_tasks ADD CONSTRAINT chk_recon_trigger_type
          CHECK (trigger_type IN ('SCHEDULED', 'MANUAL'));
        """,
      )
      sql(sql = "CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_trigger ON reconciliation_tasks(trigger_type)")
    }

    changeSet(author = "aqua-rush", id = "3.4.7-widen-reconciliation-status-checks") {
      comment("chk_task_status += SKIPPED (unconfigured wechat credentials); chk_discrepancy_status += IGNORED")
      sql(
        sql = """
        ALTER TABLE reconciliation_tasks DROP CONSTRAINT IF EXISTS chk_task_status;
        ALTER TABLE reconciliation_tasks ADD CONSTRAINT chk_task_status
          CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'));
        """,
      )
      sql(
        sql = """
        ALTER TABLE reconciliation_discrepancies DROP CONSTRAINT IF EXISTS chk_discrepancy_status;
        ALTER TABLE reconciliation_discrepancies ADD CONSTRAINT chk_discrepancy_status
          CHECK (status IN ('UNRESOLVED', 'RESOLVED', 'IGNORED'));
        """,
      )
    }
  })
