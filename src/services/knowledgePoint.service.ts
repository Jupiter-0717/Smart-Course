// 该文件用于试卷与题库功能--曹雨荷

import api from './api.service'
import type { KnowledgePointResponse } from '@/types/knowledgePoint'

const BASE_URL = '/teacher/knowledge-point'

export const knowledgePointService = {
  // 按课程查知识点 - 该接口用于试卷与题库功能--曹雨荷
  async getKnowledgePointsByCourse(courseId: number): Promise<{ data: KnowledgePointResponse[] }> {
    return api.post(`${BASE_URL}/by-course`, { courseId })
  },

  async searchKnowledgePointsByName(courseId: number, name: string) {
    const res = await api.post('/teacher/knowledge-point/search-by-name', { courseId, name })
    return res.data
  },
}
