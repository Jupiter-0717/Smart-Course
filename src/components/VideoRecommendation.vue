<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useVideoRecommendationStore } from '@/stores/videoRecommendation'
import type { RecommendationType } from '@/types/video'

const props = defineProps<{
  studentId: number
}>()

const store = useVideoRecommendationStore()
const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
// 推荐类型标签映射
const typeLabels = {
  personalized: '个性化推荐',
  replay: '回放推荐'
}

// 组件加载时获取数据
onMounted(() => {
  store.fetchRecommendations(props.studentId)
})

// 计算属性：是否有推荐数据
const hasRecommendations = computed(() => {
  return store.recommendations.personalized.length > 0 || 
         store.recommendations.replay.length > 0
})
</script>

<template>
  <div class="video-recommendations">
    <!-- 加载状态 -->
    <el-skeleton :rows="5" animated v-if="store.loading" />
    
    <!-- 错误提示 -->
    <el-alert 
      v-if="store.error"
      :title="store.error"
      type="error"
      show-icon
      class="mb-4"
    />
    
    <!-- 推荐内容 -->
    <template v-if="hasRecommendations">
      <!-- 遍历两种推荐类型 -->
      <div 
        v-for="type in Object.keys(store.recommendations) as RecommendationType[]"
        :key="type"
        class="recommendation-section"
        v-show="store.recommendations[type].length > 0"
      >
        <h2 class="section-title">{{ typeLabels[type] }}</h2>
        
        <el-row :gutter="20">
          <el-col 
            v-for="video in store.getRecommendationsByType(type)"
            :key="video.resourceId"
            :xs="24" :sm="12" :md="8"
          >
            <el-card 
              class="video-card"
              shadow="hover"
              @click="$router.push(`/videos/${video.resourceId}`)"
            >
  <div class="video-thumbnail">
    <img :src="`/thumbnails/${video.resourceId}.jpg`" alt="视频缩略图">
    <span class="duration-badge">
      {{ video.durationSeconds ? formatDuration(video.durationSeconds) : '--:--' }}
    </span>
  </div>
              
              <div class="video-info">
                <h3 class="video-title">{{ video.title }}</h3>
                
                <div class="recommendation-reason">
                  <el-tag 
                    :type="type === 'personalized' ? 'success' : 'info'"
                    size="small"
                  >
                    {{ video.recommendationReason }}
                  </el-tag>
                </div>
                
                <div 
                  v-if="type === 'personalized' && video.completionRate > 0"
                  class="completion-rate"
                >
                  <el-progress 
                    :percentage="Math.round(video.completionRate * 100)"
                    :stroke-width="8"
                    :show-text="false"
                  />
                  <span>已观看 {{ Math.round(video.completionRate * 100) }}%</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </template>
    
    <!-- 无数据提示 -->
    <el-empty 
      v-else-if="!store.loading"
      description="暂无视频推荐"
    />
  </div>
</template>

<style scoped>
.video-recommendations {
  padding: 20px;
}

.section-title {
  font-size: 18px;
  margin: 0 0 16px 8px;
  color: var(--el-text-color-primary);
}

.video-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.video-card:hover {
  transform: translateY(-5px);
}

.video-thumbnail {
  position: relative;
  height: 160px;
  overflow: hidden;
  border-radius: 4px;
  background-color: #f5f7fa;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.duration-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 3px;
  font-size: 12px;
}

.video-info {
  padding: 12px 0 0;
}

.video-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommendation-reason {
  margin: 8px 0;
}

.completion-rate {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.completion-rate span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>