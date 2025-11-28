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

package dev.yidafu.aqua.storage.domain.enums

/**
 * 文件类型枚举
 */
enum class FileType {
    /**
     * 图片文件 (jpg, jpeg, png, gif, bmp, webp等)
     */
    IMAGE,

    /**
     * 视频文件 (mp4, avi, mov, wmv, flv等)
     */
    VIDEO,

    /**
     * 音频文件 (mp3, wav, flac, aac等)
     */
    AUDIO,

    /**
     * 文档文件 (pdf, doc, docx, txt等)
     */
    DOCUMENT,

    /**
     * 电子表格文件 (xls, xlsx, csv等)
     */
    SPREADSHEET,

    /**
     * 演示文稿文件 (ppt, pptx等)
     */
    PRESENTATION,

    /**
     * 前端资源文件 (html, css, js等)
     */
    FRONTEND,

    /**
     * 压缩包文件 (zip, rar, 7z, tar, gz, tar.gz等)
     */
    ARCHIVE,

    /**
     * 可执行文件 (exe, msi, sh, bat等)
     */
    EXECUTABLE,

    /**
     * 备份文件 (bak, backup等)
     */
    BACKUP,

    /**
     * 其他文件
     */
    OTHER
}
