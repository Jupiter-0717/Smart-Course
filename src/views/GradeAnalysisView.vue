<template>
  <div class="grade-analysis-view">
    <!-- 简化的页面头部 -->
    <div class="page-header">
      <h1>{{ isTeacher ? '成绩分析管理' : '我的成绩分析' }}</h1>
      <div class="header-actions">
        <button @click="goBack"
                class="back-btn">
          返回
        </button>
        <button v-if="isTeacher"
                @click="exportGradeReport"
                class="export-btn"
                :disabled="loading || !selectedCourseId">
          <span v-if="loading">导出中...</span>
          <span v-else>导出成绩报表</span>
        </button>
      </div>
    </div>

    <!-- 简化的课程选择 -->
    <div class="course-selector">
      <label for="courseSelect">选择课程：</label>
      <select id="courseSelect"
              v-model="selectedCourseId"
              @change="loadGradeData"
              class="course-select">
        <option value="">请选择课程</option>
        <option v-for="course in courses"
                :key="course.id"
                :value="course.id">
          {{ course.name }}
        </option>
      </select>

    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载成绩数据...</p>
    </div>

    <!-- 成绩分析内容 -->
    <div v-else-if="gradeData" class="grade-analysis-content">
      <!-- 成绩概览 -->
      <div class="grade-overview">
        <div class="overview-stats">
          <div class="stat-item">
            <div class="stat-value">{{ isTeacher ? gradeData.totalStudents : formatGrade(gradeData.currentGrade) }}</div>
            <div class="stat-label">{{ isTeacher ? '总学生数' : '当前成绩' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ isTeacher ? formatGrade(gradeData.averageScore) : formatPercentage(gradeData.gradePercentage) + '%' }}</div>
            <div class="stat-label">{{ isTeacher ? '平均分' : '完成度' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ isTeacher ? formatGrade(gradeData.highestScore) : gradeData.classRank }}</div>
            <div class="stat-label">{{ isTeacher ? '最高分' : '班级排名' }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ isTeacher ? formatGrade(gradeData.lowestScore) : gradeData.totalStudents }}</div>
            <div class="stat-label">{{ isTeacher ? '最低分' : '班级总人数' }}</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 - 紧凑布局 -->
      <div class="charts-section">
        <!-- 成绩分布图表 - 柱状图 -->
        <div class="chart-container">
          <GradeChart title="成绩分布统计"
                      :data="gradeDistributionData"
                      type="bar"
                      :showControls="false" />
        </div>

        <!-- 成绩分布图表 - 饼图 -->
        <div class="chart-container">
          <GradeChart title="成绩分布比例"
                      :data="gradeDistributionData"
                      type="pie"
                      :showControls="false" />
        </div>

        <!-- 成绩趋势图表 - 折线图 -->
        <div class="chart-container full-width">
          <GradeChart :title="isTeacher ? '学生成绩排名分布' : '成绩趋势变化'"
                      :data="gradeTrendData"
                      type="line"
                      :showControls="false" />
        </div>
      </div>

      <!-- 学生成绩列表（仅教师端） -->
      <div v-if="isTeacher && gradeData.studentGrades" class="student-grades-section">
        <h3>学生成绩列表</h3>
        <div class="grades-table-container">
          <table class="grades-table">
            <thead>
              <tr>
                <th>排名</th>
                <th>学生姓名</th>
                <th>总成绩</th>
                <th>百分比</th>
                <th>等级</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="student in gradeData.studentGrades" :key="student.studentId">
                <td>{{ student.rank }}</td>
                <td>{{ student.studentName }}</td>
                <td>{{ formatGrade(student.totalScore) }}/{{ formatGrade(student.maxScore) }}</td>
                <td>{{ formatPercentage(student.percentage) }}%</td>
                <td>
                  <span class="grade-badge grade-{{ getGradeClass(student.grade) }}">
                    {{ student.grade }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 学习建议（仅学生端） -->
      <div v-if="!isTeacher && gradeData.learningSuggestions" class="suggestions-section">
        <h3>学习建议</h3>
        <div class="suggestions-list">
          <div v-for="(suggestion, index) in gradeData.learningSuggestions"
               :key="index"
               class="suggestion-item">
            {{ suggestion }}
          </div>
        </div>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-else-if="!loading && selectedCourseId" class="no-data">
      <p>暂无成绩数据</p>
    </div>

    <!-- 初始状态提示 -->
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
import { useRoute, useRouter } from 'vue-router'
import { gradeAnalysisService } from '@/services/gradeAnalysis.service'
import GradeChart from '@/components/grade/GradeChart.vue'
import type { CourseGradeReport, GradeAnalysisResponse } from '@/types/grade'
import { useAuthStore } from '@/stores/auth'
import { useLearnStore } from '@/stores/learn'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()

// 修复角色判断逻辑 - 通过路由路径判断
const isTeacher = computed(() => {
  // 从当前路由路径判断是否为教师端
  const path = route.path
  return path.includes('/teacher') || path.includes('/teacher/dashboard')
})

// 响应式数据
const loading = ref(false)
const courses = ref<Array<{id: number, name: string}>>([])
const selectedCourseId = ref<number | null>(null)
const gradeData = ref<any>(null)

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const learnStore = useLearnStore()
const { courses: studentCourses, loading: courseLoading } = storeToRefs(learnStore)

// 计算成绩分布数据
const gradeDistributionData = computed(() => {
  if (!gradeData.value) return []

  if (isTeacher.value && gradeData.value.gradeDistribution) {
    console.log('成绩分布原始数据:', gradeData.value.gradeDistribution)
    const data = [
      { taskName: '优秀(90-100)', percentage: gradeData.value.gradeDistribution.excellent },
      { taskName: '良好(80-89)', percentage: gradeData.value.gradeDistribution.good },
      { taskName: '中等(70-79)', percentage: gradeData.value.gradeDistribution.fair },
      { taskName: '及格(60-69)', percentage: gradeData.value.gradeDistribution.pass },
      { taskName: '不及格(0-59)', percentage: gradeData.value.gradeDistribution.fail }
    ]
    console.log('处理后的成绩分布数据:', data)
    return data
  }

  return []
})

// 计算成绩趋势数据
const gradeTrendData = computed(() => {
  if (!gradeData.value) return []

  if (isTeacher.value && gradeData.value.studentGrades) {
    // 教师端：显示学生成绩排名趋势（按成绩从高到低排序）
    const sortedStudents = [...gradeData.value.studentGrades].sort((a, b) => b.percentage - a.percentage)
    return sortedStudents.map((student: any, index: number) => ({
      date: `第${index + 1}名`,
      score: student.percentage,
      taskTitle: student.studentName
    }))
  } else if (!isTeacher.value && gradeData.value.gradeTrend) {
    // 学生端：显示个人成绩趋势
    return gradeData.value.gradeTrend.map((trend: any) => ({
      date: trend.date,
      score: formatGrade(trend.score),
      taskTitle: trend.taskTitle
    }))
  }

  return []
})

// 返回上一页
const goBack = () => {
  router.go(-1)
}

// 获取等级样式类
const getGradeClass = (gradeLevel: string) => {
  const gradeMap: Record<string, string> = {
    优秀: 'excellent',
    良好: 'good',
    中等: 'fair',
    及格: 'pass',
    不及格: 'fail',
    A: 'excellent',
    B: 'good',
    C: 'fair',
    D: 'pass',
    F: 'fail',
  }
  return gradeMap[gradeLevel] || 'default'
}

// 加载课程列表
const loadCourses = async () => {
  try {
    if (isTeacher.value) {
      // 教师端：获取教师课程列表
      const teacherId = user.value?.teacherId
      if (!teacherId) return
      courses.value = await gradeAnalysisService.getTeacherCourses(teacherId)
    } else {
      // 学生端：获取学生课程列表
      const studentId = user.value?.studentId
      if (!studentId) return
      await learnStore.getCurrntCourse(studentId)
      courses.value = studentCourses.value.map(c => ({
        id: c.courseId,
        name: c.courseName
      }))
    }
  } catch (error) {
    console.error('加载课程列表失败:', error)
  }
}

// 加载成绩数据
const loadGradeData = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  try {
    if (isTeacher.value) {
      gradeData.value = await gradeAnalysisService.getCourseGradeReport(selectedCourseId.value)
    } else {
      const studentId = user.value?.studentId
      console.log('请求成绩分析参数:', { studentId, courseId: selectedCourseId.value })
      if (!studentId) {
        alert('未获取到学生ID，请重新登录')
        loading.value = false
        return
      }
      if (!selectedCourseId.value) {
        alert('未选择课程')
        loading.value = false
        return
      }
      gradeData.value = await gradeAnalysisService.getStudentGradeAnalysis(studentId, selectedCourseId.value)
    }
  } catch (error) {
    console.error('加载成绩数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 导出成绩报表
const exportGradeReport = async () => {
  if (!selectedCourseId.value) {
    alert('请先选择课程')
    return
  }

  loading.value = true
  try {
    console.log('开始导出成绩报表，课程ID:', selectedCourseId.value)
    const blob = await gradeAnalysisService.exportCourseGradeReport(selectedCourseId.value)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `course_grade_report_${selectedCourseId.value}.xlsx`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
    console.log('导出成功')
  } catch (error: any) {
    console.error('导出失败:', error)

    // 根据错误类型提供不同的错误信息
    let errorMessage = '导出失败，请重试'

    if (error.response?.status === 500) {
      errorMessage = '服务器内部错误，请联系管理员'
    } else if (error.response?.status === 404) {
      errorMessage = '导出功能暂不可用'
    } else if (error.response?.status === 403) {
      errorMessage = '没有权限导出成绩报表'
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '导出超时，请重试'
    } else if (error.message.includes('Network Error')) {
      errorMessage = '网络连接失败，请检查网络'
    }

    alert(errorMessage)
  } finally {
    loading.value = false
  }
}

// 格式化成绩
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

// 格式化百分比
const formatPercentage = (percentage: any): string => {
  if (percentage === null || percentage === undefined || percentage === '') {
    return '0.0'
  }

  const num = parseFloat(percentage)
  if (isNaN(num)) {
    return '0.0'
  }

  // 如果数值小于等于1，说明是小数形式，需要乘以100
  if (num <= 1) {
    return (num * 100).toFixed(1)
  }

  return num.toFixed(1)
}

onMounted(() => {
  console.log('GradeAnalysisView 组件已挂载')
  console.log('当前路由路径:', route.path)
  console.log('isTeacher 计算属性:', isTeacher.value)
  loadCourses()
})
</script>

<style scoped>
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

  .export-btn {
    background: #007bff;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
  }

    .export-btn:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    .export-btn:hover:not(:disabled) {
      background: #0056b3;
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

  .student-grades-section {
    margin-top: 20px;
    width: 100%;
    flex-shrink: 0;
  }

    .student-grades-section h3 {
      margin-bottom: 15px;
      color: #333;
      font-size: 18px;
    }

  .grades-table-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    width: 100%;
  }

  .grades-table {
    width: 100%;
    border-collapse: collapse;
  }

    .grades-table th,
    .grades-table td {
      padding: 10px 12px;
      text-align: left;
      border-bottom: 1px solid #eee;
    }

    .grades-table th {
      background: #f8f9fa;
      font-weight: 600;
      color: #333;
    }

    .grades-table tr:hover {
      background: #f8f9fa;
    }

  .grade-badge {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
  }

  .grade-excellent {
    background: #d4edda;
    color: #155724;
  }

  .grade-good {
    background: #d1ecf1;
    color: #0c5460;
  }

  .grade-fair {
    background: #fff3cd;
    color: #856404;
  }

  .grade-pass {
    background: #f8d7da;
    color: #721c24;
  }

  .grade-fail {
    background: #f8d7da;
    color: #721c24;
  }

  .grade-default {
    background: #e2e3e5;
    color: #383d41;
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

  /* 响应式设计 */
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
