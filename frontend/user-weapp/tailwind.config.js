/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./public/index.html', './src/**/*.{html,js,ts,jsx,tsx,vue}'],
  theme: {
    extend: {
      colors: {
        // Map CSS custom properties to Tailwind colors
        primary: 'var(--theme-primary)',
        'primary-light': 'var(--theme-primary-light)',
        'primary-dark': 'var(--theme-primary-dark)',
        secondary: 'var(--theme-secondary)',
        success: 'var(--theme-success)',
        warning: 'var(--theme-warning)',
        error: 'var(--theme-error)',
        'text-primary': 'var(--theme-text-primary)',
        'text-secondary': 'var(--theme-text-secondary)',
        'text-tertiary': 'var(--theme-text-tertiary)',
        background: 'var(--theme-background)',
        'background-page': 'var(--theme-background-page)',
        border: 'var(--theme-border)',
      },
      backgroundImage: {
        'theme-gradient': 'linear-gradient(135deg, var(--theme-gradient-start) 0%, var(--theme-gradient-end) 100%)',
      },
      boxShadow: {
        'theme-light': '0 4px 16px var(--theme-shadow-light)',
        'theme-medium': '0 2px 8px var(--theme-shadow-medium)',
      },
      spacing: {
        '15': '60px', // For consistent spacing with design guidelines
      }
    },
  },
  plugins: [],
  corePlugins: {
    // 小程序不需要 preflight，因为这主要是给 h5 的，如果你要同时开发小程序和 h5 端，你应该使用环境变量来控制它
    preflight: false
  }
}

