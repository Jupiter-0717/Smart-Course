<template>
  <div class="graph-controls">
    <el-tooltip content="刷新知识图谱" placement="top">
      <el-button type="primary" plain @click="$emit('refresh')">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </el-tooltip>

    <el-tooltip content="AI智能生成知识图谱" placement="top">
      <el-button type="success" @click="$emit('generate-ai')">
        <el-icon><MagicStick /></el-icon>
        AI生成
      </el-button>
    </el-tooltip>

    <el-tooltip content="添加新知识点" placement="top">
      <el-button type="warning" @click="$emit('add-point')">
        <el-icon><CirclePlus /></el-icon>
        添加
      </el-button>
    </el-tooltip>

    <el-tooltip content="删除知识点" placement="top">
      <!-- 关键修改：移除 :disabled 属性 -->
      <el-button type="danger" @click="$emit('delete-point')">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { Refresh, MagicStick, CirclePlus, Delete } from '@element-plus/icons-vue'

// 移除不必要的 props
defineEmits(['refresh', 'generate-ai', 'add-point', 'delete-point'])
</script>

<style scoped>
.graph-controls {
  display: flex;
  gap: 10px;

  .el-button {
    display: flex;
    align-items: center;
    gap: 5px;
    position: relative;

    &.selected::after {
      content: '';
      position: absolute;
      top: -5px;
      right: -5px;
      width: 12px;
      height: 12px;
      background-color: #67c23a;
      border-radius: 50%;
    }
  }
}
</style>
