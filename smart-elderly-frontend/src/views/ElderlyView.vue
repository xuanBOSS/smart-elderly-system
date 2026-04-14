<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, User, Star, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('home')
const userInfo = ref(null)
const health = ref(null)
const checkinStatus = ref('未完成')
const doctors = ref([])
const loadingDoctors = ref(false)
const keyword = ref('')
const pageLoading = ref(true)
const showRecordDrawer = ref(false)
const appointmentRecords = ref([])
const hugeFontMode = ref(localStorage.getItem('elderlyHugeFont') === '1')
const showVoicePanel = ref(false)
const voiceText = ref('')
const doctorsLoaded = ref(false)

const weekMap = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const currentDate = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`

const displayName = computed(() => {
  return userInfo.value?.realName || '长者'
})

const vitalStatus = computed(() => {
  if (!health.value) return '暂无数据'
  const high = Number(health.value.bloodPressureHigh)
  const low = Number(health.value.bloodPressureLow)
  const heartRate = Number(health.value.heartRate)
  const sugar = Number(health.value.bloodSugar)
  const stable = high <= 140 && low <= 90 && heartRate >= 55 && heartRate <= 100 && sugar <= 7.0
  return stable ? '稳定' : '需关注'
})

const bloodPressureText = computed(() => {
  if (!health.value) return '--/--'
  const high = health.value.bloodPressureHigh ?? '--'
  const low = health.value.bloodPressureLow ?? '--'
  return `${high}/${low}`
})

const bloodPressureLevel = computed(() => {
  if (!health.value) return '暂无数据'
  const high = Number(health.value.bloodPressureHigh)
  const low = Number(health.value.bloodPressureLow)
  return high <= 140 && low <= 90 ? '正常' : '需关注'
})

const heartRateLevel = computed(() => {
  if (!health.value) return '暂无数据'
  const heartRate = Number(health.value.heartRate)
  return heartRate >= 55 && heartRate <= 100 ? '平稳' : '偏离正常'
})

const bloodSugarLevel = computed(() => {
  if (!health.value) return '暂无数据'
  const sugar = Number(health.value.bloodSugar)
  return sugar <= 7.0 ? '正常' : '偏高'
})

const voiceBroadcastText = computed(() => {
  const name = displayName.value || '长者'
  const bp = bloodPressureText.value
  const hr = health.value?.heartRate || '--'
  const sugar = health.value?.bloodSugar || '--'
  return `您好，${name}。当前血压 ${bp}，心率 ${hr}，血糖 ${sugar}。请按时服药，注意休息。`
})

const doctorCards = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return doctors.value
  return doctors.value.filter(item => item.realName?.toLowerCase().includes(text))
})

const activeAppointmentDoctorIds = computed(() => {
  return new Set(
    appointmentRecords.value
      .filter(item => item.status === '待确认' || item.status === '已确认')
      .map(item => item.doctorId)
  )
})

const goLogin = () => router.push('/login')

const toggleHugeFontMode = () => {
  hugeFontMode.value = !hugeFontMode.value
  localStorage.setItem('elderlyHugeFont', hugeFontMode.value ? '1' : '0')
}

const withFriendlyError = (error, fallbackText) => {
  if (!error.response) return '网络异常，请检查网络后重试'
  return error.response?.data?.message || error.message || fallbackText
}

const getCurrentUserId = () => {
  try {
    const cached = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return cached.userId
  } catch {
    return null
  }
}

const loadAppointmentRecords = () => {
  try {
    const userId = getCurrentUserId()
    if (!userId) return
    const all = JSON.parse(localStorage.getItem('elderlyAppointmentRecords') || '[]')
    appointmentRecords.value = all.filter(item => Number(item.userId) === Number(userId))
  } catch {
    appointmentRecords.value = []
  }
}

const saveAppointmentRecords = () => {
  const userId = getCurrentUserId()
  if (!userId) return
  let all = []
  try {
    all = JSON.parse(localStorage.getItem('elderlyAppointmentRecords') || '[]')
  } catch {
    all = []
  }
  const remained = all.filter(item => Number(item.userId) !== Number(userId))
  localStorage.setItem('elderlyAppointmentRecords', JSON.stringify([...remained, ...appointmentRecords.value]))
}

const sendEmergency = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('请先登录后再操作')
    return
  }
  try {
    const { data } = await request.post('/api/elderly/emergency', { userId })
    if (data.code !== 200) {
      ElMessage.error(data.message || '报警失败，请稍后重试')
      return
    }
    ElMessage.success(`报警成功，工单号 ${data.data.helpId}`)
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '报警失败，请稍后重试'))
  }
}

const loadUserInfo = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const { data } = await request.get(`/api/user/${userId}`)
    if (data.code !== 200) {
      ElMessage.error(data.message || '用户信息加载失败')
      return
    }
    userInfo.value = data.data
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '用户信息加载失败'))
  }
}

const loadHealth = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const { data } = await request.get('/api/health/dashboard', { params: { elderId: userId } })
    if (data.code !== 200) {
      ElMessage.error(data.message || '健康档案加载失败')
      return
    }
    const latest = data.data?.latest || {}
    const pressure = String(latest.bloodPressure || '--/--').split('/')
    health.value = {
      bloodPressureHigh: pressure[0],
      bloodPressureLow: pressure[1],
      heartRate: latest.heartRate,
      bloodSugar: latest.bloodSugar
    }
    checkinStatus.value = health.value?.heartRate && health.value?.bloodPressureHigh ? '已完成' : '未完成'
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '健康档案加载失败'))
  }
}

const loadDoctors = async () => {
  if (doctorsLoaded.value || loadingDoctors.value) return
  loadingDoctors.value = true
  try {
    const list = []
    // 目前测试库医生账号集中在 1001-1010，先按需拉取这个区间避免首屏卡顿
    for (let id = 1001; id <= 1010; id += 1) {
      const { data } = await request.get(`/api/user/${id}`)
      if (data.code === 200 && data.data && Number(data.data.role) === 2) {
        list.push(data.data)
      }
    }
    doctors.value = list
    doctorsLoaded.value = true
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '医生列表加载失败'))
  } finally {
    loadingDoctors.value = false
  }
}

const bookNow = (doctor) => {
  if (activeAppointmentDoctorIds.value.has(doctor.userId)) {
    ElMessage.warning('该医生已有进行中的预约，请勿重复提交')
    return
  }
  appointmentRecords.value.unshift({
    id: `${Date.now()}-${doctor.userId}`,
    userId: getCurrentUserId(),
    doctorId: doctor.userId,
    doctorName: doctor.realName,
    department: '老年医学门诊',
    status: '待确认',
    createTime: new Date().toLocaleString('zh-CN', { hour12: false })
  })
  saveAppointmentRecords()
  ElMessage.success(`已提交给 ${doctor.realName} 的预约申请`)
}

const openAppointmentRecords = () => {
  loadAppointmentRecords()
  showRecordDrawer.value = true
}

const openVoiceBooking = () => {
  showVoicePanel.value = true
  voiceText.value = '我要预约明天上午门诊'
}

const submitVoiceBooking = () => {
  if (!voiceText.value.trim()) {
    ElMessage.warning('请先输入或录入预约内容')
    return
  }
  ElMessage.success('语音预约内容已提交（演示 UI）')
  showVoicePanel.value = false
}

const startVoiceBroadcast = () => {
  ElMessage.success('开始语音播报（演示 UI）')
}

onMounted(async () => {
  try {
    // 首屏只加载首页必需数据，确保进入速度
    await Promise.all([loadUserInfo(), loadHealth()])
    loadAppointmentRecords()
  } finally {
    pageLoading.value = false
  }
})

watch(activeTab, (tab) => {
  if (tab === 'mine') {
    loadDoctors()
  }
})
</script>

<template>
  <div class="elderly-shell">
    <div :class="['elderly-page', { 'font-xxl': hugeFontMode }]">
      <header class="header">
        <p class="date">{{ currentDate }}</p>
        <button class="font-toggle" @click="toggleHugeFontMode">{{ hugeFontMode ? '标准' : '大字' }}</button>
        <el-button class="exit" link @click="goLogin">退出</el-button>
      </header>

      <main v-if="pageLoading" class="content">
        <el-skeleton animated :rows="8" />
      </main>

      <main
        v-else
        :class="['content', { 'content-no-scroll': activeTab === 'home' && !hugeFontMode }]"
      >
        <section v-if="activeTab === 'home'" class="panel">
          <button class="sos-button" @click="sendEmergency">SOS</button>
          <p class="welcome">欢迎您，{{ displayName }}</p>

          <div class="status-card">
            <span class="status-dot done">✓</span>
            <span class="status-text">每日打卡：{{ checkinStatus }}</span>
          </div>
          <div class="status-card">
            <span class="status-dot">♡</span>
            <span class="status-text">生命体征：{{ vitalStatus }}</span>
          </div>

          <div class="voice-action-row">
            <button class="voice-action-btn" @click="openVoiceBooking">语音预约</button>
            <button class="voice-action-btn secondary" @click="startVoiceBroadcast">语音播报</button>
          </div>
        </section>

        <section v-else-if="activeTab === 'health'" class="panel">
          <h2 class="title">健康</h2>
          <div v-if="health" class="health-stack">
            <div class="metric-card bp">
              <div class="metric-title">血压</div>
              <div class="metric-value">{{ bloodPressureText }}</div>
              <div class="metric-unit">mmHg</div>
              <div class="metric-level">{{ bloodPressureLevel }}</div>
            </div>
            <div class="metric-card hr">
              <div class="metric-title">心率</div>
              <div class="metric-value">{{ health.heartRate || '--' }}</div>
              <div class="metric-unit">BPM</div>
              <div class="metric-level">{{ heartRateLevel }}</div>
            </div>
            <div class="metric-card bs">
              <div class="metric-title">血糖</div>
              <div class="metric-value">{{ health.bloodSugar || '--' }}</div>
              <div class="metric-unit">mmol/L</div>
              <div class="metric-level">{{ bloodSugarLevel }}</div>
            </div>
          </div>
          <el-empty v-else description="暂时没有健康数据" />
        </section>

        <section v-else class="panel">
          <div class="mine-head">
            <h2 class="title">预约挂号</h2>
            <button class="record-entry" @click="openAppointmentRecords">我的预约记录</button>
          </div>
          <div class="search-box">
            <Search class="search-icon" />
            <input v-model="keyword" class="search-input" placeholder="搜索医生..." />
          </div>
          <div v-loading="loadingDoctors" class="doctor-list">
            <div v-for="item in doctorCards" :key="item.userId" class="doctor-card">
              <div class="avatar">{{ String(item.realName || '医').slice(0, 1) }}</div>
              <div class="doctor-info">
                <div class="doctor-name">{{ item.realName }}</div>
                <div class="doctor-dept">老年医学门诊</div>
                <div class="doctor-state">今日可预约</div>
              </div>
              <button :disabled="activeAppointmentDoctorIds.has(item.userId)" class="book-btn" @click="bookNow(item)">
                <span class="book-top">{{ activeAppointmentDoctorIds.has(item.userId) ? '已预约' : '预约' }}</span>
                <span class="book-bottom">{{ activeAppointmentDoctorIds.has(item.userId) ? '处理中' : '挂号' }}</span>
              </button>
            </div>
            <el-empty v-if="!loadingDoctors && doctorCards.length === 0" description="暂时没有匹配医生" />
          </div>
        </section>
      </main>

      <footer class="nav">
        <button :class="['nav-item', activeTab === 'home' && 'active']" @click="activeTab = 'home'">
          <House class="nav-icon" />
          <span class="nav-text">首页</span>
        </button>
        <button :class="['nav-item', activeTab === 'health' && 'active']" @click="activeTab = 'health'">
          <Star class="nav-icon" />
          <span class="nav-text">健康</span>
        </button>
        <button :class="['nav-item', activeTab === 'mine' && 'active']" @click="activeTab = 'mine'">
          <User class="nav-icon" />
          <span class="nav-text">我的</span>
        </button>
      </footer>

      <div v-if="showRecordDrawer" class="record-overlay" @click.self="showRecordDrawer = false">
        <div class="record-panel">
          <div class="record-header">
            <div class="record-title">我的预约记录</div>
            <button class="record-close" @click="showRecordDrawer = false">关闭</button>
          </div>
          <div class="record-list">
            <div v-for="item in appointmentRecords" :key="item.id" class="record-item">
              <div class="record-name">{{ item.doctorName }}</div>
              <div class="record-meta">{{ item.department }} · {{ item.createTime }}</div>
              <div :class="['record-status', `status-${item.status}`]">{{ item.status }}</div>
            </div>
            <el-empty v-if="appointmentRecords.length === 0" description="暂时没有预约记录" />
          </div>
        </div>
      </div>

      <div v-if="showVoicePanel" class="record-overlay" @click.self="showVoicePanel = false">
        <div class="record-panel">
          <div class="record-header">
            <div class="record-title">语音预约</div>
            <button class="record-close" @click="showVoicePanel = false">关闭</button>
          </div>
          <div class="voice-box">
            <div class="voice-tip">模拟语音识别文本（可编辑）</div>
            <textarea v-model="voiceText" class="voice-input" />
            <button class="record-entry voice-submit" @click="submitVoiceBooking">确认提交</button>
            <div class="voice-tip">语音播报示例：{{ voiceBroadcastText }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.elderly-shell {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(circle at 18% 12%, rgba(104, 162, 255, 0.18), transparent 34%),
    radial-gradient(circle at 88% 86%, rgba(66, 197, 160, 0.16), transparent 28%),
    linear-gradient(180deg, #e9eff7 0%, #dce6f1 100%);
  padding: 20px 12px;
}

.elderly-page {
  --font-multiplier: 1;
  --app-font:
    "PingFang SC",
    "HarmonyOS Sans SC",
    "Microsoft YaHei UI",
    "Noto Sans SC",
    "Source Han Sans SC",
    "Segoe UI",
    sans-serif;
  width: 100%;
  max-width: 400px;
  height: min(770px, calc(100vh - 40px));
  background: linear-gradient(180deg, #eef3fa 0%, #e4ebf4 100%);
  border-radius: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  font-family: var(--app-font);
  letter-spacing: 0.2px;
  text-rendering: optimizeLegibility;
}

.elderly-page button,
.elderly-page input {
  font-family: inherit;
}

.elderly-page.font-xxl {
  --font-multiplier: 1.2;
}

.header {
  color: #203146;
  padding: 14px 20px;
  position: relative;
  background: linear-gradient(90deg, #f6f9fe 0%, #edf3fb 100%);
  border-bottom: 1px solid #dbe5f1;
}

.date {
  margin: 0;
  font-size: calc(24px * var(--font-multiplier));
  font-weight: 700;
}

.font-toggle {
  position: absolute;
  right: 72px;
  top: 12px;
  border: none;
  background: linear-gradient(90deg, #2389df 0%, #44a9ff 100%);
  color: #fff;
  border-radius: 8px;
  height: 28px;
  min-width: 48px;
  padding: 0 8px;
  font-size: calc(12px * var(--font-multiplier));
  font-weight: 700;
}

.exit {
  position: absolute;
  right: 16px;
  top: 16px;
  color: #202020;
  font-size: calc(22px * var(--font-multiplier));
}

.content {
  flex: 1;
  min-height: 0;
  padding: 10px 16px 12px;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.content.content-no-scroll {
  overflow-y: hidden;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: calc(36px * var(--font-multiplier));
  color: #162a42;
}

.desc {
  margin: 0;
  font-size: calc(24px * var(--font-multiplier));
  color: #333;
}

.sos-button {
  width: 100%;
  min-height: 250px;
  border: 3px solid #a70808;
  border-radius: 24px;
  background: linear-gradient(180deg, #ff0b15 0%, #ef0008 90%);
  color: #fff;
  font-size: calc(88px * var(--font-multiplier));
  font-weight: 900;
  box-shadow: inset 0 0 0 8px rgba(255, 255, 255, 0.14), 0 10px 22px rgba(123, 18, 18, 0.35);
}

.sos-button:active {
  transform: translateY(2px) scale(0.995);
}

.welcome,
.today {
  margin: 0;
  color: #1d3149;
  font-weight: 800;
}

.welcome {
  font-size: calc(34px * var(--font-multiplier));
  line-height: 1.2;
  white-space: nowrap;
}

.status-card {
  background: linear-gradient(180deg, #ffffff 0%, #f5f8fc 100%);
  border-radius: 12px;
  min-height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border: 1px solid #dce8f5;
  box-shadow: 0 4px 12px rgba(35, 78, 130, 0.08);
}

.status-dot {
  width: 28px;
  height: 28px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f9fcff;
  color: #1f2a37;
  font-size: 18px;
  border: 2px solid #36506d;
}

.status-dot.done {
  background: #39a854;
  border: none;
  color: #fff;
}

.status-text {
  font-size: calc(34px * var(--font-multiplier));
  font-weight: 800;
  color: #1f3147;
}

.health-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.health-card {
  background: #f4f7fb;
  border: 3px solid #1e293b;
  border-radius: 14px;
  padding: 14px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.health-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-card {
  border-radius: 16px;
  padding: 16px 18px;
  border: 1px solid rgba(116, 150, 189, 0.25);
  box-shadow: 0 6px 16px rgba(44, 81, 120, 0.1);
}

.metric-title {
  font-size: calc(28px * var(--font-multiplier));
  font-weight: 800;
  color: #1b2e46;
}

.metric-value {
  margin-top: 8px;
  font-size: calc(54px * var(--font-multiplier));
  font-weight: 900;
  color: #162a41;
  line-height: 1.05;
}

.metric-unit {
  margin-top: 2px;
  font-size: calc(24px * var(--font-multiplier));
  font-weight: 700;
  color: #2c445f;
}

.metric-level {
  margin-top: 6px;
  font-size: calc(28px * var(--font-multiplier));
  font-weight: 800;
  color: #2f9e44;
}

.mine-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.record-entry {
  border: none;
  background: linear-gradient(90deg, #2389df 0%, #44a9ff 100%);
  color: #fff;
  border-radius: 10px;
  height: 36px;
  padding: 0 12px;
  font-size: calc(14px * var(--font-multiplier));
  font-weight: 700;
}

.metric-card.bp {
  background: linear-gradient(135deg, #dfefff 0%, #cfe3f8 100%);
}

.metric-card.hr {
  background: linear-gradient(135deg, #def7e7 0%, #cbefd9 100%);
}

.metric-card.bs {
  background: linear-gradient(135deg, #fff4c8 0%, #fae7a8 100%);
}

.search-box {
  height: 52px;
  border: 1px solid #c5d9ee;
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfdff 0%, #f2f7fc 100%);
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 8px;
}

.search-icon {
  width: 20px;
  height: 20px;
  color: #46566b;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: calc(22px * var(--font-multiplier));
  color: #111;
}

.doctor-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 4px;
}

.doctor-card {
  border: 1px solid #c8dbef;
  border-radius: 14px;
  padding: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f9ff 100%);
  display: grid;
  grid-template-columns: 72px 1fr 76px;
  gap: 10px;
  align-items: center;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  background: linear-gradient(180deg, #e6f0fb 0%, #d4e3f2 100%);
  color: #2a3f56;
  font-size: 30px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doctor-name {
  font-size: calc(20px * var(--font-multiplier));
  font-weight: 800;
  color: #182b41;
  line-height: 1.25;
}

.doctor-dept,
.doctor-state {
  margin-top: 2px;
  font-size: calc(15px * var(--font-multiplier));
  color: #4a5c70;
}

.book-btn {
  height: 72px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #13c42b 0%, #08a51d 100%);
  color: #fff;
  font-size: calc(16px * var(--font-multiplier));
  font-weight: 800;
  line-height: 1.1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.book-btn:disabled {
  background: linear-gradient(180deg, #b2bac2 0%, #9ca5af 100%);
  color: #f0f0f0;
}

.book-top,
.book-bottom {
  display: block;
}

.nav {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: linear-gradient(180deg, #fdfefe 0%, #f3f7fb 100%);
  border-top: 1px solid #d6e2ef;
}

.nav-item {
  height: 110px;
  border: none;
  background: transparent;
  font-size: 32px;
  font-weight: 800;
  color: #202020;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.nav-icon {
  width: 36px;
  height: 36px;
}

.nav-text {
  font-size: calc(28px * var(--font-multiplier));
}

.nav-item.active {
  color: #198ec8;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  max-height: 55vh;
}

.record-item {
  border: 1px solid #d4e1ee;
  border-radius: 12px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f5f9fd 100%);
}

.record-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.record-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.record-status {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 700;
}

.status-待确认 {
  color: #d97706;
}

.status-已确认 {
  color: #16a34a;
}

.status-已取消 {
  color: #dc2626;
}

.voice-action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.voice-action-btn {
  height: 52px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #2389df 0%, #43a8ff 100%);
  color: #fff;
  font-size: calc(18px * var(--font-multiplier));
  font-weight: 800;
}

.voice-action-btn.secondary {
  background: linear-gradient(90deg, #00a870 0%, #20bb86 100%);
}

.voice-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.voice-tip {
  font-size: 13px;
  color: #475569;
  line-height: 1.4;
}

.voice-input {
  width: 100%;
  min-height: 96px;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 10px;
  font-size: 15px;
  font-family: inherit;
  resize: vertical;
}

.voice-submit {
  width: 100%;
}

.record-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: flex-end;
  z-index: 30;
}

.record-panel {
  width: 100%;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 12px;
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.record-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.record-close {
  border: none;
  background: #e5edf5;
  color: #1f2937;
  border-radius: 8px;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 700;
}
</style>
