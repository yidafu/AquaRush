package dev.yidafu.aqua.common.annotation

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.id.IdGenerator
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import java.util.*

class SnowflakeIdentifierGenerator : BeforeExecutionGenerator {
  // Hibernate 6.x requires no-arg constructor for SPI discovery via @IdGeneratorType
  constructor() : this(DefaultIdGenerator())

  private val generator: IdGenerator

  constructor(generator: IdGenerator) {
    this.generator = generator
  }

  override fun generate(
    session: SharedSessionContractImplementor,
    owner: Any?,
    currentValue: Any?,
    eventType: EventType,
  ): Any = generator.generate()

  override fun generatedOnExecution(
    entity: Any?,
    session: SharedSessionContractImplementor?,
  ): Boolean = false

  override fun getEventTypes(): EnumSet<EventType> = EnumSet.of(EventType.INSERT)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@IdGeneratorType(SnowflakeIdentifierGenerator::class)
annotation class SnowflakeIdGenerator
