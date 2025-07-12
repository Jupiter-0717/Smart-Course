// 任务类型
export enum TaskType {
  HOMEWORK = 'HOMEWORK',
  QUIZ = 'QUIZ',
  REPORT = 'REPORT',
}

// 任务响应
export interface TaskResponse {
  id: number
  courseId: number
  courseName?: string
  title: string
  description?: string
  type: TaskType
  quizId?: number | null
  dueDate?: string | null
  createdAt: string
  expired: boolean
  submitted: boolean
}

// 任务详情响应
export interface TaskDetailResponse {
  id: number
  title: string
  coursename: string
  description: string
  type: string
  dueDate: string
  expired: boolean
  submitted: boolean
  resources: TaskResourceResponse[]
  grade?: number | null
  feedback?: string | null
}

// 任务资源响应
export interface TaskResourceResponse {
  id: number
  name: string
  fileType?: string | null
  filePath: string
  uploadDate: string
  uploaderId?: number | null
}

// 提交作业请求
export interface SubmitHomeworkRequest {
  taskId: number
  studentId: number
  content: string
}

// 提交报告请求
export interface SubmitReportRequest {
  taskId: number
  studentId: number
  files: File[]
}
