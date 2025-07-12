<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import axios from "axios";

const API_BASE_URL= import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const loading = ref(false)
const error = ref(null)
interface ClassResource {
  id: string;
 name: string;
  description: string;
  type: string;
  url: string;  // 文件URL在这里
}

interface WeightedResource {
  resource: ClassResource;
  weight: number;
  relatedKnowledgePointIds: number[];
}

// 更新 documents 的类型定义
const documents = ref<WeightedResource[]>([])

onMounted(async () => {
  if (user.value?.role === 'student' && user.value.studentId) {
    try {
      loading.value = true
      const response = await axios.get(`${API_BASE_URL}/api/recommendations/student/${user.value.studentId}?limit=5`)
      documents.value = response.data
      console.log(response)
    } catch (err) {
      error.value = err.message || '获取文档推荐失败'
    } finally {
      loading.value = false
    }
  }
})
const downloadDocument = (doc: WeightedResource) => {
    console.log(doc.resource.url)
  if (!doc?.resource?.url) {
    console.error('无效的文档资源:', doc)
    error.value = '文档链接无效'
    return
  }

  // 创建完整下载URL
  const downloadUrl = doc.resource.url.startsWith('http') 
    ? doc.resource.url
    : `${API_BASE_URL}${doc.resource.url}`;
  
  // 创建隐藏的a标签触发下载
  const link = document.createElement('a');
  link.href = downloadUrl;
  
  // 使用文档标题作为文件名，如果没有则从URL提取
  const fileName = doc.resource.name || 
                  downloadUrl.split('/').pop() || 
                  `document_${Date.now()}`;
  
  link.setAttribute('download', fileName);
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
// 新增的辅助方法 - 不影响原有逻辑
const getFileIconClass = (type) => {
  if (!type) return '';
  if (type.toLowerCase().includes('pdf')) return 'pdf-icon';
  if (type.toLowerCase().includes('word') || type.toLowerCase().includes('doc')) return 'doc-icon';
  if (type.toLowerCase().includes('ppt') || type.toLowerCase().includes('powerpoint')) return 'ppt-icon';
  return '';
};

const getFileEmoji = (type) => {
  if (!type) return '📄';
  if (type.toLowerCase().includes('pdf')) return '📕';
  if (type.toLowerCase().includes('word') || type.toLowerCase().includes('doc')) return '📘';
  if (type.toLowerCase().includes('ppt') || type.toLowerCase().includes('powerpoint')) return '📙';
  return '📄';
};
</script>
<template>
  <div class="document-recommendation">
    <h3 class="recommendation-title">
      <span class="title-icon">📚</span> 推荐学习资料
    </h3>
    
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在为您精心挑选学习资料...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <span class="error-icon">⚠️</span>
      <p class="error-message">暂时没有找到推荐资料</p>
    </div>

    <div v-else-if="documents.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>当前没有可推荐的资料</p>
    </div>

    <div v-else class="document-list">
      <div 
        v-for="doc in documents" 
        :key="doc.resource.id" 
        class="document-card"
        @click="downloadDocument(doc)"
      >
        <div class="document-icon" :class="getFileIconClass(doc.resource.type)">
          {{ getFileEmoji(doc.resource.type) }}
        </div>
        <div class="document-info">
          <div class="document-title">{{ doc.resource.name }}</div>
          <div class="document-description">{{ doc.resource.description }}</div>
          <div class="document-meta">
            <span class="document-type">{{ doc.resource.type }}</span>
            <div class="weight-indicator">
              <div class="weight-bar" :style="{ width: `${doc.weight * 100}%` }"></div>
              <span class="document-weight">{{ (doc.weight * 100).toFixed(0) }}%匹配</span>
            </div>
          </div>
          <div class="download-hint">
            <span class="download-icon">⬇️</span> 点击下载
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.document-recommendation {
  margin-top: 2rem;
  padding: 1.5rem;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #eaeaea;
}

.recommendation-title {
  color: #2c3e50;
  margin-bottom: 1.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  
  .title-icon {
    margin-right: 10px;
    font-size: 1.4em;
  }
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.document-card {
  display: flex;
  align-items: flex-start;
  padding: 1.2rem;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #eaeaea;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    width: 4px;
    background: #3498db;
    transform: scaleY(0);
    transition: transform 0.2s ease;
  }

  &:hover {
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: #3498db;

    &::before {
      transform: scaleY(1);
    }

    .download-hint {
      color: #3498db;
    }
  }
}

.document-icon {
  font-size: 1.8rem;
  margin-right: 1.2rem;
  flex-shrink: 0;
  
  &.pdf-icon {
    color: #e74c3c;
  }
  
  &.doc-icon {
    color: #3498db;
  }
  
  &.ppt-icon {
    color: #e67e22;
  }
}

.document-info {
  flex: 1;
  min-width: 0;
}

.document-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
  font-size: 1.05rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.document-description {
  font-size: 0.9rem;
  color: #7f8c8d;
  margin-bottom: 0.8rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.document-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.8rem;
}

.document-type {
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 12px;
  color: #34495e;
  font-size: 0.75rem;
  font-weight: 500;
}

.weight-indicator {
  width: 100px;
  height: 20px;
  background: #ecf0f1;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}

.weight-bar {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #3498db, #2ecc71);
  border-radius: 10px;
  transition: width 0.5s ease;
}

.document-weight {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  color: white;
  font-size: 0.7rem;
  font-weight: bold;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);
}

.download-hint {
  margin-top: 0.8rem;
  color: #7f8c8d;
  font-size: 0.85rem;
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  transition: color 0.2s ease;
  
  .download-icon {
    margin-right: 5px;
  }
}

.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 2rem 1rem;
  color: #7f8c8d;
  
  p {
    margin-top: 1rem;
    font-size: 0.95rem;
  }
}

.spinner {
  border: 3px solid rgba(52, 152, 219, 0.1);
  border-top: 3px solid #3498db;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

.error-icon {
  font-size: 2rem;
  display: inline-block;
  color: #e74c3c;
}

.empty-icon {
  font-size: 2rem;
  display: inline-block;
  color: #bdc3c7;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>

