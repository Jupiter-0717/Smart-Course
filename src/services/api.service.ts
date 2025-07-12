import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器：添加认证token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理错误
api.interceptors.response.use(
  response => response,
  error => {
    const { response } = error;
    if (response) {
      // 统一错误处理
      return Promise.reject({
        status: response.status,
        message: response.data?.message || '请求失败'
      });
    }
    return Promise.reject(error);
  }
);

export default api;