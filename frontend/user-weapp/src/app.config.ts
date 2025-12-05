export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/product-detail/index',
    'pages/order-confirm/index',
    'pages/address-list/index',
    'pages/address-edit/index',
    'pages/order-list/index',
    'pages/order-detail/index',
    'pages/my/index',
    'pages/profile-edit/index',
    'pages/theme-settings/index',
    'pages/about/index',
    'pages/feedback/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'AquaRush',
    navigationBarTextStyle: 'black'
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#1890ff',
    backgroundColor: '#ffffff',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页',
        iconPath: './assets/home.png',
        selectedIconPath: './assets/home-active.png'
      },
      {
        pagePath: 'pages/order-list/index',
        text: '订单',
        iconPath: './assets/order.png',
        selectedIconPath: './assets/order-active.png'
      },
      {
        pagePath: 'pages/my/index',
        text: '我的',
        iconPath: './assets/my.png',
        selectedIconPath: './assets/my-active.png'
      }
    ]
  }
})
