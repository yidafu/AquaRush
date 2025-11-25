package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {
    fun findById(id: UUID): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun findByWechatOpenId(wechatOpenId: String): User? {
        return userRepository.findByWechatOpenId(wechatOpenId)
    }

    @Transactional
    fun createUser(wechatOpenId: String, nickname: String?, avatarUrl: String?): User {
        val user = User(
            wechatOpenId = wechatOpenId,
            nickname = nickname,
            avatarUrl = avatarUrl
        )
        return userRepository.save(user)
    }

    @Transactional
    fun updateUserInfo(userId: UUID, nickname: String?, phone: String?, avatarUrl: String?): User {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }
        
        nickname?.let { user.nickname = it }
        phone?.let { user.phone = it }
        avatarUrl?.let { user.avatarUrl = it }
        
        return userRepository.save(user)
    }

    fun existsByWechatOpenId(wechatOpenId: String): Boolean {
        return userRepository.existsByWechatOpenId(wechatOpenId)
    }
}
