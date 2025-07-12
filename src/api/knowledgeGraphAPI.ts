import axios from 'axios'

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 创建axios实例
const apiClient = axios.create({
  baseURL: API_URL, // 基础URL
})

export default {
  // 创建知识点
  createKnowledgePoint(teacherId: number, data: any) {
    return apiClient.post(`/api/teachers/${teacherId}/knowledge-graph/points`, data)
  },

  // 获取课程知识图谱
  getCourseKnowledgeGraph(teacherId: number, courseId: number) {
    return apiClient.get(`/api/teachers/${teacherId}/knowledge-graph/course/${courseId}`)
  },

  // 更新知识点位置
  updateKnowledgePointPosition(teacherId: number, pointId: number, position: any) {
    return apiClient.put(
      `/api/teachers/${teacherId}/knowledge-graph/points/${pointId}/position`,
      position,
    )
  },

  // 删除知识点
  deleteKnowledgePoint(teacherId: number, pointId: number) {
    return apiClient.delete(`/api/teachers/${teacherId}/knowledge-graph/points/${pointId}`)
  },

  // AI一键生成知识图谱
  generateByAI(teacherId: number, courseId: number) {
    return apiClient.post(`/api/teachers/${teacherId}/knowledge-graph/${courseId}/generate-by-ai`)
  },

  // 获取知识点关联资源
  getPointResources(pointId: number) {
    return apiClient.get(`/api/knowledge-points/${pointId}/resources`)
  },

  // 添加资源关联
  addResourceAssociation(pointId: number, resourceId: number) {
    return apiClient.post(`/api/knowledge-points/${pointId}/resources/${resourceId}`)
  },

  // 移除资源关联
  removeResourceAssociation(pointId: number, resourceId: number) {
    return apiClient.delete(`/api/knowledge-points/${pointId}/resources/${resourceId}`)
  },
}
