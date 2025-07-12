<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { TaskResponse } from '@/types/task'
import { getTasksByCourse } from '@/services/task.service'
import { ref, onMounted, computed } from 'vue'

const authStore = useAuthStore()
const router = useRouter()
const studentId = computed(() => authStore.user?.studentId)

// 筛选条件
const filters = ref({
  courseCode: '1',
  courseName: '',
  term: '2024学年 第1学期',
  taskType: '',
  // submitted: undefined as boolean | undefined,
  submitted: '',
})

// 任务列表
const tasks = ref<TaskResponse[]>([])
const loading = ref(false)
const error = ref('')

// 学期选项
const termOptions = ['2024学年 第1学期', '2024学年 第2学期', '2024学年 暑期学期']

// 任务类型选项
const taskTypeOptions = [
  { label: '全部', value: '' },
  { label: '作业', value: 'HOMEWORK' },
  { label: '报告', value: 'REPORT' },
  { label: '测验', value: 'QUIZ' },
]

// 提交状态选项
const submittedOptions = [
  { label: '全部', value: '' },
  { label: '已提交', value: true },
  { label: '未提交', value: false },
]

// 获取任务列表
const fetchTasks = async () => {
  try {
    loading.value = true
    error.value = ''
    const studentId = authStore.user?.studentId
    if (!studentId) {
      throw new Error('未获取到学生ID')
    }
    // 类型转换，保证 submitted 为 boolean | undefined
    let submitted: boolean | undefined = undefined
    if (filters.value.submitted === true || filters.value.submitted === false) {
      submitted = filters.value.submitted
    }
    // 类型转换，保证 taskType 为 string | undefined
    const taskType = filters.value.taskType === '' ? undefined : filters.value.taskType
    const response = await getTasksByCourse(
      studentId,
      filters.value.courseCode,
      filters.value.courseName,
      filters.value.term,
      taskType,
      submitted,
    )
    tasks.value = response
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取任务列表失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  filters.value = {
    courseCode: '1',
    courseName: '',
    term: '2024学年 第1学期',
    taskType: '',
    submitted: '',
  }
  fetchTasks()
}

// 进入任务详情
const enterTaskDetail = (task: TaskResponse) => {
  if (task.type === 'HOMEWORK') {
    router.push({ name: 'HomeworkTaskDetail', params: { taskId: task.id } })
  } else if (task.type === 'REPORT') {
    router.push({ name: 'ReportTaskDetail', params: { taskId: task.id } })
  } else if (task.type === 'QUIZ') {
    router.push({ name: 'StudentQuizTest' })
  } else {
    ElMessage.warning('未知任务类型')
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

// 检查是否过期
const isExpired = (dueDate?: string) => {
  if (!dueDate) return false
  return new Date(dueDate) < new Date()
}

onMounted(() => {
  fetchTasks()
})
</script>

<template>
  <div class="task-list-container">
    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filters" class="filter-form">
        <!-- First row -->
        <div class="filter-row first-row">
          <el-form-item label="课程编号">
            <el-input
              v-model="filters.courseCode"
              placeholder="输入课程编号"
              clearable
              @clear="fetchTasks"
              style="width: 180px"
            />
          </el-form-item>

          <el-form-item label="课程名称">
            <el-input
              v-model="filters.courseName"
              placeholder="输入课程名称"
              clearable
              @clear="fetchTasks"
              style="width: 300px"
            />
          </el-form-item>

          <el-form-item label="学期">
            <el-select
              v-model="filters.term"
              placeholder="选择学期"
              clearable
              @change="fetchTasks"
              style="width: 200px"
            >
              <el-option v-for="term in termOptions" :key="term" :label="term" :value="term" />
            </el-select>
          </el-form-item>
        </div>

        <!-- Second row -->
        <div class="filter-row second-row">
          <div class="left-filters">
            <el-form-item label="任务类型" style="margin-right: 15px">
              <el-select
                v-model="filters.taskType"
                placeholder="选择任务类型"
                clearable
                @change="fetchTasks"
                style="width: 140px"
              >
                <el-option
                  v-for="type in taskTypeOptions"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="提交状态">
              <el-select
                v-model="filters.submitted"
                placeholder="选择提交状态"
                clearable
                @change="fetchTasks"
                style="width: 140px"
              >
                <el-option
                  v-for="status in submittedOptions"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="right-actions">
            <el-form-item>
              <el-button type="primary" @click="fetchTasks">搜索</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 任务列表 -->
    <div class="task-list">
      <el-table :data="tasks" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="任务标题" width="180" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag
              :type="
                row.type === 'HOMEWORK' ? 'primary' : row.type === 'REPORT' ? 'success' : 'warning'
              "
            >
              {{ row.type === 'HOMEWORK' ? '作业' : row.type === 'REPORT' ? '报告' : '测验' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" width="220">
          <template #default="{ row }">
            {{ row.courseName || `课程ID: ${row.courseId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="截止时间" width="180">
          <template #default="{ row }">
            {{ row.dueDate ? formatDate(row.dueDate) : '无截止时间' }}
          </template>
        </el-table-column>
        <el-table-column label="是否过期" width="120">
          <template #default="{ row }">
            <el-tag :type="isExpired(row.dueDate) ? 'danger' : 'success'">
              {{ isExpired(row.dueDate) ? '已过期' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.submitted ? 'success' : 'info'">
              {{ row.submitted ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="enterTaskDetail(row)">进入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.task-list-container {
  padding: 20px;
}

.filter-section {
  background-color: #f8f8f8;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
}

.first-row {
  gap: 15px;
}

.second-row {
  justify-content: space-between;
  align-items: center;
}

.left-filters {
  display: flex;
  gap: 15px;
  align-items: center;
}

.right-actions {
  display: flex;
}

.task-list {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.el-tag {
  margin-right: 5px;
}

.el-form-item {
  margin-bottom: 0;
  display: flex;
  align-items: center;
}

.el-form-item__label {
  width: auto !important;
  padding-right: 10px;
  text-align: right;
}

.el-select {
  margin-right: 0;
}
</style>
