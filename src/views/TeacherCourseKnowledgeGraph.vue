<template>
  <div class="knowledge-graph-view">
    <!-- 页面头部 -->
    <el-page-header @back="goBack" title="返回知识图谱列表">
      <template #content>
        <div class="page-header">
          <span class="title">{{ courseName }}</span>
          <el-tag type="primary">知识图谱</el-tag>
        </div>
      </template>
    </el-page-header>

    <el-divider />

    <!-- 工具栏 -->
    <div class="controls">
      <GraphControls
        @refresh="loadGraph"
        @generate-ai="generateByAI"
        @add-point="showAddDialog"
        @delete-point="handleDeletePointClick"
        @add-resource="openAddResourceDialog"
      />
      <el-button type="info" size="medium" @click="toggleDetailPanel" :icon="Document">
        {{ showDetailPanel ? '隐藏详情' : '显示详情' }}
      </el-button>
    </div>

    <div class="main-container">
      <!-- 知识图谱展示区 -->
      <div class="graph-container">
        <KnowledgeGraph
          :nodes="normalizedNodes"
          :edges="edges"
          :loading="loading"
          @node-click="handleNodeClick"
          @node-drag="handleNodeDrag"
        />
      </div>

      <!-- 知识点详情抽屉面板 - 只显示名称和描述 -->
      <div class="detail-drawer" v-if="showDetailPanel && currentPoint">
        <div class="knowledge-point-detail">
          <div class="point-content">
            <!-- 基本信息卡片 -->
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>基本信息</span>
                </div>
              </template>

              <div class="point-info">
                <p><strong>名称：</strong>{{ currentPoint.title }}</p>
                <p><strong>描述：</strong>{{ currentPoint.description || '暂无描述' }}</p>
              </div>
            </el-card>

            <!-- 关联资源部分 - 仅保留一个卡片 -->
            <el-card class="resource-section">
              <template #header>
                <div class="card-header">
                  <span>关联资源</span>
                  <el-button type="primary" size="small" @click="openAddResourceDialog">
                    添加资源
                  </el-button>
                </div>
              </template>

              <!-- 资源列表 -->
              <el-table
                :data="currentPoint.resources"
                v-if="currentPoint.resources && currentPoint.resources.length > 0"
                stripe
                style="width: 100%"
              >
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
                      type="danger"
                      size="small"
                      plain
                      @click="removeResourceAssociation(row.id)"
                    >
                      解除关联
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div
                v-if="!currentPoint.resources || currentPoint.resources.length === 0"
                class="empty-placeholder"
              >
                <el-icon><InfoFilled /></el-icon>
                <span>暂无关联资源</span>
              </div>
            </el-card>
          </div>

          <!-- 操作按钮 -->
          <div class="actions">
            <el-button type="primary" size="medium" @click="openAddResourceDialog">
              添加资源
            </el-button>
            <el-button type="danger" size="medium" @click="confirmDeletePoint">
              删除知识点
            </el-button>
          </div>
        </div>
      </div>
    </div>
    <!-- 添加知识点弹窗 -->
    <AddKnowledgePointDialog
      v-model="showAddPointDialog"
      :course-id="courseId"
      @submit="handleAddPoint"
    />

    <!-- 知识点选择弹窗（删除专用） -->
    <el-dialog v-model="showPointSelector" title="选择要删除的知识点" width="500px">
      <el-select
        v-model="selectedDeletePointId"
        placeholder="请选择知识点"
        filterable
        class="full-width-select"
      >
        <el-option
          v-for="node in normalizedNodes"
          :key="node.id"
          :label="node.title"
          :value="parseInt(node.id.split('_')[1])"
        />
      </el-select>
      <template #footer>
        <el-button @click="showPointSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmSelectedDelete">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加资源关联弹窗 -->
    <el-dialog v-model="showAddResourceDialog" title="添加关联资源" width="700px">
      <div class="resource-manager">
        <el-input
          v-model="resourceSearchQuery"
          placeholder="搜索资源名称..."
          class="search-input"
          clearable
        >
          <template #append>
            <el-button :icon="Search" />
          </template>
        </el-input>

        <el-table :data="filteredAvailableResources" stripe style="width: 100%">
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
                :disabled="isResourceAssociated(row.id)"
                :class="{ 'is-disabled': isResourceAssociated(row.id) }"
                @click="associateResource(row.id)"
              >
                {{ isResourceAssociated(row.id) ? '已关联' : '关联' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="filteredAvailableResources.length === 0" class="empty-placeholder">
          <el-icon><InfoFilled /></el-icon>
          <span>未找到匹配的资源</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAddResourceDialog = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog v-model="showDeleteConfirm" title="删除知识点" width="500px">
      <div class="delete-confirm-content">
        <el-alert type="error" :closable="false" show-icon>
          <span
            >确定要删除知识点 <strong>"{{ selectedNodeName }}"</strong> 吗？</span
          >
        </el-alert>
        <div class="consequences">
          <p>⚠️ 此操作会永久删除该知识点及其所有关联数据：</p>
          <ul>
            <li>知识点下的所有资源关联将被移除</li>
            <li>知识点与其他知识点的关联将被移除</li>
            <li>知识点的位置信息将被删除</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDeleteConfirm = false">取消</el-button>
        <el-button type="danger" @click="deletePoint">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KnowledgeGraph from '@/components/knowledge-graph/KnowledgeGraphVisualizer.vue'
import GraphControls from '@/components/knowledge-graph/GraphControls.vue'
import AddKnowledgePointDialog from '@/components/knowledge-graph/AddKnowledgePointDialog.vue'
import ResourceAssociationManager from '@/components/knowledge-graph/ResourceAssociationManager.vue'
import { useKnowledgeGraphStore } from '@/stores/knowledgeGraphStore'
import { useCourseStore } from '@/stores/course'
import { useApiStore } from '@/stores/api'
import { ElMessage } from 'element-plus'
import {
  Search,
  InfoFilled,
  Document,
  VideoPlay,
  Picture,
  Collection,
  Close,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const knowledgeGraphStore = useKnowledgeGraphStore()
const apiStore = useApiStore()

const teacherId = ref(parseInt(route.params.teacherId as string))
const courseId = ref(parseInt(route.params.courseId as string))

// 状态变量
const showAddPointDialog = ref(false)
const showDeleteConfirm = ref(false)
const selectedNodeName = ref('')
const showAddResourceDialog = ref(false)
const availableResources = ref<any[]>([])
const resourceSearchQuery = ref('')

// 当前选中的知识点和详情面板状态
const currentPoint = ref<any>(null)
const selectedPointId = ref<number | null>(null)
const showDetailPanel = ref(false) // 控制详情面板显示

// 知识点选择器相关状态
const showPointSelector = ref(false) // 控制知识点选择器显示
const selectedDeletePointId = ref<number | null>(null) // 存储要删除的知识点ID

// 资源类型映射
const resourceTypeNames: Record<string, string> = {
  pdf: 'PDF文档',
  video: '视频',
  ppt: '幻灯片',
  doc: '文档',
  image: '图片',
}

// 资源图标映射
const resourceIcons: Record<string, any> = {
  pdf: Document,
  video: VideoPlay,
  ppt: Collection,
  doc: Document,
  image: Picture,
  default: Document,
}

// 资源颜色映射
const resourceColors: Record<string, string> = {
  pdf: '#FF6B6B',
  video: '#4D96FF',
  ppt: '#9C51E0',
  doc: '#60D394',
  image: '#FFD93D',
  default: '#6BCB77',
}

// 获取资源类型名称
const getResourceTypeName = (type: string) => {
  return resourceTypeNames[type] || type
}

// 获取资源图标组件
const getResourceIcon = (type: string) => {
  return resourceIcons[type] || resourceIcons.default
}

// 获取资源颜色
const getResourceColor = (type: string) => {
  return resourceColors[type] || resourceColors.default
}

// 资源标签类型映射
const getResourceTagType = (type: string) => {
  switch (type) {
    case 'pdf':
      return ''
    case 'video':
      return 'success'
    case 'ppt':
      return 'warning'
    case 'doc':
      return ''
    case 'image':
      return 'info'
    default:
      return 'info'
  }
}

async function openAddResourceDialog() {
  try {
    if (!currentPoint.value) {
      ElMessage.warning('请先在图谱中点击一个知识点')
      return
    }

    // 加载整个课程的可用资源
    const response = await apiStore.get(`/resources?courseId=${courseId.value}`)

    // 确保数据结构正确
    if (Array.isArray(response.data)) {
      availableResources.value = response.data.map((res) => ({
        id: res.resourceId || res.id,
        name: res.name,
        type: res.type,
        description: res.description || '无描述',
      }))
    } else {
      console.error('无法识别的可用资源数据结构:', response.data)
    }

    // 重置搜索查询
    resourceSearchQuery.value = ''

    // 显示弹窗
    showAddResourceDialog.value = true
  } catch (error) {
    console.error('加载可用资源失败:', error)
    ElMessage.error('加载可用资源失败')
  }
}
// 检查资源是否已关联（仅针对当前知识点）
const isResourceAssociated = (resourceId: number) => {
  // 如果没有当前选中的知识点或没有资源列表，则返回false
  if (!currentPoint.value || !currentPoint.value.resources) {
    return false
  }

  // 检查当前知识点的资源列表中是否包含该资源ID
  return currentPoint.value.resources.some((r: any) => r.id === resourceId)
}

// 过滤后的可用资源（不过滤已关联的资源）
const filteredAvailableResources = computed(() => {
  if (!availableResources.value || !Array.isArray(availableResources.value)) {
    return []
  }

  return availableResources.value.filter((res) => {
    return res.name?.toLowerCase().includes(resourceSearchQuery.value.toLowerCase())
  })
})

const courseName = computed(() => {
  return courseStore.courses.find((c) => c.courseId === courseId.value)?.name || '未知课程'
})

const storeNodes = computed(() => knowledgeGraphStore.nodes)
const edges = computed(() => knowledgeGraphStore.edges)
const loading = computed(() => knowledgeGraphStore.loading)

const normalizedNodes = computed(() => {
  if (!Array.isArray(storeNodes.value)) {
    return []
  }

  return storeNodes.value.map((node) => ({
    ...node,
    id: node.id,
    title: node.title,
    description: node.description,
    type: node.type,
    courseId: node.courseId,
    positionX: node.positionX ?? 0,
    positionY: node.positionY ?? 0,
    resources: fixResourcesDisplay(node.resources),
  }))
})

// 修复资源显示问题
function fixResourcesDisplay(resources: any) {
  try {
    if (!Array.isArray(resources)) {
      return []
    }

    return resources.map((res) => ({
      id: res.id || res.resourceId,
      name: res.name || res.title || '未命名资源',
      type: res.type || 'unknown',
      url: res.url || '#',
      createdAt: res.createdAt || new Date().toISOString(),
      linkedAt: res.linkat || new Date().toISOString(),
    }))
  } catch (error) {
    console.error('处理资源数据时出错:', error)
    return []
  }
}

// 初始加载
onMounted(() => {
  loadGraph()
})

// 加载图谱数据
function loadGraph() {
  knowledgeGraphStore.loadCourseGraph(teacherId.value, courseId.value)
}

// 切换详情面板显示状态
function toggleDetailPanel() {
  showDetailPanel.value = !showDetailPanel.value

  // 如果没有当前选中的知识点，尝试选择第一个
  if (!currentPoint.value && normalizedNodes.value.length > 0 && showDetailPanel.value) {
    handleNodeClick(normalizedNodes.value[0])
  }
}

// 自动选择第一个节点
watch(normalizedNodes, (newNodes) => {
  if (newNodes.length > 0 && !currentPoint.value && showDetailPanel.value) {
    handleNodeClick(newNodes[0])
  }
})

// AI一键生成
async function generateByAI() {
  try {
    await knowledgeGraphStore.generateAIGraph(teacherId.value, courseId.value)
  } catch (error) {
    console.error('AI生成失败', error)
  }
}

// 打开添加对话框
function showAddDialog() {
  showAddPointDialog.value = true
}

// 添加知识点
async function handleAddPoint(pointData: any) {
  try {
    await knowledgeGraphStore.createKnowledgePoint(teacherId.value, pointData)
    await loadGraph()
  } catch (error) {
    console.error('添加知识点失败', error)
    ElMessage.error('添加知识点失败')
  }
}

// 节点点击事件 - 直接使用节点数据
function handleNodeClick(node: any) {
  const pointId = parseInt(node.id.split('_')[1])
  selectedPointId.value = pointId

  // 直接使用节点数据设置当前知识点
  currentPoint.value = {
    id: node.id,
    title: node.title,
    description: node.description,
    type: node.type,
    resources: fixResourcesDisplay(node.resources || []),
    pointId: pointId,
  }

  // 显示详情面板
  showDetailPanel.value = true
}

// 节点拖拽事件
function handleNodeDrag(node: any, newPosition: any) {
  const pointId = node.id.split('_')[1]
  knowledgeGraphStore.updateNodePosition(teacherId.value, pointId, newPosition)
}

// 处理删除按钮点击（工具栏删除）
function handleDeletePointClick() {
  showPointSelector.value = true
  selectedDeletePointId.value = null
}

// 处理知识点选择（工具栏删除流程）
function confirmSelectedDelete() {
  if (!selectedDeletePointId.value) {
    ElMessage.warning('请选择一个知识点')
    return
  }

  const selectedNode = normalizedNodes.value.find(
    (node) => parseInt(node.id.split('_')[1]) === selectedDeletePointId.value,
  )

  if (selectedNode) {
    selectedNodeName.value = selectedNode.title
    showDeleteConfirm.value = true
    showPointSelector.value = false
  } else {
    ElMessage.error('无法找到该知识点')
  }
}

async function associateResource(resourceId: number) {
  try {
    if (!currentPoint.value) {
      ElMessage.warning('请先选择一个知识点')
      return
    }

    const pointId = currentPoint.value.pointId
    const url = `/knowledge-points/${pointId}/resources/${resourceId}`

    // 准备请求数据 - 包含当前时间戳
    const requestData = {
      linkedAt: new Date().toISOString(),
    }

    const loadingMessage = ElMessage({
      message: '关联资源中...',
      type: 'info',
      duration: 0,
    })

    await apiStore.post(url, requestData)
    loadingMessage.close()
    ElMessage.success('资源关联成功')

    // 刷新图谱
    await loadGraph()

    // 更新当前知识点资源列表
    if (currentPoint.value) {
      const newResource = availableResources.value.find((r) => r.id === resourceId)
      if (newResource) {
        // 创建资源对象并添加关联时间
        const newResourceObj = {
          ...newResource,
          linkedAt: new Date().toISOString(),
        }

        // 添加到资源列表
        if (currentPoint.value.resources) {
          currentPoint.value.resources.push(newResourceObj)
        } else {
          currentPoint.value.resources = [newResourceObj]
        }
      }
    }

    // 关闭弹窗
    showAddResourceDialog.value = false
  } catch (error: any) {
    console.error('关联资源失败:', error)

    let errorMessage = '关联资源失败'
    if (error.response?.data) {
      errorMessage = `关联失败: ${error.response.data.message || error.response.data.error || JSON.stringify(error.response.data)}`
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  }
}

// 移除资源关联
async function removeResourceAssociation(resourceId: number) {
  try {
    if (!currentPoint.value) {
      ElMessage.warning('请先选择一个知识点')
      return
    }

    const pointId = currentPoint.value.pointId
    const url = `/knowledge-points/${pointId}/resources/${resourceId}`

    const loadingMessage = ElMessage({
      message: '解除关联中...',
      type: 'info',
      duration: 0,
    })

    await apiStore.delete(url)
    loadingMessage.close()
    ElMessage.success('已解除资源关联')

    // 刷新图谱
    await loadGraph()

    // 更新当前知识点资源列表
    if (currentPoint.value.resources) {
      currentPoint.value.resources = currentPoint.value.resources.filter(
        (r: any) => r.id !== resourceId,
      )
    }
  } catch (error: any) {
    console.error('解除关联失败:', error)

    let errorMessage = '解除关联失败'
    if (error.response?.data) {
      errorMessage = `解除失败: ${error.response.data}`
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error(errorMessage)
  }
}

// 确认删除当前选中的知识点（详情面板删除）
function confirmDeletePoint() {
  if (!currentPoint.value) {
    ElMessage.warning('请先选择一个知识点')
    return
  }

  selectedNodeName.value = currentPoint.value.title
  selectedDeletePointId.value = currentPoint.value.pointId
  showDeleteConfirm.value = true
}

// 执行删除（两种删除方式共用）
async function deletePoint() {
  if (selectedDeletePointId.value) {
    try {
      await knowledgeGraphStore.deleteKnowledgePoint(teacherId.value, selectedDeletePointId.value)

      ElMessage.success(`知识点"${selectedNodeName.value}"已删除`)

      // 关闭详情面板（如果删除的是当前选中的知识点）
      if (currentPoint.value?.pointId === selectedDeletePointId.value) {
        showDetailPanel.value = false
        currentPoint.value = null
        selectedPointId.value = null
      }

      showDeleteConfirm.value = false
      selectedDeletePointId.value = null
      selectedNodeName.value = ''

      // 刷新图谱
      await loadGraph()
    } catch (error) {
      console.error('删除知识点失败:', error)
      ElMessage.error('删除知识点失败，请重试')
    }
  } else {
    ElMessage.warning('未选择要删除的知识点')
  }
}

// 返回课程列表
function goBack() {
  router.push({
    name: 'TeacherDashboard',
  })
}
</script>

<style scoped lang="scss">
.knowledge-graph-view {
  height: 100%;
  display: flex;
  flex-direction: column;
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

  .controls {
    display: flex;
    justify-content: space-between;
    margin-bottom: 15px;

    .el-button {
      margin-left: 10px;
    }
  }
}

.main-container {
  display: flex;
  flex: 1;
  gap: 20px;
  overflow: hidden;
  position: relative;
}

.graph-container {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.detail-drawer {
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  width: 350px;
  background: white;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
  transition: transform 0.3s ease;
  transform: translateX(0);
  padding: 20px;
  overflow-y: auto;
}

/* 知识点详情样式 - 与独立页面保持一致 */
.knowledge-point-detail {
  width: 100%;

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

  .actions {
    margin-top: 20px;
    display: flex;
    justify-content: space-around;

    .el-button {
      flex: 1;
      margin: 0 10px;
    }
  }
}

/* 资源管理器样式 */
.resource-manager {
  .search-input {
    margin-bottom: 20px;
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

/* 其他样式 */
.full-width-select {
  width: 100%;
}

.delete-confirm-content {
  padding: 20px;

  .warning-text {
    margin-top: 15px;
    color: #e6a23c;
  }
}

/* 注意添加资源列表的样式 */
.resource-section {
  margin-top: 20px;

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

.el-button.is-disabled {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
  background-image: none;

  &:hover {
    background-color: #f5f7fa;
    border-color: #e4e7ed;
    color: #c0c4cc;
  }
}
</style>
