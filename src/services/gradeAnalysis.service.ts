import axios from 'axios'
import type {
  GradeAnalysisResponse,
  CourseGradeReportResponse,
  CourseGradeReport,
  StudentGrade,
  GradeDistribution,
} from '@/types/grade'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

class GradeAnalysisService {
  /**
   * 获取学生成绩分析
   */
  async getStudentGradeAnalysis(
    studentId: number,
    courseId: number,
  ): Promise<GradeAnalysisResponse> {
    const response = await axios.get(`${API_BASE_URL}/api/student/grade/analysis/${courseId}`, {
      params: { studentId },
    })
    return response.data
  }

  /**
   * 获取课程成绩报表（教师端）
   */
  async getCourseGradeReport(courseId: number): Promise<CourseGradeReport> {
    const response = await axios.get(`${API_BASE_URL}/api/grade-analysis/course/${courseId}/report`)
    const data: CourseGradeReportResponse = response.data

    // 转换为前端使用的格式
    return this.transformCourseGradeReport(data)
  }

  /**
   * 导出课程成绩报表
   */
  async exportCourseGradeReport(courseId: number): Promise<Blob> {
    try {
      console.log('开始导出课程成绩报表，课程ID:', courseId)
      const response = await axios.get(
        `${API_BASE_URL}/api/grade-analysis/course/${courseId}/export`,
        {
          responseType: 'blob',
          timeout: 30000, // 30秒超时
        },
      )
      console.log('导出成功，响应状态:', response.status)
      return response.data
    } catch (error: any) {
      console.error('导出失败详细信息:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        url: error.config?.url,
        method: error.config?.method
      })

      // 如果是500错误，尝试读取错误详情
      if (error.response?.status === 500) {
        try {
          const errorText = await error.response.data.text()
          console.error('服务器错误详情:', errorText)
        } catch (e) {
          console.error('无法读取错误详情')
        }
      }

      throw error
    }
  }

  /**
   * 获取教师课程列表
   */
  async getTeacherCourses(teacherId: number): Promise<Array<{ id: number; name: string }>> {
    const response = await axios.get(`${API_BASE_URL}/api/courses/teacher/${teacherId}`)
    return response.data.map((course: any) => ({
      id: course.courseId,
      name: course.name,
    }))
  }

  /**
   * 转换课程成绩报表数据格式
   */
  private transformCourseGradeReport(data: CourseGradeReportResponse): CourseGradeReport {
    // 转换成绩分布
    const gradeDistribution: GradeDistribution = {
      excellent: data.courseStatistics.gradeDistribution['优秀'] || 0,
      good: data.courseStatistics.gradeDistribution['良好'] || 0,
      fair: data.courseStatistics.gradeDistribution['中等'] || 0,
      pass: data.courseStatistics.gradeDistribution['及格'] || 0,
      fail: data.courseStatistics.gradeDistribution['不及格'] || 0,
    }

    // 转换学生成绩
    const studentGrades: StudentGrade[] = data.studentGrades.map((student) => ({
      studentId: student.studentId,
      studentName: student.studentName,
      totalScore: this.safeParseFloat(student.totalGrade) || 0,
      maxScore: this.safeParseFloat(student.maxGrade) || 100,
      percentage: this.safeParseFloat(student.gradePercentage) || 0,
      grade: this.convertGradeLevel(student.gradeLevel),
      rank: student.rank,
      taskScores: student.taskGrades.map((task) => ({
        taskId: task.taskId,
        taskName: task.taskTitle,
        score: this.safeParseFloat(task.score) || 0,
        maxScore: this.safeParseFloat(task.maxScore) || 100,
        percentage: this.safeParseFloat(task.percentage) || 0,
        submittedAt: task.submittedAt || '',
        status: this.convertTaskStatus(task.status),
      })),
    }))

    return {
      courseId: data.courseId,
      courseName: data.courseName,
      totalStudents: data.courseStatistics.totalStudents,
      averageScore: this.safeParseFloat(data.courseStatistics.classAverage),
      highestScore: this.safeParseFloat(data.courseStatistics.highestGrade),
      lowestScore: this.safeParseFloat(data.courseStatistics.lowestGrade),
      gradeDistribution,
      studentGrades,
    }
  }

  /**
   * 转换成绩等级
   */
  private convertGradeLevel(gradeLevel: string): string {
    const gradeMap: Record<string, string> = {
      优秀: 'A',
      良好: 'B',
      中等: 'C',
      及格: 'D',
      不及格: 'F',
    }
    return gradeMap[gradeLevel] || 'F'
  }

  /**
   * 转换任务状态
   */
  private convertTaskStatus(status: string): 'completed' | 'pending' | 'overdue' {
    switch (status) {
      case 'completed':
        return 'completed'
      case 'submitted':
        return 'pending'
      case 'not_submitted':
        return 'overdue'
      default:
        return 'pending'
    }
  }

  /**
   * 获取多课程成绩对比分析
   */
  async getMultiCourseAnalysis(courseIds: number[]): Promise<any> {
    const response = await axios.post(`${API_BASE_URL}/api/grade-analysis/multi-course/analysis`, courseIds)
    return response.data
  }

  /**
   * 获取成绩分析概览
   */
  async getGradeAnalysisOverview(): Promise<any> {
    const response = await axios.get(`${API_BASE_URL}/api/grade-analysis/overview`)
    return response.data
  }

  /**
   * 安全的数值解析方法
   */
  private safeParseFloat(value: any): number {
    if (value === null || value === undefined || value === '') {
      return 0
    }

    const num = parseFloat(String(value))
    return isNaN(num) ? 0 : num
  }
}

export const gradeAnalysisService = new GradeAnalysisService()
