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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.FileType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class FileMetadataCoverageTest :
  FunSpec({
    fun metadata(
      id: Long? = 1L,
      fileType: FileType = FileType.IMAGE,
    ): FileMetadata =
      FileMetadata(
        fileName = "photo.png",
        storagePath = "files/photo.png",
        fileType = fileType,
        fileSize = 2048L,
        mimeType = "image/png",
        checksum = "abc123",
        isPublic = true,
        description = "头像",
        extension = "png",
        ownerId = 42L,
      ).apply { this.id = id }

    test("副构造全参数应落齐所有字段") {
      val m = metadata()
      m.id shouldBe 1L
      m.fileName shouldBe "photo.png"
      m.storagePath shouldBe "files/photo.png"
      m.fileType shouldBe FileType.IMAGE
      m.fileSize shouldBe 2048L
      m.mimeType shouldBe "image/png"
      m.checksum shouldBe "abc123"
      m.isPublic.shouldBeTrue()
      m.description shouldBe "头像"
      m.extension shouldBe "png"
      m.ownerId shouldBe 42L
      m.createdAt.shouldNotBeNull()
      m.updatedAt.shouldNotBeNull()
    }

    test("无参构造应使用默认值") {
      val m = FileMetadata()
      m.id.shouldBeNull()
      m.fileName shouldBe ""
      m.storagePath shouldBe ""
      m.fileType shouldBe FileType.OTHER
      m.fileSize shouldBe 0L
      m.mimeType shouldBe ""
      m.checksum shouldBe ""
      m.isPublic.shouldBeTrue()
      m.description.shouldBeNull()
      m.extension.shouldBeNull()
      m.ownerId.shouldBeNull()
      m.createdAt.shouldNotBeNull()
      m.updatedAt.shouldNotBeNull()
    }

    test("equals/hashCode 按 id 判等") {
      val a = metadata(id = 7L)
      val b = metadata(id = 7L, fileType = FileType.DOCUMENT)
      val c = metadata(id = 8L)

      a shouldBe b
      (a == a).shouldBeTrue()
      a.hashCode() shouldBe b.hashCode()
      (a == c).shouldBeFalse()
      (a.equals("not-a-file")).shouldBeFalse()
      (a.equals(null as Any?)).shouldBeFalse()
    }

    test("toString 应包含关键字段") {
      val s = metadata().toString()
      s shouldContain "FileMetadata("
      s shouldContain "photo.png"
      s shouldContain "files/photo.png"
      s shouldContain "IMAGE"
      s shouldContain "2048"
      s shouldContain "image/png"
      s shouldContain "abc123"
    }

    test("preUpdate 应刷新 updatedAt") {
      val m = metadata()
      val before = m.updatedAt
      Thread.sleep(5)
      m.preUpdate()
      (m.updatedAt.isAfter(before) || m.updatedAt != before).shouldBeTrue()
    }

    test("FileType 枚举全部值应可遍历且稳定") {
      FileType.entries.map { it.name } shouldContainExactly
        listOf(
          "IMAGE",
          "VIDEO",
          "AUDIO",
          "DOCUMENT",
          "SPREADSHEET",
          "PRESENTATION",
          "FRONTEND",
          "ARCHIVE",
          "EXECUTABLE",
          "BACKUP",
          "OTHER",
        )
      FileType.entries.forEach { t ->
        metadata(fileType = t).fileType shouldBe t
      }
      FileType.valueOf("OTHER") shouldBe FileType.OTHER
    }
  })
