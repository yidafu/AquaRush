# AquaRush 配送与运营端 (delivery-app)

AquaRush 桶装水配送系统的配送与运营端应用，基于 Expo + React Native 构建，使用 expo-router 文件路由。

送水员与管理员共用一套 App：送水员登录后使用「订单（任务）/ 统计（个人业绩）/ 我的」三个 Tab；管理员登录后额外多一个「管理」Tab（经营看板 / 全部订单 / 送水员管理）。

## 技术栈

- [Expo](https://expo.dev) SDK 57
- [expo-router](https://docs.expo.dev/router/introduction) 文件路由
- React Native 0.86 / React 19
- TypeScript

## 设计规范

UI 规范（设计 token、组件样式、布局规则、验收清单）见 [docs/ui-spec.md](docs/ui-spec.md)。

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
│   ├── (tabs)/       # 底部 Tab（订单 / 统计 / 管理 / 我的）
│   │   ├── index.tsx       # 送水员任务列表
│   │   ├── statistics.tsx  # 送水员个人统计
│   │   ├── manage.tsx      # 管理员管理枢纽（看板 / 全部订单 / 送水员）
│   │   └── profile.tsx     # 个人中心
│   └── order/[orderNo].tsx # 订单详情（按角色展示操作）
├── components/
│   ├── ui/           # primitive 包装层（@rn-primitives + DialogFrame，无品牌样式）
│   └── manage/       # 管理员子屏（看板 / 全部订单 / 送水员）
├── constants/        # 主题常量（Colors / Spacing / Fonts）
└── hooks/            # 主题、认证等自定义 Hook
```

## 常用命令

```bash
npm run start    # 启动开发服务器
npm run android  # 启动并在 Android 模拟器打开
npm run ios      # 启动并在 iOS 模拟器打开
npm run web      # 启动并在浏览器打开
npm run lint     # ESLint 检查
```

## 功能

### 送水员（DELIVERY_WORKER）

- 配送任务列表（待接单 / 已接单 / 配送中）
- 订单详情与配送流程（接单、开始配送、拍照确认、收款）
- 个人中心与工作统计、每日收款、对账记录

### 管理员（ADMIN / SUPER_ADMIN）—「管理」Tab

- 经营看板：待处理订单三态、送水员/用户统计、送水员排行榜、近7天趋势
- 全部订单：状态筛选 + 订单号搜索 + 分页，详情内分配送水员
- 送水员：列表 + 统计概览
