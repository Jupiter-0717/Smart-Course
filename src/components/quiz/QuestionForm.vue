<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>{{ isEdit ? '编辑题目' : '新建题目' }}</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="question-form">
          <div class="form-group">
            <label>课程 *</label>
            <select v-model="formData.courseId" required class="form-control">
              <option value="">请选择课程</option>
              <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                {{ course.name }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <KnowledgePointSelect
              :course-id="Number(formData.courseId) || null"
              :value="formData.knowledgePointId"
              label="知识点 *"
              placeholder="请选择知识点"
              :required="true"
              @update:value="handleKnowledgePointChange"
            />
          </div>

          <div class="form-group">
            <label>题型 *</label>
            <select v-model="formData.type" required class="form-control">
              <option value="">请选择题型</option>
              <option value="SINGLE_CHOICE">单选题</option>
              <option value="MULTI_CHOICE">多选题</option>
              <option value="FILL_IN_BLANK">填空题</option>
              <option value="SHORT_ANSWER">简答题</option>
            </select>
          </div>

          <div class="form-group">
            <label>难度 *</label>
            <div class="difficulty-selector">
              <span
                v-for="i in 5"
                :key="i"
                :class="['difficulty-star', { active: i <= formData.difficulty }]"
                @click="formData.difficulty = i as Difficulty"
              >
                {{ i <= formData.difficulty ? '★' : '☆' }}
              </span>
            </div>
          </div>

          <div class="form-group">
            <label>题目内容 *</label>
            <div class="question-body-editor">
              <div class="content-section">
                <label>题干</label>
                <textarea
                  v-model="questionBody.stem"
                  required
                  class="form-control"
                  placeholder="请输入题目内容"
                  rows="4"
                />
              </div>

              <!-- 选择题选项 -->
              <div v-if="isChoiceQuestion" class="options-section">
                <label>选项</label>
                <div
                  v-for="(option, index) in questionBody.options || []"
                  :key="index"
                  class="option-item"
                >
                  <input
                    v-model="option.text"
                    class="form-control"
                    :placeholder="`选项 ${option.label}`"
                  />
                  <button
                    type="button"
                    @click="removeOption(index)"
                    class="remove-option-btn"
                    :disabled="(questionBody.options || []).length <= 2"
                  >
                    删除
                  </button>
                </div>
                <button type="button" @click="addOption" class="add-option-btn">添加选项</button>
              </div>

              <!-- 答案 -->
              <div class="answer-section">
                <label>答案</label>
                <div v-if="isChoiceQuestion" class="choice-answer">
                  <!-- 单选题：使用radio -->
                  <div v-if="formData.type === 'SINGLE_CHOICE'">
                    <div
                      v-for="(option, index) in questionBody.options || []"
                      :key="index"
                      class="answer-option"
                    >
                      <input
                        type="radio"
                        :value="option.label"
                        v-model="questionBody.answer"
                        :name="'single-answer'"
                        :id="`answer-${index}`"
                      />
                      <label :for="`answer-${index}`">{{ option.label }}. {{ option.text }}</label>
                    </div>
                  </div>
                  <!-- 多选题：使用checkbox -->
                  <div v-else-if="formData.type === 'MULTI_CHOICE'">
                    <div
                      v-for="(option, index) in questionBody.options || []"
                      :key="index"
                      class="answer-option"
                    >
                      <input
                        type="checkbox"
                        :value="option.label"
                        v-model="questionBody.answer"
                        :id="`answer-${index}`"
                      />
                      <label :for="`answer-${index}`">{{ option.label }}. {{ option.text }}</label>
                    </div>
                  </div>
                </div>
                <input
                  v-else
                  v-model="questionBody.answer"
                  class="form-control"
                  placeholder="请输入答案"
                />
              </div>

              <!-- 解析 -->
              <div class="analysis-section">
                <label>解析</label>
                <textarea
                  v-model="questionBody.analysis"
                  class="form-control"
                  placeholder="请输入题目解析（可选）"
                  rows="3"
                />
              </div>
            </div>
          </div>
        </form>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-secondary">取消</button>
        <button @click="handleSubmit" class="btn btn-primary" :disabled="isSubmitting">
          {{ isSubmitting ? '保存中...' : isEdit ? '更新' : '创建' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, computed, watch, onMounted } from 'vue'
import { questionBankService } from '../../services/questionBank.service'
import type { QuestionResponse, QuestionType, QuestionBody, Difficulty } from '../../types/question'
import type { Course } from '../../types/course'
import KnowledgePointSelect from './KnowledgePointSelect.vue'

interface Props {
  question?: QuestionResponse | null
  courses: Course[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  success: []
}>()

// 响应式数据
const isSubmitting = ref(false)
const formData = ref<{
  courseId: string
  knowledgePointId: number
  type: QuestionType | ''
  difficulty: Difficulty
}>({
  courseId: '',
  knowledgePointId: 1,
  type: '',
  difficulty: 1,
})

// 扩展QuestionBody接口以支持多选题答案数组
interface ExtendedQuestionBody extends Omit<QuestionBody, 'answer'> {
  stem: string
  options?: Array<{ label: string; text: string }>
  answer: string | string[] // 支持单选(string)和多选(string[])
  analysis?: string
}

const questionBody = ref<ExtendedQuestionBody>({
  stem: '',
  options: [],
  answer: '',
  analysis: '',
})

// 计算属性
const isEdit = computed(() => !!props.question)
const isChoiceQuestion = computed(() => {
  return formData.value.type === 'SINGLE_CHOICE' || formData.value.type === 'MULTI_CHOICE'
})

// 方法
const addOption = () => {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F']
  const nextLabel = labels[questionBody.value.options?.length || 0]
  if (questionBody.value.options) {
    questionBody.value.options.push({ label: nextLabel, text: '' })
  }
}

const removeOption = (index: number) => {
  if (questionBody.value.options && questionBody.value.options.length > 2) {
    questionBody.value.options.splice(index, 1)
  }
}

const handleSubmit = async () => {
  if (isSubmitting.value) return

  try {
    isSubmitting.value = true

    // 处理答案格式
    let processedAnswer = questionBody.value.answer

    if (formData.value.type === 'MULTI_CHOICE') {
      // 多选题：确保答案是数组格式
      if (typeof processedAnswer === 'string') {
        processedAnswer = processedAnswer
          .split(',')
          .map((a) => a.trim())
          .filter((a) => a)
      } else if (!Array.isArray(processedAnswer)) {
        processedAnswer = []
      }
    } else if (formData.value.type === 'SINGLE_CHOICE') {
      // 单选题：确保答案是字符串格式
      if (Array.isArray(processedAnswer)) {
        processedAnswer = processedAnswer[0] || ''
      }
    }

    const processedQuestionBody = {
      ...questionBody.value,
      answer: processedAnswer,
    }

    const bodyJson = JSON.stringify(processedQuestionBody)

    if (isEdit.value && props.question) {
      await questionBankService.updateQuestion({
        id: props.question.id,
        data: {
          courseId: Number(formData.value.courseId),
          knowledgePointId: formData.value.knowledgePointId,
          type: formData.value.type as QuestionType,
          body: bodyJson,
          difficulty: formData.value.difficulty,
        },
      })
    } else {
      await questionBankService.createQuestion({
        data: {
          courseId: Number(formData.value.courseId),
          knowledgePointId: formData.value.knowledgePointId,
          type: formData.value.type as QuestionType,
          body: bodyJson,
          difficulty: formData.value.difficulty,
        },
      })
    }

    emit('success')
  } catch (error) {
    console.error('保存题目失败:', error)
    alert('保存失败，请重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleOverlayClick = () => {
  emit('close')
}

const initForm = () => {
  if (props.question) {
    // 编辑模式
    formData.value = {
      courseId: props.question.courseId.toString(),
      knowledgePointId: props.question.knowledgePointId,
      type: props.question.type,
      difficulty: props.question.difficulty,
    }

    try {
      const parsedBody = JSON.parse(props.question.body)
      let answer = parsedBody.answer || ''

      // 处理答案格式
      if (props.question.type === 'MULTI_CHOICE') {
        // 多选题：确保答案是数组格式
        if (typeof answer === 'string' && answer) {
          answer = answer
            .split(',')
            .map((a: string) => a.trim())
            .filter((a: string) => a)
        } else if (!Array.isArray(answer)) {
          answer = []
        }
      } else if (props.question.type === 'SINGLE_CHOICE') {
        // 单选题：确保答案是字符串格式
        if (Array.isArray(answer)) {
          answer = answer[0] || ''
        }
      }

      questionBody.value = {
        stem: parsedBody.stem || '',
        options: parsedBody.options || [],
        answer: answer,
        analysis: parsedBody.analysis || '',
      }
    } catch {
      questionBody.value = {
        stem: props.question.body,
        options: [],
        answer: '',
        analysis: '',
      }
    }
  } else {
    // 新建模式
    formData.value = {
      courseId: '',
      knowledgePointId: 1,
      type: '',
      difficulty: 1,
    }
    questionBody.value = {
      stem: '',
      options: [],
      answer: '',
      analysis: '',
    }
  }
}

// 监听题型变化，初始化选项和答案
watch(
  () => formData.value.type,
  (newType) => {
    if (newType === 'SINGLE_CHOICE' || newType === 'MULTI_CHOICE') {
      // 初始化选项
      if (!questionBody.value.options || questionBody.value.options.length === 0) {
        questionBody.value.options = [
          { label: 'A', text: '' },
          { label: 'B', text: '' },
          { label: 'C', text: '' },
          { label: 'D', text: '' },
        ]
      }

      if (newType === 'SINGLE_CHOICE') {
        // 单选题：答案设为空字符串
        questionBody.value.answer = ''
      } else if (newType === 'MULTI_CHOICE') {
        // 多选题：答案设为空数组
        questionBody.value.answer = []
      }
    } else {
      // 填空题和简答题
      questionBody.value.options = []
      questionBody.value.answer = ''
    }
  },
)

const handleKnowledgePointChange = (newKnowledgePointId: number) => {
  formData.value.knowledgePointId = newKnowledgePointId
}

onMounted(() => {
  initForm()
})
</script>

<style scoped>
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
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

.question-form {
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

.difficulty-selector {
  display: flex;
  gap: 4px;
}

.difficulty-star {
  font-size: 20px;
  color: #d9d9d9;
  cursor: pointer;
  transition: color 0.2s;
}

.difficulty-star.active {
  color: #faad14;
}

.question-body-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-section,
.options-section,
.answer-section,
.analysis-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.remove-option-btn {
  padding: 4px 8px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.remove-option-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.add-option-btn {
  padding: 8px 16px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  align-self: flex-start;
}

.choice-answer {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.answer-option {
  display: flex;
  align-items: center;
  gap: 4px;
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
