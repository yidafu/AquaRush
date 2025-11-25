package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.user.domain.model.Address
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AddressRepository : JpaRepository<Address, UUID> {
    fun findByUserId(userId: UUID): List<Address>
    
    fun findByUserIdAndIsDefault(userId: UUID, isDefault: Boolean): Address?
    
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    fun clearDefaultByUserId(userId: UUID)
    
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Int
}
