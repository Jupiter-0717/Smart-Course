// 该配置文件用于试卷与题库功能--曹雨荷
// 临时固定teacherId配置，合并时只需修改此处的值即可
import { useAuthStore } from "@/stores/auth"
const authStore=useAuthStore()
export const TEACHER_ID = authStore.teacherId // 全局teacherId，修改此处即可全局生效

// 注释说明：
// 1. 当前临时固定为1，用于开发测试
// 2. 合并时将此值改为：useAuthStore().teacherId
// 3. 或者改为：authStore.teacherId
// 4. 所有使用此常量的地方会自动生效
