<template>
  <div class="resource-list">
    <!-- 加载状态 -->
    <el-skeleton v-if="loading" :rows="5" animated />
    
    <!-- 空状态 -->
    <el-empty v-else-if="resources.length === 0" description="暂无资源" />
    
    <!-- 资源列表 -->
    <el-table v-else :data="resources" style="width: 100%">
      <!-- 新增课程名称列 -->
      <el-table-column prop="courseName" label="关联课程" width="150">
  <template #default="{ row }">
    <div class="course-name">
      <el-tag size="small" effect="plain" v-if="row.courseName">
        {{ row.courseName }}
      </el-tag>
      <el-tag v-else type="info" size="small" effect="plain">
        未指定
      </el-tag>
    </div>
  </template>
</el-table-column>
      
      <el-table-column prop="name" label="资源名称" min-width="180">
        <template #default="{ row }">
          <div class="resource-name">
            <el-icon v-if="row.type === 'pdf'" color="#F56C6C"><Document /></el-icon>
            <el-icon v-else-if="row.type === 'ppt'" color="#E6A23C"><Histogram /></el-icon>
            <el-icon v-else-if="row.type === 'video'" color="#409EFF"><VideoPlay /></el-icon>
            <el-icon v-else><Folder /></el-icon>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTagType(row.type)" size="small">
            {{ getTypeText(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.description || '无描述' }}
        </template>
      </el-table-column>
      
      <el-table-column prop="createdAt" label="上传时间" width="150" sortable>
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <div class="resource-actions">
            <el-tooltip content="下载" placement="top">
              <el-button 
                type="primary" 
                size="small"
                circle
                @click="$emit('download', row)"
              >
                <el-icon><Download /></el-icon>
              </el-button>
            </el-tooltip>
            
            <el-tooltip content="编辑" placement="top">
              <el-button 
                type="warning" 
                size="small"
                circle
                @click="$emit('edit', row)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>
            
            <el-tooltip content="更新文件" placement="top">
              <el-button 
                type="info" 
                size="small"
                circle
                @click="$emit('update-file', row)"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
            
            <el-tooltip content="删除" placement="top">
              <el-button 
                type="danger" 
                size="small"
                circle
                @click="$emit('delete', row.resourceId)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { Document, Histogram, VideoPlay, Folder, Download, Edit, Delete, Refresh} from '@element-plus/icons-vue';
import type { Resource, ResourceType } from '@/types/resource';

defineProps({
  resources: {
    type: Array as () => Resource[],
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 新增课程列表属性，用于查找课程名称
  courses: {
    type: Array as () => Array<{ courseId: number; name: string }>,
    default: () => []
  }
});

defineEmits(['edit', 'delete', 'download', 'update-file']);

// 获取类型文本
const getTypeText = (type: ResourceType) => {
  return {
    'pdf': 'PDF文档',
    'ppt': 'PPT演示稿',
    'video': '视频文件',
    'doc': 'Word文档'
  }[type] ||'other :其他文件类型';
};

// 获取标签类型
const getTagType = (type: ResourceType) => {
  return {
    'pdf': 'danger',
    'ppt': 'warning',
    'video': 'primary',
    'doc': 'success'
  }[type] ||'other :info';
};

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
};
</script>

<style scoped>
.resource-list {
  min-height: 400px;
}

.resource-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-actions {
  display: flex;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 6px;
}

.el-button + .el-button {
  margin-left: 0;
}

.course-name {
  display: flex;
  justify-content: center;
}
</style>