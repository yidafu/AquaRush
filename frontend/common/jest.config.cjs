/**
 * @aquarush/common 单测配置(jest)
 * 覆盖当前纳入测试范围的纯业务逻辑(utils + tracking 核心),
 * 覆盖率基线详见 frontend/TESTING.md。
 * 注意:common 为 ESM("type": "module")包,配置文件名用 .cjs 避免 ESM 解析问题。
 */
module.exports = {
  // jsdom:纯 utils 在 node 下跑没问题,但 src/hooks 的 renderHook 测试需要 DOM。
  testEnvironment: 'jsdom',
  testMatch: ['<rootDir>/src/**/*.test.ts', '<rootDir>/src/**/*.test.tsx'],
  transform: {
    '^.+\\.tsx?$': [
      'babel-jest',
      {
        presets: [
          ['@babel/preset-env', { targets: { node: 'current' } }],
          '@babel/preset-typescript',
        ],
      },
    ],
  },
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  collectCoverageFrom: [
    'src/utils/**/*.ts',
    'src/hooks/**/*.ts',
    'src/tracking/tracker.ts',
    '!src/**/*.test.ts',
    '!src/**/*.test.tsx',
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
}
