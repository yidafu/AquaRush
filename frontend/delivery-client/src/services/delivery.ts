import request from '../utils/request';

// 获取配送任务列表
export const getDeliveryTasks = (workerId: string) => {
  return request.get(`/orders/delivery-worker/${workerId}`);
};

// 更新配送员状态
export const updateWorkerStatus = (workerId: string, status: 'ONLINE' | 'OFFLINE') => {
  return request.post(`/delivery/workers/${workerId}/status`, { status });
};

// 确认送达
export const confirmDelivery = (orderId: string, photos: string[]) => {
  return request.post(`/delivery/confirm/${orderId}`, { photos });
};
