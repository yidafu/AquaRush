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

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapDBCacheNamespaceTest {
  @Test
  fun `getCache should lazily create and reuse namespace`() {
    val namespace = MapDBCacheNamespace()
    val first = namespace.getCache("ns1")
    val second = namespace.getCache("ns1")
    assertEquals(first, second)
    assertTrue(namespace.hasNamespace("ns1"))
    namespace.shutdown()
  }

  @Test
  fun `getAllNamespaces should list created namespaces`() {
    val namespace = MapDBCacheNamespace()
    namespace.getCache("a")
    namespace.getCache("b")
    assertEquals(setOf("a", "b"), namespace.getAllNamespaces())
    namespace.shutdown()
  }

  @Test
  fun `clearNamespace should clear existing namespace`() {
    val namespace = MapDBCacheNamespace()
    val cache = namespace.getCache("a")
    cache.put("k", "v")
    namespace.clearNamespace("a")
    assertEquals(0L, cache.size())
    namespace.shutdown()
  }

  @Test
  fun `clearNamespace should not fail for unknown namespace`() {
    val namespace = MapDBCacheNamespace()
    namespace.clearNamespace("missing")
    assertFalse(namespace.hasNamespace("missing"))
    namespace.shutdown()
  }

  @Test
  fun `createNamespace should register namespace`() {
    val namespace = MapDBCacheNamespace()
    val cache = namespace.createNamespace("custom", CacheConfig(defaultTTL = Duration.ofMinutes(1)))
    assertTrue(namespace.hasNamespace("custom"))
    cache.put("k", "v")
    assertEquals("v", cache.get("k", String::class.java))
    namespace.shutdown()
  }

  @Test
  fun `createNamespace should not duplicate existing namespace`() {
    val namespace = MapDBCacheNamespace()
    val first = namespace.getCache("dup")
    val second = namespace.createNamespace("dup", CacheConfig())
    assertEquals(first, second)
    namespace.shutdown()
  }

  @Test
  fun `deleteNamespace should remove and shutdown cache`() {
    val namespace = MapDBCacheNamespace()
    namespace.getCache("to-delete")
    assertTrue(namespace.deleteNamespace("to-delete"))
    assertFalse(namespace.hasNamespace("to-delete"))
    namespace.shutdown()
  }

  @Test
  fun `deleteNamespace should return false for unknown namespace`() {
    val namespace = MapDBCacheNamespace()
    assertFalse(namespace.deleteNamespace("missing"))
    namespace.shutdown()
  }

  @Test
  fun `getNamespaceStats should report per namespace stats`() {
    val namespace = MapDBCacheNamespace()
    val cache = namespace.getCache("stats-ns")
    cache.put("k", "v")
    cache.get("k", String::class.java)
    val stats = namespace.getNamespaceStats()
    assertEquals(1, stats.size)
    assertEquals(1L, stats["stats-ns"]?.hitCount)
    namespace.shutdown()
  }

  @Test
  fun `shutdown should clear all namespaces`() {
    val namespace = MapDBCacheNamespace()
    namespace.getCache("a")
    namespace.getCache("b")
    namespace.shutdown()
    assertFalse(namespace.hasNamespace("a"))
    assertFalse(namespace.hasNamespace("b"))
  }

  @Test
  fun `name should be mapdb cache namespace`() {
    val namespace = MapDBCacheNamespace()
    assertEquals("mapdb-cache-namespace", namespace.name)
    namespace.shutdown()
  }

  @Test
  fun `namespace cache should support full cache operations`() {
    val namespace = MapDBCacheNamespace()
    val cache = namespace.getCache("full")
    cache.put("a", "1")
    cache.put("b", "2", Duration.ofMinutes(5))
    assertNotNull(cache.get("a", String::class.java))
    assertTrue(cache.containsKey("b"))
    assertEquals(2L, cache.size())
    assertTrue(cache.remove("a"))
    assertEquals(1L, cache.size())
    namespace.shutdown()
  }
}
