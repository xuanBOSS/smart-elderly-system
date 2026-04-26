<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { House, DataLine, Bell, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const chartRef = ref(null)
let chartInstance = null

const activeTab = ref('home')
const customerList = ref([])
const selectedElderId = ref(null)
const noticeList = ref([])
const indicatorList = ref([
  { label: '--/--', unit: 'mmHg', name: '收缩压/舒张压', color: '#FF4D4F' },
  { label: '--', unit: '次/分', name: '心率', color: '#52C41A' },
  { label: '--', unit: 'mmol/L', name: '血糖', color: '#1890FF' }
])

const showBindDialog = ref(false)
const bindLoading = ref(false)
const bindForm = ref({ username: '', relation: '' })

const doctorsForBooking = ref([])
const loadingDoctors = ref(false)
const showBookDialog = ref(false)
const bookDoctor = ref(null)
const scheduleRows = ref([])
const pickedScheduleId = ref(null)
const loadingSchedules = ref(false)

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    color: ['#FF4D4F', '#1890FF', '#52C41A'],
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['收缩压', '舒张压', '心率'],
      bottom: 0,
      textStyle: { color: '#303133', fontSize: 11 }
    },
    grid: { top: 16, left: 44, right: 12, bottom: 52, containLabel: false },
    xAxis: {
      type: 'category',
      data: [],
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399', fontSize: 10, rotate: 28, interval: 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399', fontSize: 10 },
      splitLine: { lineStyle: { color: '#F2F3F5' } }
    },
    series: [
      { name: '收缩压', type: 'line', data: [], smooth: true },
      { name: '舒张压', type: 'line', data: [], smooth: true },
      { name: '心率', type: 'line', data: [], smooth: true }
    ]
  })
}

const updateChart = (trend) => {
  if (!chartInstance) return
  chartInstance.setOption({
    xAxis: { data: trend?.dates || [] },
    series: [
      { name: '收缩压', data: trend?.systolic || [] },
      { name: '舒张压', data: trend?.diastolic || [] },
      { name: '心率', data: trend?.heartRate || [] }
    ]
  })
}

const fetchElders = async () => {
  try {
    const response = await request.get('/api/family/elders')
    const { code, message, data } = response.data
    if (code !== 200) {
      ElMessage.error(message || '获取老人列表失败')
      return
    }
    customerList.value = Array.isArray(data) ? data : []
    if (customerList.value.length > 0) {
      handleSelectElder(customerList.value[0].elderId)
    } else {
      selectedElderId.value = null
      updateChart({ dates: [], systolic: [], diastolic: [], heartRate: [] })
      noticeList.value = []
    }
  } catch (error) {
    console.error('获取老人列表失败:', error)
    ElMessage.error('获取老人列表失败')
  }
}

const fetchDashboard = async (elderId) => {
  try {
    const response = await request.get(`/api/health/dashboard?elderId=${elderId}`)
    const { code, message, data } = response.data
    if (code !== 200) {
      ElMessage.error(message || '获取健康大盘失败')
      return
    }
    const trend = data?.trend || {}
    const latest = data?.latest || {}
    indicatorList.value[0].label = latest.bloodPressure || '--/--'
    indicatorList.value[1].label = latest.heartRate || '--'
    indicatorList.value[2].label = latest.bloodSugar || '--'
    await nextTick()
    if (!chartInstance && chartRef.value) initChart()
    updateChart(trend)
    chartInstance?.resize()
  } catch (error) {
    console.error('获取健康大盘失败:', error)
    ElMessage.error('获取健康大盘失败')
  }
}

const fetchNotices = async (elderId) => {
  try {
    const response = await request.get(`/api/message/list?elderId=${elderId}`)
    const { code, message, data } = response.data
    if (code !== 200) {
      ElMessage.error(message || '获取消息失败')
      return
    }
    noticeList.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取消息失败:', error)
    ElMessage.error('获取消息失败')
  }
}

const handleSelectElder = (elderId) => {
  selectedElderId.value = elderId
  fetchDashboard(elderId)
  fetchNotices(elderId)
}

const submitBind = async () => {
  if (!bindForm.value.username || !bindForm.value.relation) {
    ElMessage.warning('请完整填写账号和亲属关系')
    return
  }
  bindLoading.value = true
  try {
    const res = await request.post('/api/family/bind', bindForm.value)
    if (res.data.code === 200) {
      ElMessage.success('绑定成功！')
      showBindDialog.value = false
      bindForm.value = { username: '', relation: '' }
      fetchElders()
    } else {
      ElMessage.error(res.data.message || '绑定失败')
    }
  } catch (error) {
    console.error('绑定请求异常:', error)
    ElMessage.error('网络或服务器异常，绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const loadDoctorsForBooking = async () => {
  if (!selectedElderId.value) {
    doctorsForBooking.value = []
    return
  }
  loadingDoctors.value = true
  try {
    const res = await request.get('/api/family/doctors-for-booking', {
      params: { elderId: selectedElderId.value }
    })
    if (res.data.code === 200) {
      doctorsForBooking.value = Array.isArray(res.data.data) ? res.data.data : []
    } else {
      doctorsForBooking.value = []
      ElMessage.error(res.data.message || '获取医生列表失败')
    }
  } catch (e) {
    doctorsForBooking.value = []
    ElMessage.error('获取医生列表失败')
  } finally {
    loadingDoctors.value = false
  }
}

const openFamilyBookDialog = async (doctor) => {
  if (!selectedElderId.value) {
    ElMessage.warning('请先在「家人」页选择要预约的老人')
    return
  }
  bookDoctor.value = doctor
  pickedScheduleId.value = null
  scheduleRows.value = []
  showBookDialog.value = true
  loadingSchedules.value = true
  try {
    const { data } = await request.get('/api/family/doctorSchedules', {
      params: { elderId: selectedElderId.value, doctorId: doctor.userId }
    })
    if (data.code === 200 && Array.isArray(data.data)) {
      scheduleRows.value = data.data.filter((row) => Number(row.remainCount) > 0)
      if (scheduleRows.value.length === 0) {
        ElMessage.warning('该医生暂无可预约号源')
      }
    } else {
      ElMessage.error(data?.message || '加载排班失败')
    }
  } catch (error) {
    ElMessage.error('加载排班失败')
  } finally {
    loadingSchedules.value = false
  }
}

const confirmFamilyBook = async () => {
  if (!pickedScheduleId.value || !selectedElderId.value || !bookDoctor.value) {
    ElMessage.warning('请选择就诊日期与时段')
    return
  }
  try {
    const res = await request.post('/api/family/appointment', {
      userId: selectedElderId.value,
      doctorId: bookDoctor.value.userId,
      scheduleId: pickedScheduleId.value
    })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '预约失败')
      return
    }
    showBookDialog.value = false
    ElMessage.success(`已为老人提交「${bookDoctor.value.realName}」的预约申请`)
    fetchNotices(selectedElderId.value)
  } catch {
    ElMessage.error('预约失败')
  }
}

const resizeChart = () => {
  chartInstance?.resize()
}

const goLogin = () => {
  localStorage.removeItem('token_1')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}

watch(activeTab, (tab) => {
  if (tab === 'trend') {
    nextTick(() => {
      if (!chartInstance && chartRef.value) initChart()
      if (selectedElderId.value) fetchDashboard(selectedElderId.value)
    })
  }
  if (tab === 'booking') {
    loadDoctorsForBooking()
  }
})

onMounted(() => {
  fetchElders()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="family-shell">
    <div class="family-page">
      <header class="family-top">
        <div>
          <div class="family-title">家属监护</div>
          <div class="family-sub">健康与预约</div>
        </div>
        <el-button type="primary" link class="exit-btn" @click="goLogin">退出</el-button>
      </header>

      <main class="family-scroll">
        <template v-if="activeTab === 'home'">
          <div class="block">
            <div class="block-title">我的家人</div>
            <div class="chip-row">
              <button
                v-for="item in customerList"
                :key="item.elderId"
                type="button"
                :class="['chip', { active: selectedElderId === item.elderId }]"
                @click="handleSelectElder(item.elderId)"
              >
                {{ item.name }}
              </button>
              <button type="button" class="chip add" @click="showBindDialog = true">+ 绑定</button>
            </div>
          </div>
          <div class="block hint">
            在「趋势」查看近7日曲线，在「消息」查看提醒，在「帮预约」为当前选中老人挂号。
          </div>
        </template>

        <template v-else-if="activeTab === 'trend'">
          <div class="block">
            <div class="block-title">近7日健康趋势</div>
            <div ref="chartRef" class="trend-chart" />
          </div>
          <div class="metrics">
            <div v-for="item in indicatorList" :key="item.name" class="metric">
              <div class="metric-val" :style="{ color: item.color }">{{ item.label }}</div>
              <div class="metric-unit">{{ item.unit }}</div>
              <div class="metric-name">{{ item.name }}</div>
            </div>
          </div>
        </template>

        <template v-else-if="activeTab === 'notices'">
          <div class="block">
            <div class="block-title">消息通知</div>
            <el-empty v-if="noticeList.length === 0" description="暂无新消息" :image-size="56" />
            <div
              v-for="(notice, index) in noticeList"
              v-else
              :key="notice.id"
              class="notice-row"
              :class="{ urgent: notice.type === 'URGENT', last: index === noticeList.length - 1 }"
            >
              <div class="notice-main">
                <div class="notice-text">{{ notice.content }}</div>
                <div class="notice-time">{{ notice.time }}</div>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="block">
            <div class="block-title">帮老人预约</div>
            <p class="sub">当前老人：{{ customerList.find((c) => c.elderId === selectedElderId)?.name || '请先绑定并选择' }}</p>
            <div v-loading="loadingDoctors" class="doctor-list">
              <div v-for="doc in doctorsForBooking" :key="doc.userId" class="doc-row">
                <span class="doc-name">{{ doc.realName }}</span>
                <button type="button" class="mini-btn" @click="openFamilyBookDialog(doc)">预约</button>
              </div>
              <el-empty v-if="!loadingDoctors && doctorsForBooking.length === 0" description="暂无医生数据" />
            </div>
          </div>
        </template>
      </main>

      <nav class="family-bottom" aria-label="底部功能切换">
        <div class="nav-caption">当前页面（高亮）</div>
        <div class="nav-btn-row">
        <button type="button" :class="['nav-btn', { on: activeTab === 'home' }]" :aria-current="activeTab==='home'?'page':undefined" @click="activeTab = 'home'">
          <House class="nav-ic" /><span>家人</span>
        </button>
        <button type="button" :class="['nav-btn', { on: activeTab === 'trend' }]" :aria-current="activeTab==='trend'?'page':undefined" @click="activeTab = 'trend'">
          <DataLine class="nav-ic" /><span>趋势</span>
        </button>
        <button type="button" :class="['nav-btn', { on: activeTab === 'notices' }]" :aria-current="activeTab==='notices'?'page':undefined" @click="activeTab = 'notices'">
          <Bell class="nav-ic" /><span>消息</span>
        </button>
        <button type="button" :class="['nav-btn', { on: activeTab === 'booking' }]" :aria-current="activeTab==='booking'?'page':undefined" @click="activeTab = 'booking'">
          <Calendar class="nav-ic" /><span>帮预约</span>
        </button>
        </div>
      </nav>

      <el-dialog v-model="showBindDialog" title="绑定老人" width="320px" center destroy-on-close>
        <el-form label-position="top">
          <el-form-item label="老人登录账号">
            <el-input v-model="bindForm.username" placeholder="如 laowang" clearable />
          </el-form-item>
          <el-form-item label="亲属关系">
            <el-input v-model="bindForm.relation" placeholder="如：父子" clearable />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showBindDialog = false">取消</el-button>
          <el-button type="primary" :loading="bindLoading" @click="submitBind">确认绑定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="showBookDialog"
        title="选择预约时段"
        width="92%"
        align-center
        destroy-on-close
        class="fam-book-dlg"
      >
        <div v-loading="loadingSchedules">
          <el-radio-group v-if="scheduleRows.length" v-model="pickedScheduleId" class="fam-schedule-group">
            <el-radio
              v-for="row in scheduleRows"
              :key="row.scheduleId"
              :label="row.scheduleId"
              class="fam-schedule-item"
            >
              {{ row.workDate }} {{ row.timeSlotText }} · 剩余 {{ row.remainCount }}
            </el-radio>
          </el-radio-group>
          <el-empty v-else-if="!loadingSchedules" description="暂无可选号源" />
        </div>
        <template #footer>
          <el-button @click="showBookDialog = false">取消</el-button>
          <el-button type="primary" :disabled="!pickedScheduleId" @click="confirmFamilyBook">确认</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.family-shell {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: stretch;
  background: linear-gradient(180deg, #e9eff7 0%, #dce6f1 100%);
  padding: 12px;
}

.family-page {
  width: 100%;
  max-width: 400px;
  min-height: min(780px, calc(100vh - 24px));
  background: linear-gradient(180deg, #f6f9fe 0%, #e8eef6 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d0dce8;
}

.family-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: linear-gradient(90deg, #1f4d92 0%, #2f74c8 100%);
  color: #fff;
}

.family-title {
  font-size: 20px;
  font-weight: 800;
}

.family-sub {
  font-size: 12px;
  opacity: 0.88;
  margin-top: 2px;
}

.exit-btn {
  color: #fff !important;
  font-weight: 700;
}

.family-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 12px 8px;
}

.block {
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid #dfeaf6;
}

.block-title {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 10px;
}

.block.hint {
  font-size: 13px;
  color: #475569;
  line-height: 1.45;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid #c5d9ee;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 14px;
  font-weight: 600;
  background: #f5f9ff;
  color: #334155;
  cursor: pointer;
}

.chip.active {
  background: linear-gradient(90deg, #2185e8 0%, #3aa0ff 100%);
  color: #fff;
  border-color: transparent;
}

.chip.add {
  border-style: dashed;
  color: #1a84de;
}

.trend-chart {
  width: 100%;
  height: 240px;
}

.metrics {
  display: flex;
  gap: 8px;
}

.metric {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 10px 8px;
  border: 1px solid #e2e8f0;
  text-align: center;
}

.metric-val {
  font-size: 17px;
  font-weight: 800;
}

.metric-unit {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.metric-name {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.notice-row {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.notice-row.last {
  border-bottom: none;
}

.notice-row.urgent .notice-text {
  color: #dc2626;
  font-weight: 700;
}

.notice-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notice-text {
  font-size: 14px;
  color: #334155;
  line-height: 1.35;
}

.notice-time {
  font-size: 12px;
  color: #94a3b8;
}

.sub {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 10px;
}

.doctor-list {
  min-height: 60px;
}

.doc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.mini-btn {
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  font-weight: 800;
  font-size: 13px;
  color: #fff;
  background: linear-gradient(90deg, #2185e8 0%, #3aa0ff 100%);
  cursor: pointer;
}

.family-bottom {
  border-top: 1px solid #d6e2ef;
  background: #fdfefe;
  padding: 6px 8px 10px;
}

.nav-caption {
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.nav-btn-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}

.nav-btn {
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  padding: 6px 0;
  cursor: pointer;
}

.nav-btn.on {
  color: #1d84de;
  background: rgba(29, 132, 222, 0.1);
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px rgba(29, 132, 222, 0.25);
}

.nav-ic {
  width: 22px;
  height: 22px;
}

.fam-schedule-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.fam-schedule-item {
  margin-right: 0;
  height: auto;
  white-space: normal;
  align-items: flex-start;
  line-height: 1.35;
}
</style>
