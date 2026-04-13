# AdminView.vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const timeText = ref('00:00:00')
const chartRef = ref(null)
let chartInstance = null

// 响应式数据源
const stats = ref({
  totalElders: 0,
  chronicElders: 0,
  todayAppointments: 0,
  monthlyEmergencies: 0
})
const orderList = ref([])

// 🌟 AI 诊断相关响应式变量
const showAiDialog = ref(false)
const aiLoading = ref(false)
const aiReport = ref('')

// 时间更新逻辑
const updateTime = () => {
  const now = new Date()
  const pad = (num) => `${num}`.padStart(2, '0')
  timeText.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

// 初始化空图表
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
      data: [], // 等待后端数据
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.3)' } },
      axisLabel: { color: '#fff' },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: [], // 等待后端数据
      barWidth: 16,
      label: { show: true, position: 'right', color: '#fff' }
    }]
  })
}

// 1. 获取左侧统计大盘数据
const fetchStatistics = async () => {
  try {
    const res = await request.get('/api/community/statistics')
    if (res.data.code === 200) {
      const data = res.data.data
      stats.value.totalElders = data.totalElders || 0
      stats.value.chronicElders = data.chronicElders || 0
      stats.value.todayAppointments = data.todayAppointments || 0
      stats.value.monthlyEmergencies = data.monthlyEmergencies || 0
      
      if (chartInstance && data.diseaseChart) {
        chartInstance.setOption({
          yAxis: { data: data.diseaseChart.labels },
          series: [{ data: data.diseaseChart.values }]
        })
      }
    }
  } catch (error) {
    console.error('获取统计大盘失败', error)
  }
}

// 2. 获取右侧实时工单列表
const fetchEmergencies = async () => {
  try {
    const res = await request.get('/api/community/emergencies')
    if (res.data.code === 200) {
      orderList.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取紧急工单失败', error)
  }
}

// 3. 处理紧急工单
const handleOrder = async (helpId, action) => {
  try {
    const res = await request.post(`/api/community/emergency/handle?helpId=${helpId}&action=${action}`)
    if (res.data.code === 200) {
      ElMessage.success(action === 2 ? '已接单，请尽快上门处理' : '问题已解决，工单归档')
      fetchEmergencies()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('工单处理异常', error)
    ElMessage.error('网络异常')
  }
}

// 🌟 4. 触发 AI 风险预测
const runAiPrediction = () => {
  // 1. 弹出输入框，让网格员动态输入需要查询的老人 ID
  ElMessageBox.prompt('请输入需要进行 AI 风险深度评估的老人 ID（如：1038）：', '🤖 智能巡检系统', {
    confirmButtonText: '启动 DeepSeek 医疗大脑',
    cancelButtonText: '取消',
    inputPattern: /^[0-9]+$/, // 正则校验：只能输入纯数字
    inputErrorMessage: '老人 ID 格式错误，必须是纯数字！',
  }).then(async ({ value }) => {
    // 2. 用户点击确认后，拿到真实输入的 ID
    const elderId = value
    
    showAiDialog.value = true // 打开报告展示弹窗
    aiLoading.value = true    // 开启炫酷加载动画
    aiReport.value = ''       // 清空上一次的旧报告
    
    try {
      // 3. 把用户动态输入的 elderId 拼接到请求 URL 中！
      const res = await request.get(`/api/community/risk/predict?elderId=${elderId}`)
      if (res.data.code === 200) {
        aiReport.value = res.data.data
      } else {
        aiReport.value = 'AI 诊断失败：' + (res.data.message || '未知错误')
      }
    } catch (error) {
      console.error('呼叫 AI 异常', error)
      aiReport.value = '连接 DeepSeek 大脑超时或失败，请检查网络。'
    } finally {
      aiLoading.value = false
    }
  }).catch(() => {
    // 用户点击取消，静默处理，什么都不做
  })
}

const resizeChart = () => {
  chartInstance?.resize()
}

const goLogin = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  updateTime()
  const timer = setInterval(updateTime, 1000)
  initChart()
  fetchStatistics()
  fetchEmergencies()
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
        <div class="stat-value">{{ stats.totalElders }}</div>
        <div class="stat-trend">实时核准</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">慢病管理人数</div>
        <div class="stat-value">{{ stats.chronicElders }}</div>
        <div class="stat-trend">高危人群监测中</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">今日门诊预约</div>
        <div class="stat-value">{{ stats.todayAppointments }}</div>
        <div class="stat-trend">系统实时同步</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">紧急求助（本月）</div>
        <div class="stat-value">{{ stats.monthlyEmergencies }}</div>
        <div class="stat-trend">含误触记录</div>
      </div>
      <div class="chart-panel">
        <div class="chart-title">慢病类型分布</div>
        <div ref="chartRef" class="admin-chart"></div>
      </div>
    </aside>

    <section class="admin-main">
      <div class="main-header">
        <div class="main-title">社区态势监控</div>
        <div class="header-actions">
          <el-button type="warning" plain size="small" @click="runAiPrediction()">
            🤖 AI 高危人群巡检
          </el-button>
          <div class="main-time">{{ timeText }}</div>
        </div>
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
        <div class="map-note">演示状态 · 3栋区域状态异常</div>
      </div>
    </section>

    <aside class="admin-right">
      <div class="side-label">实时工单</div>
      
      <div v-if="orderList.length === 0" style="text-align: center; margin-top: 50px; color: rgba(255,255,255,0.4); font-size: 14px;">
        暂无待处理的紧急求助
      </div>

      <div 
        v-for="order in orderList" 
        :key="order.helpId"
        :class="['order-card', order.type === 'URGENT' ? 'order-urgent' : 'order-normal']"
      >
        <div class="order-header">
          <span :class="['order-tag', order.type.toLowerCase()]">{{ order.type === 'URGENT' ? '紧急' : '普通' }}</span>
          <span>{{ order.time }}</span>
        </div>
        <div class="order-content">{{ order.content }}</div>
        <div class="order-status">{{ order.status }}</div>
        <el-button 
          v-if="order.status === '待处理'" 
          size="small" 
          type="primary" 
          class="order-btn" 
          @click="handleOrder(order.helpId, 2)"
        >社区接单</el-button>
        <el-button 
          v-else 
          size="small" 
          type="success" 
          class="order-btn" 
          @click="handleOrder(order.helpId, 3)"
        >确认已解决</el-button>
      </div>
      
      <div class="right-exit" @click="goLogin">退出监控中心</div>
    </aside>

    <el-dialog 
      v-model="showAiDialog" 
      title="🤖 AI 智能风险预测报告" 
      width="600px"
      class="ai-dialog"
      append-to-body
      destroy-on-close
    >
      <div v-loading="aiLoading" element-loading-text="DeepSeek 大脑正在进行深度医学推理..." class="ai-report-content">
        <div v-if="!aiReport" class="ai-empty">
           等待 AI 分析返回...
        </div>
        <div v-else class="report-text" v-html="aiReport"></div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showAiDialog = false">阅知并归档</el-button>
      </template>
    </el-dialog>
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
  overflow-y: auto;
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

/* 🌟 新增：头部操作区样式 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
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
  overflow-y: auto;
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
  line-height: 1.4;
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
  padding-top: 20px;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 🌟 新增：AI 弹窗与报告样式 */
:deep(.ai-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(24, 144, 255, 0.2);
  margin-right: 0;
}

:deep(.ai-dialog .el-dialog__title) {
  color: #1890ff;
  font-weight: 700;
}

.ai-report-content {
  min-height: 200px;
  background: rgba(24, 144, 255, 0.05);
  border: 1px solid rgba(24, 144, 255, 0.2);
  border-radius: 8px;
  padding: 20px;
  color: #333; /* 文字颜色，因为弹窗是白底的 */
}

.report-text {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap; /* 核心：识别换行符 \n */
  text-align: justify;
}

/* 如果 AI 返回的是 markdown 语法，简单的处理加粗显示 */
:deep(.report-text strong),
:deep(.report-text b) {
  color: #ff4d4f;
  font-weight: bold;
}

.ai-empty {
  text-align: center;
  color: #1890ff;
  line-height: 200px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 0.5; }
  50% { opacity: 1; }
  100% { opacity: 0.5; }
}
</style>
