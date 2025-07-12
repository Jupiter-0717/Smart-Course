<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import CourseManagement from '@/components/course/CourseManagementView.vue'
import StudentManagement from '@/components/student/StudentManagement.vue'
import { useRoute, useRouter } from 'vue-router'
import ResourceManagementView from '@/views/ResourceManagementView.vue'
import { useCourseStore } from '@/stores/course' // 导入课程状态管理
import TaskManagement from '@/components/teacher/TaskManagement.vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import CourseGraphCards from '@/components/knowledge-graph/CourseGraphCards.vue' // 导入新组件
import QuizAndQuestionView from '@/views/QuizAndQuestionView.vue'
import { TEACHER_ID } from '@/config/teacher'
const API_BASE_URL= import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const auth = useAuthStore()
// 当前选中的菜单项
const activeMenu = ref('courses') // courses, tasks, analytzics, etc.
const courseStore = useCourseStore()
const currentCourseId = ref(0)
const authStore = useAuthStore()
// 课程相关
//const courses = ref<{ courseId: number, name: string, code: string }[]>([])
const courses = ref<{ courseId: number, name: string, code: string, credit?: number, term?: string }[]>([])
const selectedCourseId = ref<number | null>(null)
const loading = ref(false)
const teacherId = ref(authStore.user?.teacherId || 1) 
// 从状态管理中获取当前选中的课程ID
if (courseStore.currentCourse) {
  currentCourseId.value = courseStore.currentCourse.courseId
} else {
  // 如果没有当前课程，尝试设置默认值（实际项目中应该有默认逻辑）
  console.warn('当前没有选中课程，将使用默认值1');
  currentCourseId.value = 1
}
// 导航到课程知识图谱卡片页面
const navigateToKnowledgeGraph = () => {
  activeMenu.value = 'knowledgeGraph'
}

// 获取当前老师的课程列表
const loadTeacherCourses = async () => {
  if (!auth.user?.teacherId) return

  loading.value = true
  try {
    console.log('请求URL:', `/api/courses/teacher/${auth.user.teacherId}`)
    const response = await axios.get(`${API_BASE_URL}/api/courses/teacher/${auth.user.teacherId}`)
        // 添加响应日志
    console.log('完整响应:', response) 
    console.log('响应数据:', response.data)
    console.log('[[[[]]]]',auth.user.teacherId)
    courses.value = response.data
    console.log('[获取课程列表]', courses)

    // 如果有课程，默认选择第一个
    if (courses.value.length > 0) {
      selectedCourseId.value = courses.value[0].courseId
      console.log('[默认选择课程]', selectedCourseId.value)
    } else {
      selectedCourseId.value = null
    }
  } catch (error) {
    console.error('[获取课程失败]', error)
    ElMessage.error('获取课程列表失败')
  } finally {
    loading.value = false
  }
}

// 处理课程选择
const handleCourseChange = (courseId: number) => {
  selectedCourseId.value = courseId
  console.log('[切换课程]', courseId)
}

// 监听用户变化，重新加载课程
watch(() => auth.user?.teacherId, (newUserId) => {
  if (newUserId) {
    console.log('[用户切换]', newUserId)
    loadTeacherCourses()
  }
}, { immediate: true })

function handleMenuClick(menu: string) {
  activeMenu.value = menu
}

// 计算属性：当前选中的课程信息
const currentCourse = computed(() => {
  return courses.value.find(course => course.courseId === selectedCourseId.value)
})
// 在挂载时获取教师课程
onMounted(async () => {
  try {
    if (authStore.user?.teacherId) {
      await courseStore.loadTeacherCourses(authStore.user.teacherId)
    }
  } catch (error) {
    console.error('加载教师课程失败:', error)
  }
})
</script>

<template>
  <div class="teacher-dashboard">
    <div class="dashboard-header">
      <h1>教师工作台</h1>
      <div class="user-info">
        <div class="avatar">张</div>
        <div class="name">张老师</div>
      </div>
    </div>
    
    <div class="dashboard-content">
      <!-- 侧边导航菜单 -->
      <div class="sidebar">
        <nav>
          <a 
            :class="{ active: activeMenu === 'courses' }"
            @click="activeMenu = 'courses'"
          >
            <i class="icon-book"></i> 课程管理
          </a>
          <a 
            :class="{ active: activeMenu === 'tasks' }"
            @click="activeMenu = 'tasks'"
          >
            <i class="icon-assignment"></i> 任务管理
          </a>
          <a 
            :class="{ active: activeMenu === 'analytics' }"
            @click="activeMenu = 'analytics'"
          >
            <i class="icon-analytics"></i> 成绩分析
          </a>
          <a 
            :class="{ active: activeMenu === 'students' }"
            @click="activeMenu = 'students'"
          >
            <i class="icon-students"></i> 学生管理
          </a>

          <a 
            :class="{ active: activeMenu === 'resources' }"
            @click="activeMenu = 'resources'"
          >
            <i class="icon-resources"></i> 学习资料
          </a>
                    <!-- 该菜单项用于题库管理的试题与组卷功能--曹雨荷 -->
          <a
            :class="{ active: activeMenu === 'quizQuestion' }"
            @click="activeMenu = 'quizQuestion'"
          >
            <i class="iconfont icon-shijuan"></i> 试题组卷
          </a>
          <a 
            :class="{ active: activeMenu === 'knowledgeGraph' }"
            @click="navigateToKnowledgeGraph"
          >
            <i class="icon-graph"></i> 知识图谱
          </a>
        </nav>
      </div>
      
      <!-- 主内容区 - 显示课程管理主界面 -->
      <div class="main-content">
        <CourseManagement v-if="activeMenu === 'courses'" />
        <StudentManagement v-else-if="activeMenu === 'students'" />

        <!-- 任务管理 -->
        <div v-else-if="activeMenu === 'tasks'">
          <!-- 任务管理组件 -->
          <TaskManagement v-if="courses.length > 0"
                          :courseId="selectedCourseId"
                          :courses="courses"
                          @courseChange="handleCourseChange" />

          <!-- 无课程提示 -->
          <div v-else-if="!loading && courses.length === 0" class="no-courses">
            <el-empty description="暂无课程">
              <el-button type="primary" @click="activeMenu = 'courses'">
                去创建课程
              </el-button>
            </el-empty>
          </div>
        </div>

        <!-- 成绩分析功能 -->
        <div v-else-if="activeMenu === 'analytics'" class="analytics-section">
          <div class="analytics-header">
            <h3>成绩分析管理</h3>
            <p>查看和分析学生成绩数据，生成成绩报表</p>
          </div>
          <div class="analytics-actions">
            <router-link to="/teacher/grade-analysis" class="analytics-btn">
              <i class="icon-chart"></i>
              进入成绩分析
            </router-link>
          </div>
        </div>
        <ResourceManagementView v-else-if="activeMenu === 'resources'"
                                :course-id="currentCourseId" />
        <!-- 该内容区用于题库管理的试题与组卷功能--曹雨荷 -->
        <QuizAndQuestionView v-else-if="activeMenu === 'quizQuestion'" />
        <!-- 课程知识图谱卡片 -->
        <CourseGraphCards v-else-if="activeMenu === 'knowledgeGraph'"
                          :teacher-id="teacherId" />
      </div>
    </div>
  </div> 
</template>

<style scoped>
.teacher-dashboard {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #3498db;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.dashboard-content {
  display: flex;
}

.sidebar {
  width: 220px;
  margin-right: 20px;
}

.sidebar nav {
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 10px 0;
}

.sidebar a {
  padding: 12px 20px;
  text-decoration: none;
  color: #333;
  border-radius: 4px;
  transition: all 0.3s;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar a:hover {
  background-color: #e9ecef;
}

.sidebar a.active {
  background-color: #e3f2fd;
  color: #0d6efd;
  font-weight: 500;
}

.main-content {
  flex: 1;
  min-height: 80vh;
}

.placeholder {
  padding: 40px;
  background-color: white;
  border-radius: 8px;
  text-align: center;
}

  .analytics-section {
    background: white;
    border-radius: 8px;
    padding: 30px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .analytics-header {
    text-align: center;
    margin-bottom: 30px;
  }

    .analytics-header h3 {
      margin: 0 0 10px 0;
      color: #333;
      font-size: 20px;
    }

    .analytics-header p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

  .analytics-actions {
    text-align: center;
  }

  .analytics-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 12px 24px;
    border-radius: 6px;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.3s;
  }

    .analytics-btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

/* 新增资源图标样式 */
/*.icon-resources::before {
  content: "📚";*/ /* 使用书本emoji作为图标 */
  /*font-size: 18px;
}*/
</style>
