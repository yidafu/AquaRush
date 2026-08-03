interface MockProduct {
  id: string;
  name: string;
  description: string;
  price: number;
  coverImageUrl: string;
  stock: number;
  status: 'ONLINE' | 'OFFLINE';
  createdAt: string;
  updatedAt: string;
  category?: string;
  brand?: string;
  specifications?: string[];
}

export const mockProducts: MockProduct[] = [
  {
    id: '1',
    name: '农夫山泉 天然矿泉水 550ml',
    description: '农夫山泉天然矿泉水，源自深层地下水源，经过天然过滤，富含多种矿物质和微量元素。水质清冽甘甜，是您日常补水的理想选择。\n\n产品特点：\n• 天然矿物质，有益健康\n• pH值7.3±0.5，天然弱碱性\n• 源自千岛湖深层水源\n• 经过严格质量检测\n• 环保包装，绿色健康',
    price: 200, // 2.00元，存储为分
    coverImageUrl: 'https://images.unsplash.com/photo-1548839140-29a74921eb34?w=800&h=600&fit=crop',
    stock: 150,
    status: 'ONLINE',
    createdAt: '2024-01-15T10:30:00Z',
    updatedAt: '2024-01-20T15:45:00Z',
    category: '矿泉水',
    brand: '农夫山泉',
    specifications: ['550ml', '24瓶装', '36瓶装']
  },
  {
    id: '2',
    name: '怡宝 纯净水 380ml',
    description: '怡宝纯净水采用多重过滤工艺，去除水中杂质和有害物质，保留对人体有益的矿物质元素。水质纯净甘甜，适合日常饮用。\n\n产品特点：\n• 多重过滤工艺\n• 去除水中杂质\n• 保留有益矿物质\n• 安全放心',
    price: 150, // 1.50元
    coverImageUrl: 'https://images.unsplash.com/photo-1596484989008-0a4938d2b5c7?w=800&h=600&fit=crop',
    stock: 89,
    status: 'ONLINE',
    createdAt: '2024-01-16T09:20:00Z',
    updatedAt: '2024-01-22T11:30:00Z',
    category: '纯净水',
    brand: '怡宝',
    specifications: ['380ml', '12瓶装', '24瓶装']
  },
  {
    id: '3',
    name: '娃哈哈 AD钙奶 220ml',
    description: '娃哈哈AD钙奶，添加维生素A、维生素D和钙质，帮助促进儿童生长发育。口感香浓，营养丰富。\n\n产品特点：\n• 添加维生素A和D\n• 富含钙质\n• 促进骨骼发育\n• 口感香浓',
    price: 300, // 3.00元
    coverImageUrl: 'https://images.unsplash.com/photo-1549496903-b551a9c4d80f?w=800&h=600&fit=crop',
    stock: 5,
    status: 'ONLINE',
    createdAt: '2024-01-17T14:15:00Z',
    updatedAt: '2024-01-18T08:45:00Z',
    category: '乳饮料',
    brand: '娃哈哈',
    specifications: ['220ml', '6盒装', '12盒装']
  },
  {
    id: '4',
    name: '康师傅 冰红茶 500ml',
    description: '康师傅冰红茶，精选优质红茶为原料，口感清爽，解渴提神。夏日必备饮品。\n\n产品特点：\n• 精选红茶原料\n• 口感清爽\n• 解渴提神\n• 独特配方',
    price: 350, // 3.50元
    coverImageUrl: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=600&fit=crop',
    stock: 0,
    status: 'OFFLINE',
    createdAt: '2024-01-18T16:45:00Z',
    updatedAt: '2024-01-25T10:20:00Z',
    category: '茶饮料',
    brand: '康师傅',
    specifications: ['500ml', '15瓶装']
  },
  {
    id: '5',
    name: '可口可乐 330ml',
    description: '可口可乐，全球知名碳酸饮料，经典口感，清爽解渴。聚会必备饮品。\n\n产品特点：\n• 经典配方\n• 独特口感\n• 清爽解渴\n• 全球知名',
    price: 350, // 3.50元
    coverImageUrl: 'https://images.unsplash.com/photo-1580957419298-3176325b1324?w=800&h=600&fit=crop',
    stock: 200,
    status: 'ONLINE',
    createdAt: '2024-01-19T11:30:00Z',
    updatedAt: '2024-01-19T11:30:00Z',
    category: '碳酸饮料',
    brand: '可口可乐',
    specifications: ['330ml', '6罐装', '24罐装']
  },
  {
    id: '6',
    name: '百事可乐 330ml',
    description: '百事可乐，与可口可乐不同口感的经典碳酸饮料，深受年轻人喜爱。\n\n产品特点：\n• 独特配方\n• 年轻时尚\n• 口感独特\n• 品质保证',
    price: 350, // 3.50元
    coverImageUrl: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&h=600&fit=crop',
    stock: 120,
    status: 'ONLINE',
    createdAt: '2024-01-20T13:20:00Z',
    updatedAt: '2024-01-20T13:20:00Z',
    category: '碳酸饮料',
    brand: '百事可乐',
    specifications: ['330ml', '6罐装', '24罐装']
  },
  {
    id: '7',
    name: '维他柠檬茶 500ml',
    description: '维他柠檬茶，采用天然柠檬提取物，口感清香，富含维生素C，健康美味。\n\n产品特点：\n• 天然柠檬提取物\n• 富含维生素C\n• 口感清香\n• 健康美味',
    price: 400, // 4.00元
    coverImageUrl: 'https://images.unsplash.com/photo-1551024506-0bccd87821d6?w=800&h=600&fit=crop',
    stock: 45,
    status: 'ONLINE',
    createdAt: '2024-01-21T10:45:00Z',
    updatedAt: '2024-01-21T10:45:00Z',
    category: '茶饮料',
    brand: '维他',
    specifications: ['500ml', '15瓶装']
  },
  {
    id: '8',
    name: '脉动 维生素饮料 600ml',
    description: '脉动维生素饮料，添加多种维生素，补充能量，提神醒脑。运动健身必备。\n\n产品特点：\n• 添加多种维生素\n• 补充能量\n• 提神醒脑\n• 运动必备',
    price: 500, // 5.00元
    coverImageUrl: 'https://images.unsplash.com/photo-1600276192503-506460544f2a?w=800&h=600&fit=crop',
    stock: 78,
    status: 'ONLINE',
    createdAt: '2024-01-22T09:30:00Z',
    updatedAt: '2024-01-22T09:30:00Z',
    category: '功能饮料',
    brand: '脉动',
    specifications: ['600ml', '12瓶装']
  },
  {
    id: '9',
    name: '红牛 功能饮料 250ml',
    description: '红牛功能饮料，提神抗疲劳，增强体力。工作和学习的好帮手。\n\n产品特点：\n• 提神抗疲劳\n• 增强体力\n• 工作学习好帮手\n• 经典配方',
    price: 600, // 6.00元
    coverImageUrl: 'https://images.unsplash.com/photo-1613478755286-6f7e41e6a5ba?w=800&h=600&fit=crop',
    stock: 25,
    status: 'ONLINE',
    createdAt: '2024-01-23T08:15:00Z',
    updatedAt: '2024-01-23T08:15:00Z',
    category: '功能饮料',
    brand: '红牛',
    specifications: ['250ml', '24罐装']
  },
  {
    id: '10',
    name: '东方树叶 绿茶 500ml',
    description: '东方树叶绿茶，采用优质绿茶茶叶，口感清爽回甘，富含茶多酚，健康养生。\n\n产品特点：\n• 优质绿茶茶叶\n• 口感清爽回甘\n• 富含茶多酚\n• 健康养生',
    price: 300, // 3.00元
    coverImageUrl: 'https://images.unsplash.com/photo-1576091160550-2173dba999ee?w=800&h=600&fit=crop',
    stock: 156,
    status: 'ONLINE',
    createdAt: '2024-01-24T12:00:00Z',
    updatedAt: '2024-01-24T12:00:00Z',
    category: '茶饮料',
    brand: '东方树叶',
    specifications: ['500ml', '15瓶装']
  }
];

// 模拟的 GraphQL 响应延迟
export const MOCK_DELAY = 500; // 500ms

// 模拟数据操作函数
export const mockProductResolvers = {
  Query: {
    products: () => {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({ products: mockProducts });
        }, MOCK_DELAY);
      });
    },
    product: (_: any, { id }: { id: string }) => {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          const product = mockProducts.find(p => p.id === id);
          if (product) {
            resolve({ product });
          } else {
            reject(new Error(`Product with id ${id} not found`));
          }
        }, MOCK_DELAY);
      });
    }
  },
  Mutation: {
    createProduct: (_: any, { input }: { input: any }) => {
      return new Promise((resolve) => {
        setTimeout(() => {
          const newProduct: MockProduct = {
            id: String(mockProducts.length + 1),
            name: input.name,
            description: input.description || '',
            price: input.price,
            coverImageUrl: input.coverImageUrl || '',
            stock: input.stock,
            status: input.status || 'OFFLINE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            category: input.category,
            brand: input.brand,
            specifications: input.specifications
          };
          mockProducts.push(newProduct);
          resolve(newProduct);
        }, MOCK_DELAY);
      });
    },
    updateProduct: (_: any, { id, input }: { id: string; input: any }) => {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          const productIndex = mockProducts.findIndex(p => p.id === id);
          if (productIndex === -1) {
            reject(new Error(`Product with id ${id} not found`));
            return;
          }

          mockProducts[productIndex] = {
            ...mockProducts[productIndex],
            ...input,
            updatedAt: new Date().toISOString()
          };
          resolve(mockProducts[productIndex]);
        }, MOCK_DELAY);
      });
    },
    deleteProduct: (_: any, { id }: { id: string }) => {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          const productIndex = mockProducts.findIndex(p => p.id === id);
          if (productIndex === -1) {
            reject(new Error(`Product with id ${id} not found`));
            return;
          }

          const deletedProduct = mockProducts[productIndex];
          mockProducts.splice(productIndex, 1);
          resolve({
            id: deletedProduct.id,
            success: true
          });
        }, MOCK_DELAY);
      });
    }
  }
};