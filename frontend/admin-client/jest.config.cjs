/**
 * admin-client 单测配置(jest)
 * 当前只测非 UI 的业务工具(utils),UI 组件(components/pages)暂不纳入,
 * 覆盖率基线详见 frontend/TESTING.md。
 * 注意:admin-client 为 ESM("type": "module")包,配置文件名用 .cjs。
 */
module.exports = {
  testEnvironment: 'node',
  testMatch: ['<rootDir>/src/**/*.test.ts'],
  // `@/` 别名与 vite 对齐(http.ts 等已从 `@/ui` 导入 message);
  // 测试用 jest.mock('@/ui', factory) 拦截,无需 tsx transform。
  moduleNameMapper: { '^@/(.*)$': '<rootDir>/src/$1' },
  transform: {
    '^.+\\.ts$': [
      'babel-jest',
      {
        presets: [
          ['@babel/preset-env', { targets: { node: 'current' } }],
          '@babel/preset-typescript',
        ],
      },
    ],
  },
  moduleFileExtensions: ['ts', 'js', 'json'],
  collectCoverageFrom: ['src/utils/**/*.ts', '!src/**/*.test.ts'],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
}
