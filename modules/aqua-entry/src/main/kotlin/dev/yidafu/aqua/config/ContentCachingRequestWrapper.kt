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

package dev.yidafu.aqua.config

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.util.StreamUtils
import java.io.*
import java.nio.charset.StandardCharsets

class ContentCachingRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
  private val cachedContent: ByteArrayOutputStream = ByteArrayOutputStream()

  init {
    // Cache the request body
    request.inputStream.use { inputStream ->
      StreamUtils.copy(inputStream, cachedContent)
    }
  }

  override fun getInputStream(): ServletInputStream {
    return CachedServletInputStream(ByteArrayInputStream(cachedContent.toByteArray()))
  }

  override fun getReader(): BufferedReader {
    return BufferedReader(InputStreamReader(inputStream, getCharacterEncoding() ?: StandardCharsets.UTF_8.name()))
  }

  fun getContentAsByteArray(): ByteArray = cachedContent.toByteArray()

  private inner class CachedServletInputStream(private val cachedInputStream: InputStream) : ServletInputStream() {
    override fun isFinished(): Boolean = try {
      cachedInputStream.available() == 0
    } catch (e: IOException) {
      true
    }

    override fun isReady(): Boolean = true

    override fun setReadListener(readListener: ReadListener) {
      throw UnsupportedOperationException()
    }

    override fun read(): Int = cachedInputStream.read()

    override fun read(b: ByteArray): Int = cachedInputStream.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = cachedInputStream.read(b, off, len)
  }
}