<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

//const props = defineProps<{ visible: boolean }>()
const props = defineProps<{ visible: boolean, taskId?: number }>()
const emit = defineEmits(['close'])

const ungradedList = ref<any[]>([])
const loading = ref(false)
const gradeDialogVisible = ref(false)
const currentItem = ref<any>(null)
const gradeScore = ref(0)
const gradeFeedback = ref('')
const gradeLoading = ref(false)

watch(() => props.visible, (val) => {
  if (val) fetchUngradedList()
})

async function fetchUngradedList() {
  loading.value = true
  try {
    console.log('[批改简答题弹窗] 当前taskId:', props.taskId)
    const res = await axios.get(`/api/correct/quiz/ungraded-short-answers/${props.taskId || 0}`)
    console.log('[批改简答题弹窗] 后端返回的未批改简答题:', res.data)
    ungradedList.value = res.data || []
  } catch (e) {
    ElMessage.error('获取未批改简答题失败')
    ungradedList.value = []
  } finally {
    loading.value = false
  }
}

function openGradeDialog(row: any) {
  currentItem.value = row
  gradeScore.value = 0
  gradeFeedback.value = ''
  gradeDialogVisible.value = true
}

async function submitGrade() {
  if (!currentItem.value) return
  gradeLoading.value = true
  try {
    await axios.post('/api/correct/quiz/grade-short-answer', {
      submissionId: currentItem.value.submissionId,
      questionId: currentItem.value.questionId,
      score: gradeScore.value,
      feedback: gradeFeedback.value
    })
    ElMessage.success('批改成功')
    gradeDialogVisible.value = false
    fetchUngradedList()
  } catch (e) {
    ElMessage.error('批改失败')
  } finally {
    gradeLoading.value = false
  }
}

// 修正：支持多种字段名，优先使用 questionContent，兼容 questionTitle
function getStem(questionData: any) {
  try {
    // 优先使用 questionContent 字段
    const content = questionData.questionContent || questionData.questionTitle || questionData
    const obj = typeof content === 'string' ? JSON.parse(content) : content
    return obj.stem || content
  } catch {
    return questionData.questionContent || questionData.questionTitle || '题目内容解析失败'
  }
}

// 修正：支持多种字段名，优先使用 questionContent，兼容 questionTitle
function getAnswer(questionData: any) {
  try {
    // 优先使用 questionContent 字段
    const content = questionData.questionContent || questionData.questionTitle || questionData
    const obj = typeof content === 'string' ? JSON.parse(content) : content
    return obj.answer || ''
  } catch {
    return ''
  }
}

// 新增：获取学生答案，支持多种字段名
function getStudentAnswer(row: any) {
  return row.studentAnswer || row.answer || '无答案'
}

// 新增：获取最大分数
function getMaxScore(row: any) {
  return row.maxScore || row.score || 10
}
</script> 


<template>
  <el-dialog :model-value="visible" title="批改简答题" width="800px" @close="$emit('close')">
    <div v-if="loading" style="text-align:center;padding:40px 0;">
      <el-icon><Loading /></el-icon> 加载中...
    </div>
    <div v-else>
      <el-table :data="ungradedList" style="width:100%">
        <el-table-column prop="studentName" label="学生" />
        <el-table-column label="题目">
          <template #default="{ row }">
            {{ getStem(row) }}
          </template>
        </el-table-column>
        <el-table-column label="学生答案">
          <template #default="{ row }">
            {{ getStudentAnswer(row) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">批改</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="ungradedList.length === 0" style="text-align:center;padding:30px;color:#888;">暂无待批改简答题</div>
    </div>
    <el-dialog v-model="gradeDialogVisible" title="批改简答题" width="400px" append-to-body>
      <div v-if="currentItem">
        <div style="margin-bottom:10px;"><b>题目：</b>{{ getStem(currentItem) }}</div>
        <div style="margin-bottom:10px;"><b>标准答案：</b>{{ getAnswer(currentItem) }}</div>
        <div style="margin-bottom:10px;"><b>学生答案：</b>{{ getStudentAnswer(currentItem) }}</div>
        <el-form label-width="60px">
          <el-form-item label="得分">
            <el-input-number v-model="gradeScore" :min="0" :max="getMaxScore(currentItem)" />
            <span style="margin-left:8px;">/ {{ getMaxScore(currentItem) }}</span>
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="gradeFeedback" type="textarea" :autosize="{ minRows: 2 }" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrade" :loading="gradeLoading">提交批改</el-button>
      </template>
    </el-dialog>
    <template #footer>
      <el-button @click="$emit('close')">关闭</el-button>
    </template>
  </el-dialog>
</template>
