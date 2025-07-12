<template>
  <div class="grade-analysis-view">
    <div class="page-header">
      <h1>我的成绩分析</h1>
      <div class="header-actions">
        <button @click="goBack" class="back-btn">返回</button>
      </div>
    </div>
    <div class="course-selector">
      <label for="courseSelect">选择课程：</label>
      <select id="courseSelect" v-model="selectedCourseId" @change="loadGradeData" class="course-select">
        <option value="">请选择课程</option>
        <option v-for="course in courses" :key="course.id" :value="course.id">
          {{ course.name }}
        </option>
      </select>
    </div>
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载成绩数据...</p>
    </div>
    <div v-else-if="gradeData" class="grade-analysis-content">
      <div class="grade-overview">
        <div class="overview-stats">
          <div class="stat-item">
            <div class="stat-value">{{ formatGrade(gradeData.currentGrade) }}</div>
            <div class="stat-label">当前成绩</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatPercentage(gradeData.completionRate) }}%</div>
            <div class="stat-label">完成度</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ gradeData.classRank }}</div>
            <div class="stat-label">班级排名</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ gradeData.totalStudents }}</div>
            <div class="stat-label">班级总人数</div>
          </div>
        </div>
      </div>
      <div class="charts-section">
        <div class="chart-container">
          <GradeChart title="成绩分布统计"
                      :data="gradeDistributionData"
                      type="bar"
                      yLabel="数量"
                      :showControls="false" />
        </div>
        <div class="chart-container">
          <GradeChart title="成绩分布比例"
                      :data="gradeDistributionData"
                      type="pie"
                      :showControls="false" />
        </div>
        <div class="chart-container full-width">
          <template v-if="gradeTrendData.length >= 3">
            <GradeChart title="成绩趋势变化"
                        :data="gradeTrendData"
                        type="line"
                        :showControls="false" />
          </template>
          <template v-else>
            <div style="text-align:center;padding:60px 0;color:#888;">暂无足够数据展示成绩趋势</div>
          </template>
        </div>
      </div>
      <div v-if="gradeData.learningSuggestions" class="suggestions-section">
        <h3>学习建议</h3>
        <div class="suggestions-list">
          <div v-for="(suggestion, index) in gradeData.learningSuggestions" :key="index" class="suggestion-item">
            {{ suggestion }}
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!loading && selectedCourseId" class="no-data">
      <p>暂无成绩数据</p>
    </div>
    <div v-else class="initial-state">
      <div class="initial-content">
        <div class="initial-icon">📊</div>
        <h3>欢迎使用成绩分析</h3>
        <p>请从上方选择课程开始查看成绩分析</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useLearnStore } from '@/stores/learn'
import { storeToRefs } from 'pinia'
import { gradeAnalysisService } from '@/services/gradeAnalysis.service'
import GradeChart from '@/components/grade/GradeChart.vue'

const router = useRouter()
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const learnStore = useLearnStore()
const { courses: studentCourses } = storeToRefs(learnStore)

const loading = ref(false)
const courses = ref<Array<{id: number, name: string}>>([])
const selectedCourseId = ref<number | null>(null)
const gradeData = ref<any>(null)

const gradeDistributionData = computed(() => {
  if (!gradeData.value) return []
  const dist = gradeData.value.statistics?.gradeDistribution
  if (dist) {
    return [
      { taskName: '优秀(90-100)', value: dist['优秀'] || 0 },
      { taskName: '良好(80-89)', value: dist['良好'] || 0 },
      { taskName: '中等(70-79)', value: dist['中等'] || 0 },
      { taskName: '及格(60-69)', value: dist['及格'] || 0 },
      { taskName: '不及格(0-59)', value: dist['不及格'] || 0 }
    ]
  }
  return []
})

const gradeTrendData = computed(() => {
  if (!gradeData.value || !gradeData.value.gradeTrend) return []
  const sorted = [...gradeData.value.gradeTrend].sort((a, b) => new Date(a.date) - new Date(b.date))
  return sorted.map((trend: any) => ({
    date: `${trend.taskTitle}${trend.date ? ' ' + trend.date.slice(5, 10) : ''}`,
    score: formatGrade(trend.score),
    taskType: trend.taskType
  }))
})

const goBack = () => {
  router.go(-1)
}

const loadCourses = async () => {
  try {
    const studentId = user.value?.studentId
    if (!studentId) return
    await learnStore.getCurrntCourse(studentId)
    courses.value = studentCourses.value.map(c => ({
      id: c.courseId,
      name: c.courseName
    }))
  } catch (error) {
    console.error('加载课程列表失败:', error)
  }
}

const loadGradeData = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  try {
    const studentId = user.value?.studentId
    if (!studentId) {
      alert('未获取到学生ID，请重新登录')
      loading.value = false
      return
    }
    gradeData.value = await gradeAnalysisService.getStudentGradeAnalysis(studentId, selectedCourseId.value)
  } catch (error) {
    console.error('加载成绩数据失败:', error)
  } finally {
    loading.value = false
  }
}

const formatGrade = (grade: any): string => {
  if (grade === null || grade === undefined || grade === '') {
    return '0.0'
  }
  const num = parseFloat(grade)
  if (isNaN(num)) {
    return '0.0'
  }
  return num.toFixed(1)
}

const formatPercentage = (percentage: any): string => {
  if (percentage === null || percentage === undefined || percentage === '') {
    return '0.0'
  }
  const num = parseFloat(percentage)
  if (isNaN(num)) {
    return '0.0'
  }
  if (num <= 1) {
    return (num * 100).toFixed(1)
  }
  return num.toFixed(1)
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
  /* 复用 GradeAnalysisView.vue 的样式 */
  .grade-analysis-view {
    padding: 20px;
    width: 100%;
    max-width: none;
    margin: 0;
    box-sizing: border-box;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-shrink: 0;
  }

    .page-header h1 {
      margin: 0;
      color: #333;
      font-size: 24px;
    }

  .header-actions {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .back-btn {
    background: #6c757d;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
  }

    .back-btn:hover {
      background: #5a6268;
    }

  .course-selector {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }

    .course-selector label {
      font-weight: 500;
      color: #333;
    }

  .course-select {
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    min-width: 200px;
  }

  .loading-container {
    text-align: center;
    padding: 50px;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  .loading-spinner {
    border: 4px solid #f3f3f3;
    border-top: 4px solid #007bff;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
    margin: 0 auto 20px;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }

    100% {
      transform: rotate(360deg);
    }
  }

  .grade-overview {
    margin-bottom: 20px;
    flex-shrink: 0;
  }

  .overview-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    gap: 15px;
  }

  .stat-item {
    background: white;
    padding: 15px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    text-align: center;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #007bff;
    margin-bottom: 5px;
  }

  .stat-label {
    font-size: 12px;
    color: #666;
  }

  .grade-analysis-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .charts-section {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-bottom: 20px;
    width: 100%;
    flex: 1;
    min-height: 0;
  }

  .chart-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    min-height: 300px;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

    .chart-container.full-width {
      grid-column: 1 / -1;
      min-height: 350px;
      width: 100%;
    }

  .suggestions-section {
    margin-top: 20px;
    width: 100%;
    flex-shrink: 0;
  }

    .suggestions-section h3 {
      margin-bottom: 15px;
      color: #333;
      font-size: 18px;
    }

  .suggestions-list {
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    width: 100%;
  }

  .suggestion-item {
    padding: 10px;
    background: #f8f9fa;
    border-radius: 6px;
    border-left: 4px solid #409eff;
    color: #333;
    margin-bottom: 10px;
  }

    .suggestion-item:last-child {
      margin-bottom: 0;
    }

  .no-data {
    text-align: center;
    padding: 50px;
    color: #666;
    width: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  @media (max-width: 768px) {
    .grade-analysis-view {
      padding: 15px;
    }

    .charts-section {
      grid-template-columns: 1fr;
    }

    .overview-stats {
      grid-template-columns: repeat(2, 1fr);
    }

    .page-header {
      flex-direction: column;
      gap: 10px;
      align-items: flex-start;
    }
  }

  .initial-state {
    text-align: center;
    padding: 50px;
    color: #666;
    width: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  .initial-content {
    text-align: center;
  }

  .initial-icon {
    font-size: 48px;
    margin-bottom: 20px;
  }

  .initial-content h3 {
    margin-bottom: 10px;
    font-size: 24px;
  }

  .initial-content p {
    font-size: 16px;
  }
</style> 
