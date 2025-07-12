<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/stores/auth';
import { useCourseStore } from '@/stores/course';
import CourseList from './CourseList.vue';
import CourseForm from './CourseForm.vue';
import CourseActions from './CourseActions.vue';
import type { Course, NullableCourse, CreateCourseRequest, UpdateCourseRequest } from '@/types/course';
import { ElMessageBox, ElNotification } from 'element-plus';

const authStore = useAuthStore();
const courseStore = useCourseStore();

const { user } = storeToRefs(authStore);
const { courses, loading, error } = storeToRefs(courseStore);

const showCourseForm = ref(false);
const currentCourse = ref<NullableCourse>(null);

// 加载当前教师课程
// src/components/course/CourseManagementView.vue
const loadCourses = () => {
  if (user.value?.role === 'teacher') {
    // 只需调用，业务逻辑在store中处理
    courseStore.loadTeacherCourses(user.value.id);
  }
};

// 添加更可靠的身份检查
onMounted(() => {
  console.log("组件挂载，用户:", user.value);
  
  if (user.value?.role === 'teacher' && user.value.id) {
    loadCourses();
  } else {
    console.warn("未获取到教师ID，无法加载课程");
    error.value = "无法识别教师身份";
  }
});

// 修复创建课程函数
const handleCreateCourse = async (data: CreateCourseRequest) => {
  try {
    // 确保包含必要的 teacherId
    const courseData: CreateCourseRequest = {
      ...data,
      teacherId: user.value?.id || 0,
      // 添加必要的缺失字段
    };
    
    console.log('创建课程数据:', courseData);
    
    await courseStore.createCourse(courseData);
    showCourseForm.value = false;
    
    // 显示成功通知
    if (typeof ElNotification !== 'undefined') {
      ElNotification.success({
        title: '成功',
        message: '课程已创建'
      });
    }
  } catch (err) {
    console.error('创建课程失败:', err);
    if (typeof ElNotification !== 'undefined') {
      ElNotification.error({
        title: '错误',
        message: '创建课程失败'
      });
    }
  }
};

// 在父组件中添加以下函数
const handleFormSubmit = (data: CreateCourseRequest | UpdateCourseRequest) => {
  if (currentCourse.value) {
    // 编辑模式：直接提交
    handleUpdateCourse(data as UpdateCourseRequest);
  } else {
    // 创建模式：添加 teacherId
    const createData: CreateCourseRequest = {
      ...(data as CreateCourseRequest),
      teacherId: user.value?.id || 0
    };
    handleCreateCourse(createData);
  }
};

// 在编辑模式打开前设置当前课程
const editCourse = (course: Course) => {
  // 关键修复：确保选择的是 store 中的课程引用
  const targetCourse = courses.value.find(c => c.courseId === course.courseId);
  
  if (targetCourse) {
    currentCourse.value = targetCourse; // 使用store中的引用
    showCourseForm.value = true;
  } else {
    console.error("编辑失败：找不到课程对象");
  }
};

// 修复更新课程函数
const handleUpdateCourse = async (data: UpdateCourseRequest) => {
  if (!currentCourse.value) return;
  
  try {
    console.log('更新课程数据:', {
      courseId: currentCourse.value.courseId,
      data
    });
    
    await courseStore.updateCourse(currentCourse.value.courseId, data);
    showCourseForm.value = false;
    currentCourse.value = null;
    
    // 显示成功提示
    ElNotification.success({
      title: '成功',
      message: '课程更新成功'
    });
  } catch (err: any) {
    console.error('更新课程失败:', err);
    ElNotification.error({
      title: '错误',
      message: `更新失败: ${err.message || '未知错误'}`
    });
  }
};

// 处理删除课程
const handleDeleteCourse = (courseId: number) => {
  if (typeof ElMessageBox !== 'undefined') {
    ElMessageBox.confirm(
      '确定要删除此课程吗？所有相关数据也将被删除',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      try {
        await courseStore.deleteCourse(courseId);
        if (typeof ElNotification !== 'undefined') {
          ElNotification.success({
            title: '成功',
            message: '课程已删除'
          });
        }
      } catch (err) {
        if (typeof ElNotification !== 'undefined') {
          ElNotification.error({
            title: '错误',
            message: '删除课程失败'
          });
        }
      }
    }).catch(() => {
      // 用户取消
    });
  }
};

</script>

<template>
  <div class="course-management-view">
    <!-- 顶部标题 -->
    <div class="header">
      <h1 v-if="!showCourseForm">课程管理</h1>
      <h1 v-else>{{ currentCourse ? '编辑课程' : '创建新课程' }}</h1>
      
      <CourseActions 
        :is-form-visible="showCourseForm"
        @create="showCourseForm = true"
        @close-form="showCourseForm = false; currentCourse = null">
      </CourseActions>
    </div>
    
    <!-- 主内容区域 -->
    <div class="content">
      <!-- 显示课程列表 -->
      <template v-if="!showCourseForm">
        <CourseList
          :courses="courses"
          :loading="loading"
          :error="error"
          @edit="editCourse"
          @delete="handleDeleteCourse" />
      </template>
      
      <!-- 显示课程表单 -->
      <template v-else>
        <CourseForm
          :course="currentCourse"
          @submit="handleFormSubmit"
          @cancel="showCourseForm = false; currentCourse = null" />
      </template>
    </div>
  </div>
</template>