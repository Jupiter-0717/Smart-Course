<template>
  <div class="course-student-management">
    <div class="header">
      <el-button type="link" icon="el-icon-arrow-left" @click="goBack">
        返回课程列表
      </el-button>
      <h3>{{ course?.name }} ({{ course?.code }}) - 学生管理</h3>
    </div>

    <div class="action-bar">
      <el-button type="primary" @click="showAddDialog = true">
        添加学生
      </el-button>
      <el-upload
        action=""
        :show-file-list="false"
        :before-upload="handleImport"
        accept=".xlsx,.xls,.csv"
      >
        <el-button type="success">批量导入</el-button>
      </el-upload>
  <el-button 
    type="warning" 
    :loading="exportLoading"
    @click="handleExport"
  >

    批量导出
  </el-button>
      <el-input
        v-model="searchQuery"
        placeholder="搜索学生..."
        class="search-input"
        clearable
        @clear="resetSearch"
      />
    </div>

    <!-- 学生列表 -->
<el-table :data="filteredStudents" style="width: 100%" border v-loading="studentStore.loading">
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="editStudent(scope.row)">编辑</el-button>
          <el-button
            size="small"
            type="danger"
            @click="confirmDelete(scope.row.studentId)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
  <el-pagination
    v-model:current-page="pagination.currentPage"
    v-model:page-size="pagination.pageSize"
    :total="pagination.totalItems"
    :page-sizes="[10, 20, 50, 100]"
    layout="total, sizes, prev, pager, next, jumper"
    @current-change="handlePageChange"
    @size-change="handleSizeChange"
  />
    <!-- 添加学生对话框 -->
    <el-dialog v-model="showAddDialog" title="添加学生" width="500px">
      <student-form
        :course-id="courseId"
        @success="handleAddSuccess"
        @cancel="showAddDialog = false"
      />
    </el-dialog>

    <!-- 编辑学生对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑学生" width="500px">
      <student-form
        :course-id="courseId"
        :student="currentStudent"
        is-edit
        @success="handleEditSuccess"
        @cancel="showEditDialog = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCourseStore } from '@/stores/course'
import StudentForm from '@/components/student/StudentForm.vue'
import { useStudentStore } from '@/stores/student'
import type { Student } from '@/types/student'
import { ElMessageBox } from 'element-plus'
import form from 'element-plus/es/components/form/index.mjs'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const studentStore = useStudentStore()

const courseId = ref(Number(route.params.courseId))
const course = computed(() => 
  courseStore.courses.find(c => c.courseId === courseId.value) || null
)
const exportLoading=ref(false)
// 搜索和分页
const searchQuery = ref('')
const pagination = ref({
  currentPage: 1, // 默认从1开始
  pageSize: 10,
  totalItems: 0
});

// 对话框控制
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const currentStudent = ref<Student | null>(null)
const showAlert = (message: string, type: 'success' | 'error' = 'success') => {
  const alertBox = document.createElement('div')
  alertBox.className = `custom-alert ${type}`
  alertBox.textContent = message
  document.body.appendChild(alertBox)
  
  setTimeout(() => {
    alertBox.remove()
  }, 3000)
}

// 过滤学生列表
const filteredStudents = computed(() => {
  if (!searchQuery.value) return studentStore.students;
  return studentStore.students.filter(student => 
    student.studentId.toString().includes(searchQuery.value) ||
    (student.studentName && student.studentName.includes(searchQuery.value)))
});

// 加载学生数据
const loadStudents = async () => {
  if (!courseId.value) return;
  
  try {
    await studentStore.loadStudents({
      courseId: courseId.value,
      page: pagination.value.currentPage - 1, // 转换为0-based
      pageSize: pagination.value.pageSize
    });
    
    // 更新本地分页信息
    pagination.value.totalItems = studentStore.pagination.totalElements;
  } catch (error) {
    console.error('加载学生列表失败:', error);
  }
}
// 分页变化处理
const handlePageChange = (newPage: number) => {
  pagination.value.currentPage = newPage;
  loadStudents();
}

const handleSizeChange = (newSize: number) => {
  pagination.value.pageSize = newSize;
  pagination.value.currentPage = 1; // 重置到第0页
  loadStudents();
}

// 初始化加载
onMounted(() => {
  loadStudents()
})

 //导出课程中学生信息
 const handleExport = async()=>{
   exportLoading.value=true;
   try{
     const courseId=Number(route.params.courseId)
     await studentStore.exportStudents(courseId)

   }catch(error){
     console.error("导出失败",error);
   }finally{
     exportLoading.value=false;
   }
 }
//单次添加学生
const handleAddSuccess = async (formData: {studentId: string, studentName: string} | null) => {
  try {
    if (!formData) {
      showAlert('表单验证失败，请检查输入', 'error');
      return;
    }
    
    console.log('[表单提交] 接收数据:', formData);

    // 直接传递字符串类型的studentId，由addStudent方法处理
    const result = await studentStore.addStudent(
      courseId.value,
      formData.studentName,
      formData.studentId
    );

    if (result.success) {
      showAlert(result.message || '添加成功');
      showAddDialog.value = false;
      await loadStudents();
    } else {
      showAlert(result.error || '添加失败', 'error');
    }
  } catch (error) {
    const errMsg = error instanceof Error ? error.message : '未知错误';
    showAlert(`操作失败: ${errMsg}`, 'error');
  }
}
// 编辑学生:没实现，组件需要
const editStudent = (student: Student) => {
  currentStudent.value = { ...student }
  showEditDialog.value = true
}

// 编辑成功处理
const handleEditSuccess = async (student:Student) => {
  showEditDialog.value = false
  await loadStudents()
}

// 批量导入学生
const handleImport = async (file: File) => {
  try {
    if (!courseId.value) return
    await studentStore.importStudents(courseId.value, file)
    showAlert('批量导入学生成功')
    await loadStudents()
  } catch (error) {
    showAlert('导入失败: ' + (error as Error).message)
  }
}

// 删除学生确认
const confirmDelete = async (studentId: number) => {
  try {
    await ElMessageBox.confirm('确定删除此学生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    if (!courseId.value) return
    await studentStore.deleteStudent(courseId.value, studentId)
    ElMessageBox.success('删除成功')
    await loadStudents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessageBox.error('删除失败: ' + (error as Error).message)
    }
  }
}



// 重置搜索
const resetSearch = () => {
  searchQuery.value = ''
}

// 返回课程列表
const goBack = () => {
  router.go(-1)
}

// 初始化加载
onMounted(() => {
  loadStudents()
})
</script>

<style scoped>
.course-student-management {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.action-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
  margin-left: auto;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>