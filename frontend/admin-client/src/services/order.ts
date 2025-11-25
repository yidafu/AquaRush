import request from '../utils/request';

export interface Order {
  id: string;
  orderNumber: string;
  userName: string;
  productName: string;
  amount: number;
  status: string;
  createdAt: string;
}

export const getOrders = (params?: any) => {
  return request.get<Order[]>('/orders', { params });
};

export const getOrderById = (id: string) => {
  return request.get<Order>(`/orders/${id}`);
};

export const assignDelivery = (orderId: string, workerId: string) => {
  return request.post(`/orders/${orderId}/assign`, { workerId });
};
