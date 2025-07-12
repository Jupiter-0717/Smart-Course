<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitReport } from '@/services/task.service'
import { getTaskDetail } from '@/services/task.service'
import { getResourcesByTask } from '@/services/task'
import type { UploadFile, UploadFiles } from 'element-plus'
import type { TaskDetailResponse, TaskResourceResponse } from '@/types/task'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const taskId = ref(Number(route.params.taskId))
const taskDetail = ref<TaskDetailResponse | null>(null)
const fileList = ref<UploadFile[]>([])
const loading = ref(false)
const resources = ref<TaskResourceResponse[]>([])
const MAX_TOTAL_SIZE = 100 * 1024 * 1024 // 100MB
const MAX_FILE_SIZE = 100 * 1024 * 1024 // 100MB

// 获取任务详情
const fetchTaskDetail = async () => {
  try {
    loading.value = true
    const studentId = authStore.user?.studentId
    if (!studentId) throw new Error('未获取到学生ID')

    const response = await getTaskDetail(studentId, taskId.value)
    taskDetail.value = response
  } catch (err) {
    console.error('任务列表接口异常:', err)
    ElMessage.error('获取任务详情失败')
  } finally {
    loading.value = false
  }
}

// 获取资源
const fetchResources = async () => {
  if (!taskId.value) return
  try {
    resources.value = await getResourcesByTask(taskId.value)
  } catch (error) {
    ElMessage.error('获取资源失败')
  }
}

// 处理文件上传
const handleUploadChange = (file: UploadFile, files: UploadFiles) => {
  const MAX_TOTAL_SIZE = 100 * 1024 * 1024 // 100MB
  const totalSize = files.reduce((sum: number, f: UploadFile) => sum + (f.size || 0), 0)
  if (totalSize > MAX_TOTAL_SIZE) {
    ElMessage.warning('所有文件总大小不能超过100MB')
    // 移除最后一个文件
    files.pop()
    return
  }
  // 确保文件列表的稳定性
  fileList.value = [...files]
}

// 删除文件
const handleRemove = (file: UploadFile) => {
  fileList.value = fileList.value.filter((f) => f.uid !== file.uid)
}

// 文件上传前的验证
const beforeUpload = (file: UploadFile) => {
  // 只允许 .pdf 和 .docx
  const allowedTypes = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  ]
  const allowedExts = ['.pdf', '.docx']

  const isTypeAllowed = allowedTypes.includes(file.type as string)
  const fileName = file.name || ''
  const isExtAllowed = allowedExts.some((ext) => fileName.toLowerCase().endsWith(ext))

  if (!isTypeAllowed || !isExtAllowed) {
    ElMessage.error('只能上传 PDF 或 Word(.docx) 文档')
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.warning('单个文件不能超过100MB')
    return false
  }
  return true
}

// 提交报告
const submitTask = async () => {
  try {
    if (taskDetail.value?.submitted) {
      ElMessage.warning('您已提交过该报告，无需重复提交')
      return
    }
    if (fileList.value.length === 0) {
      ElMessage.warning('请至少上传一个文件')
      return
    }

    loading.value = true
    const studentId = authStore.user?.studentId
    if (!studentId) throw new Error('未获取到学生ID')

    // 确保文件对象稳定，避免在传输过程中被修改
    const files = fileList.value
      .filter((file: UploadFile) => file.raw && file.raw instanceof File)
      .map((file: UploadFile) => file.raw as File)

    if (files.length === 0) {
      ElMessage.warning('没有有效的文件可以提交')
      return
    }

    await submitReport({
      taskId: taskId.value,
      studentId: studentId,
      files: files,
    })

    ElMessage.success('报告提交成功')
    router.push('/student/dashboard')
  } catch (err: unknown) {
    const error = err as { message?: string; response?: { data?: { message?: string } } }
    const msg = error?.message || error?.response?.data?.message || '提交失败'
    if (msg.includes('已提交') || msg.includes('重复提交')) {
      ElMessage.warning(msg)
    } else {
      ElMessage.error(`提交失败: ${msg}`)
    }
  } finally {
    loading.value = false
  }
}

const downloadResource = async (url: string, name: string) => {
  try {
    const response = await fetch(url)
    const blob = await response.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = name // 这里用原始文件名
    document.body.appendChild(link)
    link.click()
    window.URL.revokeObjectURL(blobUrl)
    document.body.removeChild(link)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

onMounted(() => {
  fetchTaskDetail()
  fetchResources()
})
</script>

<template>
  <div class="report-detail-container">
    <div class="header">
      <h2>{{ taskDetail?.title || '加载中...' }}</h2>
      <el-button @click="router.push('/student/dashboard')">返回列表</el-button>
    </div>

    <div class="task-content" v-loading="loading">
      <!-- 任务描述 -->
      <div class="description-section">
        <h3 style="margin-bottom: 12px">任务描述</h3>
        <div class="description-content" v-html="taskDetail?.description || '暂无描述'"></div>
      </div>

      <!-- 评分与评语展示，仅作业/报告类型显示 -->
      <div
        v-if="taskDetail && (taskDetail.type === 'HOMEWORK' || taskDetail.type === 'REPORT')"
        class="grade-feedback-section"
      >
        <div>
          <b>得分：</b>
          <span>{{ taskDetail.grade != null ? taskDetail.grade : '未评分' }}</span>
        </div>
        <div>
          <b>评语：</b>
          <span>{{ taskDetail.feedback ? taskDetail.feedback : '暂无评语' }}</span>
        </div>
      </div>

      <!-- 资源列表 -->
      <div class="resources-section" v-if="resources.length > 0">
        <h3 style="margin-bottom: 12px">相关资源</h3>
        <div class="resource-list">
          <div v-for="resource in resources" :key="resource.id" class="resource-item">
            <template v-if="resource.fileType && resource.fileType.startsWith('video')">
              <div class="video-center">
                <video
                  :src="resource.filePath"
                  controls
                  style="max-width: 600px; width: 100%; display: block; margin: 0 auto"
                />
                <div class="video-name">{{ resource.name }}</div>
              </div>
            </template>
            <template v-else>
              <span class="file-name">{{ resource.name }}</span>
              <el-button
                size="small"
                class="download-btn"
                @click="downloadResource(resource.filePath, resource.name)"
                >下载</el-button
              >
              <span class="file-type">({{ resource.fileType }})</span>
            </template>
          </div>
        </div>
      </div>

      <!-- 修改上传区域 -->
      <div class="submit-section">
        <h3 style="margin-bottom: 8px">提交报告</h3>
        <div class="upload-tip">
          <el-text type="info">请上传PDF或Word文档(.docx)</el-text>
        </div>

        <el-upload
          v-model:file-list="fileList"
          class="upload-demo"
          action="#"
          multiple
          :auto-upload="false"
          :before-upload="beforeUpload"
          :on-change="handleUploadChange"
          :on-remove="handleRemove"
        >
          <template #trigger>
            <el-button type="primary">选择文件</el-button>
          </template>

          <template #tip>
            <div class="el-upload__tip" style="margin-top: 12px">
              支持上传多个文件，每个文件不超过100MB，所有文件总和不超过100MB
            </div>
          </template>
        </el-upload>

        <div class="action-buttons" style="margin-top: 5px">
          <el-button type="primary" @click="submitTask" :loading="loading">提交报告</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grade-feedback-section {
  margin-bottom: 18px;
  padding: 12px 16px;
  background: #f8f8f8;
  border-radius: 6px;
  font-size: 16px;
}
.grade-feedback-section b {
  color: #409eff;
}
.report-detail-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.description-section,
.resources-section,
.submit-section {
  margin-bottom: 30px;
}

.description-content {
  padding: 15px;
  background-color: #f8f8f8;
  border-radius: 4px;
  line-height: 1.6;
}

.resource-list {
  padding: 15px;
  background-color: #f8f8f8;
  border-radius: 4px;
}

.resource-item {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.file-name {
  margin-right: 16px;
  font-weight: 400;
}

.download-btn {
  margin-right: 12px;
}

.video-center {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 12px;
}

.video-name {
  margin-top: 8px;
  font-size: 15px;
  color: #333;
}

.file-type {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}

.upload-tip {
  margin-bottom: 15px;
}

.action-buttons {
  margin-top: 20px;
  text-align: right;
}

:deep(.el-upload-list) {
  max-height: 200px;
  overflow-y: auto;
}

.section-title {
  font-weight: bold;
  font-size: 18px;
  margin-bottom: 10px;
}
</style>
