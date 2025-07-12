<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加知识点"
    width="500px"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="名称" required>
        <el-input v-model="form.name" placeholder="知识点名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="知识点详细描述"
        />
      </el-form-item>
      <el-form-item label="父节点">
        <el-select
          v-model="form.parentId"
          placeholder="选择父节点（可选）"
          clearable
        >
          <!-- 添加一个空选项，表示没有父节点 -->
          <el-option label="无父节点" :value="null" />
          <el-option
            v-for="node in parentNodes"
            :key="node.id"
            :label="node.title"
            :value="parseInt(node.id.split('_')[1])"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useKnowledgeGraphStore } from '@/stores/knowledgeGraphStore';

const knowledgeGraphStore = useKnowledgeGraphStore();

const form = ref({
  name: '',
  description: '',
  courseId: 0,
  parentId: null as number | null
});

const props = defineProps<{
  modelValue: boolean,
  courseId: number
}>();

const emit = defineEmits(['update:modelValue', 'submit']);

const parentNodes = computed(() => {
  return knowledgeGraphStore.nodes;
});

// 修复：使用统一的 dialogVisible 变量
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

// 当弹窗打开时，设置课程ID
watch(dialogVisible, (visible) => {
  if (visible) {
    form.value.courseId = props.courseId;
  }
});

// 提交表单
function submit() {
  if (!form.value.name.trim()) {
    return;
  }

  // 发出提交事件
  emit('submit', form.value);

  // 重置表单（保留courseId）
  form.value = {
    name: '',
    description: '',
    courseId: props.courseId,
    parentId: null // 重置为null
  };

  // 关闭弹窗
  dialogVisible.value = false;
}
</script>
