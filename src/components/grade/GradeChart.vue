<template>
  <div class="grade-chart">
    <div class="chart-header">
      <h3>{{ title }}</h3>
    </div>
    <div class="chart-container">
      <div ref="chartRef" class="chart"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'

interface Props {
  title: string
  data: any[]
  type: 'bar' | 'line' | 'pie'
  showControls?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showControls: false,
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance || !props.data.length) return

  let option: any = {}

  if (props.type === 'bar') {
    // 柱状图
    option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: props.data.map((item) => item.taskName),
        axisLabel: {
          rotate: 45,
          fontSize: 12
        }
      },
      yAxis: {
        type: 'value',
        name: '学生数量',
        nameTextStyle: {
          fontSize: 12
        }
      },
      series: [
        {
          data: props.data.map((item) => item.value !== undefined ? item.value : item.percentage),
          type: 'bar',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' }
            ])
          },
          barWidth: '60%'
        },
      ],
    }
  } else if (props.type === 'pie') {
    // 饼图
    option = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c}% ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        fontSize: 12
      },
      series: [
        {
          name: '成绩分布',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          data: props.data.map((item) => ({
            name: item.taskName,
            value: item.value !== undefined ? item.value : item.percentage,
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        },
      ],
    }
  } else if (props.type === 'line') {
    // 折线图
    option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985'
          }
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: props.data.map((item) => {
          // 如果是日期格式的字符串，则格式化为日期
          if (item.date && item.date.includes('-') || item.date.includes('/')) {
            return new Date(item.date).toLocaleDateString()
          }
          // 否则直接使用原始值（如"第1名"）
          return item.date || item.taskTitle || item.taskName
        }),
        axisLabel: {
          fontSize: 12
        }
      },
      yAxis: {
        type: 'value',
        name: '成绩百分比',
        nameTextStyle: {
          fontSize: 12
        }
      },
      series: [
        {
          data: props.data.map((item) => parseFloat(item.score) || 0),
          type: 'line',
          smooth: true,
          lineStyle: {
            color: '#409eff',
            width: 3
          },
          itemStyle: {
            color: '#409eff'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ])
          }
        },
      ],
    }
  }

  chartInstance.setOption(option)
}

// 监听窗口大小变化
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(() => props.data, () => {
  updateChart()
  // 数据变化后重新调整图表大小
  nextTick(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  })
}, { deep: true })
watch(() => props.type, () => {
  updateChart()
  // 类型变化后重新调整图表大小
  nextTick(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  })
})

onMounted(() => {
  nextTick(() => {
    initChart()
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  // 移除监听器
  window.removeEventListener('resize', handleResize)
  // 销毁图表实例
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
  .grade-chart {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  .chart-header {
    padding: 15px 20px 0 20px;
    flex-shrink: 0;
  }

    .chart-header h3 {
      margin: 0;
      color: #333;
      font-size: 16px;
      font-weight: 600;
    }

  .chart-container {
    flex: 1;
    padding: 0 20px 20px 20px;
    width: 100%;
    min-height: 0;
  }

  .chart {
    width: 100%;
    height: 100%;
    min-height: 250px;
  }
</style>
