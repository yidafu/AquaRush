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

package dev.yidafu.aqua.storage.service

import dev.yidafu.aqua.storage.config.ImageProcessingProperties
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.core.io.ByteArrayResource
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 图片处理服务单元测试: 参数校验、缩放/质量/水印、原图直通、缓存命中与回源
 */
class ImageProcessingServiceTest :
  FunSpec({
    var storageStrategy: StorageStrategy = mockk()
    var service: ImageProcessingService = mockk()
    var originalPng: ByteArray = byteArrayOf()

    fun createPng(
      w: Int,
      h: Int,
    ): ByteArray {
      val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
      val out = ByteArrayOutputStream()
      ImageIO.write(image, "png", out)
      return out.toByteArray()
    }

    beforeTest {
      storageStrategy = mockk(relaxed = true)
      val props =
        ImageProcessingProperties().apply {
          defaultQuality = 0.8f
        }
      service = ImageProcessingService(storageStrategy, props)
      originalPng = createPng(10, 10)
    }
    test("processImage rejects invalid parameters") {
      shouldThrow<IllegalArgumentException> {
        service.processImage(ByteArrayResource(originalPng), ImageParameters(width = 0))
      }
      shouldThrow<IllegalArgumentException> {
        service.processImage(ByteArrayResource(originalPng), ImageParameters(quality = 0.05f))
      }
      shouldThrow<IllegalArgumentException> {
        service.processImage(ByteArrayResource(originalPng), ImageParameters(format = "TIFF"))
      }
    }
    test("processImage produces output for plain resize") {
      val result = service.processImage(ByteArrayResource(originalPng), ImageParameters(width = 5))

      result.isNotEmpty() shouldBe true
    }
    test("processImage produces output with height only") {
      val result = service.processImage(ByteArrayResource(originalPng), ImageParameters(height = 7))

      result.isNotEmpty() shouldBe true
    }
    test("processImage produces output with width and height") {
      val result = service.processImage(ByteArrayResource(originalPng), ImageParameters(width = 5, height = 5))

      result.isNotEmpty() shouldBe true
    }
    test("processImage produces output with quality and watermark") {
      val result =
        service.processImage(
          ByteArrayResource(originalPng),
          ImageParameters(width = 6, quality = 0.5f, watermark = true, watermarkText = "AquaRush"),
        )

      result.isNotEmpty() shouldBe true
    }
    test("getOrProcessImage returns original when no parameters given") {
      val original = byteArrayOf(1, 2, 3)
      every { storageStrategy.retrieve("path") } returns ByteArrayResource(original)

      val result = service.getOrProcessImage("path", ImageParameters())

      result.size shouldBe 3
      verify(exactly = 0) { storageStrategy.exists(any()) }
    }
    test("getOrProcessImage returns cached image when cache hit") {
      val cached = byteArrayOf(9, 9)
      every { storageStrategy.exists(any()) } returns true
      every { storageStrategy.retrieve(any()) } returns ByteArrayResource(cached)

      val result = service.getOrProcessImage("path", ImageParameters(width = 5))

      result.size shouldBe 2
      result[0] shouldBe 9
      verify(exactly = 0) { storageStrategy.store(any(), any()) }
    }
    test("getOrProcessImage processes and caches on miss") {
      every { storageStrategy.exists(any()) } returns false
      every { storageStrategy.retrieve("path") } returns ByteArrayResource(originalPng)

      val result = service.getOrProcessImage("path", ImageParameters(width = 5))

      result.isNotEmpty() shouldBe true
      verify { storageStrategy.store(any(), any()) }
    }
    test("generateCacheKey includes all parameter dimensions") {
      val keyA =
        ImageParameters(
          width = 100,
          height = 50,
          quality = 0.8f,
          format = "JPEG",
          watermark = true,
          watermarkText = "AquaRush",
        ).generateCacheKey()
      val keyB =
        ImageParameters(
          width = 100,
          height = 50,
          quality = 0.8f,
          format = "JPEG",
          watermark = true,
          watermarkText = "AquaRush",
        ).generateCacheKey()
      val keyC =
        ImageParameters(
          width = 200,
          height = 50,
          quality = 0.8f,
          format = "JPEG",
          watermark = true,
          watermarkText = "AquaRush",
        ).generateCacheKey()

      keyB shouldBe keyA
      (keyA != keyC) shouldBe true
    }
    test("validate rejects negative height and out-of-range quality") {
      ImageParameters(height = -1).validate() shouldBe false
      ImageParameters(quality = 1.5f).validate() shouldBe false
      ImageParameters(width = 10, quality = 0.9f, format = "png").validate() shouldBe true
    }
  })
