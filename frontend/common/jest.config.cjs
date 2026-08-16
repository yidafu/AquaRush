/**
 * @aquarush/common 单测配置(jest)
 * 覆盖当前纳入测试范围的纯业务逻辑(utils + tracking 核心),
 * 覆盖率基线详见 frontend/TESTING.md。
 * 注意:common 为 ESM("type": "module")包,配置文件名用 .cjs 避免 ESM 解析问题。
 */
module.exports = {
  testEnvironment: 'node',
  testMatch: ['<rootDir>/src/**/*.test.ts'],
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
  collectCoverageFrom: [
    'src/utils/**/*.ts',
    'src/tracking/tracker.ts',
    '!src/**/*.test.ts',
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'json-summary', 'html'],
}
