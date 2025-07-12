import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import RegisterView from '@/views/RegisterView.vue'
import TeacherCourseKnowledgeGraph from '@/views/TeacherCourseKnowledgeGraph.vue' // 导入知识图谱视图
import VideoDetailView from '@/views/VideoDeatilView.vue'
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 - 智慧学习平台' },
  },
  {
    path: '/teacher/dashboard',
    name: 'TeacherDashboard',
    component: () => import('@/views/TeacherDashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '教师控制台 - 智慧学习平台',
      role: 'teacher',
    },
  },
  {
    path: '/student/dashboard',
    name: 'StudentDashboard',
    component: () => import('@/views/StudentDashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '学生控制台 - 智慧学习平台',
      role: 'student',
    },
  },
  // router.ts
  {
    path: '/course/:courseId',
    name: 'CourseStudentManagement',
    component: () => import('@/components/student/CourseStudentManagement.vue'), // 你的学生管理组件
    meta: { requiresAuth: true },
  },
  {
    path: '/learn/:courseId',
    name: 'CourseDetail',
    component: () => import('@/components/learn/CourseDetail.vue'),
    props: true,
  },
  {
    path: '/detail/:courseId',
    name: 'CourseResourceDetail',
    component: () => import('@/components/course/CourseResourceDeatil.vue'),
    props: true,
  },
  // 该路由用于试卷与题库功能--曹雨荷
  {
    path: '/teacher/quiz-question',
    name: 'QuizAndQuestion',
    component: () => import('@/views/QuizAndQuestionView.vue'),
    meta: {
      requiresAuth: true,
      title: '试题与组卷管理 - 智慧学习平台',
      role: 'teacher',
    },
  },
  // 添加注册路由
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
  },
  // 曹雨荷课堂测验功能
  {
    path: '/student/quiz-test',
    name: 'StudentQuizTest',
    component: () => import('@/views/StudentQuizTestView.vue'),
    meta: {
      requiresAuth: true,
      title: '课堂测试 - 智慧学习平台',
      role: 'student',
    },
  },
  {
    path: '/teacher/tasks/:taskId/submissions',
    name: 'TaskSubmissionsView',
    component: () => import('@/views/TaskSubmissionsView.vue'),
  },
  {
    path: '/teacher/submissions/:submissionId/correct',
    name: 'CorrectSubmissionView',
    component: () => import('@/views/CorrectSubmissionView.vue'),
  },
  // 添加任务相关路由
  {
    path: '/student/tasks',
    name: 'StudentTasks',
    component: () => import('@/components/task/StudentTasks.vue'),
    meta: {
      requiresAuth: true,
      title: '我的任务 - 智慧学习平台',
      role: 'student',
    },
  },
  //成绩可视化
  {
    path: '/teacher/grade-analysis',
    name: 'TeacherGradeAnalysis',
    component: () => import('@/views/GradeAnalysisView.vue'),
    meta: {
      requiresAuth: true,
      title: '成绩分析管理 - 智慧学习平台',
      role: 'teacher',
    },
  },
  {
    path: '/student/grade-analysis',
    name: 'StudentGradeAnalysis',
    component: () => import('@/views/StudentGradeAnalysisView.vue'),
    meta: {
      requiresAuth: true,
      title: '学生成绩分析 - 智慧学习平台',
      role: 'student',
    },
  },
  {
    path: '/grade-analysis',
    name: 'GradeAnalysis',
    component: () => import('@/views/GradeAnalysisView.vue'),
    meta: {
      requiresAuth: true,
      title: '成绩分析 - 智慧学习平台',
    },
  },

  //其他
  {
    path: '/student/task/homework/:taskId',
    name: 'HomeworkTaskDetail',
    component: () => import('@/components/task/HomeworkTaskDetail.vue'),
    meta: {
      requiresAuth: true,
      title: '作业详情 - 智慧学习平台',
      role: 'student',
    },
  },
  {
    path: '/student/task/report/:taskId',
    name: 'ReportTaskDetail',
    component: () => import('@/components/task/ReportTaskDetail.vue'),
    meta: {
      requiresAuth: true,
      title: '报告详情 - 智慧学习平台',
      role: 'student',
    },
  },
  {
    path: '/teacher/:teacherId/course/:courseId/knowledge-graph',
    name: 'TeacherCourseKnowledgeGraph',
    component: TeacherCourseKnowledgeGraph,
    props: true,
  },
  {
    path: '/video/:videoId',
    name: 'VideoDetail',
    component: VideoDetailView,
    meta: { title: '视频详情' },
  },
  {
    path: '/teacher/tasks/:taskId/ai-score-result',
    name: 'TaskAiScoreResult',
    component: () => import('@/views/TaskAiScoreResultView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title as string
  }

  next()
})

export default router
