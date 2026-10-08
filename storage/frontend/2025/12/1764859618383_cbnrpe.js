const {mergeConfig, getDefaultConfig} = require('@react-native/metro-config');
const {createHarmonyMetroConfig} = require('@react-native-oh/react-native-harmony/metro.config');

/**
* @type {import("metro-config").ConfigT}
*/
const config = {
  resolver: {
    platforms: ['ios', 'android', 'harmony'], // 添加harmony平台
    blockList: [
      // 排除 harmony 相关目录，避免 Metro 扫描导致的重复模块问题
      /harmony\/oh_modules\/.*/,
      /harmony\/build\/.*/,
      /harmony\/.*\/build\/.*/,
      /harmony\/.*\/oh_modules\/.*/,
      /harmony\/.*\/\.cxx\/.*/,
    ],
  },
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: true,
      },
    }),
  },
  watchFolders: [__dirname],
};

module.exports = mergeConfig(getDefaultConfig(__dirname), createHarmonyMetroConfig({
  reactNativeHarmonyPackageName: '@react-native-oh/react-native-harmony',
}), config);
