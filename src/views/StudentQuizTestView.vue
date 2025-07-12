<template>
  <div class="student-quiz-test">
    <!-- 曹雨荷课堂测验功能 -->
    <div class="quiz-test-header">
      <!-- 曹雨荷课堂测验功能 -->
      <el-tabs v-model="activeTab">
        <!-- 曹雨荷课堂测验功能 -->
        <el-tab-pane label="测验列表" name="list">
          <!-- 曹雨荷课堂测验功能 -->
          <div class="toolbar">
            <!-- 曹雨荷课堂测验功能 -->
            <el-select
              v-model="selectedCourseId"
              placeholder="全部课程"
              style="width: 150px; margin-right: 10px"
              @change="fetchQuizTasks"
            >
              <el-option label="全部课程" :value="''" />
              <el-option
                v-for="course in courseList"
                :key="course.courseId"
                :label="course.courseName"
                :value="course.courseId ?? ''"
              />
            </el-select>
            <!-- 状态筛选，曹雨荷课堂测验功能 -->
            <el-select
              v-model="selectedStatus"
              placeholder="全部状态"
              style="width: 120px; margin-right: 10px"
            >
              <el-option label="全部状态" value="ALL" />
              <el-option label="未完成" value="NOT_STARTED" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
            <el-button type="primary" @click="fetchQuizTasks">筛选</el-button>
          </div>
          <el-table :data="quizTaskList" style="width: 100%; margin-top: 20px">
            <!-- 曹雨荷课堂测验功能 -->
            <el-table-column
              prop="quizTitle"
              label="测验标题"
              align="center"
              header-align="center"
            />
            <el-table-column prop="courseName" label="课程" align="center" header-align="center" />
            <el-table-column prop="dueDate" label="截止时间" align="center" header-align="center" />
            <el-table-column
              prop="totalQuestions"
              label="总题数"
              align="center"
              header-align="center"
            />
            <el-table-column prop="totalScore" label="总分" align="center" header-align="center" />
            <!-- 状态列，曹雨荷课堂测验功能 -->
            <el-table-column prop="status" label="状态" align="center" header-align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'COMPLETED' ? 'success' : 'info'">
                  {{ scope.row.status === 'COMPLETED' ? '已完成' : '未完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" header-align="center">
              <template #default="scope">
                <el-button type="primary" size="small" @click="openQuizDialog(scope.row)">
                  开始测验
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <!-- 测验详情弹窗 -->
          <el-dialog
            v-model="quizDialogVisible"
            title="测验答题"
            width="800px"
            :close-on-click-modal="false"
          >
            <div v-if="quizDetail">
              <h3 style="margin-bottom: 24px">{{ quizDetail.title }}</h3>
              <div
                v-for="(question, idx) in quizDetail.questions"
                :key="question.questionId"
                class="quiz-question-block"
              >
                <!-- 题干 -->
                <div class="quiz-question-title">
                  <b>第{{ idx + 1 }}题：</b>{{ parseQuestionBody(question.body).stem }}
                  <span style="margin-left: 12px; color: #999; font-size: 13px"
                  >({{ getTypeText(question.type) }})</span
                  >
                </div>
                <!-- 单选题 -->
                <div v-if="question.type === 'SINGLE_CHOICE'">
                  <el-radio-group v-model="answers[question.questionId]">
                    <el-radio
                      v-for="opt in parseQuestionBody(question.body).options"
                      :key="opt.label"
                      :label="opt.label"
                    >
                      {{ opt.label }}. {{ opt.text || '' }}
                    </el-radio>
                  </el-radio-group>
                </div>
                <!-- 多选题 -->
                <div v-else-if="question.type === 'MULTI_CHOICE'">
                  <el-checkbox-group v-model="answers[question.questionId]">
                    <el-checkbox
                      v-for="opt in parseQuestionBody(question.body).options"
                      :key="opt.label"
                      :label="opt.label"
                    >
                      {{ opt.label }}. {{ opt.text || '' }}
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
                <!-- 填空题 -->
                <div v-else-if="question.type === 'FILL_IN_BLANK'">
                  <el-input
                    v-model="answers[question.questionId]"
                    placeholder="请输入答案"
                    style="width: 300px"
                  />
                </div>
                <!-- 简答题 -->
                <div v-else-if="question.type === 'SHORT_ANSWER'">
                  <el-input
                    type="textarea"
                    v-model="answers[question.questionId]"
                    placeholder="请输入答案"
                    :rows="3"
                    style="width: 100%; max-width: 600px"
                  />
                </div>
                <!-- 其他题型兜底 -->
                <div v-else style="color: #999">暂不支持的题型</div>
                <el-divider style="margin: 18px 0 8px 0" />
              </div>
              <div style="text-align: right">
                <el-button @click="quizDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitQuiz" :loading="submitLoading"
                >提交测验</el-button
                >
              </div>
            </div>
            <div v-else>加载中...</div>
          </el-dialog>
        </el-tab-pane>
        <el-tab-pane label="历史成绩" name="history">
          <!-- 曹雨荷课堂测验功能 -->
          <div class="toolbar">
            <!-- 曹雨荷课堂测验功能 -->
            <el-select
              v-model="selectedCourseIdHistory"
              placeholder="全部课程"
              style="width: 150px; margin-right: 10px"
              @change="fetchSubmissionList"
            >
              <el-option label="全部课程" :value="''" />
              <el-option
                v-for="course in courseList"
                :key="course.courseId"
                :label="course.courseName"
                :value="course.courseId ?? ''"
              />
            </el-select>
            <el-button type="primary" @click="fetchSubmissionList">筛选</el-button>
          </div>
          <el-table :data="submissionList" style="width: 100%; margin-top: 20px">
            <!-- 曹雨荷课堂测验功能 -->
            <el-table-column
              prop="quizTitle"
              label="测验标题"
              align="center"
              header-align="center"
            />
            <el-table-column prop="courseName" label="课程" align="center" header-align="center" />
            <el-table-column
              prop="submitTime"
              label="提交时间"
              align="center"
              header-align="center"
            />
            <el-table-column prop="score" label="得分" align="center" header-align="center" />
            <el-table-column label="操作" align="center" header-align="center">
              <template #default="scope">
                <el-button type="primary" size="small" @click="openSubmissionDetail(scope.row)"
                >查看详情</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <!-- 提交详情弹窗 -->
          <el-dialog
            v-model="submissionDetailDialogVisible"
            title="测验详情"
            width="800px"
            :close-on-click-modal="false"
          >
            <div v-if="submissionDetail">
              <h3>{{ submissionDetail.quizTitle }}</h3>
              <div style="margin-bottom: 8px; color: #666">
                总分：<span style="color: #faad14">{{ submissionDetail.totalScore }}</span> /
                {{ submissionDetail.maxScore }}
                <span style="margin-left: 24px">提交时间：{{ submissionDetail.submittedAt }}</span>
                <!-- 状态显示已注释，曹雨荷课堂测验功能修复 -->
                <!-- <span style="margin-left: 24px"
                  >状态：{{ getStatusText(submissionDetail.status) }}</span
                > -->
              </div>
              <el-divider />
              <div
                v-for="(q, idx) in submissionDetail.results"
                :key="q.questionId"
                class="quiz-question-block"
              >
                <div class="quiz-question-title">
                  <b>第{{ idx + 1 }}题</b>（{{ getTypeText(q.questionType) }}）
                  <span style="margin-left: 18px; color: #faad14"
                  >得分：{{ q.score }}/{{ q.maxScore }}</span
                  >
                  <span v-if="q.isCorrect === true" style="color: #67c23a; margin-left: 12px"
                  >✔ 正确</span
                  >
                  <span v-else-if="q.isCorrect === false" style="color: #f56c6c; margin-left: 12px"
                  >✘ 错误</span
                  >
                </div>
                <div style="margin-bottom: 4px">
                  你的答案：<span style="color: #409eff">{{ q.studentAnswer }}</span>
                </div>
                <div v-if="q.correctAnswer" style="margin-bottom: 4px">
                  正确答案：<span style="color: #67c23a">{{ q.correctAnswer }}</span>
                </div>
                <div v-if="q.feedback" style="margin-bottom: 4px; color: #e67e22">
                  评语：{{ q.feedback }}
                </div>
                <el-divider style="margin: 12px 0 8px 0" />
              </div>
              <div style="text-align: right">
                <el-button @click="submissionDetailDialogVisible = false">关闭</el-button>
              </div>
            </div>
            <div v-else>加载中...</div>
          </el-dialog>
        </el-tab-pane>
        <el-tab-pane label="其他功能" name="other">
          <!-- 曹雨荷课堂测验功能 -->
          <div style="padding: 40px; text-align: center">其他功能开发中...</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus/es'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()
const activeTab = ref('list') // 曹雨荷课堂测验功能
interface StudentCourseDTO {
  courseId: number
  code: string
  courseName: string
}
interface QuizTaskItem {
  id: number
  quizTitle: string
  courseName: string
  dueDate: string
  status: string // 测验状态，曹雨荷课堂测验功能
  totalQuestions: number
  totalScore: number
  quizId?: number
  taskId?: number
}
interface QuizQuestionDetail {
  questionId: number
  type: string
  body: string
}
interface QuizDetailResponse {
  id: number
  title: string
  questions: QuizQuestionDetail[]
}
const courseList = ref<StudentCourseDTO[]>([])
const selectedCourseId = ref('') // 选中的课程ID
const selectedStatus = ref('ALL') // 选中的状态，曹雨荷课堂测验功能
const quizTaskList = ref<QuizTaskItem[]>([])
const quizDialogVisible = ref(false) // 测验弹窗显示
const quizDetail = ref<QuizDetailResponse | null>(null) // 当前测验详情
const answers = ref<Record<number, string | string[]>>({}) // 答案，单选/填空/简答为string，多选为string[]
const submitLoading = ref(false)

// 获取学生ID
const studentId = ref<number | null>(null)
if (authStore.user && authStore.user.studentId) {
  studentId.value = authStore.user.studentId
}

// 解析题目内容
function parseQuestionBody(body: string) {
  try {
    return JSON.parse(body)
  } catch {
    // 兼容body末尾多余字符（如问号），曹雨荷课堂测验功能修复
    try {
      const fixed = body
        .trim()
        .replace(/[\uFF1F\?]+$/, '')
        .replace(/\s+$/, '')
      return JSON.parse(fixed)
    } catch {
      return { stem: '', options: [], answer: '', explanation: '' }
    }
  }
}

// 打开测验弹窗，拉取测验详情
const openQuizDialog = async (row: QuizTaskItem) => {
  // 状态判断，曹雨荷课堂测验功能
  if (row.status === 'COMPLETED') {
    ElMessage.warning('你已经完成了该任务，不可重复提交')
    return
  }
  quizDialogVisible.value = true
  quizDetail.value = null
  answers.value = {}
  // 保存当前任务的taskId，用于提交时使用
  const currentTaskId = row.taskId || row.id
  try {
    const res = await axios.post(
      `http://localhost:8080/api/student/quiz/quizzes/detail-by-task?studentId=${studentId.value}`,
      {
        taskId: currentTaskId, // 兼容taskId/id
      },
    ) // 端口写死，曹雨荷课堂测验功能修复
    quizDetail.value = res.data
    // 将taskId保存到quizDetail中，供提交时使用
    if (quizDetail.value) {
      ;(quizDetail.value as QuizDetailResponse & { taskId?: number }).taskId = currentTaskId
    }
    // 初始化答案对象
    if (quizDetail.value && quizDetail.value.questions) {
      quizDetail.value.questions.forEach((q) => {
        answers.value[q.questionId] = q.type === 'MULTI_CHOICE' ? [] : ''
      })
    }
  } catch {
    ElMessage.error('获取测验详情失败')
    quizDialogVisible.value = false
  }
}

// 提交测验
const submitQuiz = async () => {
  if (!quizDetail.value) return
  submitLoading.value = true
  try {
    const answerArr = quizDetail.value.questions.map((q) => {
      let ans = answers.value[q.questionId]
      // 多选题答案转为英文逗号分隔字符串，曹雨荷课堂测验功能修复
      if (q.type === 'MULTI_CHOICE' && Array.isArray(ans)) {
        ans = ans.join(',')
      }
      return {
        questionId: q.questionId,
        answerContent: ans,
      }
    })
    await axios.post('http://localhost:8080/api/student/quiz/submit', {
      studentId: studentId.value,
      quizId: quizDetail.value.id,
      taskId:
        (quizDetail.value as QuizDetailResponse & { taskId?: number }).taskId ||
        quizDetail.value.id, // 使用保存的taskId，如果没有则使用id
      answers: answerArr,
    }) // 端口写死，曹雨荷课堂测验功能修复
    ElMessage.success('提交成功！')
    quizDialogVisible.value = false
    fetchQuizTasks() // 刷新列表
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

// 获取所有课程
const fetchCourses = async () => {
  if (!studentId.value) {
    console.error('studentId 为空，无法获取课程列表', studentId.value)
    return
  }
  try {
    const res = await axios.get(`http://localhost:8080/api/student/${studentId.value}/learn/course`)
    console.log('课程接口返回数据:', res.data)
    courseList.value = (res.data || []).filter((c: StudentCourseDTO) => c.courseId != null)
    if (courseList.value.length === 0) {
      console.warn('课程列表为空，可能是接口无数据或数据结构不符', res.data)
    }
  } catch (e) {
    console.error('获取课程列表失败', e)
    ElMessage.error('获取课程列表失败: ' + (e || '未知错误'))
  }
}

// 获取测验任务
const fetchQuizTasks = async () => {
  if (!studentId.value) return
  try {
    const params = {
      studentId: studentId.value,
      courseId: selectedCourseId.value || undefined,
      page: 0,
      size: 20,
      status: selectedStatus.value || 'ALL', // 状态筛选，曹雨荷课堂测验功能
    }
    const res = await axios.post('http://localhost:8080/api/student/quiz/tasks', params) // 端口写死，曹雨荷课堂测验功能修复
    quizTaskList.value = (res.data && (res.data.quizTasks || res.data.tasks)) || []
  } catch {
    ElMessage.error('获取测验任务失败')
  }
}

// 题型中文名
function getTypeText(type: string) {
  switch (type) {
    case 'SINGLE_CHOICE':
      return '单选题'
    case 'MULTI_CHOICE':
      return '多选题'
    case 'FILL_IN_BLANK':
      return '填空题'
    case 'SHORT_ANSWER':
      return '简答题'
    default:
      return type
  }
}

// 历史成绩相关
const selectedCourseIdHistory = ref('') // 历史tab选中的课程ID
interface SubmissionListItem {
  submissionId: number
  quizTitle: string
  courseName: string
  submitTime: string
  score: number
}
interface SubmissionDetailQuestion {
  questionId: number
  questionType: string
  stem?: string
  question?: string
  studentAnswer: string
  correctAnswer: string | null
  isCorrect: boolean | null
  score: number
  maxScore: number
  analysis?: string
  feedback?: string | null
}
interface SubmissionDetail {
  quizTitle: string
  totalScore: number
  maxScore: number
  submittedAt: string
  // status字段已注释，曹雨荷课堂测验功能修复
  // status: string
  results: SubmissionDetailQuestion[]
}
const submissionList = ref<SubmissionListItem[]>([])
const submissionDetailDialogVisible = ref(false)
const submissionDetail = ref<SubmissionDetail | null>(null)

// 获取提交历史列表
const fetchSubmissionList = async () => {
  if (!studentId.value) return
  try {
    const params = {
      studentId: studentId.value,
      courseId: selectedCourseIdHistory.value || undefined,
      page: 0,
      size: 20,
    }
    const res = await axios.post('http://localhost:8080/api/student/quiz/submissions', params) // 端口写死，曹雨荷课堂测验功能修复
    submissionList.value = res.data?.submissions || []
  } catch {
    ElMessage.error('获取历史成绩失败')
  }
}

// 查看提交详情
const openSubmissionDetail = async (row: SubmissionListItem) => {
  submissionDetailDialogVisible.value = true
  submissionDetail.value = null
  try {
    const res = await axios.post('http://localhost:8080/api/student/quiz/submission/detail', {
      submissionId: row.submissionId,
      studentId: studentId.value, // 曹雨荷课堂测验功能修复，带上学生id
    }) // 端口写死，曹雨荷课堂测验功能修复
    submissionDetail.value = res.data
  } catch {
    ElMessage.error('获取详情失败')
    submissionDetailDialogVisible.value = false
  }
}

// 状态文本转换函数已注释，曹雨荷课堂测验功能修复
// function getStatusText(status: string) {
//   switch (status) {
//     case 'PENDING':
//       return '未完成'
//     case 'COMPLETED':
//       return '已完成'
//     default:
//       return status
//   }
// }

onMounted(async () => {
  console.log('authStore.user:', authStore.user)
  console.log('studentId:', studentId.value)
  await fetchCourses()
  await fetchQuizTasks()
})

// tab切换时自动刷新历史成绩
watch(activeTab, (val) => {
  if (val === 'history') fetchSubmissionList()
})
</script>

<style scoped>
.student-quiz-test {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}
.quiz-test-header {
  margin-bottom: 20px;
}
.toolbar {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
:deep(.el-table th),
:deep(.el-table td) {
  text-align: center !important;
  vertical-align: middle !important;
}
.quiz-question-block {
  margin-bottom: 12px;
  padding: 12px 0 0 0;
}
.quiz-question-title {
  font-weight: 500;
  margin-bottom: 8px;
  font-size: 15px;
}
</style>
<!-- 曹雨荷课堂测验功能 -->
