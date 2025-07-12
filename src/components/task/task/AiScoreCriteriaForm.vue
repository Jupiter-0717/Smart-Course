<script setup lang="ts">
import { ref, watch, defineEmits, defineProps } from 'vue'
import { ElMessageBox } from 'element-plus'

interface Criteria {
  criteria1: string
  c1: number
  criteria2: string
  c2: number
  criteria3: string
  c3: number
  criteria4: string
  c4: number
  criteria5: string
  c5: number
  criteria6: string
  c6: number
}

const props = defineProps<{
  modelValue?: Criteria
  loading?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'ai-score'])

const criteria = ref(
  props.modelValue || {
    criteria1: '',
    c1: 0,
    criteria2: '',
    c2: 0,
    criteria3: '',
    c3: 0,
    criteria4: '',
    c4: 0,
    criteria5: '',
    c5: 0,
    criteria6: '',
    c6: 0,
  },
)

watch(
  criteria,
  (val) => {
    emit('update:modelValue', val)
  },
  { deep: true },
)

const handleAiScore = () => {
  // 校验必填
  if (
    !criteria.value.criteria1 ||
    !criteria.value.criteria2 ||
    !criteria.value.criteria3 ||
    !criteria.value.criteria4 ||
    !criteria.value.criteria5
  ) {
    ElMessageBox.alert('请填写所有必填标准', '警告', { type: 'warning' })
    return
  }
  emit('ai-score', criteria.value)
}
</script>

<template>
  <el-card class="criteria-card" style="margin-bottom: 24px">
    <div style="font-weight: bold; margin-bottom: 12px">智能批改标准与权重</div>
    <el-form :inline="true" label-width="120px">
      <!-- 第一行：标准1和标准2 -->
      <div class="criteria-flex-row">
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 标准1*：<br />结构规范性 </span>
          </template>
          <el-input
            v-model="criteria.criteria1"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="权重1*" class="weight-tight">
          <el-input-number
            v-model="criteria.c1"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 标准2*：<br />内容符合主题 </span>
          </template>
          <el-input
            v-model="criteria.criteria2"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="权重2*" class="weight-tight">
          <el-input-number
            v-model="criteria.c2"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
      </div>

      <!-- 第二行：标准3和标准4 -->
      <div class="criteria-flex-row" style="margin-top: 8px">
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 标准3*：<br />逻辑性 </span>
          </template>
          <el-input
            v-model="criteria.criteria3"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="权重3*" class="weight-tight">
          <el-input-number
            v-model="criteria.c3"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 标准4*：<br />语言规范 </span>
          </template>
          <el-input
            v-model="criteria.criteria4"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="权重4*" class="weight-tight">
          <el-input-number
            v-model="criteria.c4"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
      </div>

      <!-- 第三行：标准5和选填标准 -->
      <div class="criteria-flex-row" style="margin-top: 8px">
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 标准5*：<br />创新性 </span>
          </template>
          <el-input
            v-model="criteria.criteria5"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="权重5*" class="weight-tight">
          <el-input-number
            v-model="criteria.c5"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span style="display: block; text-align: left"> 选填标准<br />自定义 </span>
          </template>
          <el-input
            v-model="criteria.criteria6"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入该标准下的具体要求"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="选填权重" class="weight-tight">
          <el-input-number
            v-model="criteria.c6"
            :min="0"
            :max="1"
            :step="0.01"
            style="width: 120px"
          />
        </el-form-item>
      </div>
      <el-form-item>
        <el-button type="primary" :loading="props.loading" @click="handleAiScore"
          >智能批改</el-button
        >
        <span class="ai-score-tip">
          只支持智能批改word(.docx)和pdf(.pdf)文件，所有标准的权重值相加之和为1
        </span>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.criteria-card {
  max-width: 100%;
  margin: 0 auto 24px auto;
  background: #f9f9f9;
}
.ai-score-tip {
  margin-left: 20px;
  color: #909399;
  font-size: 14px;
  vertical-align: middle;
}
.criteria-flex-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}
.weight-tight {
  margin-left: 0;
}
</style>
