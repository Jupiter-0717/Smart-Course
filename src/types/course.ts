export interface Course {
  courseId: number
  teacherId: number
  code: string
  name: string
  description: string
  credit: number
  term: string
  createdAt: string
}

export interface CreateCourseRequest {
  teacherId: number
  code: string
  name: string
  description: string
  credit: number
  term: string
}

export interface UpdateCourseRequest {
  name: string
  description: string
  credit: number
  term: string
}

export type NullableCourse = Course | null
