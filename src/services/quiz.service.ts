// 该文件用于试卷与题库功能--曹雨荷

import api from './api.service'
import { TEACHER_ID } from '@/config/teacher' // 全局teacherId--曹雨荷
import type {
  CreateQuizRequest,
  QuizDetailRequest,
  UpdateQuizRequest,
  DeleteQuizRequest,
  ListByTeacherRequest,
  AddQuestionToQuizRequest,
  RemoveQuestionFromQuizRequest,
  SetQuestionSettingsRequest,
  CopyQuizRequest,
  ByCourseQuizRequest,
  QuizResponse,
  QuizDetailResponse,
  QuizDetailRequestInput,
  UpdateQuizRequestInput,
  DeleteQuizRequestInput,
  AddQuestionToQuizRequestInput,
  RemoveQuestionFromQuizRequestInput,
  SetQuestionSettingsRequestInput,
  CopyQuizRequestInput,
  ReplaceQuestionRequest,
  BatchSetQuestionSettingsRequest,
  CreateFullQuizRequest,
  CreateFullQuizRequestInput,
} from '@/types/quiz'

const BASE_URL = '/teacher/quiz-v2'

export const quizService = {
  // 创建试卷 - 该接口用于试卷与题库功能--曹雨荷
  async createQuiz(payload: {
    title: string
    creatorId: number
    courseId: number
    totalPoints: number
  }) {
    return api.post('/teacher/quiz/quizzes', payload)
  },

  // 创建完整试卷 - 该接口用于试卷与题库功能--曹雨荷
  async createFullQuiz(request: CreateFullQuizRequestInput): Promise<QuizResponse> {
    // 确保使用全局teacherId
    const payload: CreateFullQuizRequest = {
      ...request,
      data: {
        ...request.data,
        creatorId: TEACHER_ID, // 全局teacherId--曹雨荷
      },
    }
    return api.post(`${BASE_URL}/create-full`, payload)
  },

  // 试卷详情 - 该接口用于试卷与题库功能--曹雨荷
  async getQuizDetail(request: QuizDetailRequestInput): Promise<{ data: QuizDetailResponse }> {
    // 确保使用全局teacherId
    const payload: QuizDetailRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/detail`, payload)
  },

  // 编辑试卷 - 该接口用于试卷与题库功能--曹雨荷
  async updateQuiz(request: UpdateQuizRequestInput): Promise<QuizResponse> {
    // 确保使用全局teacherId
    const payload: UpdateQuizRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/update`, payload)
  },

  // 删除试卷 - 该接口用于试卷与题库功能--曹雨荷
  async deleteQuiz(request: DeleteQuizRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: DeleteQuizRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/delete`, payload)
  },

  // 教师试卷列表 - 该接口用于试卷与题库功能--曹雨荷
  async getQuizListByTeacher(): Promise<{ data: QuizResponse[] }> {
    const payload: ListByTeacherRequest = {
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/list-by-teacher`, payload)
  },

  // 添加题目到试卷 - 该接口用于试卷与题库功能--曹雨荷
  async addQuestionToQuiz(request: AddQuestionToQuizRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: AddQuestionToQuizRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/add-question`, payload)
  },

  // 移除题目 - 该接口用于试卷与题库功能--曹雨荷
  async removeQuestionFromQuiz(request: RemoveQuestionFromQuizRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: RemoveQuestionFromQuizRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/remove-question`, payload)
  },

  // 设置分数顺序 - 该接口用于试卷与题库功能--曹雨荷
  async setQuestionSettings(request: SetQuestionSettingsRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: SetQuestionSettingsRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/set-question-settings`, payload)
  },

  // 复制试卷 - 该接口用于试卷与题库功能--曹雨荷
  async copyQuiz(request: CopyQuizRequestInput): Promise<QuizResponse> {
    // 确保使用全局teacherId
    const payload: CopyQuizRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/copy`, payload)
  },

  // 按课程查试卷 - 该接口用于试卷与题库功能--曹雨荷
  async getQuizByCourse(request: ByCourseQuizRequest): Promise<{ data: QuizResponse[] }> {
    return api.post(`${BASE_URL}/by-course`, request)
  },

  // 替换题目--该接口用于试卷与题库功能--曹雨荷
  async replaceQuestion(request: ReplaceQuestionRequest): Promise<void> {
    const payload = { ...request, teacherId: TEACHER_ID }
    return api.post(`${BASE_URL}/replace-question`, payload)
  },

  // 批量设置分数顺序--该接口用于试卷与题库功能--曹雨荷
  async batchSetQuestionSettings(request: BatchSetQuestionSettingsRequest): Promise<void> {
    const payload = { ...request, teacherId: TEACHER_ID }
    return api.post(`${BASE_URL}/batch-set-question-settings`, payload)
  },
}
