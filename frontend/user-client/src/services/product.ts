import request from '../utils/request';

export interface Product {
  id: string;
  name: string;
  price: number;
  coverImageUrl: string;
  description?: string;
  stock: number;
  status: string;
}

// 获取所有产品
export const getProducts = () => {
  return request.get<Product[]>('/products/active');
};

// 获取产品详情
export const getProductById = (id: string) => {
  return request.get<Product>(`/products/${id}`);
};
