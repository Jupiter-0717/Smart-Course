// src/services/statsService.ts
import { useApiStore } from '@/stores/api';

interface CourseStats {
  pointCount: number;
  resourceCount: number;
}

interface BatchCourseStats {
  [courseId: number]: CourseStats;
}

export default {
  // 获取单个课程的统计信息
  async getCourseStats(courseId: number): Promise<CourseStats> {
    const apiStore = useApiStore();
    try {
      const response = await apiStore.get(`/stats/course/${courseId}`);
      return response.data;
    } catch (error) {
      console.error(`获取课程 ${courseId} 统计失败:`, error);
      return { pointCount: 0, resourceCount: 0 };
    }
  },

  // 批量获取课程统计信息
  async getBatchCourseStats(courseIds: number[]): Promise<BatchCourseStats> {
    const apiStore = useApiStore();
    try {
      // 使用正确的POST方法
      const response = await apiStore.post(
        '/stats/courses/batch',
        courseIds  // 发送课程ID数组
      );
      return response.data;
    } catch (error) {
      console.error('批量获取统计失败:', error);
      throw new Error('批量接口请求失败，回退到逐个获取');
    }
  }
};