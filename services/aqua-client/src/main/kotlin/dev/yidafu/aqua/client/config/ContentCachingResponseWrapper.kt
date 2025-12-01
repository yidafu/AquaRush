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

package dev.yidafu.aqua.client.config

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.springframework.util.StreamUtils
import java.io.*

class ContentCachingResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {
  private val cachedContent = ByteArrayOutputStream()
  private val contentOutputStream: CachedContentServletOutputStream

  init {
    contentOutputStream = CachedContentServletOutputStream(response.outputStream, cachedContent)
  }

  override fun getOutputStream(): ServletOutputStream = contentOutputStream

  override fun getWriter(): PrintWriter {
    return PrintWriter(OutputStreamWriter(contentOutputStream, getCharacterEncoding() ?: "UTF-8"))
  }

  fun getContentAsByteArray(): ByteArray = cachedContent.toByteArray()

  fun copyBodyToResponse() {
    try {
      // Write cached content to the actual response
      val content = cachedContent.toByteArray()
      if (content.isNotEmpty()) {
        response.outputStream.write(content)
        response.outputStream.flush()
      }
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
  }

  private inner class CachedContentServletOutputStream(
    private val originalOutputStream: ServletOutputStream,
    private val cachedContent: ByteArrayOutputStream,
  ) : ServletOutputStream() {
    override fun isReady(): Boolean = originalOutputStream.isReady

    override fun setWriteListener(writeListener: WriteListener) {
      originalOutputStream.setWriteListener(writeListener)
    }

    override fun write(b: Int) {
      cachedContent.write(b)
      originalOutputStream.write(b)
    }

    override fun write(b: ByteArray) {
      cachedContent.write(b)
      originalOutputStream.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
      cachedContent.write(b, off, len)
      originalOutputStream.write(b, off, len)
    }

    override fun flush() {
      cachedContent.flush()
      originalOutputStream.flush()
    }

    override fun close() {
      cachedContent.close()
      originalOutputStream.close()
    }
  }
}
