<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitHomework } from '@/services/task.service'
import { getTaskDetail } from '@/services/task.service'
import { getResourcesByTask } from '@/services/task'
import type { TaskDetailResponse, TaskResourceResponse } from '@/types/task'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const taskId = ref(Number(route.params.taskId))
const taskDetail = ref<TaskDetailResponse | null>(null)
const content = ref('')
const loading = ref(false)
const resources = ref<TaskResourceResponse[]>([])

// 获取任务详情
const fetchTaskDetail = async () => {
  try {
    loading.value = true
    const studentId = authStore.user?.studentId
    if (!studentId) throw new Error('未获取到学生ID')

    const response = await getTaskDetail(studentId, taskId.value)
    taskDetail.value = response
  } catch (err) {
    ElMessage.error('获取任务详情失败')
  } finally {
    loading.value = false
  }
}

// 获取任务资源列表
const fetchResources = async () => {
  if (!taskId.value) return
  try {
    resources.value = await getResourcesByTask(taskId.value)
  } catch (e) {
    ElMessage.error('获取资源失败')
  }
}

// 清空内容
const clearContent = () => {
  content.value = ''
}

// 提交作业
const submitTask = async () => {
  try {
    if (taskDetail.value?.submitted) {
      ElMessage.warning('您已提交过该作业，无需重复提交')
      return
    }
    if (!content.value.trim()) {
      ElMessage.warning('请填写作业内容')
      return
    }
    // ...后续代码不变
    // try {
    if (!content.value.trim()) {
      ElMessage.warning('请填写作业内容')
      return
    }

    loading.value = true
    const authStore = useAuthStore()
    const studentId = authStore.user?.studentId
    if (!studentId) throw new Error('未获取到学生ID')

    await submitHomework({
      taskId: taskId.value,
      studentId: studentId,
      content: content.value,
    })

    ElMessage.success('作业提交成功')
    router.push('/student/dashboard')
    // router.push({ path: '/student/dashboard', query: { tab: 'tasks' } })
  } catch (err: any) {
    console.error('提交错误:', err.response?.data || err.message)
    ElMessage.error(`提交失败: ${err.response?.data?.message || err.message}`)
  } finally {
    loading.value = false
  }
}

// 下载方法
const downloadResource = async (url, name) => {
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
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

onMounted(() => {
  fetchTaskDetail()
  fetchResources()
})
</script>

<template>
  <div class="homework-detail-container">
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

      <!-- 提交区域 -->
      <div class="submit-section">
        <h3 style="margin-bottom: 12px">提交作业</h3>
        <el-input
          v-model="content"
          type="textarea"
          :rows="10"
          placeholder="请输入作业内容..."
          resize="none"
        />

        <div class="action-buttons">
          <el-button @click="clearContent">清空内容</el-button>
          <el-button type="primary" @click="submitTask" :loading="loading">提交作业</el-button>
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
.homework-detail-container {
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

.action-buttons {
  margin-top: 20px;
  text-align: right;
}

.action-buttons .el-button {
  margin-left: 10px;
}

.section-title {
  font-weight: bold;
  font-size: 18px;
  margin-bottom: 10px;
}
</style>
