import api from './api.service';
import type { 
  Resource, 
  ResourceType,
  ResourceUploadRequest,
  ResourceUpdateRequest
} from '@/types/resource'; // 资源相关类型
import type { Course } from '@/types/course'; 

// 获取课程资源列表
export const getCourseResources = async (courseId: number): Promise<Resource[]> => {
  const response = await api.get(`/resources`, {
    params: { courseId }
  });
  return response.data;
};
export const ResourceService = {
  // 获取教师所有资源
  async getAllTeacherResources(teacherId: number): Promise<Resource[]> {
    try {
      const response = await api.get(`/teachers/${teacherId}/resources`);
      return response.data;
    } catch (error) {
      console.error('获取教师资源失败:', error);
      return [];
    }
  }
};
// 上传资源
export const uploadResource = async (
  data: ResourceUploadRequest
): Promise<Resource> => {
  const formData = new FormData();
  formData.append('courseId', data.courseId.toString());
  formData.append('name', data.name);
  formData.append('type', data.type);
  formData.append('file', data.file);
  
  if (data.description) {
    formData.append('description', data.description);
  }

  const response = await api.post('/resources/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
};

// 删除资源
export const deleteResource = async (resourceId: number): Promise<void> => {
  await api.delete(`/resources/${resourceId}`);
};

// 更新资源信息
export const updateResource = async (
  resourceId: number,
  data: ResourceUpdateRequest
): Promise<Resource> => {
  // 添加资源ID验证
  if (!resourceId || isNaN(resourceId)) {
    throw new Error('无效的资源ID');
  }
  
  // 修改为发送 JSON 数据
  const response = await api.put(`/resources/${resourceId}`, data, {
    headers: {
      'Content-Type': 'application/json' // 确保使用 JSON 格式
    }
  });
  
  return response.data;
};




// 获取所有课程
export const getAllCourses = async (): Promise<Course[]> => {
  const response = await api.get('/courses');
  return response.data;
};


// 更新资源文件
export const updateResourceFile = async (
  resourceId: number,
  data: { file: File; courseId: number } // 添加课程ID
): Promise<Resource> => {
  const formData = new FormData();
   formData.append('file', data.file);
  formData.append('courseId', data.courseId.toString()); // 添加课程ID
  const response = await api.put(`/resources/${resourceId}/file`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  
  return response.data;
};

// 搜索资源
export const searchResources = async (
  keyword: string,
  type?: ResourceType
): Promise<Resource[]> => {
  const response = await api.get('/resources/search', {
    params: { keyword, type }
  });
  return response.data;
};




// 获取教师所有资源（不限课程）
// 在 resource.service.ts 中修改 getAllTeacherResources 函数
export const getAllTeacherResources = async (
  params: {
    teacherId: number;
    type?: ResourceType;
    keyword?: string;
  }
): Promise<Resource[]> => {
  try {
    const response = await api.get('/resources/teacher/all', {
      params: {
        teacherId: params.teacherId,
        type: params.type,
        keyword: params.keyword
      }
    });
    return response.data;
  } catch (error) {
    console.error('获取教师所有资源失败:', error);
    throw new Error('无法获取教师所有资源');
  }
};

export default {
  getCourseResources,
  uploadResource,
  deleteResource,
  updateResource,
  searchResources,
   async getResourcesByCourse(courseId: number): Promise<any> {
    const response = await api.get(`/courses/${courseId}/resources`)
    return response.data}
  
};
