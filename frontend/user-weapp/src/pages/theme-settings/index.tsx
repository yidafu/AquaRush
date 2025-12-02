import React, { useState } from 'react'
import { View, Text } from '@tarojs/components'
import { useTheme, ThemePreview } from '../../components/ThemeProvider'
import Taro from '@tarojs/taro'
import './index.scss'

const ThemeSettings: React.FC = () => {
  const { themeName, setTheme } = useTheme()
  const [isSaving, setIsSaving] = useState(false)

  // 可用主题列表
  const availableThemes = [
    { key: 'aqua', label: '水蓝色', description: '清澈的水蓝色，清新自然' },
    { key: 'blue', label: '天空蓝', description: '明亮的蓝色，充满活力' },
    { key: 'green', label: '自然绿', description: '自然的绿色，健康环保' },
    { key: 'purple', label: '梦幻紫', description: '优雅的紫色，时尚高端' },
  ]

  // 处理主题切换
  const handleThemeSelect = (selectedTheme: string) => {
    try {
      setIsSaving(true)

      // 添加延迟以显示加载效果
      setTimeout(() => {
        setTheme(selectedTheme as any)
        setIsSaving(false)

        // 显示成功提示
        Taro.showToast({
          title: '主题切换成功',
          icon: 'success',
          duration: 2000,
        })

        // 返回上一页
        setTimeout(() => {
          Taro.navigateBack()
        }, 500)
      }, 800)

    } catch (error) {
      console.error('Failed to switch theme:', error)
      setIsSaving(false)

      Taro.showToast({
        title: '主题切换失败',
        icon: 'error',
        duration: 2000,
      })
    }
  }

  // 页面配置
  Taro.useShareAppMessage(() => {
    return {
      title: 'AquaRush 主题设置',
      path: '/pages/theme-settings/index',
    }
  })

  return (
    <View className="theme-settings-page">
      {/* 页面头部 */}
      <View className="theme-settings-header">
        <Text className="theme-settings-title">主题设置</Text>
        <Text className="theme-settings-subtitle">选择您喜欢的主题颜色</Text>
      </View>

      {/* 主题选择区域 */}
      <View className="theme-settings-section">
        <View className="theme-settings-section-title">
          <Text>预设主题</Text>
        </View>
        <View className="theme-settings-section-content">
          <View className="theme-preview-container">
            {availableThemes.map((theme) => (
              <ThemePreview
                key={theme.key}
                theme={theme.key as any}
                isSelected={themeName === theme.key}
                onSelect={() => handleThemeSelect(theme.key)}
              />
            ))}
          </View>
        </View>
      </View>

      {/* 主题详情 */}
      <View className="theme-settings-section">
        <View className="theme-settings-section-title">
          <Text>当前主题</Text>
        </View>
        <View className="theme-settings-section-content">
          <View className="current-theme-info">
            {availableThemes.map((theme) => {
              if (theme.key === themeName) {
                return (
                  <View key={theme.key} className="current-theme-details">
                    <Text className="current-theme-name">{theme.label}</Text>
                    <Text className="current-theme-desc">{theme.description}</Text>
                  </View>
                )
              }
              return null
            })}
          </View>
        </View>
      </View>

      {/* 使用说明 */}
      <View className="theme-settings-section">
        <View className="theme-settings-section-title">
          <Text>使用说明</Text>
        </View>
        <View className="theme-settings-section-content">
          <View className="theme-usage-tips">
            <Text className="usage-tip">• 点击主题颜色即可切换应用主题</Text>
            <Text className="usage-tip">• 主题设置会自动保存，下次启动时生效</Text>
            <Text className="usage-tip">• 不同主题会影响应用的颜色风格和视觉效果</Text>
            <Text className="usage-tip">• 可以随时在设置页面中更换主题</Text>
          </View>
        </View>
      </View>

      {/* 加载遮罩 */}
      {isSaving && (
        <View className="theme-saving-overlay">
          <View className="theme-saving-content">
            <View className="theme-loading-spinner" />
            <Text className="theme-saving-text">主题切换中...</Text>
          </View>
        </View>
      )}
    </View>
  )
}

export default ThemeSettings