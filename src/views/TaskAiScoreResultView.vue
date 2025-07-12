<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
interface TableSpanMethodProps {
  row: FlatRow
  column: unknown
  rowIndex: number
  columnIndex: number
}
interface FileScore {
  index: number
  fileName: string
  fileExtension: string
  paperGrade: number
  c1?: number
  c2?: number
  completeness?: number
  innovation?: number
  filePath: string
}
interface StudentResult {
  studentId: number
  studentName: string
  submissionId: number
  submittedAt: string
  totalGrade: number
  feedback: string
  hasAiScore: boolean
  fileScores: FileScore[]
}
interface TaskAiScoreResult {
  taskId: number
  taskTitle: string
  taskDescription: string
  studentResults: StudentResult[]
}
const resultData = ref<TaskAiScoreResult | null>(null)
const loading = ref(false)

// 需要 taskId 和 goBack 方法
const url = new URL(window.location.href)
const taskId = Number(url.pathname.match(/\d+/)?.[0] || 0)
function goBack() {
  window.history.back()
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await axios.get(`/api/ai-score/task/${taskId}/results`)
    resultData.value = res.data
  } finally {
    loading.value = false
  }
})

interface FlatRow {
  studentName: string
  fileName: string
  fileExtension: string
  paperGrade: number
  c1?: number
  c2?: number
}
const flatRows = computed<FlatRow[]>(() => {
  if (!resultData.value) return []
  const rows: FlatRow[] = []
  for (const stu of resultData.value.studentResults) {
    for (const file of stu.fileScores) {
      rows.push({
        studentName: stu.studentName,
        fileName: file.fileName,
        fileExtension: file.fileExtension,
        paperGrade: file.paperGrade,
        c1: file.c1 ?? file.completeness,
        c2: file.c2 ?? file.innovation,
      })
    }
  }
  return rows
})

function tableSpanMethod({ rowIndex, columnIndex }: TableSpanMethodProps) {
  if (columnIndex === 0) {
    // 学生姓名列
    const student = flatRows.value[rowIndex].studentName
    const firstIndex = flatRows.value.findIndex((r) => r.studentName === student)
    const count = flatRows.value.filter((r) => r.studentName === student).length
    if (rowIndex === firstIndex) {
      return { rowspan: count, colspan: 1 }
    } else {
      return { rowspan: 0, colspan: 0 }
    }
  }
}
</script>

<template>
  <div>
    <div class="header-bar">
      <el-button type="primary" @click="goBack" class="back-btn">返回</el-button>
      <div class="page-title">智能批改结果</div>
    </div>
    <el-divider />
    <el-table
      v-if="resultData"
      :data="flatRows"
      style="width: 100%; margin-top: 24px"
      :span-method="tableSpanMethod"
    >
      <el-table-column prop="studentName" label="学生姓名" width="120" />
      <el-table-column label="文件">
        <template #default="{ row }">
          <span style="color: #409eff; font-weight: 500">
            {{ row.fileName }}.{{ row.fileExtension }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="智能评分">
        <template #default="{ row }">
          <span>
            智能得分: <b style="color: #67c23a">{{ row.paperGrade ?? '-' }}</b>
          </span>
          <span style="margin-left: 12px">
            完整性: <b>{{ row.c1 ?? '-' }}</b>
          </span>
          <span style="margin-left: 12px">
            创新性: <b>{{ row.c2 ?? '-' }}</b>
          </span>
        </template>
      </el-table-column>
    </el-table>
    <div v-else>暂无数据</div>
  </div>
</template>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  background: #f5f8ff;
  padding: 24px 0 16px 0;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.06);
  position: relative;
}
.back-btn {
  min-width: 80px;
  margin-right: 24px;
  margin-left: 32px;
}
.page-title {
  font-size: 2.2rem;
  font-weight: bold;
  color: #409eff;
  letter-spacing: 2px;
}
.result-title {
  font-size: 1.3rem;
  font-weight: bold;
  color: #409eff;
  margin: 24px 0 12px 0;
  text-align: left;
}
.el-table {
  font-size: 16px;
}
.el-table th.el-table__cell {
  font-weight: bold;
  font-size: 17px;
}
</style>
