package dev.yidafu.aqua.common.domain.model.enums

/**
 * 后台账号状态(PRD/07-user.md §F-USR-020/021)
 *
 * 停用(DISABLED)账号无法登录后台/送水工端,旧 token 亦失效;
 * 停用送水员不可派单/接单(AC-USR-020-07、AC-USR-021-07)。
 */
enum class AdminAccountStatus {
  ACTIVE, // 正常
  DISABLED, // 停用
  ;

  companion object {
    fun fromValue(value: String): AdminAccountStatus =
      when (value) {
        "ACTIVE" -> ACTIVE
        "DISABLED" -> DISABLED
        else -> throw IllegalArgumentException("Unknown AdminAccountStatus value: '" + value + "'")
      }
  }
}
