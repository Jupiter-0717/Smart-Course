<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="question-bank-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="search-section">
        <input
          v-model="searchKeyword"
          placeholder="输入关键词搜索题目..."
          class="search-input"
          @keyup.enter="handleKeywordSearch"
        />
        <button @click="handleKeywordSearch" class="search-btn">搜索</button>
      </div>

      <div class="filter-section">
        <select v-model="filterCourseId" class="filter-select">
          <option value="">全部课程</option>
          <option v-for="course in courses" :key="course.courseId" :value="String(course.courseId)">
            {{ course.name }}
          </option>
        </select>

        <select v-model="filterType" class="filter-select">
          <option value="">全部题型</option>
          <option value="SINGLE_CHOICE">单选题</option>
          <option value="MULTI_CHOICE">多选题</option>
          <option value="FILL_IN_BLANK">填空题</option>
          <option value="SHORT_ANSWER">简答题</option>
        </select>

        <button @click="handleSearch" class="filter-btn">筛选</button>
      </div>

      <div class="action-section">
        <button @click="showCreateForm = true" class="create-btn">新建题目</button>
        <button @click="handleBatchDelete" class="delete-btn" :disabled="selectedIds.length === 0">
          批量删除 ({{ selectedIds.length }})
        </button>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="question-list">
      <div class="list-header">
        <label class="checkbox-wrapper">
          <input type="checkbox" :checked="isAllSelected" @change="handleSelectAll" />
          <span>全选</span>
        </label>
        <span class="header-item">题目内容</span>
        <span class="header-item">题型</span>
        <span class="header-item">难度</span>
        <span class="header-item">课程</span>
        <span class="header-item">操作</span>
      </div>

      <div class="list-content">
        <div v-for="question in questions" :key="question.id" class="question-item">
          <label class="checkbox-wrapper">
            <input type="checkbox" :value="question.id" v-model="selectedIds" />
          </label>

          <div class="question-content">
            <div class="question-text">{{ getQuestionText(question.body) }}</div>
          </div>

          <div class="question-type">{{ getTypeText(question.type) }}</div>

          <div class="question-difficulty">
            <span class="difficulty-star" v-for="i in 5" :key="i">
              {{ i <= question.difficulty ? '★' : '☆' }}
            </span>
          </div>

          <div class="question-course">{{ question.courseName }}</div>

          <div class="question-actions">
            <button @click="handleViewDetail(question)" class="action-btn view-btn">查看</button>
            <button @click="handleEdit(question)" class="action-btn edit-btn">编辑</button>
            <button @click="handleDelete(question)" class="action-btn delete-btn">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <button
        @click="handlePageChange(currentPage - 1)"
        :disabled="currentPage <= 0"
        class="page-btn"
      >
        上一页
      </button>
      <span class="page-info">第 {{ currentPage + 1 }} 页，共 {{ totalPages }} 页</span>
      <button
        @click="handlePageChange(currentPage + 1)"
        :disabled="currentPage >= totalPages - 1"
        class="page-btn"
      >
        下一页
      </button>
    </div>

    <!-- 新建/编辑题目弹窗 -->
    <QuestionForm
      v-if="showCreateForm || showEditForm"
      :question="editingQuestion"
      :courses="courses"
      @close="closeForm"
      @success="handleFormSuccess"
    />

    <!-- 题目详情弹窗 -->
    <QuestionDetail
      v-if="showDetailModal"
      :question="selectedQuestion"
      @close="showDetailModal = false"
    />

    <!-- 在题目列表上方加调试输出 -->
    <!-- <pre>{{ questions }}</pre> -->
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, computed, onMounted } from 'vue'
import { questionBankService } from '../../services/questionBank.service'
import courseService from '../../services/course.service' // 该import用于题库管理的试题与组卷功能--曹雨荷
import type { QuestionType, Difficulty } from '../../types/question'
import type { Course } from '../../types/course'
import QuestionForm from './QuestionForm.vue'
import QuestionDetail from './QuestionDetail.vue'

// 定义后端返回的题目数据结构
interface BackendQuestionData {
  id: number
  courseId: number
  type: QuestionType
  body: string
  difficulty: Difficulty
  knowledgePointId: number
  courseName: string
  knowledgePointName: string
  createdAt?: string
  updatedAt?: string
}

// 响应式数据
const questions = ref<BackendQuestionData[]>([]) // 使用具体的类型定义
const courses = ref<Course[]>([])
const selectedIds = ref<number[]>([])
const searchKeyword = ref('')
const filterCourseId = ref<string | ''>('')
const filterType = ref<QuestionType | ''>('')
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = ref(10)

// 弹窗控制
const showCreateForm = ref(false)
const showEditForm = ref(false)
const showDetailModal = ref(false)
const editingQuestion = ref<BackendQuestionData | null>(null)
const selectedQuestion = ref<BackendQuestionData | null>(null)

// 计算属性
const isAllSelected = computed(() => {
  return (
    questions.value &&
    questions.value.length > 0 &&
    selectedIds.value.length === questions.value.length
  )
})

// 方法
const loadQuestions = async () => {
  try {
    // 始终带上courseId和type字段，为全部时传null
    const requestData = {
      page: currentPage.value,
      size: pageSize.value,
      courseId: filterCourseId.value !== '' ? Number(filterCourseId.value) : null,
      type: filterType.value !== '' ? filterType.value : null,
    }
    const response = await questionBankService.searchQuestions({
      data: requestData,
    })
    console.log('后端返回题目数据:', response)
    questions.value = response.content // 只取content
    console.log('questions.value:', questions.value)
    totalPages.value = response.totalPages
  } catch (error) {
    console.error('加载题目失败:', error)
  }
}

const loadCourses = async () => {
  try {
    courses.value = await courseService.getAllCourses() // 该调用用于题库管理的试题与组卷功能--曹雨荷
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadQuestions()
}

const handleKeywordSearch = async () => {
  console.log('搜索关键词：', searchKeyword.value)
  if (!searchKeyword.value.trim()) {
    handleSearch()
    return
  }
  try {
    const result = await questionBankService.searchByKeyword({
      keyword: searchKeyword.value,
    })
    console.log('关键词搜索API返回：', result)
    // 兼容对象和数组两种返回
    questions.value = Array.isArray(result) ? result : result.content
    currentPage.value = 0
    totalPages.value = 1
    console.log('questions.value 赋值后:', questions.value)
  } catch (error) {
    console.error('关键词搜索失败:', error)
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadQuestions()
}

const handleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = questions.value.map((q) => q.id)
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 道题目吗？`)) return

  try {
    await questionBankService.batchDeleteQuestions({
      ids: selectedIds.value,
    })
    selectedIds.value = []
    loadQuestions()
  } catch (error) {
    console.error('批量删除失败:', error)
  }
}

const handleViewDetail = (question: BackendQuestionData) => {
  selectedQuestion.value = question
  showDetailModal.value = true
}

const handleEdit = (question: BackendQuestionData) => {
  editingQuestion.value = question
  showEditForm.value = true
}

const handleDelete = async (question: BackendQuestionData) => {
  if (!confirm('确定要删除这道题目吗？')) return

  try {
    await questionBankService.deleteQuestion({
      id: question.id,
    })
    loadQuestions()
  } catch (error) {
    console.error('删除题目失败:', error)
  }
}

const closeForm = () => {
  showCreateForm.value = false
  showEditForm.value = false
  editingQuestion.value = null
}

const handleFormSuccess = () => {
  closeForm()
  loadQuestions() // 添加或编辑成功后刷新题目列表
}

const getQuestionText = (body: string) => {
  try {
    const parsed = JSON.parse(body)
    return parsed.stem || parsed.content || body
  } catch {
    return body
  }
}

const getTypeText = (type: QuestionType | string) => {
  const typeMap: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    FILL_IN_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return typeMap[type] || type
}

// 生命周期
onMounted(() => {
  loadQuestions()
  loadCourses()
})
</script>

<style scoped>
.question-bank-container {
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

.search-section {
  display: flex;
  gap: 8px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  width: 200px;
}

.search-btn {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
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

.delete-btn {
  padding: 8px 16px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.delete-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.question-list {
  flex: 1;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.list-header {
  display: grid;
  grid-template-columns: 40px 1fr 100px 100px 120px 120px;
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

.question-item {
  display: grid;
  grid-template-columns: 40px 1fr 100px 100px 120px 120px;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.question-item:hover {
  background: #f5f5f5;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
  flex-direction: row;
}

.question-content {
  max-width: 300px;
  text-align: center;
}

.question-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.question-type {
  text-align: center;
}

.question-difficulty {
  text-align: center;
}

.difficulty-star {
  color: #faad14;
  font-size: 12px;
}

.question-course {
  text-align: center;
}

.question-actions {
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
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.page-btn:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
}
</style>
