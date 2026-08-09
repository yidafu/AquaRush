# AquaRush 送水工端 (delivery-app)

AquaRush 桶装水配送系统的送水工端应用，基于 Expo + React Native 构建，使用 expo-router 文件路由。

## 技术栈

- [Expo](https://expo.dev) SDK 57
- [expo-router](https://docs.expo.dev/router/introduction) 文件路由
- React Native 0.86 / React 19
- TypeScript

## 开始使用

```bash
npm install
npx expo start
```

启动后可在终端中选择打开方式：

- `i` — iOS 模拟器
- `a` — Android 模拟器
- `w` — Web 浏览器
- 或使用 [Expo Go](https://expo.dev/go) 扫码真机预览

## 目录结构

```text
src/
├── app/              # expo-router 页面（文件路由）
│   ├── _layout.tsx   # 根布局（主题 + 底部标签导航）
│   ├── index.tsx     # 配送任务列表
│   └── profile.tsx   # 个人中心
├── components/       # 可复用组件
├── constants/        # 主题常量（Colors / Spacing / Fonts）
└── hooks/            # 主题等自定义 Hook
```

## 常用命令

```bash
npm run start    # 启动开发服务器
npm run android  # 启动并在 Android 模拟器打开
npm run ios      # 启动并在 iOS 模拟器打开
npm run web      # 启动并在浏览器打开
npm run lint     # ESLint 检查
```

## 规划功能

- 配送任务列表（待接单 / 已接单）
- 订单详情与配送流程（接单、开始配送、拍照确认、收款）
- 个人中心与工作统计
