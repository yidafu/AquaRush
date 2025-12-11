import Taro from '@tarojs/taro'

/**
 * 清除本地存储的认证信息
 */
export const clearAuthData = (): void => {
  try {
    // 清除认证 token
    Taro.removeStorageSync('auth_token')

    // 清除用户信息
    Taro.removeStorageSync('user_info')

    // 清除其他可能的认证相关数据
    Taro.removeStorageSync('refresh_token')
    Taro.removeStorageSync('login_time')

    console.log('✅ 本地认证数据已清除')
  } catch (error) {
    console.error('❌ 清除认证数据失败:', error)
  }
}

/**
 * 检查当前认证状态
 */
export const checkAuthStatus = (): { hasToken: boolean; hasUserInfo: boolean } => {
  try {
    const token = Taro.getStorageSync('auth_token')
    const userInfo = Taro.getStorageSync('user_info')

    return {
      hasToken: !!token,
      hasUserInfo: !!userInfo
    }
  } catch (error) {
    console.error('检查认证状态失败:', error)
    return {
      hasToken: false,
      hasUserInfo: false
    }
  }
}

/**
 * 重新登录函数
 */
export const forceRelogin = (): void => {
  // 清除认证数据
  clearAuthData()

  // 提示用户重新登录
  Taro.showModal({
    title: '登录已过期',
    content: '您的登录状态已过期，请重新登录',
    showCancel: false,
    confirmText: '重新登录',
    success: () => {
      // 重定向到登录页面或首页
      Taro.reLaunch({
        url: '/pages/index/index'
      })
    }
  })
}

// 如果直接运行此文件，则清除认证数据
if (typeof window === 'undefined' && typeof global !== 'undefined') {
  clearAuthData()
}