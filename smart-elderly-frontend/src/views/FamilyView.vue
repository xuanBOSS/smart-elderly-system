# FamilyView.vue - 家属监护端界面组件

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedName = ref('王大爷')
const chartRef = ref(null)
let chartInstance = null

const customerList = ['王大爷', '李奶奶']

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    color: ['#FF4D4F', '#1890FF', '#52C41A'],
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['收缩压', '舒张压', '心率'],
      textStyle: { color: '#303133' }
    },
    grid: { top: 20, left: 50, right: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['12/14', '12/15', '12/16', '12/17', '12/18', '12/19', '12/20'],
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399' },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F2F3F5' } }
    },
    series: [
      { name: '收缩压', type: 'line', data: [135, 132, 138, 130, 136, 140, 137], smooth: true },
      { name: '舒张压', type: 'line', data: [85, 80, 88, 82, 86, 90, 87], smooth: true },
      { name: '心率', type: 'line', data: [72, 75, 70, 74, 73, 76, 71], smooth: true }
    ]
  })
}

const resizeChart = () => {
  chartInstance?.resize()
}

const goLogin = () => router.push('/login')

const indicatorList = [
  { label: '137/87', unit: 'mmHg', name: '收缩压/舒张压', color: '#FF4D4F' },
  { label: '71', unit: '次/分', name: '心率', color: '#52C41A' },
  { label: '5.8', unit: 'mmol/L', name: '血糖', color: '#1890FF' }
]

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="family-page">
    <div class="family-header">
      <div class="family-title">家属监护端</div>
      <el-button type="text" class="exit-link" @click="goLogin">退出</el-button>
    </div>

    <div class="customer-tabs">
      <button
        v-for="name in customerList"
        :key="name"
        :class="['customer-tab', { active: selectedName === name }]"
        @click="selectedName = name"
      >
        {{ name }}
      </button>
    </div>

    <el-card class="trend-card" shadow="never">
      <div class="section-title">近7日健康趋势</div>
      <div ref="chartRef" class="trend-chart"></div>
    </el-card>

    <el-card class="summary-card" shadow="never">
      <div class="summary-row">
        <div class="summary-item" v-for="item in indicatorList" :key="item.name">
          <div class="summary-value" :style="{ color: item.color }">{{ item.label }}</div>
          <div class="summary-unit">{{ item.unit }}</div>
          <div class="summary-name">{{ item.name }}</div>
        </div>
      </div>
    </el-card>

    <el-card class="notice-card" shadow="never">
      <div class="section-title">消息通知</div>
      <div class="notice-item notice-urgent">
        <span class="notice-dot notice-dot-red"></span>
        <div class="notice-content">
          <div class="notice-text">王大爷触发了紧急求助</div>
          <div class="notice-time">14:30</div>
        </div>
      </div>
      <div class="notice-item">
        <span class="notice-dot notice-dot-blue"></span>
        <div class="notice-content">
          <div class="notice-text">门诊预约已确认（李医生 12/22）</div>
          <div class="notice-time">10:15</div>
        </div>
      </div>
      <div class="notice-item notice-last">
        <span class="notice-dot notice-dot-orange"></span>
        <div class="notice-content">
          <div class="notice-text">血压偏高预警提醒</div>
          <div class="notice-time">昨天</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.family-page {
  min-height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  background: #f7f8fa;
  padding-bottom: 24px;
}

.family-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.family-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.exit-link {
  color: #909399;
}

.customer-tabs {
  display: flex;
  gap: 12px;
  background: #fff;
  padding: 12px 20px 20px;
}

.customer-tab {
  border: none;
  border-radius: 20px;
  padding: 8px 20px;
  font-size: 15px;
  color: #4b5563;
  background: #f5f7fa;
  cursor: pointer;
}

.customer-tab.active {
  background: #1890ff;
  color: #fff;
}

.trend-card,
.summary-card,
.notice-card {
  background: #fff;
  border-radius: 12px;
  margin: 12px 16px;
  padding: 20px;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 16px;
}

.trend-chart {
  width: 100%;
  height: 220px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.summary-item {
  flex: 1;
  min-width: 0;
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
}

.summary-unit {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.summary-name {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.notice-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}

.notice-item.notice-last {
  border-bottom: none;
}

.notice-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 12px;
}

.notice-dot-red { background: #ff4d4f; }
.notice-dot-blue { background: #1890ff; }
.notice-dot-orange { background: #e6a23c; }

.notice-content {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.notice-text {
  font-size: 14px;
  color: #303133;
}

.notice-urgent .notice-text {
  color: #ff4d4f;
}

.notice-time {
  font-size: 12px;
  color: #909399;
}
</style>
