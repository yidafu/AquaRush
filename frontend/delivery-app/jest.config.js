/**
 * delivery-app 单测配置(jest-expo,SDK 57)
 *
 * 约定与 frontend/TESTING.md 保持一致:先覆盖非 UI 的纯业务逻辑
 * (constants / utils / services),UI 组件与页面测试后续再补。
 *
 * - preset: jest-expo(SDK 57 对应 jest 29 生态),自动读取项目 babel.config.js
 *   (babel-preset-expo + babel-plugin-aquarush-track),并按 tsconfig paths
 *   生成 `@/*` 的 moduleNameMapper;
 * - 测试文件与源码同目录放置(形如 `xxx.test.ts`),仅测逻辑不测快照;
 * - AsyncStorage 等原生依赖由 jest-expo 的 setup 或测试内 jest.mock 处理。
 */
module.exports = {
  preset: 'jest-expo',
  testMatch: ['<rootDir>/src/**/*.test.ts', '<rootDir>/src/**/*.test.tsx'],
  // @aquarush/common 的 package.json exports 仅暴露 `import` 条件(Jest CJS 解析器
  // 走 `require` 会找不到),这里直接映射到 workspace 内 TS 源码。
  moduleNameMapper: {
    '^@aquarush/common$': '<rootDir>/../common/src/index.ts',
    '^@aquarush/common/(.*)$': '<rootDir>/../common/src/$1',
  },
  collectCoverageFrom: [
    'src/constants/**/*.ts',
    'src/utils/**/*.ts',
    'src/services/**/*.ts',
    '!src/**/*.test.ts',
    '!src/**/*.test.tsx',
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
};
