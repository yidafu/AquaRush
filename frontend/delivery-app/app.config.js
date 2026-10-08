/**
 * 动态应用配置（替代 app.json）。
 *
 * 深链支持：设置环境变量 EXPO_PUBLIC_DEEP_LINK_DOMAIN（如 aquarush.example.com）后自动启用：
 * - iOS Universal Links：ios.associatedDomains
 * - Android App Links：android.intentFilters（VIEW + autoVerify）
 * - expo-router origin：Linking.createURL('/order/xxx') 产出 https://<domain>/order/xxx
 *
 * 未设置时保持纯 scheme（deliveryapp://）深链，可正常开发调试。
 * 域名就绪后还需在服务器部署验证文件：
 * - https://<domain>/.well-known/apple-app-site-association（iOS）
 * - https://<domain>/.well-known/assetlinks.json（Android，package 为应用包名）
 */
const domain = process.env.EXPO_PUBLIC_DEEP_LINK_DOMAIN;

const config = {
  expo: {
    name: 'AquaPush',
    slug: 'delivery-app',
    version: '1.0.0',
    orientation: 'portrait',
    icon: './assets/images/icon.png',
    scheme: 'deliveryapp',
    userInterfaceStyle: 'automatic',
    ios: {
      icon: './assets/expo.icon',
      bundleIdentifier: 'com.aquarush.delivery',
      // 轨迹上报(F-ADR-012):react-native-fitness-geolocation 原生定位
      // + 配送地图用户定位(MapLibre UserLocation)
      infoPlist: {
        NSLocationWhenInUseUsageDescription: '需要获取您的位置以展示配送地图并上报配送轨迹',
        NSLocationAlwaysAndWhenInUseUsageDescription: '配送中需要持续获取您的位置,以便后台持续上报配送轨迹',
        UIBackgroundModes: ['location'],
      },
    },
    android: {
      package: 'com.aquarush.delivery',
      adaptiveIcon: {
        backgroundColor: '#FBFBFB',
        foregroundImage: './assets/images/android-icon-foreground.png',
        backgroundImage: './assets/images/android-icon-background.png',
        monochromeImage: './assets/images/android-icon-monochrome.png',
      },
      predictiveBackGestureEnabled: false,
      // 轨迹上报(F-ADR-012):react-native-fitness-geolocation 所需定位/前台服务权限
      // (SDK 自带 manifest 亦声明,应用层显式声明保证权限清单完整)
      permissions: [
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.ACCESS_COARSE_LOCATION',
        'android.permission.ACCESS_BACKGROUND_LOCATION',
        'android.permission.FOREGROUND_SERVICE',
        'android.permission.FOREGROUND_SERVICE_LOCATION',
      ],
    },
    web: {
      output: 'static',
      favicon: './assets/images/favicon.png',
    },
    plugins: [
      'expo-router',
      // 配送地图(F-ADR-012 扩展):@maplibre/maplibre-react-native 需 dev build,
      // 定位权限已在 android.permissions / ios.infoPlist 中声明,无需重复配置
      '@maplibre/maplibre-react-native',
      // 系统通知栏(F-NTF-003):WS 实时推送 → expo-notifications 本地通知;
      // Android 通知权限/默认图标/通知通道由插件自动注册
      'expo-notifications',
      [
        'expo-splash-screen',
        {
          backgroundColor: '#ffffff',
          image: './assets/images/icon.png',
          imageWidth: 200,
          resizeMode: 'contain',
        },
      ],
      [
        'expo-image-picker',
        {
          photosPermission: '需要访问相册以选择配送照片',
          cameraPermission: '需要访问相机以拍摄配送照片',
        },
      ],
    ],
    experiments: {
      typedRoutes: true,
      reactCompiler: true,
    },
  },
};

if (domain) {
  config.expo.ios = {
    ...config.expo.ios,
    associatedDomains: [`applinks:${domain}`],
  };
  config.expo.android = {
    ...config.expo.android,
    intentFilters: [
      {
        action: 'VIEW',
        autoVerify: true,
        data: [{ scheme: 'https', host: domain, pathPrefix: '/order' }],
        category: ['BROWSABLE', 'DEFAULT'],
      },
    ],
  };
  config.expo.plugins = config.expo.plugins.map((plugin) =>
    plugin === 'expo-router'
      ? ['expo-router', { origin: `https://${domain}` }]
      : plugin,
  );
}

module.exports = config;
