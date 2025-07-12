<script setup lang="ts">
  import { ref, onMounted, watch, computed } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    TaskType,
    type TaskRequest,
    type TaskResponse,
    uploadTaskResources,
    getTasksByCourse,
    deleteTask,
    getResourcesByTask,
    type TaskResourceResponse,
    createTask,
    // 新增：导入试卷相关类型和工具函数
    type QuizDetailResponse,
    parseQuestionBody,
    getQuestionTypeName,
    getDifficultyName,
    // 新增：导入试卷选择相关
    getAvailableQuizzes,
    getQuizDetail,
    type QuizResponse,
  } from '@/services/task'

  import {
    Document,
    Plus,
    Search,
    Refresh,
    School,
    Clock,
    Folder,
    Delete,
    EditPen,
    QuestionFilled,
    Upload,
    ArrowDown,
    View,
  } from '@element-plus/icons-vue'
  import { useAuthStore } from '@/stores/auth'

  import { useRouter } from 'vue-router'
  const router = useRouter()
  function goToSubmissions(taskId: number) {
    router.push(`/teacher/tasks/${taskId}/submissions`)
  }

  // 修改 props，接收课程列表和当前选中的课程ID
  const props = defineProps<{
    courseId: number
    courses?: Array<{ courseId: number; name: string; code: string }>
  }>()

  // 新增：本地变量用于 v-model
  const selectedCourseId = ref(props.courseId)

  // 监听 props.courseId 变化，同步本地变量
  watch(
    () => props.courseId,
    (newVal) => {
      selectedCourseId.value = newVal
    },
  )

  // 定义事件
  const emit = defineEmits<{
    courseChange: [courseId: number]
  }>()

  const auth = useAuthStore()

  // 弹窗控制
  const createDialogVisible = ref(false)
  const loading = ref(false)

  // 分页相关
  const currentPage = ref(1)
  const pageSize = ref(8)
  const total = ref(0)

  // 查询条件
  const searchForm = ref({
    courseId: props.courseId,
    taskType: '',
    keyword: '',
    dateRange: [] as string[],
    status: '',
  })

  // 所有任务
  const tasks = ref<TaskResponse[]>([])

  // 每个任务对应的资源列表
  const taskResources = ref<Record<number, TaskResourceResponse[]>>({})

  // 新建任务数据
  const newTask = ref<TaskRequest>({
    courseId: props.courseId,
    title: '',
    description: '',
    type: TaskType.HOMEWORK,
    quizId: null,
    dueDate: null,
  })

  // 文件相关
  const selectedFiles = ref<File[]>([])
  interface UploadFile {
    name: string
    url?: string
    raw: File
  }
  const fileList = ref<UploadFile[]>([])

  // 在现有的响应式变量后添加
  // 试卷详情弹窗控制
  const quizDetailVisible = ref(false)
  const currentQuizDetail = ref<QuizDetailResponse | null>(null)

  // 新增：试卷选择相关
  const availableQuizzes = ref<QuizResponse[]>([])
  const selectedQuizId = ref<number | null>(null)
  const quizLoading = ref(false)
  const quizPreviewVisible = ref(false)
  const currentQuizPreview = ref<QuizDetailResponse | null>(null)

  // 监听fileList变化
  watch(
    fileList,
    (newFileList) => {
      selectedFiles.value = newFileList.map((f) => f.raw)
    },
    { deep: true },
  )

  // 监听课程ID变化
  watch(
    () => props.courseId,
    (newCourseId) => {
      searchForm.value.courseId = newCourseId
      newTask.value.courseId = newCourseId
      loadTasks()
    },
  )

  // 处理课程选择变化
  const handleCourseChange = (courseId: number) => {
    emit('courseChange', courseId)
  }

  // 文件选择处理函数
  function handleBeforeUpload(file: File) {
    return false
  }

  // 文件移除处理函数
  function handleFileRemove(file: UploadFile) {
    // watch会自动处理
  }

  // 打开创建任务弹窗
  const openCreateDialog = () => {
    createDialogVisible.value = true
    // 重置表单
    newTask.value = {
      courseId: props.courseId, // 确保使用正确的课程ID
      title: '',
      description: '',
      type: TaskType.HOMEWORK,
      quizId: null,
      dueDate: null,
    }
    selectedFiles.value = []
    fileList.value = []
    selectedQuizId.value = null
    availableQuizzes.value = []
  }

  // 关闭创建任务弹窗
  const closeCreateDialog = () => {
    createDialogVisible.value = false
  }

  // 创建任务 + 上传资源
  const onCreateTask = async () => {
    if (!newTask.value.title.trim()) {
      ElMessage.warning('请输入任务标题')
      return
    }

    // 如果是测验类型，检查是否选择了试卷
    if (newTask.value.type === TaskType.QUIZ && !selectedQuizId.value) {
      ElMessage.warning('请选择试卷')
      return
    }

    loading.value = true
    try {
      // 设置选中的试卷ID
      if (newTask.value.type === TaskType.QUIZ) {
        newTask.value.quizId = selectedQuizId.value
      }

      const created = await createTask(newTask.value)
      ElMessage.success('任务创建成功')

      // 新增：如果是QUIZ类型且有试卷详情，显示试卷信息
      if (created.type === TaskType.QUIZ && created.quizDetail) {
        console.log('试卷详情:', created.quizDetail)

        // 显示试卷详情弹窗
        currentQuizDetail.value = created.quizDetail
        quizDetailVisible.value = true
      }

      // 检查是否需要上传文件
      if (newTask.value.type === TaskType.REPORT && selectedFiles.value.length > 0) {
        const teacherId = Number(auth.user!.teacherId)
        try {
          await uploadTaskResources(created.id, teacherId, selectedFiles.value)
          ElMessage.success('资源上传成功')
        } catch (uploadError) {
          ElMessage.error(`资源上传失败: ${uploadError.message}`)
        }
      }

      await loadTasks()
      closeCreateDialog()
    } catch (e) {
      ElMessage.error(`创建失败: ${e.message}`)
    } finally {
      loading.value = false
    }
  }

  // 删除任务
  const onDeleteTask = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除这个任务吗？', '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })

      await deleteTask(id)
      ElMessage.success('删除成功')
      await loadTasks()
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('删除失败')
      }
    }
  }

  // 查询任务
  const searchTasks = async () => {
    currentPage.value = 1
    await loadTasks()
  }

  // 重置查询条件
  const resetSearch = () => {
    searchForm.value = {
      courseId: props.courseId,
      taskType: '',
      keyword: '',
      dateRange: [],
      status: '',
    }
    currentPage.value = 1
    loadTasks()
  }

  // 过滤后的任务列表
  const filteredTasks = computed(() => {
    let result = tasks.value

    // 根据课程ID筛选
    if (searchForm.value.courseId) {
      result = result.filter((task) => task.courseId === searchForm.value.courseId)
    }

    // 根据任务类型筛选
    if (searchForm.value.taskType) {
      result = result.filter((task) => task.type === searchForm.value.taskType)
    }

    // 根据关键词搜索
    if (searchForm.value.keyword) {
      const keyword = searchForm.value.keyword.toLowerCase()
      result = result.filter(
        (task) =>
          task.title.toLowerCase().includes(keyword) ||
          (task.description && task.description.toLowerCase().includes(keyword)),
      )
    }

    // 根据时间范围筛选
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      const startDate = new Date(searchForm.value.dateRange[0])
      const endDate = new Date(searchForm.value.dateRange[1])
      result = result.filter((task) => {
        const taskDate = new Date(task.createdAt)
        return taskDate >= startDate && taskDate <= endDate
      })
    }

    // 根据状态筛选
    if (searchForm.value.status) {
      const now = new Date()
      result = result.filter((task) => {
        if (!task.dueDate) return searchForm.value.status === 'no-due'

        const dueDate = new Date(task.dueDate)
        if (searchForm.value.status === 'overdue') {
          return dueDate < now
        } else if (searchForm.value.status === 'upcoming') {
          return dueDate > now
        }
        return true
      })
    }

    return result
  })

  // 分页计算
  const paginatedTasks = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredTasks.value.slice(start, end)
  })

  // 处理页码变化
  const handlePageChange = (page: number) => {
    currentPage.value = page
  }

  // 获取任务类型标签
  const getTaskTypeTag = (type: TaskType) => {
    const typeMap = {
      [TaskType.HOMEWORK]: { type: 'primary', label: '作业' },
      [TaskType.QUIZ]: { type: 'warning', label: '测验' },
      [TaskType.REPORT]: { type: 'success', label: '报告' },
    }
    return typeMap[type] || { type: 'info', label: '未知' }
  }

  // 格式化时间
  const formatDateTime = (dateString: string) => {
    if (!dateString) return '无截止时间'
    return new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  // 获取任务状态
  const getTaskStatus = (task: TaskResponse) => {
    if (!task.dueDate) return { type: 'info', label: '无截止时间' }

    const now = new Date()
    const dueDate = new Date(task.dueDate)

    if (dueDate < now) {
      return { type: 'danger', label: '已逾期' }
    } else if (dueDate.getTime() - now.getTime() < 24 * 60 * 60 * 1000) {
      return { type: 'warning', label: '即将到期' }
    } else {
      return { type: 'success', label: '进行中' }
    }
  }

  // 加载任务及资源
  const loadTasks = async () => {
    try {
      //tasks.value = await getTasksByCourse(props.courseId)
      // 修改：传递教师ID进行权限验证
      const teacherId = Number(auth.user!.teacherId)
      tasks.value = await getTasksByCourse(props.courseId, teacherId)

      total.value = filteredTasks.value.length

      // 加载每个任务的资源
      for (const task of tasks.value) {
        try {
          const resList = await getResourcesByTask(task.id)
          taskResources.value[task.id] = resList
        } catch (resourceError) {
          taskResources.value[task.id] = []
        }
      }
    } catch (e) {
      //ElMessage.error('加载任务失败')
      if (e.response?.status === 403) {
        ElMessage.error('无权访问此课程的任务')
      } else {
        ElMessage.error('加载任务失败')
      }
    }
  }

  // 显示试卷详情
  const showQuizDetail = (quizDetail: QuizDetailResponse) => {
    currentQuizDetail.value = quizDetail
    quizDetailVisible.value = true
  }

  // 关闭试卷详情弹窗
  const closeQuizDetail = () => {
    quizDetailVisible.value = false
    currentQuizDetail.value = null
  }

  // 新增：试卷选择相关函数
  // 加载可用试卷列表
  const loadAvailableQuizzes = async () => {
    if (!auth.user?.id) return

    quizLoading.value = true
    try {
      const teacherId = Number(auth.user.teacherId)
      availableQuizzes.value = await getAvailableQuizzes(teacherId, props.courseId)
      console.log('[加载可用试卷]', availableQuizzes.value)
    } catch (error) {
      console.error('[加载试卷失败]', error)
      ElMessage.error('加载试卷列表失败')
    } finally {
      quizLoading.value = false
    }
  }

  // 预览试卷
  const previewQuiz = async (quizId: number) => {
    try {
      const teacherId = Number(auth.user!.teacherId)
      const quizDetail = await getQuizDetail(quizId, teacherId)
      currentQuizPreview.value = quizDetail
      quizPreviewVisible.value = true

      // 调试打印每道题解析结果
      quizDetail.questions.forEach((question) => {
        console.log('解析题目内容:', parseQuestionBody(question.body))
      })
    } catch (error) {
      console.error('[预览试卷失败]', error)
      ElMessage.error('预览试卷失败')
    }
  }

  // 关闭试卷预览
  const closeQuizPreview = () => {
    quizPreviewVisible.value = false
    currentQuizPreview.value = null
  }

  // 监听任务类型变化，如果是QUIZ类型则加载试卷列表
  watch(
    () => newTask.value.type,
    (newType) => {
      if (newType === TaskType.QUIZ) {
        loadAvailableQuizzes()
      } else {
        selectedQuizId.value = null
        availableQuizzes.value = []
      }
    },
  )

  onMounted(() => {
    loadTasks()
  })
</script>

<template>
  <div class="task-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">
          <el-icon><Document /></el-icon>
          任务管理
        </h2>
        <div class="header-actions">
          <!-- 课程选择下拉菜单 -->
          <el-select v-model="selectedCourseId"
                     placeholder="选择课程"
                     @change="handleCourseChange"
                     style="width: 250px; margin-right: 12px"
                     class="course-select">
            <el-option v-for="course in props.courses"
                       :key="course.courseId"
                       :label="`${course.name} (${course.code})`"
                       :value="course.courseId">
              <div class="course-option">
                <div class="course-name">{{ course.name }}</div>
                <div class="course-code">{{ course.code }}</div>
              </div>
            </el-option>
          </el-select>

          <el-button type="primary" @click="openCreateDialog" class="create-btn">
            <el-icon><Plus /></el-icon>
            创建任务
          </el-button>
          <el-button @click="loadTasks" :loading="loading" class="refresh-btn">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </div>

    <!-- 查询条件 -->
    <div class="search-panel">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="课程ID">
          <el-input v-model.number="searchForm.courseId"
                    placeholder="输入课程ID"
                    style="width: 120px" />
        </el-form-item>

        <el-form-item label="任务类型">
          <el-select v-model="searchForm.taskType" placeholder="选择类型" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="作业" :value="TaskType.HOMEWORK" />
            <el-option label="测验" :value="TaskType.QUIZ" />
            <el-option label="报告" :value="TaskType.REPORT" />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword"
                    placeholder="搜索标题或描述"
                    style="width: 200px" />
        </el-form-item>

        <el-form-item label="创建时间">
          <el-date-picker v-model="searchForm.dateRange"
                          type="daterange"
                          range-separator="至"
                          start-placeholder="开始日期"
                          end-placeholder="结束日期"
                          style="width: 240px" />
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="upcoming" />
            <el-option label="已逾期" value="overdue" />
            <el-option label="无截止时间" value="no-due" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="searchTasks">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 任务统计 -->
    <div class="task-stats">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">{{ filteredTasks.length }}</div>
            <div class="stat-label">筛选结果</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">
              {{ filteredTasks.filter((t) => t.type === TaskType.HOMEWORK).length }}
            </div>
            <div class="stat-label">作业任务</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">
              {{ filteredTasks.filter((t) => t.type === TaskType.QUIZ).length }}
            </div>
            <div class="stat-label">测验任务</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-number">
              {{ filteredTasks.filter((t) => t.type === TaskType.REPORT).length }}
            </div>
            <div class="stat-label">报告任务</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 任务列表 -->
    <div class="task-list">
      <el-row :gutter="20">
        <el-col v-for="task in paginatedTasks" :key="task.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="task-card" shadow="hover">
            <div class="task-header">
              <div class="task-title">{{ task.title }}</div>
              <div class="task-tags">
                <el-tag :type="getTaskTypeTag(task.type).type" size="small" class="task-type-tag">
                  {{ getTaskTypeTag(task.type).label }}
                </el-tag>
                <el-tag :type="getTaskStatus(task).type" size="small" class="task-status-tag">
                  {{ getTaskStatus(task).label }}
                </el-tag>
              </div>
            </div>

            <div class="task-description" v-if="task.description">
              {{ task.description }}
            </div>

            <div class="task-meta">
              <div class="course-info">
                <el-icon><School /></el-icon>
                课程ID: {{ task.courseId }}
              </div>
              <div class="due-date">
                <el-icon><Clock /></el-icon>
                {{ formatDateTime(task.dueDate) }}
              </div>
              <div class="created-date">创建于 {{ formatDateTime(task.createdAt) }}</div>
            </div>

            <!-- 资源展示 -->
            <div v-if="taskResources[task.id]?.length" class="task-resources">
              <el-divider content-position="left">
                <el-icon><Folder /></el-icon>
                资源文件
              </el-divider>
              <div class="resource-list">
                <div v-for="res in taskResources[task.id]" :key="res.id" class="resource-item">
                  <el-icon><Document /></el-icon>
                  <a :href="res.filePath" target="_blank" class="resource-link">
                    {{ res.name }}
                  </a>
                  <span class="resource-type">{{ res.fileType || '未知类型' }}</span>
                </div>
              </div>
            </div>

            <div class="task-actions">
              <!-- 新增：如果是QUIZ类型且有试卷详情，显示查看试卷按钮 -->
              <el-button v-if="task.type === TaskType.QUIZ && task.quizDetail"
                         type="primary"
                         size="small"
                         @click="showQuizDetail(task.quizDetail)"
                         class="view-quiz-btn">
                <el-icon><QuestionFilled /></el-icon>
                查看试卷
              </el-button>

              <el-button type="primary" size="small" @click="goToSubmissions(task.id)">
                批改
              </el-button>

              <el-button type="danger"
                         size="small"
                         @click="onDeleteTask(task.id)"
                         class="delete-btn">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="filteredTasks.length > pageSize">
      <el-pagination v-model:current-page="currentPage"
                     :page-size="pageSize"
                     :total="filteredTasks.length"
                     layout="total, prev, pager, next, jumper"
                     @current-change="handlePageChange"
                     background />
    </div>

    <!-- 创建任务弹窗 -->
    <el-dialog v-model="createDialogVisible"
               title="创建新任务"
               width="600px"
               :close-on-click-modal="false">
      <el-form :model="newTask" label-width="80px" class="create-form">
        <el-form-item label="课程ID" required>
          <el-input v-model.number="newTask.courseId" placeholder="输入课程ID" disabled />
        </el-form-item>

        <el-form-item label="标题" required>
          <el-input v-model="newTask.title"
                    placeholder="请输入任务标题"
                    maxlength="100"
                    show-word-limit />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="newTask.description"
                    type="textarea"
                    placeholder="任务描述（可选）"
                    :rows="3"
                    maxlength="500"
                    show-word-limit />
        </el-form-item>

        <el-form-item label="类型" required>
          <el-select v-model="newTask.type" placeholder="选择任务类型" style="width: 100%">
            <el-option label="作业" :value="TaskType.HOMEWORK">
              <el-icon><EditPen /></el-icon>
              作业
            </el-option>
            <el-option label="测验" :value="TaskType.QUIZ">
              <el-icon><QuestionFilled /></el-icon>
              测验
            </el-option>
            <el-option label="报告" :value="TaskType.REPORT">
              <el-icon><Document /></el-icon>
              报告
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 新增：试卷选择 -->
        <el-form-item v-if="newTask.type === TaskType.QUIZ" label="选择试卷" required>
          <div class="quiz-selection">
            <el-select v-model="selectedQuizId"
                       placeholder="请选择试卷"
                       style="width: 100%"
                       :loading="quizLoading"
                       clearable
                       class="quiz-select">
              <el-option v-for="quiz in availableQuizzes"
                         :key="quiz.id"
                         :label="`${quiz.title} (${quiz.questionCount}题, ${quiz.totalPoints}分)`"
                         :value="quiz.id"
                         class="quiz-option-item">
                <div class="quiz-option-content">
                  <div class="quiz-option-header">
                    <div class="quiz-title">{{ quiz.title }}</div>
                    <div class="quiz-creator">{{ quiz.creatorName }}</div>
                  </div>
                  <div class="quiz-option-footer">
                    <el-tag size="small" type="info" class="quiz-tag">
                      <el-icon><QuestionFilled /></el-icon>
                      {{ quiz.questionCount }}题
                    </el-tag>
                    <el-tag size="small" type="success" class="quiz-tag">
                      <el-icon><Clock /></el-icon>
                      {{ quiz.totalPoints }}分
                    </el-tag>
                  </div>
                </div>
              </el-option>
            </el-select>

            <!-- 预览按钮 -->
            <el-button v-if="selectedQuizId"
                       type="primary"
                       size="small"
                       @click="previewQuiz(selectedQuizId)"
                       class="preview-btn">
              <el-icon><View /></el-icon>
              预览试卷
            </el-button>

            <!-- 无试卷提示 -->
            <div v-if="!quizLoading && availableQuizzes.length === 0" class="no-quiz-tip">
              <el-empty description="该课程暂无可用试卷" :image-size="60">
                <el-button type="primary" size="small"> 去创建试卷 </el-button>
              </el-empty>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="截止时间">
          <el-date-picker v-model="newTask.dueDate"
                          type="datetime"
                          placeholder="选择截止时间"
                          value-format="YYYY-MM-DDTHH:mm:ss"
                          style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="newTask.type === TaskType.REPORT" label="上传资源" class="upload-field">
          <el-upload :before-upload="handleBeforeUpload"
                     :auto-upload="false"
                     v-model:file-list="fileList"
                     multiple
                     :on-remove="handleFileRemove"
                     class="upload-area">
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">已选择 {{ fileList.length }} 个文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeCreateDialog">取消</el-button>
          <el-button type="primary" @click="onCreateTask" :loading="loading"> 创建任务 </el-button>
        </div>
      </template>
    </el-dialog>
  </div>

  <!-- 试卷详情弹窗 -->
  <el-dialog v-model="quizDetailVisible"
             title="试卷详情"
             width="800px"
             :close-on-click-modal="false">
    <div v-if="currentQuizDetail" class="quiz-detail">
      <div class="quiz-header">
        <h3 class="quiz-title">{{ currentQuizDetail.title }}</h3>
        <div class="quiz-meta">
          <el-tag type="info">创建者: {{ currentQuizDetail.creatorName }}</el-tag>
          <el-tag type="success">总分: {{ currentQuizDetail.totalPoints }}</el-tag>
          <el-tag type="warning">题目数量: {{ currentQuizDetail.questions.length }}</el-tag>
        </div>
      </div>

      <el-divider />

      <div v-if="currentQuizDetail.questions.length > 0" class="questions-list">
        <div v-for="(question, index) in currentQuizDetail.questions"
             :key="question.questionId"
             class="question-item">
          <div class="question-header">
            <h4 class="question-title">
              第{{ index + 1 }}题
              <el-tag size="small" type="primary">{{ getQuestionTypeName(question.type) }}</el-tag>
              <el-tag size="small" type="warning">
                {{
                getDifficultyName(question.difficulty)
                }}
              </el-tag>
            </h4>
          </div>

          <div v-if="parseQuestionBody(question.body)" class="question-content">
            <div class="question-text">{{ parseQuestionBody(question.body).stem }}</div>

            <!-- 选项（单选/多选） -->
            <div v-if="question.type === 'SINGLE_CHOICE' || question.type === 'MULTI_CHOICE'"
                 class="question-options">
              <div v-for="option in parseQuestionBody(question.body).options"
                   :key="option.label"
                   class="option-item">
                {{ option.label }}. {{ option.text }}
              </div>
            </div>

            <!-- 填空题答案 -->
            <div v-if="question.type === 'FILL_IN_BLANK'" class="question-answer">
              <strong>答案:</strong> {{ parseQuestionBody(question.body).answer }}
            </div>

            <!-- 简答题答案 -->
            <div v-if="question.type === 'SHORT_ANSWER'" class="question-answer">
              <strong>参考答案:</strong> {{ parseQuestionBody(question.body).answer }}
            </div>

            <!-- 解析 -->
            <div class="question-explanation" v-if="parseQuestionBody(question.body).analysis">
              <strong>解析:</strong> {{ parseQuestionBody(question.body).analysis }}
            </div>
          </div>
        </div>
      </div>

      <div v-else class="no-questions">
        <el-empty description="暂无题目详情" />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeQuizDetail">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 试卷预览弹窗 -->
  <el-dialog v-model="quizPreviewVisible"
             title="试卷预览"
             width="800px"
             :close-on-click-modal="false">
    <div v-if="currentQuizPreview" class="quiz-detail">
      <div class="quiz-header">
        <h3 class="quiz-title">{{ currentQuizPreview.title }}</h3>
        <div class="quiz-meta">
          <el-tag type="info">创建者: {{ currentQuizPreview.creatorName }}</el-tag>
          <el-tag type="success">总分: {{ currentQuizPreview.totalPoints }}</el-tag>
          <el-tag type="warning">题目数量: {{ currentQuizPreview.questions.length }}</el-tag>
        </div>
      </div>

      <el-divider />

      <div v-if="currentQuizPreview.questions.length > 0" class="questions-list">
        <div v-for="(question, index) in currentQuizPreview.questions"
             :key="question.questionId"
             class="question-item">
          <div class="question-header">
            <h4 class="question-title">
              第{{ index + 1 }}题
              <el-tag size="small" type="primary">{{ getQuestionTypeName(question.type) }}</el-tag>
              <el-tag size="small" type="warning">
                {{
                getDifficultyName(question.difficulty)
                }}
              </el-tag>
            </h4>
          </div>

          <div v-if="parseQuestionBody(question.body)" class="question-content">
            <div class="question-text">{{ parseQuestionBody(question.body).stem }}</div>

            <!-- 根据题目类型显示不同内容 -->
            <div v-if="question.type === 'SINGLE_CHOICE' || question.type === 'MULTI_CHOICE'"
                 class="question-options">
              <!-- <div
                v-for="(option, optIndex) in parseQuestionBody(question.body).options"
                :key="optIndex"
                class="option-item"
              >
                {{ String.fromCharCode(65 + optIndex) }}. {{ option }}
              </div> -->

              <div v-for="(option, optIndex) in parseQuestionBody(question.body).options"
                   :key="optIndex"
                   class="option-item">
                {{ option.label }}. {{ option.text }}
              </div>
            </div>

            <div v-if="question.type === 'FILL_IN_BLANK'" class="question-answer">
              <strong>答案:</strong> {{ parseQuestionBody(question.body).answer }}
            </div>

            <div v-if="question.type === 'SHORT_ANSWER'" class="question-answer">
              <strong>参考答案:</strong> {{ parseQuestionBody(question.body).answer }}
            </div>

            <div class="question-explanation" v-if="parseQuestionBody(question.body).analysis">
              <strong>解析:</strong> {{ parseQuestionBody(question.body).analysis }}
            </div>
          </div>
        </div>
      </div>

      <div v-else class="no-questions">
        <el-empty description="暂无题目详情" />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeQuizPreview">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
  .task-management {
    background: #f5f7fa;
    min-height: 100vh;
    padding: 20px;
  }

  .page-header {
    background: white;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .page-title {
    margin: 0;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 24px;
    font-weight: 600;
  }

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .course-select {
    .course-option

  {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .course-name {
    font-weight: 500;
    color: #303133;
  }

  .course-code {
    font-size: 12px;
    color: #909399;
  }

  }

  .create-btn {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .refresh-btn {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .search-panel {
    background: white;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .search-form {
    margin: 0;
  }

  .task-stats {
    margin-bottom: 20px;
  }

  .stat-card {
    background: white;
    border-radius: 8px;
    padding: 20px;
    text-align: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s;
  }

    .stat-card:hover {
      transform: translateY(-2px);
    }

  .stat-number {
    font-size: 32px;
    font-weight: bold;
    color: #409eff;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #606266;
    font-size: 14px;
  }

  .task-list {
    margin-bottom: 20px;
  }

  .task-card {
    margin-bottom: 20px;
    transition: all 0.3s;
    border: none;
  }

    .task-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
    }

  .task-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .task-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    flex: 1;
    margin-right: 8px;
    line-height: 1.4;
  }

  .task-tags {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
  }

  .task-type-tag,
  .task-status-tag {
    font-size: 11px;
  }

  .task-description {
    color: #606266;
    font-size: 14px;
    line-height: 1.5;
    margin-bottom: 12px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .task-meta {
    margin-bottom: 16px;
  }

  .course-info {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #409eff;
    font-size: 13px;
    margin-bottom: 4px;
  }

  .due-date {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #e6a23c;
    font-size: 13px;
    margin-bottom: 4px;
  }

  .created-date {
    color: #909399;
    font-size: 12px;
  }

  .task-resources {
    margin-bottom: 16px;
  }

  .resource-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .resource-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px;
    background: #f8f9fa;
    border-radius: 4px;
    font-size: 13px;
  }

  .resource-link {
    color: #409eff;
    text-decoration: none;
    flex: 1;
  }

    .resource-link:hover {
      text-decoration: underline;
    }

  .resource-type {
    color: #909399;
    font-size: 12px;
  }

  .task-actions {
    display: flex;
    justify-content: flex-end;
  }

  .delete-btn {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  .create-form {
    padding: 20px 0;
  }

  .upload-field {
    align-items: flex-start;
  }

  .upload-area {
    width: 100%;
  }

  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .task-management {
      padding: 10px;
    }

    .header-content {
      flex-direction: column;
      gap: 16px;
      align-items: stretch;
    }

    .create-btn {
      width: 100%;
    }

    .stat-card {
      margin-bottom: 16px;
    }

    .search-form {
      flex-direction: column;
    }

      .search-form .el-form-item {
        margin-right: 0;
        margin-bottom: 16px;
      }
  }

  /* 试卷详情弹窗样式 */
  .quiz-detail {
    max-height: 600px;
    overflow-y: auto;
  }

  .quiz-header {
    margin-bottom: 20px;
  }

  .quiz-title {
    margin: 0 0 12px 0;
    color: #303133;
    font-size: 20px;
    font-weight: 600;
  }

  .quiz-meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .questions-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .question-item {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    padding: 16px;
    background: #fafafa;
  }

  .question-header {
    margin-bottom: 12px;
  }

  .question-title {
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 16px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .question-content {
    color: #606266;
  }

  .question-text {
    margin-bottom: 12px;
    font-size: 14px;
    line-height: 1.6;
  }

  .question-options {
    margin-bottom: 12px;
  }

  .option-item {
    padding: 4px 0;
    font-size: 14px;
  }

  .question-answer {
    margin-bottom: 8px;
    padding: 8px;
    background: #f0f9ff;
    border-radius: 4px;
    font-size: 14px;
  }

  .question-explanation {
    margin-top: 8px;
    padding: 8px;
    background: #fef7e0;
    border-radius: 4px;
    font-size: 14px;
    color: #e6a23c;
  }

  .view-quiz-btn {
    margin-right: 8px;
  }

  .quiz-selection {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .quiz-select {
    .el-select-dropdown__item

  {
    padding: 16px 20px;
    min-height: 80px;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.2s ease;
  }

  .el-select-dropdown__item:hover {
    background-color: #f5f7fa;
    transform: translateX(4px);
  }

  .el-select-dropdown__item.selected {
    background-color: #ecf5ff;
    color: #409eff;
    border-left: 3px solid #409eff;
  }

  .el-select-dropdown__item:last-child {
    border-bottom: none;
  }

  }

  .quiz-option-item {
    height: auto !important;
    padding: 16px 20px !important;
    cursor: pointer;
    transition: all 0.2s ease;
  }

    .quiz-option-item:hover {
      background-color: #f5f7fa;
    }

  .quiz-option-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }

  .quiz-option-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
  }

  .quiz-title {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
    line-height: 1.4;
    flex: 1;
    margin: 0;
  }

  .quiz-creator {
    color: #909399;
    font-size: 12px;
    white-space: nowrap;
    font-style: italic;
  }

  .quiz-option-footer {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .quiz-tag {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    font-weight: 500;
    transition: all 0.2s ease;
  }

    .quiz-tag:hover {
      transform: scale(1.05);
    }

  .preview-btn {
    align-self: flex-start;
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    border-radius: 6px;
    transition: all 0.3s;
    font-weight: 500;
  }

    .preview-btn:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(64, 158, 255, 0.2);
    }

  .no-quiz-tip {
    margin-top: 16px;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 8px;
    border: 1px dashed #d9d9d9;
    text-align: center;
    transition: all 0.3s ease;
  }

    .no-quiz-tip:hover {
      border-color: #409eff;
      background: #f0f9ff;
    }
</style>
