package dev.yidafu.aqua.common.domain.model

enum class ProductModelStatus(
  val label: String,
) {
  OFFLINE("OFFLINE"),
  ONLINE("ONLINE"),
  OUT_OF_STOCK("OUT_OF_STOCK"),
}
