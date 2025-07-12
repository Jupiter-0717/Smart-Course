<!-- 该文件用于试卷与题库功能--曹雨荷 -->
<template>
  <div class="knowledge-point-select">
    <label v-if="label" class="form-label">{{ label }}</label>
    <el-select
      v-model="selectedValue"
      filterable
      remote
      clearable
      placeholder="请输入关键字搜索知识点"
      :remote-method="handleRemoteSearch"
      :loading="loading"
      style="width: 100%"
      :disabled="!props.courseId"
      @change="handleChange"
    >
      <el-option
        v-for="point in knowledgePoints"
        :key="point.id"
        :label="point.name"
        :value="Number(point.id)"
      />
    </el-select>
    <!-- <pre>{{ knowledgePoints }}</pre> -->
  </div>
</template>

<script setup lang="ts">
// 该文件用于试卷与题库功能--曹雨荷
import { ref, watch } from 'vue'
import { knowledgePointService } from '../../services/knowledgePoint.service'
import type { KnowledgePointResponse } from '../../types/knowledgePoint'

const props = withDefaults(
  defineProps<{
    courseId?: number | null
    value?: number
    label?: string
    required?: boolean
  }>(),
  {
    required: false,
  },
)

const emit = defineEmits(['update:value', 'change'])

const knowledgePoints = ref<KnowledgePointResponse[]>([])
const allKnowledgePoints = ref<KnowledgePointResponse[]>([])
const loading = ref(false)
const selectedValue = ref<number | undefined>(props.value)

const handleRemoteSearch = async (query: string) => {
  if (!props.courseId) {
    knowledgePoints.value = []
    return
  }
  if (!query.trim()) {
    knowledgePoints.value = allKnowledgePoints.value.slice()
    return
  }
  loading.value = true
  const result = await knowledgePointService.searchKnowledgePointsByName(props.courseId, query)
  knowledgePoints.value = Array.isArray(result.data) ? result.data : []
  loading.value = false
}

const loadAllKnowledgePoints = async () => {
  console.log('加载全部知识点', props.courseId)
  if (!props.courseId) {
    knowledgePoints.value = []
    allKnowledgePoints.value = []
    return
  }
  loading.value = true
  const result = await knowledgePointService.getKnowledgePointsByCourse(props.courseId)
  console.log('知识点接口返回', result)
  allKnowledgePoints.value = Array.isArray(result.data) ? result.data : []
  knowledgePoints.value = allKnowledgePoints.value.slice()
  loading.value = false
  console.log('最终knowledgePoints', knowledgePoints.value)
}

const handleChange = (val: number) => {
  emit('update:value', val)
  emit('change', val)
}

watch(
  () => props.value,
  (val) => {
    selectedValue.value = val
  },
)

watch(
  () => selectedValue.value,
  (val) => {
    emit('update:value', val)
    emit('change', val)
  },
)

watch(
  () => props.courseId,
  () => {
    console.log('watch到courseId变化:', props.courseId)
    selectedValue.value = undefined
    loadAllKnowledgePoints()
  },
  { immediate: true },
)
</script>

<style scoped>
.knowledge-point-select {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-weight: bold;
  margin-bottom: 4px;
}

.form-control {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
}

.form-control:focus {
  outline: none;
  border-color: #1890ff;
}

.form-control:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.loading-text {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
</style>
