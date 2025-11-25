package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.user.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByWechatOpenId(wechatOpenId: String): User?
    fun existsByWechatOpenId(wechatOpenId: String): Boolean
}
