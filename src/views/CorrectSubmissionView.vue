<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Document, Download, View } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const submissionId = route.params.submissionId
interface SubmissionDetail {
  id: number
  studentName: string
  submittedAt: string
  content?: string
  filePath?: string
  grade?: number
  feedback?: string
  taskType?: string
  taskId?: number
  answers?: Answer[]
  description?: string
}

interface SubmissionFile {
  index: number
  originalName: string
  savedPath: string
}

interface Answer {
  id: number
  questionBody: string
  answerContent: string
  correctAnswer?: string
  type: string
  score: number
  maxScore: number
  isCorrect?: boolean
}

interface ShortAnswer {
  submissionId: number
  questionId: number
  questionContent: string
  studentAnswer: string
  maxScore: number
}

const detail = ref<SubmissionDetail | null>(null)
const grade = ref(0)
const feedback = ref('')
const downloading = ref(false)
const previewing = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFileName = ref('')
const textContent = ref('')
const wordLoading = ref(false)
const answers = ref<Answer[]>([])

// 文件相关
const submissionFiles = ref<SubmissionFile[]>([])
const filesLoading = ref(false)

// 简答题批改相关
const ungradedShortAnswers = ref<ShortAnswer[]>([])
const shortAnswerLoading = ref(false)
const gradeDialogVisible = ref(false)
const currentShortAnswer = ref<ShortAnswer | null>(null)
const gradeScore = ref(0)
const gradeFeedback = ref('')
const gradeLoading = ref(false)

// 计算属性：判断是否为测验
const isQuiz = computed(() => {
  return (
    detail.value?.taskType === 'QUIZ' || (detail.value?.answers && detail.value.answers.length > 0)
  )
})

// 计算属性：自动计算总分
const calculatedTotalScore = computed(() => {
  if (!answers.value || answers.value.length === 0) return 0

  return answers.value.reduce((total, answer) => {
    return total + (answer.score || 0)
  }, 0)
})

// 计算属性：计算满分
const calculatedMaxScore = computed(() => {
  if (!answers.value || answers.value.length === 0) return 0

  return answers.value.reduce((total, answer) => {
    return total + (answer.maxScore || 10)
  }, 0)
})

function formatDateTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

// 获取文件列表
async function fetchSubmissionFiles() {
  if (!submissionId) return

  filesLoading.value = true
  try {
    const response = await axios.get(`/api/correct/files/${submissionId}`)
    submissionFiles.value = response.data.files || []
    console.log('获取到的文件列表:', submissionFiles.value)
  } catch (error) {
    console.error('获取文件列表失败:', error)
    // 如果新接口不存在，尝试解析原有的filePath
    if (detail.value?.filePath) {
      try {
        const filePath = detail.value.filePath
        if (filePath.trim().startsWith('[')) {
          // 尝试解析JSON格式
          submissionFiles.value = JSON.parse(filePath)
        } else {
          // 单个文件的情况
          submissionFiles.value = [
            {
              index: 0,
              originalName: getFileName(filePath),
              savedPath: filePath,
            },
          ]
        }
      } catch (e) {
        console.error('解析文件路径失败:', e)
        submissionFiles.value = []
      }
    }
  } finally {
    filesLoading.value = false
  }
}

function getFileName(filePath: string) {
  if (!filePath) return '未知文件'

  // 检查是否是新的JSON格式
  if (filePath.trim().startsWith('{')) {
    try {
      // 解析JSON格式的文件路径
      const jsonData = JSON.parse(filePath)

      // 优先使用original字段（原始文件名），如果没有则使用saved字段
      const originalFileName = jsonData.originalName
      if (originalFileName && originalFileName.trim()) {
        return originalFileName
      }

      // 如果original字段为空，从saved字段中提取文件名
      const savedPath = jsonData.savedPath
      if (savedPath && savedPath.trim()) {
        return extractFileNameFromPath(savedPath)
      }

      console.warn('无法从JSON格式的文件路径中提取文件名:', filePath)
      return '未知文件'
    } catch (error) {
      console.error('解析JSON格式文件路径失败:', error)
      return '解析失败'
    }
  }

  // 原有的处理逻辑
  return extractFileNameFromPath(filePath)
}

// 从路径中提取文件名的辅助函数
function extractFileNameFromPath(filePath: string): string {
  if (!filePath) return '未知文件'

  const parts = filePath.split('/')
  const fileName = parts[parts.length - 1]

  // 如果是系统生成的文件名（包含下划线），尝试提取原始文件名
  if (fileName.includes('_')) {
    const underscoreIndex = fileName.indexOf('_')
    if (underscoreIndex > 0) {
      return fileName.substring(underscoreIndex + 1)
    }
  }

  return fileName || '未知文件'
}

function getFileType(filePath: string): string {
  if (!filePath) return '未知类型'
  const fileName = getFileName(filePath).toLowerCase()
  const extension = fileName.substring(fileName.lastIndexOf('.') + 1)

  const typeMap: Record<string, string> = {
    pdf: 'PDF文档',
    doc: 'Word文档',
    docx: 'Word文档',
    ppt: 'PowerPoint',
    pptx: 'PowerPoint',
    xls: 'Excel表格',
    xlsx: 'Excel表格',
    txt: '文本文件',
    jpg: '图片文件',
    jpeg: '图片文件',
    png: '图片文件',
    gif: '图片文件',
    mp4: '视频文件',
    avi: '视频文件',
    mov: '视频文件',
    zip: '压缩文件',
    rar: '压缩文件',
  }

  return typeMap[extension] || `${extension.toUpperCase()}文件`
}

// 文件类型判断函数
function isImageFile(fileName: string): boolean {
  if (!fileName) return false
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.') + 1)
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)
}

function isPdfFile(fileName: string): boolean {
  if (!fileName) return false
  return fileName.toLowerCase().endsWith('.pdf')
}

function isTextFile(fileName: string): boolean {
  if (!fileName) return false
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.') + 1)
  return ['txt', 'md', 'json', 'xml', 'html', 'css', 'js'].includes(ext)
}

// 获取题目类型的中文名称
function getQuestionTypeName(type: string) {
  const typeMap: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    FILL_IN_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return typeMap[type] || type
}

// 获取题目类型的颜色
function getQuestionTypeColor(type: string) {
  const colorMap: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTI_CHOICE: 'success',
    FILL_IN_BLANK: 'warning',
    SHORT_ANSWER: 'info',
  }
  return colorMap[type] || 'default'
}

// 加载文本内容
async function loadTextContent() {
  if (!isTextFile(previewFileName.value)) return

  try {
    const response = await axios.get(previewUrl.value)
    textContent.value = response.data
  } catch (error) {
    console.error('加载文本内容失败:', error)
    textContent.value = '无法加载文件内容'
  }
}

function handleImageError() {
  ElMessage.error('图片加载失败')
}

// 下载指定文件
async function downloadFile(fileIndex: number) {
  if (submissionFiles.value.length === 0) return

  const file = submissionFiles.value.find((f) => f.index === fileIndex)
  if (!file) {
    ElMessage.error('文件不存在')
    return
  }

  downloading.value = true
  try {
    const BASE_API = 'http://localhost:8080'
    const url = BASE_API + file.savedPath
    const response = await axios.get(url, {
      responseType: 'blob',
      timeout: 30000,
    })

    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/octet-stream',
    })
    const fileName = file.originalName || getFileName(file.savedPath)

    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)

    ElMessage.success('文件下载成功')
  } catch (error: unknown) {
    console.error('下载失败:', error)
    const err = error as { response?: { status?: number }; code?: string }
    let errorMsg = '文件下载失败'
    if (err.response?.status === 404) {
      errorMsg = '文件不存在'
    } else if (err.code === 'ECONNABORTED') {
      errorMsg = '下载超时，请重试'
    }
    ElMessage.error(errorMsg)
  } finally {
    downloading.value = false
  }
}

async function previewFile(fileIndex: number) {
  if (submissionFiles.value.length === 0) return

  const file = submissionFiles.value.find((f) => f.index === fileIndex)
  if (!file) {
    ElMessage.error('文件不存在')
    return
  }

  // 获取文件名，判断类型
  const fileName = (file.originalName || getFileName(file.savedPath)).toLowerCase()
  const isImage = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].some((ext) => fileName.endsWith(ext))
  const isText = ['txt', 'md', 'json', 'xml', 'html', 'css', 'js'].some((ext) =>
    fileName.endsWith(ext),
  )
  // pdf和word都不能预览，直接下载
  const isPdfOrWord =
    fileName.endsWith('.pdf') || fileName.endsWith('.doc') || fileName.endsWith('.docx')

  if (isImage || isText) {
    // 这些文件类型可以在页面内预览
    try {
      const response = await axios.get(`/api/correct/preview/${submissionId}/file/${fileIndex}`)
      const BASE_API = 'http://localhost:8080'
      previewUrl.value = BASE_API + response.data // response.data 是 /uploads/xxx
      previewFileName.value = file.originalName || getFileName(file.savedPath)
      previewVisible.value = true
      ElMessage.success('文件预览已打开')
    } catch (error: unknown) {
      console.error('预览失败:', error)
      const err = error as { response?: { status?: number } }
      let errorMsg = '文件预览失败'
      if (err.response?.status === 404) {
        errorMsg = '文件不存在'
      } else if (err.response?.status === 400) {
        errorMsg = '该文件类型不支持预览'
      }
      ElMessage.error(errorMsg)
    }
  } else if (isPdfOrWord) {
    // pdf和word都不能预览，直接下载
    ElMessage.info('该文件类型不支持在线预览，请下载后查看')
    downloadFile(fileIndex)
  } else {
    // 其他文件类型提示用户下载
    ElMessage.info('该文件类型不支持在线预览，请下载后查看')
    downloadFile(fileIndex)
  }
}

// 简答题批改相关函数
async function fetchUngradedShortAnswers() {
  if (!detail.value?.taskId) {
    console.log('[简答题批改] 没有taskId，无法获取简答题')
    return
  }

  shortAnswerLoading.value = true
  try {
    console.log('[简答题批改] 获取taskId:', detail.value.taskId)
    const res = await axios.get(`/api/correct/quiz/ungraded-short-answers/${detail.value.taskId}`)
    console.log('[简答题批改] 后端返回的未批改简答题:', res.data)
    ungradedShortAnswers.value = res.data || []
  } catch (e) {
    console.error('[简答题批改] 获取失败:', e)
    ElMessage.error('获取未批改简答题失败')
    ungradedShortAnswers.value = []
  } finally {
    shortAnswerLoading.value = false
  }
}

function openGradeDialog(row: ShortAnswer) {
  currentShortAnswer.value = row
  gradeScore.value = 0
  gradeFeedback.value = ''
  gradeDialogVisible.value = true
}

async function submitShortAnswerGrade() {
  if (!currentShortAnswer.value) return
  gradeLoading.value = true
  try {
    await axios.post('/api/correct/quiz/grade-short-answer', {
      submissionId: currentShortAnswer.value.submissionId,
      questionId: currentShortAnswer.value.questionId,
      score: gradeScore.value,
      feedback: gradeFeedback.value,
    })
    ElMessage.success('批改成功')
    gradeDialogVisible.value = false
    fetchUngradedShortAnswers()
  } catch (e) {
    console.error('[简答题批改] 提交失败:', e)
    ElMessage.error('批改失败')
  } finally {
    gradeLoading.value = false
  }
}

// 简答题数据解析函数
function getStem(questionData: ShortAnswer) {
  try {
    const content = questionData.questionContent || questionData.questionTitle || questionData
    const obj = typeof content === 'string' ? JSON.parse(content) : content
    return obj.stem || content
  } catch {
    return questionData.questionContent || questionData.questionTitle || '题目内容解析失败'
  }
}

function getAnswer(questionData: ShortAnswer) {
  try {
    const content = questionData.questionContent || questionData.questionTitle || questionData
    const obj = typeof content === 'string' ? JSON.parse(content) : content
    return obj.answer || ''
  } catch {
    return ''
  }
}

function getStudentAnswer(row: ShortAnswer) {
  return row.studentAnswer || row.answer || '无答案'
}

function getMaxScore(row: ShortAnswer) {
  return row.maxScore || row.score || 10
}

onMounted(async () => {
  try {
    const res = await axios.get(`/api/correct/detail/${submissionId}`)
    detail.value = res.data
    console.log('批改详情 detail:', detail.value)
    console.log('文件路径 filePath:', detail.value.filePath)
    console.log('是否有文件路径:', !!detail.value.filePath)
    grade.value = detail.value.grade || 0
    feedback.value = detail.value.feedback || ''
    answers.value = res.data.answers || []

    // 新增：调试正确答案
    console.log('=== 调试正确答案 ===')
    console.log('answers数组:', answers.value)
    if (answers.value && answers.value.length > 0) {
      answers.value.forEach((answer, index) => {
        console.log(`答案${index + 1}:`, {
          id: answer.id,
          questionBody: answer.questionBody,
          answerContent: answer.answerContent,
          correctAnswer: answer.correctAnswer, // 检查这个字段
          type: answer.type,
          score: answer.score,
          maxScore: answer.maxScore,
          isCorrect: answer.isCorrect,
        })
      })
    }

    // 如果是测验，自动计算总分
    if (isQuiz.value) {
      grade.value = calculatedTotalScore.value
    }
    // 获取文件列表
    await fetchSubmissionFiles()

    // 加载简答题数据
    await fetchUngradedShortAnswers()
  } catch (error) {
    console.error('加载批改详情失败:', error)
    ElMessage.error('加载批改详情失败')
  }
})

// 监听预览URL变化，加载文本内容
watch(
  () => previewUrl.value,
  (newUrl) => {
    if (newUrl && isTextFile(previewFileName.value)) {
      loadTextContent()
    }
  },
  { immediate: true },
)

// 监听答案变化，自动更新总分
watch(
  answers,
  () => {
    if (isQuiz.value) {
      grade.value = calculatedTotalScore.value
    }
  },
  { deep: true },
)

async function submitGrade() {
  try {
    await axios.post(`/api/correct/grade/${submissionId}`, {
      grade: grade.value,
      feedback: feedback.value,
      answers: answers.value.map((ans) => ({
        id: ans.id,
        score: ans.score,
      })),
    })
    ElMessage.success('批改成功')
    router.back()
  } catch (error) {
    console.error('批改失败:', error)
    ElMessage.error('批改失败')
  }
}
</script>

<template>
  <div class="correct-submission-view">
    <div class="header-bar">
      <div class="page-title">
        <el-icon style="font-size: 28px; margin-right: 8px"><i class="el-icon-edit"></i></el-icon>
        <span>{{ isQuiz ? '批改测验' : '批改作业' }}</span>
      </div>
    </div>
    <div v-if="detail" class="card">
      <div v-if="detail.description" class="info-row">
        <span class="info-label">任务内容：</span>
        <span class="content-text">{{ detail.description }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">学生：</span>
        <span>{{ detail.studentName }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">提交时间：</span>
        <span>{{ formatDateTime(detail.submittedAt) }}</span>
      </div>

      <!-- 非测验才显示作答内容 -->
      <div v-if="detail.content && !isQuiz" class="info-row">
        <span class="info-label">作答内容：</span>
        <span class="content-text">{{ detail.content }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">附件：</span>
        <div v-if="submissionFiles.length > 0" class="files-container">
          <div v-for="file in submissionFiles" :key="file.index" class="file-info">
            <el-icon class="file-icon"><Document /></el-icon>
            <div class="file-actions">
              <div class="file-header">
                <span class="file-name">{{
                  file.originalName || getFileName(file.savedPath)
                }}</span>
                <span class="file-type">{{ getFileType(file.savedPath) }}</span>
              </div>
              <div class="file-buttons">
                <el-button
                  type="primary"
                  size="small"
                  @click="downloadFile(file.index)"
                  :loading="downloading"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button
                  type="success"
                  size="small"
                  @click="previewFile(file.index)"
                  :loading="previewing"
                >
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="detail.filePath" class="file-info">
          <el-icon class="file-icon"><Document /></el-icon>
          <div class="file-actions">
            <div class="file-header">
              <span class="file-name">{{ getFileName(detail.filePath) }}</span>
              <span class="file-type">{{ getFileType(detail.filePath) }}</span>
            </div>
            <div class="file-buttons">
              <el-button
                type="primary"
                size="small"
                @click="downloadFile(0)"
                :loading="downloading"
              >
                <el-icon><Download /></el-icon>
                下载
              </el-button>
              <el-button type="success" size="small" @click="previewFile(0)" :loading="previewing">
                <el-icon><View /></el-icon>
                预览
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="no-file">
          <span class="no-file-text">学生未提交文件</span>
        </div>
      </div>
      <el-divider />

      <!-- 测验题目批改部分 -->
      <div v-if="answers.length > 0" class="quiz-answers">
        <h3>测验题目批改</h3>
        <div class="score-summary">
          <span>总分：{{ calculatedTotalScore }} / {{ calculatedMaxScore }}</span>
        </div>
        <div v-for="(ans, idx) in answers" :key="ans.id" class="quiz-answer-card">
          <div class="question-title">
            <b>第{{ idx + 1 }}题：</b> {{ ans.questionBody }}
            <span v-if="ans.maxScore !== undefined" class="score-tag"
              >（满分：{{ ans.maxScore }}分）</span
            >
            <el-tag size="small" :type="getQuestionTypeColor(ans.type)" style="margin-left: 8px">
              {{ getQuestionTypeName(ans.type) }}
            </el-tag>
          </div>
          <div class="answer-content">
            <span class="label">学生答案：</span>
            <span class="student-answer">{{ ans.answerContent || '未作答' }}</span>
          </div>

          <!-- 显示正确答案 -->
          <div v-if="ans.correctAnswer" class="correct-answer-row">
            <span class="label">正确答案：</span>
            <span class="correct-answer">{{ ans.correctAnswer }}</span>
          </div>

          <div class="answer-score-row">
            <span class="label">得分：</span>
            <el-input-number
              v-model="ans.score"
              :min="0"
              :max="ans.maxScore"
              :step="1"
              :precision="0"
              size="small"
              style="width: 100px"
            />
            <span v-if="ans.type === 'SINGLE_CHOICE' || ans.type === 'MULTI_CHOICE'">
              <el-tag
                :type="ans.isCorrect ? 'success' : 'danger'"
                size="small"
                style="margin-left: 8px"
              >
                {{ ans.isCorrect ? '正确' : '错误' }}
              </el-tag>
            </span>
          </div>
        </div>
      </div>

      <!-- 非测验才显示总分批改 -->
      <el-form v-if="!isQuiz" label-width="60px" class="grade-form">
        <el-form-item label="得分">
          <el-input-number v-model="grade" :min="0" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="feedback" type="textarea" :autosize="{ minRows: 3 }" />
        </el-form-item>
      </el-form>

      <!-- 测验显示自动计算的总分和评语 -->
      <div v-if="isQuiz" class="quiz-grade-section">
        <el-form label-width="60px" class="grade-form">
          <el-form-item label="总分">
            <span class="auto-score">{{ calculatedTotalScore }} / {{ calculatedMaxScore }}</span>
            <span class="score-note">（自动计算）</span>
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="feedback" type="textarea" :autosize="{ minRows: 3 }" />
          </el-form-item>
        </el-form>
      </div>

      <div class="btn-row">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" @click="submitGrade">保存批改</el-button>
      </div>
    </div>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewFileName"
      width="80%"
      :before-close="() => (previewVisible = false)"
    >
      <div class="preview-content">
        <!-- 图片预览 -->
        <img
          v-if="isImageFile(previewFileName)"
          :src="previewUrl"
          :alt="previewFileName"
          class="preview-image"
          @error="handleImageError"
        />

        <!-- PDF预览 -->
        <iframe
          v-else-if="isPdfFile(previewFileName)"
          :src="previewUrl"
          class="preview-pdf"
          frameborder="0"
        ></iframe>

        <!-- 文本文件预览 -->
        <div
          v-else-if="isTextFile(previewFileName)"
          class="preview-text"
          v-html="textContent"
        ></div>

        <!-- 不支持预览的文件 -->
        <div v-else class="preview-unsupported">
          <el-icon class="unsupported-icon"><Document /></el-icon>
          <p>该文件类型不支持在线预览</p>
          <el-button type="primary" @click="downloadFile(0)">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="previewVisible = false">关闭</el-button>
          <el-button type="primary" @click="downloadFile(0)">
            <el-icon><Download /></el-icon>
            下载
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 简答题批改对话框 -->
    <el-dialog v-model="gradeDialogVisible" title="批改简答题" width="500px">
      <div v-if="currentShortAnswer">
        <div style="margin-bottom: 15px"><b>题目：</b>{{ getStem(currentShortAnswer) }}</div>
        <div style="margin-bottom: 15px"><b>标准答案：</b>{{ getAnswer(currentShortAnswer) }}</div>
        <div style="margin-bottom: 15px">
          <b>学生答案：</b>{{ getStudentAnswer(currentShortAnswer) }}
        </div>
        <el-form label-width="60px">
          <el-form-item label="得分">
            <el-input-number v-model="gradeScore" :min="0" :max="getMaxScore(currentShortAnswer)" />
            <span style="margin-left: 8px">/ {{ getMaxScore(currentShortAnswer) }}</span>
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="gradeFeedback" type="textarea" :autosize="{ minRows: 2 }" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShortAnswerGrade" :loading="gradeLoading"
          >提交批改</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.correct-submission-view {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 0;
}
.header-bar {
  padding: 32px 0 0 0;
  display: flex;
  align-items: center;
  margin-bottom: 0;
}
.page-title {
  display: flex;
  align-items: center;
  font-size: 2rem;
  font-weight: bold;
  color: #409eff;
  margin-left: 40px;
}
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  width: 100vw;
  min-height: calc(100vh - 80px);
  margin: 32px 0 0 0;
  padding: 40px 6vw 32px 6vw;
}
.info-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  font-size: 16px;
}
.info-label {
  color: #909399;
  min-width: 80px;
  font-weight: 500;
}
.content-text {
  white-space: pre-wrap;
  word-break: break-all;
  color: #333;
}
.files-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.file-name {
  color: #409eff;
  font-weight: 500;
  margin-right: 8px;
}
.file-info {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex: 1;
  padding: 12px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  background: #f8f9fa;
}
.file-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.file-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.file-type {
  color: #909399;
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}
.file-buttons {
  display: flex;
  gap: 8px;
}
.file-icon {
  color: #409eff;
  font-size: 18px;
}
.file-link {
  color: #409eff;
  text-decoration: underline;
  font-weight: 500;
}
.no-file {
  display: flex;
  align-items: center;
}
.no-file-text {
  color: #c0c4cc;
  font-style: italic;
}
.grade-form {
  margin-top: 12px;
}
.btn-row {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 18px;
}

/* 测验相关样式 */
.quiz-answers {
  margin: 20px 0;
}

.quiz-answers h3 {
  color: #409eff;
  margin-bottom: 16px;
  font-size: 18px;
}

.score-summary {
  background: #f0f9ff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-weight: bold;
  color: #409eff;
}

.quiz-grade-section {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.auto-score {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.score-note {
  margin-left: 8px;
  color: #909399;
  font-size: 14px;
}

/* 预览对话框样式 */
.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  max-height: 70vh;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-pdf {
  width: 100%;
  height: 70vh;
  border: none;
}

.preview-text {
  width: 100%;
  height: 70vh;
  overflow: auto;
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px;
  color: #909399;
}

.unsupported-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.preview-unsupported p {
  margin: 0;
  font-size: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.quiz-answer-card {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  background: #fff;
}

.question-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.score-tag {
  color: #909399;
  font-size: 0.8em;
  margin-left: 8px;
}

.answer-content {
  margin-bottom: 8px;
}

.correct-answer-row {
  margin-bottom: 8px;
}

.label {
  font-weight: bold;
  color: #606266;
  min-width: 80px;
  display: inline-block;
}

.student-answer {
  background-color: #f0f9ff;
  padding: 4px 8px;
  border-radius: 4px;
  border-left: 3px solid #409eff;
  color: #409eff;
  font-weight: 500;
}

.correct-answer {
  background-color: #f0f9ff;
  padding: 4px 8px;
  border-radius: 4px;
  border-left: 3px solid #67c23a;
  color: #67c23a;
  font-weight: 500;
}

.answer-score-row {
  display: flex;
  align-items: center;
}
</style>
