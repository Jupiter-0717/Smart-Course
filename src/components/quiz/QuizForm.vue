<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>{{ isEdit ? '编辑试卷' : '新建试卷' }}</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="quiz-form">
          <div class="form-group">
            <label>试卷标题 *</label>
            <input
              v-model="formData.title"
              required
              class="form-control"
              placeholder="请输入试卷标题"
            />
          </div>

          <div class="form-group">
            <label>总分 *</label>
            <input
              v-model.number="formData.totalPoints"
              type="number"
              required
              min="1"
              class="form-control"
              placeholder="请输入试卷总分"
            />
          </div>
          <!-- 新增：课程选择下拉框 -->
          <div class="form-group">
            <label>课程 *</label>
            <select v-model.number="formData.courseId" required class="form-control">
              <option value="" disabled>请选择课程</option>
              <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                {{ course.name }}
              </option>
            </select>
          </div>
        </form>
        <!-- 优化：Element Plus卡片式题目编辑区，内容参考详情弹窗 -->
        <div v-if="isEdit && quizDetail" class="quiz-question-edit-section">
          <h4>本试卷题目列表</h4>
          <el-row :gutter="16">
            <el-col :span="24" v-for="q in quizDetail.questions" :key="q.questionId">
              <el-card
                class="question-card"
                shadow="hover"
                :class="{
                  'deleted-question': q.editStatus === 'deleted',
                  'new-question': q.editStatus === 'new',
                  'modified-question': q.editStatus === 'modified',
                }"
              >
                <div class="question-header">
                  <!-- 状态标签 -->
                  <el-tag v-if="q.editStatus === 'deleted'" type="info" size="small">已删除</el-tag>
                  <el-tag v-if="q.editStatus === 'new'" type="success" size="small">新</el-tag>
                  <el-tag v-if="q.editStatus === 'modified'" type="warning" size="small"
                    >已修改</el-tag
                  >

                  <el-tag size="small" type="info">{{ getTypeText(q.type) }}</el-tag>
                  <el-input-number
                    v-model="q.score"
                    :min="0"
                    size="small"
                    style="margin: 0 8px; width: 80px"
                    @change="markQuestionModified(q)"
                    :disabled="q.editStatus === 'deleted'"
                  />
                  <span class="score-label">分</span>
                  <el-input-number
                    v-model="q.orderIndex"
                    :min="1"
                    size="small"
                    style="margin: 0 8px; width: 80px"
                    @change="markQuestionModified(q)"
                    :disabled="q.editStatus === 'deleted'"
                  />
                  <span class="order-label">顺序</span>

                  <!-- 删除/恢复按钮 -->
                  <el-button
                    @click="removeQuestionLocal(q)"
                    size="small"
                    :type="q.editStatus === 'deleted' ? 'success' : 'danger'"
                    plain
                    style="margin-left: 8px"
                  >
                    {{ q.editStatus === 'deleted' ? '恢复' : '移除' }}
                  </el-button>
                  <el-button @click="replaceQuestion" size="small" type="primary" plain
                    >替换</el-button
                  >
                </div>
                <!-- 题目内容 -->
                <div class="question-content">
                  <div class="question-body" v-html="parseQuestionBody(q.body)"></div>
                  <div class="question-footer">
                    <span>难度: {{ getDifficultyStars(q.difficulty) }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <div style="margin-top: 16px">
            <el-button @click="showAddQuestionDialog = true" type="primary">添加题目</el-button>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <el-form-item v-if="isEdit">
          <el-button
            type="primary"
            @click="saveAllChanges"
            :disabled="!hasUnsavedChanges"
            :loading="isSaving"
          >
            {{ isSaving ? '保存中...' : '保存所有变更' }}
          </el-button>
          <el-button @click="$emit('close')">取消</el-button>
          <span
            v-if="hasUnsavedChanges"
            class="unsaved-indicator"
            style="color: #e6a23c; margin-left: 8px"
          >
            * 有未保存的变更
          </span>
        </el-form-item>
        <el-form-item v-else>
          <el-button type="primary" @click="createQuiz" :loading="isSaving">
            {{ isSaving ? '创建中...' : '创建试卷' }}
          </el-button>
          <el-button @click="$emit('close')">取消</el-button>
        </el-form-item>
      </div>
    </div>

    <!-- 移到这里，不依赖题目编辑区的条件渲染 -->
    <QuestionSearchDialog
      :visible="showAddQuestionDialog"
      :added-ids="quizDetail?.questions.map((q) => q.questionId) || []"
      @close="showAddQuestionDialog = false"
      @add="handleAddQuestion"
    />
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, computed, onMounted } from 'vue'
import { quizService } from '../../services/quiz.service'
import type { QuizResponse, QuizDetailResponse, QuizQuestionDetail } from '../../types/quiz'
import type { QuestionType } from '../../types/question'
import { TEACHER_ID } from '@/config/teacher' // 全局teacherId--曹雨荷
import { ElMessageBox } from 'element-plus'
import courseService from '../../services/course.service' // 新增：导入课程服务
import QuestionSearchDialog from './QuestionSearchDialog.vue'
// 修正类型定义
import type { Course } from '../../types/course'
import type { QuestionResponse } from '../../types/question'

interface Props {
  quiz?: QuizResponse | null
}

const props = defineProps<Props>()
const emit = defineEmits(['close', 'success'])

// 响应式数据
const isSubmitting = ref(false)
const formData = ref({
  title: '',
  totalPoints: 100,
  courseId: undefined as number | undefined, // 类型修正
})

// 新增：题目编辑状态和操作记录
const deletedQuestions = ref<number[]>([]) // 被删除的题目ID
const addedQuestions = ref<QuizQuestionDetail[]>([]) // 新添加的题目
const modifiedQuestions = ref<{ questionId: number; score: number; orderIndex: number }[]>([]) // 修改的题目

// 为题目添加editStatus字段
interface QuizQuestionDetailWithStatus extends QuizQuestionDetail {
  editStatus: 'normal' | 'deleted' | 'new' | 'modified'
}

// 修改题目列表类型
const quizDetail = ref<(QuizDetailResponse & { questions: QuizQuestionDetailWithStatus[] }) | null>(
  null,
)

// 新增：课程列表
const courses = ref<Course[]>([])

// 新增：添加题目弹窗状态
const showAddQuestionDialog = ref(false)

// 计算属性
const isEdit = computed(() => !!props.quiz)

// 新增：保存状态
const isSaving = ref(false)

// 方法
const handleSubmit = async () => {
  if (isSubmitting.value) return

  // 校验题目总分
  if (quizDetail.value && quizDetail.value.questions.length > 0) {
    const totalScore = quizDetail.value.questions.reduce(
      (sum, q) => sum + (Number(q.score) || 0),
      0,
    )
    if (totalScore !== Number(formData.value.totalPoints)) {
      try {
        await ElMessageBox.confirm(
          `题目总分为${totalScore}，与设定总分${formData.value.totalPoints}不符。\n是否将总分变更为${totalScore}后保存？`,
          '总分校验',
          {
            confirmButtonText: '变更并保存',
            cancelButtonText: '取消',
            type: 'warning',
          },
        )
        formData.value.totalPoints = totalScore
      } catch {
        // 用户取消
        return
      }
    }
  }

  if (!formData.value.courseId) {
    alert('请选择课程')
    return
  }

  try {
    isSubmitting.value = true

    // 1. 批量保存题目分数/顺序
    if (quizDetail.value && quizDetail.value.questions.length > 0 && props.quiz) {
      const batch = quizDetail.value.questions.map((q, idx) => ({
        questionId: q.questionId,
        score: q.score,
        orderIndex: q.orderIndex && q.orderIndex > 0 ? q.orderIndex : idx + 1,
      }))
      await quizService.batchSetQuestionSettings({
        quizId: props.quiz.id,
        data: batch,
        teacherId: TEACHER_ID,
      })
    }

    // 2. 保存试卷其它信息
    if (isEdit.value && props.quiz) {
      await quizService.updateQuiz({
        quizId: props.quiz.id,
        data: {
          title: formData.value.title,
          totalPoints: formData.value.totalPoints,
        },
      })
      await loadQuizDetail()
    } else {
      await quizService.createQuiz({
        title: formData.value.title,
        creatorId: TEACHER_ID, // 全局teacherId--曹雨荷
        courseId: formData.value.courseId, // 新增：传递课程ID
        totalPoints: formData.value.totalPoints,
      })
    }

    emit('success')
  } catch (error) {
    console.error('保存试卷失败:', error)
    alert('保存失败，请重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleOverlayClick = () => {
  emit('close')
}

const initForm = () => {
  if (props.quiz) {
    // 编辑模式
    formData.value = {
      title: props.quiz.title,
      totalPoints: props.quiz.totalPoints,
      courseId: props.quiz.courseId || undefined, // 编辑时从详情加载课程ID
    }
    loadQuizDetail()
  } else {
    // 新建模式
    formData.value = {
      title: '',
      totalPoints: 100,
      courseId: undefined, // 新建时清空课程ID
    }
    quizDetail.value = null
  }
}

// 加载试卷详情
const loadQuizDetail = async () => {
  if (!props.quiz) return
  try {
    const result = await quizService.getQuizDetail({ quizId: props.quiz.id })
    // 为题目添加editStatus字段
    const questionsWithStatus: QuizQuestionDetailWithStatus[] = result.data.questions.map((q) => ({
      ...q,
      editStatus: 'normal' as const,
    }))

    quizDetail.value = {
      ...result.data,
      questions: questionsWithStatus,
    }

    // 更新表单数据
    formData.value.title = result.data.title
    formData.value.courseId = result.data.courseId
    formData.value.totalPoints = result.data.totalPoints
  } catch {
    console.error('加载试卷详情失败')
  }
}

// 题目相关操作
const replaceQuestion = async () => {
  alert('请在题库中选择替换题目功能，暂不支持直接输入ID替换。')
}

// 移除未使用的openAddQuestionDialog

// 新增：删除/恢复题目（仅前端暂存）
const removeQuestionLocal = (q: QuizQuestionDetailWithStatus) => {
  if (q.editStatus === 'deleted') {
    // 恢复题目 - 恢复到原始状态或保持new状态
    if (addedQuestions.value.some((added) => added.questionId === q.questionId)) {
      q.editStatus = 'new'
    } else {
      q.editStatus = 'normal'
    }
    const index = deletedQuestions.value.indexOf(q.questionId)
    if (index > -1) {
      deletedQuestions.value.splice(index, 1)
    }
  } else {
    // 删除题目
    q.editStatus = 'deleted'
    deletedQuestions.value.push(q.questionId)
  }
}

// 新增：记录题目分数/顺序修改（仅前端暂存）
const markQuestionModified = (q: QuizQuestionDetailWithStatus) => {
  if (q.editStatus === 'new') return // 新题目不需要记录修改

  q.editStatus = 'modified'
  const existing = modifiedQuestions.value.find((item) => item.questionId === q.questionId)
  if (existing) {
    existing.score = q.score
    existing.orderIndex = q.orderIndex
  } else {
    modifiedQuestions.value.push({
      questionId: q.questionId,
      score: q.score,
      orderIndex: q.orderIndex,
    })
  }
}

// 修改：添加单个题目到本地暂存（适配自定义弹窗）
const handleAddQuestion = (question: QuestionResponse) => {
  if (!quizDetail.value) return
  if (quizDetail.value.questions.some((q) => q.questionId === question.id)) return
  const newQuestion: QuizQuestionDetailWithStatus = {
    questionId: question.id,
    type: question.type,
    body: question.body,
    difficulty: question.difficulty,
    score: 10,
    orderIndex: quizDetail.value.questions.length + 1,
    editStatus: 'new',
  }
  quizDetail.value.questions.push(newQuestion)
  addedQuestions.value.push(newQuestion)
}

const getTypeText = (type: QuestionType) => {
  const typeMap = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    FILL_IN_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return typeMap[type] || type
}

// 新增：解析题目内容
const parseQuestionBody = (body: string) => {
  try {
    const parsed = JSON.parse(body)
    let html = `<div class="stem"><strong>${parsed.stem || ''}</strong></div>`
    // 处理选择题选项
    if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
      html += '<div class="options-list">'
      parsed.options.forEach((option: { label: string; text: string }, index: number) => {
        const letter = String.fromCharCode(65 + index) // A, B, C, D
        const text = option.text || option.label || option // 支持不同的选项格式
        // 判断是否为正确答案
        let isCorrect = false
        if (Array.isArray(parsed.answer)) {
          // 多选题：答案是数组
          isCorrect = parsed.answer.includes(letter)
        } else {
          // 单选题：答案是字符串
          isCorrect = parsed.answer === letter
        }
        html += `<div class="option-item ${isCorrect ? 'correct-option' : ''}">${letter}. ${text}</div>`
      })
      html += '</div>'
    }
    // 显示答案
    if (parsed.answer) {
      let answerText = parsed.answer
      if (Array.isArray(parsed.answer)) {
        answerText = parsed.answer.join(', ') // 多选题显示所有答案
      }
      html += `<div class="answer-section"><strong>答案：</strong>${answerText}</div>`
    }
    // 显示解析
    if (parsed.analysis) {
      html += `<div class="analysis-section"><strong>解析：</strong>${parsed.analysis}</div>`
    }
    return html
  } catch {
    return `<div class="stem">${body}</div>`
  }
}

// 新增：难度星级显示
const getDifficultyStars = (difficulty: number) => {
  return '★'.repeat(difficulty) + '☆'.repeat(5 - difficulty)
}

// 删除未使用的方法
// const getQuestionText = (body: string) => { ... }
// const isChoiceQuestion = (type: string) => { ... }
// const getQuestionOptions = (body: string) => { ... }
// const getAnswer = (body: string) => { ... }
// const getAnalysis = (body: string) => { ... }
// const isAnswer = (option: any, body: string) => { ... }
// const cancelEdit = (q: QuizQuestionDetail) => { ... }
// const saveEdit = (q: QuizQuestionDetail) => { ... }

// 新增：保存所有变更
const saveAllChanges = async () => {
  if (!quizDetail.value || !props.quiz) {
    ElMessageBox.alert('数据加载错误', '错误', { type: 'error' })
    return
  }

  if (isSaving.value) return
  isSaving.value = true

  try {
    // 1. 删除题目
    for (const questionId of deletedQuestions.value) {
      await quizService.removeQuestionFromQuiz({
        quizId: props.quiz.id,
        questionId,
      })
    }

    // 2. 添加题目
    for (const question of addedQuestions.value) {
      await quizService.addQuestionToQuiz({
        quizId: props.quiz.id,
        data: {
          questionId: question.questionId,
          score: question.score,
          orderIndex: question.orderIndex,
        },
      })
    }

    // 3. 更新题目设置（分数和顺序）
    for (const modified of modifiedQuestions.value) {
      await quizService.setQuestionSettings({
        quizId: props.quiz.id,
        data: {
          questionId: modified.questionId,
          score: modified.score,
          orderIndex: modified.orderIndex,
        },
      })
    }

    // 4. 检测并更新试卷基本信息
    const hasBasicInfoChanged =
      formData.value.title !== quizDetail.value.title ||
      formData.value.courseId !== quizDetail.value.courseId ||
      formData.value.totalPoints !== quizDetail.value.totalPoints

    if (hasBasicInfoChanged) {
      await quizService.updateQuiz({
        quizId: props.quiz.id,
        data: {
          title: formData.value.title,
          courseId: formData.value.courseId,
          totalPoints: formData.value.totalPoints,
        },
      })
    }

    // 5. 清空暂存数据并重新加载
    deletedQuestions.value = []
    addedQuestions.value = []
    modifiedQuestions.value = []

    await loadQuizDetail()
    ElMessageBox.alert('保存成功', '成功', { type: 'success' })
    emit('success')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessageBox.alert('保存失败，请重试', '错误', { type: 'error' })
  } finally {
    isSaving.value = false
  }
}

// 新增：检查是否有未保存的变更
const hasUnsavedChanges = computed(() => {
  if (!quizDetail.value) return false

  const hasQuestionChanges =
    deletedQuestions.value.length > 0 ||
    addedQuestions.value.length > 0 ||
    modifiedQuestions.value.length > 0

  const hasBasicInfoChanges =
    formData.value.title !== quizDetail.value.title ||
    formData.value.courseId !== quizDetail.value.courseId ||
    formData.value.totalPoints !== quizDetail.value.totalPoints

  return hasQuestionChanges || hasBasicInfoChanges
})

// 新建试卷API调用
const createQuiz = async () => {
  if (!formData.value.title || !formData.value.courseId || !formData.value.totalPoints) {
    alert('请填写完整信息')
    return
  }
  try {
    isSaving.value = true
    await quizService.createQuiz({
      title: formData.value.title,
      creatorId: TEACHER_ID,
      courseId: formData.value.courseId,
      totalPoints: formData.value.totalPoints,
    })
    emit('success')
    emit('close')
  } catch {
    alert('创建失败，请重试')
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  initForm()
  // 加载课程列表
  courses.value = await courseService.getAllCourses()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.quiz-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.form-control:focus {
  outline: none;
  border-color: #1890ff;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.quiz-question-edit-section {
  margin-top: 20px;
}

.question-card {
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

/* 删除状态的题目卡片 */
.deleted-question {
  opacity: 0.5;
  background-color: #f5f5f5;
  border-left: 4px solid #909399;
}

.deleted-question .question-content {
  text-decoration: line-through;
  color: #909399;
}

/* 新增状态的题目卡片 */
.new-question {
  border-left: 4px solid #67c23a;
  background-color: #f0f9ff;
}

/* 修改状态的题目卡片 */
.modified-question {
  border-left: 4px solid #e6a23c;
  background-color: #fdf6ec;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.question-content {
  line-height: 1.6;
  color: #303133;
}

.score-label,
.order-label {
  font-size: 12px;
  color: #909399;
}

.unsaved-indicator {
  color: #e6a23c;
  font-size: 12px;
  margin-left: 8px;
}

/* 按钮样式优化 */
.el-button + .el-button {
  margin-left: 8px;
}

/* 题目内容样式 */
.question-content .stem {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  line-height: 1.5;
}

.question-content .options-list {
  margin: 8px 0;
}

.question-content .option-item {
  margin: 4px 0;
  padding: 4px 8px;
  background-color: #f8f9fa;
  border-radius: 4px;
  font-size: 14px;
}

.question-content .option-item.correct-option {
  background-color: #f0f9ff;
  border-left: 3px solid #67c23a;
  font-weight: 500;
}

.question-content .answer-section {
  margin: 8px 0;
  padding: 6px 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  font-size: 14px;
  color: #67c23a;
}

.question-content .analysis-section {
  margin: 8px 0;
  padding: 6px 8px;
  background-color: #fdf6ec;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
  line-height: 1.4;
}

.question-footer {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
