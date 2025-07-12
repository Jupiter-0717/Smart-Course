<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useLearnStore } from '@/stores/learn'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import VideoRecommendationList from '@/components/video/VideoRecommendationList.vue'
import DocumentRecommendationList from '@/components/DocumentRecommendationList.vue'
const authStore = useAuthStore()
const learnStore = useLearnStore()
const { user } = storeToRefs(authStore)
const router = useRouter()
const showRecommendations = ref(false)

// 直接使用store中的状态
const { loading, error, courses } = storeToRefs(learnStore)

// 初始化加载
onMounted(async () => {
  console.log(user.value)
  if (user.value?.role === 'student' && user.value.studentId) {
    await learnStore.getCurrntCourse(user.value.studentId)
    console.log(courses.value)
    console.log(user.value)
    console.log(user.value.studentId)
    // 加载完课程后再显示推荐
    showRecommendations.value = true
  }
})

const viewCourseDetail = (courseId: number) => {
  router.push(`/learn/${courseId}`)
}
</script>

<template>
  <div class="learn-container">
    <!-- 课程列表部分保持不变 -->
    <div class="course-list-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>课程加载中...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <span class="error-icon">⚠️</span>
        <p>{{ error }}</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!courses || courses.length === 0" class="empty-state">
        <p>暂无课程数据</p>
      </div>

      <!-- 课程列表 -->
      <div v-else class="course-list">
        <h2 class="list-title">我的课程</h2>
        <div class="course-grid">
          <div
            v-for="course in courses"
            :key="course.code"
            class="course-card"
            @click="viewCourseDetail(course.courseId)"
          >
            <div class="course-code">{{ course.code }}</div>
            <div class="course-name">{{ course.courseName }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增的视频推荐部分 -->
    <div
      v-if="showRecommendations && !loading && !error && courses && courses.length > 0"
      class="recommendations-section"
    >
      <!-- 个性化推荐 -->
      <VideoRecommendationList recommendation-type="personalized" />

      <!-- 复习推荐 -->
      <VideoRecommendationList recommendation-type="replay" />
      <DocumentRecommendationList />
    </div>
  </div>
</template>

<style scoped>
.learn-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.recommendations-section {
  margin-top: 2rem;
}

/* 原有样式保持不变 */
.course-list-container {
  width: 100%;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  color: #e74c3c;
}

.error-icon {
  font-size: 2rem;
  margin-bottom: 15px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.list-title {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
  font-weight: 600;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.course-card {
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eaeaea;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
  border-color: #3498db;
}

.course-code {
  font-size: 1.2rem;
  font-weight: bold;
  color: #3498db;
  margin-bottom: 8px;
  font-family: 'Courier New', monospace;
}

.course-name {
  font-size: 1rem;
  color: #555;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .course-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
