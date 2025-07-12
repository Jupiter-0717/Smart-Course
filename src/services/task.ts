import axios from 'axios';

export enum TaskType {
  HOMEWORK = 'HOMEWORK',
  QUIZ = 'QUIZ',
  REPORT = 'REPORT',
}
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
// 题目详情类型
export interface QuizQuestionDetail {
  questionId: number;
  type: string; // 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'FILL_IN_BLANK' | 'SHORT_ANSWER'
  body: string; // JSON格式的题目内容
  difficulty: number;
  score?: number;
  orderIndex?: number;
}

// 试卷详情响应类型
export interface QuizDetailResponse {
  id: number;
  title: string;
  creatorId: number;
  creatorName: string;
  totalPoints: number;
  questions: QuizQuestionDetail[];
}

// 更新 TaskResponse 接口
export interface TaskResponse {
  id: number;
  courseId: number;
  title: string;
  description?: string;
  type: TaskType;
  quizId?: number | null;
  dueDate?: string | null;
  createdAt: string;
  // 新增：当任务类型为QUIZ时，包含试卷详细信息
  quizDetail?: QuizDetailResponse;
}

// Task 请求和响应类型定义（根据你的 DTO 可适当调整）
export interface TaskRequest {
  courseId: number;
  title: string;
  description?: string;
  type: TaskType; // "HOMEWORK" | "QUIZ" | "REPORT"
  quizId?: number | null;
  dueDate?: string | null; // ISO 日期字符串
}

// Task_Resource 响应类型
export interface TaskResourceResponse {
  id: number;
  name: string;
  fileType?: string | null;
  filePath: string;
  uploadDate: string;
  uploaderId?: number | null;
}

const api = axios.create({
  baseURL: '/api', // 后端基础路径，视项目配置调整
});

// === Task 相关 API ===

export function createTask(request: TaskRequest): Promise<TaskResponse> {
  return axios.post(`${API_BASE_URL}/api/tasks`, request).then(res => res.data);
}

// 修改：添加teacherId参数以匹配后端API要求
export function getTasksByCourse(courseId: number, teacherId: number): Promise<TaskResponse[]> {
  console.log(`[API调用] 获取任务: courseId=${courseId}, teacherId=${teacherId}`);
  return axios.get(`${API_BASE_URL}/api/tasks/course/${courseId}`, {
    params: { teacherId }
  }).then(res => {
    console.log('[获取任务成功]', res.data);
    return res.data;
  }).catch(error => {
    console.error('[获取任务失败]', error);
    throw error;
  });
}

export function deleteTask(taskId: number): Promise<void> {
  return axios.delete(`${API_BASE_URL}/api/tasks/${taskId}`).then(() => { });
}

// === TaskResource 相关 API ===

// files: FileList 或 File[]
// export function uploadTaskResources(
//   taskId: number,
//   teacherId: number,
//   files: File[]
// ): Promise<TaskResourceResponse[]> {
//   const formData = new FormData();
//   files.forEach(file => formData.append('files', file));
//   formData.append('teacherId', String(teacherId));

//   return api
//     .post(`/tasks/${taskId}/resources/upload`, formData, {
//       headers: { 'Content-Type': 'multipart/form-data' },
//     })
//     .then(res => res.data);
// }

export function uploadTaskResources(
  taskId: number,
  teacherId: number,
  files: File[]
): Promise<TaskResourceResponse[]> {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
    console.log(`[上传准备] 文件名: ${file.name}, 大小: ${file.size} bytes`);
  });
  formData.append('teacherId', String(teacherId));

  return axios
    .post(`${API_BASE_URL}/api/tasks/${taskId}/resources/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then(res => {
      console.log('[上传成功] 服务器返回：', res.data);
      return res.data;
    })
    .catch(error => {
      console.error('[上传失败] 错误信息：', error);
      throw error;
    });
}

export function getResourcesByTask(taskId: number): Promise<TaskResourceResponse[]> {
  console.log(`[获取资源] 任务 ID: ${taskId}`);
  return axios
    .get(`${API_BASE_URL}/api/tasks/${taskId}/resources`)
    .then(res => {
      console.log('[获取成功] 资源列表：', res.data);
      return res.data;
    })
    .catch(error => {
      console.error('[获取失败] 错误信息：', error);
      throw error;
    });
}

/**
 * 解析题目内容JSON
 */
export function parseQuestionBody(body: string) {
  try {
    return JSON.parse(body);
  } catch (error) {
    console.error('解析题目内容失败:', error);
    return { question: body, options: [], answer: '', explanation: '' };
  }
}

/**
 * 获取题目类型名称
 */
export function getQuestionTypeName(type: string): string {
  const typeMap: Record<string, string> = {
    'SINGLE_CHOICE': '单选题',
    'MULTI_CHOICE': '多选题',
    'FILL_IN_BLANK': '填空题',
    'SHORT_ANSWER': '简答题'
  };
  return typeMap[type] || type;
}

/**
 * 获取难度等级名称
 */
export function getDifficultyName(difficulty: number): string {
  const difficultyMap: Record<number, string> = {
    1: '简单',
    2: '较简单',
    3: '中等',
    4: '较难',
    5: '困难'
  };
  return difficultyMap[difficulty] || `难度${difficulty}`;
}

// 获取教师可用的试卷列表
export function getAvailableQuizzes(teacherId: number, courseId?: number): Promise<QuizResponse[]> {
  if (courseId) {
    // 使用课程ID获取试卷列表
    return axios.post(`${API_BASE_URL}/api/teacher/quiz-v2/by-course`, {
      courseId: courseId
    }).then(res => res.data);
  } else {
    // 获取教师的所有试卷
    return axios.post(`${API_BASE_URL}/api/teacher/quiz-v2/list-by-teacher`, {
      teacherId: teacherId
    }).then(res => res.data);
  }
}

// 获取试卷详情
export function getQuizDetail(quizId: number, teacherId: number): Promise<QuizDetailResponse> {
  return axios.post(`${API_BASE_URL}/api/teacher/quiz-v2/detail`, {
    quizId: quizId,
    teacherId: teacherId
  }).then(res => res.data);
}

// 试卷响应类型
export interface QuizResponse {
  id: number;
  title: string;
  creatorId: number;
  creatorName: string;
  totalPoints: number;
  questionCount: number;
}
