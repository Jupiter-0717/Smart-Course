<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo-section">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
          <path d="M12 3L1 9l4 2.18v6L12 21l7-3.82v-6l2-1.09V17h2V9L12 3zm6.82 6L12 12.72 5.18 9 12 5.28 18.82 9zM17 15.99l-5 2.73-5-2.73v-3.72L12 15l5-2.73v3.72z" fill="#2563eb"/>
        </svg>
        <div>
          <h1>智慧学习平台</h1>
          <p>专业教学管理系统</p>
        </div>
      </div>

      <!-- 错误提示框 -->
      <div v-if="errorMessage" class="error-message">
        <i class="fas fa-exclamation-circle"></i>
        {{ errorMessage }}
      </div>

      <div class="role-tabs">
        <div 
          :class="['role-tab', { active: form.role === 'teacher' }]"
          @click="switchRole('teacher')"
        >
          <i class="fas fa-chalkboard-teacher"></i> 教师登录
        </div>
        <div 
          :class="['role-tab', { active: form.role === 'student' }]"
          @click="switchRole('student')"
        >
          <i class="fas fa-user-graduate"></i> 学生登录
        </div>
      </div>

      <div class="login-form">
        <div class="input-field">
          <i class="fas fa-user"></i>
          <input
            v-model="form.account"
            placeholder="邮箱/用户名"
            class="form-input"
            @focus="clearError"
          />
        </div>
        
        <div class="input-field">
          <i class="fas fa-lock"></i>
          <input
            v-model="form.password"
            :type="passwordVisible ? 'text' : 'password'"
            placeholder="密码"
            class="form-input"
            @focus="clearError"
            @keyup.enter="handleLogin"
          />
          <span 
            @click="togglePasswordVisibility"
            class="password-toggle"
          >
            <i v-if="passwordVisible" class="fas fa-eye-slash"></i>
            <i v-else class="fas fa-eye"></i>
          </span>
        </div>
        
        <div class="remember-field">
          <input type="checkbox" id="remember" v-model="rememberMe">
          <label for="remember">记住登录状态</label>
        </div>
        
        <button
          @click.prevent="handleLogin"
          :disabled="loading || !isFormValid"
          class="login-button"
          :class="{disabled: loading || !isFormValid}"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </button>
      </div>

      <div class="footer-links">
        <span>© {{ new Date().getFullYear() }} 智慧学习平台</span>
        <div>
          <a href="#">忘记密码</a>
          <router-link to="/register">注册</router-link>
          
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const form = ref({
  account: '',
  password: '',
  role: 'teacher'
});

// 添加错误信息状态
const errorMessage = ref('');
const passwordVisible = ref(false);
const rememberMe = ref(false);
const loading = ref(false);

// 表单验证状态
const isFormValid = computed(() => {
  return form.value.account.trim().length > 0 && form.value.password.length > 0;
});

const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
};

const switchRole = (role: 'teacher' | 'student') => {
  form.value.role = role;
  form.value.account = '';
  form.value.password = '';
  clearError();
};

// 清除错误信息
const clearError = () => {
  if (errorMessage.value) {
    errorMessage.value = '';
  }
};

const handleLogin = async () => {
  // 表单验证
  if (!form.value.account.trim()) {
    errorMessage.value = '请输入账号或邮箱';
    return;
  }
  
  if (!form.value.password) {
    errorMessage.value = '请输入密码';
    return;
  }
  
  // 登录逻辑
  loading.value = true;
  clearError();
  
  try {
    await authStore.login(form.value);
    
    if (rememberMe.value) {
      localStorage.setItem('rememberedAccount', form.value.account);
      localStorage.setItem('rememberedRole', form.value.role);
    } else {
      localStorage.removeItem('rememberedAccount');
      localStorage.removeItem('rememberedRole');
    }
    
    const dashboardPath = form.value.role === 'teacher' 
      ? '/teacher/dashboard' 
      : '/student/dashboard';
      
    router.push(dashboardPath);
  } catch (error: any) {
    // 更友好的错误提示
    if (error.response) {
      // API 返回的错误信息
      const { status, data } = error.response;
      if (status === 401) {
        errorMessage.value = '账号或密码错误，请重试';
      } else if (status === 403) {
        errorMessage.value = '该账号无权限访问系统';
      } else {
        errorMessage.value = `登录失败：${data.message || '服务器错误'}`;
      }
    } else {
      // 非API错误（网络问题等）
      errorMessage.value = '网络连接失败，请稍后再试';
    }
    
    console.error('登录失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const rememberedAccount = localStorage.getItem('rememberedAccount');
  const rememberedRole = localStorage.getItem('rememberedRole');
  
  if (rememberedAccount && rememberedRole) {
    form.value.account = rememberedAccount;
    form.value.role = rememberedRole;
    rememberMe.value = true;
  }
});
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.login-card {
  width: 480px;
  background: #ffffff;
  border-radius: 10px;
  padding: 40px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  position: relative;
}

/* 错误提示样式 */
.error-message {
  background-color: #fff6f6;
  color: #ff5252;
  padding: 12px 20px;
  border-radius: 6px;
  margin-bottom: 20px;
  border-left: 4px solid #ff5252;
  display: flex;
  align-items: center;
  gap: 10px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.error-message i {
  font-size: 18px;
}

.logo-section {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.logo-section svg {
  width: 48px;
  height: 48px;
  margin-right: 16px;
}

.logo-section h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #1e293b;
}

.logo-section p {
  font-size: 14px;
  color: #64748b;
}

.role-tabs {
  display: flex;
  margin-bottom: 25px;
  border-radius: 8px;
  background-color: #f1f5f9;
}

.role-tab {
  flex: 1;
  padding: 14px;
  text-align: center;
  cursor: pointer;
  font-weight: 500;
  color: #64748b;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.role-tab.active {
  background: #2563eb;
  color: white;
  border-radius: 8px;
}

.login-form {
  margin-top: 10px;
}

.input-field {
  position: relative;
  margin-bottom: 20px;
}

.input-field i {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.form-input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.password-toggle {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #64748b;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.password-toggle:hover {
  color: #2563eb;
}

.password-toggle i {
  position: static;
  transform: none;
}

.remember-field {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
}

.remember-field input {
  margin-right: 8px;
}

.login-button {
  width: 100%;
  padding: 14px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.login-button:hover:not(.disabled) {
  background: #1d4ed8;
}

.login-button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.footer-links {
  display: flex;
  justify-content: space-between;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
  font-size: 13px;
  color: #64748b;
}

.footer-links a {
  color: #2563eb;
  text-decoration: none;
  margin-left: 15px;
}

.footer-links a:hover {
  text-decoration: underline;
}

/* 禁用的登录按钮样式 */
.disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>