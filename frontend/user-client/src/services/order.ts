import request from '../utils/request';

export interface Order {
  id: string;
  orderNumber: string;
  productId: string;
  quantity: number;
  amount: number;
  status: string;
  createdAt: string;
}

// 创建订单
export const createOrder = (data: any) => {
  return request.post<Order>('/orders', data);
};

// 获取用户订单
export const getUserOrders = (userId: string) => {
  return request.get<Order[]>(`/orders/user/${userId}`);
};

// 取消订单
export const cancelOrder = (orderId: string) => {
  return request.post(`/orders/${orderId}/cancel`);
};
