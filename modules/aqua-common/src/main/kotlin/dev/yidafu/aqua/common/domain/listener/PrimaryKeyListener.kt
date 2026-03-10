package dev.yidafu.aqua.common.domain.listener

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import jakarta.persistence.PrePersist
import org.springframework.stereotype.Component

@Component
class PrimaryKeyListener {
  private val generator = DefaultIdGenerator()

  @PrePersist
  fun onPrePersist(entity: Any) {
    val entityClass = entity.javaClass
    val idFiled =
      try {
        entityClass.getDeclaredField("id")
      } catch (e: NoSuchFieldException) {
        return
      }
    idFiled.isAccessible = true
    val currentId = idFiled.getLong(entity)
    if (currentId < 0L) {
      idFiled.set(entity, generator.generate())
    }
  }
}
