package dev.yidafu.aqua.admin.repro

import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DailyReconciliationReproTest {

  @Autowired
  lateinit var reconciliationRepository: DailyReconciliationRepository

  @Test
  fun findAllReconciliations() {
    val result = reconciliationRepository.findAll()
    println("REPRO RESULT SIZE: ${result.size}")
    result.forEach { println("REPRO ROW: id=${it.id} date=${it.reconciliationDate} status=${it.status} reportData=${it.reportData}") }
  }
}
