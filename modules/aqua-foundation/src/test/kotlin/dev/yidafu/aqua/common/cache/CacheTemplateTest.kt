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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CacheTemplateTest {
  private lateinit var cacheNamespace: CacheNamespace
  private lateinit var defaultCache: CacheManager
  private lateinit var nsCache: CacheManager
  private lateinit var template: CacheTemplate

  @BeforeEach
  fun setUp() {
    cacheNamespace = mockk(relaxed = true)
    defaultCache = mockk(relaxed = true)
    nsCache = mockk(relaxed = true)
    every { cacheNamespace.getCache("default") } returns defaultCache
    every { cacheNamespace.getCache("orders") } returns nsCache
    every { cacheNamespace.getAllNamespaces() } returns setOf("default", "orders")
    template = CacheTemplate(cacheNamespace)
  }

  @Test
  fun `put should delegate to default cache`() {
    template.put("k", "v")
    verify { defaultCache.put("k", "v", null) }
  }

  @Test
  fun `put with ttl should pass ttl to default cache`() {
    val ttl = Duration.ofMinutes(5)
    template.put(key = "k", value = "v", ttl = ttl)
    verify { defaultCache.put("k", "v", ttl) }
  }

  @Test
  fun `put with namespace should delegate to namespace cache`() {
    template.put("orders", "k", "v")
    verify { nsCache.put("k", "v", null) }
  }

  @Test
  fun `get should delegate to default cache`() {
    every { defaultCache.get("k", String::class.java) } returns "v"
    assertEquals("v", template.get("k", String::class.java))
    verify { defaultCache.get("k", String::class.java) }
  }

  @Test
  fun `get with namespace should delegate to namespace cache`() {
    every { nsCache.get("k", String::class.java) } returns "ns-v"
    assertEquals("ns-v", template.get("orders", "k", String::class.java))
    verify { nsCache.get("k", String::class.java) }
  }

  @Test
  fun `getOrPut should delegate to default cache`() {
    every { defaultCache.getOrPut("k", String::class.java, any(), null) } returns "v"
    assertEquals("v", template.getOrPut("k", String::class.java, supplier = { "computed" }))
  }

  @Test
  fun `getOrPut with namespace should delegate to namespace cache`() {
    every { nsCache.getOrPut("k", String::class.java, any(), null) } returns "ns-v"
    assertEquals("ns-v", template.getOrPut("orders", "k", String::class.java, supplier = { "computed" }))
  }

  @Test
  fun `getOrPutAsync should delegate to default cache`() {
    val future: CompletableFuture<String> = CompletableFuture.completedFuture("v")
    every { defaultCache.getOrPutAsync("k", String::class.java, any(), null) } returns future
    assertEquals("v", template.getOrPutAsync("k", String::class.java, supplier = { future }).join())
  }

  @Test
  fun `getOrPutAsync with namespace should delegate to namespace cache`() {
    val future: CompletableFuture<String> = CompletableFuture.completedFuture("ns-v")
    every { nsCache.getOrPutAsync("k", String::class.java, any(), null) } returns future
    assertEquals("ns-v", template.getOrPutAsync("orders", "k", String::class.java, supplier = { future }).join())
  }

  @Test
  fun `containsKey should delegate to default cache`() {
    every { defaultCache.containsKey("k") } returns true
    assertTrue(template.containsKey("k"))
    verify { defaultCache.containsKey("k") }
  }

  @Test
  fun `containsKey with namespace should delegate to namespace cache`() {
    every { nsCache.containsKey("k") } returns false
    assertEquals(false, template.containsKey("orders", "k"))
  }

  @Test
  fun `remove should delegate to default cache`() {
    every { defaultCache.remove("k") } returns true
    assertEquals(true, template.remove("k"))
    verify { defaultCache.remove("k") }
  }

  @Test
  fun `remove with namespace should delegate to namespace cache`() {
    every { nsCache.remove("k") } returns false
    assertEquals(false, template.remove("orders", "k"))
  }

  @Test
  fun `clear should delegate to default cache`() {
    template.clear()
    verify { defaultCache.clear() }
  }

  @Test
  fun `clear with namespace should call clearNamespace`() {
    template.clear("orders")
    verify { cacheNamespace.clearNamespace("orders") }
  }

  @Test
  fun `size should delegate to default cache`() {
    every { defaultCache.size() } returns 3L
    assertEquals(3L, template.size())
  }

  @Test
  fun `size with namespace should delegate to namespace cache`() {
    every { nsCache.size() } returns 5L
    assertEquals(5L, template.size("orders"))
  }

  @Test
  fun `getAllNamespaces should delegate to namespace`() {
    assertEquals(setOf("default", "orders"), template.getAllNamespaces())
    verify { cacheNamespace.getAllNamespaces() }
  }
}
