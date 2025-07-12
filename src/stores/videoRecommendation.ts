import { defineStore } from 'pinia'
import { VideoRecommendationService } from '@/services/videoService'
import type { VideoRecommendation, RecommendationType } from '@/types/video'

interface State {
  recommendations: {
    personalized: VideoRecommendation[]
    replay: VideoRecommendation[]
  }
  loading: boolean
  error: string | null
}

export const useVideoRecommendationStore = defineStore('videoRecommendations', {
  state: (): State => ({
    recommendations: {
      personalized: [],
      replay: []
    },
    loading: false,
    error: null
  }),
  actions: {
    // 获取推荐视频
    async fetchRecommendations(studentId: number) {
      this.loading = true
      this.error = null
      
      try {
        const [personalized, replay] = await Promise.all([
          VideoRecommendationService.getPersonalizedRecommendations(studentId),
          VideoRecommendationService.getReplayRecommendations(studentId)
        ])
        
        this.recommendations = { personalized, replay }
      } catch (err) {
        this.error = '获取推荐视频失败'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    
    // 按类型获取推荐
    getRecommendationsByType(type: RecommendationType): VideoRecommendation[] {
      return this.recommendations[type]
    }
  }
})

