<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { House, User, Star, Search, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const sosConfirmEnabled = ref(localStorage.getItem('elderlySosConfirm') !== '0')
const sosVibrateEnabled = ref(localStorage.getItem('elderlySosVibrate') !== '0')
const sosGeoWarnEnabled = ref(localStorage.getItem('elderlySosGeoWarn') !== '0')
const appointmentAutoRefreshEnabled = ref(localStorage.getItem('elderlyAppointmentAutoRefresh') !== '0')
const sosConfirmPending = ref(false)
let sosConfirmTimer = null
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
      .filter(item => (item.status === '待确认' || item.status === '已确认') && item?.appointId)
      .map(item => String(item.doctorId))
  )
})

const todaySchedules = ref([])

const doctorTodayAvailableIds = computed(() => {
  return new Set(
    todaySchedules.value
      .filter(s => Number(s.remainCount) > 0)
      .map(s => String(s.doctorId))
  )
})

const goLogin = () => router.push('/login')

const toggleHugeFontMode = () => {
  hugeFontMode.value = !hugeFontMode.value
  localStorage.setItem('elderlyHugeFont', hugeFontMode.value ? '1' : '0')
}

const setHugeFontMode = (large) => {
  hugeFontMode.value = !!large
  localStorage.setItem('elderlyHugeFont', hugeFontMode.value ? '1' : '0')
}

watch(sosConfirmEnabled, (val) => {
  localStorage.setItem('elderlySosConfirm', val ? '1' : '0')
  if (!val) {
    sosConfirmPending.value = false
    if (sosConfirmTimer) clearTimeout(sosConfirmTimer)
    sosConfirmTimer = null
  }
})

watch(sosVibrateEnabled, (val) => {
  localStorage.setItem('elderlySosVibrate', val ? '1' : '0')
})

watch(sosGeoWarnEnabled, (val) => {
  localStorage.setItem('elderlySosGeoWarn', val ? '1' : '0')
})

watch(appointmentAutoRefreshEnabled, (val) => {
  localStorage.setItem('elderlyAppointmentAutoRefresh', val ? '1' : '0')
})

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

const loadAppointmentRecords = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    appointmentRecords.value = []
    return
  }

  try {
    const { data } = await request.get('/api/elderly/appointments', { params: { userId } })
    if (data.code === 200) {
      appointmentRecords.value = data.data || []
    } else {
      appointmentRecords.value = []
    }
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

const formatAppointTime = (appointTime) => {
  if (!appointTime) return new Date().toLocaleString('zh-CN', { hour12: false })
  const d = new Date(appointTime)
  if (Number.isNaN(d.getTime())) return String(appointTime)
  return d.toLocaleString('zh-CN', { hour12: false })
}

const getEmergencyLocation = () => {
  if (!navigator.geolocation) {
    return Promise.resolve({ text: '', latitude: null, longitude: null })
  }

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords
        const text = `定位坐标(${latitude.toFixed(6)}, ${longitude.toFixed(6)})，精度约${Math.round(accuracy)}米`
        resolve({ text, latitude, longitude })
      },
      () => resolve({ text: '', latitude: null, longitude: null }),
      {
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 60000
      }
    )
  })
}

const sendEmergency = async () => {
  if (!sosConfirmEnabled.value) {
    return sendEmergencyReal()
  }

  // 二次确认：点击一次->提示->6秒内再点一次才真正发起报警
  if (sosConfirmPending.value) {
    sosConfirmPending.value = false
    if (sosConfirmTimer) clearTimeout(sosConfirmTimer)
    sosConfirmTimer = null
    return sendEmergencyReal()
  }

  sosConfirmPending.value = true
  ElMessage.warning('已收到 SOS 触发：请在 6 秒内再次点击以确认报警')
  if (sosConfirmTimer) clearTimeout(sosConfirmTimer)
  sosConfirmTimer = setTimeout(() => {
    sosConfirmPending.value = false
    sosConfirmTimer = null
  }, 6000)
}

const sendEmergencyReal = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('请先登录后再操作')
    return
  }

  try {
    // SOS震动提示（演示）
    if (sosVibrateEnabled.value && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(200)
    }

    const geo = await getEmergencyLocation()
    const { data } = await request.post('/api/elderly/emergency', {
      userId,
      latitude: geo.latitude ?? undefined,
      longitude: geo.longitude ?? undefined
    })
    if (data.code !== 200) {
      ElMessage.error(data.message || '报警失败，请稍后重试')
      return
    }
    const helpId = data.data?.helpId // 拿到后端刚刚生成的那条救命工单的 ID
    ElMessage.success('SOS 信号已发出！社区大屏已亮起红灯！')

    speak("救援已呼叫。能听到吗？请告诉我您遇到了什么情况？", () => {
      // 只有等系统这句话彻底读完闭嘴了，才打开麦克风！
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
      if (!SpeechRecognition) return

      const recognition = new SpeechRecognition()
      recognition.lang = 'zh-CN'
      recognition.continuous = false
      recognition.interimResults = false

      recognition.onstart = () => {
        ElMessage({ message: '🎙️ 正在倾听您的情况...', type: 'warning', duration: 8000 })
      }

      recognition.onresult = async (event) => {
        const transcript = event.results[0][0].transcript
        console.log("老人补充的求救情况：", transcript)
        
        // 🌟 动作 3：把老人补充的伤情，更新到那条报警记录里！
        await request.post('/api/elderly/emergency/updateDesc', {
          helpId: helpId,
          desc: transcript
        })
        speak("收到，已经把您的具体情况同步给社区救援人员。")
      }

      recognition.onerror = () => {
        // 如果老人疼得说不出话，或者周围太吵没识别出来
        // 不做任何报错，因为救命的第一步早就发出了！
        speak("请您保持体力，救援人员马上就到。")
      }

      recognition.start()
    })

  } catch (error) {
    ElMessage.error(withFriendlyError(error, '报警失败，请检查网络'))
    speak("网络异常，请立刻拨打 1 2 0。")
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
    const { data } = await request.get('/api/elderly/health', { params: { userId } })
    if (data.code !== 200) {
      health.value = null
      checkinStatus.value = '未完成'
      ElMessage.error(data.message || '健康档案加载失败')
      return
    }
    health.value = {
      bloodPressureHigh: data.data?.bloodPressureHigh,
      bloodPressureLow: data.data?.bloodPressureLow,
      heartRate: data.data?.heartRate,
      bloodSugar: data.data?.bloodSugar
    }
    checkinStatus.value = health.value?.heartRate && health.value?.bloodPressureHigh ? '已完成' : '未完成'
  } catch (error) {
    health.value = null
    checkinStatus.value = '未完成'
    ElMessage.error(withFriendlyError(error, '健康档案加载失败'))
  }
}

const loadDoctors = async () => {
  if (doctorsLoaded.value || loadingDoctors.value) return
  loadingDoctors.value = true
  try {
    // 拉取当天各医生的号源情况（用于“今日可约/不可约”显示与禁用按钮）
    const { data: scheduleRes } = await request.post('/api/elderly/appointment', {})
    if (scheduleRes?.code === 200) {
      todaySchedules.value = scheduleRes.data?.schedules || []
    } else {
      todaySchedules.value = []
    }

    const userId = getCurrentUserId()
    if (!userId) {
      doctors.value = []
      return
    }

    // 获取所有医生（后端 role=2）
    const { data: doctorsRes } = await request.get('/api/elderly/doctors', { params: { userId } })
    if (doctorsRes?.code === 200) {
      doctors.value = Array.isArray(doctorsRes.data) ? doctorsRes.data : []
    } else {
      doctors.value = []
    }
    doctorsLoaded.value = true
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '医生列表加载失败'))
  } finally {
    loadingDoctors.value = false
  }
}

const bookNow = async (doctor) => {
  if (activeAppointmentDoctorIds.value.has(String(doctor.userId))) {
    ElMessage.warning('该医生已有进行中的预约，请勿重复提交')
    return
  }

  if (!doctorTodayAvailableIds.value.has(String(doctor.userId))) {
    ElMessage.warning('该医生今日号源不足，暂时无法预约')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('请先登录后再预约')
    return
  }

  try {
    const { data } = await request.post('/api/elderly/appointment', { userId, doctorId: doctor.userId })
    if (data.code !== 200) {
      ElMessage.error(data.message || '预约失败，请稍后重试')
      return
    }

    const appointId = data.data?.appointId
    const appointTime = data.data?.appointTime

    appointmentRecords.value.unshift({
      id: String(appointId),
      appointId,
      userId,
      doctorId: doctor.userId,
      doctorName: doctor.realName,
      status: '待确认',
      createTime: formatAppointTime(appointTime)
    })
    saveAppointmentRecords()
    // 以服务器数据为准，避免 localStorage 与后端状态不一致
    loadAppointmentRecords()
    ElMessage.success(`已提交给 ${doctor.realName} 的预约申请`)
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '预约失败，请稍后重试'))
  }
}

const openAppointmentRecords = () => {
  showRecordDrawer.value = true
  loadAppointmentRecords()
}

const canCancelAppointment = (item) => {
  return (item?.status === '待确认' || item?.status === '已确认') && item?.appointId
}

const cancelAppointment = async (item) => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.warning('请先登录后再操作')
    return
  }

  if (!item?.appointId) {
    ElMessage.warning('该预约无法取消（缺少预约ID）')
    return
  }

  try {
    const { data } = await request.post('/api/elderly/appointment/cancel', null, {
      params: { appointId: item.appointId, userId }
    })
    if (data.code !== 200) {
      ElMessage.error(data.message || '取消失败，请稍后重试')
      return
    }

    await loadAppointmentRecords()
    ElMessage.success('预约已取消')
  } catch (error) {
    ElMessage.error(withFriendlyError(error, '取消失败，请稍后重试'))
  }
}

// ================= 🌟 1. 语音播报 (TTS - 系统的嘴巴) =================
const speak = (text, onEndCallback = null) => {
  if (!('speechSynthesis' in window)) {
    ElMessage.error('当前浏览器不支持语音播报')
    if (onEndCallback) onEndCallback()
    return
  }
  window.speechSynthesis.cancel() // 停止正在播放的语音
  
  const msg = new SpeechSynthesisUtterance(text)
  msg.lang = 'zh-CN'
  msg.rate = 0.85
  msg.pitch = 1
  
  // 🌟 核心魔法：当系统彻底读完这句话时，触发回调！
  if (onEndCallback) {
    msg.onend = () => {
      onEndCallback()
    }
  }
  
  window.speechSynthesis.speak(msg)
}

// 替换原来的 startVoiceBroadcast
const startVoiceBroadcast = () => {
  ElMessage.success('正在为您播报...')
  speak(voiceBroadcastText.value) // 直接读出你写的完美文案！
}

// ================= 🌟 2. 语音录入 (ASR - 系统的耳朵) =================
const isListening = ref(false)

const processVoiceData = async (text) => {
  const userId = getCurrentUserId()
  if (!userId) {
    speak("请您先登录系统")
    return
  }

  try {
    // 调用我们刚刚写好的后端神仙接口
    const { data } = await request.post('/api/elderly/voice/intent', {
      userId: userId,
      text: text
    })

    if (data.code === 200) {
      // 1. 界面弹窗提示成功
      ElMessage.success(data.data)
      // 2. 让电脑把后端的成功提示（如"体征数据已自动记录..."）朗读出来！
      speak(data.data)
      
      // 3. 极度丝滑的细节：自动刷新页面数据
      if (data.data.includes('健康档案')) {
        loadHealth() // 刷新健康板块的数字
      } else if (data.data.includes('预约')) {
        loadAppointmentRecords() // 刷新预约记录列表
      }
    } else {
      speak("处理失败了，" + (data.message || "未知错误"))
    }
  } catch (error) {
    console.error(error)
    speak("抱歉，系统网络好像有点问题，请稍后再试。")
  }
}

const openVoiceBooking = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessageBox.alert('抱歉，您的浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器。', '提示')
    showVoicePanel.value = true
    voiceText.value = '我要预约明天上午门诊'
    return
  }

  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = false

  // 1. 刚点按钮，状态变更为准备中
  isListening.value = true
  
  // 2. 🌟 让系统先说话，并传入一个回调函数（说话结束后才执行）
  speak("我在听，请告诉我您的身体状况，血压多少，血糖多少，心率如何，或者您想预约哪天看病？", () => {
    
    // 3. 🌟 系统的嘴巴闭上了，现在立刻弹出提示，并正式打开麦克风！
    ElMessage({
      message: '🎙️ 现在请说话...',
      type: 'warning',
      duration: 5000
    })
    
    try {
      recognition.start() // 正式启动麦克风
    } catch (e) {
      console.error("麦克风启动异常", e)
      isListening.value = false
    }
  })

  // 麦克风开始录音时的事件
  recognition.onstart = () => {
    console.log("麦克风已真实开启")
  }

  // 监听到老人的话
  recognition.onresult = async (event) => {
    const transcript = event.results[0][0].transcript
    console.log("老人说的话：", transcript)
    
    ElMessageBox.confirm(`系统听到您说：<br/><b style="color:#409EFF;font-size:18px">“${transcript}”</b><br/><br/>正在为您智能处理...`, '语音识别成功', {
      dangerouslyUseHTMLString: true,
      showCancelButton: false,
      confirmButtonText: '好的'
    })
    
    // 发给后端 DeepSeek 处理
    await processVoiceData(transcript)
  }

  recognition.onerror = (event) => {
    console.error('语音识别错误', event.error)
    speak("抱歉，我没有听清，请您稍微大点声再说一遍。")
    isListening.value = false
  }

  recognition.onend = () => {
    isListening.value = false
  }
}

const submitVoiceBooking = () => {
  if (!voiceText.value.trim()) {
    ElMessage.warning('请先输入或录入内容')
    return
  }
  ElMessage.success('语音内容已提交（手动录入）')
  showVoicePanel.value = false
}

onMounted(async () => {
  try {
    await Promise.all([loadUserInfo(), loadHealth()])
    await loadAppointmentRecords()
  } finally {
    pageLoading.value = false
  }
})

watch(activeTab, (tab) => {
  if (tab === 'mine') {
    loadDoctors()
  }
})

let appointmentRefreshTimer = null
watch(showRecordDrawer, (open) => {
  if (appointmentRefreshTimer) {
    clearInterval(appointmentRefreshTimer)
    appointmentRefreshTimer = null
  }
  if (!open) return
  loadAppointmentRecords()
  if (!appointmentAutoRefreshEnabled.value) return
  appointmentRefreshTimer = setInterval(() => {
    loadAppointmentRecords()
  }, 3000)
})
</script>

<template>
  <div class="elderly-shell">
    <div :class="['elderly-page', { 'font-xxl': hugeFontMode }]">
      <header class="header">
        <p class="date">{{ currentDate }}</p>
        <el-button class="exit" link @click="goLogin">退出</el-button>
      </header>

      <main v-if="pageLoading" class="content">
        <el-skeleton animated :rows="8" />
      </main>

      <main
        v-else
        class="content"
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
            <button class="voice-action-btn" @click="openVoiceBooking">
              <svg class="voice-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M12 14a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3Zm7-3a1 1 0 1 0-2 0 5 5 0 0 1-10 0 1 1 0 1 0-2 0 7 7 0 0 0 6 6.93V21H9a1 1 0 1 0 0 2h6a1 1 0 1 0 0-2h-2v-3.07A7 7 0 0 0 19 11Z"
                />
              </svg>
              {{ isListening ? '正在聆听...' : '语音录入' }}
            </button>
            <button class="voice-action-btn secondary" @click="startVoiceBroadcast">
              <svg class="voice-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M3 10v4a1 1 0 0 0 1 1h4l5 4V5L8 9H4a1 1 0 0 0-1 1Zm13.5 2a4.5 4.5 0 0 0-2.2-3.87 1 1 0 1 0-.9 1.79A2.5 2.5 0 0 1 15.5 12c0 .98-.54 1.88-1.4 2.37a1 1 0 0 0 .9 1.79A4.5 4.5 0 0 0 16.5 12Zm2.5 0a7 7 0 0 0-3.6-6.1 1 1 0 1 0-.8 1.82A5 5 0 0 1 19 12a5 5 0 0 1-3 4.28 1 1 0 0 0 .8 1.82A7 7 0 0 0 19 12Z"
                />
              </svg>
              语音播报
            </button>
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

        <section v-else-if="activeTab === 'settings'" class="panel">
          <h2 class="title">设置</h2>

          <div class="settings-body">
            <div class="setting-row">
              <div class="setting-label">字体大小</div>
              <div class="setting-btn-row">
                <button class="setting-action-btn" :class="{ active: !hugeFontMode }" @click="setHugeFontMode(false)">
                  标准
                </button>
                <button class="setting-action-btn" :class="{ active: hugeFontMode }" @click="setHugeFontMode(true)">
                  大字
                </button>
              </div>
            </div>

            <div class="setting-row setting-switch-row">
              <span>SOS 防误触二次确认</span>
              <el-switch
                v-model="sosConfirmEnabled"
                :active-color="'#13c42b'"
                :inactive-color="'#cbd5e1'"
              />
            </div>

            <div class="setting-row setting-switch-row">
              <span>SOS 震动提示（演示）</span>
              <el-switch
                v-model="sosVibrateEnabled"
                :active-color="'#13c42b'"
                :inactive-color="'#cbd5e1'"
              />
            </div>

            <div class="setting-row setting-switch-row">
              <span>SOS 定位失败提示</span>
              <el-switch
                v-model="sosGeoWarnEnabled"
                :active-color="'#13c42b'"
                :inactive-color="'#cbd5e1'"
              />
            </div>

            <div class="setting-row setting-switch-row">
              <span>我的预约记录自动刷新</span>
              <el-switch
                v-model="appointmentAutoRefreshEnabled"
                :active-color="'#13c42b'"
                :inactive-color="'#cbd5e1'"
              />
            </div>

            <div class="setting-tip">小提示：开启“二次确认”后，点击一次 SOS 会先提示，6 秒内再点才会真正发起报警。</div>
          </div>
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
                <div class="doctor-state">
                  {{
                    activeAppointmentDoctorIds.has(String(item.userId))
                      ? '预约处理中'
                      : doctorTodayAvailableIds.has(String(item.userId))
                        ? '今日可预约'
                        : '今日不可约'
                  }}
                </div>
              </div>
              <button
                :disabled="
                  activeAppointmentDoctorIds.has(String(item.userId)) ||
                  !doctorTodayAvailableIds.has(String(item.userId))
                "
                class="book-btn"
                @click="bookNow(item)"
              >
                <span class="book-top">
                  {{ activeAppointmentDoctorIds.has(String(item.userId)) ? '已预约' : '预约' }}
                </span>
                <span class="book-bottom">
                  {{
                    activeAppointmentDoctorIds.has(String(item.userId))
                      ? '处理中'
                      : !doctorTodayAvailableIds.has(String(item.userId))
                        ? '今日满号/无排班'
                        : '挂号'
                  }}
                </span>
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
        <button :class="['nav-item', activeTab === 'settings' && 'active']" @click="activeTab = 'settings'">
          <Setting class="nav-icon" />
          <span class="nav-text">设置</span>
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
              <div class="record-meta">{{ item.createTime }}</div>
              <div :class="['record-status', `status-${item.status}`]">{{ item.status }}</div>
              <div class="record-actions">
                <button
                  v-if="canCancelAppointment(item)"
                  class="record-cancel-btn"
                  @click.stop="cancelAppointment(item)"
                >
                  取消
                </button>
              </div>
            </div>
            <el-empty v-if="appointmentRecords.length === 0" description="暂时没有预约记录" />
          </div>
        </div>
      </div>

      <div v-if="showVoicePanel" class="record-overlay" @click.self="showVoicePanel = false">
        <div class="record-panel">
          <div class="record-header">
            <div class="record-title">语音预约/录入</div>
            <button class="record-close" @click="showVoicePanel = false">关闭</button>
          </div>
          <div class="voice-box">
            <div class="voice-tip">您的浏览器暂不支持麦克风，请手动输入文字内容：</div>
            <textarea v-model="voiceText" class="voice-input" />
            <button class="record-entry voice-submit" @click="submitVoiceBooking">确认提交</button>
            <div class="voice-tip">播报示例：{{ voiceBroadcastText }}</div>
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
  --font-multiplier: 0.6;
  width: 100%;
  max-width: 400px;
  height: min(770px, calc(100vh - 40px));
  background: linear-gradient(180deg, #eef3fa 0%, #e4ebf4 100%);
  border-radius: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  font-family: inherit;
  letter-spacing: 0.18px;
  text-rendering: optimizeLegibility;
}

.elderly-page button,
.elderly-page input {
  font-family: inherit;
}

.elderly-page.font-xxl {
  --font-multiplier: 1.08;
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

.settings-toggle {
  position: absolute;
  right: 72px;
  top: 12px;
  border: none;
  background: linear-gradient(90deg, #198ec8 0%, #43a8ff 100%);
  color: #fff;
  border-radius: 8px;
  height: 28px;
  min-width: 56px;
  padding: 0 10px;
  font-size: calc(12px * var(--font-multiplier));
  font-weight: 800;
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
  grid-template-columns: repeat(4, 1fr);
  background: linear-gradient(180deg, #fdfefe 0%, #f3f7fb 100%);
  border-top: 1px solid #d6e2ef;
}

.nav-item {
  height: 95px;
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

.record-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.record-cancel-btn {
  border: none;
  background: linear-gradient(90deg, #ff4d4f 0%, #ff7875 100%);
  color: #fff;
  border-radius: 10px;
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 800;
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
  height: 60px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #2389df 0%, #43a8ff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: calc(24px * var(--font-multiplier));
  font-weight: 800;
}

.voice-action-btn.secondary {
  background: linear-gradient(90deg, #00a870 0%, #20bb86 100%);
}

.voice-icon {
  width: calc(34px * var(--font-multiplier));
  height: calc(34px * var(--font-multiplier));
  flex-shrink: 0;
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

.settings-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #111827;
  font-size: 14px;
}

.setting-checkbox-row {
  justify-content: flex-start;
  gap: 10px;
}

.setting-switch-row .el-switch {
  transform: scale(0.92);
  transform-origin: right center;
}

.setting-label {
  font-weight: 800;
}

.setting-btn-row {
  display: flex;
  gap: 10px;
}

.setting-action-btn {
  border: none;
  background: #e5edf5;
  color: #1f2937;
  border-radius: 10px;
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 800;
}

.setting-action-btn.active {
  background: linear-gradient(90deg, #2389df 0%, #43a8ff 100%);
  color: #fff;
}

.setting-tip {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}
</style>
