package dev.yidafu.aqua.api.dto

import org.springframework.data.domain.Page

data class PageInfo(
  val hasNext: Boolean = false,
  val hasPrevious: Boolean = false,
  val pageNum: Int = 1,
  val pageSize: Int = 20,
  val total: Int = 0,
  val totalPages: Int = 0,
)

private fun <T : Any> Page<T>.toPageInfo(): PageInfo =
  PageInfo(
    hasNext = hasNext(),
    hasPrevious = hasPrevious(),
    pageNum = number,
    pageSize = size,
    total = totalElements.toInt(),
    totalPages = totalPages,
  )

data class PageImpl<T>(
  val list: List<T>,
  val pageInfo: PageInfo,
) {
  companion object {
    fun <T : Any> fromPage(page: Page<T>) =
      PageImpl(
        page.content,
        page.toPageInfo(),
      )
  }
}
