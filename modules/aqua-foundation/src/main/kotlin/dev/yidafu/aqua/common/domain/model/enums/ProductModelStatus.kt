package dev.yidafu.aqua.common.domain.model.enums

enum class ProductModelStatus(
  val label: String,
) {
  OFFLINE("OFFLINE"),
  ONLINE("ONLINE"),
  OUT_OF_STOCK("OUT_OF_STOCK"),
  ;

  companion object {
    fun fromValue(value: String): ProductModelStatus =
      when (value) {
        "OFFLINE" -> OFFLINE
        "ONLINE" -> ONLINE
        "OUT_OF_STOCK" -> OUT_OF_STOCK
        else -> throw IllegalArgumentException("Unknown ProductModelStatus value: '" + value + "'")
      }
  }
}
