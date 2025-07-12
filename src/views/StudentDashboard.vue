<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import CourseManagement from '@/components/course/CourseManagementView.vue'
import StudentManagement from '@/components/student/StudentManagement.vue'
import { useRoute, useRouter } from 'vue-router'
import LearnList from '@/components/learn/LearnList.vue'
import TaskList from '@/components/task/TaskList.vue'
import QuizTestView from '@/views/StudentQuizTestView.vue'
// 当前选中的菜单项
const activeMenu = ref('learn')
// const route = useRoute()

// onMounted(() => {
//   if (route.query.tab === 'tasks') {
//     activeMenu.value = 'tasks'
//   }
// })

// watch(
//   () => route.query.tab,
//   (newTab) => {
//     if (newTab === 'tasks') {
//       activeMenu.value = 'tasks'
//     }
//     if (newTab === 'learn') {
//       activeMenu.value = 'learn'
//     }
//   },
// )
</script>

<template>
  <div class="student-dashboard">
    <div class="dashboard-header">
      <h1>学生工作台</h1>
      <div class="user-info">
        <div class="avatar">张</div>
        <div class="name">张一一</div>
      </div>
    </div>

    <div class="dashboard-content">
      <!-- 侧边导航菜单 -->
      <div class="sidebar">
        <nav>
          <a :class="{ active: activeMenu === 'learn' }" @click="activeMenu = 'learn'">
            <i class="icon-book"></i> 课程学习
          </a>
          <a :class="{ active: activeMenu === 'tasks' }" @click="activeMenu = 'tasks'">
            <i class="icon-assignment"></i> 课程任务
          </a>
          <a :class="{ active: activeMenu === 'quizTest' }" @click="activeMenu = 'quizTest'">
            <i class="icon-quiz"></i> 课堂测试
          </a>
        </nav>
      </div>

      <!-- 主内容区 - 显示课程管理主界面 -->
      <div class="main-content">
        <LearnList v-if="activeMenu === 'learn'" />
        <TaskList v-else-if="activeMenu === 'tasks'" />
        <QuizTestView v-else-if="activeMenu === 'quizTest'" />

        <!-- 成绩分析功能 -->
        <div v-else-if="activeMenu === 'grades'" class="grades-section">
          <div class="grades-header">
            <h3>我的成绩分析</h3>
            <p>查看个人成绩趋势和学习建议</p>
          </div>
          <div class="grades-actions">
            <router-link to="/student/grade-analysis" class="grades-btn">
              <i class="icon-chart"></i>
              查看成绩分析
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-dashboard {
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

.grades-section {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.grades-header {
  text-align: center;
  margin-bottom: 30px;
}

.grades-header h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 20px;
}

.grades-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.grades-actions {
  text-align: center;
}

.grades-btn {
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

.grades-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
</style>
