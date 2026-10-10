package dev.yidafu.aqua.common.domain.model.enums

enum class AdminRoleModel {
  SUPER_ADMIN, // 超级管理员
  ADMIN, // 管理员
  DELIVERY_WORKER, // 送水员
  ;

  companion object {
    fun fromValue(value: String): AdminRoleModel =
      when (value) {
        "SUPER_ADMIN" -> SUPER_ADMIN
        "ADMIN" -> ADMIN
        "DELIVERY_WORKER" -> DELIVERY_WORKER
        else -> throw IllegalArgumentException("Unknown AdminRoleModel value: '" + value + "'")
      }
  }
}
