export interface Student{
    studentId:number;
    studentName:string;
}
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // 这是Spring Data返回的当前页码(0-based)
  size: number   // 这是Spring Data返回的每页大小
}
export interface StudentDTO{
    studentId:number;
    studentName:string;
}