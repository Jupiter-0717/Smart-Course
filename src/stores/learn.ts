import { defineStore } from 'pinia'
import learnSerivice from '@/services/learnSerivice'
import type {StudentCourseDTO} from  '@/types/class_resource'
import type { ClassResource,ResourceType } from '@/types/class_resource'
import type { Course } from '@/types/course'
import Loading from 'element-plus/es/components/loading/src/service.mjs'
import { resolveTypeElements } from 'vue/compiler-sfc'
import axios from 'axios'
interface ResourceState{
    courses:StudentCourseDTO[],
    resources:ClassResource[],
    loading: boolean,
    error: string | null
}

export const useLearnStore=defineStore('resource',{
    state:():ResourceState=>({
        courses:[] ,
        resources:[],
        loading:false,
        error:null
        
    }),
    actions:{
        async fetchResourceByTypeAndCourse(
            studentId:number,
            courseId:number,
            type:ResourceType
        ){
            this.loading=true,
            this.error=null

            try{
                const result=await learnSerivice.getStudentByTypeAndCourse(
                    studentId,courseId,type
                )
                this.resources=result
                console.log(this.resources)
            }catch(error:any){
                this.error =error.message
                throw error
            }finally{
                this.loading=false
            }
        },
        //根据类型过滤资源
// 在 learn store 中
getResourceByType(type: ResourceType): ClassResource[] {
  if (!this.resources || !Array.isArray(this.resources)) {
    return []
  }
  return this.resources.filter(resource => resource.type === type)
},
        async getCurrntCourse(
            studentId:number
        ){
            this.loading=true,
            this.error=null
            try{
                
                const result=await learnSerivice.getCurrntCourse(studentId)
                this.courses=result
                console.log('原始 API 响应:',result );
 
            }catch(error:any){
                this.error=error.message
                throw error
            }finally{
                this.loading=false
            }
        },
    async fetchResourceByTeacher(
            courseId:number,
            type:ResourceType
        ){
            this.loading=true,
            this.error=null

            try{
                const result=await learnSerivice.getResourceByCourse(
                    courseId,type
                )
                this.resources=result
                console.log(this.resources)
            }catch(error:any){
                this.error =error.message
                throw error
            }finally{
                this.loading=false
            }
        },

    }
}

)
