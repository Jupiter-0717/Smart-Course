// src/services/authService.ts
import axios from 'axios';

// 设置API基础URL
const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// 登录请求接口
export interface LoginRequest {
  account: string;
  password: string;
  role: string;
}

// 登录响应接口
export interface LoginResponse {
  userId: number;
  account: string;
  role: string;
  token: string;
  teacherId?: number; // 添加可选属性
  studentId?: number; // 添加可选属性
}

// src/services/authService.ts
export const login = async (data: { 
  account: string; 
  password: string; 
  role: string 
}): Promise<LoginResponse> => {
  try {
    const response = await axios.post(`${API_URL}/api/auth/login`, {
      account: data.account,
      password: data.password,
      role: data.role // 确保包含角色信息
    });
    
    return {
      userId: response.data.userId,
      account: response.data.account,
      role: data.role, // 使用前端传递的角色
      token: response.data.token || '',
      teacherId: response.data.teacherId || 0, // 添加教师ID
      studentId: response.data.studentId || 0  // 添加学生ID
    };
  } catch (error) {
    // 统一错误消息格式
    if (axios.isAxiosError(error) && error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw new Error('登录失败，请检查网络连接');
  }
};