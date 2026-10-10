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

package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.TransactionProbeService
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager

/**
 * 事务闭合验收(P5):`@Transactional` 必须覆盖 Exposed 数据访问。
 *
 * 迁移期 Exposed 仓储各自 `transaction(database) { }` 开独立连接,服务层事务不覆盖数据访问;
 * P5 注册 Exposed `SpringTransactionManager` 为主事务管理器后,同一 `@Transactional` 方法内
 * 跨多个仓储的写入必须一起提交/回滚。本测试用 [TransactionProbeService] 写 users + regions
 * 两张互不相关的表(两个不同仓储),分别验证回滚与提交路径。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/security/"])
@ApplyExtension(SpringExtension::class)
class TransactionBoundaryIntegrationTest(
  private val probe: TransactionProbeService,
  private val userRepository: UserRepository,
  private val regionRepository: RegionRepository,
  private val transactionManager: PlatformTransactionManager,
) : FunSpec({
    test("主事务管理器必须是 Exposed SpringTransactionManager") {
      transactionManager::class.java.name shouldBe "org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager"
    }

    test("@Transactional 内跨仓储写入抛异常时整体回滚") {
      val openId = "tx-probe-rollback-${System.nanoTime()}"
      val regionCode = "TX${System.nanoTime() % 1_000_000}"

      shouldThrow<IllegalStateException> { probe.writeTwoAndThrow(openId, regionCode) }

      withClue("第一个仓储(users)的写入必须随事务回滚") {
        userRepository.findByWechatOpenId(openId).shouldBeNull()
      }
      withClue("第二个仓储(regions)的写入必须随同一事务回滚") {
        regionRepository.findByCode(regionCode).shouldBeNull()
      }
    }

    test("@Transactional 内跨仓储写入正常提交,两张表都能查到") {
      val openId = "tx-probe-commit-${System.nanoTime()}"
      val regionCode = "TX${System.nanoTime() % 1_000_000}"

      probe.writeTwo(openId, regionCode)

      withClue("users 写入应可见") {
        userRepository.findByWechatOpenId(openId).shouldNotBeNull()
      }
      withClue("regions 写入应可见(第二个仓储同一事务)") {
        regionRepository.findByCode(regionCode).shouldNotBeNull()
      }
    }
  })
