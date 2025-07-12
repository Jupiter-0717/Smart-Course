<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="knowledge-point-manage-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="filter-section">
        <select v-model="selectedCourseId" class="filter-select" @change="handleCourseChange">
          <option value="">请选择课程</option>
          <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
            {{ course.name }}
          </option>
        </select>
      </div>

      <div class="info-section">
        <span v-if="selectedCourseId" class="course-info">
          当前课程：{{ getCourseName(selectedCourseId) }}
        </span>
        <span v-else class="course-info">请选择课程查看知识点</span>
      </div>
    </div>

    <!-- 知识点列表 -->
    <div v-if="selectedCourseId" class="knowledge-point-list">
      <div class="list-header">
        <span class="header-item">知识点名称</span>
        <span class="header-item">描述</span>
        <span class="header-item">操作</span>
      </div>

      <div class="list-content">
        <div v-for="point in knowledgePoints" :key="point.id" class="knowledge-point-item">
          <div class="point-name">{{ point.name }}</div>
          <div class="point-description">{{ point.description || '暂无描述' }}</div>
          <div class="point-actions">
            <button @click="handleViewDetail(point)" class="action-btn view-btn">查看</button>
            <button @click="handleEdit(point)" class="action-btn edit-btn">编辑</button>
            <button @click="handleDelete(point)" class="action-btn delete-btn">删除</button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="knowledgePoints.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📚</div>
        <div class="empty-text">该课程暂无知识点</div>
        <div class="empty-hint">请联系管理员添加知识点</div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-icon">⏳</div>
        <div class="loading-text">加载中...</div>
      </div>
    </div>

    <!-- 选择课程提示 -->
    <div v-else class="select-course-prompt">
      <div class="prompt-icon">📖</div>
      <div class="prompt-text">请选择课程查看知识点</div>
    </div>

    <!-- 知识点详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="showDetailModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>知识点详情</h3>
          <button @click="showDetailModal = false" class="close-btn">&times;</button>
        </div>

        <div class="modal-body">
          <div class="detail-item">
            <label>知识点名称：</label>
            <span>{{ selectedPoint?.name }}</span>
          </div>
          <div class="detail-item">
            <label>描述：</label>
            <span>{{ selectedPoint?.description || '暂无描述' }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="showDetailModal = false" class="btn btn-primary">关闭</button>
        </div>
      </div>
    </div>

    <!-- 调试输出 -->
    <!-- <pre>{{ knowledgePoints }}</pre> -->
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, onMounted } from 'vue'
import { knowledgePointService } from '../../services/knowledgePoint.service'
import courseService from '../../services/course.service'
import type { KnowledgePointResponse } from '../../types/knowledgePoint'
import type { Course } from '../../types/course'

// 响应式数据
const courses = ref<Course[]>([])
const knowledgePoints = ref<KnowledgePointResponse[]>([])
const selectedCourseId = ref<number | ''>('')
const loading = ref(false)

// 弹窗控制
const showDetailModal = ref(false)
const selectedPoint = ref<KnowledgePointResponse | null>(null)

// 方法
const loadCourses = async () => {
  try {
    courses.value = await courseService.getAllCourses()
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const loadKnowledgePoints = async (courseId: number) => {
  try {
    loading.value = true
    const result = await knowledgePointService.getKnowledgePointsByCourse(courseId)
    knowledgePoints.value = Array.isArray(result.data) ? result.data : []
    console.log('知识点数据:', knowledgePoints.value)
  } catch (error) {
    console.error('加载知识点失败:', error)
    knowledgePoints.value = []
  } finally {
    loading.value = false
  }
}

const handleCourseChange = () => {
  if (selectedCourseId.value) {
    loadKnowledgePoints(selectedCourseId.value)
  } else {
    knowledgePoints.value = []
  }
}

const getCourseName = (courseId: number) => {
  const course = courses.value.find((c) => c.courseId === courseId)
  return course?.name || '未知课程'
}

const handleViewDetail = (point: KnowledgePointResponse) => {
  selectedPoint.value = point
  showDetailModal.value = true
}

const handleEdit = (point: KnowledgePointResponse) => {
  // TODO: 实现编辑功能
  console.log('编辑知识点:', point)
  alert('编辑功能待实现')
}

const handleDelete = (point: KnowledgePointResponse) => {
  // TODO: 实现删除功能
  console.log('删除知识点:', point)
  alert('删除功能待实现')
}

// 生命周期
onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.knowledge-point-manage-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  min-width: 200px;
}

.info-section {
  display: flex;
  align-items: center;
}

.course-info {
  font-size: 14px;
  color: #666;
}

.knowledge-point-list {
  flex: 1;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.list-header {
  display: grid;
  grid-template-columns: 1fr 2fr 150px;
  gap: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.list-content {
  max-height: 400px;
  overflow-y: auto;
}

.knowledge-point-item {
  display: grid;
  grid-template-columns: 1fr 2fr 150px;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.knowledge-point-item:hover {
  background: #f5f5f5;
}

.point-name {
  font-weight: 500;
  color: #333;
}

.point-description {
  color: #666;
  font-size: 14px;
}

.point-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.view-btn {
  background: #1890ff;
  color: white;
}

.edit-btn {
  background: #52c41a;
  color: white;
}

.delete-btn {
  background: #ff4d4f;
  color: white;
}

.empty-state,
.loading-state,
.select-course-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon,
.loading-icon,
.prompt-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text,
.loading-text,
.prompt-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #999;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.detail-item {
  display: flex;
  margin-bottom: 16px;
}

.detail-item label {
  font-weight: 500;
  color: #333;
  min-width: 100px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  text-align: right;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  margin-right: 8px;
}
</style>
