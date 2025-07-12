<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="quiz-question-container">
    <div class="header">
      <h2>试题与组卷管理</h2>
    </div>

    <div class="tab-container">
      <div class="tab-header">
        <button
          :class="['tab-button', { active: activeTab === 'question' }]"
          @click="activeTab = 'question'"
        >
          题库管理
        </button>
        <button
          :class="['tab-button', { active: activeTab === 'quiz' }]"
          @click="activeTab = 'quiz'"
        >
          组卷管理
        </button>
        <button
          :class="['tab-button', { active: activeTab === 'knowledge' }]"
          @click="activeTab = 'knowledge'"
        >
          知识点管理
        </button>
      </div>

      <div class="tab-content">
        <QuestionBankView v-if="activeTab === 'question'" />
        <QuizManageView v-if="activeTab === 'quiz'" />
        <KnowledgePointManageView v-if="activeTab === 'knowledge'" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref } from 'vue'
import QuestionBankView from '../components/quiz/QuestionBankView.vue'
import QuizManageView from '../components/quiz/QuizManageView.vue'
import KnowledgePointManageView from '../components/quiz/KnowledgePointManageView.vue'

const activeTab = ref<'question' | 'quiz' | 'knowledge'>('question')
</script>

<style scoped>
.quiz-question-container {
  max-width: 1080px;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.tab-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-header {
  display: flex;
  border-bottom: 2px solid #e0e0e0;
  margin-bottom: 20px;
}

.tab-button {
  padding: 12px 24px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.tab-button:hover {
  color: #1890ff;
}

.tab-button.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}
</style>
