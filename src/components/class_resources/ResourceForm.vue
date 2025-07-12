<template>
  <div class="resource-form">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" label-position="top">
      <!-- 课程选择下拉框 - 修改后 -->
      <el-form-item v-if="shouldShowCourseSelect" label="关联课程" prop="courseId">
        <!-- 在编辑模式下显示课程名称（不可编辑） -->
        <div v-if="resource" class="course-name-display">
          <el-tag type="success" size="medium">
            {{ courseName }}
          </el-tag>
          <div class="hint">
            <el-icon><InfoFilled /></el-icon>
            <span>关联课程不可更改</span>
          </div>
        </div>

        <!-- 在添加模式下显示可编辑的下拉框 -->
        <el-select v-else v-model="form.courseId" placeholder="请选择关联课程" class="full-width">
          <el-option
            v-for="course in courses"
            :key="course.courseId"
            :label="course.name"
            :value="course.courseId"
          >
            <div class="course-option">
              <span>{{ course.name }}</span>
              <span class="course-id">ID: {{ course.courseId }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 资源名称 -->
      <el-form-item label="资源名称" prop="name">
        <el-input v-model="form.name" placeholder="输入资源名称" clearable />
      </el-form-item>

      <!-- 资源类型 -->
      <el-form-item label="资源类型" prop="type">
        <el-select v-model="form.type" placeholder="选择资源类型" :disabled="!!resource">
          <el-option label="PDF文档" value="pdf" />
          <el-option label="PPT演示稿" value="ppt" />
          <el-option label="视频文件" value="video" />
          <el-option label="Word文档" value="doc" />
          <el-option label="其他" value="other" />
        </el-select>
        <div v-if="resource" class="type-disabled-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>资源类型不可更改，如需更改文件类型请使用"更新文件"功能</span>
        </div>
      </el-form-item>

      <!-- 文件上传 -->
      <el-form-item v-if="!resource" label="选择新文件" prop="file">
        <el-upload
          class="upload-demo"
          drag
          :limit="1"
          :auto-upload="false"
          :on-change="handleFileChange"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 PDF, PPT, Word, MP4 格式文件，大小不超过100MB</div>
          </template>
        </el-upload>
      </el-form-item>

      <!-- 资源描述 -->
      <el-form-item label="资源描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="输入资源描述 (可选)"
        />
      </el-form-item>

      <!-- 表单操作 -->
      <el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="submitForm">
            {{ resource ? '保存修改' : '上传资源' }}
          </el-button>
          <el-button @click="$emit('cancel')">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { UploadFilled, InfoFilled } from '@element-plus/icons-vue' // 添加 InfoFilled 图标
import type { Resource, ResourceType } from '@/types/resource'
import type { Course } from '@/types/course'
import { ElNotification } from 'element-plus'

const validCourseId = computed<number>(() => {
  // 编辑模式下优先使用资源的课程ID
  if (props.resource?.courseId) {
    return props.resource.courseId
  }

  // 尝试使用表单中已有的课程ID（大于0）
  if (form.courseId && form.courseId > 0) {
    return form.courseId
  }

  // 尝试使用课程列表中的第一个
  if (props.courses.length > 0) {
    return props.courses[0].courseId
  }

  // 都没有则返回0
  return 0
})
const props = defineProps({
  resource: {
    type: Object as () => Resource | null,
    default: null,
  },
  courses: {
    type: Array as () => Course[],
    default: () => [],
  },
})

const emit = defineEmits(['submit', 'cancel'])

// 计算是否显示课程选择
const shouldShowCourseSelect = computed(() => {
  return props.courses.length > 0
})

// 计算课程名称（用于显示在编辑模式）
const courseName = computed(() => {
  if (props.resource) {
    const course = props.courses.find((c) => c.courseId === form.courseId)
    return course ? course.name : '未知课程'
  }
  return ''
})

// 表单数据
const form = reactive({
  courseId: props.resource?.courseId || (props.courses[0]?.courseId ?? 0),
  name: props.resource?.name || '',
  type: (props.resource?.type as ResourceType) || ('' as ResourceType),
  file: null as File | null,
  description: props.resource?.description || '',
})

// 表单验证规则（更新后）
const rules = reactive({
  // 在编辑模式下不需要课程ID验证
  courseId: props.resource
    ? []
    : [{ required: true, message: '请选择关联课程', trigger: 'change' }],
  name: [
    { required: true, message: '请输入资源名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
  file: props.resource ? [] : [{ required: true, message: '请上传资源文件', trigger: 'blur' }],
})

// 处理文件选择
const handleFileChange = (file: any) => {
  form.file = file.raw
}

// 提交表单
const submitForm = () => {
  if (props.resource) {
    // 编辑模式：只发送名称和描述
    const updateData = {
      name: form.name,
      description: form.description,
      courseId: form.courseId, // 确保包含课程ID
    }
    emit('submit', updateData)
  } else {
    // 确保课程ID有效
    if (!validCourseId.value || validCourseId.value <= 0) {
      ElNotification.error('请选择关联课程')
      return
    }

    form.courseId = validCourseId.value

    // 上传模式：发送完整的资源数据
    if (!form.file) {
      ElNotification.error('请选择资源文件')
      return
    }

    const resourceData = {
      ...form,
      file: form.file,
    }
    emit('submit', resourceData)
  }
}

// 当传入资源对象变化时更新表单
watch(
  () => props.resource,
  (newResource) => {
    if (newResource) {
      form.name = newResource.name
      form.type = newResource.type
      form.description = newResource.description || ''
      form.courseId = newResource.courseId || validCourseId.value // 确保有值
    } else {
      form.courseId = validCourseId.value // 设置默认值
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.resource-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.upload-demo {
  width: 100%;
}

.form-actions {
  text-align: right;
}

.course-option {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.course-id {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.full-width {
  width: 100%;
}

.el-form-item {
  margin-bottom: 24px;
}

/* 调试信息样式 */
.debug-info {
  padding: 10px;
  background-color: #f8f8f8;
  border: 1px solid #eaeaea;
  margin-bottom: 20px;
  font-size: 14px;
}

.debug-info p {
  margin: 5px 0;
}

/* 新增样式 */
.course-name-display {
  padding: 10px 0;
  border-radius: 4px;
}

.course-name-display .el-tag {
  font-size: 14px;
  padding: 8px 12px;
}

.hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
}

.hint .el-icon {
  margin-right: 6px;
}
</style>
