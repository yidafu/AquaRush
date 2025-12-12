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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.RegionHierarchyModel
import dev.yidafu.aqua.user.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RegionQueryResolverTest {

    private lateinit var regionRepository: RegionRepository
    private lateinit var regionQueryResolver: RegionQueryResolver
    private lateinit var userPrincipal: UserPrincipal

    // Test data
    private val sampleProvince = RegionModel(
        id = 1L,
        name = "广东省",
        code = "440000",
        parentCode = null,
        level = 1,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val sampleCity = RegionModel(
        id = 2L,
        name = "深圳市",
        code = "440300",
        parentCode = "440000",
        level = 2,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val sampleDistrict = RegionModel(
        id = 3L,
        name = "南山区",
        code = "440305",
        parentCode = "440300",
        level = 3,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val anotherCity = RegionModel(
        id = 4L,
        name = "广州市",
        code = "440100",
        parentCode = "440000",
        level = 2,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val anotherDistrict = RegionModel(
        id = 5L,
        name = "天河区",
        code = "440106",
        parentCode = "440100",
        level = 3,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        regionRepository = mockk()
        regionQueryResolver = RegionQueryResolver(
            regionRepository = regionRepository,
            defaultDistrictCode = "440305"
        )

        userPrincipal = UserPrincipal(
            id = 123L,
            _username = "testuser",
            userType = "USER",
            _authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @Test
    fun `should return all regions when no parameters provided`() {
        // Given
        val allRegions = listOf(sampleProvince, sampleCity, sampleDistrict)
        every { regionRepository.findAll() } returns allRegions

        // When
        val result = regionQueryResolver.regions(null, null, userPrincipal)

        // Then
        assertEquals(3, result.size)
        assertEquals(allRegions, result)
        verify { regionRepository.findAll() }
    }

    @Test
    fun `should return regions by level when only level provided`() {
        // Given
        every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

        // When
        val result = regionQueryResolver.regions(1, null, userPrincipal)

        // Then
        assertEquals(1, result.size)
        assertEquals(sampleProvince, result[0])
        verify { regionRepository.findRootRegions(1) }
    }

    @Test
    fun `should return root regions when level is 1`() {
        // Given
        every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

        // When
        val result = regionQueryResolver.regions(1, null, userPrincipal)

        // Then
        assertEquals(1, result.size)
        assertEquals(sampleProvince, result[0])
        verify { regionRepository.findRootRegions(1) }
    }

    @Test
    fun `should call findByLevel for levels other than 1`() {
        // Given
        every { regionRepository.findByLevel(2) } returns listOf(sampleCity, anotherCity)

        // When
        val result = regionQueryResolver.regions(2, null, userPrincipal)

        // Then
        assertEquals(2, result.size)
        verify { regionRepository.findByLevel(2) }
    }

    @Test
    fun `should return regions by level and parent code when both provided`() {
        // Given
        every { regionRepository.findByParentCodeAndLevel("440000", 2) } returns listOf(sampleCity, anotherCity)

        // When
        val result = regionQueryResolver.regions(2, "440000", userPrincipal)

        // Then
        assertEquals(2, result.size)
        assertEquals(sampleCity, result[0])
        assertEquals(anotherCity, result[1])
        verify { regionRepository.findByParentCodeAndLevel("440000", 2) }
    }

    @Test
    fun `should return region by code`() {
        // Given
        every { regionRepository.findByCode("440305") } returns sampleDistrict

        // When
        val result = regionQueryResolver.region("440305", userPrincipal)

        // Then
        assertNotNull(result)
        assertEquals(sampleDistrict, result)
        verify { regionRepository.findByCode("440305") }
    }

    @Test
    fun `should return null when region code not found`() {
        // Given
        every { regionRepository.findByCode("999999") } returns null

        // When
        val result = regionQueryResolver.region("999999", userPrincipal)

        // Then
        assertNull(result)
        verify { regionRepository.findByCode("999999") }
    }

    @Test
    fun `should return default region hierarchy when default district code is set`() {
        // Given
        every { regionRepository.findByCode("440305") } returns sampleDistrict
        every { regionRepository.findByCode("440300") } returns sampleCity
        every { regionRepository.findByCode("440000") } returns sampleProvince
        every { regionRepository.findByLevelOrderByCode(1) } returns listOf(sampleProvince)
        every { regionRepository.findByParentCodeOrderByCode("440000") } returns listOf(sampleCity, anotherCity)
        every { regionRepository.findByParentCodeOrderByCode("440300") } returns listOf(sampleDistrict)

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNotNull(result)
        assertEquals(sampleProvince, result.province)
        assertEquals(sampleCity, result.city)
        assertEquals(sampleDistrict, result.district)
        assertEquals(1, result.provinces.size)
        assertEquals(sampleProvince, result.provinces[0])
        assertEquals(2, result.cities.size)
        assertEquals(sampleCity, result.cities[0])
        assertEquals(anotherCity, result.cities[1])
        assertEquals(1, result.districts.size)
        assertEquals(sampleDistrict, result.districts[0])
    }

    @Test
    fun `should return null when default district code is not set`() {
        // Given
        val resolverWithoutDefault = RegionQueryResolver(
            regionRepository = regionRepository,
            defaultDistrictCode = null
        )

        // When
        val result = resolverWithoutDefault.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when default district not found`() {
        // Given
        every { regionRepository.findByCode("440305") } returns null

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNull(result)
        verify { regionRepository.findByCode("440305") }
    }

    @Test
    fun `should return null when parent region not found in hierarchy`() {
        // Given
        every { regionRepository.findByCode("440305") } returns sampleDistrict
        every { regionRepository.findByCode("440300") } returns null  // City not found

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNull(result)
        verify { regionRepository.findByCode("440305") }
        verify { regionRepository.findByCode("440300") }
    }

    @Test
    fun `should return null when grandparent region not found in hierarchy`() {
        // Given
        every { regionRepository.findByCode("440305") } returns sampleDistrict
        every { regionRepository.findByCode("440300") } returns sampleCity
        every { regionRepository.findByCode("440000") } returns null  // Province not found

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNull(result)
        verify { regionRepository.findByCode("440305") }
        verify { regionRepository.findByCode("440300") }
        verify { regionRepository.findByCode("440000") }
    }

    @Test
    fun `should handle exception gracefully in hierarchy building`() {
        // Given
        every { regionRepository.findByCode("440305") } throws RuntimeException("Database error")

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNull(result)
        verify { regionRepository.findByCode("440305") }
    }

    @Test
    fun `should work with null user principal`() {
        // Given
        every { regionRepository.findAll() } returns listOf(sampleProvince)

        // When
        val result = regionQueryResolver.regions(null, null, null)

        // Then
        assertEquals(1, result.size)
        assertEquals(sampleProvince, result[0])
        verify { regionRepository.findAll() }
    }

    @Test
    fun `should handle regions query with mixed parameters correctly`() {
        // Given
        val cities = listOf(sampleCity, anotherCity)
        every { regionRepository.findByParentCodeAndLevel("440000", 2) } returns cities

        // When
        val result = regionQueryResolver.regions(2, "440000", null)

        // Then
        assertEquals(2, result.size)
        assertEquals(cities, result)
        verify { regionRepository.findByParentCodeAndLevel("440000", 2) }
    }

    @Test
    fun `should return empty list when no regions found for given criteria`() {
        // Given
        every { regionRepository.findByLevel(99) } returns emptyList()

        // When
        val result = regionQueryResolver.regions(99, null, userPrincipal)

        // Then
        assertEquals(0, result.size)
        verify { regionRepository.findByLevel(99) }
    }

    @Test
    fun `should handle level 0 correctly`() {
        // Given
        every { regionRepository.findByLevel(0) } returns listOf(sampleProvince, sampleCity)

        // When
        val result = regionQueryResolver.regions(0, null, userPrincipal)

        // Then
        assertEquals(2, result.size)
        verify { regionRepository.findByLevel(0) }
    }

    @Test
    fun `should build complete hierarchy with multiple children correctly`() {
        // Given
        val allProvinces = listOf(sampleProvince)
        val citiesInProvince = listOf(sampleCity, anotherCity)
        val districtsInCity = listOf(sampleDistrict, anotherDistrict)

        every { regionRepository.findByCode("440305") } returns sampleDistrict
        every { regionRepository.findByCode("440300") } returns sampleCity
        every { regionRepository.findByCode("440000") } returns sampleProvince
        every { regionRepository.findByLevelOrderByCode(1) } returns allProvinces
        every { regionRepository.findByParentCodeOrderByCode("440000") } returns citiesInProvince
        every { regionRepository.findByParentCodeOrderByCode("440300") } returns districtsInCity

        // When
        val result = regionQueryResolver.defaultRegionHierarchy(userPrincipal)

        // Then
        assertNotNull(result)
        assertEquals(allProvinces, result.provinces)
        assertEquals(citiesInProvince, result.cities)
        assertEquals(districtsInCity, result.districts)
        assertEquals(2, result.cities.size)
        assertEquals(2, result.districts.size)
    }
}