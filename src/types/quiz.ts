// 该文件用于试卷与题库功能--曹雨荷

import type { QuestionType, Difficulty } from './question'

export interface QuizResponse {
  id: number
  title: string
  creatorId: number
  creatorName: string
  totalPoints: number
  questionCount: number
  courseId: number // 新增
}

export interface QuizQuestionDetail {
  questionId: number
  type: QuestionType
  body: string // JSON字符串
  difficulty: Difficulty
  score: number
  orderIndex: number
}

export interface QuizDetailResponse {
  id: number
  title: string
  creatorId: number
  creatorName: string
  totalPoints: number
  questions: QuizQuestionDetail[]
  courseId: number // 新增
}

// 创建试卷请求
export interface CreateQuizRequest {
  data: {
    title: string
    creatorId: number
    totalPoints: number
    courseId: number // 新增
  }
}

// 创建完整试卷请求（一次性创建试卷并添加所有题目）
export interface CreateFullQuizRequest {
  data: {
    title: string
    creatorId: number
    totalPoints: number
    questions: {
      questionId: number
      score: number
      orderIndex: number
    }[]
  }
}

// 试卷详情请求
export interface QuizDetailRequest {
  quizId: number
  teacherId: number
}

// 编辑试卷请求
export interface UpdateQuizRequest {
  quizId: number
  data: Partial<CreateQuizRequest['data']>
  teacherId: number
}

// 删除试卷请求
export interface DeleteQuizRequest {
  quizId: number
  teacherId: number
}

// 教师试卷列表
export interface ListByTeacherRequest {
  teacherId: number
}

// 添加题目到试卷
export interface AddQuestionToQuizRequest {
  quizId: number
  data: {
    questionId: number
    score: number
    orderIndex: number
  }
  teacherId: number
}

// 移除题目
export interface RemoveQuestionFromQuizRequest {
  quizId: number
  questionId: number
  teacherId: number
}

// 设置分数顺序
export interface SetQuestionSettingsRequest {
  quizId: number
  data: {
    questionId: number
    score: number
    orderIndex: number
  }
  teacherId: number
}

// 复制试卷
export interface CopyQuizRequest {
  quizId: number
  newTitle: string
  teacherId: number
}

// 按课程查试卷
export interface ByCourseQuizRequest {
  courseId: number
}

// 前端API调用类型定义（不包含teacherId，由API服务内部处理）--曹雨荷
export interface QuizDetailRequestInput {
  quizId: number
}

export interface UpdateQuizRequestInput {
  quizId: number
  data: Partial<CreateQuizRequest['data']>
}

export interface DeleteQuizRequestInput {
  quizId: number
}

export interface AddQuestionToQuizRequestInput {
  quizId: number
  data: {
    questionId: number
    score: number
    orderIndex: number
  }
}

export interface RemoveQuestionFromQuizRequestInput {
  quizId: number
  questionId: number
}

export interface SetQuestionSettingsRequestInput {
  quizId: number
  data: {
    questionId: number
    score: number
    orderIndex: number
  }
}

export interface CopyQuizRequestInput {
  quizId: number
  newTitle: string
}

// 创建完整试卷请求输入（前端API调用类型定义）
export interface CreateFullQuizRequestInput {
  data: {
    title: string
    totalPoints: number
    questions: {
      questionId: number
      score: number
      orderIndex: number
    }[]
  }
}

// 替换题目--该类型用于试卷与题库功能--曹雨荷
export interface ReplaceQuestionRequest {
  quizId: number
  data: {
    oldQuestionId: number
    newQuestionId: number
    score: number
    orderIndex: number
  }
  teacherId: number
}

// 批量设置分数顺序--该类型用于试卷与题库功能--曹雨荷
export interface BatchSetQuestionSettingsRequest {
  quizId: number
  data: {
    questionId: number
    score: number
    orderIndex: number
  }[]
  teacherId: number
}
