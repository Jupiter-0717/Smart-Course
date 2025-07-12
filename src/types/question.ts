// 该文件用于试卷与题库功能--曹雨荷

// 题目类型枚举
export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'FILL_IN_BLANK' | 'SHORT_ANSWER'

// 难度枚举
export type Difficulty = 1 | 2 | 3 | 4 | 5

// 题目body结构
export interface QuestionBodyOption {
  label: string // 选项标识，如A、B、C
  text: string // 选项内容
}
export interface QuestionBody {
  stem: string // 题干（问题）
  options?: QuestionBodyOption[] // 选择题选项
  answer: string // 答案
  analysis?: string // 解析
}

// 题目响应结构
export interface QuestionResponse {
  id: number
  courseId: number
  type: QuestionType
  body: string // JSON字符串，前端需解析为QuestionBody
  difficulty: Difficulty
  knowledgePointId: number
  courseName: string
  knowledgePointName: string
}

// 创建题目请求
export interface CreateQuestionRequest {
  data: {
    courseId: number
    knowledgePointId: number
    type: QuestionType
    body: string // JSON字符串
    difficulty: Difficulty
  }
  teacherId: number
}

// 筛选题目请求
export interface SearchQuestionRequest {
  data: {
    courseId?: number | null
    type?: QuestionType | null
    page: number
    size: number
  }
  teacherId: number
}

// 题目详情请求
export interface QuestionDetailRequest {
  id: number
  teacherId: number
}

// 编辑题目请求
export interface UpdateQuestionRequest {
  id: number
  data: Partial<CreateQuestionRequest['data']>
  teacherId: number
}

// 删除题目请求
export interface DeleteQuestionRequest {
  id: number
  teacherId: number
}

// 批量删除请求
export interface BatchDeleteQuestionRequest {
  ids: number[]
  teacherId: number
}

// 按课程获取
export interface ByCourseQuestionRequest {
  courseId: number
  teacherId: number
}

// 关键词搜索
export interface SearchByKeywordRequest {
  keyword: string
  teacherId: number
}

// 前端API调用类型定义（不包含teacherId，由API服务内部处理）--曹雨荷
export interface CreateQuestionRequestInput {
  data: {
    courseId: number
    knowledgePointId: number
    type: QuestionType
    body: string // JSON字符串
    difficulty: Difficulty
  }
}

export interface SearchQuestionRequestInput {
  data: {
    courseId?: number | null
    type?: QuestionType | null
    page: number
    size: number
  }
}

export interface QuestionDetailRequestInput {
  id: number
}

export interface UpdateQuestionRequestInput {
  id: number
  data: Partial<CreateQuestionRequest['data']>
}

export interface DeleteQuestionRequestInput {
  id: number
}

export interface BatchDeleteQuestionRequestInput {
  ids: number[]
}

export interface ByCourseQuestionRequestInput {
  courseId: number
}

export interface SearchByKeywordRequestInput {
  keyword: string
}
