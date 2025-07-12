<template>
  <div class="grade-analysis-detail">
    <div class="student-info">
      <h3>{{ analysis.studentName }} - {{ analysis.courseName }}</h3>
      <div class="grade-overview">
        <div class="grade-item">
          <span class="label">总成绩:</span>
          <span class="value">{{ parseFloat(analysis.currentGrade).toFixed(1) }}/{{ parseFloat(analysis.maxGrade).toFixed(1) }}</span>
        </div>
        <div class="grade-item">
          <span class="label">百分比:</span>
          <span class="value">{{ analysis.gradePercentage.toFixed(1) }}%</span>
        </div>
        <div class="grade-item">
          <span class="label">等级:</span>
          <span class="value grade-badge grade-{{ getGradeClass(analysis.gradeLevel) }}">
            {{ analysis.gradeLevel }}
          </span>
        </div>
        <div class="grade-item">
          <span class="label">排名:</span>
          <span class="value">{{ analysis.classRank }}/{{ analysis.totalStudents }}</span>
        </div>
      </div>
    </div>

    <div class="analysis-sections">
      <!-- 学习建议 -->
      <div class="section">
        <h4>学习建议</h4>
        <div class="suggestions">
          <div v-for="(suggestion, index) in analysis.learningSuggestions"
               :key="index"
               class="suggestion-item">
            {{ suggestion }}
          </div>
        </div>
      </div>

      <!-- 任务类型成绩 -->
      <div class="section">
        <h4>各任务类型成绩</h4>
        <div class="task-type-grades">
          <div v-for="(grade, type) in analysis.taskTypeGrades" :key="type" class="task-type-item">
            <div class="task-type-header">
              <span class="task-type-name">{{ getTaskTypeName(type) }}</span>
              <span class="task-count">({{ grade.taskCount }}个任务)</span>
            </div>
            <div class="task-type-stats">
              <div class="stat">
                <span class="label">平均分:</span>
                <span class="value">{{ parseFloat(grade.averageScore).toFixed(1) }}/{{ parseFloat(grade.maxScore).toFixed(1) }}</span>
              </div>
              <div class="stat">
                <span class="label">完成率:</span>
                <span class="value">{{ grade.completionRate.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="section">
        <h4>统计信息</h4>
        <div class="statistics">
          <div class="stat-item">
            <span class="label">已完成任务:</span>
            <span class="value">{{ analysis.statistics.completedTasks }}/{{ analysis.statistics.totalTasks }}</span>
          </div>
          <div class="stat-item">
            <span class="label">完成率:</span>
            <span class="value">{{ analysis.statistics.completionRate.toFixed(1) }}%</span>
          </div>
          <div class="stat-item">
            <span class="label">平均分:</span>
            <span class="value">{{ parseFloat(analysis.statistics.averageGrade).toFixed(1) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">最高分:</span>
            <span class="value">{{ parseFloat(analysis.statistics.highestGrade).toFixed(1) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">最低分:</span>
            <span class="value">{{ parseFloat(analysis.statistics.lowestGrade).toFixed(1) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { GradeAnalysisResponse } from '@/types/grade'

interface Props {
  analysis: GradeAnalysisResponse
}

defineProps<Props>()

const getGradeClass = (gradeLevel: string) => {
  const gradeMap: Record<string, string> = {
    优秀: 'excellent',
    良好: 'good',
    中等: 'fair',
    及格: 'pass',
    不及格: 'fail',
  }
  return gradeMap[gradeLevel] || 'default'
}

const getTaskTypeName = (taskType: string) => {
  const typeMap: Record<string, string> = {
    HOMEWORK: '作业',
    QUIZ: '测验',
    REPORT: '报告',
    PROJECT: '项目',
  }
  return typeMap[taskType] || taskType
}
</script>

<style scoped>
  .grade-analysis-detail {
    padding: 20px;
  }

  .student-info {
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
  }

    .student-info h3 {
      margin: 0 0 15px 0;
      color: #333;
      font-size: 18px;
    }

  .grade-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 15px;
  }

  .grade-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px;
    background: #f8f9fa;
    border-radius: 6px;
  }

    .grade-item .label {
      font-weight: 500;
      color: #666;
    }

    .grade-item .value {
      font-weight: 600;
      color: #333;
    }

  .grade-badge {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
  }

  .grade-excellent {
    background: #d4edda;
    color: #155724;
  }

  .grade-good {
    background: #d1ecf1;
    color: #0c5460;
  }

  .grade-fair {
    background: #fff3cd;
    color: #856404;
  }

  .grade-pass {
    background: #f8d7da;
    color: #721c24;
  }

  .grade-fail {
    background: #f8d7da;
    color: #721c24;
  }

  .grade-default {
    background: #e2e3e5;
    color: #383d41;
  }

  .analysis-sections {
    display: flex;
    flex-direction: column;
    gap: 25px;
  }

  .section {
    background: white;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

    .section h4 {
      margin: 0 0 15px 0;
      color: #333;
      font-size: 16px;
      border-bottom: 2px solid #409eff;
      padding-bottom: 8px;
    }

  .suggestions {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .suggestion-item {
    padding: 10px;
    background: #f8f9fa;
    border-radius: 6px;
    border-left: 4px solid #409eff;
    color: #333;
  }

  .task-type-grades {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }

  .task-type-item {
    padding: 15px;
    background: #f8f9fa;
    border-radius: 8px;
    border: 1px solid #e9ecef;
  }

  .task-type-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }

  .task-type-name {
    font-weight: 600;
    color: #333;
  }

  .task-count {
    font-size: 12px;
    color: #666;
  }

  .task-type-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 10px;
  }

  .stat {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

    .stat .label {
      font-size: 14px;
      color: #666;
    }

    .stat .value {
      font-weight: 500;
      color: #333;
    }

  .statistics {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 15px;
  }

  .stat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px;
    background: #f8f9fa;
    border-radius: 6px;
  }

    .stat-item .label {
      font-size: 14px;
      color: #666;
    }

    .stat-item .value {
      font-weight: 600;
      color: #333;
    }
</style>
