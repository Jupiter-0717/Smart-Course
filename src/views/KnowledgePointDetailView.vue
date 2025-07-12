<template>
  <div class="knowledge-point-detail">
    <el-page-header @back="goBack" title="返回知识图谱">
      <template #content>
        <div class="page-header">
          <span class="title">{{ point?.title }}</span>
          <el-tag type="primary">知识点详情</el-tag>
        </div>
      </template>
    </el-page-header>

    <el-divider />

    <!-- 知识点详情内容 -->
    <div class="point-content">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>

        <div class="point-info">
          <p><strong>描述：</strong>{{ point?.description || '暂无描述' }}</p>
          <p><strong>类型：</strong>{{ point?.type || '未知类型' }}</p>
          <p><strong>创建时间：</strong>{{ point?.createdAt || '未知' }}</p>
        </div>
      </el-card>

      <!-- 关联资源部分 -->
      <el-card class="resource-section">
        <template #header>
          <div class="card-header">
            <span>关联资源</span>
            <el-button type="primary" size="small" @click="openAddResourceDialog">
              添加资源
            </el-button>
          </div>
        </template>

        <ResourceAssociationManager
          mode="display"
          :resources="pointResources"
          @add="openAddResourceDialog"
          @remove="removeResourceAssociation"
        />
      </el-card>
    </div>

    <!-- 添加资源关联弹窗 -->
    <el-dialog v-model="showAddResourceDialog" title="添加关联资源" width="700px">
      <ResourceAssociationManager
        mode="association"
        :available-resources="availableResources"
        @select="associateResource"
        @cancel="showAddResourceDialog = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ResourceAssociationManager from '@/components/knowledge-graph/ResourceAssociationManager.vue'
import { useApiStore } from '@/stores/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const apiStore = useApiStore()

const teacherId = ref(parseInt(route.params.teacherId as string))
const courseId = ref(parseInt(route.params.courseId as string))
const pointId = ref(parseInt(route.params.pointId as string))

// 状态变量
const point = ref<any>(null)
const pointResources = ref<any[]>([])
const availableResources = ref<any[]>([])
const showAddResourceDialog = ref(false)

// 加载知识点详情
async function loadPointDetail() {
  try {
    // 添加 /api 前缀
    const response = await apiStore.get(`/api/knowledge-points/${pointId.value}`)
    point.value = response.data
  } catch (error) {
    console.error('加载知识点详情失败', error)
    ElMessage.error('加载知识点详情失败')
  }
}

// 加载知识点关联资源
async function loadPointResources() {
  try {
    // 添加 /api 前缀
    const response = await apiStore.get(`/api/knowledge-points/${pointId.value}/resources`)
    pointResources.value = response.data
  } catch (error) {
    console.error('加载资源失败', error)
    ElMessage.error('加载资源失败')
  }
}

// 打开添加资源关联弹窗
async function openAddResourceDialog() {
  try {
    // 添加 /api 前缀
    const response = await apiStore.get(`/api/resources?courseId=${courseId.value}`)

    // 确保数据结构正确
    if (Array.isArray(response.data)) {
      availableResources.value = response.data.map((res) => ({
        id: res.resourceId || res.id,
        name: res.name,
        type: res.type,
      }))
    } else {
      console.error('无法识别的可用资源数据结构:', response.data)
    }

    // 显示弹窗
    showAddResourceDialog.value = true
  } catch (error) {
    console.error('加载可用资源失败:', error)
    ElMessage.error('加载可用资源失败')
  }
}

// 关联资源
async function associateResource(resourceId: number) {
  try {
    // 添加 /api 前缀
    const url = `/api/knowledge-points/${pointId.value}/resources/${resourceId}`

    // 显示加载中提示
    const loadingMessage = ElMessage({
      message: '关联资源中...',
      type: 'info',
      duration: 0,
    })

    await apiStore.post(url, null)

    // 关闭加载提示
    loadingMessage.close()

    // 显示成功提示
    ElMessage.success('资源关联成功')

    // 刷新资源列表
    await loadPointResources()

    // 关闭弹窗
    showAddResourceDialog.value = false
  } catch (error: any) {
    console.error('关联资源失败:', error)

    let errorMessage = '关联资源失败'
    if (error.response?.data) {
      errorMessage = `关联失败: ${error.response.data}`
    }
    ElMessage.error(errorMessage)
  }
}

// 移除资源关联
async function removeResourceAssociation(resourceId: number) {
  try {
    // 添加 /api 前缀
    const url = `/api/knowledge-points/${pointId.value}/resources/${resourceId}`

    // 显示加载中提示
    const loadingMessage = ElMessage({
      message: '移除关联中...',
      type: 'info',
      duration: 0,
    })

    await apiStore.delete(url)

    // 关闭加载提示
    loadingMessage.close()

    // 显示成功提示
    ElMessage.success('已移除资源关联')

    // 刷新资源列表
    await loadPointResources()
  } catch (error: any) {
    console.error('移除关联失败:', error)

    let errorMessage = '移除关联失败'
    if (error.response?.data) {
      errorMessage = `移除失败: ${error.response.data}`
    }
    ElMessage.error(errorMessage)
  }
}

// 返回知识图谱
function goBack() {
  router.push({
    name: 'TeacherCourseKnowledgeGraph',
    params: {
      teacherId: teacherId.value,
      courseId: courseId.value,
    },
  })
}

// 初始加载
onMounted(() => {
  loadPointDetail()
  loadPointResources()
})
</script>

<style scoped lang="scss">
.knowledge-point-detail {
  padding: 20px;

  .page-header {
    display: flex;
    align-items: center;
    gap: 15px;

    .title {
      font-size: 18px;
      font-weight: bold;
    }
  }

  .point-content {
    margin-top: 20px;

    .point-info {
      padding: 15px;

      p {
        margin-bottom: 10px;
      }
    }

    .resource-section {
      margin-top: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }
  }
}
</style>
