import type { User, Order, Address } from '@aquarush/common';

/** 用户详情页单页查询(UserDetailPageData)返回结构 */
export interface UserDetailPageData {
  user?: User;
  userAddresses: Address[];
  orders: {
    content: Order[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
  completedOrders: {
    totalElements: number;
  };
}
