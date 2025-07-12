<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useLearnStore } from '@/stores/learn'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import { useVideoAnalyticsStore } from '@/stores/videoAnalytics'
import type { ResourceType, ClassResource } from '@/types/class_resource'
import axios from 'axios';
import { useRouter } from 'vue-router'

const route = useRoute()
const authStore = useAuthStore()
const learnStore = useLearnStore()
const { user } = storeToRefs(authStore)
const { resources, loading, error } = storeToRefs(learnStore)
const router = useRouter()
const videoAnalytics = useVideoAnalyticsStore()

// 计算属性获取 store 中的状态
const teacherAnalyticsLoading = computed(() => videoAnalytics.loading)
const teacherAnalyticsData = computed(() => videoAnalytics.teacherAnalyticsData)

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

const currentResources = ref<ClassResource[]>([])

// 修改 loadResources 方法
const loadResources = async () => {
  if (!user.value?.id) return
  await learnStore.fetchResourceByTeacher(
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

// 获取教师分析数据
const fetchTeacherAnalytics = async (resourceId: number) => {
  try {
    await videoAnalytics.fetchTeacherAnalytics(resourceId)
  } catch (error) {
    console.error('获取分析数据失败:', error)
  }
}

// 重试加载视频
const retryLoad = (resource: ClassResource) => {
  delete videoErrors.value[resource.resourceId]
  currentResources.value = processResources([...currentResources.value])
}

// 下载其他资源
const handleDownloadResource = async (resource: ClassResource) => {
  if (resource.type === 'video') return;

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
    
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);

  } catch (error) {
    console.error('下载失败:', error);
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
// 格式化时间显示 (秒 → MM:SS)
const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 计算热播条宽度 (基于回放次数)
const calculateWidth = (replayCount: number) => {
    const maxReplay = Math.max(...(teacherAnalyticsData.value?.hotSpots?.map(s => s.replayCount) || [1]))
    return `${(replayCount / maxReplay) * 100}%`
}
// 获取资源类型图标
const getResourceIcon = (type: ResourceType) => {
  switch(type) {
    case 'video': return 'el-icon-video-camera';
    case 'doc': return 'el-icon-document';
    case 'ppt': return 'el-icon-data-board';
    case 'pdf': return 'el-icon-tickets';
    default: return 'el-icon-folder';
  }
}

// 获取资源类型名称
const getResourceTypeName = (type: ResourceType) => {
  switch(type) {
    case 'video': return '视频';
    case 'doc': return '文档';
    case 'ppt': return '课件PPT';
    case 'pdf': return '课件PDF';
    default: return '';
  }
}

// 计算最大回放次数
const maxReplayCount = computed(() => {
  if (!teacherAnalyticsData.value?.hotSpots?.length) return 1;
  return Math.max(...teacherAnalyticsData.value.hotSpots.map(s => s.replayCount));
});
</script>

<template>
  <div class="course-detail-container">
    <!-- 返回按钮和标题 -->
    <div class="header">
      <el-button class="back-btn" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        返回课程列表
      </el-button>
      <h2 class="course-title">课程资源中心</h2>
    </div>
    
    <!-- 资源类型标签 -->
    <div class="resource-tabs">
      <button
        v-for="type in resourceTypes"
        :key="type"
        :class="{ active: activeType === type }"
        @click="switchType(type)"
      >
        <i :class="getResourceIcon(type)"></i>
        {{ type === 'video' ? '视频' : 
           type === 'doc' ? '文档' :
           type === 'ppt' ? '课件PPT' : 
           type === 'pdf' ? '课件PDF' :
           '其他' }}
      </button>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <el-alert :title="error" type="error" show-icon />
      </div>

      <!-- 资源展示区 -->
      <div v-else class="resource-content">
        <!-- 视频资源特殊处理 -->
        <template v-if="activeType === 'video'">
          <div class="video-content" v-if="currentResources.length > 0">
            <div 
              v-for="resource in currentResources" 
              :key="resource.resourceId" 
              class="video-card"
            >
              <div class="video-header">
                <h3 class="video-title">{{ resource.name }}</h3>
                <el-button 
                  v-if="user?.role === 'teacher'"
                  @click.stop="fetchTeacherAnalytics(resource.resourceId)"
                  class="analytics-btn"
                  :loading="teacherAnalyticsLoading"
                  type="primary"
                  size="small"
                >
                  <i class="el-icon-data-analysis"></i>
                  {{ teacherAnalyticsLoading ? '分析中...' : '获取分析数据' }}
                </el-button>
              </div>
              
              <div class="video-wrapper">
                <video
                  controls
                  :src="resource.url"
                  @error="handleVideoError(resource, $event)"
                  class="course-video"
                  crossorigin="anonymous"
                />
              </div>
              
              <!-- 分析数据面板 -->
              <div v-if="teacherAnalyticsData" class="analytics-panel">
                <div class="panel-header">
                  <i class="el-icon-s-data"></i>
                  <span>视频分析数据</span>
                </div>
                
                <div class="metrics-grid">
                  <div class="metric-card">
                    <div class="metric-value">{{ (teacherAnalyticsData.completionRate * 100).toFixed(1) }}%</div>
                    <div class="metric-label">完播率</div>
                  </div>
                  
                  <div class="metric-card">
                    <div class="metric-value">{{ teacherAnalyticsData.averageWatchTime }}秒</div>
                    <div class="metric-label">平均观看时长</div>
                  </div>
                  
                  <div class="metric-card">
                    <div class="metric-value">{{ teacherAnalyticsData.totalViewers }}</div>
                    <div class="metric-label">观看人数</div>
                  </div>
                </div>
                
                <!-- 热播区间 -->
                <div class="hotspots-section" v-if="teacherAnalyticsData.hotSpots?.length">
                  <div class="section-title">
                    <i class="el-icon-hot-water"></i>
                    <span>热播区间</span>
                  </div>
                  
                  <div class="hotspots-list">
                    <div v-for="(spot, index) in teacherAnalyticsData.hotSpots" 
                         :key="index" 
                         class="hotspot-item">
                      <div class="time-range">
                        {{ formatTime(spot.startTime) }} - {{ formatTime(spot.endTime) }}
                      </div>
                      <div class="replay-count">
                        <el-tag type="danger" size="small">
                          {{ spot.replayCount }}次回放
                        </el-tag>
                      </div>
                      <el-progress 
                        :percentage="(spot.replayCount / maxReplayCount) * 100" 
                        :show-text="false"
                        :stroke-width="8"
                        color="#F56C6C"
                      />
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 错误提示 -->
              <div v-if="videoErrors[resource.resourceId]" class="video-error">
                <el-alert :title="videoErrors[resource.resourceId]" type="error" show-icon />
                <el-button @click="retryLoad(resource)" size="small">重试加载</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty description="暂无视频资源"></el-empty>
          </div>
        </template>

        <!-- 其他类型资源 -->
        <template v-else>
          <div class="document-list" v-if="currentResources.length > 0">
            <div 
              v-for="resource in currentResources"
              :key="resource.resourceId"
              class="document-card"
            >
              <div class="document-icon">
                <i :class="getResourceIcon(resource.type)"></i>
              </div>
              <div class="document-info">
                <div class="document-name">{{ resource.name }}</div>
                <el-button 
                  @click="handleDownloadResource(resource)" 
                  type="text"
                  icon="el-icon-download"
                >
                  下载
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-empty :description="`暂无${getResourceTypeName(activeType)}资源`"></el-empty>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 基础样式 */
.course-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
}

/* 头部样式 */
.header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.back-btn {
  margin-right: 20px;
  padding: 8px 12px;
}

.course-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

/* 资源标签样式 */
.resource-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.resource-tabs button {
  padding: 10px 20px;
  border: none;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
  display: flex;
  align-items: center;
}

.resource-tabs button i {
  margin-right: 6px;
  font-size: 16px;
}

.resource-tabs button.active {
  background: #409eff;
  color: white;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 主要内容区 */
.main-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

/* 视频卡片样式 */
.video-card {
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.video-title {
  margin: 0;
  font-size: 16px;
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
}

/* 分析面板样式 */
.analytics-panel {
  padding: 20px;
  background: #f9fafc;
  border-top: 1px solid #ebeef5;
}

.panel-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  font-size: 16px;
  color: #303133;
  font-weight: 500;
}

.panel-header i {
  margin-right: 8px;
  color: #409eff;
}

/* 指标卡片样式 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  background: white;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
}

/* 热播区间样式 */
.hotspots-section {
  margin-top: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  font-size: 15px;
  color: #303133;
}

.section-title i {
  margin-right: 8px;
  color: #f56c6c;
}

.hotspots-list {
  background: white;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.hotspot-item {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.hotspot-item:last-child {
  border-bottom: none;
}

.time-range {
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
}

.replay-count {
  margin-bottom: 8px;
}

/* 文档列表样式 */
.document-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.document-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.document-card:hover {
  transform: translateY(-3px);
}

.document-icon {
  margin-right: 12px;
  font-size: 24px;
  color: #409eff;
}

.document-info {
  flex: 1;
}

.document-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

/* 空状态样式 */
.empty-state {
  padding: 40px 0;
}

/* 错误状态 */
.video-error {
  padding: 16px;
  background: #fef0f0;
  border-top: 1px solid #fde2e2;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  
  .document-list {
    grid-template-columns: 1fr;
  }
}
</style>