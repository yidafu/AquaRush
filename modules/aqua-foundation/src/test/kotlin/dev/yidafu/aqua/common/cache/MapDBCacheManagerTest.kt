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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration

class MapDBCacheManagerTest :
  FunSpec({
    fun createManager(config: CacheConfig = CacheConfig()): MapDBCacheManager = MapDBCacheManager(config)

    test("put and get should round trip string values") {
      val manager = createManager()
      manager.put("key1", "value1")
      manager.get("key1", String::class.java) shouldBe "value1"
      manager.shutdown()
    }
    test("get should return null for missing key") {
      val manager = createManager()
      manager.get("missing", String::class.java).shouldBeNull()
      manager.shutdown()
    }
    test("put with ttl and get should work before expiry") {
      val manager = createManager()
      manager.put("key2", "value2", Duration.ofMinutes(10))
      manager.get("key2", String::class.java) shouldBe "value2"
      manager.shutdown()
    }
    test("put with zero ttl should fall back to default ttl") {
      val manager = createManager()
      manager.put("key3", "value3", Duration.ZERO)
      manager.getTTL("key3").shouldNotBeNull()
      manager.shutdown()
    }
    test("put with negative ttl should fall back to default ttl") {
      val manager = createManager()
      manager.put("key4", "value4", Duration.ofMillis(-1))
      manager.getTTL("key4").shouldNotBeNull()
      manager.shutdown()
    }
    test("expired entry should be removed on get") {
      val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
      manager.put("expiring", "value")
      manager.containsKey("expiring") shouldBe true
      Thread.sleep(200)
      manager.get("expiring", String::class.java).shouldBeNull()
      manager.containsKey("expiring") shouldBe false
      manager.shutdown()
    }
    test("getOrPut should return cached value") {
      val manager = createManager()
      manager.put("k", "cached")
      var supplierCalled = false
      val result =
        manager.getOrPut("k", String::class.java, supplier = {
          supplierCalled = true
          "computed"
        })
      result shouldBe "cached"
      supplierCalled shouldBe false
      manager.shutdown()
    }
    test("getOrPut should compute and store when missing") {
      val manager = createManager()
      val result = manager.getOrPut("k2", String::class.java, supplier = { "computed" })
      result shouldBe "computed"
      manager.get("k2", String::class.java) shouldBe "computed"
      manager.shutdown()
    }
    test("getOrPut should store data class values") {
      data class Foo(
        val name: String,
      )
      val manager = createManager()
      val result = manager.getOrPut("k3", Foo::class.java, supplier = { Foo("bar") })
      result shouldBe Foo("bar")
      manager.get("k3", Foo::class.java) shouldBe Foo("bar")
      manager.shutdown()
    }
    test("containsKey should reflect cache state") {
      val manager = createManager()
      manager.containsKey("missing") shouldBe false
      manager.put("exists", "v")
      manager.containsKey("exists") shouldBe true
      manager.shutdown()
    }
    test("remove should return true for existing and false for missing") {
      val manager = createManager()
      manager.put("r1", "v")
      manager.remove("r1") shouldBe true
      manager.remove("r1") shouldBe false
      manager.shutdown()
    }
    test("clear should empty the cache") {
      val manager = createManager()
      manager.put("a", "1")
      manager.put("b", "2")
      manager.clear()
      manager.size() shouldBe 0L
      manager.shutdown()
    }
    test("size should reflect entry count") {
      val manager = createManager()
      manager.put("a", "1")
      manager.put("b", "2")
      manager.size() shouldBe 2L
      manager.shutdown()
    }
    test("keys should return stored keys") {
      val manager = createManager()
      manager.put("a", "1")
      manager.put("b", "2")
      manager.keys() shouldBe setOf("a", "b")
      manager.shutdown()
    }
    test("keys should exclude expired keys") {
      val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
      manager.put("expired", "v")
      manager.put("stays", "v", Duration.ofMinutes(5))
      Thread.sleep(200)
      manager.keys() shouldBe setOf("stays")
      manager.shutdown()
    }
    test("removeAll should remove multiple keys") {
      val manager = createManager()
      manager.put("a", "1")
      manager.put("b", "2")
      manager.put("c", "3")
      val removed = manager.removeAll(setOf("a", "b", "not-present"))
      removed shouldBe 2L
      manager.size() shouldBe 1L
      manager.shutdown()
    }
    test("setTTL should return false for missing key") {
      val manager = createManager()
      manager.setTTL("missing", Duration.ofMinutes(5)) shouldBe false
      manager.shutdown()
    }
    test("setTTL should update ttl for existing key") {
      val manager = createManager()
      manager.put("t", "v")
      manager.setTTL("t", Duration.ofMinutes(15)) shouldBe true
      manager.getTTL("t").shouldNotBeNull()
      manager.shutdown()
    }
    test("setTTL with null should remove ttl") {
      val manager = createManager()
      manager.put("t2", "v", Duration.ofMinutes(5))
      manager.setTTL("t2", null) shouldBe true
      manager.getTTL("t2").shouldBeNull()
      manager.shutdown()
    }
    test("getTTL should return null for missing key") {
      val manager = createManager()
      manager.getTTL("missing").shouldBeNull()
      manager.shutdown()
    }
    test("getTTL should return null for expired key") {
      val manager = createManager(CacheConfig(defaultTTL = Duration.ofMillis(50)))
      manager.put("exp", "v")
      Thread.sleep(200)
      manager.getTTL("exp").shouldBeNull()
      manager.shutdown()
    }
    test("getStats should track hits and misses") {
      val manager = createManager()
      manager.put("s", "v")
      manager.get("s", String::class.java) // hit
      manager.get("s", String::class.java) // hit
      manager.get("missing", String::class.java) // miss
      val stats = manager.getStats()
      stats.hitCount shouldBe 2L
      stats.missCount shouldBe 1L
      stats.putCount shouldBe 1L
      stats.size shouldBe 1L
      stats.hitRate shouldBe 2.0 / 3.0
      manager.shutdown()
    }
    test("getStats should report zero hit rate when no accesses") {
      val manager = createManager()
      val stats = manager.getStats()
      stats.hitRate shouldBe 0.0
      stats.size shouldBe 0L
      manager.shutdown()
    }
    test("clear should reset stats") {
      val manager = createManager()
      manager.put("s", "v")
      manager.get("s", String::class.java)
      manager.clear()
      val stats = manager.getStats()
      stats.hitCount shouldBe 0L
      stats.missCount shouldBe 0L
      stats.putCount shouldBe 0L
      manager.shutdown()
    }
    test("async put and get should complete") {
      val manager = createManager()
      manager.putAsync("ak", "av").join()
      val future = manager.getAsync("ak", String::class.java)
      future.join() shouldBe "av"
      manager.shutdown()
    }
    test("async containsKey and size should complete") {
      val manager = createManager()
      manager.put("a", "1")
      manager.containsKeyAsync("a").join() shouldBe true
      manager.sizeAsync().join() shouldBe 1L
      manager.shutdown()
    }
    test("async remove and keys should complete") {
      val manager = createManager()
      manager.put("a", "1")
      manager.removeAsync("a").join() shouldBe true
      manager.keysAsync().join() shouldBe (emptySet<String>())
      manager.shutdown()
    }
    test("async removeAll and getOrPut should complete") {
      val manager = createManager()
      manager.put("a", "1")
      manager.put("b", "2")
      manager.removeAllAsync(setOf("a", "b")).join() shouldBe 2L
      val future =
        manager.getOrPutAsync(
          "c",
          String::class.java,
          supplier = {
            java.util.concurrent.CompletableFuture
              .completedFuture("cached")
          },
        )
      future.join() shouldBe "cached"
      manager.shutdown()
    }
    test("async setTTL and getTTL should complete") {
      val manager = createManager()
      manager.put("t", "v")
      manager.setTTLAsync("t", Duration.ofMinutes(5)).join() shouldBe true
      manager.getTTLAsync("t").join().shouldNotBeNull()
      manager.shutdown()
    }
    test("clearAsync should complete") {
      val manager = createManager()
      manager.put("a", "1")
      manager.clearAsync().join()
      manager.size() shouldBe 0L
      manager.shutdown()
    }
    test("removeAllAsync with empty set should return zero") {
      val manager = createManager()
      manager.removeAllAsync(emptySet()).join() shouldBe 0L
      manager.shutdown()
    }
    test("put with un-serializable value should throw CacheException") {
      class Unserializable {
        val boom: String
          get() = throw IllegalStateException("cannot serialize")
      }

      val manager = createManager()
      shouldThrow<CacheException> {
        manager.put("bad", Unserializable())
      }
      manager.shutdown()
    }
    test("getStats should report cache stats type") {
      val manager = createManager()
      manager.put("k", "v")
      manager.get("k", String::class.java)
      (manager.getStats().hitRate >= 0.0) shouldBe true
      manager.shutdown()
    }
  })
