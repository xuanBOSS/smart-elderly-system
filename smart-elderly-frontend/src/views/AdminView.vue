<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const timeText = ref('00:00:00')
const chartRef = ref(null)
let chartInstance = null
let clockTimer = null
let dataPollTimer = null

// 响应式数据源
const stats = ref({
  totalElders: 0,
  chronicElders: 0,
  todayAppointments: 0,
  monthlyEmergencies: 0
})
const orderList = ref([])
const showStatDetail = ref(false)
const statDetailTitle = ref('')
const statDetailRows = ref([])
const statDetailLoading = ref(false)
const statTableColumns = ref([])

// 🌟 AI 诊断相关响应式变量
const showAiDialog = ref(false)
const aiLoading = ref(false)
const aiReport = ref('')
const aiArchiveElderId = ref('')

/** 从工单文案中解析 1–12 栋，用于态势图高亮（不再写死 3 栋） */
const extractBuildingNums = (text) => {
  if (text == null || text === '') return []
  const s = String(text)
  const found = new Set()
  const patterns = [/(\d{1,2})\s*栋/g, /第\s*(\d{1,2})\s*栋/g, /(\d{1,2})\s*号楼/g]
  for (const re of patterns) {
    let m
    const r = new RegExp(re.source, re.flags)
    while ((m = r.exec(s))) {
      const n = parseInt(m[1], 10)
      if (!Number.isNaN(n) && n >= 1 && n <= 12) found.add(n)
    }
  }
  return [...found]
}

const alarmBuildingNumbers = computed(() => {
  const set = new Set()
  for (const o of orderList.value) {
    // 方案2：仅“待处理/处理中”参与态势高亮，不包含“已解决”
    if (!(o.status === '待处理' || o.status === '家属处理中' || o.status === '社区已接单')) continue
    for (const n of extractBuildingNums(o.content)) set.add(n)
  }
  return set
})

const mapStatusNote = computed(() => {
  const nums = [...alarmBuildingNumbers.value].sort((a, b) => a - b)
  if (nums.length === 0) return '当前无待处理/处理中工单关联楼栋，网格为常态监测'
  return `待处理/处理中关联楼栋：${nums.map((n) => `${n}栋`).join('、')}`
})

const canResolveOrder = (status) => status === '家属处理中' || status === '社区已接单'

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
    tooltip: {
      trigger: 'item',
      axisPointer: { type: 'none' },
      backgroundColor: 'rgba(15, 33, 53, 0.96)',
      borderColor: 'rgba(120, 175, 235, 0.45)',
      borderWidth: 1,
      textStyle: { color: '#eaf4ff', fontSize: 12 },
      formatter: (params) => `${params.name}：${params.value}`
    },
    grid: { left: '5%', right: '12%', bottom: '4%', top: '20%', containLabel: true },
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
    aiArchiveElderId.value = elderId

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

const archiveAiReport = () => {
  const report = (aiReport.value || '').trim()
  if (report) {
    const key = 'community_ai_risk_archives'
    let list = []
    try {
      list = JSON.parse(localStorage.getItem(key) || '[]')
      if (!Array.isArray(list)) list = []
    } catch {
      list = []
    }
    list.unshift({
      at: new Date().toISOString(),
      elderId: aiArchiveElderId.value || '',
      preview: report.slice(0, 400)
    })
    localStorage.setItem(key, JSON.stringify(list.slice(0, 50)))
  }
  ElMessage.success('已阅知并归档（本机存储）')
  showAiDialog.value = false
}

const resizeChart = () => {
  chartInstance?.resize()
}

const goLogin = () => {
  localStorage.removeItem('token_3')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}

const statDetailTitles = {
  elders: '在册老人名册（最多 200 条）',
  chronic: '慢病管理关注名单',
  todayAppointments: '今日门诊预约明细',
  monthEmergencies: '本月紧急求助记录'
}
const statDetailColumnDefs = {
  elders: [
    { prop: 'name', label: '姓名', minWidth: 90 },
    { prop: 'username', label: '账号', minWidth: 90 },
    { prop: 'userId', label: 'ID', width: 140 }
  ],
  chronic: [
    { prop: 'name', label: '姓名', minWidth: 90 },
    { prop: 'userId', label: '老人ID', width: 150 }
  ],
  todayAppointments: [
    { prop: 'elderName', label: '老人', minWidth: 70 },
    { prop: 'doctorName', label: '医生', minWidth: 70 },
    { prop: 'time', label: '预约时间', minWidth: 120 },
    { prop: 'status', label: '状态', width: 120 }
  ],
  monthEmergencies: [
    { prop: 'time', label: '时间', minWidth: 70 },
    { prop: 'elderName', label: '老人', minWidth: 70 },
    { prop: 'location', label: '地点', minWidth: 140 },
    { prop: 'statusText', label: '状态', width: 120 }
  ]
}

const openStatDetail = async (typeKey) => {
  statDetailTitle.value = statDetailTitles[typeKey] || '数据明细'
  statTableColumns.value = statDetailColumnDefs[typeKey] || []
  showStatDetail.value = true
  statDetailLoading.value = true
  statDetailRows.value = []
  try {
    const res = await request.get('/api/community/statDetail', { params: { type: typeKey } })
    if (res.data.code === 200) {
      statDetailRows.value = res.data.data || []
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('网络异常')
  } finally {
    statDetailLoading.value = false
  }
}

onMounted(() => {
  updateTime()
  clockTimer = setInterval(updateTime, 1000)
  initChart()
  fetchStatistics()
  fetchEmergencies()
  window.addEventListener('resize', resizeChart)
  dataPollTimer = setInterval(() => {
    fetchStatistics()
    fetchEmergencies()
  }, 20000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (dataPollTimer) clearInterval(dataPollTimer)
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <div class="side-label">数据概览</div>
      <div class="stat-card stat-card-click" @click="openStatDetail('elders')">
        <div class="stat-title">社区老年人口</div>
        <div class="stat-value">{{ stats.totalElders }}</div>
        <div class="stat-trend">实时核准</div>
      </div>
      <div class="stat-card stat-card-click" @click="openStatDetail('chronic')">
        <div class="stat-title">慢病管理人数</div>
        <div class="stat-value">{{ stats.chronicElders }}</div>
        <div class="stat-trend">高危人群监测中</div>
      </div>
      <div class="stat-card stat-card-click" @click="openStatDetail('todayAppointments')">
        <div class="stat-title">今日门诊预约</div>
        <div class="stat-value">{{ stats.todayAppointments }}</div>
        <div class="stat-trend">系统实时同步</div>
      </div>
      <div class="stat-card stat-card-click" @click="openStatDetail('monthEmergencies')">
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
          <el-button type="warning" plain size="small" @click="runAiPrediction()">🤖 AI 高危人群巡检</el-button>
          <div class="main-time">{{ timeText }}</div>
        </div>
        <div class="header-center-logout">
          <el-button type="danger" round class="logout-main-btn" @click="goLogin">安全退出登录</el-button>
        </div>
      </div>
      <div class="status-panel">
        <div class="grid-map">
          <template v-for="num in 12" :key="num">
            <div :class="['map-cell', { alarm: alarmBuildingNumbers.has(num) }]">
              <span class="map-cell-label">{{ num }}栋</span>
              <span v-if="alarmBuildingNumbers.has(num)" class="alarm-dot"></span>
            </div>
          </template>
        </div>
        <div class="map-note">{{ mapStatusNote }}</div>
      </div>
    </section>

    <aside class="admin-right">
      <div class="side-label">实时工单</div>
      
      <div v-if="orderList.length === 0" class="empty-order-tip">
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
          v-else-if="canResolveOrder(order.status)" 
          size="small" 
          type="success" 
          class="order-btn" 
          @click="handleOrder(order.helpId, 3)"
        >确认已解决</el-button>
        <el-button
          v-else
          size="small"
          class="order-btn"
          disabled
        >已归档</el-button>
      </div>
      
    </aside>

    <el-dialog v-model="showStatDetail" :title="statDetailTitle" width="720px" append-to-body destroy-on-close>
      <el-table v-loading="statDetailLoading" :data="statDetailRows" stripe border max-height="420">
        <el-table-column
          v-for="col in statTableColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          show-overflow-tooltip
        />
      </el-table>
    </el-dialog>

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
        <el-button type="primary" @click="archiveAiReport">阅知并归档</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  width: 100%;
  min-width: 960px;
  height: 100vh;
  overflow-x: auto;
  overflow-y: hidden;
  background:
    radial-gradient(circle at 22% 12%, rgba(62, 137, 255, 0.18), transparent 30%),
    radial-gradient(circle at 78% 88%, rgba(49, 194, 156, 0.15), transparent 26%),
    linear-gradient(180deg, #0f2238 0%, #0b1a2c 100%);
  color: #fff;
}

.admin-side,
.admin-right {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
}

.admin-side {
  flex: 0 0 340px;
  width: 340px;
  min-width: 300px;
  border-right: 1px solid rgba(123, 176, 236, 0.16);
  background: rgba(15, 33, 53, 0.65);
  backdrop-filter: blur(2px);
  overflow-y: auto;
}

.side-label {
  font-size: 13px;
  color: rgba(174, 212, 248, 0.82);
  letter-spacing: 2px;
  margin-bottom: 20px;
  font-weight: 600;
}

.stat-card {
  background: linear-gradient(180deg, rgba(31, 61, 92, 0.7) 0%, rgba(24, 48, 75, 0.76) 100%);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid rgba(120, 175, 235, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.02);
}

.stat-card-click {
  cursor: pointer;
}

.stat-card-click:hover {
  border-color: rgba(180, 220, 255, 0.45);
}

.stat-title {
  font-size: 12px;
  color: rgba(189, 218, 245, 0.78);
}

.stat-value {
  margin-top: 8px;
  font-size: 34px;
  font-weight: 700;
  color: #f4fbff;
  letter-spacing: 0.3px;
  line-height: 1.2;
  word-break: break-all;
}

.stat-trend {
  margin-top: 8px;
  font-size: 11px;
  color: #61d882;
}

.chart-panel {
  background: linear-gradient(180deg, rgba(31, 61, 92, 0.72) 0%, rgba(24, 48, 75, 0.78) 100%);
  border-radius: 12px;
  padding: 16px;
  margin-top: 12px;
  border: 1px solid rgba(120, 175, 235, 0.18);
}

.chart-title {
  font-size: 13px;
  color: #d6ebff;
  margin-bottom: 14px;
  font-weight: 600;
}

.admin-chart {
  width: 100%;
  height: 190px;
}

.admin-main {
  flex: 1 1 auto;
  min-width: 380px;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 20px;
}

.main-title {
  font-size: 20px;
  font-weight: 700;
  color: #e8f3ff;
}

/* 🌟 新增：头部操作区样式 */
.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-center-logout {
  margin-top: 0;
  display: flex;
  justify-content: center;
}

.logout-main-btn {
  min-width: 160px;
  font-weight: 700;
}

.main-time {
  font-size: 14px;
  color: rgba(196, 220, 245, 0.85);
  font-weight: 600;
}

.status-panel {
  flex: 1;
  background: linear-gradient(180deg, rgba(20, 42, 66, 0.76) 0%, rgba(15, 34, 53, 0.8) 100%);
  border: 1px solid rgba(117, 173, 235, 0.2);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.02);
}

.grid-map {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.map-cell {
  width: 80px;
  height: 60px;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(58, 105, 153, 0.34) 0%, rgba(38, 76, 116, 0.42) 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  color: rgba(215, 231, 246, 0.85);
  font-size: 12px;
  border: 1px solid rgba(133, 183, 240, 0.24);
}

.map-cell.alarm {
  background: linear-gradient(180deg, rgba(255, 77, 79, 0.35) 0%, rgba(190, 42, 44, 0.42) 100%);
  border: 1px solid #ff4d4f;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 0 0 1px rgba(255, 77, 79, 0.25), 0 0 16px rgba(255, 77, 79, 0.26);
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
  color: rgba(202, 224, 246, 0.85);
  font-size: 12px;
}

.admin-right {
  flex: 0 0 300px;
  width: 300px;
  min-width: 260px;
  border-left: 1px solid rgba(123, 176, 236, 0.16);
  background: rgba(14, 31, 50, 0.64);
  backdrop-filter: blur(2px);
  overflow-y: auto;
}

.order-card {
  background: linear-gradient(180deg, rgba(31, 61, 92, 0.72) 0%, rgba(24, 48, 75, 0.78) 100%);
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid rgba(120, 175, 235, 0.18);
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
  color: rgba(227, 239, 250, 0.92);
  line-height: 1.4;
  white-space: normal;
  word-break: break-word;
}

.order-status {
  margin-top: 6px;
  font-size: 12px;
  color: #ff9ea0;
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
  color: rgba(194, 218, 243, 0.45);
  cursor: pointer;
}

.empty-order-tip {
  text-align: center;
  margin-top: 50px;
  color: rgba(189, 217, 245, 0.65);
  font-size: 14px;
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
