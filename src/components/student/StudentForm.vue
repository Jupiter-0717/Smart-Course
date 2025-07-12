<template>
  <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
    <el-form-item label="学号" prop="studentId" required>
      <el-input v-model="form.studentId" :disabled="isEdit" />
    </el-form-item>
    <el-form-item label="姓名" prop="studentName" required>
      <el-input v-model="form.studentName" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submitForm">提交</el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Student } from '@/types/student'

const props = defineProps({
  courseId: {
    type: Number,
    required: true
  },
  student: {
    type: Object as () => Student | null,
    default: null
  },
  isEdit: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['success', 'cancel'])

const form = ref({
  studentId: '',
  studentName: ''
})

const formRef = ref()
const rules = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d+$/, message: '学号必须为数字', trigger: 'blur' }
  ],
  studentName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在2到10个字符', trigger: 'blur' }
  ]
}

// 初始化表单数据
watch(() => props.student, (student) => {
  if (student) {
    form.value = {
      studentId: student.studentId.toString(),
      studentName: student.studentName
    }
  } else {
    form.value = {
      studentId: '',
      studentName: ''
    }
  }
}, { immediate: true })

const submitForm = async () => {
  try {
    await formRef.value?.validate()
    
    // 确保 studentId 有值
    if (!form.value.studentId) {
      throw new Error('学号不能为空')
    }
    
    // 构造符合 handleSuccess 要求的数据结构
    const formData = {
      studentId: form.value.studentId.trim(),  // 去除前后空格
      studentName: form.value.studentName.trim()
    }
    
    console.log('[表单提交] 准备提交数据:', formData) // 添加调试日志
    
    // 触发 success 事件，传递 formData
    emit('success', formData)
  } catch (error) {
    console.error('[表单提交错误]', error)
    // 错误处理交给父组件
    emit('success', null)
  }
}
</script>

<style scoped>
.el-form-item {
  margin-bottom: 22px;
}

.el-input {
  width: 100%;
}

.el-button + .el-button {
  margin-left: 12px;
}
</style>