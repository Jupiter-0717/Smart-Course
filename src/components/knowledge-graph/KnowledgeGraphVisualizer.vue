<template>
  <div class="graph-container" ref="graphContainer">
    <div v-if="loading" class="graph-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>知识图谱加载中...</span>
    </div>

    <div v-else>
      <!-- 画布区域 -->
      <div class="graph-canvas" ref="graphCanvas">
        <!-- 连接线使用SVG渲染 -->
        <svg class="edges-layer">
          <line
            v-for="(edge, index) in edges"
            :key="index"
            :x1="getNodePosition(edge.source).x"
            :y1="getNodePosition(edge.source).y"
            :x2="getNodePosition(edge.target).x"
            :y2="getNodePosition(edge.target).y"
            stroke="#999"
            stroke-width="2"
          />
        </svg>

        <!-- 节点 -->
        <div
          v-for="node in nodes"
          :key="node.id"
          class="graph-node"
          :class="{ selected: selectedNode?.id === node.id }"
          :style="{
            left: `${node.positionX}px`,
            top: `${node.positionY}px`,
          }"
          @click="selectNode(node)"
          @mousedown="startDrag(node, $event)"
        >
          <div class="node-header">
            <el-tag size="small" type="primary">知识点</el-tag>
          </div>
          <div class="node-title">{{ node.title }}</div>
          <div v-if="node.resources?.length" class="resources-count">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ node.resources.length }}个资源</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { FolderOpened, Loading } from '@element-plus/icons-vue'

interface GraphNode {
  id: string
  title: string
  description: string
  type: string
  courseId: number
  positionX: number
  positionY: number
  resources: any[] | null
}

interface GraphEdge {
  source: string
  target: string
  relationship: string
}

const props = defineProps({
  nodes: {
    type: Array as () => GraphNode[],
    required: true,
  },
  edges: {
    type: Array as () => GraphEdge[],
    required: true,
  },
  loading: Boolean,
})

const emit = defineEmits(['node-click', 'node-drag'])

const graphContainer = ref<HTMLElement | null>(null)
const selectedNode = ref<GraphNode | null>(null)

// 拖拽功能
let isDragging = false
let dragNode: GraphNode | null = null

function startDrag(node: GraphNode, event: MouseEvent) {
  isDragging = true
  dragNode = node
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  event.preventDefault()
}

function onDrag(event: MouseEvent) {
  if (isDragging && dragNode && graphContainer.value) {
    const rect = graphContainer.value.getBoundingClientRect()
    dragNode.positionX = event.clientX - rect.left
    dragNode.positionY = event.clientY - rect.top
    emit('node-drag', dragNode, {
      positionX: dragNode.positionX,
      positionY: dragNode.positionY,
    })
  }
}

function stopDrag() {
  isDragging = false
  dragNode = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

function selectNode(node: GraphNode) {
  selectedNode.value = node
  emit('node-click', node)
}

// 获取节点位置
function getNodePosition(nodeId: string) {
  const node = props.nodes.find((n) => n.id === nodeId)
  return node
    ? {
        x: node.positionX + 90, // 节点中心点
        y: node.positionY + 50,
      }
    : { x: 0, y: 0 }
}
</script>

<style scoped>
.graph-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #f9fafc;
}

.graph-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.graph-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.edges-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.graph-node {
  position: absolute;
  width: 180px;
  min-height: 100px;
  padding: 15px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translate(-50%, -50%);
  cursor: grab;
  transition: all 0.3s;
  z-index: 10;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
    z-index: 20;
  }

  .node-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .node-title {
    font-weight: 600;
    font-size: 16px;
    margin-bottom: 10px;
  }

  .resources-count {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #909399;
  }
}
</style>
