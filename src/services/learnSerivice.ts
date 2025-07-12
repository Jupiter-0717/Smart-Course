import type { ResourceType, StudentCourseDTO } from '@/types/class_resource';
import axios from 'axios';
import { alphaSliderProps } from 'element-plus/es/components/color-picker/src/props/alpha-slider.mjs';


const API_BASE_URL= import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export default{
    async getStudentByTypeAndCourse(
        studentId:number,
        courseId:number,
        type:ResourceType
    ){
        const response=await axios.get(
            `${API_BASE_URL}/api/student/${studentId}/learn/resources`,
            {
                params:{
                    courseId,type
                }
            }
        )
        console.log(2222)
        console.log(response.data)
        return response.data
    },
    async getCurrntCourse(
        studentId:number,

    ):Promise<StudentCourseDTO[]>{
        const response=await axios.get(
            `${API_BASE_URL}/api/student/${studentId}/learn/course`
        )
        console.log(1111)
        console.log(response.data)
       return response.data
    },
    async getResourceByCourse(
        courseId:number,
        type:ResourceType
    ){
        const response=await axios.get(
            `${API_BASE_URL}/api/resources/course/${courseId}`,
            {
                params:{
                    type
                }
            }
        )
        return response.data
    }
    
    
}