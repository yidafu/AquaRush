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

package dev.yidafu.aqua.storage.service.storage

import dev.yidafu.aqua.storage.config.StorageProperties
import dev.yidafu.aqua.storage.domain.model.FileMetadata
import dev.yidafu.aqua.storage.domain.model.enums.FileType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * 本地存储策略单元测试: 按类型/年月落盘、读取、删除、URL 生成、文件大小
 */
class LocalStorageStrategyTest :
  FunSpec({
    var tempDir: Path = mockk()
    var strategy: LocalStorageStrategy = mockk()

    fun file(
      name: String = "a.jpg",
      bytes: ByteArray = byteArrayOf(1, 2, 3, 4),
    ): MultipartFile =
      object : MultipartFile {
        override fun getInputStream(): java.io.InputStream = ByteArrayInputStream(bytes)

        override fun getName(): String = "file"

        override fun getOriginalFilename(): String? = name

        override fun getContentType(): String? = "image/jpeg"

        override fun isEmpty(): Boolean = bytes.isEmpty()

        override fun getSize(): Long = bytes.size.toLong()

        override fun getBytes(): ByteArray = bytes

        override fun transferTo(dest: java.io.File) {
          dest.writeBytes(bytes)
        }
      }

    fun metadata(fileType: FileType = FileType.IMAGE): FileMetadata =
      FileMetadata().apply {
        fileName = "a.jpg"
        this.fileType = fileType
      }

    beforeTest { tempDir = Files.createTempDirectory("aqua-storage-test") }
    beforeTest {
      val props =
        StorageProperties().apply {
          local.basePath = tempDir.toString()
          local.baseUrl = "http://localhost:9090"
        }
      strategy = LocalStorageStrategy(props)
    }
    afterTest {
      tempDir.toFile().deleteRecursively()
    }
    test("store writes file under type and returns relative path") {
      val path = strategy.store(file(), metadata())

      path.startsWith("image/") shouldBe true
      path.endsWith(".jpg") shouldBe true
      withClue("stored file should exist") { Files.exists(tempDir.resolve(path)) shouldBe true }
    }
    test("retrieve returns file content") {
      val path = strategy.store(file(name = "b.png", bytes = byteArrayOf(9, 8, 7)), metadata(FileType.IMAGE))

      val resource = strategy.retrieve(path)

      resource.contentLength() shouldBe 3
      resource.inputStream.read() shouldBe 9
    }
    test("retrieve throws for missing file") {
      shouldThrow<RuntimeException> {
        strategy.retrieve("image/2025/06/nope.jpg")
      }
    }
    test("delete removes file and returns true") {
      val path = strategy.store(file(), metadata())

      val deleted = strategy.delete(path)

      deleted shouldBe true
      Files.exists(tempDir.resolve(path)) shouldBe false
    }
    test("delete returns false when file missing") {
      strategy.delete("image/2025/06/never.jpg") shouldBe false
    }
    test("generateUrl builds download url with id and name") {
      val url = strategy.generateUrl(42L, "a.jpg")

      url shouldBe "http://localhost:9090/api/v1/storage/files/42?name=a.jpg"
    }
    test("exists reflects file presence") {
      val path = strategy.store(file(), metadata())

      strategy.exists(path) shouldBe true
      strategy.exists("image/2025/06/ghost.jpg") shouldBe false
    }
    test("getFileSize returns stored size") {
      val path = strategy.store(file(bytes = ByteArray(123)), metadata())

      strategy.getFileSize(path) shouldBe 123L
    }
    test("getFileSize returns zero for missing file") {
      strategy.getFileSize("image/2025/06/ghost.jpg") shouldBe 0L
    }
    test("store handles file without extension") {
      val path = strategy.store(file(name = "noext", bytes = byteArrayOf(5)), metadata(FileType.DOCUMENT))

      path.startsWith("document/") shouldBe true
      path.endsWith(".") shouldBe false
      Files.exists(tempDir.resolve(path)) shouldBe true
    }
  })
