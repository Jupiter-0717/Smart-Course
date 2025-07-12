import { defineStore } from "pinia";
import axios from 'axios'
import type { VideoAnalyticsDTO, VideoResourceAnalyticsDTO } from "@/types/video_analytics";
import Loading from "element-plus/es/components/loading/src/service.mjs";
import { flattenDiagnosticMessageText } from "typescript";
import { ca, fa, tr } from "element-plus/es/locales.mjs";
const API_BASE_URL= import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const useVideoAnalyticsStore = defineStore('videoAnalytics',{
    state:()=>({
        analyticsData:null as VideoAnalyticsDTO | null,
        loading:false,
        error:null as string | null,
        teacherAnalyticsData:null as VideoResourceAnalyticsDTO | null
    }),
    actions:{
        async fetchAnalytics(resourceId:number,studentId?:number){
            this.loading=true
            this.error=null
            try{
                const url=studentId?`${API_BASE_URL}/api/video/resources/${resourceId}/students/${studentId}`:
                `${API_BASE_URL}/api/video/resources/${resourceId}`
                const response=await axios.get<VideoAnalyticsDTO>(url)
                this.analyticsData=response.data
            }catch(err){
                this.error=err instanceof Error?err.message:"获取分析数据失败"

            }finally{
                this.loading=false
            }
        },
        async fetchTeacherAnalytics(resourceId:number){
            this.loading=true
            this.error=null
            try{
                 console.log('发送请求获取resourceId:', resourceId);
                const url=`${API_BASE_URL}/api/video/resources/${resourceId}/teacher`
                const response=await axios.get<VideoResourceAnalyticsDTO>(url,{
                    headers:{
                        'Accept':'application/json',
                        'Content-Type': 'application/json'
                    }
                })
                this.teacherAnalyticsData=response.data
                console.log('收到响应:', response.data);
            }catch(err){
                this.error=err instanceof Error?err.message:"失败"

            }finally{
                this.loading=false
            }
        },
        async fetchHotSpots(resourceId:number){
            try{
                const response=await axios.get(`
                    ${API_BASE_URL}/api/video/resources/${resourceId}/hotspots`)
                    return response.data
            }catch(err){
                console.error(err)
                return []
            }

        },
        async generateVideoAnalytics(){
            try{
                await axios.post(`${API_BASE_URL}/api/generate`)

            }catch(err){
                console.error(err)
            }
        }
    }
})