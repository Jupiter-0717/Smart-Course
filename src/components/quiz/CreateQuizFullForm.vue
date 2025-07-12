<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>新建整张试卷</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>
      <div class="modal-body">
        <form class="quiz-form">
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
            <label>课程 *</label>
            <el-select
              v-model="formData.courseId"
              filterable
              placeholder="请选择课程"
              style="width: 100%"
              @change="onCourseChange"
            >
              <el-option
                v-for="c in courseList"
                :key="c.courseId"
                :label="c.name"
                :value="c.courseId"
              />
            </el-select>
          </div>
          <div class="form-group">
            <label>选择题目 *</label>
            <el-select
              v-model="selectedQuestionIds"
              multiple
              filterable
              remote
              :remote-method="searchQuestions"
              :loading="loadingQuestions"
              placeholder="搜索并选择题目"
              style="width: 100%"
              @change="onQuestionSelect"
            >
              <el-option v-for="q in questionOptions" :key="q.id" :label="q.stem" :value="q.id">
                <div style="display: flex; flex-direction: column">
                  <span>{{ q.stem }}</span>
                  <span style="color: #999; font-size: 12px">ID: {{ q.id }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
          <div v-if="selectedQuestions.length" class="selected-questions-section">
            <h4>已选题目</h4>
            <el-table :data="selectedQuestions" border style="width: 100%">
              <el-table-column label="#" type="index" width="40" />
              <el-table-column label="题干" prop="stem" />
              <el-table-column label="分数" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="1" :max="100" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="顺序" width="100">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.orderIndex"
                    :min="1"
                    :max="selectedQuestions.length"
                    size="small"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="danger" size="small" @click="removeQuestion(row.id)"
                    >移除</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top: 8px; text-align: right; font-weight: bold; color: #409eff">
              总分：{{ totalPoints }} 分
            </div>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-secondary">取消</button>
        <button @click="handleSubmit" class="btn btn-primary">
          {{ isSubmitting ? '保存中...' : '创建' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus/es'
import * as courseService from '../../services/course.service'
import { questionBankService } from '../../services/questionBank.service'
import type { QuestionResponse } from '../../types/question'
import type { Course } from '../../types/course'
import { quizService } from '../../services/quiz.service'
// 你需要有courseService和questionBankService的API支持
// import { questionBankService } from '../../services/questionBank.service'

const props = defineProps<{ presetQuestionIds?: number[] }>() // 支持题库管理界面批量选题
const emit = defineEmits(['close', 'success'])

const formData = reactive({
  title: '',
  courseId: null as number | null,
})

const courseList = ref<Course[]>([])
const questionOptions = ref<(QuestionResponse & { stem: string })[]>([])
const selectedQuestionIds = ref<number[]>(props.presetQuestionIds || [])
const selectedQuestions = ref<SelectedQuizQuestion[]>([])
const loadingQuestions = ref(false)
const isSubmitting = ref(false)

interface SelectedQuizQuestion {
  id: number
  stem: string
  score: number
  orderIndex: number
}

const totalPoints = computed(() =>
  selectedQuestions.value.reduce(
    (sum: number, q: SelectedQuizQuestion) => sum + (Number(q.score) || 0),
    0,
  ),
)

onMounted(async () => {
  // 获取当前教师的课程列表
  try {
    const res = await courseService.getCoursesByTeacher()
    courseList.value = res || []
  } catch {
    courseList.value = []
  }
})

const onCourseChange = async () => {
  selectedQuestionIds.value = []
  selectedQuestions.value = []
  // 课程切换时自动加载全部题目
  await loadAllQuestionsForCourse()
}

const loadAllQuestionsForCourse = async () => {
  if (!formData.courseId) {
    questionOptions.value = []
    return
  }
  loadingQuestions.value = true
  try {
    const res = await questionBankService.getQuestionsByCourse({
      courseId: formData.courseId,
    })
    console.log('getQuestionsByCourse返回：', res)
    questionOptions.value = (res || []).map((q) => {
      let stem = ''
      try {
        stem = JSON.parse(q.body).stem || ''
      } catch {
        stem = q.body
      }
      return { ...q, stem }
    })
  } finally {
    loadingQuestions.value = false
  }
}

const searchQuestions = async (keyword: string) => {
  if (!formData.courseId) {
    questionOptions.value = []
    return
  }
  loadingQuestions.value = true
  try {
    if (!keyword) {
      await loadAllQuestionsForCourse()
      return
    }
    const res = await questionBankService.searchQuestions({
      data: {
        courseId: formData.courseId,
        page: 0,
        size: 100,
      },
    })
    console.log('searchQuestions返回：', res)
    questionOptions.value = (res.content || [])
      .map((q) => {
        let stem = ''
        try {
          stem = JSON.parse(q.body).stem || ''
        } catch {
          stem = q.body
        }
        return { ...q, stem }
      })
      .filter((q) => q.stem.includes(keyword))
  } finally {
    loadingQuestions.value = false
  }
}

// 课程变化时自动加载全部题目
watch(
  () => formData.courseId,
  () => {
    if (formData.courseId) {
      loadAllQuestionsForCourse()
    } else {
      questionOptions.value = []
    }
  },
)

const onQuestionSelect = () => {
  selectedQuestions.value = selectedQuestionIds.value.map((id, idx) => {
    const q = questionOptions.value.find((item) => item.id === id)
    // 保持已选题目的分数不丢失
    const old = selectedQuestions.value.find((item) => item.id === id)
    return {
      id: q?.id || 0,
      stem: q?.stem || '',
      score: old?.score || 10,
      orderIndex: idx + 1,
    } as SelectedQuizQuestion
  })
}

const removeQuestion = (id: number) => {
  selectedQuestionIds.value = selectedQuestionIds.value.filter((qid) => qid !== id)
  selectedQuestions.value = selectedQuestions.value.filter((q) => q.id !== id)
}

watch(selectedQuestionIds, onQuestionSelect)

const handleSubmit = async () => {
  if (!formData.title || !formData.courseId || !selectedQuestions.value.length) {
    ElMessage.error('请填写完整信息并选择题目')
    return
  }
  try {
    isSubmitting.value = true
    const payload = {
      data: {
        title: formData.title,
        totalPoints: totalPoints.value,
        questions: selectedQuestions.value.map((q) => ({
          questionId: q.id,
          score: q.score,
          orderIndex: q.orderIndex,
        })),
      },
    }
    await quizService.createFullQuiz(payload)
    ElMessage.success('试卷创建成功')
    emit('success')
    emit('close')
  } catch (error) {
    console.error('创建试卷失败:', error)
    ElMessage.error('创建失败')
  } finally {
    isSubmitting.value = false
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
  max-width: 900px;
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
.selected-questions-section {
  margin-top: 20px;
}
</style>
