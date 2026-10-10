import type { UserVo, OrderVo, AddressVo } from '@aquarush/common';

/** 用户详情页单页查询(UserDetailPageData)返回结构 */
export interface UserDetailPageData {
  user?: UserVo;
  userAddresses: AddressVo[];
  orders: {
    content: OrderVo[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
  completedOrders: {
    totalElements: number;
  };
}
