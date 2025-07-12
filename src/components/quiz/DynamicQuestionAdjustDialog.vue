<!-- 曹雨荷动态题目调整 - 动态题目调整对话框组件 -->
<template>
  <el-dialog
    v-model="dialogVisible"
    title="动态题目调整"
    width="90%"
    :close-on-click-modal="false"
    class="dynamic-adjust-dialog"
  >
    <div class="dialog-content">
      <!-- 筛选区域 -->
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item course-selection">
            <label>课程选择：</label>
            <el-select
              v-model="selectedCourseIds"
              multiple
              placeholder="请选择课程（默认全部）"
              style="width: 350px"
              @change="(value: number[]) => handleCourseChange(value)"
              clearable
              :collapse-tags="true"
              :max-collapse-tags="2"
              collapse-tags-tooltip
            >
              <el-option
                v-for="course in availableCourses"
                :key="course.courseId || course.id"
                :label="course.name"
                :value="course.courseId || course.id"
              />
            </el-select>
          </div>
          <div class="filter-item">
            <el-checkbox v-model="includeAllCourses" @change="handleIncludeAllChange">
              包含所有课程
            </el-checkbox>
          </div>
          <div class="filter-item">
            <el-button size="small" @click="clearCourseSelection"> 清空选择 </el-button>
          </div>
          <div class="filter-item">
            <el-button type="primary" @click="queryAdjustableQuestions" :loading="loading">
              查询可调整题目
            </el-button>
          </div>
        </div>
      </div>

      <!-- 统计区域 -->
      <div v-if="statistics" class="statistics-section">
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-number">{{ statistics.totalQuestions }}</div>
            <div class="stat-label">总题目数</div>
          </div>
          <div class="stat-card">
            <div class="stat-number highlight">{{ statistics.needAdjustment }}</div>
            <div class="stat-label">需调整题目</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ (statistics.averageCorrectRate * 100).toFixed(1) }}%</div>
            <div class="stat-label">平均正确率</div>
          </div>
        </div>
      </div>

      <!-- 操作区域 -->
      <div v-if="questions.length > 0" class="operation-section">
        <div class="operation-bar">
          <div class="left-operations">
            <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
            <span class="selected-count">已选择 {{ selectedQuestions.length }} 题</span>
          </div>
          <div class="right-operations">
            <el-button @click="batchAcceptSuggestions" :disabled="selectedQuestions.length === 0">
              批量采纳建议
            </el-button>
            <el-button @click="batchRejectSuggestions" :disabled="selectedQuestions.length === 0">
              批量拒绝调整
            </el-button>
            <el-button type="success" @click="confirmAdjustments" :disabled="!hasAdjustments">
              确认调整 ({{ pendingAdjustments.length }})
            </el-button>
          </div>
        </div>
      </div>

      <!-- 题目列表区域 -->
      <div v-if="questions.length > 0" class="questions-section">
        <div class="question-list" :key="`list-${forceUpdateKey}`">
          <div
            v-for="question in questions"
            :key="`${question.id}-${forceUpdateKey}`"
            class="question-item"
            :class="{ selected: selectedQuestions.includes(question.id) }"
          >
            <div class="question-header">
              <div class="question-meta">
                <el-checkbox
                  :model-value="selectedQuestions.includes(question.id)"
                  @change="(checked: boolean) => handleQuestionSelect(question.id, checked)"
                />
                <span class="course-name">{{ question.courseName }}</span>
                <span class="knowledge-point">{{ question.knowledgePointName }}</span>
              </div>
              <div class="question-stats">
                <el-tag :type="getCorrectRateType(question.correctRate)" size="small">
                  正确率: {{ (question.correctRate * 100).toFixed(1) }}%
                </el-tag>
                <span class="attempts">答题次数: {{ question.totalAttempts }}</span>
              </div>
            </div>

            <div class="question-content">
              <!-- 曹雨荷动态题目调整 - 优化题目内容显示 -->
              <div class="question-text">
                <strong>题目：</strong>{{ parseQuestionBody(question.body).question }}
              </div>
              <div v-if="parseQuestionBody(question.body).options" class="question-options">
                <div class="options-title">选项：</div>
                <div
                  v-for="(option, index) in parseQuestionBody(question.body).options"
                  :key="index"
                  class="option-item"
                  :class="{
                    'correct-option': option.startsWith(parseQuestionBody(question.body).answer),
                  }"
                >
                  {{ option }}
                </div>
              </div>
              <div v-if="parseQuestionBody(question.body).answer" class="correct-answer">
                <strong>正确答案：</strong>{{ parseQuestionBody(question.body).answer }}
              </div>
            </div>

            <div class="difficulty-section">
              <!-- 曹雨荷动态题目调整 - 显示调整状态和难度 -->
              <div v-if="hasAdjustment(question.id)" class="adjustment-status">
                <el-tag type="success" size="small">
                  {{
                    getCurrentAdjustmentDifficulty(question.id) === question.suggestedDifficulty
                      ? '已采纳建议'
                      : '已自定义调整'
                  }}
                </el-tag>
              </div>

              <div class="difficulty-display">
                <div class="current-difficulty">
                  <span class="label">当前难度:</span>
                  <div class="stars">
                    <el-rate
                      v-model="question.currentDifficulty"
                      disabled
                      show-score
                      text-color="#ff9900"
                    />
                  </div>
                </div>
                <div class="suggested-difficulty">
                  <span class="label">
                    {{ hasAdjustment(question.id) ? '调整为:' : '建议难度:' }}
                  </span>
                  <div class="stars">
                    <el-rate
                      :model-value="
                        hasAdjustment(question.id)
                          ? getCurrentAdjustmentDifficulty(question.id)
                          : question.suggestedDifficulty
                      "
                      disabled
                      show-score
                      :text-color="hasAdjustment(question.id) ? '#1890ff' : '#67c23a'"
                    />
                  </div>
                </div>
              </div>
              <div class="change-reason">
                <el-tag type="info" size="small">{{ question.changeReason }}</el-tag>
              </div>
            </div>

            <!-- 选项统计图表（仅选择题） -->
            <div
              v-if="question.optionStats && Object.keys(question.optionStats).length > 0"
              class="option-stats"
            >
              <div class="stats-title">选项分布统计：</div>
              <div class="stats-bars">
                <div
                  v-for="(count, option) in getProcessedOptionStats(question.optionStats)"
                  :key="option"
                  class="stat-bar"
                >
                  <span class="option-label">{{ option }}:</span>
                  <div class="bar-container">
                    <div
                      class="bar-fill"
                      :style="{
                        width: `${(count / getTotalIndividualChoices(question.optionStats)) * 100}%`,
                      }"
                      :class="{ correct: option === parseQuestionBody(question.body).answer }"
                    ></div>
                    <span class="count">{{ count }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="question-actions">
              <el-button size="small" type="primary" @click="showCustomAdjust(question)">
                自定义调整
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading && hasQueried" class="empty-state">
        <el-empty description="暂无需要调整的题目" />
      </div>
    </div>

    <!-- 对话框底部按钮 -->
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeDialog">关闭</el-button>
        <el-button type="primary" @click="exportReport" :disabled="questions.length === 0">
          导出报告
        </el-button>
      </span>
    </template>

    <!-- 自定义调整对话框 -->
    <el-dialog v-model="showCustomDialog" title="自定义难度调整" width="500px" append-to-body>
      <div v-if="currentQuestion">
        <p><strong>题目：</strong>{{ parseQuestionBody(currentQuestion.body).question }}</p>
        <p><strong>当前难度：</strong>{{ currentQuestion.currentDifficulty }} 星</p>
        <p><strong>建议难度：</strong>{{ currentQuestion.suggestedDifficulty }} 星</p>
        <div class="custom-difficulty">
          <label>自定义难度：</label>
          <el-rate v-model="customDifficulty" show-score text-color="#ff9900" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCustomDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCustomAdjust">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
// 曹雨荷动态题目调整 - 动态题目调整对话框组件脚本
import { ref, computed, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
// 曹雨荷动态题目调整 - 简化消息提示，直接使用console.log记录操作
const showMessage = {
  success: (msg: string) => console.log('✅ ' + msg),
  info: (msg: string) => console.log('ℹ️ ' + msg),
  warning: (msg: string) => console.log('⚠️ ' + msg),
  error: (msg: string) => console.error('❌ ' + msg),
}
import { dynamicAdjustService } from '../../services/dynamicAdjust.service'
import courseService from '../../services/course.service'

// 定义组件属性
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// 响应式数据
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

// 类型定义
interface CourseOption {
  id: number
  name: string
}

interface QuestionData {
  id: number
  body: string
  currentDifficulty: number
  suggestedDifficulty: number
  correctRate: number
  totalAttempts: number
  courseName: string
  knowledgePointName: string
  changeReason: string
  optionStats?: Record<string, number>
}

interface StatisticsData {
  totalQuestions: number
  needAdjustment: number
  averageCorrectRate: number
}

interface QuestionAdjustment {
  questionId: number
  newDifficulty: number
  shouldAdjust: boolean
}

const loading = ref(false)
const hasQueried = ref(false)
const includeAllCourses = ref(true)
const selectedCourseIds = ref<number[]>([])
const availableCourses = ref<Course[]>([])

// 题目数据
const questions = ref<Question[]>([])
const statistics = ref<Statistics | null>(null)
const selectedQuestions = ref<number[]>([])
const selectAll = ref(false)

// 调整数据
const pendingAdjustments = ref<Adjustment[]>([])

// 自定义调整
const showCustomDialog = ref(false)
const currentQuestion = ref<Question | null>(null)
const customDifficulty = ref(1)

// 曹雨荷动态题目调整 - 强制更新界面的计数器
const forceUpdateKey = ref(0)

// 计算属性
const hasAdjustments = computed(() => pendingAdjustments.value.length > 0)

// 监听全选状态
watch(selectAll, (newVal) => {
  if (newVal) {
    selectedQuestions.value = questions.value.map((q) => q.id)
  } else {
    selectedQuestions.value = []
  }
})

// 监听题目选择状态
watch(
  selectedQuestions,
  (newVal) => {
    selectAll.value = newVal.length === questions.value.length && questions.value.length > 0
  },
  { deep: true },
)

// 监听对话框显示状态
watch(dialogVisible, (newVal) => {
  if (newVal) {
    loadAvailableCourses()
  } else {
    resetDialog()
  }
})

// 曹雨荷动态题目调整 - 监听课程数据变化
watch(availableCourses, (newCourses) => {
  console.log('📚 课程数据更新:', newCourses.length, '个课程')
  if (newCourses.length > 0) {
    // 强制更新界面确保下拉框正确显示
    forceUpdateKey.value++
  }
})

// 方法
const loadAvailableCourses = async () => {
  try {
    const courses = await courseService.getAllCourses()
    availableCourses.value = courses

    // 曹雨荷动态题目调整 - 调试信息：打印课程数据
    console.log('📚 加载的课程列表:', courses)
    console.log('📚 课程数量:', courses.length)
    if (courses.length > 0) {
      console.log('📚 第一个课程示例:', courses[0])
    }
  } catch (error) {
    console.error('加载课程列表失败:', error)
    showMessage.error('加载课程列表失败')
  }
}

const handleCourseChange = (selectedIds: number[]) => {
  // 曹雨荷动态题目调整 - 调试信息：课程选择变化
  console.log('🎯 课程选择变化:', selectedIds)
  console.log('🎯 当前包含所有课程状态:', includeAllCourses.value)

  // 显示所选课程的具体信息
  if (selectedIds.length > 0) {
    const selectedCourseNames = selectedIds.map((id) => {
      const course = availableCourses.value.find((c) => (c.courseId || c.id) === id)
      return course ? course.name : `未知课程(${id})`
    })
    console.log('🎯 选中的课程:', selectedCourseNames)
  }

  selectedCourseIds.value = selectedIds

  // 如果选择了具体课程，自动取消"包含所有课程"
  if (selectedIds.length > 0) {
    includeAllCourses.value = false
    console.log('🎯 已取消"包含所有课程"，切换为指定课程模式')
  } else {
    // 如果没有选择任何课程，自动选择"包含所有课程"
    includeAllCourses.value = true
    console.log('🎯 没有选择具体课程，自动切换为包含所有课程模式')
  }
}

const handleIncludeAllChange = (checked: boolean) => {
  // 曹雨荷动态题目调整 - 调试信息：包含所有课程状态变化
  console.log('🎯 包含所有课程状态变化:', checked)

  if (checked) {
    // 选择包含所有课程时，清空具体课程选择
    selectedCourseIds.value = []
    console.log('🎯 已清空具体课程选择，切换为全部课程模式')
  }
}

// 曹雨荷动态题目调整 - 清空课程选择
const clearCourseSelection = () => {
  console.log('🧹 清空课程选择')
  selectedCourseIds.value = []
  includeAllCourses.value = true
  console.log('✅ 课程选择已清空，重置为包含所有课程')
}

const queryAdjustableQuestions = async () => {
  try {
    loading.value = true
    hasQueried.value = true

    const params = {
      courseIds: includeAllCourses.value ? [] : selectedCourseIds.value,
      includeAllCourses: includeAllCourses.value,
    }

    // 曹雨荷动态题目调整 - 调试信息：查询参数
    console.log('🔍 查询参数:', params)
    console.log('🔍 包含所有课程:', includeAllCourses.value)
    console.log('🔍 选中课程IDs:', selectedCourseIds.value)

    const response = await dynamicAdjustService.queryAdjustableQuestions(params)
    questions.value = response.questions
    statistics.value = response.statistics

    // 重置选择状态
    selectedQuestions.value = []
    selectAll.value = false
    pendingAdjustments.value = []

    showMessage.success(`查询完成，找到 ${response.questions.length} 个需要调整的题目`)
  } catch (error) {
    console.error('查询可调整题目失败:', error)
    showMessage.error('查询可调整题目失败')
  } finally {
    loading.value = false
  }
}

const handleSelectAll = (checked: boolean) => {
  selectAll.value = checked
}

const handleQuestionSelect = (questionId: number, checked: boolean) => {
  if (checked) {
    if (!selectedQuestions.value.includes(questionId)) {
      selectedQuestions.value.push(questionId)
    }
  } else {
    const index = selectedQuestions.value.indexOf(questionId)
    if (index > -1) {
      selectedQuestions.value.splice(index, 1)
    }
  }

  // 曹雨荷动态题目调整 - 根据勾选状态自动管理调整建议
  updateAdjustmentForQuestion(questionId, checked)
}

// 曹雨荷动态题目调整 - 增强的题目内容解析函数
const parseQuestionBody = (body: string) => {
  try {
    const parsed = JSON.parse(body)

    // 处理选项格式：支持字符串数组和对象数组两种格式
    let formattedOptions = null
    if (parsed.options && Array.isArray(parsed.options)) {
      formattedOptions = parsed.options.map((option: any) => {
        if (typeof option === 'string') {
          return option
        } else if (typeof option === 'object' && option.label && option.text) {
          // 处理 {"label": "A", "text": "class"} 格式
          return `${option.label}. ${option.text}`
        } else if (typeof option === 'object' && option.text) {
          // 处理只有text字段的格式
          return option.text
        }
        return String(option)
      })
    }

    return {
      question: parsed.question || parsed.stem || parsed.title || '题目内容解析失败',
      options: formattedOptions,
      answer: parsed.answer || parsed.correctAnswer,
      type: parsed.type || 'unknown',
    }
  } catch (error) {
    console.error('解析题目内容失败:', error)
    return {
      question: body.length > 100 ? body.substring(0, 100) + '...' : body,
      options: null,
      answer: null,
      type: 'unknown',
    }
  }
}

const getCorrectRateType = (rate: number) => {
  if (rate >= 0.85) return 'success'
  if (rate >= 0.7) return 'warning'
  if (rate >= 0.5) return ''
  return 'danger'
}

// 曹雨荷动态题目调整 - 处理选项统计，将多选组合拆分成单个选项
const getProcessedOptionStats = (optionStats: any) => {
  const processed: { [key: string]: number } = {}

  Object.entries(optionStats).forEach(([option, count]) => {
    let shouldProcess = false

    try {
      // 尝试解析为JSON数组（多选格式：["A", "C"]）
      const optionArray = JSON.parse(option)
      if (Array.isArray(optionArray)) {
        // 多选题：将每个选项分别统计
        optionArray.forEach((singleOption: string) => {
          processed[singleOption] = (processed[singleOption] || 0) + (count as number)
        })
        shouldProcess = true
      }
    } catch {
      // JSON解析失败，继续检查其他格式
    }

    // 如果不是JSON格式，检查是否为逗号分隔的多选格式（如："D,C"）
    if (!shouldProcess && option.includes(',')) {
      const optionArray = option.split(',').map((opt) => opt.trim())
      if (optionArray.length > 1) {
        // 多选题：将每个选项分别统计
        optionArray.forEach((singleOption: string) => {
          processed[singleOption] = (processed[singleOption] || 0) + (count as number)
        })
        shouldProcess = true
      }
    }

    // 如果都不是多选格式，直接作为单选处理
    if (!shouldProcess) {
      processed[option] = count as number
    }
  })

  // 按选项标签排序（A, B, C, D...）
  const sortedKeys = Object.keys(processed).sort()
  const sortedStats: { [key: string]: number } = {}
  sortedKeys.forEach((key) => {
    sortedStats[key] = processed[key]
  })

  return sortedStats
}

// 曹雨荷动态题目调整 - 计算拆分后的总选择次数
const getTotalIndividualChoices = (optionStats: any) => {
  const processed = getProcessedOptionStats(optionStats)
  return Object.values(processed).reduce((sum: number, count: number) => sum + count, 0)
}

// 曹雨荷动态题目调整 - 获取题目的当前调整难度（自定义优先）
const getCurrentAdjustmentDifficulty = (questionId: number) => {
  const adjustment = pendingAdjustments.value.find((adj) => adj.questionId === questionId)
  return adjustment ? adjustment.newDifficulty : null
}

// 曹雨荷动态题目调整 - 检查题目是否有待调整
const hasAdjustment = (questionId: number) => {
  return pendingAdjustments.value.some((adj) => adj.questionId === questionId && adj.shouldAdjust)
}

// 曹雨荷动态题目调整 - 通过勾选状态自动管理调整建议
const updateAdjustmentForQuestion = (questionId: number, shouldAdjust: boolean) => {
  const question = questions.value.find((q) => q.id === questionId)
  if (!question) return

  const existingIndex = pendingAdjustments.value.findIndex((adj) => adj.questionId === questionId)

  if (shouldAdjust) {
    // 勾选时添加调整建议
    const adjustment = {
      questionId: questionId,
      newDifficulty: question.suggestedDifficulty,
      shouldAdjust: true,
    }

    if (existingIndex > -1) {
      pendingAdjustments.value[existingIndex] = adjustment
    } else {
      pendingAdjustments.value.push(adjustment)
    }
  } else {
    // 取消勾选时移除调整
    if (existingIndex > -1) {
      pendingAdjustments.value.splice(existingIndex, 1)
    }
  }
}

const showCustomAdjust = (question: any) => {
  currentQuestion.value = question
  customDifficulty.value = question.currentDifficulty
  showCustomDialog.value = true
}

const confirmCustomAdjust = () => {
  if (!currentQuestion.value) return

  // 曹雨荷动态题目调整 - 调试信息：自定义调整前状态
  console.log('🎯 自定义调整 - 题目ID:', currentQuestion.value.id)
  console.log('🎯 自定义调整 - 当前难度:', currentQuestion.value.currentDifficulty)
  console.log('🎯 自定义调整 - 建议难度:', currentQuestion.value.suggestedDifficulty)
  console.log('🎯 自定义调整 - 自定义难度:', customDifficulty.value)

  const existingIndex = pendingAdjustments.value.findIndex(
    (adj) => adj.questionId === currentQuestion.value.id,
  )
  const adjustment = {
    questionId: currentQuestion.value.id,
    newDifficulty: customDifficulty.value,
    shouldAdjust: true,
  }

  if (existingIndex > -1) {
    pendingAdjustments.value[existingIndex] = adjustment
    console.log('🔄 更新现有调整项')
  } else {
    pendingAdjustments.value.push(adjustment)
    console.log('➕ 添加新调整项')
  }

  // 曹雨荷动态题目调整 - 自定义调整后自动勾选该题目
  if (!selectedQuestions.value.includes(currentQuestion.value.id)) {
    selectedQuestions.value.push(currentQuestion.value.id)
    console.log('☑️ 自动勾选题目')
  }

  console.log('📝 当前待调整列表:', pendingAdjustments.value)

  // 曹雨荷动态题目调整 - 强制更新界面显示
  forceUpdateKey.value++

  showCustomDialog.value = false
  showMessage.success(`已设置自定义调整：${customDifficulty.value}星`)
}

const batchAcceptSuggestions = () => {
  // 曹雨荷动态题目调整 - 批量勾选所有已选择的题目
  selectedQuestions.value.forEach((questionId) => {
    updateAdjustmentForQuestion(questionId, true)
  })

  showMessage.success(`已批量采纳 ${selectedQuestions.value.length} 个题目的调整建议`)
}

const batchRejectSuggestions = () => {
  // 曹雨荷动态题目调整 - 批量取消勾选，移除调整建议
  const rejectedCount = selectedQuestions.value.length

  selectedQuestions.value.forEach((questionId) => {
    updateAdjustmentForQuestion(questionId, false)
  })

  // 同时清空选择状态
  selectedQuestions.value = []
  selectAll.value = false

  showMessage.info(`已批量拒绝 ${rejectedCount} 个题目的调整`)
}

const confirmAdjustments = async () => {
  if (pendingAdjustments.value.length === 0) {
    showMessage.warning('没有需要调整的题目')
    return
  }

  // 曹雨荷动态题目调整 - 调试信息：打印待调整数据
  console.log('🔧 待调整数据:', JSON.stringify(pendingAdjustments.value, null, 2))

  try {
    await ElMessageBox.confirm(
      `确认调整 ${pendingAdjustments.value.length} 个题目的难度？此操作不可撤销。`,
      '确认调整',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    loading.value = true

    // 曹雨荷动态题目调整 - 调试信息：发送请求前
    console.log('📡 发送调整请求:', {
      adjustments: pendingAdjustments.value,
    })

    const response = await dynamicAdjustService.confirmAdjustments({
      adjustments: pendingAdjustments.value,
    })

    // 曹雨荷动态题目调整 - 调试信息：响应结果
    console.log('✅ 调整响应:', response)

    showMessage.success(`调整完成！成功: ${response.successCount}, 失败: ${response.failedCount}`)

    // 重新查询更新数据
    await queryAdjustableQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认调整失败:', error)
      showMessage.error('确认调整失败')
    }
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  // TODO: 实现导出报告功能
  showMessage.info('导出功能开发中...')
}

const resetDialog = () => {
  // 曹雨荷动态题目调整 - 重置对话框状态
  console.log('🔄 重置对话框状态')

  hasQueried.value = false
  questions.value = []
  statistics.value = null
  selectedQuestions.value = []
  selectAll.value = false
  pendingAdjustments.value = []
  selectedCourseIds.value = []
  includeAllCourses.value = true

  // 强制刷新界面
  forceUpdateKey.value++

  console.log('✅ 对话框状态重置完成')
}

const closeDialog = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
/* 曹雨荷动态题目调整 - 样式定义 */
.dynamic-adjust-dialog {
  .dialog-content {
    max-height: 80vh;
    overflow-y: auto;
  }
}

.filter-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item label {
  font-weight: 500;
  color: #333;
}

/* 曹雨荷动态题目调整 - 课程选择区域样式 */
.course-selection {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.statistics-section {
  margin-bottom: 20px;
}

.stat-cards {
  display: flex;
  gap: 16px;
}

.stat-card {
  flex: 1;
  padding: 16px;
  background: white;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.stat-number.highlight {
  color: #e6a23c;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.operation-section {
  margin-bottom: 16px;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.left-operations {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-count {
  font-size: 14px;
  color: #666;
}

.right-operations {
  display: flex;
  gap: 8px;
}

.questions-section {
  .question-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

.question-item {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  background: white;
  transition: all 0.3s ease;
}

.question-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.question-item.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.course-name {
  font-weight: 500;
  color: #409eff;
}

.knowledge-point {
  color: #666;
  font-size: 14px;
}

.question-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.attempts {
  font-size: 14px;
  color: #666;
}

.question-content {
  margin-bottom: 16px;
}

/* 曹雨荷动态题目调整 - 优化题目内容样式 */
.question-text {
  font-size: 16px;
  margin-bottom: 12px;
  line-height: 1.6;
  color: #333;
}

.question-options {
  margin: 12px 0;
}

.options-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #333;
  font-size: 14px;
}

.option-item {
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 6px;
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
}

.option-item:hover {
  background: #e9ecef;
}

.correct-option {
  background: #e8f5e8 !important;
  border-left-color: #52c41a;
  font-weight: 500;
}

.correct-answer {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f0f9f0;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  font-size: 14px;
  color: #389e0d;
}

.difficulty-section {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

/* 曹雨荷动态题目调整 - 调整状态样式 */
.adjustment-status {
  margin-bottom: 8px;
  text-align: center;
}

.difficulty-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.current-difficulty,
.suggested-difficulty {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-size: 14px;
  font-weight: 500;
}

.stars {
  display: flex;
  align-items: center;
}

.change-reason {
  text-align: center;
}

.option-stats {
  margin-bottom: 16px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 6px;
}

.stats-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.stats-bars {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-label {
  min-width: 20px;
  font-size: 14px;
  font-weight: 500;
}

.bar-container {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-fill {
  height: 20px;
  background: #e0e0e0;
  border-radius: 10px;
  position: relative;
  min-width: 2px;
  transition: width 0.3s ease;
}

.bar-fill.correct {
  background: #67c23a;
}

.count {
  font-size: 12px;
  color: #666;
  min-width: 20px;
}

.question-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.custom-difficulty {
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.custom-difficulty label {
  font-weight: 500;
}
</style>
