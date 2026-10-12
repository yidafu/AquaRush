/**
 * @aquarush/admin-ui 单测配置(jest)
 * 组件库自带的 DOM/logic 测试随库迁移自 admin-client src/ui。
 *
 * 环境:jsdom —— *.dom.test.tsx 需要 DOM(getComputedStyle / createRoot)。
 *
 * 转译:babel-jest 统一吃 .ts/.tsx —— preset-typescript 解析 TS,
 * preset-react(runtime: automatic)解析 JSX,@stylexjs/babel-plugin 把
 * stylex.create/defineVars 编译为原子类名。
 *
 * stylex 参数与 vite 插件(vite-plugin-aquarush-stylex/src/index.ts)对齐
 * (unstable_moduleResolution commonJS, rootDir=仓库根,保证跨包 token
 * @aquarush/common/theme/tokens.stylex 的 var 名一致),runtimeInjection
 * 与之相反地设为 true:vite 靠 generateBundle 抽取 CSS,jest 没有该阶段,
 * false 会把编译出的 CSS 直接丢弃 → jsdom 里 getComputedStyle 全空,
 * 样式断言无法成立。
 */
const path = require('path')

const REPO_ROOT = path.resolve(__dirname, '../..')

module.exports = {
  testEnvironment: 'jsdom',
  // jsdom 无 TextEncoder/TextDecoder(react-router 等依赖),启动前 polyfill
  setupFiles: ['<rootDir>/jest.setup.cjs'],
  testMatch: ['<rootDir>/src/**/*.test.ts', '<rootDir>/src/**/*.test.tsx'],
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
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/**/*.test.ts', '!src/**/*.test.tsx'],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
}
