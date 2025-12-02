import { Component, PropsWithChildren } from 'react'
import { ThemeProvider } from './components/ThemeProvider'

import './app.scss'

class App extends Component<PropsWithChildren> {

  componentDidMount () {
    // 初始化主题
    this.initAppTheme()
  }

  componentDidShow () {}

  componentDidHide () {}

  private initAppTheme() {
    // 可以在这里进行主题的初始化设置
    // 例如根据系统主题、用户偏好等设置默认主题
    try {
      const hour = new Date().getHours()
      // 根据时间自动切换主题（可选）
      if (hour >= 18 || hour < 6) {
        // 晚上可以使用更柔和的颜色
        // setTheme('purple')
      }
    } catch (error) {
      console.warn('Failed to initialize app theme:', error)
    }
  }

  // this.props.children 是将要会渲染的页面
  render () {
    return (
      <ThemeProvider defaultTheme="aqua">
        {this.props.children}
      </ThemeProvider>
    )
  }
}

export default App
