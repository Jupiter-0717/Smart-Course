
export interface ClassResource{
    resourceId:number,
    courseId:number,
    taskId:number,
    name:string,
    type:ResourceType,
    url:string,
    creatAt:string,
    description:string,
    duration:number
}
export interface ApiResponse<T>{
    data:T,
    code:number,
    message:string
}

// export interface StudentCourseDTO{
//     courseId:number,
//     code:string,
//     courseName:string
// }
export interface StudentCourseDTO{
    courseId:number
    code:string
    courseName:string
}
export type ResourceType = 'video' | 'pdf' | 'ppt' | 'doc'

