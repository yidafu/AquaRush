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

package dev.yidafu.aqua.storage.service

import dev.yidafu.aqua.storage.config.ImageProcessingProperties
import dev.yidafu.aqua.storage.dto.ImageParameters
import dev.yidafu.aqua.storage.service.storage.StorageStrategy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.ByteArrayResource
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageProcessingServiceTest {

    private lateinit var storageStrategy: StorageStrategy
    private lateinit var imageProcessingProperties: ImageProcessingProperties
    private lateinit var imageProcessingService: ImageProcessingService

    @BeforeEach
    fun setUp() {
        storageStrategy = mockk()
        imageProcessingProperties = ImageProcessingProperties(
            defaultQuality = 0.8f,
            maxWidth = 4096,
            maxHeight = 4096,
            supportedFormats = setOf("JPEG", "PNG", "WEBP")
        )
        imageProcessingService = ImageProcessingService(storageStrategy, imageProcessingProperties)
    }

    @Test
    fun `should process image with resize parameters`() {
        // Given
        val originalImage = createTestImage(800, 600)
        val imageResource = ByteArrayResource(imageToBytes(originalImage))
        val parameters = ImageParameters(
            width = 400,
            height = 300,
            quality = 0.7f,
            format = "JPEG"
        )

        // When
        val result = imageProcessingService.processImage(imageResource, parameters)

        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should throw exception for invalid parameters`() {
        // Given
        val imageResource = ByteArrayResource(createTestImage(100, 100).let { imageToBytes(it) })
        val invalidParameters = ImageParameters(
            width = -100,
            quality = 1.5f
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            imageProcessingService.processImage(imageResource, invalidParameters)
        }
    }

    @Test
    fun `should return original image when no processing parameters provided`() {
        // Given
        val originalImage = createTestImage(200, 200)
        val imageBytes = imageToBytes(originalImage)
        val imageResource = ByteArrayResource(imageBytes)
        val parameters = ImageParameters()
        val storagePath = "test/image.jpg"

        every { storageStrategy.exists(any()) } returns false
        every { storageStrategy.retrieve(storagePath) } returns imageResource

        // When
        val result = imageProcessingService.getOrProcessImage(storagePath, parameters)

        // Then
        assertArrayEquals(imageBytes, result)
    }

    @Test
    fun `should add watermark when requested`() {
        // Given
        val originalImage = createTestImage(800, 600)
        val imageResource = ByteArrayResource(imageToBytes(originalImage))
        val parameters = ImageParameters(
            width = 400,
            watermark = true,
            watermarkText = "Test Watermark"
        )

        // When
        val result = imageProcessingService.processImage(imageResource, parameters)

        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    private fun createTestImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.background = java.awt.Color.WHITE
        graphics.clearRect(0, 0, width, height)
        graphics.color = java.awt.Color.BLUE
        graphics.fillRect(50, 50, width - 100, height - 100)
        graphics.dispose()
        return image
    }

    private fun imageToBytes(image: BufferedImage, format: String = "JPEG"): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }
}
