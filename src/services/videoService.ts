import axios from 'axios'
import type { VideoRecommendation } from '@/types/video'

const API_BASE = import.meta.env.VITE_API_BASE_URL

export const VideoRecommendationService = {
  // 获取个性化推荐
  async getPersonalizedRecommendations(studentId: number): Promise<VideoRecommendation[]> {
    const response = await axios.get<VideoRecommendation[]>(
      `${API_BASE}/api/recommendations/videos/personalized/${studentId}`
    )
    return response.data
  },

  // 获取回放推荐
  async getReplayRecommendations(studentId: number): Promise<VideoRecommendation[]> {
    const response = await axios.get<VideoRecommendation[]>(
      `${API_BASE}/api/recommendations/videos/replay/${studentId}`
    )
    return response.data
  }
}