// src/stores/api.ts
import { defineStore } from 'pinia';
import axios from 'axios';
import { useAuthStore } from './auth';

export const useApiStore = defineStore('api', () => {
  const authStore = useAuthStore();
  
  // 创建带有认证的axios实例
  const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  });
  
  // 请求拦截器 - 添加token
  api.interceptors.request.use(config => {
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }
    return config;
  });
  
  // API方法
  const get = async (url: string) => {
    return await api.get(url);
  };
  
  const post = async (url: string, data: any) => {
    return await api.post(url, data);
  };
  
  const put = async (url: string, data: any) => {
    return await api.put(url, data);
  };
  
  const del = async (url: string) => {
    return await api.delete(url);
  };
  
  return {
    get,
    post,
    put,
    delete: del
  };
});