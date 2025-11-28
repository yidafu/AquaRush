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

package dev.yidafu.aqua.storage.dto

/**
 * 图片处理参数
 */
data class ImageParameters(
    /**
     * 图片宽度
     */
    val width: Int? = null,

    /**
     * 图片高度
     */
    val height: Int? = null,

    /**
     * 图片质量 (0.1-1.0)
     */
    val quality: Float? = null,

    /**
     * 输出格式 (JPEG, PNG, WEBP等)
     */
    val format: String? = null,

    /**
     * 是否添加水印
     */
    val watermark: Boolean = false,

    /**
     * 水印文本（当watermark为true时使用）
     */
    val watermarkText: String? = null
) {
    /**
     * 验证参数有效性
     */
    fun validate(): Boolean {
        // 验证宽度和高度
        if (width != null && width <= 0) return false
        if (height != null && height <= 0) return false

        // 验证质量范围
        if (quality != null && (quality < 0.1f || quality > 1.0f)) return false

        // 验证格式
        if (format != null) {
            val supportedFormats = setOf("JPEG", "JPG", "PNG", "WEBP", "GIF", "BMP")
            if (!supportedFormats.contains(format.uppercase())) return false
        }

        return true
    }

    /**
     * 生成处理参数的唯一标识符
     */
    fun generateCacheKey(): String {
        return "${width}_${height}_${quality}_${format?.uppercase()}_${watermark}_${watermarkText}"
    }
}
