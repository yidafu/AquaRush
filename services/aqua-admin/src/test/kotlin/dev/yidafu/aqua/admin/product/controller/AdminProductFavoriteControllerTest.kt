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

package dev.yidafu.aqua.admin.product.controller

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.common.graphql.generated.ExportFavoritesResult
import dev.yidafu.aqua.common.graphql.generated.ExportFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminProductFavoriteControllerTest {
  private lateinit var productFavoriteService: ProductFavoriteService
  private lateinit var controller: AdminProductFavoriteController

  private fun successResult(): ExportFavoritesResult =
    ExportFavoritesResult(
      success = true,
      message = "导出成功",
      downloadUrl = "http://x/download.csv",
      fileName = "favorites.csv",
      fileSize = 1024,
      recordCount = 10,
      expiresAt = LocalDateTime.now().plusDays(1),
    )

  @BeforeEach
  fun setUp() {
    productFavoriteService = mockk(relaxed = true)
    controller = AdminProductFavoriteController(productFavoriteService)
  }

  @Test
  fun `exportFavorites should export successfully`() {
    // Given
    every { productFavoriteService.exportFavorites(any()) } returns successResult()
    val request = ExportFavoritesRequest(format = ExportFormat.CSV, userId = 1L)

    // When
    val result: ResponseEntity<ExportFavoritesResult> = controller.exportFavorites(request)

    // Then
    assertTrue(result.body!!.success)
    assertEquals(10, result.body!!.recordCount)
    verify { productFavoriteService.exportFavorites(any()) }
  }

  @Test
  fun `exportFavorites should reject inverted date range`() {
    // Given
    val request =
      ExportFavoritesRequest(
        dateFrom = LocalDateTime.of(2025, 6, 2, 0, 0),
        dateTo = LocalDateTime.of(2025, 6, 1, 0, 0),
      )

    // When
    val result: ResponseEntity<ExportFavoritesResult> = controller.exportFavorites(request)

    // Then
    assertFalse(result.body!!.success)
    assertTrue(result.body!!.message.contains("开始时间不能晚于结束时间"))
  }

  @Test
  fun `exportFavorites should return failure result on service error`() {
    // Given
    every { productFavoriteService.exportFavorites(any()) } throws RuntimeException("boom")

    // When
    val result: ResponseEntity<ExportFavoritesResult> = controller.exportFavorites(ExportFavoritesRequest())

    // Then
    assertFalse(result.body!!.success)
    assertTrue(result.body!!.message.contains("导出失败"))
  }
}
