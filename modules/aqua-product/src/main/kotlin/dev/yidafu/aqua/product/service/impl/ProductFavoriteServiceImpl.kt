/**
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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.mapper.toGraphQL
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ProductFavoriteServiceImpl(
  private val productFavoriteRepository: ProductFavoriteRepository,
  private val productRepository: ProductRepository,
  private val userService: UserService,
  private val productService: ProductService,
) : ProductFavoriteService {
  /**
   * Add product to user's favorites
   */
  override fun addToFavorites(
    userId: Long,
    productId: Long,
  ): ProductFavoriteModel {
    // Validate product exists and is active
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { NotFoundException("Product not found") }

    if (product.status != ProductModelStatus.ONLINE) {
      throw BadRequestException("Product is not available for favorites")
    }

    // Check if already exists (regardless of enable status)
    val existing = productFavoriteRepository.findByUserIdAndProductId(userId, productId)
    if (existing != null) {
      // If exists but disabled, enable it
      if (!existing.enable) {
        val updatedRows = productFavoriteRepository.updateEnableStatus(userId, productId, true)
        if (updatedRows > 0) {
          // Update the updatedAt timestamp
          val record = productFavoriteRepository.findByUserIdAndProductId(userId, productId)
          record?.let {
            // Create a new instance with updated fields since val cannot be modified
            val updatedFavorite =
              ProductFavoriteModel(
                id = it.id,
                userId = it.userId,
                productId = it.productId,
                product = it.product,
                enable = true,
                createdAt = it.createdAt,
                updatedAt = LocalDateTime.now(),
                deletedAt = it.deletedAt,
                deletedBy = it.deletedBy,
              )
            return productFavoriteRepository.save(updatedFavorite)
          }
        }
        throw BadRequestException("Failed to enable favorite")
      } else {
        throw BadRequestException("Product already in favorites")
      }
    } else {
      // Create new favorite with enable = true
      val favorite =
        ProductFavoriteModel(
          userId = userId,
          productId = productId,
          enable = true,
        )
      return productFavoriteRepository.save(favorite)
    }
  }

  /**
   * Get user's favorite products with pagination
   */
  override fun getFavoriteProducts(
    userId: Long,
    pageable: Pageable,
  ): Page<ProductModel> {
    // Get paginated product IDs from favorites
    val favoriteIdsPage = productFavoriteRepository.findFavoriteIdsByUserId(userId, pageable)

    if (favoriteIdsPage.isEmpty) {
      return Page.empty(pageable)
    }

    // Get products by IDs
    val products = productRepository.findAllById(favoriteIdsPage.content)

    // Filter by status (active/online only)
    val activeProducts =
      products.filter {
        it.status == ProductModelStatus.ONLINE
      }

    return PageImpl(activeProducts, pageable, favoriteIdsPage.totalElements)
  }

  /**
   * Check if product is favorited by user
   */
  override fun isProductFavorited(
    userId: Long,
    productId: Long,
  ): Boolean = productFavoriteRepository.existsByUserIdAndProductId(userId, productId)

  /**
   * Get total count of user's favorites
   */
  @Cacheable(value = ["user_favorites_count"], key = "#userId")
  override fun getFavoritesCount(userId: Long): Long = productFavoriteRepository.countByUserId(userId)

  /**
   * Get user's favorite product IDs only (for internal use)
   */
  override fun getFavoriteProductIds(userId: Long): List<Long> = productFavoriteRepository.findFavoriteProductIdsByUserId(userId)

  /**
   * Toggle favorite status (add if not exists, toggle enable field if exists)
   */
  override fun toggleFavorite(
    userId: Long,
    productId: Long,
  ): Boolean {
    val existing = productFavoriteRepository.findByUserIdAndProductId(userId, productId)

    return if (existing != null) {
      // Toggle enable field
      val newEnableStatus = !existing.enable
      val updatedRows = productFavoriteRepository.updateEnableStatus(userId, productId, newEnableStatus)

      // Update the updatedAt timestamp for the record
      if (updatedRows > 0) {
        // Find the updated record and update timestamp
        val updated = productFavoriteRepository.findByUserIdAndProductId(userId, productId)
        updated?.let {
          // Create a new instance with updated fields since val cannot be modified
          val updatedFavorite =
            ProductFavoriteModel(
              id = it.id,
              userId = it.userId,
              productId = it.productId,
              product = it.product,
              enable = newEnableStatus,
              createdAt = it.createdAt,
              updatedAt = LocalDateTime.now(),
              deletedAt = it.deletedAt,
              deletedBy = it.deletedBy,
            )
          productFavoriteRepository.save(updatedFavorite)
        }
      }

      newEnableStatus // Return new enable status (true = enabled, false = disabled)
    } else {
      // Create new favorite with enable = true
      addToFavorites(userId, productId)
      true
    }
  }

  /**
   * Get user's favorite entities (not products) for internal operations
   */
  override fun getUserFavoriteEntities(
    userId: Long,
    pageable: Pageable,
  ): Page<ProductFavoriteModel> = productFavoriteRepository.findByUserId(userId, pageable)

  // ============== Admin Methods ==============

  /**
   * Get comprehensive analytics about user favorites
   */
  override fun getFavoriteAnalytics(): UserFavoriteAnalyticsVo {
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

    return UserFavoriteAnalyticsVo(
      totalFavorites = totalFavorites,
      activeUsersWithFavorites = activeUsers,
      averageFavoritesPerUser = averageFavorites.toFloat(),
      newFavoritesToday = newToday,
      newFavoritesThisWeek = newThisWeek,
      newFavoritesThisMonth = newThisMonth,
    )
  }

  /**
   * Get all products favorite statistics
   *
   * 规则 10:总数走 SQL 聚合(`count(distinct product_id)` / `count(*)`),不再把整个收藏聚合读进内存。
   */
  override fun getAllProductsFavoriteStats(): AllProductsFavoriteStatsVo {
    val totalProducts = productFavoriteRepository.countFavoritedProducts()
    val totalFavorites = productFavoriteRepository.countTotalFavorites()
    val averageFavoritesPerProduct =
      if (totalProducts > 0) {
        totalFavorites.toFloat() / totalProducts
      } else {
        0.0f
      }

    return AllProductsFavoriteStatsVo(
      totalProducts = totalProducts,
      totalFavorites = totalFavorites,
      averageFavoritesPerProduct = averageFavoritesPerProduct,
    )
  }

  /**
   * Get product favorite statistics for each product
   *
   * 规则 10:Top-N 由 SQL limit 决定,商品名批量按 id 取(`findAllById`),不再整表进内存。
   */
  override fun getProductFavoriteStats(): List<ProductFavoriteStatVo> {
    val mostFavorited = productFavoriteRepository.findTopFavoritedProducts(PRODUCT_FAVORITE_STATS_LIMIT)
    if (mostFavorited.isEmpty()) return emptyList()
    val productsById = productRepository.findAllById(mostFavorited.map { it.productId }).associateBy { it.id }

    return mostFavorited.map { favoriteCount ->
      ProductFavoriteStatVo(
        productId = favoriteCount.productId,
        productName = productsById[favoriteCount.productId]?.name ?: UNKNOWN_PRODUCT_NAME,
        favoriteCount = favoriteCount.favoriteCount,
      )
    }
  }

  /**
   * Get products sorted by favorite count with pagination
   *
   * 规则 10:分组/过滤(minFavorites → HAVING)/排序/分页全部下推 SQL;商品与最后收藏时间
   * 一次批量取(原实现是「全量聚合 + 内存切页 + 逐商品 N+1 查询」)。
   */
  override fun getProductsByFavorites(
    page: Int,
    size: Int,
    minFavorites: Int?,
  ): ProductFavoriteVoPage {
    val pageable = PageRequest.of(page, size)
    val favoritesPage =
      productFavoriteRepository.findMostFavoritedProducts(
        pageable = pageable,
        minFavorites = minFavorites?.toLong()?.takeIf { it > 0 },
      )

    val productsById =
      productRepository
        .findAllById(favoritesPage.content.map { it.productId })
        .associateBy { it.id }

    val list =
      favoritesPage.content.mapNotNull { item ->
        val product = productsById[item.productId] ?: return@mapNotNull null
        ProductFavoriteItemVo(
          product = convertToGraphQLProduct(product),
          favoriteCount = item.favoriteCount,
          lastFavoritedAt = item.lastFavoritedAt ?: LocalDateTime.now(),
        )
      }

    return ProductFavoriteVoPage(
      list = list,
      pageInfo =
        PageInfoVo(
          total = favoritesPage.totalElements.toInt(),
          pageSize = size,
          pageNum = page,
          hasNext = favoritesPage.hasNext(),
          hasPrevious = page > 0,
          totalPages = favoritesPage.totalPages,
        ),
    )
  }

  /**
   * Perform batch operations on user favorites
   */
  override fun performBatchOperation(input: BatchFavoriteOperationInput): BatchOperationResult {
    val results = mutableListOf<String>()
    var successCount = 0
    var failureCount = 0

    try {
      when (input.operation) {
        BatchFavoriteOperation.EXPORT_FAVORITES -> {
          results.add("Export functionality initiated")
          successCount = 1
        }

        BatchFavoriteOperation.ANALYZE_FAVORITES -> {
          results.add("Analysis functionality initiated")
          successCount = 1
        }

        else -> {
          throw BadRequestException("Unsupported batch operation: ${input.operation}")
        }
      }
    } catch (e: Exception) {
      failureCount++
      results.add("Operation failed: ${e.message}")
    }

    return BatchOperationResult(
      success = failureCount == 0,
      successCount = successCount,
      failureCount = failureCount,
      message = "Batch operation completed",
      details = mapOf("results" to results),
      errors = emptyList(),
      processedCount = successCount + failureCount,
    )
  }

  /**
   * Export favorites data to CSV/Excel format
   */
  override fun exportFavorites(input: ExportFavoritesInput): ExportFavoritesResult {
    val outputStream = ByteArrayOutputStream()
    var recordCount = 0

    when (input.format) {
      ExportFormat.CSV -> {
        outputStream.use { os ->
          val printer =
            CSVPrinter(
              os.writer(),
              CSVFormat.DEFAULT.withHeader(
                "ID",
                "User ID",
                "User Nickname",
                "Product ID",
                "Product Name",
                "Price (Cents)",
                "Created At",
              ),
            )
          recordCount =
            forEachFavoriteBatch(input) { batch, users, products ->
              batch.forEach { favorite ->
                printer.printRecord(
                  favorite.id,
                  favorite.userId,
                  users[favorite.userId]?.nickname ?: "",
                  favorite.productId,
                  products[favorite.productId]?.name ?: "",
                  products[favorite.productId]?.price ?: 0,
                  favorite.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )
              }
            }
          printer.flush()
        }
      }

      ExportFormat.EXCEL -> {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("用户收藏数据")

        sheet.setColumnWidth(0, 4000)
        sheet.setColumnWidth(1, 5000)
        sheet.setColumnWidth(2, 6000)
        sheet.setColumnWidth(3, 5000)
        sheet.setColumnWidth(4, 8000)
        sheet.setColumnWidth(5, 6000)
        sheet.setColumnWidth(6, 8000)

        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.fontHeightInPoints = 12
        headerFont.setColor(IndexedColors.WHITE.index)
        headerStyle.setFont(headerFont)
        headerStyle.fillForegroundColor = IndexedColors.DARK_BLUE.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        headerStyle.alignment = HorizontalAlignment.CENTER
        headerStyle.verticalAlignment = VerticalAlignment.CENTER
        headerStyle.setBorderBottom(BorderStyle.THIN)
        headerStyle.setBorderTop(BorderStyle.THIN)
        headerStyle.setBorderLeft(BorderStyle.THIN)
        headerStyle.setBorderRight(BorderStyle.THIN)

        val dataStyle = workbook.createCellStyle()
        dataStyle.alignment = HorizontalAlignment.LEFT
        dataStyle.verticalAlignment = VerticalAlignment.CENTER
        dataStyle.setBorderBottom(BorderStyle.THIN)
        dataStyle.setBorderTop(BorderStyle.THIN)
        dataStyle.setBorderLeft(BorderStyle.THIN)
        dataStyle.setBorderRight(BorderStyle.THIN)

        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("用户收藏数据导出")
        val titleStyle = workbook.createCellStyle()
        val titleFont = workbook.createFont()
        titleFont.bold = true
        titleFont.fontHeightInPoints = 16
        titleStyle.setFont(titleFont)
        titleStyle.alignment = HorizontalAlignment.CENTER
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

        val headerRow = sheet.createRow(2)
        val headers = arrayOf("ID", "用户ID", "用户昵称", "商品ID", "商品名称", "价格(分)", "收藏时间")
        headers.forEachIndexed { index, header ->
          val cell = headerRow.createCell(index)
          cell.setCellValue(header)
          cell.cellStyle = headerStyle
        }

        var rowIndex = 3
        recordCount =
          forEachFavoriteBatch(input) { batch, users, products ->
            batch.forEach { favorite ->
              val dataRow = sheet.createRow(rowIndex++)
              val values =
                arrayOf(
                  favorite.id?.toString() ?: "",
                  favorite.userId.toString(),
                  users[favorite.userId]?.nickname ?: "",
                  favorite.productId.toString(),
                  products[favorite.productId]?.name ?: "",
                  (products[favorite.productId]?.price ?: 0).toString(),
                  favorite.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                )
              values.forEachIndexed { colIndex, value ->
                val cell = dataRow.createCell(colIndex)
                cell.setCellValue(value)
                cell.cellStyle = dataStyle
              }
            }
          }

        val summaryRow = sheet.createRow(recordCount + 5)
        val summaryCell = summaryRow.createCell(0)
        summaryCell.setCellValue("总计: $recordCount 条记录")
        val summaryStyle = workbook.createCellStyle()
        val summaryFont = workbook.createFont()
        summaryFont.bold = true
        summaryFont.fontHeightInPoints = 11
        summaryStyle.setFont(summaryFont)
        summaryCell.cellStyle = summaryStyle
        sheet.addMergedRegion(CellRangeAddress(recordCount + 5, recordCount + 5, 0, 2))

        val metadataRow = sheet.createRow(recordCount + 7)
        val metadataCell = metadataRow.createCell(0)
        val exportInfo = "导出时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
        metadataCell.setCellValue(exportInfo)
        metadataCell.cellStyle = dataStyle
        sheet.addMergedRegion(CellRangeAddress(recordCount + 7, recordCount + 7, 0, 6))

        workbook.write(outputStream)
        workbook.close()
      }

      ExportFormat.JSON -> {
        throw BadRequestException("JSON export not yet implemented")
      }
    }

    if (recordCount == 0) {
      return ExportFavoritesResult(
        success = false,
        message = "No data found for the given criteria",
        downloadUrl = null,
        fileName = "",
        fileSize = 0,
        recordCount = 0,
        expiresAt = LocalDateTime.now().plusDays(1),
      )
    }

    val fileExtension =
      when (input.format) {
        ExportFormat.CSV -> "csv"
        ExportFormat.EXCEL -> "xlsx"
        ExportFormat.JSON -> "json"
      }
    val fileName = "favorites_export_${
      LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }_${System.currentTimeMillis()}.$fileExtension"
    val fileSize = outputStream.size().toLong()
    val expiresAt = LocalDateTime.now().plusDays(7)

    return ExportFavoritesResult(
      success = true,
      message = "Export completed successfully",
      downloadUrl = "/api/exports/$fileName",
      fileName = fileName,
      fileSize = fileSize,
      recordCount = recordCount,
      expiresAt = expiresAt,
    )
  }

  /**
   * 按 keyset 游标分批扫描收藏数据(每批 [EXPORT_BATCH_SIZE] 条),批内批量取用户/商品名称。
   * 返回导出行数。AGENTS.md 规则 10:导出必须分批/流式,禁止整表读入内存。
   */
  private inline fun forEachFavoriteBatch(
    input: ExportFavoritesInput,
    consume: (
      batch: List<ProductFavoriteModel>,
      users: Map<Long, UserModel>,
      products: Map<Long, ProductModel>,
    ) -> Unit,
  ): Int {
    var afterId: Long? = null
    var total = 0
    while (true) {
      val batch =
        productFavoriteRepository.findFavoritesForExport(
          userId = input.userId,
          productIds = input.productIds?.toList(),
          dateFrom = input.dateFrom,
          dateTo = input.dateTo,
          afterId = afterId,
          limit = EXPORT_BATCH_SIZE,
        )
      if (batch.isEmpty()) break
      val users = userService.findByIds(batch.map { it.userId }.distinct()).associateBy { requireNotNull(it.id) }
      val products =
        productRepository
          .findAllById(batch.map { it.productId }.distinct())
          .associateBy { requireNotNull(it.id) }
      consume(batch, users, products)
      total += batch.size
      afterId = batch.last().id
      if (batch.size < EXPORT_BATCH_SIZE) break
    }
    return total
  }

  // Private helper methods

  private fun convertToGraphQLProduct(product: ProductModel): ProductVo = product.toGraphQL()

  private fun convertProductStatus(status: ProductStatus): ProductStatus = status

  private companion object {
    /** 收藏排行展示条数(SQL 侧 limit,避免全量聚合进内存 —— AGENTS.md 规则 10) */
    const val PRODUCT_FAVORITE_STATS_LIMIT = 10

    /** 商品已删除/不存在时的展示占位 */
    const val UNKNOWN_PRODUCT_NAME = "未知商品"

    /** 导出分批大小(keyset 游标) */
    const val EXPORT_BATCH_SIZE = 1000
  }
}
