<template>
  <div class="resource-manager">
    <template v-if="mode === 'display'">
      <!-- 显示模式 - 表格形式展示已关联资源 -->
      <el-table :data="resources" v-if="resources.length > 0" stripe style="width: 100%">
        <el-table-column label="资源名称" prop="name" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getResourceTagType(row.type)">
              {{ getResourceTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述">
          <template #default="{ row }">
            {{ row.description || '无描述' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              plain
              @click="$emit('remove', row.id)"  <!-- 修改为传递 row.id -->
            >
              解除关联
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="resources.length === 0" class="empty-placeholder">
        <el-icon><InfoFilled /></el-icon>
        <span>暂无关联资源</span>
      </div>

      <div class="actions">
        <el-button type="primary" @click="$emit('add')">添加关联资源</el-button>
      </div>
    </template>

    <template v-else-if="mode === 'association'">
      <!-- 关联模式 - 表格形式展示可用资源 -->
      <el-input
        v-model="searchQuery"
        placeholder="搜索资源名称..."
        class="search-input"
        clearable
      >
        <template #append>
          <el-button :icon="Search" />
        </template>
      </el-input>

      <el-table :data="filteredResources" stripe style="width: 100%">
        <el-table-column label="资源名称" prop="name" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getResourceTagType(row.type)">
              {{ getResourceTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述">
          <template #default="{ row }">
            {{ row.description || '无描述' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="selectResource(row)"
            >
              关联
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredResources.length === 0" class="empty-placeholder">
        <el-icon><InfoFilled /></el-icon>
        <span>未找到匹配的资源</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Search, InfoFilled } from '@element-plus/icons-vue';

// 定义资源接口
export interface Resource {
  id: number;
  name: string;
  type: string;
  description?: string;
  url: string;
}

const props = defineProps({
  resources: {
    type: Array as () => Resource[],
    default: () => []
  },
  mode: {
    type: String,
    default: 'display'
  },
  availableResources: {
    type: Array as () => Resource[],
    default: () => []
  }
});

// 定义事件
const emit = defineEmits<{
  (event: 'select', id: number): void;
  (event: 'remove', id: number): void;  // 修改为接收数字 ID
  (event: 'add'): void;
  (event: 'cancel'): void;
}>();

const searchQuery = ref('');

// 资源类型映射
const resourceTypeNames: Record<string, string> = {
  'pdf': 'PDF文档',
  'video': '视频',
  'ppt': '幻灯片',
  'doc': '文档',
  'image': '图片'
};

// 资源标签类型映射
const getResourceTagType = (type: string) => {
  switch (type) {
    case 'pdf': return '';
    case 'video': return 'success';
    case 'ppt': return 'warning';
    case 'doc': return '';
    case 'image': return 'info';
    default: return 'info';
  }
};

const getResourceTypeName = (type: string) => {
  return resourceTypeNames[type] || type;
};

const filteredResources = computed<Resource[]>(() => {
  if (!props.availableResources || !Array.isArray(props.availableResources)) {
    return [];
  }

  return props.availableResources.filter((res: Resource) => {
    return res.name?.toLowerCase().includes(searchQuery.value.toLowerCase());
  });
});

// 选择资源
function selectResource(row: Resource) {
  emit('select', row.id);
}
</script>

<style scoped lang="scss">
.resource-manager {
  .search-input {
    margin-bottom: 20px;
  }

  .actions {
    margin-top: 20px;
    text-align: right;
  }

  .empty-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 150px;
    color: #909399;
    padding: 20px;
    border: 1px dashed #ebeef5;
    border-radius: 4px;
    background-color: #f8f8f8;

    .el-icon {
      font-size: 48px;
      margin-bottom: 10px;
      color: #c0c4cc;
    }

    span {
      font-size: 14px;
    }
  }
}
</style>