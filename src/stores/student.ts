import { defineStore } from 'pinia'
import type{ Student ,StudentDTO} from '@/types/student'
import studentService from '@/services/studentService'
import { styleType } from 'element-plus/es/components/table-v2/src/common.mjs'
import { tr } from 'element-plus/es/locales.mjs'
export const useStudentStore = defineStore('student',{
      state: () => ({
        students: [] as Student[],
        currentStudent: null as Student | null,
        pagination: {
            currentPage: 1,  // 改为1-based分页
            totalPage: 0,
            totalElements: 0,
            pageSize: 10
        },
        loading: false,
        error: null as string | null
    }),
    
    //添加一个学生
    actions:{
    async addStudent(courseId: number, studentName:string,studentId:string) {
      this.loading = true
      this.error = null
      try {
        const result = await studentService.addStudent(courseId,studentName,studentId)
        return result
      } catch (error: any) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },



    // 批量导入学生
    async importStudents(courseId: number, file: File) {
      this.loading = true
      this.error = null
      try {
        const result = await studentService.importStudnts(courseId, file)
        // 重新加载学生列表

        return result
      } catch (error: any) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // 删除学生
    async deleteStudent(courseId: number, studentId: number) {
      this.loading = true
      this.error = null
      try {
        await studentService.deleteStudent(courseId, studentId)
        this.students = this.students.filter(s => s.studentId !== studentId)
      } catch (error: any) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取学生列表

// studentStore.ts
async loadStudents(payload: { courseId: number; page?: number; pageSize?: number }) {
  this.loading = true;
  this.error = null;
  try {
    
    const page = payload.page ?? this.pagination.currentPage - 1;
    const pageSize = payload.pageSize ?? this.pagination.pageSize;

    const data = await studentService.getStudents(
      payload.courseId,
      page,
      pageSize,
      'studentId,asc' // 默认按姓名排序
    );
    
    this.students = data.content || [];
    this.pagination = {
      currentPage: data.number + 1,
      totalPage: data.totalPages,
      totalElements: data.totalElements,
      pageSize: data.size
    };
  } catch (error: unknown) {
    this.error = error instanceof Error ? error.message : '加载学生列表失败';
    throw error;
  } finally {
    this.loading = false;
  }
},
    //按学号搜索学生
    async getStudentById(courseId:number,studentId:number){
      this.loading=true
      this.error=null
      try{
        const response=await studentService.getStudentById(courseId,studentId)
        this.students = response.data

      }catch (error: any) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
  async exportStudents(courseId: number) {
       this.loading = true;
       this.error = null;

       try {
         const data = await studentService.exportStudenta(courseId);
         studentService.triggerDownload(data, `students_${courseId}.xlsx`);
         return true;
      } catch (error) {
         this.error = error instanceof Error ? error.message : '导出失败';
         throw error;
       } finally {
         this.loading = false;
       }
     },
   

    }
  

  
})