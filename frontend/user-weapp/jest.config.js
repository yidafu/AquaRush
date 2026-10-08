const defineJestConfig = require('@tarojs/test-utils-react/dist/jest.js').default

// user-weapp 单测配置(基于 Taro 官方 jest preset):
// - 当前只跑 src/**/*.test.ts 的非 UI 业务逻辑单测(cart/marketing/feedback 等 utils);
// - Taro 组件/页面 UI 测试(__tests__/index.test.js 等,基于 @tarojs/test-utils-react)
//   暂未纳入:@tarojs/router 在 jsdom 下模块加载即访问 window.history 报错,
//   属 Taro UI 测试基建问题,待修复后再加入 testMatch。
// 覆盖率基线详见 frontend/TESTING.md。
module.exports = defineJestConfig({
  testEnvironment: 'jsdom',
  testMatch: ['<rootDir>/src/**/*.test.ts'],
  collectCoverageFrom: ['src/utils/**/*.ts', '!src/**/*.test.ts'],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
  moduleNameMapper: {
    // jest 的 resolver 走 require 条件,而 @aquarush/common 的 exports 只声明了
    // "import",这里显式映射到源码,让单测直接消费 TS 源码
    '^@aquarush/common$': '<rootDir>/../common/src/index.ts'
  }
})
