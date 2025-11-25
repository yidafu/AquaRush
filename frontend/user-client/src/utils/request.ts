import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // TODO: 添加 token
    const token = wx.getStorageSync('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data.success) {
      return data.data;
    }
    return Promise.reject(new Error(data.message || '请求失败'));
  },
  (error) => {
    wx.showToast({
      title: error.message || '网络错误',
      icon: 'none'
    });
    return Promise.reject(error);
  }
);

export default request;
