<!-- src/views/VideoDetailView.vue -->
<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import type { VideoDetailDTO } from '@/types/video'

const route = useRoute()
const video = ref<VideoDetailDTO | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 修改fetchVideoDetails方法
const fetchVideoDetails = async () => {
  try {
    const videoId = route.params.videoId
    if (!videoId || videoId === 'undefined' || isNaN(Number(videoId))) {
      throw new Error('视频ID格式不正确')
    }

    isLoading.value = true
    error.value = null
    
    console.log('正在请求视频详情，videoId:', videoId)
    const response = await fetch(`${API_BASE_URL}/api/videos/${videoId}`)
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new Error(errorData.message || '获取视频详情失败')
    }
    
    const backendData = await response.json()
    video.value = {
      videoId: backendData.videoId || backendData.resourceId,
      title: backendData.title,
      url: backendData.url,
      durationSeconds: backendData.durationSeconds,
      description: backendData.description
    }
    
    console.log('获取视频详情成功:', {
      originalUrl: backendData.url,
      safeUrl: safeVideoUrl.value
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取视频详情时出错'
    console.error('视频加载错误:', err)
  } finally {
    isLoading.value = false
  }
}

const safeVideoUrl = computed(() => {
  if (!video.value?.url) return '';

  try {
    // 1. 获取原始URL并清理
    let url = video.value.url.trim().replace(/[\n\r]+/g, '');
    
    // 2. 如果是本地路径，直接使用（不编码）
    if (url.startsWith('/uploads/')) {
      // 确保路径格式正确
      return `${API_BASE_URL}${url}`;
    }
    
    // 3. 其他情况（远程URL）才进行编码
    const urlObj = new URL(url, API_BASE_URL);
    urlObj.pathname = urlObj.pathname
      .split('/')
      .map(segment => segment === '' ? '' : encodeURIComponent(segment))
      .join('/');
    
    return urlObj.toString();
  } catch (e) {
    console.error('URL处理失败:', e);
    return '';
  }
});

// 视频错误处理
const handleVideoError = (event: Event) => {
  const videoEl = event.target as HTMLVideoElement
  console.error('视频加载失败:', {
    src: videoEl.src,
    error: videoEl.error,
    networkState: ['EMPTY', 'IDLE', 'LOADING', 'NO_SOURCE'][videoEl.networkState]
  })
  error.value = '视频加载失败，请检查网络或联系管理员'
}

// 监听路由参数变化
watch(() => route.params.videoId, () => {
  if (route.params.videoId) {
    fetchVideoDetails()
  }
})

// 初始化加载
onMounted(() => {
  if (route.params.videoId) {
    fetchVideoDetails()
  } else {
    error.value = '缺少视频ID参数'
  }
})

// 格式化时长显示
const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}分${secs.toString().padStart(2, '0')}秒`
}
</script>

<template>
  <div class="video-detail-container">
    <div v-if="isLoading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    
    <div v-else-if="video" class="video-detail">
      <div class="video-player">
        <video
          controls
          :src="safeVideoUrl"
          class="video-element"
          @error="handleVideoError"
          v-if="safeVideoUrl"
        >
          <source :src="safeVideoUrl" type="video/mp4">
          您的浏览器不支持视频播放
        </video>
        
        <div v-if="!safeVideoUrl" class="debug-info">
          视频URL无效，无法播放
        </div>
      </div>
      
      <div class="video-meta">
        <h1 class="title">{{ video.title }}</h1>
        <div class="duration">{{ formatDuration(video.durationSeconds) }}</div>
        
        <div class="description-section">
          <h3>视频描述</h3>
          <p>{{ video.description || '暂无描述' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .error {
  padding: 40px;
  text-align: center;
  font-size: 1.2rem;
}

.error {
  color: #f56c6c;
}

.video-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.video-player {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-element {
  width: 100%;
  display: block;
  max-height: 70vh;
}

.debug-info {
  padding: 20px;
  background: #ffeeee;
  color: #f56c6c;
  text-align: center;
}

.video-meta {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  color: #333;
}

.duration {
  color: #666;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.description-section {
  margin-top: 1.5rem;
}

.description-section h3 {
  margin: 0 0 0.8rem;
  font-size: 1.2rem;
  color: #333;
}

.description-section p {
  color: #555;
  line-height: 1.6;
  margin: 0;
}
</style>