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
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapDBCacheManagerTest {
  private fun createManager(config: CacheConfig = CacheConfig()): MapDBCacheManager = MapDBCacheManager(config)

  @Test
  fun `put and get should round trip string values`() {
    val manager = createManager()
    manager.put("key1", "value1")
    assertEquals("value1", manager.get("key1", String::class.java))
    manager.shutdown()
  }

  @Test
  fun `get should return null for missing key`() {
    val manager = createManager()
    assertNull(manager.get("missing", String::class.java))
    manager.shutdown()
  }

  @Test
  fun `put with ttl and get should work before expiry`() {
    val manager = createManager()
    manager.put("key2", "value2", Duration.ofMinutes(10))
    assertEquals("value2", manager.get("key2", String::class.java))
    manager.shutdown()
  }

  @Test
  fun `put with zero ttl should fall back to default ttl`() {
    val manager = createManager()
    manager.put("key3", "value3", Duration.ZERO)
    assertNotNull(manager.getTTL("key3"))
    manager.shutdown()
  }

  @Test
  fun `put with negative ttl should fall back to default ttl`() {
    val manager = createManager()
    manager.put("key4", "value4", Duration.ofMillis(-1))
    assertNotNull(manager.getTTL("key4"))
    manager.shutdown()
  }

  @Test
  fun `expired entry should be removed on get`() {
    val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
    manager.put("expiring", "value")
    assertTrue(manager.containsKey("expiring"))
    Thread.sleep(200)
    assertNull(manager.get("expiring", String::class.java))
    assertFalse(manager.containsKey("expiring"))
    manager.shutdown()
  }

  @Test
  fun `getOrPut should return cached value`() {
    val manager = createManager()
    manager.put("k", "cached")
    var supplierCalled = false
    val result =
      manager.getOrPut("k", String::class.java, supplier = {
        supplierCalled = true
        "computed"
      })
    assertEquals("cached", result)
    assertFalse(supplierCalled)
    manager.shutdown()
  }

  @Test
  fun `getOrPut should compute and store when missing`() {
    val manager = createManager()
    val result = manager.getOrPut("k2", String::class.java, supplier = { "computed" })
    assertEquals("computed", result)
    assertEquals("computed", manager.get("k2", String::class.java))
    manager.shutdown()
  }

  @Test
  fun `getOrPut should store data class values`() {
    data class Foo(
      val name: String,
    )
    val manager = createManager()
    val result = manager.getOrPut("k3", Foo::class.java, supplier = { Foo("bar") })
    assertEquals(Foo("bar"), result)
    assertEquals(Foo("bar"), manager.get("k3", Foo::class.java))
    manager.shutdown()
  }

  @Test
  fun `containsKey should reflect cache state`() {
    val manager = createManager()
    assertFalse(manager.containsKey("missing"))
    manager.put("exists", "v")
    assertTrue(manager.containsKey("exists"))
    manager.shutdown()
  }

  @Test
  fun `remove should return true for existing and false for missing`() {
    val manager = createManager()
    manager.put("r1", "v")
    assertTrue(manager.remove("r1"))
    assertFalse(manager.remove("r1"))
    manager.shutdown()
  }

  @Test
  fun `clear should empty the cache`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.put("b", "2")
    manager.clear()
    assertEquals(0L, manager.size())
    manager.shutdown()
  }

  @Test
  fun `size should reflect entry count`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.put("b", "2")
    assertEquals(2L, manager.size())
    manager.shutdown()
  }

  @Test
  fun `keys should return stored keys`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.put("b", "2")
    assertEquals(setOf("a", "b"), manager.keys())
    manager.shutdown()
  }

  @Test
  fun `keys should exclude expired keys`() {
    val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
    manager.put("expired", "v")
    manager.put("stays", "v", Duration.ofMinutes(5))
    Thread.sleep(200)
    assertEquals(setOf("stays"), manager.keys())
    manager.shutdown()
  }

  @Test
  fun `removeAll should remove multiple keys`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.put("b", "2")
    manager.put("c", "3")
    val removed = manager.removeAll(setOf("a", "b", "not-present"))
    assertEquals(2L, removed)
    assertEquals(1L, manager.size())
    manager.shutdown()
  }

  @Test
  fun `setTTL should return false for missing key`() {
    val manager = createManager()
    assertFalse(manager.setTTL("missing", Duration.ofMinutes(5)))
    manager.shutdown()
  }

  @Test
  fun `setTTL should update ttl for existing key`() {
    val manager = createManager()
    manager.put("t", "v")
    assertTrue(manager.setTTL("t", Duration.ofMinutes(15)))
    assertNotNull(manager.getTTL("t"))
    manager.shutdown()
  }

  @Test
  fun `setTTL with null should remove ttl`() {
    val manager = createManager()
    manager.put("t2", "v", Duration.ofMinutes(5))
    assertTrue(manager.setTTL("t2", null))
    assertNull(manager.getTTL("t2"))
    manager.shutdown()
  }

  @Test
  fun `getTTL should return null for missing key`() {
    val manager = createManager()
    assertNull(manager.getTTL("missing"))
    manager.shutdown()
  }

  @Test
  fun `getTTL should return null for expired key`() {
    val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
    manager.put("exp", "v")
    Thread.sleep(200)
    assertNull(manager.getTTL("exp"))
    manager.shutdown()
  }

  @Test
  fun `getStats should track hits and misses`() {
    val manager = createManager()
    manager.put("s", "v")
    manager.get("s", String::class.java) // hit
    manager.get("s", String::class.java) // hit
    manager.get("missing", String::class.java) // miss
    val stats = manager.getStats()
    assertEquals(2L, stats.hitCount)
    assertEquals(1L, stats.missCount)
    assertEquals(1L, stats.putCount)
    assertEquals(1L, stats.size)
    assertEquals(2.0 / 3.0, stats.hitRate)
    manager.shutdown()
  }

  @Test
  fun `getStats should report zero hit rate when no accesses`() {
    val manager = createManager()
    val stats = manager.getStats()
    assertEquals(0.0, stats.hitRate)
    assertEquals(0L, stats.size)
    manager.shutdown()
  }

  @Test
  fun `clear should reset stats`() {
    val manager = createManager()
    manager.put("s", "v")
    manager.get("s", String::class.java)
    manager.clear()
    val stats = manager.getStats()
    assertEquals(0L, stats.hitCount)
    assertEquals(0L, stats.missCount)
    assertEquals(0L, stats.putCount)
    manager.shutdown()
  }

  @Test
  fun `async put and get should complete`() {
    val manager = createManager()
    manager.putAsync("ak", "av").join()
    val future = manager.getAsync("ak", String::class.java)
    assertEquals("av", future.join())
    manager.shutdown()
  }

  @Test
  fun `async containsKey and size should complete`() {
    val manager = createManager()
    manager.put("a", "1")
    assertTrue(manager.containsKeyAsync("a").join())
    assertEquals(1L, manager.sizeAsync().join())
    manager.shutdown()
  }

  @Test
  fun `async remove and keys should complete`() {
    val manager = createManager()
    manager.put("a", "1")
    assertTrue(manager.removeAsync("a").join())
    assertEquals(emptySet<String>(), manager.keysAsync().join())
    manager.shutdown()
  }

  @Test
  fun `async removeAll and getOrPut should complete`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.put("b", "2")
    assertEquals(2L, manager.removeAllAsync(setOf("a", "b")).join())
    val future =
      manager.getOrPutAsync(
        "c",
        String::class.java,
        supplier = {
          java.util.concurrent.CompletableFuture
            .completedFuture("cached")
        },
      )
    assertEquals("cached", future.join())
    manager.shutdown()
  }

  @Test
  fun `async setTTL and getTTL should complete`() {
    val manager = createManager()
    manager.put("t", "v")
    assertTrue(manager.setTTLAsync("t", Duration.ofMinutes(5)).join())
    assertNotNull(manager.getTTLAsync("t").join())
    manager.shutdown()
  }

  @Test
  fun `clearAsync should complete`() {
    val manager = createManager()
    manager.put("a", "1")
    manager.clearAsync().join()
    assertEquals(0L, manager.size())
    manager.shutdown()
  }

  @Test
  fun `removeAllAsync with empty set should return zero`() {
    val manager = createManager()
    assertEquals(0L, manager.removeAllAsync(emptySet()).join())
    manager.shutdown()
  }

  @Test
  fun `put with un-serializable value should throw CacheException`() {
    class Unserializable {
      val boom: String
        get() = throw IllegalStateException("cannot serialize")
    }

    val manager = createManager()
    assertThrows<CacheException> {
      manager.put("bad", Unserializable())
    }
    manager.shutdown()
  }

  @Test
  fun `getStats should report cache stats type`() {
    val manager = createManager()
    manager.put("k", "v")
    manager.get("k", String::class.java)
    assertTrue(manager.getStats().hitRate >= 0.0)
    manager.shutdown()
  }
}
