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

package dev.yidafu.aqua.common.mapper

import org.assertj.core.api.Assertions.assertThat
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Mapper 测试基类，提供通用的映射完整性验证方法
 */
abstract class MapperTestBase {
  /**
   * 验证映射的完整性
   * 确保源对象的所有非空字段都在目标对象中有对应的映射
   */
  protected fun <S : Any, T : Any> verifyMappingCompleteness(
    mapper: (S) -> T,
    source: S,
    targetClass: KClass<T>,
  ) {
    val result = mapper(source)

    // 验证结果不为空且类型正确
    assertThat(result).isInstanceOf(targetClass.java)

    // 获取源对象和目标对象的所有属性
    val sourceProperties = source!!::class.memberProperties
    val targetProperties = result!!::class.memberProperties

    // 验证所有源对象中的非空属性都在目标对象中有对应的字段
    sourceProperties.forEach { sourceProp ->
      val sourceValue = sourceProp.getter.call(source)
      if (sourceValue != null) {
        val matchingTargetProp =
          targetProperties.find { targetProp ->
            targetProp.name.equals(sourceProp.name, ignoreCase = true)
          }

        assertThat(matchingTargetProp)
          .`as`("字段 '${sourceProp.name}' 在目标对象中缺失")
          .isNotNull()
      }
    }
  }

  /**
   * 验证特定字段的映射
   */
  protected fun <S : Any, T : Any> verifyFieldMapping(
    mapper: (S) -> T,
    source: S,
    fieldName: String,
    expectedValue: Any?,
  ) {
    val result = mapper(source)
    val targetProp =
      result!!::class.memberProperties.find {
        it.name.equals(fieldName, ignoreCase = true)
      }

    assertThat(targetProp)
      .`as`("字段 '$fieldName' 在目标对象中不存在")
      .isNotNull()

    val actualValue = targetProp!!.getter.call(result)
    assertThat(actualValue)
      .`as`("字段 '$fieldName' 的映射值不正确")
      .isEqualTo(expectedValue)
  }
}
