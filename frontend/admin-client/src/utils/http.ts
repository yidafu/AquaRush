import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { message } from 'antd';
export interface ApiResponse<T> {
  success: boolean,
  data?: T,
  message: string,
  code: string,
}
/**
 * HTTP 请求封装类
 * 提供 get/post/put/delete 方法，直接返回响应数据
 */
class Http {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: '/api',
      timeout: 10000,
    });

    // 请求拦截器
    this.instance.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // 响应拦截器 - 直接返回 data，不做额外包装
    this.instance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse<any>>) => {
        const data  = response.data
        if (data.success) {
          return data.data;
        }
        throw new Error(data.message)
      },
      (error) => {
        message.error(error.message || '网络错误');
        return Promise.reject(error);
      }
    );
  }

  /**
   * GET 请求
   */
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.get(url, config);
  }

  /**
   * POST 请求
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.post(url, data, config);
  }

  /**
   * PUT 请求
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.put(url, data, config);
  }

  /**
   * DELETE 请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.delete(url, config);
  }
}

export default new Http();
