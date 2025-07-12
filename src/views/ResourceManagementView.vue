<template>
  <div class="resource-management-view">
    <!-- 顶部标题区 -->

        <div class="header">
      <div class="header-container">
      <h1 v-if="!showResourceForm && !showFileUpdateForm">{{ courseId > 0 ? `学习资源管理 - 课程 ` : '学习资源管理' }}</h1>
      <h1 v-else-if="showResourceForm">{{ selectedResource ? '编辑资源' : '上传新资源' }}</h1>
      <h1 v-else-if="showFileUpdateForm">更新资源文件</h1>
      </div>
      
      <!-- 资源操作按钮 -->
      <div class="actions">
        <!-- 居中放置搜索框 -->
        <div class="search-container">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索资源名称..."
            clearable
            @input="handleSearchInput"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 上传按钮放在右侧 -->
        <div class="upload-container">
          <el-button 
            v-if="!showResourceForm && !showFileUpdateForm && courseId > 0"
            type="primary"
            size="medium"
            @click="startUpload"
          >
            <el-icon><Upload /></el-icon> 上传新资源
          </el-button>
          <el-button 
            v-if="showResourceForm || showFileUpdateForm"
            type="info"
            size="medium"
            @click="closeAllForms"
          >
            <el-icon><Close /></el-icon> 取消
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="content">
      <!-- 课程未选择提示 -->
      <div v-if="courseId <= 0" class="course-warning">
        <el-alert
          title="请先选择课程"
          type="warning"
          :closable="false"
          show-icon
        >
          <p>您需要在课程管理中选择一个课程</p>
          <div class="mt-4">
            <el-button type="primary" plain @click="$emit('switch-to-courses')">
              <el-icon><SwitchButton /></el-icon> 前往课程管理
            </el-button>
          </div>
        </el-alert>
      </div>
      
      <!-- 课程已选择 -->
      <template v-else>
        <!-- 显示资源列表 -->
        <template v-if="!showResourceForm && !showFileUpdateForm">
          <!-- 搜索结果统计 -->
          <div v-if="isSearching" class="search-info">
            <el-icon class="loading-icon"><Loading /></el-icon>
            正在搜索...
          </div>
          
          <div v-else-if="searchApplied" class="search-info">
            找到 {{ filteredResources.length }} 个匹配的资源
            <el-button 
              type="text"
              @click="clearSearch"
            >
              清除搜索
            </el-button>
          </div>

          <!-- 显示资源列表 -->
          <ResourceList
            :resources="filteredResources"
            :courses="courses"
            :loading="isSearching"
            @edit="editResource"
            @update-file="handleUpdateFileResource"
            @delete="handleDeleteResource"
            @download="handleDownloadResource"
          />
        </template>
        
        <!-- 显示资源上传/编辑表单 -->
        <template v-else-if="showResourceForm">
          <ResourceForm
            :resource="selectedResource"
            :courses="courses"
            @submit="handleFormSubmit"
            @cancel="closeResourceForm"
          />
        </template>
        
        <!-- 显示文件更新表单 -->
        <template v-else-if="showFileUpdateForm">
          <ResourceFileUpdateForm
           v-if="selectedResource"
            :resource="selectedResource"
            @submit="handleFileUpdateSubmit"
            @cancel="closeFileUpdateForm"
          />
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount, computed } from 'vue';
import { Search, Upload, Close, SwitchButton, Loading } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import ResourceList from '@/components/class_resources/ResourceList.vue';
import ResourceForm from '@/components/class_resources/ResourceForm.vue';
import { ElMessageBox, ElNotification } from 'element-plus';
import type { Course } from '@/types/course'; 
import { 
  getAllCourses,
  getAllTeacherResources,
  uploadResource as uploadResourceApi,
  updateResource as updateResourceApi,
  deleteResource as deleteResourceApi,
  updateResourceFile as updateResourceFileApi
} from '@/services/resource.service';
import type { Resource, ResourceUpdateRequest, ResourceUploadRequest } from '@/types/resource';
import ResourceFileUpdateForm from '@/components/class_resources/ResourceFileUpdateForm.vue';

const props = defineProps({
  courseId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['switch-to-courses']);

const authStore = useAuthStore();
const courses = ref<Course[]>([]);
const allResources = ref<Resource[]>([]); // 存储所有资源（不搜索时使用）
const resources = ref<Resource[]>([]); // 当前显示的资源列表
const loading = ref(false);
const showResourceForm = ref(false);
const showFileUpdateForm = ref(false);
const selectedResource = ref<Resource | null>(null);

// 搜索相关状态
const searchKeyword = ref<string>('');
const isSearching = ref(false);
const searchApplied = ref(false);
const searchTimer = ref<number | null>(null);

// 实时搜索过滤的资源
const filteredResources = computed(() => {
  if (!searchApplied.value || !searchKeyword.value.trim()) {
    return resources.value;
  }
  
  const keyword = searchKeyword.value.toLowerCase();
  return allResources.value.filter(resource => 
    resource.name?.toLowerCase().includes(keyword)
  );
});

// 关闭所有表单
const closeAllForms = () => {
  showResourceForm.value = false;
  showFileUpdateForm.value = false;
  selectedResource.value = null;
};

// 加载课程
const loadCourses = async () => {
  try {
    courses.value = await getAllCourses();
    
    if (authStore.user?.role === 'teacher') {
      if (authStore.user.teacherId === undefined || authStore.user.teacherId === 0) {
        // 尝试从用户名中提取教师ID
        const username = authStore.user.account;
        if (username?.startsWith('teacher_')) {
          const parts = username.split('_');
          if (parts.length >= 2) {
            const teacherId = parseInt(parts[1]);
            if (!isNaN(teacherId)) {
              authStore.user.teacherId = teacherId;
            }
          }
        }
        
        if (!authStore.user.teacherId) {
          ElNotification.error('无法识别教师身份');
          return;
        }
      }
    }
  } catch (error) {
    console.error('加载课程失败', error);
    ElNotification.error('加载课程列表失败');
  }
};

// 监听课程ID变化
watch(() => props.courseId, (newId) => {
  if (newId > 0) {
    fetchResources();
  } else {
    allResources.value = [];
    resources.value = [];
  }
});

// 处理搜索输入 (带防抖)
const handleSearchInput = () => {
  // 清除之前的定时器
  if (searchTimer.value) {
    clearTimeout(searchTimer.value);
  }
  
  // 设置新的定时器
  searchTimer.value = setTimeout(() => {
    performSearch();
  }, 300) as unknown as number;
};

// 执行实时搜索
const performSearch = () => {
  if (searchKeyword.value.trim() === '') {
    clearSearch();
    return;
  }
  
  searchApplied.value = true;
  isSearching.value = true;
  
  // 使用setTimeout模拟搜索耗时
  setTimeout(() => {
    isSearching.value = false;
    ElNotification.success(`找到 ${filteredResources.value.length} 条结果`);
  }, 150);
};

// 清除搜索
const clearSearch = () => {
  searchKeyword.value = '';
  searchApplied.value = false;
  resources.value = [...allResources.value]; // 恢复显示所有资源
};

// 获取资源
const fetchResources = async () => {
  try {
    loading.value = true;
    
    const teacherId = authStore.user?.teacherId;
    if (!teacherId) {
      ElNotification.warning('无法获取资源：教师ID未识别');
      return;
    }
    
    // 获取所有资源
    const resourcesData = await getAllTeacherResources({ teacherId });
    
    // 添加课程名称
    resourcesData.forEach(resource => {
      const course = courses.value.find(c => c.courseId === resource.courseId);
      resource.courseName = course ? course.name : '未知课程';
    });
    
    // 保存到两个列表
    allResources.value = [...resourcesData];
    resources.value = [...resourcesData];
    
  } catch (err: any) {
    console.error('获取资源失败:', err);
    ElNotification.error(`获取资源失败: ${err.message || '未知错误'}`);
    allResources.value = [];
    resources.value = [];
  } finally {
    loading.value = false;
  }
};

// 启动资源上传
const startUpload = () => {
  selectedResource.value = null;
  showResourceForm.value = true;
};

// 关闭资源表单
const closeResourceForm = () => {
  showResourceForm.value = false;
  selectedResource.value = null;
};

// 编辑资源
const editResource = (resource: Resource) => {
  if (!resource.resourceId) {
    ElNotification.error('无法编辑资源：缺少资源ID');
    return;
  }
  
  selectedResource.value = { ...resource } as Resource;
  showResourceForm.value = true;
};

// 处理表单提交
const handleFormSubmit = async (data: any) => {
  if (selectedResource.value) {
    await handleUpdateResource(data);
  } else {
    await handleUploadResource(data);
  }
};

// 上传新资源
// 上传新资源处理 - 确保使用有效课程ID
const handleUploadResource = async (data: ResourceUploadRequest) => {
  try {
    loading.value = true;
    
    // 添加调试日志 - 检查上传数据
    console.log('上传资源数据:', {
      courseId: data.courseId,
      name: data.name,
      type: data.type,
      file: data.file.name
    });
    
    const newResource = await uploadResourceApi(data);
    
    // 为新资源设置课程名称
    const course = courses.value.find(c => c.courseId === newResource.courseId);
    newResource.courseName = course ? course.name : '未知课程';
    
    // 更新本地资源列表
    resources.value.push(newResource);
    allResources.value.push(newResource);
    
    ElNotification.success('资源上传成功');
    closeResourceForm();
  } catch (err: any) {
    console.error('上传失败:', err);
    let errorMsg = '上传失败';
    
    if (err.response) {
      errorMsg = `服务器错误: ${err.response.status}`;
      if (err.response.data?.message) {
        errorMsg += ` - ${err.response.data.message}`;
      }
    } else if (err.message) {
      errorMsg = err.message;
    }
    
    ElNotification.error(`上传失败: ${errorMsg}`);
  } finally {
    loading.value = false;
  }
};

// 更新资源处理 - 包含课程ID
// 更新资源
const handleUpdateResource = async (data: ResourceUpdateRequest) => {
  if (!selectedResource.value || !selectedResource.value.resourceId) {
    ElNotification.error('无法更新资源：资源ID未定义');
    return;
  }

  try {
    loading.value = true;
    
    // 保存当前的课程ID和名称
    const currentCourseId = selectedResource.value.courseId;
    const currentCourseName = selectedResource.value.courseName;
    
    const updatedResource = await updateResourceApi(selectedResource.value.resourceId, data);
    
    // 恢复课程信息
    updatedResource.courseId = currentCourseId;
    updatedResource.courseName = currentCourseName;
    
    // 更新本地资源列表
    const index = resources.value.findIndex(r => r.resourceId === updatedResource.resourceId);
    if (index !== -1) {
      resources.value.splice(index, 1, updatedResource);
    }
    
    ElNotification.success('资源信息已更新');
    closeResourceForm();
  } catch (err: any) {
    console.error('更新失败:', err);
    ElNotification.error(`更新失败: ${err.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
};

// 处理文件更新请求
const handleUpdateFileResource = (resource: Resource) => {
  selectedResource.value = resource;
  showFileUpdateForm.value = true;
};

// 关闭文件更新表单
const closeFileUpdateForm = () => {
  showFileUpdateForm.value = false;
  selectedResource.value = null;
};

// 处理文件更新提交
// 修改 handleFileUpdateSubmit 方法
const handleFileUpdateSubmit = async (file: File) => {
  if (!selectedResource.value || !selectedResource.value.resourceId) {
    ElNotification.error('无法更新文件：资源ID未定义');
    return;
  }

  // 确保资源对象有课程ID
  if (!selectedResource.value.courseId) {
    // 尝试从资源的描述中提取课程ID（如果有）
    const extractedCourseId = extractCourseIdFromResource(selectedResource.value);
    
    if (extractedCourseId > 0) {
      // 更新本地资源对象和表单中的课程ID
      selectedResource.value.courseId = extractedCourseId;
      selectedResource.value.courseName = courses.value.find(c => 
        c.courseId === extractedCourseId)?.name || '未知课程';
    } else {
      ElNotification.error('资源缺少课程ID，请先编辑资源添加课程信息');
      return;
    }
  }

  try {
    loading.value = true;

    // 创建包含课程ID的更新请求
    const updateData = {
      file: file,
      courseId: selectedResource.value.courseId
    };
    
    // 调用API时传递包含课程ID的对象
    const updatedResource = await updateResourceFileApi(
      selectedResource.value.resourceId, 
      updateData
    );
    
    // 恢复课程信息（防止被后端覆盖）
    updatedResource.courseId = selectedResource.value.courseId;
    updatedResource.courseName = selectedResource.value.courseName;
    
    // 更新本地资源列表
    const index = resources.value.findIndex(r => r.resourceId === updatedResource.resourceId);
    if (index !== -1) {
      resources.value.splice(index, 1, updatedResource);
    }
    
    ElNotification.success('资源文件已更新');
    closeFileUpdateForm();
  } catch (err: any) {
    console.error('文件更新失败:', err);
    
    let errorMsg = '文件更新失败';
    if (err.response) {
      // 尝试获取后端返回的具体错误信息
      errorMsg = err.response.data?.message || JSON.stringify(err.response.data);
    } else if (err.message) {
      errorMsg = err.message;
    }
    
    ElNotification.error(`文件更新失败: ${errorMsg}`);
  } finally {
    loading.value = false;
  }
};

// 辅助函数：从资源描述中提取课程ID
const extractCourseIdFromResource = (resource: Resource): number => {
  if (!resource.description) return 0;
  
  // 尝试从描述中提取课程ID
  const match = resource.description.match(/课程ID[:：]\s*(\d+)/);
  if (match && match[1]) {
    return parseInt(match[1]);
  }
  
  return 0;
};

// 下载资源
const handleDownloadResource = async (resource: Resource) => {
  try {
    // 构建下载URL
    const downloadUrl = `http://localhost:8080/api/resources/download/${resource.resourceId}`;
    
    // 创建隐藏的下载链接
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = resource.name || 'resource';
    link.style.display = 'none';
    
    // 添加到DOM并触发点击
    document.body.appendChild(link);
    link.click();
    
    // 清理
    document.body.removeChild(link);
    
    ElNotification.success('下载已开始');
  } catch (err: any) {
    ElNotification.error(`下载失败: ${err.message || '未知错误'}`);
  }
};


// 删除资源
const handleDeleteResource = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除此资源吗？删除后不可恢复',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await deleteResourceApi(id);
    resources.value = resources.value.filter(r => r.resourceId !== id);
    ElNotification.success('资源已删除');
  } catch (err: any) {
    if (err !== 'cancel') {
      ElNotification.error(`删除失败: ${err.message || '未知错误'}`);
    }
  }
};

onMounted(() => {
  loadCourses();
  if (props.courseId > 0) {
    fetchResources();
  }
});

// 初始化
onMounted(() => {
  loadCourses();
  if (props.courseId > 0) {
    fetchResources();
  }
});

// 清理定时器
onBeforeUnmount(() => {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value);
  }
});
</script>

<style scoped>
/* 搜索容器居中样式 */
.search-container-center {
  display: flex;
  justify-content: center;
  flex-grow: 1;
  max-width: 500px;
  margin: 0 auto;
}

.search-container-center .el-input {
  width: 100%;
}

/* 上传容器靠右样式 */
.upload-container {
  margin-left: auto;
}

/* 搜索信息样式 */
.search-info {
  padding: 10px 0;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  margin-right: 8px;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 整体布局样式 */
.resource-management-view {
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  min-height: 80vh;
}

.header {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.header h1 {
  margin: 0 auto;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.actions {
  display: flex;
  align-items: center;
  width: 100%;
}

.content {
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.course-warning {
  padding: 20px;
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
}

.course-warning p {
  margin: 10px 0;
  color: #606266;
}
</style>