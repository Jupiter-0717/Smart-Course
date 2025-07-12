<template>
  <div class="course-graph-cards">
    <el-row :gutter="20">
      <el-col
        v-for="course in courses"
        :key="course.courseId"
        :span="8"
        class="course-card-col"
      >
        <el-card class="course-card">
          <div class="course-header">
            <h3>{{ course.name }}</h3>
            <el-button
              type="primary"
              plain
              @click="viewKnowledgeGraph(course.courseId)"
            >
              查看图谱
            </el-button>
          </div>
          <div class="course-info">
            <div class="info-item">
              <el-icon><Collection /></el-icon>
              <span>{{ courseStats[course.courseId]?.pointCount ?? 0 }}个知识点</span>
            </div>
            <div class="info-item">
              <el-icon><Folder /></el-icon>
              <span>{{ courseStats[course.courseId]?.resourceCount ?? 0 }}个资源</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 加载状态 -->
    <div v-if="loadingStats" class="stats-loading">
      <el-progress :percentage="statsProgress" :show-text="false" />
      <p>加载统计信息中... {{ statsProgress }}%</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useCourseStore } from '@/stores/course';
import { useAuthStore } from '@/stores/auth';
import StatsService from '@/services/statsService';
import { Collection, Folder } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const courseStore = useCourseStore();
const authStore = useAuthStore();

// 存储课程统计信息
const courseStats = ref<Record<number, any>>({});

// 加载状态
const loadingStats = ref(false);
const statsProgress = ref(0);
const statsError = ref('');

// 教师ID
const teacherId = computed(() => authStore.user?.teacherId || 0);

// 课程列表
const courses = computed(() => courseStore.courses);

// 查看知识图谱
const viewKnowledgeGraph = (courseId: number) => {
  router.push({
    name: 'TeacherCourseKnowledgeGraph',
    params: {
      teacherId: teacherId.value,
      courseId
    }
  });
};

// 加载教师课程和统计信息
onMounted(async () => {
  if (teacherId.value) {
    try {
      // 1. 加载教师课程
      await courseStore.loadTeacherCourses(teacherId.value);

      // 2. 获取所有课程ID
      const courseIds = courses.value.map(c => c.courseId);

      if (courseIds.length === 0) {
        return;
      }

      // 3. 加载统计信息
      loadingStats.value = true;
      statsError.value = '';

      try {
        // 3.1 尝试批量获取统计信息
        const batchStats = await StatsService.getBatchCourseStats(courseIds);
        courseStats.value = batchStats;
      } catch (batchError) {
        console.error('批量获取统计信息失败:', batchError);
        statsError.value = '批量获取统计信息失败，改为逐个获取';
        ElMessage.warning('批量获取统计信息失败，改为逐个获取');

        // 3.2 回退到逐个获取
        for (let i = 0; i < courseIds.length; i++) {
          const courseId = courseIds[i];
          try {
            const stats = await StatsService.getCourseStats(courseId);
            courseStats.value[courseId] = stats;
          } catch (singleError) {
            console.error(`获取课程 ${courseId} 统计信息失败:`, singleError);
            courseStats.value[courseId] = { pointCount: 0, resourceCount: 0 };
          }

          // 更新进度
          statsProgress.value = Math.round((i + 1) / courseIds.length * 100);
        }
      }
    } catch (error) {
      console.error('加载课程数据失败:', error);
      statsError.value = '加载课程数据失败';
      ElMessage.error('加载课程数据失败');
    } finally {
      loadingStats.value = false;
    }
  }
});
</script>

<style scoped lang="scss">
.course-graph-cards {
  padding: 20px;
  position: relative;

  .course-card-col {
    margin-bottom: 20px;
  }

  .course-card {
    height: 100%;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transition: transform 0.3s, box-shadow 0.3s;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
    }

    .course-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;
      padding-bottom: 12px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: bold;
        max-width: 65%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .el-button {
        padding: 8px 12px;
        height: auto;
      }
    }

    .course-info {
      display: flex;
      justify-content: space-between;
      padding: 0 5px;

      .info-item {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: #606266;
        padding: 5px 10px;
        background: #f8f9fa;
        border-radius: 4px;
        transition: background-color 0.3s;

        &:hover {
          background-color: #e8f4ff;
        }

        .el-icon {
          margin-right: 8px;
          font-size: 16px;
          color: #409eff;
        }
      }
    }
  }

  .stats-loading {
    position: fixed;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(255, 255, 255, 0.9);
    padding: 10px 20px;
    border-radius: 4px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    z-index: 2000;
    text-align: center;

    p {
      margin: 5px 0 0;
      font-size: 14px;
      color: #606266;
    }

    .el-progress {
      width: 200px;
    }
  }

  .stats-error {
    margin-top: 15px;
  }
}
</style>
