package dev.yidafu.aqua.user.ext

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.AdminRoleModel
import org.springframework.security.core.authority.SimpleGrantedAuthority

fun AdminRoleModel.toSimpleGrantedAuthority() = SimpleGrantedAuthority("ROLE_${this.name}")

fun AdminModel.getAuthorities(): List<SimpleGrantedAuthority> = listOf(role.toSimpleGrantedAuthority())
