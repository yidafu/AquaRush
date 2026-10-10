package dev.yidafu.aqua.common.domain.model.enums

/**
 * 对账差异状态
 */
enum class DiscrepancyStatus {
  UNRESOLVED, // 未解决
  RESOLVED, // 已解决
  ;

  companion object {
    fun fromValue(value: String): DiscrepancyStatus =
      when (value) {
        "UNRESOLVED" -> UNRESOLVED
        "RESOLVED" -> RESOLVED
        else -> throw IllegalArgumentException("Unknown DiscrepancyStatus value: '" + value + "'")
      }
  }
}
