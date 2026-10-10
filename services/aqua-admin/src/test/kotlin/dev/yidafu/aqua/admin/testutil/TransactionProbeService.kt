/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.admin.testutil

import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 事务边界探针(P5 事务闭合验证用,仅在测试 classpath 上存在)。
 *
 * 目的:证明 `@Transactional` 覆盖 Exposed 数据访问 —— 同一事务里跨**两个互不相关仓储/表**
 * (users + regions)写入,抛异常后两笔必须一起回滚。若 Exposed 仓储各自开连接(迁移期语义),
 * 第一笔已提交,回滚断言会失败。
 *
 * 注意:测试 classpath 上的 @Service 会被 `@SpringBootTest` 的组件扫描一并装载,
 * 因此本类会出现在每个 admin 集成测试上下文里(无副作用,只持有两个仓储)。
 */
@Service
class TransactionProbeService(
  private val userRepository: UserRepository,
  private val regionRepository: RegionRepository,
) {
  /** 两个仓储写入后抛异常:调用方断言两笔都不可见。 */
  @Transactional
  fun writeTwoAndThrow(
    openId: String,
    regionCode: String,
  ): Nothing {
    writeBoth(openId, regionCode)
    throw IllegalStateException("transaction probe rollback")
  }

  /** 两个仓储写入后正常返回:调用方断言两笔都可见。 */
  @Transactional
  fun writeTwo(
    openId: String,
    regionCode: String,
  ) {
    writeBoth(openId, regionCode)
  }

  private fun writeBoth(
    openId: String,
    regionCode: String,
  ) {
    userRepository.save(
      UserModel(
        wechatOpenId = openId,
        email = "$openId@test.local",
      ),
    )
    regionRepository.save(
      RegionModel(
        name = "事务探针区",
        code = regionCode,
        parentCode = "000000",
        level = 4,
      ),
    )
  }
}
