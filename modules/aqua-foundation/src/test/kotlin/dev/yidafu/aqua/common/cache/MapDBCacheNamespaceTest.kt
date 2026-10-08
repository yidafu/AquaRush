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

package dev.yidafu.aqua.common.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration

class MapDBCacheNamespaceTest :
  FunSpec({

    test("getCache should lazily create and reuse namespace") {
      val namespace = MapDBCacheNamespace()
      val first = namespace.getCache("ns1")
      val second = namespace.getCache("ns1")
      second shouldBe first
      namespace.hasNamespace("ns1") shouldBe true
      namespace.shutdown()
    }
    test("getAllNamespaces should list created namespaces") {
      val namespace = MapDBCacheNamespace()
      namespace.getCache("a")
      namespace.getCache("b")
      namespace.getAllNamespaces() shouldBe setOf("a", "b")
      namespace.shutdown()
    }
    test("clearNamespace should clear existing namespace") {
      val namespace = MapDBCacheNamespace()
      val cache = namespace.getCache("a")
      cache.put("k", "v")
      namespace.clearNamespace("a")
      cache.size() shouldBe 0L
      namespace.shutdown()
    }
    test("clearNamespace should not fail for unknown namespace") {
      val namespace = MapDBCacheNamespace()
      namespace.clearNamespace("missing")
      namespace.hasNamespace("missing") shouldBe false
      namespace.shutdown()
    }
    test("createNamespace should register namespace") {
      val namespace = MapDBCacheNamespace()
      val cache = namespace.createNamespace("custom", CacheConfig(defaultTTL = Duration.ofMinutes(1)))
      namespace.hasNamespace("custom") shouldBe true
      cache.put("k", "v")
      cache.get("k", String::class.java) shouldBe "v"
      namespace.shutdown()
    }
    test("createNamespace should not duplicate existing namespace") {
      val namespace = MapDBCacheNamespace()
      val first = namespace.getCache("dup")
      val second = namespace.createNamespace("dup", CacheConfig())
      second shouldBe first
      namespace.shutdown()
    }
    test("deleteNamespace should remove and shutdown cache") {
      val namespace = MapDBCacheNamespace()
      namespace.getCache("to-delete")
      namespace.deleteNamespace("to-delete") shouldBe true
      namespace.hasNamespace("to-delete") shouldBe false
      namespace.shutdown()
    }
    test("deleteNamespace should return false for unknown namespace") {
      val namespace = MapDBCacheNamespace()
      namespace.deleteNamespace("missing") shouldBe false
      namespace.shutdown()
    }
    test("getNamespaceStats should report per namespace stats") {
      val namespace = MapDBCacheNamespace()
      val cache = namespace.getCache("stats-ns")
      cache.put("k", "v")
      cache.get("k", String::class.java)
      val stats = namespace.getNamespaceStats()
      stats.size shouldBe 1
      stats["stats-ns"]?.hitCount shouldBe 1L
      namespace.shutdown()
    }
    test("shutdown should clear all namespaces") {
      val namespace = MapDBCacheNamespace()
      namespace.getCache("a")
      namespace.getCache("b")
      namespace.shutdown()
      namespace.hasNamespace("a") shouldBe false
      namespace.hasNamespace("b") shouldBe false
    }
    test("name should be mapdb cache namespace") {
      val namespace = MapDBCacheNamespace()
      namespace.name shouldBe "mapdb-cache-namespace"
      namespace.shutdown()
    }
    test("namespace cache should support full cache operations") {
      val namespace = MapDBCacheNamespace()
      val cache = namespace.getCache("full")
      cache.put("a", "1")
      cache.put("b", "2", Duration.ofMinutes(5))
      cache.get("a", String::class.java).shouldNotBeNull()
      cache.containsKey("b") shouldBe true
      cache.size() shouldBe 2L
      cache.remove("a") shouldBe true
      cache.size() shouldBe 1L
      namespace.shutdown()
    }
  })
