# babel-plugin-aquarush-track

编译期声明式埋点插件：把 JSX 元素上的 `track-*` props 自动改写为 `@aquarush/common/tracking` 的 `<Track>` 包裹，并自动注入 import。业务代码只配 props，不显式调用任何上报函数。

TypeScript 源码（`src/`），`tsc` 编译到 `dist/`（CJS）。改源码后需重新构建：`pnpm --filter babel-plugin-aquarush-track build`。

## 用法

```jsx
// 输入(三端一致:weapp / 浏览器 / RN)
<Button trackTarget="login-submit" trackElementText="登录" trackProps={{ username }} onClick={handleSubmit}>
  登录
</Button>

// 编译产物(自动注入 import { Track } from '@aquarush/common/tracking')
<Track target="login-submit" elementText="登录" props={{ username }}>
  <Button onClick={handleSubmit}>登录</Button>
</Track>
```

## 契约

| JSX prop（camel 或 kebab） | Track prop | 说明 |
|---|---|---|
| `trackTarget` | `target` | 必填；缺失 → 告警并跳过该元素 |
| `trackAction` | `action` | 缺省 CLICK |
| `trackProps` | `props` | 对象表达式 / JSON 字符串（编译期解析）/ 其他表达式（`{...expr}`） |
| `trackElementId` | `elementId` | |
| `trackElementType` | `elementType` | |
| `trackElementText` | `elementText` | |
| `trackDisabled` | `disabled` | 动态跳过上报（仍转发事件），支持任意表达式 |
| `trackIgnore` | — | 值为真（`true`/`'true'`/`'1'`/常量表达式）→ 剥离全部 track-* 属性、不包裹 |
| 其他 `trackXxx` | — | 首字母小写为 `xxx`，合并进 `props` |

## 接线

```js
// user-weapp babel.config.js / delivery-app babel.config.js(Metro 等 babel 管线)
plugins: [['babel-plugin-aquarush-track', {}]]

// admin-client vite.config.ts(@vitejs/plugin-react 走 babel 管线)
react({ babel: { plugins: [['babel-plugin-aquarush-track', {}]] } })

// user-weapp Taro 4 vite 编译器(config/index.ts)——Taro vite 用 esbuild 转 JSX,
// 不读 babel.config.js 的 plugins,必须走 vite 插件入口:
import { aquarushTrackVitePlugin } from 'babel-plugin-aquarush-track/vite'
compiler: { type: 'vite', vitePlugins: [aquarushTrackVitePlugin()] }
```

选项：`{ moduleSource?: string }`，默认 `'@aquarush/common/tracking'`。

## 约束

- 只处理 JSX 属性；`createElement` 调用不在范围。
- 元素已有 `onClick/onPress/onSubmit/onConfirm` 保留在子元素上（Track 注入的 handler 链式调用，不冲突）。
- `key` 移到外层 `<Track>`，`ref` 留在内层元素。
- 不要在一个 `<Track>` 内再放带 track-* 的子元素（双重上报）。
- 子组件需转发 `onPress/onClick` 等事件属性（注入模式依赖）。

## 测试

```bash
pnpm --filter babel-plugin-aquarush-track test   # tsc 构建 + node --test
```
