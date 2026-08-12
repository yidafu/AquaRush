/**
 * 排除 pnpm workspace 中 Taro（user-weapp）peerDependencies 拉进来的 RN 原生模块。
 * 这些模块不是本应用的依赖，会被 RN autolinking 自动 link 进原生构建，
 * 且部分模块的 build.gradle 仍使用 Gradle 9 已移除的 API（如 jcenter()），导致构建失败。
 *
 * 对应 expo 原生模块的排除见 package.json 的 expo.autolinking.exclude。
 */
module.exports = {
  dependencies: {
    '@bam.tech/react-native-image-resizer': { platforms: { android: null, ios: null } },
    '@react-native-community/cameraroll': { platforms: { android: null, ios: null } },
    '@react-native-community/geolocation': { platforms: { android: null, ios: null } },
    '@react-native-community/netinfo': { platforms: { android: null, ios: null } },
    '@react-native-community/slider': { platforms: { android: null, ios: null } },
    'react-native-device-info': { platforms: { android: null, ios: null } },
    'react-native-pager-view': { platforms: { android: null, ios: null } },
    'react-native-webview': { platforms: { android: null, ios: null } },
  },
};
