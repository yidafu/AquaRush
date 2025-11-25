package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.delivery.domain.model.DeliveryArea
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeliveryAreaRepository : JpaRepository<DeliveryArea, UUID> {
    
    fun findByProvinceAndCityAndDistrict(
        province: String,
        city: String,
        district: String
    ): DeliveryArea?
    
    fun findByIsEnabledTrue(): List<DeliveryArea>
}
