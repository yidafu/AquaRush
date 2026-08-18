# Frontend Development Reference

> Imported by the root `CLAUDE.md`. Frontend architecture, React/TypeScript & Taro page patterns, and UI design guidelines.

## Frontend Architecture

### WeChat Mini Programs (Taro + React)

**User Mini Program** (`frontend/user-weapp`): Product browsing, order placement, payment, address management, order history, marketing touchpoints (coupon center `coupon-center/`, coupon list `coupon-list/`, ticket shop `ticket-shop/`, ticket list `ticket-list/`).

It uses:
- Taro 4 framework for cross-platform Mini Program development (React-based)
- TypeScript for type safety
- taro-ui component library
- Custom TabBar icons and navigation

### Delivery App (`frontend/delivery-app`, Expo + React Native)

A native delivery client built with Expo SDK 57 and expo-router (file-based routing). It is part of the pnpm workspace. See `frontend/delivery-app/README.md` for structure and `AGENTS.md` for the versioned Expo docs requirement.

### Shared Frontend Package (`frontend/common`, `@aquarush/common`)

A pnpm-workspace package shared by `user-weapp`, `delivery-app`, and `admin-client`. Holds GraphQL TypeScript types plus `money` (Long-cents ↔ yuan formatting), `date`, `image`, and `network` utils. All money formatting/parsing in the frontends should go through `@aquarush/common/utils/money`.

### Admin Dashboard (`frontend/admin-client`, React + Vite + Ant Design)

- React 18 + TypeScript with Ant Design 5
- Vite for fast development and building
- Role-based dashboard with different views for admin users (super-admin-only menu items for admin-account management and system settings)
- Real-time order management and analytics
- Domains surfaced as menu groups: 运营配置 (`/ops/*` — banners, notices, feedback, store profile, delivery fee rule), 营销管理 (`/marketing/*` — customer groups, group prices, coupons, promotions, water tickets, flash sale, buy-gift, newcomer), 系统设置 (`/system-config/*` — parameters, order/fulfillment params, stock thresholds), 日志管理 (`/logs/*` — API logs, user actions, business logs)

## 声明式埋点（用户操作上报）

三端（weapp / 浏览器 / RN）统一走同一套声明式埋点：业务 JSX 只写 `track-*` props，由 `babel-plugin-aquarush-track`（仓库根目录 workspace 包）在编译期改写成 `@aquarush/common/tracking` 的 `<Track>` 包裹并自动注入 import。业务代码不 import、不调用任何上报函数。

```tsx
// 业务代码（三端一致）
<Button trackTarget="login-submit" trackElementText="登录" trackProps={{ username }} onClick={handleSubmit}>
  登录
</Button>

// 编译产物（插件自动完成）
<Track target="login-submit" elementText="登录" props={{ username }}>
  <Button onClick={handleSubmit}>登录</Button>
</Track>
```

- **契约**：`trackTarget`（必填）、`trackAction`、`trackProps`（对象/JSON 字符串）、`trackElementId`、`trackElementType`、`trackElementText`、`trackDisabled`（动态跳过上报）、`trackIgnore`（静态剥离）、散列键 `trackXxx` → `properties.xxx`。类型定义见 `@aquarush/common/tracking` 的 `TrackAttributes`。
- **机制**：`<Track>` 通过 `React.cloneElement` 把组合 handler 注入子元素的 `onPress/onClick/onSubmit/onConfirm`（先上报、后转发原 handler），RN 嵌套可点击组件无 responder 抢占问题。子组件需转发事件属性。
- **接线**：delivery-app `babel.config.js`（`babel-preset-expo`，Metro）、admin-client `vite.config.ts`（`@vitejs/plugin-react` 的 `babel.plugins`）。user-weapp 的 Taro 4 vite 编译器用 esbuild 转 JSX、不读 babel.config.js 的 plugins，因此走 vite 插件入口：`config/index.ts` 的 `compiler.vitePlugins` 挂 `aquarushTrackVitePlugin()`（`babel-plugin-aquarush-track/vite`）；babel.config.js 里保留插件条目供 jest 等 babel 消费方使用。
- **类型**：自研组件 props `extends TrackAttributes`（如 delivery-app `Button`）；weapp 由 `src/types/tracking.d.ts` 增强 Taro `StandardProps` 与 taro-ui `AtComponent`；三方组件用 `{...( {...} as TrackAttributes )}` 断言 spread。
- **例外**：handler 期才可知的属性（如表单回调里的值、确认弹窗后的登出、需显式用户上下文的 logout）保留 `userActionTracker.track(...)` 显式调用（同一 SDK 单例，逻辑不变）。PAGE_VIEW 由各端 `usePageViewTracking` + 路由映射自动上报。

## React/TypeScript Development Requirements

**IMPORTANT**: All React components in frontend projects must use functional components with TypeScript annotations, not class components. This ensures:

- Better type safety and developer experience
- Compatibility with modern React patterns (hooks, suspense, etc.)
- Improved code maintainability and testing
- Consistency across all frontend applications

**Functional Component Pattern**:

```typescript
import React, { useState, useEffect } from 'react'

interface Props {
  title: string
  onSubmit: (data: FormData) => void
}

const MyComponent: React.FC<Props> = ({ title, onSubmit }) => {
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // Component logic
  }, [])

  const handleSubmit = (data: FormData) => {
    setLoading(true)
    onSubmit(data)
  }

  return (
    <div>
      <h1>{title}</h1>
      {/* Component JSX */}
    </div>
  )
}

export default MyComponent
```

**Taro Page Component Pattern**:

```typescript
import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import {
  useReady,
  useDidShow,
  useDidHide,
  usePullDownRefresh,
  useReachBottom,
  usePageScroll,
  useShareAppMessage,
  useShareTimeline,
  useAddToFavorites,
  useTitleClick,
  useOptionMenuClick,
  useResize,
  useTabItemTap,
  useBeforeLeave,
  useSaveExitState,
  useRestoreExitState
} from '@tarojs/taro'

interface Props {
  // Page props (rarely used in Taro pages)
}

const MyPage: React.FC<Props> = () => {
  const [data, setData] = useState<any>(null)
  const [loading, setLoading] = useState<boolean>(false)

  // Standard React hooks
  useEffect(() => {
    // Component mounted logic
    console.log('Component mounted')

    return () => {
      // Cleanup logic
      console.log('Component unmounted')
    }
  }, [])

  // Taro page lifecycle hooks
  useReady(() => {
    // 对应类组件的 componentDidMount 和 componentDidReady
    console.log('Page ready')
    // 页面准备就绪时的逻辑
  })

  useDidShow(() => {
    // 对应类组件的 componentDidShow
    console.log('Page shown')
    // 页面显示时的逻辑（每次页面显示都会调用）
    loadData()
  })

  useDidHide(() => {
    // 对应类组件的 componentDidHide
    console.log('Page hidden')
    // 页面隐藏时的逻辑
  })

  usePullDownRefresh(() => {
    // 对应类组件的 onPullDownRefresh
    console.log('Pull down refresh')
    handleRefresh()
  })

  useReachBottom(() => {
    // 对应类组件的 onReachBottom
    console.log('Reach bottom')
    loadMore()
  })

  usePageScroll((event) => {
    // 对应类组件的 onPageScroll
    console.log('Page scroll:', event.scrollTop)
  })

  useShareAppMessage((res) => {
    // 对应类组件的 onShareAppMessage
    return {
      title: '分享标题',
      path: '/pages/index/index'
    }
  })

  useShareTimeline(() => {
    // 对应类组件的 onShareTimeline
    return {
      title: '朋友圈分享标题'
    }
  })

  // 自定义方法
  const loadData = async () => {
    try {
      setLoading(true)
      // 加载数据逻辑
      const result = await Taro.request({
        url: 'your-api-url',
        method: 'GET'
      })
      setData(result.data)
    } catch (error) {
      console.error('Load data failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    try {
      await loadData()
      Taro.stopPullDownRefresh()
    } catch (error) {
      console.error('Refresh failed:', error)
      Taro.stopPullDownRefresh()
    }
  }

  const loadMore = async () => {
    // 加载更多逻辑
    console.log('Loading more data...')
  }

  return (
    <View className='page'>
      {loading ? (
        <Text>加载中...</Text>
      ) : (
        <View>
          {/* 页面内容 */}
          <Text>Page Content</Text>
        </View>
      )}
    </View>
  )
}

export default MyPage
```

**TypeScript Requirements**:

- All components must have explicit `Props` interfaces
- Use `React.FC<Props>` type for functional components
- All state variables should be typed explicitly
- All functions must have parameter and return type annotations
- Use proper TypeScript event handling (e.g., `React.MouseEvent`, `React.ChangeEvent`)

## GraphQL 数据请求规范（单页请求合并）

**IMPORTANT**: 一个页面的一次渲染只发起**一个** GraphQL 请求。GraphQL 允许在单个 query 中通过字段选择一次取回该页所需全部数据（如列表 + 详情 + 统计等），三端（user-weapp / admin-client / delivery-app）都必须遵守：

- 禁止为同一页面发起多个独立 GraphQL 请求（无论串行还是并行）——每次网络往返都会拖慢页面加载，移动端尤为明显。
- 页面数据获取应聚合为一个 query（可用 fragment 复用字段选择），由同一个数据请求统一发出。
- 例外：确有独立刷新语义的区域（如下拉分页加载更多、轮询状态）可以单独发请求，但页面初始渲染仍必须合并为一次。

## UI Design and Layout Guidelines

> 三端（user-weapp / admin-client / delivery-app）UI 重设计的品牌级规范见 `docs/ui-design-system.md`；各端页面级规范见各自 `docs/ui-spec.md`（user-weapp、admin-client、delivery-app）。实施页面改动前先对照对应规范。

**IMPORTANT**: All UI components should be designed for 1x scale (实际像素尺寸) to ensure proper display and usability on devices. This ensures:

- Better readability and accessibility
- Larger touch targets for mobile users
- Improved user experience on smaller screens
- Consistent visual hierarchy across all pages

**Page Layout Standards**:

```scss
// 页面容器
.page {
  min-height: 100vh;
  padding: 60px 32px 32px; // 1x 倍尺寸，充足的间距
  box-sizing: border-box;
}

// 标题样式
.title {
  font-size: 40px; // 大标题使用 40px
  font-weight: bold;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 20px; // 副标题使用 20px
  margin-bottom: 12px;
}

// 表单元素
.form-input {
  height: 64px; // 输入框高度 64px
  font-size: 20px; // 输入文字大小 20px
  padding: 16px 20px;
  border-radius: 12px;
}

.form-textarea {
  min-height: 180px; // 文本域最小高度 180px
  font-size: 20px; // 文本大小 20px
  padding: 20px;
  line-height: 1.6;
}

// 按钮样式
.primary-button {
  height: 64px; // 按钮高度 64px
  font-size: 20px; // 按钮文字大小 20px
  border-radius: 16px;
  font-weight: 600;
}

// 卡片和容器
.card {
  padding: 32px; // 卡片内边距 32px
  border-radius: 20px;
  margin-bottom: 32px;
}

// 图片上传区域
.image-upload-item {
  width: 140px; // 图片预览尺寸 140px x 140px
  height: 140px;
  border-radius: 12px;
}

// 选择器按钮
.selector-option {
  padding: 12px 24px;
  border-radius: 28px;
  font-size: 16px;
  min-height: 48px;
}
```

**Touch Target Guidelines**:

- **Minimum touch target**: 48px × 48px (符合无障碍标准)
- **Recommended button height**: 64px for primary actions
- **Input field height**: 64px for better touch accuracy
- **Icon buttons**: Minimum 32px × 32px with adequate padding

**Typography Scale**:

- **Page titles**: 40px, bold
- **Section headers**: 24px, semibold
- **Body text**: 16px, regular
- **Small text**: 14px, regular
- **Form labels**: 20px, semibold

**Spacing System**:

- **Page margins**: 32px (left/right), 60px (top)
- **Section spacing**: 32px - 48px
- **Element spacing**: 16px - 24px
- **Component padding**: 20px - 32px

**Accessibility Considerations**:

- All interactive elements must meet WCAG AA contrast standards
- Focus states should be clearly visible with 4px minimum outline
- Text should be readable at 1x scale without requiring zoom
- Touch targets should have adequate spacing between them
