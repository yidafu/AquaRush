/*
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

package dev.yidafu.aqua.product.service

import dev.yidafu.aqua.api.service.ProductService
import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import jakarta.persistence.criteria.Predicate as JpaPredicate
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class AdminUserFavoriteService(
  private val productFavoriteRepository: ProductFavoriteRepository,
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val productService: ProductService,
  private val productRepository: ProductRepository,
  private val userFavoriteService: UserFavoriteService
) {

  /**
   * Get comprehensive analytics about user favorites
   */
  @Cacheable(value = ["admin_favorites_analytics"], key = "'stats'", unless = "#result == null")
  fun getFavoriteAnalytics(): UserFavoriteAnalytics {
    val now = LocalDateTime.now()
    val todayStart = now.toLocalDate().atStartOfDay()
    val weekStart = now.minusDays(7).toLocalDate().atStartOfDay()
    val monthStart = now.minusDays(30).toLocalDate().atStartOfDay()

    val totalFavorites = productFavoriteRepository.count()
    val activeUsers = productFavoriteRepository.countDistinctUsersWithFavorites()
    val averageFavorites = if (activeUsers > 0) productFavoriteRepository.getAverageFavoritesPerUser() else 0.0

    val newToday = productFavoriteRepository.countFavoritesSince(todayStart)
    val newThisWeek = productFavoriteRepository.countFavoritesSince(weekStart)
    val newThisMonth = productFavoriteRepository.countFavoritesSince(monthStart)

    val mostFavoritedProducts = getMostFavoritedProducts(10)
    val favoritesTrend = getFavoritesTrend(30)

    return UserFavoriteAnalytics(
      totalFavorites = totalFavorites,
      activeUsersWithFavorites = activeUsers,
      averageFavoritesPerUser = averageFavorites.toFloat(),
      mostFavoritedProducts = mostFavoritedProducts,
      favoritesTrend = favoritesTrend,
      newFavoritesToday = newToday,
      newFavoritesThisWeek = newThisWeek,
      newFavoritesThisMonth = newThisMonth
    )
  }

  /**
   * Get most favorited products with detailed stats
   */
  @Cacheable(value = ["admin_trending_products"], key = "#limit", unless = "#result == null")
  fun getMostFavoritedProducts(limit: Int = 10): List<ProductFavoriteCount> {
    val now = LocalDateTime.now()
    val weekStart = now.minusDays(7).toLocalDate().atStartOfDay()
    val monthStart = now.minusDays(30).toLocalDate().atStartOfDay()
    val todayStart = now.toLocalDate().atStartOfDay()

    val mostFavorited = productFavoriteRepository.findMostFavoritedProducts()
      .take(limit)

    return mostFavorited.map { row ->
      val product = row[0] as ProductModel
      val totalCount = (row[1] as Number).toLong()

      // Get period-specific counts
      val addedThisWeek = productFavoriteRepository.countFavoritesBetween(weekStart, now)
        .let { total -> if (total == 0L) 0L else
          productFavoriteRepository.findMostFavoritedProductsSince(weekStart)
            .find { it[0] == product }?.let { weekRow -> (weekRow[1] as Number).toLong() } ?: 0L
        }

      val addedThisMonth = productFavoriteRepository.countFavoritesBetween(monthStart, now)
        .let { total -> if (total == 0L) 0L else
          productFavoriteRepository.findMostFavoritedProductsSince(monthStart)
            .find { it[0] == product }?.let { monthRow -> (monthRow[1] as Number).toLong() } ?: 0L
        }

      val addedToday = productFavoriteRepository.countFavoritesBetween(todayStart, now)
        .let { total -> if (total == 0L) 0L else
          productFavoriteRepository.findMostFavoritedProductsSince(todayStart)
            .find { it[0] == product }?.let { todayRow -> (todayRow[1] as Number).toLong() } ?: 0L
        }

      ProductFavoriteCount(
        product = convertToGraphQLProduct(product),
        favoriteCount = totalCount,
        addedThisWeek = addedThisWeek,
        addedThisMonth = addedThisMonth,
        addedToday = addedToday
      )
    }
  }

  /**
   * Get favorites trend data for the specified number of days
   */
  @Cacheable(value = ["admin_favorites_trend"], key = "#days", unless = "#result == null")
  fun getFavoritesTrend(days: Int = 30): List<FavoriteTrendData> {
    val startDate = LocalDateTime.now().minusDays(days.toLong())
    val trendData = productFavoriteRepository.getFavoritesTrend(startDate)

    return trendData.map { row ->
      val date = row[0] as String
      val newFavorites = (row[1] as Number).toLong()
      val activeUsers = (row[2] as Number).toLong()

      // Calculate cumulative total favorites up to this date
      val totalFavorites = productFavoriteRepository.countFavoritesBetween(
        LocalDateTime.now().minusYears(1),
        LocalDateTime.parse("${date}T23:59:59")
      )

      FavoriteTrendData(
        date = date,
        newFavorites = newFavorites,
        totalFavorites = totalFavorites,
        activeUsers = activeUsers
      )
    }
  }

  /**
   * Get users with favorites summary
   */
  fun getUsersWithFavorites(input: AdminFavoriteListInput): UserFavoriteSummaryPage {
    val page = input.page ?: 0;
    val size = input.size ?: 20;
    val pageable = PageRequest.of(page, size)

    // Build specification for filtering
    val spec = buildFavoriteSpecification(input)
    val favoritesPage = productFavoriteRepository.findAll(spec, pageable)

    // Group by user and create summaries
    val userSummaries = favoritesPage.content
      .groupBy { it.userId }
      .map { (userId, userFavorites) ->
        val user = userService.findById(userId) ?: throw NotFoundException("User not found: $userId")

        val products = userFavorites.mapNotNull { favorite ->
          productRepository.findById(favorite.productId).orElse(null)
        }

        val totalValue = products.sumOf { it.price }
        val categories = products.mapNotNull { extractCategoryFromProduct(it) }.distinct()
        val lastFavorite = userFavorites.maxByOrNull { it.createdAt }?.createdAt

        UserFavoriteSummary(
          user = convertToGraphQLUser(user),
          favoriteCount = userFavorites.size.toLong(),
          lastFavoriteAdded = lastFavorite,
          totalValueOfFavoritedProducts = totalValue,
          categories = categories,
          mostFavoritedCategory = categories.groupBy { it }.maxByOrNull { it.key }?.key
        )
      }
      .sortedWith(compareByDescending<UserFavoriteSummary> { it.favoriteCount }
        .thenByDescending { it.lastFavoriteAdded })

    val totalCount = productFavoriteRepository.countDistinctUsersWithFavorites()

    return UserFavoriteSummaryPage(
      list = userSummaries,
      pageInfo = PageInfo(
        total = totalCount.toInt(),
        pageSize = size,
        pageNum = page,
        hasNext = (page + 1) * size < totalCount,
        hasPrevious = page > 0,
        totalPages = Math.ceil(totalCount.toDouble() / size).toInt()
      )
    )
  }

  /**
   * Get detailed favorites with product information
   */
  fun getUserFavoritesWithDetails(input: AdminFavoriteListInput): UserFavoriteWithProductPage {
    val pageable = createPageableFromInput(input)

    val favoritesPage = when {
      input.userId != null && input.productId != null -> {
        val favorites = productFavoriteRepository.findByUserIdsAndProductIds(
          listOf(input.userId!!),
          listOf(input.productId!!)
        )
        PageImpl(favorites, pageable, favorites.size.toLong())
      }
      else -> {
        productFavoriteRepository.findFavoritesWithFilters(
          input.userId,
          input.productId,
          input.dateFrom,
          input.dateTo,
          pageable
        )
      }
    }

    val favoritesWithDetails = favoritesPage.content.mapNotNull { favorite ->
      val user = userService.findById(favorite.userId)
      val product = productRepository.findById(favorite.productId).orElse(null)

      if (user != null && product != null) {
        UserFavoriteWithProduct(
          id = favorite.id!!,
          user = convertToGraphQLUser(user),
          product = convertToGraphQLProduct(product),
          createdAt = favorite.createdAt,
          productStatus = convertProductStatus(product.status),
          productStock = product.stock,
          productSalesVolume = product.salesVolume
        )
      } else null
    }

    return UserFavoriteWithProductPage(
      list = favoritesWithDetails,
      pageInfo = PageInfo(
        total = favoritesPage.totalElements.toInt(),
        pageSize = input.size ?: 20,
        pageNum = input.page ?: 0,
        hasNext = favoritesPage.hasNext(),
        hasPrevious = favoritesPage.hasPrevious(),
        totalPages = favoritesPage.totalPages
      )
    )
  }

  /**
   * Get user favorite insights for engagement analysis
   */
  @Cacheable(value = ["admin_user_insights"], key = "#userId", unless = "#result == null")
  fun getUserFavoriteInsights(userId: Long): UserFavoriteInsights {
    val user = userService.findById(userId) ?: throw NotFoundException("User not found: $userId")

    val userFavorites = productFavoriteRepository.findByUserId(
      userId,
      PageRequest.of(0, Integer.MAX_VALUE)
    ).content

    if (userFavorites.isEmpty()) {
      return UserFavoriteInsights(
        userId = userId,
        userNickname = user.nickname ?: "Unknown",
        totalFavorites = 0,
        favoriteCategories = emptyList(),
        averageFavoritePrice = 0,
        favoriteToOrderRate = 0.0f,
        lastActiveAt = user.updatedAt ?: user.createdAt!!,
        engagementLevel = UserEngagementLevel.LOW
      )
    }

    val products = userFavorites.mapNotNull { favorite ->
      productRepository.findById(favorite.productId).orElse(null)
    }

    val categories = products.mapNotNull { extractCategoryFromProduct(it) }.distinct()
    val averagePrice = if (products.isNotEmpty()) {
      products.map { it.price }.average().toLong()
    } else 0L

    val engagementLevel = calculateEngagementLevel(userFavorites.size, products)

    return UserFavoriteInsights(
      userId = userId,
      userNickname = user.nickname ?: "Unknown",
      totalFavorites = userFavorites.size.toLong(),
      favoriteCategories = categories,
      averageFavoritePrice = averagePrice,
      favoriteToOrderRate = 0.0f, // TODO: Implement order tracking
      lastActiveAt = userFavorites.maxByOrNull { it.createdAt }?.createdAt ?: user.updatedAt!!,
      engagementLevel = engagementLevel
    )
  }

  /**
   * Perform batch operations on user favorites
   */
  @CacheEvict(value = ["admin_favorites_analytics", "admin_trending_products", "admin_favorites_trend"], allEntries = true)
  fun performBatchOperation(input: BatchFavoriteOperationInput): BatchOperationResult {
    val results = mutableListOf<String>()
    var successCount = 0
    var failureCount = 0

    try {
      when (input.operation) {
        BatchFavoriteOperation.EXPORT_FAVORITES -> {
          // Export functionality would be implemented here
          results.add("Export functionality initiated")
          successCount = 1
        }

        BatchFavoriteOperation.ANALYZE_FAVORITES -> {
          // Analysis functionality would be implemented here
          results.add("Analysis functionality initiated")
          successCount = 1
        }

        else -> {
          throw BadRequestException("Unsupported batch operation: ${input.operation}")
        }
      }
    } catch (e: Exception) {
      failureCount = input.userIds.toList().size + input.productIds.toList().size
      results.add("Operation failed: ${e.message}")
    }

    return BatchOperationResult(
      success = failureCount == 0,
      message = if (failureCount == 0) "Operation completed successfully" else "Operation completed with $failureCount failures",
      processedCount = input.userIds.toList().size + input.productIds.toList().size,
      successCount = successCount,
      failureCount = failureCount,
      errors = if (failureCount > 0) listOf("Some operations failed") else emptyList(),
      details = mapOf(
        "operation" to input.operation.name,
        "userCount" to input.userIds.toList().size,
        "productCount" to input.productIds.toList().size,
        "reason" to (input.reason ?: "")
      )
    )
  }

  /**
   * Export favorites data to CSV/Excel format
   */
  @Cacheable(value = ["admin_favorites_export"], key = "#input.hashCode()", unless = "#result == null")
  fun exportFavorites(input: ExportFavoritesInput): ExportFavoritesResult {
    val favorites = productFavoriteRepository.findFavoritesForExport(
      input.userId,
      input.productIds?.toList(),
      input.dateFrom,
      input.dateTo
    )

    val outputStream = ByteArrayOutputStream()

    when (input.format) {
      ExportFormat.CSV -> {
        val printer = CSVPrinter(outputStream.writer(), CSVFormat.DEFAULT.withHeader(
          "ID", "User ID", "User Name", "Product ID", "Product Name", "Price", "Created At"
        ))

        favorites.forEach { favorite ->
          val user = userService.findById(favorite.userId)
          val product = productRepository.findById(favorite.productId).orElse(null)

          printer.printRecord(
            favorite.id,
            favorite.userId,
            user?.nickname ?: "",
            favorite.productId,
            product?.name ?: "",
            product?.price ?: 0,
            favorite.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
          )
        }

        printer.flush()
      }

      ExportFormat.EXCEL -> {
        // Excel export would be implemented here with Apache POI
        throw BadRequestException("Excel export not yet implemented")
      }

      ExportFormat.JSON -> {
        // JSON export would be implemented here
        throw BadRequestException("JSON export not yet implemented")
      }
    }

    val fileName = "favorites_export_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}_${System.currentTimeMillis()}.csv"
    val fileSize = outputStream.size().toLong()
    val expiresAt = LocalDateTime.now().plusDays(7)

    return ExportFavoritesResult(
      success = true,
      message = "Export completed successfully",
      downloadUrl = "/api/exports/$fileName", // This would be implemented with file storage
      fileName = fileName,
      fileSize = fileSize,
      recordCount = favorites.size,
      expiresAt = expiresAt
    )
  }

  // Private helper methods

  private fun buildFavoriteSpecification(input: AdminFavoriteListInput): Specification<ProductFavoriteModel> {
    return Specification { root, query, criteriaBuilder ->
      val predicates = mutableListOf<JpaPredicate>()

      input.userId?.let { userId ->
        predicates.add(criteriaBuilder.equal(root.get<Long>("userId"), userId))
      }

      input.productId?.let { productId ->
        predicates.add(criteriaBuilder.equal(root.get<Long>("productId"), productId))
      }

      input.dateFrom?.let { dateFrom ->
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), dateFrom))
      }

      input.dateTo?.let { dateTo ->
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), dateTo))
      }

      criteriaBuilder.and(*predicates.toTypedArray())
    }
  }

  private fun createPageableFromInput(input: AdminFavoriteListInput): Pageable {
    val sort = when (input.sortBy) {
      FavoriteSortBy.CREATED_AT_ASC -> Sort.by("createdAt").ascending()
      FavoriteSortBy.CREATED_AT_DESC -> Sort.by("createdAt").descending()
      FavoriteSortBy.USER_ID_ASC -> Sort.by("userId").ascending()
      FavoriteSortBy.USER_ID_DESC -> Sort.by("userId").descending()
      FavoriteSortBy.PRODUCT_ID_ASC -> Sort.by("productId").ascending()
      FavoriteSortBy.PRODUCT_ID_DESC -> Sort.by("productId").descending()
      // Product-based sorting would require joins
      else -> Sort.by("createdAt").descending()
    }

    return PageRequest.of(input.page ?: 0, input.size ?: 20, sort)
  }

  private fun convertToGraphQLUser(user: UserModel): User {
    return User(
      id = user.id!!,
      wechatOpenId = user.wechatOpenId,
      nickname = user.nickname,
      phone = user.phone,
      avatarUrl = user.avatarUrl,
      status = user.status,
      balanceCents = user.balanceCents,
      totalSpentCents = user.totalSpentCents,
      email = user.email,
      role = user.role,
      createdAt = user.createdAt!!,
      updatedAt = user.updatedAt!!
    )
  }

  private fun convertToGraphQLProduct(product: ProductModel): Product {
    return Product(
      id = product.id!!,
      name = product.name,
      subtitle = product.subtitle,
      price = product.price ?: 0L,
      originalPrice = product.originalPrice,
      depositPrice = product.depositPrice,
      coverImageUrl = product.coverImageUrl,
      imageGallery = product.imageGallery,
      specification = product.specification,
      waterSource = product.waterSource,
      mineralContent = product.mineralContent,
      stock = product.stock,
      salesVolume = product.salesVolume,
      status = convertProductStatus(product.status),
      sortOrder = product.sortOrder,
      tags = product.tags,
      detailContent = product.detailContent,
      certificateImages = product.certificateImages,
      deliverySettings = product.deliverySettings,
      createdAt = product.createdAt!!,
      updatedAt = product.updatedAt!!,
      isDeleted = false
    )
  }

  private fun convertProductStatus(status: ProductStatus): ProductStatus {
    return status
  }

  private fun extractCategoryFromProduct(product: ProductModel): String? {
    // Extract category from product tags, water source, or other metadata
    return product.tags?.let { tags ->
      if (tags is List<*>) {
        tags.firstOrNull()?.toString()
      } else null
    } ?: product.waterSource?.let { source ->
      when {
        source.contains("矿泉") -> "矿泉水"
        source.contains("纯") -> "纯净水"
        source.contains("天然") -> "天然水"
        else -> "其他"
      }
    }
  }

  private fun calculateEngagementLevel(favoriteCount: Int, products: List<ProductModel>): UserEngagementLevel {
    return when {
      favoriteCount >= 20 -> UserEngagementLevel.VERY_HIGH
      favoriteCount >= 10 -> UserEngagementLevel.HIGH
      favoriteCount >= 5 -> UserEngagementLevel.MEDIUM
      else -> UserEngagementLevel.LOW
    }
  }
}
