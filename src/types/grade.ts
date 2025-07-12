// 成绩分析相关类型定义 - 与后端API匹配

export interface GradeAnalysisResponse {
  studentId: number
  studentName: string
  courseId: number
  courseName: string
  currentGrade: string
  maxGrade: string
  gradePercentage: number
  gradeLevel: string
  classRank: number
  totalStudents: number
  gradeTrend: GradeTrendData[]
  taskTypeGrades: Record<string, TaskTypeGrade>
  statistics: GradeStatistics
  learningSuggestions: string[]
}

export interface GradeTrendData {
  date: string
  taskTitle: string
  score: string
  maxScore: string
  taskType: string
  percentage: number
}

export interface TaskTypeGrade {
  taskType: string
  taskCount: number
  maxScore: string
  averageScore: string
  completionRate: number
}

export interface GradeStatistics {
  averageGrade: string
  highestGrade: string
  lowestGrade: string
  standardDeviation: string
  completedTasks: number
  totalTasks: number
  completionRate: number
  gradeDistribution: Record<string, number>
}

export interface CourseGradeReportResponse {
  courseId: number
  courseName: string
  courseCode: string
  teacherName: string
  generatedAt: string
  studentGrades: StudentGradeInfo[]
  courseStatistics: CourseStatistics
  taskStatistics: TaskStatistics[]
}

export interface StudentGradeInfo {
  studentId: number
  studentName: string
  studentNumber: string
  totalGrade: string
  maxGrade: string
  gradePercentage: number
  gradeLevel: string
  rank: number
  completedTasks: number
  totalTasks: number
  completionRate: number
  taskGrades: TaskGradeDetail[]
}

export interface TaskGradeDetail {
  taskId: number
  taskTitle: string
  taskType: string
  dueDate: string
  submittedAt: string | null
  score: string | null
  maxScore: string
  percentage: number | null
  status: 'completed' | 'submitted' | 'not_submitted'
}

export interface CourseStatistics {
  totalStudents: number
  enrolledStudents: number
  classAverage: string
  highestGrade: string
  lowestGrade: string
  standardDeviation: string
  gradeDistribution: Record<string, number>
  completionRateDistribution: any
}

export interface TaskStatistics {
  taskId: number
  taskTitle: string
  taskType: string
  dueDate: string
  totalStudents: number
  submittedStudents: number
  completedStudents: number
  averageScore: string | null
  maxScore: string
  completionRate: number
  averagePercentage: number | null
  scoreDistribution: any
}

// 前端使用的简化类型
export interface CourseGradeReport {
  courseId: number
  courseName: string
  totalStudents: number
  averageScore: number
  highestScore: number
  lowestScore: number
  gradeDistribution: GradeDistribution
  studentGrades: StudentGrade[]
}

export interface GradeDistribution {
  excellent: number
  good: number
  fair: number
  pass: number
  fail: number
}

export interface StudentGrade {
  studentId: number
  studentName: string
  totalScore: number
  maxScore: number
  percentage: number
  grade: string
  rank: number
  taskScores: TaskScore[]
}

export interface TaskScore {
  taskId: number
  taskName: string
  score: number
  maxScore: number
  percentage: number
  submittedAt: string
  status: 'completed' | 'pending' | 'overdue'
}

export interface PerformanceAnalysis {
  strengths: string[]
  weaknesses: string[]
  recommendations: string[]
  improvementAreas: string[]
}

export interface GradeExportRequest {
  courseId: number
  format: 'excel' | 'pdf'
}
