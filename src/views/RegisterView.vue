<template>
  <div class="register-view">
    <div class="register-card">
      <h1>创建新账号</h1>
      <RegisterForm />
    </div>
  </div>
</template>

<script setup lang="ts">
import RegisterForm from '@/components/login/RegisterForm.vue';
</script>

<style scoped>
.register-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.register-card {
  width: 480px;
  background: #ffffff;
  border-radius: 10px;
  padding: 40px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 30px;
  color: #1e293b;
  text-align: center;
}
</style>