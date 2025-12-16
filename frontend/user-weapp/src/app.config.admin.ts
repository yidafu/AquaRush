export default defineAppConfig({
  pages: [
    'pages/product-detail/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'AquaRush - 商品详情',
    navigationBarTextStyle: 'black'
  }
  // Note: No tabBar for admin preview since we only need the product detail page
});