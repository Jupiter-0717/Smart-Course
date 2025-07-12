import axios from "axios";

import type {Student,PaginatedResponse,StudentDTO} from '@/types/student';
import { handleError } from "vue";
import { getCourseById } from "./course.service";

const API_BASE_URL= import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export default{
async addStudent(courseId: number, studentName: string, studentId: string) {
  try {
    // 构造标准的查询参数URL
    const url = new URL(`${API_BASE_URL}/api/course/${courseId}/student/add`);
    
    // 添加查询参数
    const params = new URLSearchParams();
    params.append('courseId', courseId.toString());
    params.append('studentId', studentId);
    params.append('studentName', studentName);
    
    const fullUrl = `${url.toString()}?${params.toString()}`;
    console.log('[API请求] 请求URL:', fullUrl);

    const response = await axios.post(
      fullUrl,
      null, // POST请求体为空
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    );

    console.log('[API响应] 响应数据:', response.data);
    
    return {
      success: true,
      data: response.data,
      message: response.data?.message || '添加学生成功'
    };
  } catch(error) {
    console.error('[API错误]', error);
    
    let errorMsg = '添加学生失败';
    if (axios.isAxiosError(error)) {
      errorMsg = error.response?.data?.message || 
                error.message ||
                `HTTP ${error.response?.status}`;
    }
    
    return {
      success: false,
      error: errorMsg
    };
  }
},
    //批量导入学生
  async importStudnts(courseId: number,file:File){
      try{
          const formData=new FormData();
          formData.append(`file`,file);

          const response=await axios.post(
              `${API_BASE_URL}/api/course/${courseId}/student/batch-import`,formData,
              {
                  headers:{
                      'Content-Type':'multipart/form-data'
                  }

              }
          );
          return response.data;

      }catch(error){
          if (axios.isAxiosError(error) && error.response?.data?.message) {
              throw new Error(error.response.data.message);
          }
              throw new Error('导入学生失败，请检查网络连接');
      }
    },
    //删除学生
  async deleteStudent(courseId:number,studentId:number){
    try{
          const response=await axios.delete(
              `${API_BASE_URL}/api/course/${courseId}/student/${studentId}`
          );
          return response.data;
      }catch(error){
          if (axios.isAxiosError(error) && error.response?.data?.message) {
              throw new Error(error.response.data.message);
          }
              throw new Error('删除学生失败，请检查网络连接');
      }
        
  },
    //获取学生列表

  async getStudents(
    courseId: number,
    page: number,
    pageSize: number,
    sort: string = 'studentId,asc'
  ): Promise<PaginatedResponse<StudentDTO>> {
    try {
      console.log('发送的排序参数:', sort);
      const response = await axios.get<PaginatedResponse<StudentDTO>>(
        `${API_BASE_URL}/api/course/${courseId}/student`,
        {
          params: {
            page,
            size: pageSize,
            sort
          }
        }
      );
      return response.data;
    } catch (error) {
      throw new Error('获取学生列表失败');
    }
  },
//按学号搜索学生
    async getStudentById(
        courseId:number,
        studentId:number
    ){
        try{
            const response=await axios.get(
                `${API_BASE_URL}/api/course/${courseId}/student/${studentId}`
            )
            return response.data;
        }catch(error){
            if (axios.isAxiosError(error) && error.response?.data?.message) {
                throw new Error(error.response.data.message);
            }
                throw new Error('搜索学生失败，请检查网络连接');
        }
    },


   async exportStudenta(courseId: number )  
   {try {
     const response = await axios.get<Blob>(
       `${API_BASE_URL}/api/course/${courseId}/student/export`,
       {
         responseType: 'blob',
         headers: {
           'Content-Type': 'application/octet-stream',
         },
       }
     );

     return response.data;
   } catch (error) {
     if (axios.isAxiosError(error)) {
       const errorMessage = error.response?.data?.message 
         || error.message
         || '导出失败';
       throw new Error(`[导出服务错误] ${errorMessage}`);
     }
     throw new Error('未知导出错误');
   }
 },
 async triggerDownload(
   data:Blob,
   filename:string
 ){
   const url=window.URL.createObjectURL(data);
   const link=document.createElement('a');
   link.href=url;
   link.setAttribute('download',filename);
   document.body.appendChild(link);
   link.click();
  window.URL.revokeObjectURL(url);
   document.body.removeChild(link);
 }


}
