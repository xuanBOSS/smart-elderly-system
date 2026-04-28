<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
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
const activeBookingDoctorIds = ref(new Set())
const familyAppointments = ref([])
const loadingAppointments = ref(false)
const selectedElderMedication = ref('暂无明确用药要求')
const selectedElderDiagnoses = ref([])
const homeLowerTab = ref('diagnosis')

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
      bottom: 4,
      textStyle: { color: '#303133', fontSize: 11 }
    },
    grid: { top: 16, left: 44, right: 12, bottom: 72, containLabel: false },
    xAxis: {
      type: 'category',
      data: [],
      axisLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399', fontSize: 10, rotate: 24, interval: 0, margin: 14 }
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
      selectedElderMedication.value = '暂无明确用药要求'
      selectedElderDiagnoses.value = []
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
    selectedElderMedication.value = (latest.medicationInfo && String(latest.medicationInfo).trim())
      ? String(latest.medicationInfo).trim()
      : '暂无明确用药要求'
    selectedElderDiagnoses.value = Array.isArray(data?.diagnosisRecords) ? data.diagnosisRecords : []
    await nextTick()
    if (!chartInstance && chartRef.value) initChart()
    updateChart(trend)
    chartInstance?.resize()
  } catch (error) {
    console.error('获取健康大盘失败:', error)
    ElMessage.error('获取健康大盘失败')
    selectedElderMedication.value = '暂无明确用药要求'
    selectedElderDiagnoses.value = []
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

const loadActiveBookings = async (elderId) => {
  if (!elderId) {
    activeBookingDoctorIds.value = new Set()
    return
  }
  try {
    const res = await request.get('/api/family/appointments', { params: { elderId } })
    if (res.data.code !== 200 || !Array.isArray(res.data.data)) {
      activeBookingDoctorIds.value = new Set()
      return
    }
    const set = new Set(
      res.data.data
        .filter((item) => item && (item.status === '待确认' || item.status === '已确认'))
        .map((item) => String(item.doctorId))
    )
    activeBookingDoctorIds.value = set
  } catch {
    activeBookingDoctorIds.value = new Set()
  }
}

const loadFamilyAppointments = async (elderId) => {
  if (!elderId) {
    familyAppointments.value = []
    return
  }
  loadingAppointments.value = true
  try {
    const res = await request.get('/api/family/appointments', { params: { elderId } })
    if (res.data.code !== 200 || !Array.isArray(res.data.data)) {
      familyAppointments.value = []
      return
    }
    familyAppointments.value = res.data.data
  } catch {
    familyAppointments.value = []
  } finally {
    loadingAppointments.value = false
  }
}

const handleSelectElder = (elderId) => {
  selectedElderId.value = elderId
  fetchDashboard(elderId)
  fetchNotices(elderId)
  loadActiveBookings(elderId)
  loadFamilyAppointments(elderId)
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

const bookingDoctorsView = computed(() => {
  return (doctorsForBooking.value || []).map((doc) => ({
    ...doc,
    hasActiveBooking: activeBookingDoctorIds.value.has(String(doc.userId))
  }))
})

const openFamilyBookDialog = async (doctor) => {
  if (!selectedElderId.value) {
    ElMessage.warning('请先在「家人」页选择要预约的老人')
    return
  }
  if (activeBookingDoctorIds.value.has(String(doctor.userId))) {
    ElMessage.warning('该医生已有进行中的预约，请勿重复提交')
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
    await loadActiveBookings(selectedElderId.value)
    await loadFamilyAppointments(selectedElderId.value)
    await loadDoctorsForBooking()
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
      // 趋势页切换会销毁并重建 DOM，需要判断图表实例是否仍绑定当前节点
      if (!chartRef.value) return
      if (!chartInstance) {
        initChart()
      } else if (chartInstance.getDom() !== chartRef.value) {
        chartInstance.dispose()
        chartInstance = null
        initChart()
      }
      if (selectedElderId.value) fetchDashboard(selectedElderId.value)
      chartInstance?.resize()
    })
  }
  if (tab === 'booking') {
    loadActiveBookings(selectedElderId.value)
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

      <main :class="['family-scroll', { 'booking-mode': activeTab === 'booking', 'home-mode': activeTab === 'home' }]">
        <template v-if="activeTab === 'home'">
          <div class="home-fixed-top">
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
            <div class="block">
              <div class="block-title">用药要求</div>
              <p class="sub">当前老人：{{ customerList.find((c) => c.elderId === selectedElderId)?.name || '请先绑定并选择' }}</p>
              <div class="medication-panel">{{ selectedElderMedication }}</div>
            </div>
          </div>

          <div class="block home-lower-panel">
            <div class="home-lower-tabs">
              <button
                type="button"
                :class="['home-lower-tab', { active: homeLowerTab === 'diagnosis' }]"
                @click="homeLowerTab = 'diagnosis'"
              >
                诊断
              </button>
              <button
                type="button"
                :class="['home-lower-tab', { active: homeLowerTab === 'appointments' }]"
                @click="homeLowerTab = 'appointments'"
              >
                预约
              </button>
            </div>

            <div v-if="homeLowerTab === 'diagnosis'" class="home-lower-content diagnosis-list">
              <template v-if="selectedElderDiagnoses.length > 0">
                <div v-for="row in selectedElderDiagnoses" :key="row.diagnosisId" class="diagnosis-item">
                  <div class="diagnosis-top">
                    <span class="diagnosis-type">{{ row.type }}</span>
                    <span class="diagnosis-time">{{ row.time }}</span>
                  </div>
                  <div class="diagnosis-note">{{ row.note || '无补充说明' }}</div>
                  <div class="diagnosis-doctor">诊断医生：{{ row.doctorName || '系统记录' }}</div>
                </div>
              </template>
              <el-empty v-else description="暂无就诊诊断结果" :image-size="52" />
            </div>

            <div v-else v-loading="loadingAppointments" class="home-lower-content appointment-list">
              <template v-if="familyAppointments.length > 0">
                <div
                  v-for="item in familyAppointments"
                  :key="item.appointId || item.id"
                  class="appointment-row"
                >
                  <div class="appointment-main">
                    <div class="appointment-doctor">{{ item.doctorName || '未知医生' }}</div>
                    <div class="appointment-time">{{ item.createTime || '--' }}</div>
                  </div>
                  <div :class="['appointment-status', `status-${item.status}`]">{{ item.status || '未知' }}</div>
                </div>
              </template>
              <el-empty v-else description="暂无预约记录" :image-size="52" />
            </div>
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
          <div class="block booking-block">
            <div class="block-title">帮老人预约</div>
            <p class="sub">当前老人：{{ customerList.find((c) => c.elderId === selectedElderId)?.name || '请先绑定并选择' }}</p>
            <div v-loading="loadingDoctors" class="doctor-list">
              <div v-for="doc in bookingDoctorsView" :key="doc.userId" class="doc-row">
                <span class="doc-name">{{ doc.realName }}</span>
                <button type="button" class="mini-btn" :disabled="doc.hasActiveBooking" @click="openFamilyBookDialog(doc)">
                  {{ doc.hasActiveBooking ? '已预约' : '预约' }}
                </button>
              </div>
              <el-empty v-if="!loadingDoctors && doctorsForBooking.length === 0" description="暂无医生数据" />
            </div>
          </div>
        </template>
      </main>

      <nav class="family-bottom" aria-label="底部功能切换">
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
  align-items: center;
  background: linear-gradient(180deg, #e9eff7 0%, #dce6f1 100%);
  padding: 20px 12px;
}

.family-page {
  width: 100%;
  max-width: 400px;
  height: min(770px, calc(100vh - 40px));
  background: linear-gradient(180deg, #f6f9fe 0%, #e8eef6 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d0dce8;
  border-radius: 0;
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

.family-scroll.booking-mode,
.family-scroll.home-mode {
  display: flex;
  flex-direction: column;
}

.home-fixed-top {
  flex-shrink: 0;
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
  height: 290px;
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
  overflow-y: auto;
  max-height: calc(100vh - 320px);
  padding-right: 2px;
}

.appointment-list {
  min-height: 60px;
  height: auto;
  max-height: none;
  overflow: visible;
  padding-right: 0;
}

.medication-panel {
  min-height: 52px;
  border: 1px dashed #c6dbef;
  border-radius: 10px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #eff6fe 100%);
  color: #1e3a5f;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.diagnosis-list {
  min-height: 60px;
  height: auto;
  max-height: none;
  overflow: visible;
  padding-right: 0;
}

.diagnosis-item {
  padding: 10px 0;
  border-bottom: 1px solid #eef3f8;
}

.diagnosis-item:last-child {
  border-bottom: none;
}

.diagnosis-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.diagnosis-type {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.diagnosis-time {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

.diagnosis-note {
  margin-top: 4px;
  font-size: 13px;
  color: #334155;
  line-height: 1.45;
  word-break: break-word;
}

.diagnosis-doctor {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.appointment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.appointment-row:last-child {
  border-bottom: none;
}

.appointment-main {
  min-width: 0;
}

.appointment-doctor {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.appointment-time {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.appointment-status {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.appointment-status.status-待确认 {
  color: #b45309;
  background: #fff7ed;
}

.appointment-status.status-已确认 {
  color: #166534;
  background: #f0fdf4;
}

.appointment-status.status-已取消 {
  color: #b91c1c;
  background: #fef2f2;
}

.booking-block {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.family-scroll.home-mode .block {
  flex-shrink: 0;
}

.home-lower-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.home-lower-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.home-lower-tab {
  border: none;
  border-radius: 10px;
  background: #eaf2fb;
  color: #48627f;
  font-size: 13px;
  font-weight: 700;
  height: 34px;
  cursor: pointer;
}

.home-lower-tab.active {
  background: linear-gradient(90deg, #2185e8 0%, #3aa0ff 100%);
  color: #fff;
}

.home-lower-content {
  flex: 1;
  min-height: 0;
  max-height: 220px;
  overflow-y: scroll;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding-right: 2px;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(80, 120, 170, 0.45) rgba(180, 200, 220, 0.2);
}

.home-lower-content::-webkit-scrollbar {
  width: 6px;
}

.home-lower-content::-webkit-scrollbar-thumb {
  background: rgba(80, 120, 170, 0.45);
  border-radius: 6px;
}

.home-lower-content::-webkit-scrollbar-track {
  background: rgba(180, 200, 220, 0.2);
  border-radius: 6px;
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

.mini-btn:disabled {
  background: linear-gradient(90deg, #b2bac2 0%, #9ca5af 100%);
  color: #f0f0f0;
  cursor: not-allowed;
}

.family-bottom {
  border-top: 1px solid #d6e2ef;
  background: #fdfefe;
  padding: 6px 8px 10px;
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
