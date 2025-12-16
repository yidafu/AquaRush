import Taro from "@tarojs/taro";

// 主题配置接口
export interface ThemeColors {
  // 主色调 - 水蓝色系
  primary: string;
  primaryLight: string;
  primaryDark: string;

  // 渐变色
  gradientStart: string;
  gradientEnd: string;

  // 功能色
  secondary: string;    // 橙色强调色
  success: string;      // 成功色
  warning: string;      // 警告色
  error: string;        // 错误色

  // 中性色
  textPrimary: string;
  textSecondary: string;
  textTertiary: string;
  background: string;
  backgroundPage: string;
  border: string;

  // 阴影
  shadowLight: string;
  shadowMedium: string;
}

// 默认水蓝色主题
export const aquaTheme: ThemeColors = {
  // 主色调 - 水蓝色系
  primary: '#00A8CC',      // 主水蓝色
  primaryLight: '#E6F7FB', // 浅水蓝色背景
  primaryDark: '#0088AA',  // 深水蓝色

  // 渐变色 - 水蓝色渐变
  gradientStart: '#00A8CC', // 清澈水蓝色
  gradientEnd: '#006B8A',   // 深海蓝色

  // 功能色
  secondary: '#FF6B35',   // 橙色强调色
  success: '#52C41A',     // 成功色
  warning: '#FAAD14',     // 警告色
  error: '#FF4D4F',      // 错误色

  // 中性色
  textPrimary: '#333333',     // 主要文字
  textSecondary: '#666666',   // 次要文字
  textTertiary: '#999999',    // 三级文字
  background: '#FFFFFF',      // 背景色
  backgroundPage: '#F5F5F5', // 页面背景色
  border: '#F0F0F0',         // 边框色

  // 阴影
  shadowLight: 'rgba(0, 168, 204, 0.08)',  // 水蓝色淡阴影
  shadowMedium: 'rgba(0, 0, 0, 0.05)',    // 中等阴影
};

// 备选主题配置
export const themePresets = {
  aqua: aquaTheme,
  blue: {
    ...aquaTheme,
    primary: '#1890FF',
    primaryLight: '#E6F7FF',
    primaryDark: '#096DD9',
    gradientStart: '#1890FF',
    gradientEnd: '#0050B3',
    shadowLight: 'rgba(24, 144, 255, 0.08)',
  } as ThemeColors,
  green: {
    ...aquaTheme,
    primary: '#52C41A',
    primaryLight: '#F6FFED',
    primaryDark: '#389E0D',
    gradientStart: '#52C41A',
    gradientEnd: '#237804',
    shadowLight: 'rgba(82, 196, 26, 0.08)',
  } as ThemeColors,
  purple: {
    ...aquaTheme,
    primary: '#667EEA',
    primaryLight: '#F0F2FF',
    primaryDark: '#4C63D2',
    gradientStart: '#667EEA',
    gradientEnd: '#764BA2',
    shadowLight: 'rgba(102, 126, 234, 0.08)',
  } as ThemeColors,
};

export type ThemePreset = keyof typeof themePresets;

// 当前使用的主题
let currentTheme: ThemeColors = aquaTheme;
let currentThemeName: ThemePreset = 'aqua';

// 主题管理类
export class ThemeManager {
  static getCurrentTheme(): ThemeColors {
    return currentTheme;
  }

  static getCurrentThemeName(): ThemePreset {
    return currentThemeName;
  }

  static setTheme(themeName: ThemePreset): void {
    currentTheme = themePresets[themeName];
    currentThemeName = themeName;

    // 保存到本地存储
    try {
      Taro.setStorageSync('app_theme', themeName);
    } catch (error) {
      console.warn('Failed to save theme to storage:', error);
    }

    // 应用CSS变量
    this.applyCSSVariables();
  }

  static setCustomTheme(colors: Partial<ThemeColors>): void {
    currentTheme = { ...currentTheme, ...colors };
    currentThemeName = 'aqua'; // 自定义主题归为aqua类型

    // 保存自定义主题配置
    try {
      Taro.setStorageSync('custom_theme', currentTheme);
    } catch (error) {
      console.warn('Failed to save custom theme to storage:', error);
    }

    this.applyCSSVariables();
  }

  static initTheme(): void {
    // 尝试从本地存储加载主题设置
    try {
      const savedThemeName = Taro.getStorageSync('app_theme') as ThemePreset;
      if (savedThemeName && themePresets[savedThemeName]) {
        currentTheme = themePresets[savedThemeName];
        currentThemeName = savedThemeName;
      } else {
        // 尝试加载自定义主题
        const customTheme = Taro.getStorageSync('custom_theme') as ThemeColors;
        if (customTheme) {
          currentTheme = customTheme;
          currentThemeName = 'aqua';
        }
      }
    } catch (error) {
      console.warn('Failed to load theme from storage:', error);
    }

    this.applyCSSVariables();
  }

  private static applyCSSVariables(): void {
    // 为小程序页面应用CSS变量（通过页面级样式类）
    const page = Taro.getCurrentPages().pop();
    if (page && page.setData) {
      page.setData({
        cssVariables: this.getCSSVariables(),
        themeName: currentThemeName,
      });
    }
  }

  static getCSSVariables(): Record<string, string> {
    return {
      '--theme-primary': currentTheme.primary,
      '--theme-primary-light': currentTheme.primaryLight,
      '--theme-primary-dark': currentTheme.primaryDark,
      '--theme-gradient-start': currentTheme.gradientStart,
      '--theme-gradient-end': currentTheme.gradientEnd,
      '--theme-secondary': currentTheme.secondary,
      '--theme-success': currentTheme.success,
      '--theme-warning': currentTheme.warning,
      '--theme-error': currentTheme.error,
      '--theme-text-primary': currentTheme.textPrimary,
      '--theme-text-secondary': currentTheme.textSecondary,
      '--theme-text-tertiary': currentTheme.textTertiary,
      '--theme-background': currentTheme.background,
      '--theme-background-page': currentTheme.backgroundPage,
      '--theme-border': currentTheme.border,
      '--theme-shadow-light': currentTheme.shadowLight,
      '--theme-shadow-medium': currentTheme.shadowMedium,
    };
  }

  // 获取主题相关的样式类名
  static getThemeClasses(): Record<string, string> {
    return {
      primary: 'theme-primary',
      primaryLight: 'theme-primary-light',
      primaryDark: 'theme-primary-dark',
      gradient: 'theme-gradient',
      secondary: 'theme-secondary',
      success: 'theme-success',
      warning: 'theme-warning',
      error: 'theme-error',
    };
  }
}

// 便捷的颜色获取函数
export const getThemeColor = (key: keyof ThemeColors): string => {
  return currentTheme[key];
};

// 初始化主题
export const initTheme = (): void => {
  ThemeManager.initTheme();
};

// 设置主题
export const setTheme = (themeName: ThemePreset): void => {
  ThemeManager.setTheme(themeName);
};

// 获取当前主题
export const getCurrentTheme = (): ThemeColors => {
  return ThemeManager.getCurrentTheme();
};
