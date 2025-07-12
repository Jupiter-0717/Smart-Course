<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>题目详情</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>

      <div class="modal-body">
        <div v-if="question" class="question-detail">
          <div class="detail-section">
            <label>题目ID</label>
            <span>{{ question.id }}</span>
          </div>

          <div class="detail-section">
            <label>课程</label>
            <span>{{ question.courseName }}</span>
          </div>

          <div class="detail-section">
            <label>知识点</label>
            <span>{{ question.knowledgePointName }}</span>
          </div>

          <div class="detail-section">
            <label>题型</label>
            <span>{{ getTypeText(question.type) }}</span>
          </div>

          <div class="detail-section">
            <label>难度</label>
            <div class="difficulty-display">
              <span class="difficulty-star" v-for="i in 5" :key="i">
                {{ i <= question.difficulty ? '★' : '☆' }}
              </span>
              <span class="difficulty-text">({{ question.difficulty }}/5)</span>
            </div>
          </div>

          <div class="detail-section">
            <label>题目内容</label>
            <div class="question-content">
              <div
                class="question-score"
                style="margin-bottom: 8px; color: #409eff; font-weight: bold"
              >
                分数：{{ (question as any).score }} 分
              </div>
              <div class="content-text">
                {{ parsedBody.stem || question.body }}
              </div>

              <!-- 选择题选项 -->
              <div v-if="isChoiceQuestion && parsedBody.options" class="options-list">
                <div v-for="(option, index) in parsedBody.options" :key="index" class="option-item">
                  <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                  <span class="option-text">{{ option.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <label>答案</label>
            <div class="answer-display">
              <span v-if="isChoiceQuestion && Array.isArray(parsedBody.answer)">
                {{ parsedBody.answer.join(', ') }}
              </span>
              <span v-else>
                {{ parsedBody.answer }}
              </span>
            </div>
          </div>

          <div v-if="parsedBody.analysis" class="detail-section">
            <label>解析</label>
            <div class="analysis-text">{{ parsedBody.analysis }}</div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-primary">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { computed } from 'vue'
import type { QuestionResponse, QuestionType, QuestionBody } from '../../types/question'

interface Props {
  question: QuestionResponse | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

// 计算属性
const parsedBody = computed((): QuestionBody => {
  if (!props.question) {
    return { stem: '', options: [], answer: '', analysis: '' }
  }

  try {
    return JSON.parse(props.question.body)
  } catch {
    return {
      stem: props.question.body,
      options: [],
      answer: '',
      analysis: '',
    }
  }
})

const isChoiceQuestion = computed(() => {
  return props.question?.type === 'SINGLE_CHOICE' || props.question?.type === 'MULTI_CHOICE'
})

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

.question-detail {
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

.difficulty-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.difficulty-star {
  color: #faad14;
  font-size: 16px;
}

.difficulty-text {
  color: #999;
  font-size: 12px;
}

.question-content {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
  border-left: 4px solid #1890ff;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.question-index {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.question-type {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.question-score {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.content-text {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 12px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.answer-display {
  background: #f6ffed;
  padding: 12px;
  border-radius: 6px;
  border-left: 4px solid #52c41a;
  font-weight: 500;
  color: #52c41a;
}

.analysis-text {
  background: #fff7e6;
  padding: 12px;
  border-radius: 6px;
  border-left: 4px solid #faad14;
  color: #333;
  line-height: 1.6;
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
