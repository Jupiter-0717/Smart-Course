<template>
  <el-dialog
    v-model="visible"
    :title="fileName || '文件预览'"
    width="80%"
    :before-close="handleClose"
    class="file-preview-dialog"
  >
    <div class="preview-content">
      <!-- 图片预览 -->
      <img
        v-if="isImage"
        :src="fileUrl"
        :alt="fileName"
        class="preview-image"
        @error="handleImageError"
      />
      
      <!-- PDF预览 -->
      <iframe
        v-else-if="isPdf"
        :src="fileUrl"
        class="preview-pdf"
        frameborder="0"
      ></iframe>
      
      <!-- 文本文件预览 -->
      <div
        v-else-if="isText"
        class="preview-text"
        v-html="textContent"
      ></div>
      
      <!-- 不支持预览的文件 -->
      <div v-else class="preview-unsupported">
        <el-icon class="unsupported-icon"><Document /></el-icon>
        <p>该文件类型不支持在线预览</p>
        <el-button type="primary" @click="downloadFile">
          <el-icon><Download /></el-icon>
          下载文件
        </el-button>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="downloadFile">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps<{
  visible: boolean
  fileUrl: string
  fileName: string
  submissionId: string | number
}>()

// 添加默认值处理
const visible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const textContent = ref('')
const imageError = ref(false)

// 计算属性
const isImage = computed(() => {
  const ext = getFileExtension(props.fileName).toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)
})

const isPdf = computed(() => {
  return getFileExtension(props.fileName).toLowerCase() === 'pdf'
})

const isText = computed(() => {
  const ext = getFileExtension(props.fileName).toLowerCase()
  return ['txt', 'md', 'json', 'xml', 'html', 'css', 'js'].includes(ext)
})

// 方法
function getFileExtension(fileName: string): string {
  const parts = fileName.split('.')
  return parts.length > 1 ? parts[parts.length - 1] : ''
}

function handleClose() {
  emit('update:visible', false)
}

async function downloadFile() {
  try {
    const response = await axios.get(`/api/correct/download/${props.submissionId}`, {
      responseType: 'blob'
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', props.fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('文件下载失败')
  }
}

async function loadTextContent() {
  if (!isText.value) return
  
  try {
    const response = await axios.get(props.fileUrl)
    textContent.value = response.data
  } catch (error) {
    console.error('加载文本内容失败:', error)
    textContent.value = '无法加载文件内容'
  }
}

function handleImageError() {
  imageError.value = true
  ElMessage.error('图片加载失败')
}

// 监听文件URL变化
watch(() => props.fileUrl, (newUrl) => {
  if (newUrl && isText.value) {
    loadTextContent()
  }
}, { immediate: true })
</script>

<style scoped>
.file-preview-dialog {
  max-height: 90vh;
}

.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  max-height: 70vh;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-pdf {
  width: 100%;
  height: 70vh;
  border: none;
}

.preview-text {
  width: 100%;
  height: 70vh;
  overflow: auto;
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px;
  color: #909399;
}

.unsupported-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.preview-unsupported p {
  margin: 0;
  font-size: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 