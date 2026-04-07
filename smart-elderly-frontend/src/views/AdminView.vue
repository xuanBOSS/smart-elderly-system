# AdminView.vue - 社区工作人员界面组件

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'

const router = useRouter()
const timeText = ref('00:00:00')
const chartRef = ref(null)
let chartInstance = null

const updateTime = () => {
  const now = new Date()
  const pad = (num) => `${num}`.padStart(2, '0')
  timeText.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    backgroundColor: 'transparent',
    color: ['#1890FF', '#52C41A', '#E6A23C', '#FF4D4F'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, textStyle: { color: '#fff' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '20%', containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.3)' } },
      axisLabel: { color: '#fff' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
    },
    yAxis: {
      type: 'category',
      data: ['高血压', '糖尿病', '关节炎', '冠心病'],
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.3)' } },
      axisLabel: { color: '#fff' },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: [186, 98, 72, 67],
      barWidth: 16,
      label: { show: true, position: 'right', color: '#fff' }
    }]
  })
}

const resizeChart = () => {
  chartInstance?.resize()
}

const goLogin = () => router.push('/login')

onMounted(() => {
  updateTime()
  const timer = setInterval(updateTime, 1000)
  initChart()
  window.addEventListener('resize', resizeChart)
  onUnmounted(() => {
    clearInterval(timer)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <div class="side-label">数据概览</div>
      <div class="stat-card">
        <div class="stat-title">社区老年人口</div>
        <div class="stat-value">1,286</div>
        <div class="stat-trend">+12 本月新增</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">慢病管理人数</div>
        <div class="stat-value">423</div>
        <div class="stat-trend">+8 本月新增</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">今日门诊预约</div>
        <div class="stat-value">47</div>
        <div class="stat-trend">+3 本日新增</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">紧急求助（本月）</div>
        <div class="stat-value">8</div>
        <div class="stat-trend">-1 环比</div>
      </div>
      <div class="chart-panel">
        <div class="chart-title">慢病类型分布</div>
        <div ref="chartRef" class="admin-chart"></div>
      </div>
    </aside>

    <section class="admin-main">
      <div class="main-header">
        <div class="main-title">社区态势监控</div>
        <div class="main-time">{{ timeText }}</div>
      </div>
      <div class="status-panel">
        <div class="grid-map">
          <template v-for="num in 12" :key="num">
            <div :class="['map-cell', { alarm: num === 3 }]">
              <span class="map-cell-label">{{ num }}栋</span>
              <span v-if="num === 3" class="alarm-dot"></span>
            </div>
          </template>
        </div>
        <div class="map-note">模拟演示 · 3栋触发紧急求助</div>
      </div>
    </section>

    <aside class="admin-right">
      <div class="side-label">实时工单</div>
      <div class="order-card order-urgent">
        <div class="order-header">
          <span class="order-tag urgent">紧急</span>
          <span>14:30</span>
        </div>
        <div class="order-content">3栋2单元401 王大爷 摔倒求助</div>
        <div class="order-status">待处理</div>
        <el-button size="small" type="primary" class="order-btn">接单处理</el-button>
      </div>
      <div class="order-card order-urgent">
        <div class="order-header">
          <span class="order-tag urgent">紧急</span>
          <span>14:15</span>
        </div>
        <div class="order-content">7栋1单元201 李奶奶 胸闷不适</div>
        <div class="order-status">家属已接单</div>
        <el-button size="small" type="primary" class="order-btn">接单处理</el-button>
      </div>
      <div class="order-card order-normal">
        <div class="order-header">
          <span class="order-tag normal">普通</span>
          <span>11:20</span>
        </div>
        <div class="order-content">2栋3单元502 张大爷 用药咨询</div>
        <div class="order-status">已解决</div>
        <el-button size="small" type="primary" class="order-btn">接单处理</el-button>
      </div>
      <div class="order-card order-normal">
        <div class="order-header">
          <span class="order-tag normal">普通</span>
          <span>09:40</span>
        </div>
        <div class="order-content">5栋1单元102 赵奶奶 预约确认</div>
        <div class="order-status">已解决</div>
        <el-button size="small" type="primary" class="order-btn">接单处理</el-button>
      </div>
      <div class="right-exit" @click="goLogin">退出</div>
    </aside>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #0d1b2a;
  color: #fff;
}

.admin-side,
.admin-right {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
}

.admin-side {
  width: 280px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.side-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 2px;
  margin-bottom: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.stat-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
}

.stat-trend {
  margin-top: 8px;
  font-size: 11px;
  color: #52c41a;
}

.chart-panel {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 16px;
  margin-top: 12px;
}

.chart-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 14px;
}

.admin-chart {
  width: 100%;
  height: 160px;
}

.admin-main {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.main-title {
  font-size: 18px;
  font-weight: 700;
}

.main-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.status-panel {
  flex: 1;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.grid-map {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.map-cell {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.map-cell.alarm {
  background: rgba(255, 77, 79, 0.3);
  border: 1px solid #ff4d4f;
  color: #fff;
  font-weight: 700;
}

.alarm-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
  animation: blink 1.2s infinite;
}

.map-note {
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.admin-right {
  width: 300px;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
}

.order-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 10px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}

.order-tag.urgent {
  background: rgba(255, 77, 79, 0.18);
  color: #ff4d4f;
}

.order-tag.normal {
  background: rgba(24, 144, 255, 0.18);
  color: #1890ff;
}

.order-content {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.order-status {
  margin-top: 6px;
  font-size: 12px;
  color: #ff4d4f;
}

.order-btn {
  margin-top: 10px;
  width: 100%;
}

.right-exit {
  margin-top: auto;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
