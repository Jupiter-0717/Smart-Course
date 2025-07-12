// 曹雨荷动态题目调整 - 动态题目调整API服务
import axios from 'axios'

const API_BASE_URL = '/api/teacher/question-difficulty-adjust'
const IS_DEVELOPMENT = import.meta.env.DEV

// 定义接口类型
export interface QueryRequest {
  courseIds: number[]
  includeAllCourses: boolean
}

export interface QuestionAdjustment {
  questionId: number
  newDifficulty: number
  shouldAdjust: boolean
}

export interface ConfirmRequest {
  adjustments: QuestionAdjustment[]
}

export interface Question {
  id: number
  body: string
  currentDifficulty: number
  suggestedDifficulty: number
  correctRate: number
  totalAttempts: number
  wrongAttempts: number
  optionStats?: Record<string, number>
  courseName: string
  knowledgePointName: string
  changeReason: string
}

export interface Statistics {
  totalQuestions: number
  needAdjustment: number
  averageCorrectRate: number
}

export interface QueryResponse {
  questions: Question[]
  statistics: Statistics
}

export interface AdjustmentDetail {
  questionId: number
  status: 'success' | 'failed' | 'skipped'
  oldDifficulty: number | null
  newDifficulty: number | null
}

export interface ConfirmResponse {
  successCount: number
  failedCount: number
  details: AdjustmentDetail[]
}

export interface HealthResponse {
  status: string
  service: string
  timestamp: number
}

export interface ErrorResponse {
  success: false
  errorCode: string
  message: string
  timestamp: number
  details?: Record<string, string>
}

class DynamicAdjustService {
  /**
   * 健康检查接口
   */
  async healthCheck(): Promise<HealthResponse> {
    try {
      const response = await axios.get(`${API_BASE_URL}/health`)
      return response.data
    } catch (error) {
      console.error('Health check failed:', error)
      throw this.handleError(error)
    }
  }

  /**
   * 查询可调整题目及建议
   */
  async queryAdjustableQuestions(params: QueryRequest): Promise<QueryResponse> {
    try {
      // 在调用业务接口前先检查服务状态
      await this.healthCheck()

      const response = await axios.post(`${API_BASE_URL}/query`, params)
      return response.data
    } catch (error) {
      console.error('Query adjustable questions failed:', error)
      throw this.handleError(error)
    }
  }

  /**
   * 教师确认调整题目难度
   */
  async confirmAdjustments(params: ConfirmRequest): Promise<ConfirmResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/confirm`, params)
      return response.data
    } catch (error) {
      console.error('Confirm adjustments failed:', error)
      throw this.handleError(error)
    }
  }

  /**
   * 解析题目内容
   */
  parseQuestionBody(body: string) {
    try {
      const questionData = JSON.parse(body)
      return {
        question: questionData.question || '',
        options: questionData.options || null,
        answer: questionData.answer || null,
        type: questionData.type || 'unknown',
      }
    } catch (error) {
      console.error('Parse question body failed:', error)
      return {
        question: body,
        options: null,
        answer: null,
        type: 'unknown',
      }
    }
  }

  /**
   * 根据正确率获取难度星级
   */
  getDifficultyByCorrectRate(correctRate: number): number {
    if (correctRate >= 0.85) return 1 // 简单
    if (correctRate >= 0.7) return 2 // 较简单
    if (correctRate >= 0.5) return 3 // 中等
    if (correctRate >= 0.3) return 4 // 较难
    return 5 // 困难
  }

  /**
   * 获取难度等级描述
   */
  getDifficultyDescription(difficulty: number): string {
    const descriptions = {
      1: '简单题目，正确率≥85%',
      2: '较简单，正确率70-85%',
      3: '中等难度，正确率50-70%',
      4: '较难题目，正确率30-50%',
      5: '困难题目，正确率≤30%',
    }
    return descriptions[difficulty as keyof typeof descriptions] || '未知难度'
  }

  /**
   * 统一错误处理
   */
  private handleError(error: unknown): Error {
    if (error && typeof error === 'object' && 'response' in error) {
      const axiosError = error as { response?: { data?: ErrorResponse } }
      if (axiosError.response?.data) {
        const errorData = axiosError.response.data
        return new Error(errorData.message || '请求失败')
      }
    }

    if (error && typeof error === 'object' && 'message' in error) {
      const messageError = error as { message: string }
      return new Error(messageError.message)
    }

    return new Error('网络请求失败，请检查网络连接')
  }

  /**
   * 验证调整参数
   */
  validateAdjustments(adjustments: QuestionAdjustment[]): string[] {
    const errors: string[] = []

    if (!Array.isArray(adjustments) || adjustments.length === 0) {
      errors.push('调整列表不能为空')
      return errors
    }

    adjustments.forEach((adj, index) => {
      if (!adj.questionId || adj.questionId <= 0) {
        errors.push(`第${index + 1}个调整项的题目ID无效`)
      }

      if (adj.shouldAdjust && (adj.newDifficulty < 1 || adj.newDifficulty > 5)) {
        errors.push(`第${index + 1}个调整项的难度值必须在1-5之间`)
      }
    })

    return errors
  }

  /**
   * 批量设置调整建议
   */
  createBatchAdjustments(
    questions: Question[],
    selectedIds: number[],
    type: 'accept' | 'reject',
  ): QuestionAdjustment[] {
    return questions
      .filter((q) => selectedIds.includes(q.id))
      .map((q) => ({
        questionId: q.id,
        newDifficulty: type === 'accept' ? q.suggestedDifficulty : q.currentDifficulty,
        shouldAdjust: type === 'accept',
      }))
  }

  /**
   * 生成调整报告数据
   */
  generateReport(questions: Question[], adjustments: QuestionAdjustment[]) {
    const adjustmentMap = new Map(adjustments.map((adj) => [adj.questionId, adj]))

    return questions.map((q) => {
      const adjustment = adjustmentMap.get(q.id)
      return {
        题目ID: q.id,
        课程名称: q.courseName,
        知识点: q.knowledgePointName,
        题目内容: this.parseQuestionBody(q.body).question,
        当前难度: q.currentDifficulty,
        建议难度: q.suggestedDifficulty,
        调整后难度: adjustment?.shouldAdjust ? adjustment.newDifficulty : q.currentDifficulty,
        正确率: `${(q.correctRate * 100).toFixed(1)}%`,
        答题次数: q.totalAttempts,
        调整原因: q.changeReason,
        是否调整: adjustment?.shouldAdjust ? '是' : '否',
      }
    })
  }
}

// 创建并导出服务实例
export const dynamicAdjustService = new DynamicAdjustService()
export default dynamicAdjustService
