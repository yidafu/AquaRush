import http from '../utils/http';

export interface LoginDto {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tokenType?: string;
  userInfo: {
    id: string;
    username: string;
    nickname?: string;
    role: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
    avatar?: string;
  };
}

export interface UserInfo {
  id: string;
  username: string;
  nickname?: string;
  phone?: string;
  email?: string;
  role: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
  avatar?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  createdAt: string;
}

/**
 * 登录
 */
export const login = (data: LoginDto) => {
  return http.post<LoginResponse>('/auth/login', data);
};

/**
 * 登出
 */
export const logout = () => {
  return http.post('/auth/logout');
};

/**
 * 获取当前用户信息
 */
export const getCurrentUser = () => {
  return http.get<UserInfo>('/auth/current-user');
};

/**
 * 刷新 Token
 */
export const refreshToken = (refreshToken: string) => {
  return http.post<{ token: string }>('/auth/refresh-token', { refreshToken });
};

/**
 * 修改密码
 */
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return http.post('/auth/change-password', data);
};

/**
 * 忘记密码（发送验证码）
 */
export const forgotPassword = (phone: string) => {
  return http.post('/auth/forgot-password', { phone });
};

/**
 * 重置密码
 */
export const resetPassword = (data: { phone: string; code: string; newPassword: string }) => {
  return http.post('/auth/reset-password', data);
};
