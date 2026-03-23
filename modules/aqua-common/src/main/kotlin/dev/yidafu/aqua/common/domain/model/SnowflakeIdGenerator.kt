package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.id.IdGenerator
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.id.IdentifierGenerator
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type
import java.util.*

inline fun isEmptyId(id: Any?): Boolean = Objects.isNull(id) || (id is String && id.isEmpty()) || (id is Long && id <= 0)

class SnowflakeIdentifierGenerator :
  IdentifierGenerator,
  BeforeExecutionGenerator {
  // Hibernate 6.x requires no-arg constructor for SPI discovery
  constructor() : this(DefaultIdGenerator())

  private val generator: IdGenerator

  constructor(generator: IdGenerator) {
    this.generator = generator
  }

  override fun configure(
    type: Type?,
    parameters: Properties?,
    serviceRegistry: ServiceRegistry?,
  ) {
  }

  private fun getId(
    entity: Any?,
    session: SharedSessionContractImplementor,
  ): Any? = session.getEntityPersister(null, entity!!).getIdentifier(entity, session)

  override fun generatedOnExecution(
    entity: Any?,
    session: SharedSessionContractImplementor?,
  ): Boolean {
    val id = getId(entity, session!!)
    return isEmptyId(id)
  }

  override fun generate(
    session: SharedSessionContractImplementor?,
    entity: Any?,
  ): Any? {
    val id = getId(entity, session!!)
    if (isEmptyId(id)) {
      if (entity != null) {
        val newId = generator.generate()
        session.getEntityPersister(null, entity).setIdentifier(entity, newId, session)
        return newId
      }
    }
    return id
  }

  override fun getEventTypes(): EnumSet<EventType?> = EnumSet.of(EventType.INSERT)

  override fun generatesSometimes(): Boolean = true
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@IdGeneratorType(SnowflakeIdentifierGenerator::class)
annotation class SnowflakeIdGenerator
