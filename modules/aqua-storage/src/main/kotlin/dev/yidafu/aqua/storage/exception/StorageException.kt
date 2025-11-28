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

package dev.yidafu.aqua.storage.exception

/**
 * 存储相关异常基类
 */
open class StorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 文件存储异常
 */
class FileStorageException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * 文件不存在异常
 */
class FileNotFoundException(message: String) : StorageException(message)

/**
 * 文件大小超出限制异常
 */
class FileSizeExceededException(message: String) : StorageException(message)

/**
 * 不支持的文件类型异常
 */
class UnsupportedFileTypeException(message: String) : StorageException(message)

/**
 * 图片处理异常
 */
class ImageProcessingException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * 存储空间不足异常
 */
class InsufficientStorageException(message: String) : StorageException(message)

/**
 * 文件校验失败异常
 */
class FileValidationException(message: String) : StorageException(message)
