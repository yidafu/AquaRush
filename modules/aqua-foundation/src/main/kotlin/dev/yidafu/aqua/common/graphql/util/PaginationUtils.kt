package dev.yidafu.aqua.common.graphql.util

import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import org.springframework.data.domain.Page

/**
 * Converts a Spring Data Page to a GraphQL Page structure with PageInfoVo
 *
 * @param page The Spring Data Page to convert
 * @param mapper Function to transform domain entities to GraphQL types
 * @return A Pair containing the list of items and PageInfoVo
 */
inline fun <T : Any, R> Page<T>.toPageInfo(mapper: (T) -> R): Pair<List<R>, PageInfoVo> =
  Pair(
    content.map(mapper),
    PageInfoVo(
      hasNext = hasNext(),
      hasPrevious = hasPrevious(),
      pageNum = number,
      pageSize = size,
      total = totalElements.toInt(),
      totalPages = totalPages,
    ),
  )

/**
 * Creates PageInfoVo directly from a Spring Data Page
 */
fun <T : Any> Page<T>.toPageInfo(): PageInfoVo =
  PageInfoVo(
    hasNext = hasNext(),
    hasPrevious = hasPrevious(),
    pageNum = number,
    pageSize = size,
    total = totalElements.toInt(),
    totalPages = totalPages,
  )

/**
 * Creates an empty PageInfoVo for empty results
 */
fun emptyPageInfo(
  pageNum: Int = 0,
  pageSize: Int = 20,
): PageInfoVo =
  PageInfoVo(
    hasNext = false,
    hasPrevious = pageNum > 0,
    pageNum = pageNum,
    pageSize = pageSize,
    total = 0,
    totalPages = 0,
  )
// }
