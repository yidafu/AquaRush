// 清除认证数据的脚本
// 在小程序开发工具中运行

const clearAuth = () => {
  try {
    // 清除 token
    wx.removeStorageSync('auth_token')

    // 清除用户信息
    wx.removeStorageSync('user_info')

    // 清除其他认证相关数据
    wx.removeStorageSync('refresh_token')
    wx.removeStorageSync('login_time')

    console.log('✅ 认证数据清除成功')
    wx.showToast({
      title: '已清除登录信息',
      icon: 'success',
      duration: 2000
    })
  } catch (error) {
    console.error('❌ 清除失败:', error)
    wx.showToast({
      title: '清除失败',
      icon: 'error',
      duration: 2000
    })
  }
}

// 执行清除
clearAuth()