// 该文件用于试卷与题库功能--曹雨荷

import api from './api.service'
import { TEACHER_ID } from '@/config/teacher' // 全局teacherId--曹雨荷
import type {
  CreateQuestionRequest,
  SearchQuestionRequest,
  QuestionDetailRequest,
  UpdateQuestionRequest,
  DeleteQuestionRequest,
  BatchDeleteQuestionRequest,
  ByCourseQuestionRequest,
  SearchByKeywordRequest,
  QuestionResponse,
  CreateQuestionRequestInput,
  SearchQuestionRequestInput,
  QuestionDetailRequestInput,
  UpdateQuestionRequestInput,
  DeleteQuestionRequestInput,
  BatchDeleteQuestionRequestInput,
  ByCourseQuestionRequestInput,
  SearchByKeywordRequestInput,
} from '@/types/question'

const BASE_URL = '/teacher/question-bank-v2'

export const questionBankService = {
  // 创建题目 - 该接口用于试卷与题库功能--曹雨荷
  async createQuestion(request: CreateQuestionRequestInput): Promise<QuestionResponse> {
    // 确保使用全局teacherId
    const payload: CreateQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/create`, payload)
  },

  // 筛选题目 - 该接口用于试卷与题库功能--曹雨荷
  async searchQuestions(
    request: SearchQuestionRequestInput,
  ): Promise<{ content: QuestionResponse[]; totalPages: number }> {
    // 确保使用全局teacherId
    const payload: SearchQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    const res = await api.post(`${BASE_URL}/search`, payload)
    console.log('searchQuestions实际返回：', res)
    // 如果res.data才是后端数据，返回res.data
    return res.data ? res.data : res
  },

  // 题目详情 - 该接口用于试卷与题库功能--曹雨荷
  async getQuestionDetail(request: QuestionDetailRequestInput): Promise<QuestionResponse> {
    // 确保使用全局teacherId
    const payload: QuestionDetailRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/detail`, payload)
  },

  // 编辑题目 - 该接口用于试卷与题库功能--曹雨荷
  async updateQuestion(request: UpdateQuestionRequestInput): Promise<QuestionResponse> {
    // 确保使用全局teacherId
    const payload: UpdateQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/update`, payload)
  },

  // 删除题目 - 该接口用于试卷与题库功能--曹雨荷
  async deleteQuestion(request: DeleteQuestionRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: DeleteQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/delete`, payload)
  },

  // 批量删除 - 该接口用于试卷与题库功能--曹雨荷
  async batchDeleteQuestions(request: BatchDeleteQuestionRequestInput): Promise<void> {
    // 确保使用全局teacherId
    const payload: BatchDeleteQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    return api.post(`${BASE_URL}/batch-delete`, payload)
  },

  // 按课程获取题目 - 该接口用于试卷与题库功能--曹雨荷
  async getQuestionsByCourse(request: ByCourseQuestionRequestInput): Promise<QuestionResponse[]> {
    // 确保使用全局teacherId
    const payload: ByCourseQuestionRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    const res = await api.post(`${BASE_URL}/by-course`, payload)
    return res.data ? res.data : res
  },

  // 关键词搜索 - 该接口用于试卷与题库功能--曹雨荷
  async searchByKeyword(
    request: SearchByKeywordRequestInput,
  ): Promise<{ content: QuestionResponse[] }> {
    // 确保使用全局teacherId
    const payload: SearchByKeywordRequest = {
      ...request,
      teacherId: TEACHER_ID, // 全局teacherId--曹雨荷
    }
    const res = await api.post(`${BASE_URL}/search-by-keyword`, payload)
    return res.data // 只返回题目数组
  },

  async searchQuestionsV2(request: any) {
    // V2题目搜索接口
    const res = await api.post('/teacher/question-bank-v2/search', request)
    return res.data
  },
}
