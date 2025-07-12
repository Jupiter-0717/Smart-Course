<!-- src/components/video/VideoRecommendationList.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { VideoRecommendationDTO } from '@/types/video'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  recommendationType: 'personalized' | 'replay'
}>()

const authStore = useAuthStore()
const videos = ref<VideoRecommendationDTO[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// 修改fetchVideos方法，添加数据转换
const fetchVideos = async () => {
  try {
    isLoading.value = true
    error.value = null
    
    const endpoint = props.recommendationType === 'personalized'
      ? `/api/recommendations/videos/personalized/${authStore.user?.studentId}`
      : `/api/recommendations/videos/replay/${authStore.user?.studentId}`
    
    const response = await fetch(endpoint)
    if (!response.ok) throw new Error('获取视频推荐失败')
    
    // 转换后端返回的数据格式
    const backendData = await response.json()
    videos.value = backendData.map((item: any) => ({
      videoId: item.resourceId,  // 关键修改：将resourceId映射为videoId
      title: item.title,
      durationSeconds: item.durationSeconds,
      description: item.description,
      recommendationReason: item.recommendationReason
    }))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取视频推荐时出错'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  if (authStore.user?.studentId) {
    fetchVideos()
  }
})

const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>
<template>
  <div class="video-recommendations">
    <h3 class="section-title">
      <span class="title-icon">{{ recommendationType === 'personalized' ? '🎯' : '🔄' }}</span>
      {{ recommendationType === 'personalized' ? '为你推荐的视频' : '你可能想复习的视频' }}
    </h3>
    
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载推荐视频...</p>
    </div>
    
    <div v-else-if="error" class="error-state">
      <span class="error-icon">⚠️</span>
      <p>{{ error }}</p>
    </div>
    
    <div v-else-if="videos.length === 0" class="empty-state">
      <span class="empty-icon">📽️</span>
      <p>暂无推荐视频</p>
    </div>
    
    <div v-else class="video-grid">
      <div 
        v-for="video in videos" 
        :key="video.videoId" 
        class="video-card"
        @click="$router.push(`/video/${video.videoId}`)"
      >
        <div class="video-thumbnail">
          <div class="play-overlay">
            <div class="play-icon">▶</div>
          </div>
          <div class="duration-badge">{{ formatDuration(video.durationSeconds) }}</div>
        </div>
        
        <div class="video-content">
          <h4 class="video-title">{{ video.title }}</h4>
          
          <div v-if="video.title" class="video-description">
            {{ video.title }}
          </div>
          
          <div v-if="video.recommendationReason" class="recommendation-reason">
            <span class="reason-icon">💡</span> {{ video.recommendationReason }}
          </div>
          
          <div class="video-footer">
            <span class="watch-action">点击观看</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.video-recommendations {
  margin: 2rem 0;
  padding: 1.5rem;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.05);
}

.section-title {
  color: #2c3e50;
  margin-bottom: 1.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  
  .title-icon {
    margin-right: 10px;
    font-size: 1.2em;
  }
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.video-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #eaeaea;
  position: relative;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
    border-color: #3498db;
    
    .play-icon {
      transform: scale(1.1);
    }
    
    .watch-action {
      color: #3498db;
    }
  }
}

.video-thumbnail {
  position: relative;
  height: 160px;
  background: linear-gradient(135deg, #f5f7fa, #e4e8eb);
  display: flex;
  align-items: center;
  justify-content: center;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.1);
    transition: background 0.2s ease;
  }
  
  &:hover::before {
    background: rgba(0, 0, 0, 0.2);
  }
}

.play-overlay {
  position: absolute;
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.play-icon {
  font-size: 1.2rem;
  color: #2c3e50;
  transition: transform 0.2s ease;
}

.duration-badge {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  z-index: 1;
}

.video-content {
  padding: 1.2rem;
}

.video-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.8rem;
  font-size: 1rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-description {
  font-size: 0.85rem;
  color: #7f8c8d;
  margin-bottom: 0.8rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommendation-reason {
  font-size: 0.8rem;
  color: #3498db;
  background: #f0f7ff;
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  
  .reason-icon {
    margin-right: 5px;
  }
}

.video-footer {
  display: flex;
  justify-content: flex-end;
}

.watch-action {
  font-size: 0.85rem;
  color: #7f8c8d;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  
  &::after {
    content: '→';
    margin-left: 4px;
  }
}

.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 2rem;
  color: #7f8c8d;
  
  p {
    margin-top: 1rem;
  }
}

.spinner {
  border: 3px solid rgba(52, 152, 219, 0.1);
  border-top: 3px solid #3498db;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

.error-icon {
  font-size: 1.8rem;
  color: #e74c3c;
  display: inline-block;
  margin-bottom: 0.5rem;
}

.empty-icon {
  font-size: 2rem;
  color: #bdc3c7;
  display: inline-block;
  margin-bottom: 0.5rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>