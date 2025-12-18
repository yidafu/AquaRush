package dev.yidafu.aqua.common.graphql.util

import org.springframework.data.domain.Page
import dev.yidafu.aqua.common.graphql.generated.PageInfo

/**
 * Utility object for consistent pagination mapping from Spring Data Page to GraphQL Page types
 */
//object PaginationUtils {

    /**
     * Converts a Spring Data Page to a GraphQL Page structure with PageInfo
     *
     * @param page The Spring Data Page to convert
     * @param mapper Function to transform domain entities to GraphQL types
     * @return A Pair containing the list of items and PageInfo
     */
    inline fun <T : Any, R> Page<T>.toPageInfo(mapper: (T) -> R): Pair<List<R>, PageInfo> {
        return Pair(
            content.map(mapper),
            PageInfo(
                hasNext = hasNext(),
                hasPrevious = hasPrevious(),
                pageNum = number,
                pageSize = size,
                total = totalElements.toInt(),
                totalPages = totalPages
            )
        )
    }

    /**
     * Creates PageInfo directly from a Spring Data Page
     */
    fun <T : Any> Page<T>.toPageInfo(): PageInfo {
        return PageInfo(
            hasNext = hasNext(),
            hasPrevious = hasPrevious(),
            pageNum = number,
            pageSize = size,
            total = totalElements.toInt(),
            totalPages = totalPages
        )
    }

    /**
     * Creates an empty PageInfo for empty results
     */
    fun emptyPageInfo(pageNum: Int = 0, pageSize: Int = 20): PageInfo {
        return PageInfo(
            hasNext = false,
            hasPrevious = pageNum > 0,
            pageNum = pageNum,
            pageSize = pageSize,
            total = 0,
            totalPages = 0
        )
    }
//}
