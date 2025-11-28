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
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * 图片处理服务
 */
@Service
class ImageProcessingService(
    private val storageStrategy: StorageStrategy,
    private val imageProcessingProperties: ImageProcessingProperties
) {

    /**
     * 处理图片
     * @param originalResource 原始图片资源
     * @param parameters 图片处理参数
     * @return 处理后的图片字节数组
     */
    fun processImage(originalResource: Resource, parameters: ImageParameters): ByteArray {
        if (!parameters.validate()) {
            throw IllegalArgumentException("Invalid image processing parameters")
        }

        return try {
            val originalImage = ImageIO.read(originalResource.inputStream)
            var thumbnailBuilder = Thumbnails.of(originalImage)

            // 调整大小
            if (parameters.width != null || parameters.height != null) {
                thumbnailBuilder = when {
                    parameters.width != null && parameters.height != null -> {
                        thumbnailBuilder.size(parameters.width, parameters.height)
                    }
                    parameters.width != null -> {
                        thumbnailBuilder.width(parameters.width)
                    }
                    else -> {
                        thumbnailBuilder.height(parameters.height ?: 200)
                    }
                }
            }

            // 调整质量
            val quality = parameters.quality ?: imageProcessingProperties.defaultQuality
            thumbnailBuilder.outputQuality(quality.toDouble())

            // 格式转换
            val format = parameters.format?.uppercase() ?: "JPEG"

            // 添加水印
            if (parameters.watermark) {
                thumbnailBuilder = thumbnailBuilder.watermark(
                    Positions.BOTTOM_RIGHT,
                    createWatermarkImage(parameters.watermarkText ?: "AquaRush"),
                    0.5f
                )
            }

            // 生成处理后的图片
            val outputStream = ByteArrayOutputStream()
            thumbnailBuilder.toOutputStream(outputStream)

            outputStream.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException("Failed to process image", e)
        }
    }

    /**
     * 获取或生成处理后的图片
     * @param originalPath 原始图片路径
     * @param parameters 图片处理参数
     * @return 处理后的图片字节数组
     */
    fun getOrProcessImage(originalPath: String, parameters: ImageParameters): ByteArray {
        // 如果没有处理参数，直接返回原始图片
        if (parameters.width == null &&
            parameters.height == null &&
            parameters.quality == null &&
            parameters.format == null &&
            !parameters.watermark) {
            return storageStrategy.retrieve(originalPath).inputStream.readBytes()
        }

        // 生成缓存文件路径
        val cacheKey = generateCacheKey(originalPath, parameters)
        val cachePath = generateCachePath(cacheKey, parameters.format)

        // 检查缓存是否存在
        if (storageStrategy.exists(cachePath)) {
            return storageStrategy.retrieve(cachePath).inputStream.readBytes()
        }

        // 处理图片
        val originalResource = storageStrategy.retrieve(originalPath)
        val processedImage = processImage(originalResource, parameters)

        // 缓存处理后的图片（异步）
        cacheProcessedImage(cachePath, processedImage)

        return processedImage
    }

    /**
     * 创建水印图片
     */
    private fun createWatermarkImage(text: String): BufferedImage {
        val width = 200
        val height = 50
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        try {
            graphics.background = Color(255, 255, 255, 0)
            graphics.clearRect(0, 0, width, height)

            graphics.color = Color(255, 255, 255, 180)
            graphics.font = Font("Arial", Font.BOLD, 20)

            val metrics = graphics.fontMetrics
            val x = (width - metrics.stringWidth(text)) / 2
            val y = (height - metrics.height) / 2 + metrics.ascent

            graphics.drawString(text, x, y)
        } finally {
            graphics.dispose()
        }

        return image
    }

    /**
     * 生成缓存键
     */
    private fun generateCacheKey(originalPath: String, parameters: ImageParameters): String {
        val combined = "$originalPath:${parameters.generateCacheKey()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 生成缓存路径
     */
    private fun generateCachePath(cacheKey: String, format: String?): String {
        val extension = format?.lowercase() ?: "jpg"
        return "cache/images/${cacheKey.substring(0, 2)}/$cacheKey.$extension"
    }

    /**
     * 缓存处理后的图片
     */
    private fun cacheProcessedImage(cachePath: String, processedImage: ByteArray) {
        try {
            val tempFile = ByteArrayInputStream(processedImage)
            val tempMetadata = createTempFileMetadata(cachePath)
            storageStrategy.store(
                object : org.springframework.web.multipart.MultipartFile {
                    override fun getInputStream() = tempFile
                    override fun getName() = "temp"
                    override fun getOriginalFilename() = cachePath.split("/").last()
                    override fun getContentType() = "image/jpeg"
                    override fun isEmpty() = processedImage.isEmpty()
                    override fun getSize() = processedImage.size.toLong()
                    override fun getBytes() = processedImage
                    override fun transferTo(dest: java.io.File) {
                        dest.writeBytes(processedImage)
                    }
                },
                tempMetadata
            )
        } catch (e: Exception) {
            // 缓存失败不应该影响主要的图片处理功能
            // 可以记录日志，但不会抛出异常
            println("Failed to cache processed image: $cachePath")
        }
    }

    /**
     * 创建临时文件元数据
     */
    private fun createTempFileMetadata(cachePath: String): dev.yidafu.aqua.storage.domain.entity.FileMetadata {
        return dev.yidafu.aqua.storage.domain.entity.FileMetadata(
            fileName = cachePath.split("/").last(),
            storagePath = cachePath,
            fileType = dev.yidafu.aqua.storage.domain.enums.FileType.IMAGE,
            fileSize = 0L, // 这将在实际存储时更新
            mimeType = "image/jpeg",
            checksum = "temp"
        )
    }
}
