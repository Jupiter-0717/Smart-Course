<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLearnStore } from '@/stores/learn'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import type { ResourceType, ClassResource } from '@/types/class_resource'
import axios from 'axios';
import { useRouter } from 'vue-router'
import { useVideoEventStore } from '@/stores/videoEvents'
import type { VideoEventType } from '@/types/videoEvents'
const videoEventStore = useVideoEventStore()
const route = useRoute()
const authStore = useAuthStore()
const learnStore = useLearnStore()
const { user } = storeToRefs(authStore)
const { resources, loading, error } = storeToRefs(learnStore)
const router=useRouter()
// 当前课程ID
const courseId = computed(() => Number(route.params.courseId))

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const resourceTypes = ref<ResourceType[]>(['video','doc','pdf','ppt'])
const activeType = ref<ResourceType>('video')
const videoErrors = ref<Record<number, string>>({})

// 视频错误处理
const handleVideoError = (resource: ClassResource, event: Event) => {
  videoErrors.value[resource.resourceId] = "无法加载视频资源"
  console.error("视频加载错误:", resource.url, event)
}
// 生成唯一会话ID
const generateSessionId = () => {
  return Math.random().toString(36).substring(2) + Date.now().toString(36)
}

// 当前会话ID
const sessionId = ref(generateSessionId())
const lastEventTime = ref(0)
const eventQueue = ref<Promise<any>[]>([]) // 事件队列用于批量处理
const DEBOUNCE_DELAY = 2000 // 2秒防抖

// 发送视频事件到后端（带防抖和队列处理）
const sendVideoEvent = async (
  resource: ClassResource,
  eventType: VideoEventType,
  currentTime: number
) => {
  try {
        // 检查用户是否已登录且有studentId
    if (!user.value?.studentId) {
      throw new Error('用户未登录或未关联学生ID')
    }
    const eventPromise = videoEventStore.recordEvent({
      studentId:user.value.studentId,
      resourceId: resource.resourceId,
      eventType,
      videoTimestamp: Math.floor(currentTime),
      sessionId: sessionId.value,
      duration: resource.duration || 0
    })

    // 加入队列并等待完成
    eventQueue.value.push(eventPromise)
    await Promise.allSettled(eventQueue.value)
    eventQueue.value = []
  } catch (error) {
    console.error('发送视频事件失败:', error)
  }
}

// 防抖函数
const debounce = <T extends (...args: any[]) => void>(fn: T, delay: number) => {
  let timer: ReturnType<typeof setTimeout>
  return (...args: Parameters<T>) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

// 防抖处理的事件发送
const debouncedSendEvent = debounce(sendVideoEvent, DEBOUNCE_DELAY)

// 视频事件处理函数
const handleVideoPlay = (resource: ClassResource, event: Event) => {
  const video = event.target as HTMLVideoElement
  debouncedSendEvent(resource, 'PLAY', video.currentTime)
}

const handleVideoPause = (resource: ClassResource, event: Event) => {
  const video = event.target as HTMLVideoElement
  debouncedSendEvent(resource, 'PAUSE', video.currentTime)
}

const handleVideoSeeked = (resource: ClassResource, event: Event) => {
  const video = event.target as HTMLVideoElement
  debouncedSendEvent(resource, 'SEEK', video.currentTime)
}

const handleVideoEnded = (resource: ClassResource, event: Event) => {
  const video = event.target as HTMLVideoElement
  debouncedSendEvent(resource, 'ENDED', video.currentTime)
}

// 页面卸载时发送暂停事件
onUnmounted(() => {
  document.querySelectorAll('video').forEach(video => {
    if (!video.paused) {
      const resourceId = video.dataset.resourceId
      const resource = currentResources.value.find(r => r.resourceId.toString() === resourceId)
      if (resource) {
        sendVideoEvent(resource, 'PAUSE', video.currentTime)
      }
    }
  })
})

const currentResources = ref<ClassResource[]>([])

// 修改 loadResources 方法
const loadResources = async () => {
  if (!user.value?.studentId) return
  await learnStore.fetchResourceByTypeAndCourse(
    user.value.studentId,
    courseId.value,
    activeType.value
  )
  // 手动更新资源
  currentResources.value = processResources(learnStore.getResourceByType(activeType.value))
}



// 切换资源类型
const switchType = (type: ResourceType) => {
  activeType.value = type
  loadResources()
}

// 初始化加载
onMounted(() => {
  loadResources()
})

// 计算当前类型的资源
const processResources = (resources: ClassResource[] | undefined) => {
  return (resources || []).map(resource => {
    let resourceUrl = resource.url

    if(resource.type === 'video') {
      resourceUrl = resourceUrl
        .replace(/\/+/g, '/')
        .replace(/^\/?/, '/')

      try {
        resourceUrl = resourceUrl.split('/')
          .map(part => part === '' ? '' : encodeURIComponent(part))
          .join('/')
      } catch (e) {
        console.error('URL编码失败:', e)
      }

      if (!resourceUrl.startsWith('http')) {
        resourceUrl = `${API_BASE_URL}${resourceUrl}`
      }
    }

    return {
      ...resource,
      url: resourceUrl
    }
  })
}

//retryLoad 方法，加载视频用的
const retryLoad = (resource: ClassResource) => {
  delete videoErrors.value[resource.resourceId]
  currentResources.value = processResources([...currentResources.value])
}
//下载其他资源用的
const handleDownloadResource = async (resource: ClassResource) => {
  // 如果是视频资源，直接使用video标签播放，不下载
  if (resource.type === 'video') {
    return;
  }

  const fileUrl = resource.url.startsWith('http')
    ? resource.url
    : `${API_BASE_URL}${resource.url.startsWith('/') ? '' : '/'}${resource.url}`;

  const fileName = `${resource.name}${getFileExtension(resource.type)}`;

  try {
    const response = await axios.get(fileUrl, {
      responseType: 'blob',
      headers: {
        'Cache-Control': 'no-cache',
      }
    });

    // 从响应头获取正确的Content-Type，处理拼写错误
    const contentType = response.headers['content-type'] ||
                       response.headers['Content-Type'] ||
                       getMimeType(resource.type);

    const blob = new Blob([response.data], { type: contentType });
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    link.style.display = 'none';

    document.body.appendChild(link);
    link.click();

    // 清理
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);

  } catch (error) {
    console.error('下载失败:', error);
    // 可以添加用户提示

  }
}

// 根据资源类型获取文件扩展名
const getFileExtension = (type: ResourceType) => {
  switch(type) {
    case 'pdf': return '.pdf';
    case 'doc': return '.docx';
    case 'ppt': return '.ppt';
    default: return '';
  }
}

// 根据资源类型获取MIME类型
const getMimeType = (type: ResourceType) => {
  switch(type) {
    case 'pdf': return 'application/pdf';
    case 'doc': return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
    case 'ppt': return 'application/vnd.ms-powerpoint';
    default: return 'application/octet-stream';
  }
}
// 返回课程列表
const goBack = () => {
  router.go(-1)
}

// 添加的工具方法
const getTabIcon = (type: ResourceType) => {
  switch(type) {
    case 'video': return 'el-icon-video-camera';
    case 'doc': return 'el-icon-document';
    case 'ppt': return 'el-icon-data-board';
    case 'pdf': return 'el-icon-tickets';
    default: return 'el-icon-folder';
  }
}

const getTabName = (type: ResourceType) => {
  switch(type) {
    case 'video': return '视频';
    case 'doc': return '文档';
    case 'ppt': return '课件PPT';
    case 'pdf': return '课件PDF';
    default: return '其他';
  }
}

const getDocIcon = (type: ResourceType) => {
  switch(type) {
    case 'doc': return 'el-icon-document';
    case 'ppt': return 'el-icon-data-board';
    case 'pdf': return 'el-icon-tickets';
    default: return 'el-icon-folder';
  }
}

// ...原有代码保持不变...
</script>



<template>
  <div class="course-container">
    <!-- 顶部导航 -->
    <div class="course-header">
      <el-button class="back-btn" @click="goBack" plain>
        <i class="el-icon-arrow-left"></i>
        返回课程列表
      </el-button>
      <h1 class="course-title">智能学习平台</h1>
    </div>

    <!-- 资源类型标签 -->
    <div class="resource-tabs">
      <el-radio-group v-model="activeType" @change="switchType">
        <el-radio-button
          v-for="type in resourceTypes"
          :key="type"
          :label="type"
        >
          <i :class="getTabIcon(type)" class="tab-icon"></i>
          {{ getTabName(type) }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <el-alert :title="error" type="error" show-icon />
      </div>

      <!-- 资源展示区 -->
      <div v-else class="resource-content">
        <!-- 视频资源特殊处理 -->
        <template v-if="activeType === 'video'">
          <div v-if="currentResources.length > 0" class="video-grid">
            <div
              v-for="resource in currentResources"
              :key="resource.resourceId"
              class="video-card"
            >
              <div class="video-header">
                <h3 class="video-title">{{ resource.name }}</h3>
              </div>

              <div class="video-wrapper">
                <video
                  controls
                  :src="resource.url"
                  @error="handleVideoError(resource, $event)"
                  @play="handleVideoPlay(resource, $event)"
                  @pause="handleVideoPause(resource, $event)"
                  @seeked="handleVideoSeeked(resource, $event)"
                  @ended="handleVideoEnded(resource, $event)"
                  :data-resource-id="resource.resourceId"
                  class="course-video"
                  crossorigin="anonymous"
                />
              </div>

              <div v-if="videoErrors[resource.resourceId]" class="video-error">
                <el-alert :title="videoErrors[resource.resourceId]" type="error" show-icon />
                <el-button @click="retryLoad(resource)" size="small">重试加载</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty description="暂无视频资源" />
          </div>
        </template>

        <!-- 其他类型资源 -->
        <template v-else>
          <div v-if="currentResources.length > 0" class="document-grid">
            <div
              v-for="resource in currentResources"
              :key="resource.resourceId"
              class="document-card"
            >
              <div class="document-icon">
                <i :class="getDocIcon(resource.type)"></i>
              </div>
              <div class="document-info">
                <div class="document-name">{{ resource.name }}</div>
                <el-button
                  @click="handleDownloadResource(resource)"
                  type="primary"
                  size="small"
                  icon="el-icon-download"
                >
                  下载
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty :description="`暂无${getTabName(activeType)}资源`" />
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 基础样式 */
.course-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
  color: #333;
}

/* 顶部导航 */
.course-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.back-btn {
  margin-right: 24px;
  padding: 8px 16px;
  border-radius: 4px;
  color: #409EFF;
  border-color: #409EFF;
}

.back-btn:hover {
  background-color: #ecf5ff;
}

.course-title {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
  color: #303133;
}

/* 资源标签样式 */
.resource-tabs {
  margin-bottom: 24px;
}

.tab-icon {
  margin-right: 6px;
}

/* 主要内容区 */
.main-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
}

/* 视频卡片样式 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.video-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
}

.video-header {
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
}

.video-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

/* 视频播放器样式 */
.video-wrapper {
  position: relative;
  padding-bottom: 56.25%; /* 16:9 比例 */
  height: 0;
  background: #000;
}

.course-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  outline: none;
}

/* 文档卡片样式 */
.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.document-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  border: 1px solid #ebeef5;
}

.document-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.12);
}

.document-icon {
  margin-right: 16px;
  font-size: 32px;
  color: #409EFF;
}

.document-info {
  flex: 1;
}

.document-name {
  font-size: 14px;
  margin-bottom: 8px;
  color: #303133;
  font-weight: 500;
}

/* 空状态样式 */
.empty-state {
  padding: 60px 0;
  text-align: center;
}

/* 错误状态 */
.video-error {
  padding: 16px;
  background: #fef0f0;
  border-top: 1px solid #fde2e2;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .course-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .back-btn {
    margin-right: 0;
    margin-bottom: 12px;
  }

  .video-grid,
  .document-grid {
    grid-template-columns: 1fr;
  }
}
</style>
