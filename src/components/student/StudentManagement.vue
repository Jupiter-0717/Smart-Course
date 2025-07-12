<template>
  <div class="student-management">
    <div class="header">
      <h2>学生管理</h2>
      <div class="search-container">
        <el-input
          v-model="searchQuery"
          placeholder="搜索课程..."
          class="search-input"
          clearable
          @clear="resetSearch"
        />
        <el-select 
          v-model="selectedTerm" 
          placeholder="全部学期" 
          class="term-select"
          @change="filterByTerm"
        >
          <el-option label="全部学期" value="" />
          <el-option
            v-for="term in getSemesters"
            :key="term"
            :label="term"
            :value="term"
          />
        </el-select>
      </div>
    </div>

    <!-- 课程列表 -->
    <el-table 
      :data="filteredCourses" 
      style="width: 100%" 
      border 
      v-loading="loading"
    >
      <el-table-column prop="code" label="课程编号" width="120" />
      <el-table-column prop="name" label="课程名称">
        <template #default="scope">
          <el-link type="primary" @click="enterCourse(scope.row.courseId)">
            {{ scope.row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="credit" label="学分" width="80" align="center" />
      <el-table-column prop="term" label="学期" width="180" />
      <el-table-column prop="createdAt" label="创建时间" width="120">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCourseStore } from '@/stores/course'
import type { Course } from '@/types/course'
import { useAuthStore } from '@/stores/auth';
import { storeToRefs } from 'pinia';
const router = useRouter()
const courseStore = useCourseStore()
const authStore=useAuthStore()
const { user } = storeToRefs(authStore);
// 搜索和筛选
const searchQuery = ref('')
const selectedTerm = ref('')

// 获取课程列表
const loading = ref(false)

// 计算属性
const getSemesters = computed(() => courseStore.getSemesters)

const filteredCourses = computed(() => {
  return courseStore.courses.filter(course => {
    const matchesSearch = course.name.includes(searchQuery.value) || 
                         course.code.includes(searchQuery.value)
    const matchesTerm = selectedTerm.value ? course.term === selectedTerm.value : true
    return matchesSearch && matchesTerm
  })
})

// 日期格式化
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

// 进入课程学生管理
const enterCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 按学期筛选
const filterByTerm = () => {
  searchQuery.value = ''
}

// 重置搜索
const resetSearch = () => {
  searchQuery.value = ''
  selectedTerm.value = ''
}

// 初始化加载课程
onMounted(async () => {
  loading.value = true
  try {
    if(user.value?.role==='teacher'){
        await courseStore.loadTeacherCourses(user.value.id);

    } 
  } catch (error) {
    console.error('加载课程失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.student-management {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-container {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 300px;
}

.term-select {
  width: 150px;
}
</style>