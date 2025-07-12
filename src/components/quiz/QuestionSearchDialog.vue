<template>
  <!-- 遮罩层 -->
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <!-- 弹窗主体 -->
    <div class="modal-content" @click.stop>
      <!-- 弹窗头部 -->
      <div class="modal-header">
        <h3>选择题目</h3>
        <button @click="$emit('close')" class="close-btn">&times;</button>
      </div>

      <!-- 弹窗内容 -->
      <div class="modal-body">
        <!-- 筛选区 -->
        <div class="filter-section">
          <el-form :inline="true" size="small">
            <el-form-item label="课程">
              <el-select v-model="filters.courseId" placeholder="请选择课程" style="width: 120px">
                <el-option
                  v-for="course in courses"
                  :key="course.courseId"
                  :label="course.name"
                  :value="course.courseId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="filters.type" placeholder="全部题型" style="width: 120px">
                <el-option label="全部" value="" />
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTI_CHOICE" />
                <el-option label="填空题" value="FILL_IN_BLANK" />
                <el-option label="简答题" value="SHORT_ANSWER" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="filters.difficulty" placeholder="全部难度" style="width: 100px">
                <el-option label="全部" value="" />
                <el-option v-for="i in 5" :key="i" :label="i + '星'" :value="i" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识点">
              <KnowledgePointSelect v-model="filters.knowledgePointId" style="width: 150px" />
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="filters.keyword" placeholder="题干关键词" style="width: 150px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">筛选</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 题目列表 -->
        <el-table :data="questions" style="width: 100%; margin-top: 10px" height="400">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="type" label="题型" width="90">
            <template #default="scope">{{ getTypeText(scope.row.type) }}</template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="80">
            <template #default="scope">
              <span v-for="i in 5" :key="i">{{ i <= scope.row.difficulty ? '★' : '☆' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="courseName" label="课程" width="120" />
          <el-table-column prop="knowledgePointName" label="知识点" width="120" />
          <el-table-column prop="body" label="题干">
            <template #default="scope">{{ getQuestionText(scope.row.body) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button
                size="mini"
                type="primary"
                @click="handleAdd(scope.row)"
                :disabled="isAdded(scope.row.id)"
              >
                添加
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div style="margin-top: 10px; text-align: right">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page + 1"
            :page-size="size"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import KnowledgePointSelect from './KnowledgePointSelect.vue'
import { questionBankService } from '../../services/questionBank.service'
import courseService from '../../services/course.service'
import { TEACHER_ID } from '@/config/teacher'

const props = defineProps<{ visible: boolean; addedIds: number[] }>()
const emit = defineEmits(['close', 'add', 'update:visible'])

function onVisibleUpdate(val: boolean) {
  emit('update:visible', val)
}

const courses = ref([])
const questions = ref([])
const total = ref(0)
const page = ref(0)
const size = ref(10)

const filters = ref({
  courseId: '',
  type: '',
  difficulty: '',
  knowledgePointId: '',
  keyword: '',
})

const handleSearch = async () => {
  const data: any = {
    page: page.value,
    size: size.value,
  }
  if (filters.value.courseId) data.courseId = filters.value.courseId
  if (filters.value.type) data.type = filters.value.type
  if (filters.value.difficulty) data.difficulty = filters.value.difficulty
  if (filters.value.knowledgePointId) data.knowledgePointId = filters.value.knowledgePointId
  if (filters.value.keyword) data.keyword = filters.value.keyword
  const res = await questionBankService.searchQuestionsV2({
    data,
    teacherId: TEACHER_ID,
  })
  questions.value = res.content
  total.value = res.totalElements
}

const handlePageChange = (val: number) => {
  page.value = val - 1
  handleSearch()
}

const handleAdd = (row: any) => {
  emit('add', row)
}

const isAdded = (id: number) => {
  return props.addedIds.includes(id)
}

const getTypeText = (type: string) => {
  const map: any = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    FILL_IN_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
  }
  return map[type] || type
}

const getQuestionText = (body: string) => {
  try {
    const parsed = JSON.parse(body)
    return parsed.stem || parsed.content || body
  } catch {
    return body
  }
}

onMounted(async () => {
  console.log('QuestionSearchDialog弹窗已挂载', props.visible)
  courses.value = await courseService.getAllCourses()
  handleSearch()
})

watch(
  () => props.visible,
  (v) => {
    console.log('QuestionSearchDialog弹窗visible变化:', v)
    if (v) handleSearch()
  },
)
</script>

<style scoped>
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
  z-index: 3000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 1000px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #909399;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #409eff;
}

.modal-body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.filter-section {
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}
</style>
