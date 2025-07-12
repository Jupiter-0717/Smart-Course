<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import AiScoreCriteriaForm from '@/components/task/AiScoreCriteriaForm.vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId
interface Submission {
  studentName: string
  submittedAt: string
  grade?: number
  feedback?: string
  submissionId: number
}

interface Criteria {
  criteria1: string
  c1: number
  criteria2: string
  c2: number
  criteria3: string
  c3: number
  criteria4: string
  c4: number
  criteria5: string
  c5: number
  criteria6: string
  c6: number
}

const submissions = ref<Submission[]>([])

// 智能批改相关
const criteria = ref<Criteria>({
  criteria1: '',
  c1: 0,
  criteria2: '',
  c2: 0,
  criteria3: '',
  c3: 0,
  criteria4: '',
  c4: 0,
  criteria5: '',
  c5: 0,
  criteria6: '',
  c6: 0,
})
const aiScoreLoading = ref(false)

function formatDateTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

console.log('taskId:', taskId)

onMounted(async () => {
  try {
    const res = await axios.get(`/api/correct/tasks/${taskId}/list`)
    console.log('接口返回:', res.data)
    submissions.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('请求失败:', e)
    submissions.value = []
  }
})

function goToCorrect(submissionId: number) {
  router.push(`/teacher/submissions/${submissionId}/correct`)
}

function goBack() {
  router.back()
}

// 智能批改处理
const handleAiScore = async (criteriaValue: Criteria) => {
  aiScoreLoading.value = true
  try {
    const res = await axios.post('/api/ai-score/task', {
      taskId,
      criteria: criteriaValue,
    })
    if (res.data && res.data.success === false) {
      ElMessageBox.alert(res.data.message || '权重校验失败', '错误', { type: 'error' })
      return
    }
    ElMessageBox.alert('智能批改已提交，正在处理...', '成功', { type: 'success' })
    router.push({ name: 'TaskAiScoreResult', params: { taskId } })
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    const msg = err?.response?.data?.message
    ElMessageBox.alert(msg || '请求失败，请稍后重试', '错误', { type: 'error' })
    console.error('智能批改失败:', error)
  } finally {
    aiScoreLoading.value = false
  }
}
</script>

<template>
  <div class="task-submissions-view">
    <div class="header-bar">
      <el-button type="primary" @click="goBack" class="back-btn center-btn">返回</el-button>
      <div class="page-title">
        <el-icon style="font-size: 28px; margin-right: 8px"
          ><i class="el-icon-document"></i
        ></el-icon>
        <span>学生提交列表</span>
      </div>
    </div>
    <div class="divider"></div>

    <!-- 智能批改标准和权重表单 -->
    <AiScoreCriteriaForm v-model="criteria" :loading="aiScoreLoading" @ai-score="handleAiScore" />

    <el-table
      :data="submissions"
      class="submissions-table"
      height="calc(100vh - 110px)"
      style="width: 100%"
    >
      <el-table-column prop="studentName" label="学生姓名" />
      <el-table-column prop="submittedAt" label="提交时间">
        <template #default="{ row }">
          {{ formatDateTime(row.submittedAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="grade" label="得分" />
      <el-table-column prop="feedback" label="评语" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="primary" @click="goToCorrect(row.submissionId)">批改/查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.task-submissions-view {
  width: 100vw;
  min-height: 100vh;
  background: #fff;
  padding: 0;
  margin: 0;
  border-radius: 0;
  box-shadow: none;
  display: flex;
  flex-direction: column;
}
.header-bar {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  padding: 24px 32px 0 32px;
  background: #fff;
  z-index: 2;
  justify-content: flex-start;
}
.page-title {
  display: flex;
  align-items: center;
  font-size: 2rem;
  font-weight: bold;
  margin-left: 0;
  color: #409eff;
}
.back-btn.center-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  font-size: 16px;
  border-radius: 6px;
  min-width: 80px;
}
.divider {
  width: 100%;
  height: 1px;
  background: #e5e6eb;
  margin: 18px 0 12px 0;
}
.submissions-table {
  flex: 1 1 auto;
  margin: 0;
  padding: 0 32px 32px 32px;
  background: #fff;
}
</style>
