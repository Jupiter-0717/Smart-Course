<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="quiz-manage-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="filter-section">
        <select v-model="filterCourseId" class="filter-select">
          <option value="">全部课程</option>
          <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
            {{ course.name }}
          </option>
        </select>

        <button @click="handleFilter" class="filter-btn">筛选</button>
      </div>

      <div class="action-section">
        <button @click="showCreateForm = true" class="create-btn">新建试卷</button>
      </div>
    </div>

    <!-- 试卷列表 -->
    <div class="quiz-list">
      <div class="list-header">
        <span class="header-item">试卷标题</span>
        <span class="header-item">创建者</span>
        <span class="header-item">题目数量</span>
        <span class="header-item">总分</span>
        <span class="header-item">操作</span>
      </div>

      <div class="list-content">
        <div v-for="quiz in quizzes" :key="quiz.id" class="quiz-item">
          <div class="quiz-title">{{ quiz.title }}</div>
          <div class="quiz-creator">{{ quiz.creatorName }}</div>
          <div class="quiz-count">{{ quiz.questionCount }} 题</div>
          <div class="quiz-points">{{ quiz.totalPoints }} 分</div>

          <div class="quiz-actions">
            <button @click="handleViewDetail(quiz)" class="action-btn view-btn">查看</button>
            <button @click="handleEdit(quiz)" class="action-btn edit-btn">编辑</button>
            <button @click="handleDelete(quiz)" class="action-btn delete-btn">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑试卷弹窗 -->
    <QuizForm
      v-if="showCreateForm || showEditForm"
      :quiz="editingQuiz"
      @close="closeForm"
      @success="handleFormSuccess"
    />

    <!-- 试卷详情弹窗 -->
    <QuizDetail v-if="showDetailModal" :quiz="selectedQuiz" @close="showDetailModal = false" />
    <pre v-if="showDetailModal">{{ JSON.stringify(selectedQuiz, null, 2) }}</pre>

    <!-- 在试卷列表上方加调试输出 -->
    <!-- <pre>{{ quizzes }}</pre> -->
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, onMounted } from 'vue'
import { quizService } from '../../services/quiz.service'
import courseService from '../../services/course.service' // 该import用于题库管理的试题与组卷功能--曹雨荷
import type { QuizResponse, QuizDetailResponse } from '../../types/quiz'
import type { Course } from '../../types/course'
import QuizForm from './QuizForm.vue'
import QuizDetail from './QuizDetail.vue'

// 响应式数据
const quizzes = ref<QuizResponse[]>([])
const courses = ref<Course[]>([])
const filterCourseId = ref<number | ''>('')

// 弹窗控制
const showCreateForm = ref(false)
const showEditForm = ref(false)
const showDetailModal = ref(false)
const editingQuiz = ref<QuizResponse | null>(null)
const selectedQuiz = ref<QuizDetailResponse | null>(null)

// 方法
const loadQuizzes = async () => {
  try {
    const result = await quizService.getQuizListByTeacher()
    console.log('后端返回试卷数据:', result)
    quizzes.value = result.data // 只取data
    console.log('quizzes.value:', quizzes.value)
  } catch (error) {
    console.error('加载试卷失败:', error)
  }
}

const loadCourses = async () => {
  try {
    courses.value = await courseService.getAllCourses() // 该调用用于题库管理的试题与组卷功能--曹雨荷
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const handleFilter = async () => {
  const courseId = filterCourseId.value ? filterCourseId.value : null
  if (!courseId) {
    loadQuizzes()
    return
  }
  try {
    const result = await quizService.getQuizByCourse({
      courseId: courseId,
    })
    quizzes.value = result.data // 只取data
  } catch (error) {
    console.error('筛选试卷失败:', error)
  }
}

const handleViewDetail = async (quiz: QuizResponse) => {
  try {
    const detail = await quizService.getQuizDetail({
      quizId: quiz.id,
    })
    console.log('接口返回的试卷详情 detail:', detail)
    selectedQuiz.value = detail.data // 只取data
    console.log('赋值后的 selectedQuiz:', selectedQuiz.value)
    showDetailModal.value = true
  } catch (error) {
    console.error('获取试卷详情失败:', error)
  }
}

const handleEdit = (quiz: QuizResponse) => {
  editingQuiz.value = quiz
  showEditForm.value = true
}

const handleDelete = async (quiz: QuizResponse) => {
  if (!confirm('确定要删除这份试卷吗？')) return

  try {
    await quizService.deleteQuiz({
      quizId: quiz.id,
    })
    loadQuizzes()
  } catch (error) {
    console.error('删除试卷失败:', error)
  }
}

const closeForm = () => {
  showCreateForm.value = false
  showEditForm.value = false
  editingQuiz.value = null
}

const handleFormSuccess = () => {
  closeForm()
  loadQuizzes()
}

// 生命周期
onMounted(() => {
  loadQuizzes()
  loadCourses()
})
</script>

<style scoped>
.quiz-manage-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  min-width: 120px;
}

.filter-btn {
  padding: 8px 16px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-section {
  display: flex;
  gap: 8px;
}

.create-btn {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.quiz-list {
  flex: 1;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.list-header {
  display: grid;
  grid-template-columns: 1fr 120px 100px 100px 200px;
  gap: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
  text-align: center;
}

.list-header .header-item {
  text-align: center;
}

.list-content {
  max-height: 400px;
  overflow-y: auto;
}

.quiz-item {
  display: grid;
  grid-template-columns: 1fr 120px 100px 100px 200px;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.quiz-item:hover {
  background: #f5f5f5;
}

.quiz-title {
  font-weight: 500;
  color: #333;
  text-align: center;
}

.quiz-creator {
  text-align: center;
  color: #666;
}

.quiz-count {
  text-align: center;
  color: #666;
}

.quiz-points {
  text-align: center;
  color: #666;
}

.quiz-actions {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.view-btn {
  background: #1890ff;
  color: white;
}

.edit-btn {
  background: #52c41a;
  color: white;
}

.delete-btn {
  background: #ff4d4f;
  color: white;
}

/* 复制弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  color: #333;
}

.form-control {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #d9d9d9;
}
</style>
