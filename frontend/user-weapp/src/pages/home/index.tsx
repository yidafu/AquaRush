import { Component } from 'react'
import { View, Text, Image, Navigator, Swiper, SwiperItem } from '@tarojs/components'
import { AtButton, AtCard, AtGrid, AtDivider } from 'taro-ui'
import Taro from '@tarojs/taro'
import CustomNavBar from '../../components/CustomNavBar'
import { authService } from '../../utils/auth'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/grid.scss"
import "taro-ui/dist/style/components/divider.scss"
import './index.scss'

interface Product {
  id: string
  name: string
  price: number
  image: string
  description: string
  stock: number
}

interface HomeState {
  products: Product[]
  loading: boolean
  userInfo: any
  currentSlide: number
}

export default class HomePage extends Component<{}, HomeState> {
  constructor(props) {
    super(props)
    this.state = {
      products: [],
      loading: true,
      userInfo: null,
      currentSlide: 0
    }
  }

  componentDidMount() {
    this.loadUserInfo()
    this.loadProducts()
  }

  componentDidShow() {
    // 页面显示时刷新用户信息和产品数据
    this.loadUserInfo()
    this.loadProducts()
  }

  loadUserInfo = async () => {
    try {
      // 检查用户是否已登录，如果已登录则获取用户信息
      if (authService.isAuthenticated()) {
        const userInfo = authService.getUserInfo()
        this.setState({ userInfo })
      } else {
        // 如果用户未登录，设置为null，组件会根据状态显示不同的内容
        this.setState({ userInfo: null })
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      this.setState({ userInfo: null })
    }
  }

  loadProducts = async () => {
    try {
      this.setState({ loading: true })

      // TODO: 实际项目中这里应该调用获取产品列表的API
      // const products = await getProducts()

      // 模拟产品数据，使用真实图片
      const mockProducts: Product[] = [
        {
          id: '1',
          name: '农夫山泉 矿泉水 550ml',
          price: 2.00,
          image: '/assets/home-active.png',
          description: '天然矿泉水，源自千岛湖深层水源，口感清甜甘冽',
          stock: 100
        },
        {
          id: '2',
          name: '怡宝 纯净水 380ml',
          price: 1.50,
          image: '/assets/order-active.png',
          description: '采用先进反渗透技术，滴滴纯净，安全放心',
          stock: 80
        },
        {
          id: '3',
          name: '娃哈哈 AD钙奶 220ml',
          price: 3.50,
          image: '/assets/my-active.png',
          description: '富含维生素A、D和钙质，营养美味',
          stock: 60
        },
        {
          id: '4',
          name: '康师傅 冰红茶 500ml',
          price: 4.00,
          image: '/assets/home.png',
          description: '精选红茶，冰爽解渴，夏日必备',
          stock: 45
        },
        {
          id: '5',
          name: '统一 阿萨姆奶茶 500ml',
          price: 5.00,
          image: '/assets/order.png',
          description: '浓郁奶茶口感，丝滑香醇，回味无穷',
          stock: 30
        },
        {
          id: '6',
          name: '百事可乐 330ml',
          price: 3.00,
          image: '/assets/my.png',
          description: '经典可乐，气泡十足，畅爽无比',
          stock: 70
        }
      ]

      this.setState({ products: mockProducts })
    } catch (error) {
      console.error('获取产品列表失败:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setState({ loading: false })
    }
  }

  // 轮播图数据
  getCarouselData = () => {
    return [
      {
        id: 1,
        title: '新鲜好水',
        subtitle: '送货上门 • 即时配送',
        image: '/assets/home-active.png',
        buttonText: '快速下单'
      },
      {
        id: 2,
        title: '品质保证',
        subtitle: '正品行货 • 假一赔十',
        image: '/assets/order-active.png',
        buttonText: '立即选购'
      },
      {
        id: 3,
        title: '优惠活动',
        subtitle: '新用户专享 • 满减优惠',
        image: '/assets/my-active.png',
        buttonText: '查看详情'
      }
    ]
  }

  // 轮播图切换
  handleSwiperChange = (e) => {
    this.setState({
      currentSlide: e.detail.current
    })
  }

  handleProductClick = (product: Product) => {
    Taro.navigateTo({
      url: `/pages/product-detail/index?id=${product.id}`
    })
  }

  handleQuickOrder = () => {
    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }

  handleMenuClick = (item: any) => {
    const { url } = item
    if (url) {
      Taro.navigateTo({ url })
    }
  }

  handleSearch = (query: string) => {
    console.log('Search query:', query)
    // TODO: Implement search functionality
    // You can filter products based on the query
    if (query.trim()) {
      // Filter products logic here
    }
  }

  handleProfileClick = () => {
    Taro.navigateTo({
      url: '/pages/my/index'
    })
  }

  handleCategoryToggle = () => {
    Taro.showActionSheet({
      itemList: ['全部商品', '矿泉水', '纯净水', '饮料'],
      success: (res) => {
        console.log('Selected category:', res.tapIndex)
        // TODO: Implement category filtering
      }
    })
  }

  render() {
    const { products, loading, userInfo, currentSlide } = this.state
    const carouselData = this.getCarouselData()



    return (
      <View className='home-page'>
        {/* 顶部轮播图 */}
        <View className='carousel-section'>
          <Swiper
            className='carousel-swiper'
            indicatorColor='rgba(255, 255, 255, 0.4)'
            indicatorActiveColor='white'
            indicatorDots
            circular
            autoplay
            interval={3000}
            duration={500}
            onChange={this.handleSwiperChange}
          >
            {carouselData.map((item) => (
              <SwiperItem key={item.id} className='carousel-swiper-item'>
                <View className='carousel-slide'>
                  <View className='carousel-background'>
                    <Image
                      src={item.image}
                      className='carousel-bg-image'
                      mode='aspectFill'
                    />
                    <View className='carousel-overlay' />
                  </View>
                  <View className='carousel-content'>
                    <Text className='carousel-title'>{item.title}</Text>
                    <Text className='carousel-subtitle'>{item.subtitle}</Text>
                    <AtButton
                      type='primary'
                      size='small'
                      onClick={this.handleQuickOrder}
                      className='carousel-button'
                    >
                      {item.buttonText}
                    </AtButton>
                  </View>
                </View>
              </SwiperItem>
            ))}
          </Swiper>
        </View>

        <AtDivider content='推荐产品' />

        {/* 产品网格 */}
        <View className='product-section'>
          {loading ? (
            <View className='loading-container'>
              <Text>加载中...</Text>
            </View>
          ) : products.length === 0 ? (
            <View className='empty-container'>
              <Text>暂无产品</Text>
            </View>
          ) : (
            <View className='product-grid'>
              {products.map((product) => (
                <View
                  key={product.id}
                  className='product-card'
                  onClick={() => this.handleProductClick(product)}
                >
                  <View className='product-image-container'>
                    <Image
                      src={product.image}
                      mode='aspectFill'
                      className='product-image'
                    />
                  </View>
                  <View className='product-content'>
                    <Text className='product-name'>{product.name}</Text>
                    <Text className='product-desc' numberOfLines={2}>{product.description}</Text>
                    <View className='product-bottom'>
                      <Text className='product-price'>¥{product.price.toFixed(2)}</Text>
                      <Text className='product-stock'>库存{product.stock}</Text>
                    </View>
                  </View>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 底部安全区域 */}
        <View className='safe-bottom' />
      </View>
    )
  }
}
