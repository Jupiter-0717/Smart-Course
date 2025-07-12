<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import type { 
  Course, 
  CreateCourseRequest,
  UpdateCourseRequest,
  NullableCourse 
} from '@/types/course';
import { ElNotification } from 'element-plus';

const props = defineProps<{
  course?: NullableCourse
}>();

const emit = defineEmits<{
  (e: 'submit', data: CreateCourseRequest | UpdateCourseRequest): void
  (e: 'cancel'): void
}>();

// 表单引用
const formRef = ref<any>();

// 分离课程数据和表单数据
const courseCode = ref(''); // 用于课程编号
const formData = ref({
  name: '',
  description: '',
  credit: 2,
  term: ''
});

// 学期选项
const termOptions = ref([
  '2024学年 第1学期',
  '2024学年 第2学期',
  '2024学年 暑期学期'
]);

// 是否编辑模式
const isEditMode = computed(() => !!props.course);

// 课程编号验证规则 - 关键修复：添加调试输出
const validateCode = (rule: any, value: string, callback: (error?: Error) => void) => {
  console.log(`验证课程编号: "${value}", isEditMode: ${isEditMode.value}`);
  
  if (!isEditMode.value) {
    // 仅在创建模式需要验证
    if (!value) {
      callback(new Error('请输入课程编号'));
      return;
    } else if (value.trim().length === 0) {
      callback(new Error('课程编号不能为空'));
      return;
    } else if (value.length > 20) {
      callback(new Error('课程编号不能超过20个字符'));
      return;
    }
  }
  
  callback();
};

// 手动触发课程编号验证
const triggerCodeValidation = () => {
  if (formRef.value && !isEditMode.value) {
    formRef.value.validateField('code');
  }
};

// 初始化表单数据
const initForm = () => {
  if (props.course) {
    // 编辑模式：设置课程编号
    courseCode.value = props.course.code || '';
    // 填充表单数据
    formData.value = {
      name: props.course.name,
      description: props.course.description,
      credit: props.course.credit,
      term: props.course.term
    };
  } else {
    // 创建模式：重置数据和编号
    courseCode.value = '';
    formData.value = {
      name: '',
      description: '',
      credit: 2,
      term: ''
    };
    
    // 重置后立即触发一次验证
    nextTick(triggerCodeValidation);
  }
};

onMounted(initForm);
watch(() => props.course, initForm, { immediate: true });

// 课程名称验证规则
const validateName = (_: any, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入课程名称'));
  } else if (value.length > 100) {
    callback(new Error('课程名称不能超过100个字符'));
  } else {
    callback();
  }
};

// 重置表单
const resetForm = () => {
  initForm();
};

// 提交表单
const submitForm = () => {
  if (!formRef.value) return;
  
  // 在提交前手动验证编号（创建模式）
  if (!isEditMode.value) {
    formRef.value.validateField('code');
  }
  
  formRef.value.validate((valid: boolean, fields: any) => {
    console.log('表单验证结果:', valid);
    console.log('验证字段详情:', fields);
    
    if (valid) {
      if (isEditMode.value) {
        // 编辑模式：直接提交表单数据
        emit('submit', formData.value as UpdateCourseRequest);
      } else {
        // 创建模式：组合表单数据 - 父组件会添加 teacherId
        const createData: Omit<CreateCourseRequest, 'teacherId'> = {
          code: courseCode.value,
          ...formData.value
        };
        // 父组件会处理 teacherId
        emit('submit', createData as CreateCourseRequest);
      }
    } else {
      ElNotification.warning({
        title: '验证失败',
        message: '请检查表单中的错误项',
        duration: 3000
      });
    }
  });
};
</script>

<template>
  <el-form
    ref="formRef"
    :model="isEditMode ? formData : { ...formData, code: courseCode }"
    label-position="top"
    label-width="120px"
    style="max-width: 800px"
  >
    <!-- 课程编号处理 - 关键修复：添加即时验证触发 -->
    <el-form-item 
      v-if="!isEditMode"
      label="课程编号" 
      prop="code"
      :rules="[{ validator: validateCode, trigger: ['blur', 'change'] }]"
      key="code-input">
      <el-input 
        v-model="courseCode"
        placeholder="例如：A101"
        maxlength="20"
        @input="triggerCodeValidation"
      >
        <template #prefix>
          <el-icon><Document /></el-icon>
        </template>
      </el-input>
    </el-form-item>
    
    <!-- 编辑模式下显示课程编号 -->
    <el-form-item v-if="isEditMode" label="课程编号">
      <el-input :model-value="courseCode" disabled />
    </el-form-item>
    
    <!-- 课程名称 -->
    <el-form-item 
      label="课程名称" 
      prop="name"
      :rules="[{ validator: validateName, trigger: 'blur' }]">
      <el-input v-model="formData.name" placeholder="例如：软件测试与质量保证" />
    </el-form-item>
    
    <!-- 学分 -->
    <el-form-item label="学分" prop="credit">
      <el-input-number 
        v-model="formData.credit" 
        :min="1" 
        :max="10"
        placeholder="1-10分" 
        controls-position="right" />
      <span class="credit-hint">（通常1-5学分）</span>
    </el-form-item>
    
    <!-- 学期 -->
    <el-form-item label="学期" prop="term">
      <el-select v-model="formData.term" placeholder="请选择学期">
        <el-option
          v-for="term in termOptions"
          :key="term"
          :label="term"
          :value="term"
        />
      </el-select>
    </el-form-item>
    
    <!-- 课程描述 -->
    <el-form-item label="课程描述">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="4"
        placeholder="请输入课程简介、教学内容、学习目标等..."
      />
    </el-form-item>
    
    <!-- 表单操作 -->
    <el-form-item class="form-actions">
      <el-button type="primary" @click="submitForm" :loading="false">
        {{ isEditMode ? '保存更改' : '创建课程' }}
      </el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
      <el-button v-if="isEditMode" @click="resetForm" plain>重置表单</el-button>
    </el-form-item>
    
  </el-form>
</template>

<style scoped>
.credit-hint {
  color: #999;
  font-size: 12px;
  margin-left: 10px;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 12px;
}

.validation-status {
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
}

.validation-status:empty::before {
  content: "请填写课程编号";
  color: #f56c6c;
}

.debug-panel {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f8f8;
  border-radius: 4px;
  border: 1px solid #eaeaea;
  font-size: 12px;
}

.debug-panel h4 {
  margin-top: 0;
  margin-bottom: 8px;
  color: #333;
}
</style>