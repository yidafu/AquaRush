package dev.yidafu.aqua.api.service.admin

import dev.yidafu.aqua.api.dto.LoginResponse

interface AdminAuthService {
  fun authenticate(
    username: String,
    password: String,
  ): LoginResponse
}
