import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
});

request.interceptors.request.use(
  (config) => {
    const token = wx.getStorageSync('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

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
