import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login as apiLogin } from '@/services/authService';

// 用户接口定义
interface User {
  id: number;
  account: string;
  role: string;
  name: string;
  teacherId?: number; // 添加可选属性
  studentId?: number; // 添加可选属性

}

export const useAuthStore = defineStore('auth', () => {
  // 当前用户信息
  const user = ref<User | null>(null);
  
  // JWT 认证令牌
  const token = ref<string | null>(localStorage.getItem('token'));
  
  // 用户是否已认证
  const isAuthenticated = computed(() => !!token.value);
  
  // 用户角色 (student 或 teacher)
  const userRole = computed(() => user.value?.role || '');
  
  // 登录方法
  const login = async (loginData: { 
    account: string; 
    password: string; 
    role: string 
  }) => {
    try {
      const response = await apiLogin(loginData);
      
      user.value = {
        id: response.userId,
        account: response.account,
        role: loginData.role, // 使用前端传递的角色
        name: loginData.role === 'teacher' ? '教师用户' : '学生用户',
        teacherId: response.teacherId, // 存储教师ID
        studentId: response.studentId  // 存储学生ID
      };
      
      token.value = response.token || '';
      
      // 本地存储
      localStorage.setItem('token', token.value);
      localStorage.setItem('user', JSON.stringify(user.value));
      
      return user.value;
    } catch (error) {
      throw error;
    }
  };
  
  // 登出方法
  const logout = () => {
    // 清除用户信息
    user.value = null;
    token.value = null;
    
    // 清除本地存储
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    
    console.log('用户已登出');
  };
  
  // 初始化方法 - 从本地存储加载用户信息
  const initializeAuth = () => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
  const parsedUser = JSON.parse(storedUser);
      
      // 确保 teacherId 存在
      if (parsedUser.role === 'teacher' && !parsedUser.teacherId) {
        // 尝试从用户名中提取教师ID
        const username = parsedUser.account;
        if (username.startsWith('teacher_')) {
          const parts = username.split('_');
          if (parts.length >= 2) {
            parsedUser.teacherId = parseInt(parts[1]);
          }
        }
      }

        user.value = JSON.parse(storedUser);
        console.log('用户信息已从本地存储加载');
      } catch (e) {
        console.error('加载用户信息失败', e);
        localStorage.removeItem('user');
      }
    }
  };
  
  // 检查用户角色权限
  const hasPermission = (requiredRole: string) => {
    return userRole.value === requiredRole;
  };
  const teacherId = computed(() => user.value?.teacherId || 0);
  // 返回所有状态和方法
  return {
    user,
    teacherId,
    token,
    isAuthenticated,
    userRole,
    login,
    logout,
    initializeAuth,
    hasPermission
  };
});