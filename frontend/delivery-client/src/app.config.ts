export default {
  pages: [
    'pages/task-list/index',
    'pages/task-detail/index',
    'pages/delivery-confirm/index',
    'pages/history/index',
    'pages/my/index'
  ],
  window: {
    navigationBarTitleText: '配送管理',
    navigationBarBackgroundColor: '#52c41a',
    navigationBarTextStyle: 'white'
  },
  tabBar: {
    color: '#999',
    selectedColor: '#52c41a',
    backgroundColor: '#fff',
    list: [
      {
        pagePath: 'pages/task-list/index',
        text: '任务',
        iconPath: './assets/task.png',
        selectedIconPath: './assets/task-active.png'
      },
      {
        pagePath: 'pages/history/index',
        text: '历史',
        iconPath: './assets/history.png',
        selectedIconPath: './assets/history-active.png'
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
