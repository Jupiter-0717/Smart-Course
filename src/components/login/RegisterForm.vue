
<template>
  <div class="register-container">
   <!-- 成功提示框 -->
    <div v-if="successMessage" class="success-message">
      <i class="fas fa-check-circle"></i>
      {{ successMessage }}
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
        <i class="fas fa-chalkboard-teacher"></i> 教师注册
      </div>
      <div 
        :class="['role-tab', { active: form.role === 'student' }]"
        @click="switchRole('student')"
      >
        <i class="fas fa-user-graduate"></i> 学生注册
      </div>
    </div>

    <div class="register-form">
      <div class="input-field">
        <i class="fas fa-user"></i>
        <input
          v-model="form.account"
          placeholder="设置您的账号"
          class="form-input"
          @focus="clearError"
        />
      </div>
      
      <div class="input-field">
        <i class="fas fa-lock"></i>
        <input
          v-model="form.password"
          :type="passwordVisible ? 'text' : 'password'"
          placeholder="设置密码"
          class="form-input"
          @focus="clearError"
        />
        <span 
          @click="togglePasswordVisibility"
          class="password-toggle"
        >
          <i v-if="passwordVisible" class="fas fa-eye-slash"></i>
          <i v-else class="fas fa-eye"></i>
        </span>
      </div>

      <div class="input-field">
        <i class="fas fa-signature"></i>
        <input
          v-model="form.name"
          placeholder="真实姓名"
          class="form-input"
          @focus="clearError"
        />
      </div>

      <!-- 学生注册的班级信息 -->
      <div v-if="form.role === 'student'" class="input-field">
        <i class="fas fa-users"></i>
        <input
          v-model="form.className"
          placeholder="班级（例如：计算机科学2023级1班）"
          class="form-input"
          @focus="clearError"
        />
      </div><!-- 学生注册的学号信息 -->
<div v-if="form.role === 'student'" class="input-field">
  <i class="fas fa-id-card"></i>
  <input
    v-model="form.studentId"
    placeholder="学号（如：20230001）"
    class="form-input"
    @focus="clearError"
    type="number"
  />
</div>


      <!-- 教师注册的院系信息 -->
      <div v-if="form.role === 'teacher'" class="input-field">
        <i class="fas fa-building"></i>
        <input
          v-model="form.department"
          placeholder="院系信息"
          class="form-input"
          @focus="clearError"
        />
      </div>
      
      <button
        @click.prevent="handleRegister"
        :disabled="loading || !isFormValid"
        class="register-button"
        :class="{disabled: loading || !isFormValid}"
      >
        <span v-if="!loading">创建账号</span>
        <span v-else>正在创建...</span>
      </button>
    </div>

    <div class="footer-links">
      <span>已有账号？</span>
      <div>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import type { Ref } from 'vue';

const router = useRouter();

// 表单数据结构
const form = ref({
  account: '',
  password: '',
  name: '',
  role: 'teacher', // teacher or student
  className: '', // 学生特有
  department: '', // 教师特有
  studentId: ''
});
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
// 错误信息状态
const errorMessage: Ref<string> = ref('');
const successMessage: Ref<string> = ref(''); 
const passwordVisible = ref(false);
const loading = ref(false);

// 表单验证状态
const isFormValid = computed(() => {
  // 基础验证
  const valid = form.value.account.trim().length > 0 && 
               form.value.password.length > 0 && 
               form.value.name.trim().length > 0;
  
  // 角色特定验证
  if (form.value.role === 'student') {
    return valid && form.value.className.trim().length > 0;
  } else if (form.value.role === 'teacher') {
    return valid && form.value.department.trim().length > 0;
  }
  
  return valid;
});

// 切换密码可见性
const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
};

const switchRole = (role: 'teacher' | 'student') => {
  // 清空整个表单
  form.value = {
    account: '',
    password: '',
    name: '',
    role: role, // 设置新的角色
    className: '',
    department: '',
    studentId:''
  };
  clearError();
};
// 清除错误信息
const clearError = () => {
  errorMessage.value = '';
};
// 处理注册请求
const handleRegister = async () => {
    // 表单验证（新增学号校验）
  // if (form.value.role === 'student' && !form.value.studentId.trim()) {
  //   errorMessage.value = '请输入学号';
  //   return;
  // }
  // 表单验证
  if (!form.value.account.trim()) {
    errorMessage.value = '请输入账号';
    return;
  }
  
  if (form.value.password.length < 2) {
    errorMessage.value = '密码至少需要6个字符';
    return;
  }
  
  if (!form.value.name.trim()) {
    errorMessage.value = '请输入真实姓名';
    return;
  }
  
  if (form.value.role === 'student' && !form.value.className.trim()) {
    errorMessage.value = '请输入班级信息';
    return;
  }
  
  if (form.value.role === 'teacher' && !form.value.department.trim()) {
    errorMessage.value = '请输入院系信息';
    return;
  }
  
  // 登录逻辑
  loading.value = true;
  clearError();
  
  try {
     const normalizedRole = form.value.role.toLowerCase();
    // 准备API请求数据
    const registrationData = {
      account: form.value.account,
      password: form.value.password,
      role: normalizedRole,
      name: form.value.name,
      className: form.value.className,   // 学生使用
      department: form.value.department,
      studentId: form.value.studentId  // 教师使用
    };
    
    // 发送注册请求
    const response = await axios.post(
  `${API_BASE_URL}/api/auth/register`,
  registrationData
);
    
    // 注册成功后提示并跳转到登录页面
    if (response.data === '注册成功') {
      
      successMessage.value = '注册成功！即将跳转到登录页面...';
      // 2秒后跳转
      setTimeout(() => {
        router.push('/login');
      }, 1000);
    } else {
      errorMessage.value = '注册失败，请稍后再试';
    }
  } catch (error: any) {
    successMessage.value = '';
    // 错误处理
    if (error.response) {
      // API 返回的错误信息
      const { status, data } = error.response;
      
      if (status === 409) {
        errorMessage.value = '该账号已被注册';
      } else {
        errorMessage.value = `注册失败：${data.message || '服务器错误'}`;
      }
    } else {
      // 非API错误（网络问题等）
      errorMessage.value = '网络连接失败，请稍后再试';
    }
    
    console.error('注册失败:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-container {
  max-width: 100%;
}

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

.register-form {
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

.register-button {
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
  margin-top: 10px;
}

.register-button:hover:not(.disabled) {
  background: #1d4ed8;
}

.register-button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.footer-links {
  display: flex;
  justify-content: center;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
  font-size: 14px;
  color: #64748b;
  gap: 8px;
}

.footer-links a {
  color: #2563eb;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}

.success-message {
  background-color: #f0fff4;
  color: #38a169;
  padding: 12px 20px;
  border-radius: 6px;
  margin-bottom: 20px;
  border-left: 4px solid #38a169;
  display: flex;
  align-items: center;
  gap: 10px;
  animation: fadeIn 0.3s ease;
}


/* 禁用的登录按钮样式 */
.disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>