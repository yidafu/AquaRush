/**
 * admin-client 单测配置(jest)
 * 覆盖率基线详见 frontend/TESTING.md。
 * 注意:admin-client 为 ESM("type": "module")包,配置文件名用 .cjs。
 *
 * 环境:jsdom —— 既有 10 个 .ts 逻辑套件在 jsdom 下全部通过(http.test 的
 * Object.defineProperty(globalThis,'localStorage') 可覆盖 jsdom 自带实现,
 * 因其 configurable:true);新 .test.tsx 组件套件需要 DOM。
 *
 * 转译:babel-jest 统一吃 .ts/.tsx —— preset-typescript 解析 TS,
 * preset-react(runtime: automatic)解析 JSX,@stylexjs/babel-plugin 把
 * stylex.create/defineVars 编译为原子类名。
 *
 * stylex 参数与 vite 插件(vite-plugin-aquarush-stylex/src/index.ts)对齐
 * (unstable_moduleResolution commonJS, rootDir=仓库根,保证跨包 token
 * @aquarush/common/theme/tokens.stylex 的 var 名一致),但 runtimeInjection
 * 与之相反地设为 true:vite 靠 generateBundle 抽取 CSS,jest 没有该阶段,
 * false 会把编译出的 CSS 直接丢弃 → jsdom 里 getComputedStyle 全空,
 * T7 的 ellipsis/overflow/sticky 样式断言无法成立。实测 true/false 下
 * className 完全一致,仅决定 CSS 是否注入。
 */
const path = require('path')

const REPO_ROOT = path.resolve(__dirname, '../..')

module.exports = {
  testEnvironment: 'jsdom',
  // jsdom 无 TextEncoder/TextDecoder(react-router 7 需要),启动前 polyfill
  setupFiles: ['<rootDir>/jest.setup.cjs'],
  testMatch: ['<rootDir>/src/**/*.test.ts', '<rootDir>/src/**/*.test.tsx'],
  // `@/` 别名与 vite 对齐(http.ts 等已从 `@/ui` 导入 message);
  // 测试用 jest.mock('@/ui', factory) 拦截。
  moduleNameMapper: { '^@/(.*)$': '<rootDir>/src/$1' },
  transform: {
    '^.+\\.[tj]sx?$': [
      'babel-jest',
      {
        presets: [
          ['@babel/preset-env', { targets: { node: 'current' } }],
          '@babel/preset-typescript',
          ['@babel/preset-react', { runtime: 'automatic' }],
        ],
        plugins: [
          [
            require.resolve('@stylexjs/babel-plugin'),
            {
              runtimeInjection: true,
              unstable_moduleResolution: {
                type: 'commonJS',
                rootDir: REPO_ROOT,
              },
            },
          ],
        ],
      },
    ],
  },
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  // 默认忽略 node_modules;例外:@tanstack/*(table-core/react-table/store)v9 仅发
  // ESM("type":"module" + dist/*.js ESM),须经 babel-jest 转 CJS 才能被 require。
  // 匹配写法兼容 pnpm 布局(.pnpm/@tanstack+xxx@ver/...)与扁平 node_modules/@tanstack/。
  transformIgnorePatterns: [
    '^(?!.*(?:@tanstack\\+|/node_modules/@tanstack/)).*node_modules',
    '\\.pnp\\.[^\\\\]+$',
  ],
  collectCoverageFrom: ['src/utils/**/*.ts', '!src/**/*.test.ts', '!src/**/*.test.tsx'],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
}
