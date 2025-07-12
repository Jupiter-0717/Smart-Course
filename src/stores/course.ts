import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import * as CourseService from '@/services/course.service';
import type { Course, CreateCourseRequest, UpdateCourseRequest } from '@/types/course';

export const useCourseStore = defineStore('course', () => {
  // 状态
  const courses = ref<Course[]>([]);
  const currentCourse = ref<Course | null>(null);
  const loading = ref(false);
  const error = ref<string | undefined>(undefined); // 修改：统一使用 undefined

  // 计算属性
  const courseCount = computed(() => courses.value.length);

  // Action: 加载教师课程
  const loadTeacherCourses = async (teacherId: number): Promise<void> => {
    try {
      loading.value = true;
      const loadedCourses = await CourseService.getCoursesByTeacher(teacherId);
      
      // 业务逻辑放在store中
      courses.value = loadedCourses.map(course => ({
        ...course,
        teacherId: course.teacherId || teacherId
      }));
      
      error.value = undefined; // 修改：使用 undefined 而不是 null
    } catch (err: any) {
      error.value = `加载课程失败: ${err.message || err}`;
    } finally {
      loading.value = false;
    }
  };

  // Action: 创建课程
  const createCourse = async (data: CreateCourseRequest) => {
    try {
      loading.value = true;
      const newCourse = await CourseService.createCourse(data);
      courses.value.push(newCourse);
      return newCourse;
    } catch (err: any) {
      error.value = `创建课程失败: ${err.message || err}`;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  // Action: 更新课程
  const updateCourse = async (courseId: number, data: UpdateCourseRequest) => {
    try {
      loading.value = true;
      const updatedCourse = await CourseService.updateCourse(courseId, data);
      
      // 更新本地列表
      const index = courses.value.findIndex(c => c.courseId === courseId);
      if (index !== -1) {
        courses.value[index] = updatedCourse;
      }
      return updatedCourse;
    } catch (err: any) {
      error.value = `更新课程失败: ${err.message || err}`;
      throw err;
    } finally {
      loading.value = false;
    }
  };

  // Action: 删除课程
  const deleteCourse = async (courseId: number) => {
    try {
      loading.value = true;
      await CourseService.deleteCourse(courseId);
      
      // 从本地移除
      courses.value = courses.value.filter(c => c.courseId !== courseId);
    } catch (err: any) {
      error.value = `删除课程失败: ${err.message || err}`;
      throw err;
    } finally {
      loading.value = false;
    }
  };
  const resetFilters = () => {
  // 实际应用中可重置所有过滤条件
};
const getSemesters = computed(() => {
  return Array.from(
    new Set(courses.value.map(course => course.term))
  );
});
  return {
    courses,
    currentCourse,
    loading,
    error,
    courseCount,
    loadTeacherCourses,
    createCourse,
    updateCourse,
    deleteCourse,
    resetFilters,
    getSemesters
  };
});
