<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>试卷详情</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>

      <div class="modal-body">
        <div v-if="props.quiz" class="quiz-detail">
          <div class="detail-section">
            <label>试卷ID</label>
            <span>{{ props.quiz.id }}</span>
          </div>

          <div class="detail-section">
            <label>试卷标题</label>
            <span>{{ props.quiz.title }}</span>
          </div>

          <div class="detail-section">
            <label>创建者</label>
            <span>{{ props.quiz.creatorName }}</span>
          </div>

          <div class="detail-section">
            <label>总分</label>
            <span>{{ props.quiz.totalPoints }} 分</span>
          </div>

          <div class="detail-section">
            <label>题目列表</label>
            <div class="questions-list">
              <div
                v-for="(question, index) in props.quiz.questions"
                :key="question.questionId"
                class="question-item"
              >
                <div class="question-header">
                  <span class="question-index">{{ index + 1 }}.</span>
                  <span class="question-type">{{ getTypeText(question.type) }}</span>
                  <span class="question-score">{{ question.score }} 分</span>
                </div>

                <div class="question-content">
                  <div class="content-text">{{ getQuestionText(question.body) }}</div>

                  <!-- 选择题选项 -->
                  <div
                    v-if="isChoiceQuestion(question.type) && getQuestionOptions(question.body)"
                    class="options-list"
                  >
                    <div
                      v-for="(option, optIndex) in getQuestionOptions(question.body)"
                      :key="optIndex"
                      class="option-item"
                    >
                      <span class="option-label">{{ option.label }}.</span>
                      <span class="option-text">{{ option.text }}</span>
                    </div>
                  </div>
                </div>

                <div class="question-footer">
                  <span class="difficulty"
                    >难度:
                    <span class="difficulty-star" v-for="i in 5" :key="i">
                      {{ i <= question.difficulty ? '★' : '☆' }}
                    </span>
                  </span>
                  <span class="order">顺序: {{ question.orderIndex }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-primary">关闭</button>
      </div>
    </div>
  </div>

  <!-- 调试：输出quiz对象的JSON字符串 -->
  <pre>{{ JSON.stringify(props.quiz, null, 2) }}</pre>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import type { QuizDetailResponse } from '../../types/quiz'
import type { QuestionType } from '../../types/question'

interface Props {
  quiz: QuizDetailResponse | null
}

const props = defineProps<Props>()
console.log('QuizDetail.vue接收到的quiz:', props.quiz) // 调试
const emit = defineEmits<{
  close: []
}>()

// 计算属性
const isChoiceQuestion = (type: QuestionType) => {
  return type === 'SINGLE_CHOICE' || type === 'MULTI_CHOICE'
}

// 方法
const getTypeText = (type: QuestionType) => {
  const typeMap = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    FILL_IN_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return typeMap[type] || type
}

const getQuestionText = (body: string) => {
  try {
    const parsed = JSON.parse(body)
    return parsed.stem || parsed.content || body
  } catch {
    return body
  }
}

const getQuestionOptions = (body: string) => {
  try {
    const parsed = JSON.parse(body)
    return parsed.options || []
  } catch {
    return []
  }
}

const handleOverlayClick = () => {
  emit('close')
}
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
  width: 95%;
  max-width: 800px;
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

.quiz-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-section label {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.detail-section span {
  color: #666;
  font-size: 14px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.question-index {
  font-weight: 500;
  color: #1890ff;
  font-size: 16px;
}

.question-type {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.question-score {
  background: #52c41a;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.question-content {
  margin-bottom: 12px;
}

.content-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.option-label {
  font-weight: 500;
  color: #1890ff;
  min-width: 20px;
}

.option-text {
  color: #333;
  line-height: 1.5;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.difficulty {
  display: flex;
  align-items: center;
  gap: 4px;
}

.difficulty-star {
  color: #faad14;
  font-size: 12px;
}

.order {
  color: #666;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
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
</style>
