<template>
  <div class="file-update-form">
    <!-- 简洁的信息提示区 -->
    <div class="info-section">
      <div class="info-item">
        <span class="info-label">当前类型：</span>
        <span class="type-value">{{ resource.type.toUpperCase() }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">更换规则：</span>
        <span class="info-text">请选择相同类型的新文件</span>
      </div>
    </div>

    <el-form label-position="top">
      <el-form-item label="选择新文件">
        <el-upload
          class="upload-demo"
          drag
          :limit="1"
          :auto-upload="false"
          :on-change="handleFileChange"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 PDF, PPT, Word, MP4 格式文件，大小不超过100MB</div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="submitForm" :disabled="!file"> 更新文件 </el-button>
          <el-button @click="$emit('cancel')">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import type { Resource } from '@/types/resource'

const props = defineProps({
  resource: {
    type: Object as () => Resource,
    required: true,
  },
})

const emit = defineEmits(['submit', 'cancel'])

const file = ref<File | null>(null)

const handleFileChange = (uploadedFile: any) => {
  file.value = uploadedFile.raw
}

const submitForm = () => {
  if (file.value) {
    emit('submit', file.value)
  }
}
</script>

<style scoped>
.file-update-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

/* 简洁的信息区域 */
.info-section {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #606266;
  min-width: 70px;
}

.type-value {
  font-weight: 500;
  color: #1a73e8;
}

.info-text {
  color: #5a7da0;
}

/* 上传区域 */
.upload-demo {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.el-upload-dragger {
  padding: 30px;
}

.el-icon--upload {
  font-size: 40px;
  color: #409eff;
}

.el-upload__text em {
  color: #409eff;
  font-style: normal;
}

/* 按钮区域 */
.form-actions {
  text-align: right;
  margin-top: 20px;
}

.el-button {
  padding: 10px 20px;
}
</style>
