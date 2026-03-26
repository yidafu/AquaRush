
export default defineAppConfig({
  pages: [
    'pages/login/index',
    'pages/bind-phone/index',
    'pages/order-list/index',
    'pages/order-detail/index',
    'pages/create-order/index',
    'pages/statistics/index',
    'pages/history-orders/index',
    'pages/my/index',
    'pages/about/index'
  ],
  window: {
    navigationBarTitleText: '配送管理',
    navigationBarBackgroundColor: '#1890ff',
    navigationBarTextStyle: 'white'
  },
  tabBar: {
    color: '#999',
    selectedColor: '#1890ff',
    backgroundColor: '#fff',
    list: [
      {
        pagePath: 'pages/order-list/index',
        text: '订单',
        iconPath: './assets/task.png',
        selectedIconPath: './assets/task-active.png'
      },
      {
        pagePath: 'pages/statistics/index',
        text: '统计',
        iconPath: './assets/statistics.png',
        selectedIconPath: './assets/statistics-active.png'
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
