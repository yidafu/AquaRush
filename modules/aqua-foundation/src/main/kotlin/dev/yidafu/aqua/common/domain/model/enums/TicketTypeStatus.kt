package dev.yidafu.aqua.common.domain.model.enums

/**
 * 桶水票票种状态:
 * - ACTIVE: 上架(用户端水票商城可购)
 * - DISABLED: 停用(不可再购,已购票按快照继续有效)
 */
enum class TicketTypeStatus {
  ACTIVE,
  DISABLED,
  ;

  companion object {
    fun fromValue(value: String): TicketTypeStatus =
      when (value) {
        "ACTIVE" -> ACTIVE
        "DISABLED" -> DISABLED
        else -> throw IllegalArgumentException("Unknown TicketTypeStatus value: '" + value + "'")
      }
  }
}
