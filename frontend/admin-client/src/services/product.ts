import request from '../utils/request';

export interface Product {
  id: string;
  name: string;
  price: number;
  stock: number;
  status: string;
}

export const getProducts = () => {
  return request.get<Product[]>('/products');
};

export const createProduct = (data: Partial<Product>) => {
  return request.post<Product>('/products', data);
};

export const updateProduct = (id: string, data: Partial<Product>) => {
  return request.put<Product>(`/products/${id}`, data);
};

export const deleteProduct = (id: string) => {
  return request.delete(`/products/${id}`);
};
