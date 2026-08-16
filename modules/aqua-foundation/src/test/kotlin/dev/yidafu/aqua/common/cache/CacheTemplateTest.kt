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
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.util.concurrent.CompletableFuture

class CacheTemplateTest :
  FunSpec({
    var cacheNamespace: CacheNamespace = mockk()
    var defaultCache: CacheManager = mockk()
    var nsCache: CacheManager = mockk()
    var template: CacheTemplate = mockk()

    beforeTest {
      cacheNamespace = mockk(relaxed = true)
      defaultCache = mockk(relaxed = true)
      nsCache = mockk(relaxed = true)
      every { cacheNamespace.getCache("default") } returns defaultCache
      every { cacheNamespace.getCache("orders") } returns nsCache
      every { cacheNamespace.getAllNamespaces() } returns setOf("default", "orders")
      template = CacheTemplate(cacheNamespace)
    }
    test("put should delegate to default cache") {
      template.put("k", "v")
      verify { defaultCache.put("k", "v", null) }
    }
    test("put with ttl should pass ttl to default cache") {
      val ttl = Duration.ofMinutes(5)
      template.put(key = "k", value = "v", ttl = ttl)
      verify { defaultCache.put("k", "v", ttl) }
    }
    test("put with namespace should delegate to namespace cache") {
      template.put("orders", "k", "v")
      verify { nsCache.put("k", "v", null) }
    }
    test("get should delegate to default cache") {
      every { defaultCache.get("k", String::class.java) } returns "v"
      template.get("k", String::class.java) shouldBe "v"
      verify { defaultCache.get("k", String::class.java) }
    }
    test("get with namespace should delegate to namespace cache") {
      every { nsCache.get("k", String::class.java) } returns "ns-v"
      template.get("orders", "k", String::class.java) shouldBe "ns-v"
      verify { nsCache.get("k", String::class.java) }
    }
    test("getOrPut should delegate to default cache") {
      every { defaultCache.getOrPut("k", String::class.java, any(), null) } returns "v"
      template.getOrPut("k", String::class.java, supplier = { "computed" }) shouldBe "v"
    }
    test("getOrPut with namespace should delegate to namespace cache") {
      every { nsCache.getOrPut("k", String::class.java, any(), null) } returns "ns-v"
      template.getOrPut("orders", "k", String::class.java, supplier = { "computed" }) shouldBe "ns-v"
    }
    test("getOrPutAsync should delegate to default cache") {
      val future: CompletableFuture<String> = CompletableFuture.completedFuture("v")
      every { defaultCache.getOrPutAsync("k", String::class.java, any(), null) } returns future
      template.getOrPutAsync("k", String::class.java, supplier = { future }).join() shouldBe "v"
    }
    test("getOrPutAsync with namespace should delegate to namespace cache") {
      val future: CompletableFuture<String> = CompletableFuture.completedFuture("ns-v")
      every { nsCache.getOrPutAsync("k", String::class.java, any(), null) } returns future
      template.getOrPutAsync("orders", "k", String::class.java, supplier = { future }).join() shouldBe "ns-v"
    }
    test("containsKey should delegate to default cache") {
      every { defaultCache.containsKey("k") } returns true
      template.containsKey("k") shouldBe true
      verify { defaultCache.containsKey("k") }
    }
    test("containsKey with namespace should delegate to namespace cache") {
      every { nsCache.containsKey("k") } returns false
      template.containsKey("orders", "k") shouldBe false
    }
    test("remove should delegate to default cache") {
      every { defaultCache.remove("k") } returns true
      template.remove("k") shouldBe true
      verify { defaultCache.remove("k") }
    }
    test("remove with namespace should delegate to namespace cache") {
      every { nsCache.remove("k") } returns false
      template.remove("orders", "k") shouldBe false
    }
    test("clear should delegate to default cache") {
      template.clear()
      verify { defaultCache.clear() }
    }
    test("clear with namespace should call clearNamespace") {
      template.clear("orders")
      verify { cacheNamespace.clearNamespace("orders") }
    }
    test("size should delegate to default cache") {
      every { defaultCache.size() } returns 3L
      template.size() shouldBe 3L
    }
    test("size with namespace should delegate to namespace cache") {
      every { nsCache.size() } returns 5L
      template.size("orders") shouldBe 5L
    }
    test("getAllNamespaces should delegate to namespace") {
      template.getAllNamespaces() shouldBe setOf("default", "orders")
      verify { cacheNamespace.getAllNamespaces() }
    }
  })
