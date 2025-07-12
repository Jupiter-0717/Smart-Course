<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { Course } from '@/types/course';
import { Search } from '@element-plus/icons-vue';

const props = defineProps<{
  courses: Course[]
  loading: boolean
  error?: string
}>();

const emit = defineEmits<{
  (e: 'edit', course: Course): void
  (e: 'delete', courseId: number): void
}>();

// 搜索关键词
const searchQuery = ref('');
const searchCategory = ref<'all' | 'code' | 'name' | 'term'>('all');

// 清空搜索
const clearSearch = () => {
  searchQuery.value = '';
};

// 过滤课程列表
const filteredCourses = computed(() => {
  if (!searchQuery.value.trim()) {
    return [...props.courses];
  }
  
  const query = searchQuery.value.trim().toLowerCase();
  
  return props.courses.filter(course => {
    switch (searchCategory.value) {
      case 'code':
        return course.code?.toLowerCase().includes(query);
      case 'name':
        return course.name?.toLowerCase().includes(query);
      case 'term':
        return course.term?.toLowerCase().includes(query);
      default: // 'all'
        return (
          course.code?.toLowerCase().includes(query) ||
          course.name?.toLowerCase().includes(query) ||
          course.term?.toLowerCase().includes(query) ||
          (course.description?.toLowerCase().includes(query) && query.length > 2)
        );
    }
  });
});

// 按创建时间排序
const sortedCourses = computed(() => {
  return [...filteredCourses.value].sort((a, b) => 
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );
});

// 当课程数据变化时重置搜索
watch(() => props.courses, () => {
  clearSearch();
});
</script>

<template>
  <div class="course-list">
    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      <el-alert :title="error" type="error" show-icon />
    </div>
    
    <!-- 搜索框区域 -->
    <div v-if="!loading" class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索课程..."
        clearable
        @clear="clearSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-select 
            v-model="searchCategory"
            placeholder="搜索范围"
            class="search-category"
          >
            <el-option label="全部" value="all" />
            <el-option label="课程编号" value="code" />
            <el-option label="课程名称" value="name" />
            <el-option label="学期" value="term" />
          </el-select>
        </template>
      </el-input>
      
      <div class="search-status">
        <span v-if="filteredCourses.length === props.courses.length">
          显示全部课程 ({{ filteredCourses.length }})
        </span>
        <span v-else>
          找到 {{ filteredCourses.length }} 门匹配课程（共 {{ props.courses.length }} 门）
        </span>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 空状态 -->
    <div v-if="!loading && sortedCourses.length === 0" class="empty-state">
      <el-empty :description="searchQuery ? '未找到匹配课程' : '暂无课程数据'" />
      <el-button 
        type="primary" 
        plain 
        class="mt-4"
        v-if="searchQuery"
        @click="clearSearch"
      >
        清除搜索
      </el-button>
    </div>
    
    <!-- 课程列表 -->
    <div v-if="!loading && sortedCourses.length > 0">
      <el-table :data="sortedCourses" style="width: 100%" highlight-current-row>
        <el-table-column prop="code" label="课程编号" width="120">
          <template #default="{ row }">
            <span v-if="searchQuery" class="highlight">
              {{ row.code }}
            </span>
            <span v-else>{{ row.code }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="课程名称">
          <template #default="{ row }">
            <span v-if="searchQuery" class="highlight">
              {{ row.name }}
            </span>
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="credit" label="学分" width="80" align="center" />
        <el-table-column prop="term" label="学期" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleDateString() }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="emit('edit', row)">
              编辑
            </el-button>
            <el-button 
              size="small" 
              type="danger"
              @click="emit('delete', row.courseId)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.course-list {
  margin-top: 20px;
}

.search-container {
  background: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
  max-width: 600px;
  margin-bottom: 10px;
}

.search-category {
  width: 120px;
}

.search-status {
  font-size: 14px;
  color: #606266;
  margin-top: 8px;
}

.loading-container,
.empty-state,
.error-message {
  margin-top: 30px;
}

.highlight {
  background-color: #fffd99;
  padding: 1px 3px;
  border-radius: 3px;
}

.empty-state .mt-4 {
  margin-top: 16px;
}
</style>