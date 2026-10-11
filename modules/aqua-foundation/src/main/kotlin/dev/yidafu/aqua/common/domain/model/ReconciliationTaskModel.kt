/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
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

package dev.yidafu.aqua.common.domain.model

/**
 * 对账任务实体
 */
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType
import java.time.LocalDateTime
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator as GenericGeneratorKlass

class ReconciliationTaskModel : SoftDeletable {
  var id: Long? = null

  var taskId: String = ""

  lateinit var taskType: ReconciliationTaskType

  var status: ReconciliationTaskStatus = ReconciliationTaskStatus.PENDING

  var taskDate: LocalDateTime? = null

  var startTime: LocalDateTime? = null

  var endTime: LocalDateTime? = null

  var totalRecords: Int = 0

  var matchedRecords: Int = 0

  var unmatchedRecords: Int = 0

  var errorMessage: String? = null

  /** 触发方式(调度自动/管理员手动;F-ORD-031 详情页「触发方式」) */
  var triggerType: ReconciliationTriggerType = ReconciliationTriggerType.SCHEDULED

  /** 手动触发的管理员 id;调度为 NULL */
  var executedBy: Long? = null

  /** 微信账单侧总笔数(三方校验之一,R-PAY-04) */
  var wechatTotalCount: Int = 0

  /** 微信账单侧总金额(分) */
  var wechatTotalAmountCents: Long = 0L

  /** 本地账实侧总笔数 */
  var localTotalCount: Int = 0

  /** 本地账实侧总金额(分) */
  var localTotalAmountCents: Long = 0L

  var createdAt: LocalDateTime = LocalDateTime.now()

  var updatedAt: LocalDateTime = LocalDateTime.now()

  override var deletedAt: LocalDateTime? = null

  override var deletedBy: Long? = null

  companion object {
    fun createPaymentTask(
      date: LocalDateTime,
      triggerType: ReconciliationTriggerType = ReconciliationTriggerType.SCHEDULED,
      executedBy: Long? = null,
    ): ReconciliationTaskModel =
      ReconciliationTaskModel().apply {
        taskId = GenericGeneratorKlass().generate().toString()
        taskType = ReconciliationTaskType.PAYMENT
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
        this.triggerType = triggerType
        this.executedBy = executedBy
      }

    fun createRefundTask(
      date: LocalDateTime,
      triggerType: ReconciliationTriggerType = ReconciliationTriggerType.SCHEDULED,
      executedBy: Long? = null,
    ): ReconciliationTaskModel =
      ReconciliationTaskModel().apply {
        taskId = GenericGeneratorKlass().generate().toString()
        taskType = ReconciliationTaskType.REFUND
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
        this.triggerType = triggerType
        this.executedBy = executedBy
      }

    fun createSettlementTask(
      date: LocalDateTime,
      triggerType: ReconciliationTriggerType = ReconciliationTriggerType.SCHEDULED,
      executedBy: Long? = null,
    ): ReconciliationTaskModel =
      ReconciliationTaskModel().apply {
        taskId = GenericGeneratorKlass().generate().toString()
        taskType = ReconciliationTaskType.SETTLEMENT
        taskDate = date
        status = ReconciliationTaskStatus.PENDING
        this.triggerType = triggerType
        this.executedBy = executedBy
      }
  }
}
