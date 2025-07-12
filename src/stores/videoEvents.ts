import { defineStore } from 'pinia'
import axios from 'axios'
import type { VideoEventRequest } from '@/types/videoEvents'
import { useAuthStore } from './auth'
export const useVideoEventStore = defineStore('videoEvents', {
  actions: {
    async recordEvent(eventData: VideoEventRequest) {
      const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
      try {
        // 添加当前用户ID
        const authStore = useAuthStore()
        const payload = {
          ...eventData,
          studentId: authStore.user?.studentId // 从authStore获取当前用户ID
        }
        
        await axios.post(`${API_BASE_URL}/api/video/events`, payload, {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authStore.token}` // 如果需要认证
          }
        })
      } catch (error) {
        console.error('记录视频事件失败:', error)
        throw error
      }
    }
  }
})