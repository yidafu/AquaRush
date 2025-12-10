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

package dev.yidafu.aqua.common.graphql.validation

import org.springframework.stereotype.Component
import java.util.regex.Pattern

/**
 * GraphQL 输入验证工具类
 * 可以在 Resolver 中直接使用来验证输入参数
 */
@Component
class GraphQLInputValidator {

  private val emailPattern = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")
  private val phonePattern = Pattern.compile("^1[3-9]\\d{9}$")

  /**
   * 验证用户输入
   * @param input 输入参数 Map
   * @throws ValidationException 验证失败时抛出
   */
  fun validateUserInput(input: Map<String, Any>?) {
    if (input == null) {
      throw ValidationException("输入参数不能为空")
    }

    // 验证用户名
    val username = input["username"] as? String
    if (username != null) {
      validateNotBlank(username, "用户名")
      validateSize(username, 3, 20, "用户名")
      validatePattern(username, "^[a-zA-Z0-9_\\-]+$", "用户名", "只能包含字母、数字、下划线和连字符")
    }

    // 验证邮箱
    val email = input["email"] as? String
    if (email != null) {
      validateNotBlank(email, "邮箱")
      validateEmail(email)
    }

    // 验证密码
    val password = input["password"] as? String
    if (password != null) {
      validateNotBlank(password, "密码")
      validateSize(password, 6, 32, "密码")
    }

    // 验证手机号
    val phone = input["phone"] as? String
    if (phone != null && phone.isNotEmpty()) {
      validatePhone(phone)
    }

    // 验证昵称
    val nickname = input["nickname"] as? String
    if (nickname != null && nickname.isNotEmpty()) {
      validateSize(nickname, 2, 50, "昵称")
    }

    // 验证年龄
    val age = input["age"] as? Int
    if (age != null) {
      validateRange(age.toDouble(), 1.0, 150.0, "年龄")
    }
  }

  /**
   * 验证地址输入
   */
  fun validateAddressInput(input: Map<String, Any>?) {
    if (input == null) {
      throw ValidationException("地址信息不能为空")
    }

    // 验证收货人姓名
    val receiverName = input["receiverName"] as? String
    if (receiverName != null) {
      validateNotBlank(receiverName, "收货人姓名")
      validateSize(receiverName, 2, 20, "收货人姓名")
    }

    // 验证收货人手机号
    val phone = input["phone"] as? String
    if (phone != null) {
      validateNotBlank(phone, "收货人手机号")
      validatePhone(phone)
    }

    // 验证省市区
    val province = input["province"] as? String
    if (province != null) {
      validateNotBlank(province, "省份")
    }

    val city = input["city"] as? String
    if (city != null) {
      validateNotBlank(city, "城市")
    }

    val district = input["district"] as? String
    if (district != null) {
      validateNotBlank(district, "区县")
    }

    // 验证详细地址
    val detailAddress = input["detailAddress"] as? String
    if (detailAddress != null) {
      validateNotBlank(detailAddress, "详细地址")
      validateSize(detailAddress, 5, 200, "详细地址")
    }

    // 验证经纬度
    val longitude = input["longitude"] as? Double
    if (longitude != null) {
      validateRange(longitude, -180.0, 180.0, "经度")
    }

    val latitude = input["latitude"] as? Double
    if (latitude != null) {
      validateRange(latitude, -90.0, 90.0, "纬度")
    }
  }

  /**
   * 验证订单输入
   */
  fun validateOrderInput(input: Map<String, Any>?) {
    if (input == null) {
      throw ValidationException("订单信息不能为空")
    }

    // 验证商品ID
    val productId = input["productId"] as? Number
    if (productId != null) {
      validatePositive(productId.toDouble(), "商品ID")
    }

    // 验证地址ID
    val addressId = input["addressId"] as? Number
    if (addressId != null) {
      validatePositive(addressId.toDouble(), "地址ID")
    }

    // 验证购买数量
    val quantity = input["quantity"] as? Int
    if (quantity != null) {
      validateRange(quantity.toDouble(), 1.0, 999.0, "购买数量")
    }
  }

  /**
   * 验证创建商品输入
   */
  fun validateCreateProductInput(input: Map<String, Any>?) {
    if (input == null) {
      throw ValidationException("商品信息不能为空")
    }

    // 验证商品名称
    val name = input["name"] as? String
    if (name != null) {
      validateNotBlank(name, "商品名称")
      validateSize(name, 2, 100, "商品名称")
    }

    // 验证价格
    val price = input["price"] as? Number
    if (price != null) {
      validatePositive(price.toDouble(), "商品价格")
      validateRange(price.toDouble(), 0.01, 99999.99, "商品价格")
    }

    // 验证封面图片URL
    val coverImageUrl = input["coverImageUrl"] as? String
    if (coverImageUrl != null) {
      validateNotBlank(coverImageUrl, "商品封面图片")
      validateSize(coverImageUrl, 1, 500, "封面图片URL")
    }

    // 验证库存
    val stock = input["stock"] as? Int
    if (stock != null) {
      validateRange(stock.toDouble(), 0.0, 99999.0, "库存数量")
    }
  }

  /**
   * 验证分页参数
   */
  fun validatePaginationParams(page: Int?, size: Int?) {
    if (page != null) {
      validateRange(page.toDouble(), 1.0, 1000.0, "页码")
    }
    if (size != null) {
      validateRange(size.toDouble(), 1.0, 100.0, "每页数量")
    }
  }

  // 基础验证方法

  fun validateNotBlank(value: String, fieldName: String) {
    if (value.trim().isEmpty()) {
      throw ValidationException("$fieldName 不能为空")
    }
  }

  fun validateSize(value: String, min: Int?, max: Int?, fieldName: String) {
    val length = value.length
    if (min != null && length < min || max != null && length > max) {
      val message = when {
        min != null && max != null -> "$fieldName 长度必须在${min}-${max}个字符之间"
        min != null -> "$fieldName 长度不能少于${min}个字符"
        max != null -> "$fieldName 长度不能超过${max}个字符"
        else -> "$fieldName 长度不符合要求"
      }
      throw ValidationException(message)
    }
  }

  fun validateEmail(value: String) {
    if (!emailPattern.matcher(value).matches()) {
      throw ValidationException("邮箱格式不正确")
    }
  }

  fun validatePhone(value: String) {
    if (!phonePattern.matcher(value).matches()) {
      throw ValidationException("手机号格式不正确")
    }
  }

  fun validatePattern(value: String, regex: String, fieldName: String, message: String = "格式不符合要求") {
    val pattern = Pattern.compile(regex)
    if (!pattern.matcher(value).matches()) {
      throw ValidationException("$fieldName $message")
    }
  }

  fun validateRange(value: Double, min: Double?, max: Double?, fieldName: String) {
    if (min != null && value < min || max != null && value > max) {
      val message = when {
        min != null && max != null -> "$fieldName 必须在${min}-${max}之间"
        min != null -> "$fieldName 不能小于${min}"
        max != null -> "$fieldName 不能大于${max}"
        else -> "$fieldName 超出范围"
      }
      throw ValidationException(message)
    }
  }

  fun validatePositive(value: Double, fieldName: String) {
    if (value <= 0) {
      throw ValidationException("$fieldName 必须为正数")
    }
  }
}
