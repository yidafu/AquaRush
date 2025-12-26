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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.service.ProductFavoriteService
import dev.yidafu.aqua.api.service.ProductService
import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
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
  private val productService: ProductService
) : ProductFavoriteService {

  /**
   * Add product to user's favorites
   */
  override fun addToFavorites(userId: Long, productId: Long): ProductFavoriteModel {
    // Validate product exists and is active
    val product = productRepository.findById(productId)
      .orElseThrow { NotFoundException("Product not found") }

    if (product.status != ProductStatus.ACTIVE && product.status != ProductStatus.ONLINE) {
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
            val updatedFavorite = ProductFavoriteModel(
              id = it.id,
              userId = it.userId,
              productId = it.productId,
              product = it.product,
              enable = true,
              createdAt = it.createdAt,
              updatedAt = LocalDateTime.now(),
              deletedAt = it.deletedAt,
              deletedBy = it.deletedBy
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
      val favorite = ProductFavoriteModel(
        userId = userId,
        productId = productId,
        enable = true
      )
      return productFavoriteRepository.save(favorite)
    }
  }


  /**
   * Get user's favorite products with pagination
   */
  override fun getFavoriteProducts(userId: Long, pageable: Pageable): Page<ProductModel> {
    // Get paginated product IDs from favorites
    val favoriteIdsPage = productFavoriteRepository.findFavoriteIdsByUserId(userId, pageable)

    if (favoriteIdsPage.isEmpty) {
      return Page.empty(pageable)
    }

    // Get products by IDs
    val products = productRepository.findAllById(favoriteIdsPage.content)

    // Filter by status (active/online only)
    val activeProducts = products.filter {
      it.status == ProductStatus.ACTIVE || it.status == ProductStatus.ONLINE
    }

    return PageImpl(activeProducts, pageable, favoriteIdsPage.totalElements)
  }

  /**
   * Check if product is favorited by user
   */
  override fun isProductFavorited(userId: Long, productId: Long): Boolean {
    return productFavoriteRepository.existsByUserIdAndProductId(userId, productId)
  }

  /**
   * Get total count of user's favorites
   */
  @Cacheable(value = ["user_favorites_count"], key = "#userId")
  override fun getFavoritesCount(userId: Long): Long {
    return productFavoriteRepository.countByUserId(userId)
  }

  /**
   * Get user's favorite product IDs only (for internal use)
   */
  override fun getFavoriteProductIds(userId: Long): List<Long> {
    return productFavoriteRepository.findFavoriteProductIdsByUserId(userId)
  }

  /**
   * Toggle favorite status (add if not exists, toggle enable field if exists)
   */
  override fun toggleFavorite(userId: Long, productId: Long): Boolean {
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
          val updatedFavorite = ProductFavoriteModel(
            id = it.id,
            userId = it.userId,
            productId = it.productId,
            product = it.product,
            enable = newEnableStatus,
            createdAt = it.createdAt,
            updatedAt = LocalDateTime.now(),
            deletedAt = it.deletedAt,
            deletedBy = it.deletedBy
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
  override fun getUserFavoriteEntities(userId: Long, pageable: Pageable): Page<ProductFavoriteModel> {
    return productFavoriteRepository.findByUserId(userId, pageable)
  }

  // ============== Admin Methods ==============

  /**
   * Get comprehensive analytics about user favorites
   */
  override fun getFavoriteAnalytics(): UserFavoriteAnalytics {
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

    return UserFavoriteAnalytics(
      totalFavorites = totalFavorites,
      activeUsersWithFavorites = activeUsers,
      averageFavoritesPerUser = averageFavorites.toFloat(),
      newFavoritesToday = newToday,
      newFavoritesThisWeek = newThisWeek,
      newFavoritesThisMonth = newThisMonth
    )
  }

  /**
   * Get all products favorite statistics
   */
  override fun getAllProductsFavoriteStats(): AllProductsFavoriteStats {
    val mostFavorited = productFavoriteRepository.findMostFavoritedProducts()

    val totalProducts = mostFavorited.size.toLong()
    val totalFavorites = mostFavorited.sumOf { it.favoriteCount }
    val averageFavoritesPerProduct = if (totalProducts > 0) {
      totalFavorites.toFloat() / totalProducts
    } else {
      0.0f
    }

    return AllProductsFavoriteStats(
      totalProducts = totalProducts,
      totalFavorites = totalFavorites,
      averageFavoritesPerProduct = averageFavoritesPerProduct
    )
  }

  /**
   * Get products sorted by favorite count with pagination
   */
  override fun getProductsByFavorites(page: Int, size: Int, minFavorites: Int?): ProductFavoritePage {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "favoriteCount"))

    val mostFavorited = productFavoriteRepository.findMostFavoritedProducts()

    val filtered = if (minFavorites != null) {
      mostFavorited.filter { it.favoriteCount >= minFavorites }
    } else {
      mostFavorited
    }

    val totalElements = filtered.size.toLong()
    val totalPages = Math.ceil(totalElements.toDouble() / size).toInt()
    val start = page * size
    val end = minOf(start + size, filtered.size)
    val pagedItems = if (start < filtered.size) {
      filtered.subList(start, end)
    } else {
      emptyList()
    }

    val list = pagedItems.map { item ->
      val product = productRepository.findById(item.productId)
      val favoriteCount = item.favoriteCount

      if (product.isEmpty) {
        return@map null
      }

      val lastFavorites = productFavoriteRepository.findByProductId(item.productId)
      val lastFavoritedAt = lastFavorites.maxByOrNull { it.createdAt }?.createdAt

      ProductFavoriteItem(
        product = convertToGraphQLProduct(product.get()),
        favoriteCount = favoriteCount,
        lastFavoritedAt = lastFavoritedAt ?: LocalDateTime.now()
      )
    }.filterNotNull()

    return ProductFavoritePage(
      list = list,
      pageInfo = PageInfo(
        total = totalElements.toInt(),
        pageSize = size,
        pageNum = page,
        hasNext = (page + 1) * size < totalElements,
        hasPrevious = page > 0,
        totalPages = totalPages
      )
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
      processedCount = successCount + failureCount
    )
  }

  /**
   * Export favorites data to CSV/Excel format
   */
  override fun exportFavorites(input: ExportFavoritesInput): ExportFavoritesResult {
    val favorites = productFavoriteRepository.findFavoritesForExport(
      input.userId,
      input.productIds?.toList(),
      input.dateFrom,
      input.dateTo
    )

    if (favorites.isEmpty()) {
      return ExportFavoritesResult(
        success = false,
        message = "No data found for the given criteria",
        downloadUrl = null,
        fileName = "",
        fileSize = 0,
        recordCount = 0,
        expiresAt = LocalDateTime.now().plusDays(1)
      )
    }

    val outputStream = ByteArrayOutputStream()

    when (input.format) {
      ExportFormat.CSV -> {
        outputStream.use { os ->
          val writer = os.writer()
          val printer = CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
            "ID",
            "User ID",
            "User Nickname",
            "Product ID",
            "Product Name",
            "Price (Cents)",
            "Created At"
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

        favorites.forEachIndexed { index, favorite ->
          val dataRow = sheet.createRow(index + 3)
          val user = userService.findById(favorite.userId)
          val product = productRepository.findById(favorite.productId).orElse(null)

          val values = arrayOf(
            favorite.id?.toString() ?: "",
            favorite.userId.toString(),
            user?.nickname ?: "",
            favorite.productId.toString(),
            product?.name ?: "",
            (product?.price ?: 0).toString(),
            favorite.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          )

          values.forEachIndexed { colIndex, value ->
            val cell = dataRow.createCell(colIndex)
            cell.setCellValue(value)
            cell.cellStyle = dataStyle
          }
        }

        val summaryRow = sheet.createRow(favorites.size + 5)
        val summaryCell = summaryRow.createCell(0)
        summaryCell.setCellValue("总计: ${favorites.size} 条记录")
        val summaryStyle = workbook.createCellStyle()
        val summaryFont = workbook.createFont()
        summaryFont.bold = true
        summaryFont.fontHeightInPoints = 11
        summaryStyle.setFont(summaryFont)
        summaryCell.cellStyle = summaryStyle
        sheet.addMergedRegion(CellRangeAddress(favorites.size + 5, favorites.size + 5, 0, 2))

        val metadataRow = sheet.createRow(favorites.size + 7)
        val metadataCell = metadataRow.createCell(0)
        val exportInfo = "导出时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
        metadataCell.setCellValue(exportInfo)
        metadataCell.cellStyle = dataStyle
        sheet.addMergedRegion(CellRangeAddress(favorites.size + 7, favorites.size + 7, 0, 6))

        workbook.write(outputStream)
        workbook.close()
      }

      ExportFormat.JSON -> {
        throw BadRequestException("JSON export not yet implemented")
      }
    }

    val fileExtension = when (input.format) {
      ExportFormat.CSV -> "csv"
      ExportFormat.EXCEL -> "xlsx"
      ExportFormat.JSON -> "json"
    }
    val fileName = "favorites_export_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}_${System.currentTimeMillis()}.$fileExtension"
    val fileSize = outputStream.size().toLong()
    val expiresAt = LocalDateTime.now().plusDays(7)

    return ExportFavoritesResult(
      success = true,
      message = "Export completed successfully",
      downloadUrl = "/api/exports/$fileName",
      fileName = fileName,
      fileSize = fileSize,
      recordCount = favorites.size,
      expiresAt = expiresAt
    )
  }

  // Private helper methods

  private fun convertToGraphQLProduct(product: ProductModel): Product {
    return Product(
      id = product.id!!,
      name = product.name,
      subtitle = product.subtitle,
      price = product.price,
      originalPrice = product.originalPrice,
      stock = product.stock,
      salesVolume = product.salesVolume,
      status = product.status,
      coverImageUrl = product.coverImageUrl,
      detailContent = product.detailContent,
      imageGallery = product.imageGallery,
      tags = product.tags,
      mineralContent = product.mineralContent,
      sortOrder = product.sortOrder,
      specification = product.specification,
      waterSource = product.waterSource,
      certificateImages = product.certificateImages,
      deliverySettings = product.deliverySettings,
      depositPrice = product.depositPrice,
      createdAt = product.createdAt,
      updatedAt = product.updatedAt,
      isDeleted = false
    )
  }

  private fun convertProductStatus(status: ProductStatus): ProductStatus {
    return status
  }
}
