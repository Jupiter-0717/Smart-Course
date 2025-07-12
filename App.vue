<template>
  <div class="app-container">
    <RouterView />
    
    <footer class="global-footer">
      <div class="footer-content">
        <div class="footer-brand">
          <svg viewBox="0 0 24 24" fill="none" class="footer-logo">
            <path d="M4 19V6.2C4 5.0799 4 4.51984 4.21799 4.09202C4.40973 3.71569 4.71569 3.40973 5.09202 3.21799C5.51984 3 6.0799 3 7.2 3H16.8C17.9201 3 18.4802 3 18.908 3.21799C19.2843 3.40973 19.5903 3.71569 19.782 4.09202C20 4.51984 20 5.0799 20 6.2V17H6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 10H16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M8 14H13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <div>
            <p class="brand-name">智慧学习平台</p>
            <p class="brand-tagline">助力个人知识管理</p>
          </div>
        </div>
        
        <div class="footer-divider"></div>
        
        <div class="footer-links">
          <div class="link-column">
            <h4>产品</h4>
            <ul>
              <li><a href="#">功能特性</a></li>
              <li><a href="#">更新日志</a></li>
              <li><a href="#">路线图</a></li>
            </ul>
          </div>
          
          <div class="link-column">
            <h4>支持</h4>
            <ul>
              <li><a href="#">帮助中心</a></li>
              <li><a href="#">社区</a></li>
              <li><a href="#">API</a></li>
            </ul>
          </div>
          
          <div class="link-column">
            <h4>公司</h4>
            <ul>
              <li><a href="#">关于我们</a></li>
              <li><a href="#">职业机会</a></li>
              <li><a href="#">联系方式</a></li>
            </ul>
          </div>
        </div>
      </div>
      
      <div class="footer-info">
        <p>© 2023 智慧学习平台 版权所有</p>
        <div>
          <a href="#">隐私政策</a>
          <a href="#">服务条款</a>
          <a href="#">安全</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  width: 100%;
}

/* 全局页脚样式 - 极简桌面风格 */
.global-footer {
  background-color: #ffffff;
  color: #334155;
  padding: 40px 0 20px;
  margin-top: auto;
  border-top: 1px solid #e2e8f0;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: block;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.footer-logo {
  width: 36px;
  height: 36px;
  color: #6366f1;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #1e293b;
}

.brand-tagline {
  font-size: 13px;
  color: #64748b;
}

.footer-divider {
  display: true;
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  margin-bottom: 40px;
}

.link-column {
  min-width: unset;
}

.link-column h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 14px;
  color: #1e293b;
}

.link-column ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.link-column li {
  margin-bottom: 10px;
}

.link-column a {
  color: #64748b;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}

.link-column a:hover {
  color: #3b82f6;
}

.footer-info {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 20px 0;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-info p {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.footer-info div {
  display: flex;
  gap: 20px;
}

.footer-info a {
  font-size: 12px;
  color: #64748b;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-info a:hover {
  color: #3b82f6;
}


</style>