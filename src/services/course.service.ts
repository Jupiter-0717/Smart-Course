import api from './api.service';
import type { 
  Course, 
  CreateCourseRequest, 
  UpdateCourseRequest 
} from '@/types/course';

// 正确导出服务方法（确保所有方法名称正确）
export const createCourse = async (data: CreateCourseRequest): Promise<Course> => {
  const response = await api.post('/courses', data);
  return response.data;
};

export const getAllCourses = async (): Promise<Course[]> => {
  const response = await api.get('/courses');
  return response.data;
};

export const getCoursesByTeacher = async (teacherId: number): Promise<Course[]> => {
  const response = await api.get(`/courses/teacher/${teacherId}`);
  return response.data;
};

export const getCourseById = async (courseId: number): Promise<Course> => {
  const response = await api.get(`/courses/${courseId}`);
  return response.data;
};

export const updateCourse = async (
  courseId: number, 
  data: UpdateCourseRequest
): Promise<Course> => {
  const response = await api.put(`/courses/${courseId}`, data);
  return response.data;
};

export const deleteCourse = async (courseId: number): Promise<void> => {
  await api.delete(`/courses/${courseId}`);
};

// 也可以这样导出为一个对象
export default {
  createCourse,
  getAllCourses,
  getCoursesByTeacher,
  getCourseById,
  updateCourse,
  deleteCourse
};

// 定义分页响应结构
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// 带分页的教师课程接口
export const getTeacherCoursesPaged = async (
  teacherId: number,
  page: number,
  size: number
): Promise<PagedResponse<Course>> => {
  const response = await api.get(`/teacher/${teacherId}/courses-paged`, {
    params: { page, size }
  });
  
  return {
    content: response.data.content,
    totalElements: response.data.totalElements,
    totalPages: response.data.totalPages,
    page: response.data.page,
    size: response.data.size
  };
};

// 搜索教师课程
export const searchTeacherCourses = async (
  teacherId: number,
  query: string,
  page: number,
  size: number
): Promise<PagedResponse<Course>> => {
  const response = await api.get(`/teacher/${teacherId}/search`, {
    params: { q: query, page, size }
  });
  
  return {
    content: response.data.content,
    totalElements: response.data.totalElements,
    totalPages: response.data.totalPages,
    page: response.data.page,
    size: response.data.size
  };
};