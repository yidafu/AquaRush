export default {
  pages: [
    'pages/home/index',
    'pages/product-detail/index',
    'pages/order-confirm/index',
    'pages/my/index',
    'pages/address-list/index',
    'pages/address-edit/index',
    'pages/order-list/index',
    'pages/order-detail/index'
  ],
  window: {
    navigationBarTitleText: '桶装水订水',
    navigationBarBackgroundColor: '#1890ff',
    navigationBarTextStyle: 'white'
  },
  tabBar: {
    color: '#999',
    selectedColor: '#1890ff',
    backgroundColor: '#fff',
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
};
