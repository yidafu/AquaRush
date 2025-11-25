import request from '../utils/request';

export interface User {
  id: string;
  username: string;
  phone: string;
  nickname?: string;
  role: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserDto {
  username: string;
  password: string;
  phone: string;
  nickname?: string;
  role: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
  status?: 'ACTIVE' | 'INACTIVE' | 'BANNED';
}

export interface UpdateUserDto {
  phone?: string;
  nickname?: string;
  role?: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
  status?: 'ACTIVE' | 'INACTIVE' | 'BANNED';
}

export interface UserListQuery {
  page?: number;
  pageSize?: number;
  role?: string;
  status?: string;
  keyword?: string;
}

/**
 * 获取用户列表
 */
export const getUserList = (params?: UserListQuery) => {
  return request.get<{
    list: User[];
    total: number;
  }>('/users', { params });
};

/**
 * 获取用户详情
 */
export const getUserById = (id: string) => {
  return request.get<User>(`/users/${id}`);
};

/**
 * 创建用户
 */
export const createUser = (data: CreateUserDto) => {
  return request.post<User>('/users', data);
};

/**
 * 更新用户
 */
export const updateUser = (id: string, data: UpdateUserDto) => {
  return request.put<User>(`/users/${id}`, data);
};

/**
 * 删除用户
 */
export const deleteUser = (id: string) => {
  return request.delete(`/users/${id}`);
};

/**
 * 重置用户密码
 */
export const resetUserPassword = (id: string) => {
  return request.post<{ newPassword: string }>(`/users/${id}/reset-password`);
};

/**
 * 批量删除用户
 */
export const batchDeleteUsers = (ids: string[]) => {
  return request.post('/users/batch-delete', { ids });
};

/**
 * 启用/禁用用户
 */
export const toggleUserStatus = (id: string, status: 'ACTIVE' | 'BANNED') => {
  return request.patch(`/users/${id}/status`, { status });
};
