import React, { useEffect, useState, createContext, useContext } from 'react';
import { initTheme, setTheme, getCurrentTheme, ThemePreset, ThemeColors } from '../../styles/theme';

interface ThemeContextType {
  theme: ThemeColors;
  themeName: ThemePreset;
  setTheme: (themeName: ThemePreset) => void;
  isInitialized: boolean;
}

const ThemeContext = createContext<ThemeContextType>({
  theme: getCurrentTheme(),
  themeName: 'aqua',
  setTheme: () => {},
  isInitialized: false,
});

export const useTheme = () => useContext(ThemeContext);

interface ThemeProviderProps {
  children: React.ReactNode;
  defaultTheme?: ThemePreset;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({
  children,
  defaultTheme = 'aqua'
}) => {
  const [theme, setCurrentTheme] = useState<ThemeColors>(getCurrentTheme());
  const [themeName, setCurrentThemeName] = useState<ThemePreset>(defaultTheme);
  const [isInitialized, setIsInitialized] = useState(false);

  // 初始化主题
  useEffect(() => {
    const initializeTheme = () => {
      try {
        // 初始化主题系统
        initTheme();

        // 获取当前主题
        const currentTheme = getCurrentTheme();
        setCurrentTheme(currentTheme);
        setIsInitialized(true);

        // 为页面应用CSS变量
        applyCSSVariables(currentTheme);

      } catch (error) {
        console.warn('Failed to initialize theme:', error);
        // 使用默认主题
        setCurrentTheme(getCurrentTheme());
        setIsInitialized(true);
      }
    };

    initializeTheme();
  }, []);

  // 应用CSS变量到页面
  const applyCSSVariables = (themeColors: ThemeColors) => {
    try {
      const page = getCurrentPages().pop();
      if (page && page.setData) {
        page.setData({
          cssVariables: {
            '--theme-primary': themeColors.primary,
            '--theme-primary-light': themeColors.primaryLight,
            '--theme-primary-dark': themeColors.primaryDark,
            '--theme-gradient-start': themeColors.gradientStart,
            '--theme-gradient-end': themeColors.gradientEnd,
            '--theme-secondary': themeColors.secondary,
            '--theme-success': themeColors.success,
            '--theme-warning': themeColors.warning,
            '--theme-error': themeColors.error,
            '--theme-text-primary': themeColors.textPrimary,
            '--theme-text-secondary': themeColors.textSecondary,
            '--theme-text-tertiary': themeColors.textTertiary,
            '--theme-background': themeColors.background,
            '--theme-background-page': themeColors.backgroundPage,
            '--theme-border': themeColors.border,
            '--theme-shadow-light': themeColors.shadowLight,
            '--theme-shadow-medium': themeColors.shadowMedium,
          },
          themeName: themeName,
        });
      }
    } catch (error) {
      console.warn('Failed to apply CSS variables:', error);
    }
  };

  // 设置主题
  const handleSetTheme = (newThemeName: ThemePreset) => {
    try {
      // 更新主题
      setTheme(newThemeName);

      // 获取新主题配置
      const newTheme = getCurrentTheme();
      setCurrentTheme(newTheme);
      setCurrentThemeName(newThemeName);

      // 应用CSS变量
      applyCSSVariables(newTheme);

      // 显示切换成功提示
      Taro.showToast({
        title: '主题切换成功',
        icon: 'success',
        duration: 1500,
      });

    } catch (error) {
      console.error('Failed to set theme:', error);
      Taro.showToast({
        title: '主题切换失败',
        icon: 'error',
        duration: 2000,
      });
    }
  };

  const contextValue: ThemeContextType = {
    theme,
    themeName,
    setTheme: handleSetTheme,
    isInitialized,
  };

  return (
    <ThemeContext.Provider value={contextValue}>
      {children}
    </ThemeContext.Provider>
  );
};

// 主题切换器组件
interface ThemeSwitcherProps {
  className?: string;
  showLabel?: boolean;
}

export const ThemeSwitcher: React.FC<ThemeSwitcherProps> = ({
  className = '',
  showLabel = false
}) => {
  const { themeName, setTheme } = useTheme();
  const [showPicker, setShowPicker] = useState(false);

  const themeOptions = [
    { key: 'aqua', label: '水蓝色', icon: '💧' },
    { key: 'blue', label: '天空蓝', icon: '🌊' },
    { key: 'green', label: '自然绿', icon: '🌿' },
    { key: 'purple', label: '梦幻紫', icon: '💜' },
  ];

  const handleThemeSelect = (selectedTheme: ThemePreset) => {
    setTheme(selectedTheme);
    setShowPicker(false);
  };

  const currentThemeOption = themeOptions.find(option => option.key === themeName);

  return (
    <view className={`theme-switcher ${className}`}>
      <view
        className="theme-switcher-trigger"
        onClick={() => setShowPicker(!showPicker)}
      >
        <text className="theme-switcher-icon">
          {currentThemeOption?.icon || '💧'}
        </text>
        {showLabel && (
          <text className="theme-switcher-label">
            {currentThemeOption?.label || '水蓝色'}
          </text>
        )}
        <text className="theme-switcher-arrow">▼</text>
      </view>

      {showPicker && (
        <view className="theme-switcher-popup">
          <view className="theme-switcher-popup-content">
            {themeOptions.map((option) => (
              <view
                key={option.key}
                className={`theme-option ${option.key === themeName ? 'theme-option-active' : ''}`}
                onClick={() => handleThemeSelect(option.key as ThemePreset)}
              >
                <text className="theme-option-icon">{option.icon}</text>
                <text className="theme-option-label">{option.label}</text>
                {option.key === themeName && (
                  <text className="theme-option-check">✓</text>
                )}
              </view>
            ))}
          </view>
        </view>
      )}
    </view>
  );
};

// 主题预览组件
interface ThemePreviewProps {
  theme: ThemePreset;
  isSelected?: boolean;
  onSelect?: (theme: ThemePreset) => void;
}

export const ThemePreview: React.FC<ThemePreviewProps> = ({
  theme,
  isSelected = false,
  onSelect
}) => {
  const themeConfigs = {
    aqua: { primary: '#00A8CC', label: '水蓝色' },
    blue: { primary: '#1890FF', label: '天空蓝' },
    green: { primary: '#52C41A', label: '自然绿' },
    purple: { primary: '#667EEA', label: '梦幻紫' },
  };

  const config = themeConfigs[theme];

  const handleClick = () => {
    if (onSelect) {
      onSelect(theme);
    }
  };

  return (
    <view
      className={`theme-preview ${isSelected ? 'theme-preview-selected' : ''}`}
      style={{ '--preview-color': config.primary } as React.CSSProperties}
      onClick={handleClick}
    >
      <view className="theme-preview-color" style={{ backgroundColor: config.primary }} />
      <text className="theme-preview-label">{config.label}</text>
      {isSelected && (
        <view className="theme-preview-selected-icon">
          <text>✓</text>
        </view>
      )}
    </view>
  );
};

export default ThemeProvider;