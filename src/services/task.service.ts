import { useAuthStore } from '@/stores/auth'
import api from './api.service'
import type {
  TaskResponse,
  TaskDetailResponse,
  SubmitHomeworkRequest,
  SubmitReportRequest,
  TaskResourceResponse,
} from '@/types/task'
import { ref, onMounted } from 'vue'
import { getResourcesByTask } from './task'

// const resources = ref<TaskResourceResponse[]>([])

// const fetchResources = async () => {
//   if (!taskId.value) return
//   try {
//     resources.value = await getResourcesByTask(taskId.value)
//   } catch (e) {
//     ElMessage.error('获取资源失败')
//   }
// }

// 获取学生任务列表
export const getTasksByCourse = async (
  studentId: number,
  courseCode?: string,
  courseName?: string,
  term?: string,
  taskType?: string,
  submitted?: boolean,
): Promise<TaskResponse[]> => {
  const response = await api.get(`/student/${studentId}/tasks`, {
    params: {
      courseCode,
      courseName,
      term,
      taskType,
      submitted,
    },
  })
  return response.data
}

// 更新获取任务详情的方法
export const getTaskDetail = async (
  studentId: number,
  taskId: number,
): Promise<TaskDetailResponse> => {
  const response = await api.get(`/student/${studentId}/tasks/${taskId}`)
  return response.data
}

// 提交作业（文本类型）
export const submitHomework = async (data: SubmitHomeworkRequest): Promise<void> => {
  const formData = new FormData()
  formData.append('content', data.content)

  await api.post(`/student/${data.studentId}/tasks/${data.taskId}/submit`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 提交报告（文件类型）
export const submitReport = async (data: SubmitReportRequest): Promise<void> => {
  const formData = new FormData()

  // 为每个文件添加索引信息
  data.files.forEach((file, index) => {
    formData.append('files', file)
    formData.append('fileIndexes', index.toString())
  })

  await api.post(`/student/${data.studentId}/tasks/${data.taskId}/submit`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export default {
  getTasksByCourse,
  getTaskDetail,
  submitHomework,
  submitReport,
}
