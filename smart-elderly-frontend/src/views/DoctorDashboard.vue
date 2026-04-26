<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request' // 🌟 引入咱们的网络大总管

const weekMap = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const currentDate = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`

const schedule = ref([
  { day: '周一', value: '' },
  { day: '周二', value: '' },
  { day: '周三', value: '' },
  { day: '周四', value: '' },
  { day: '周五', value: '' },
  { day: '周六', value: '' },
  { day: '周日', value: '' }
])

// 动态绑定的预约列表数据
const appointments = ref([])
const loading = ref(false) // 控制表格的加载动画

// 1. 获取待处理预约列表
const fetchAppointments = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/doctor/appointments/pending')
    if (res.data.code === 200) {
      appointments.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取预约列表失败')
    }
  } catch (error) {
    console.error('获取预约异常:', error)
    ElMessage.error('网络异常，无法获取预约数据')
  } finally {
    loading.value = false
  }
}

// 新增：获取我的排班，并自动填入顶部的 7 天网格中！
const fetchScheduleGrid = async () => {
  try {
    const res = await request.get('/api/doctor/schedule')
    if (res.data.code === 200) {
      const apiData = res.data.data // 后端返回的真实排班

      // 每次刷新都先清空
      schedule.value = schedule.value.map(item => ({ ...item, value: '' }))

      const parseBookedMax = (note) => {
        // note: 门诊接诊 (已约: 3 / 15，待确认: 1) 满
        const m = String(note || '').match(/已约:\s*(\d+)\s*\/\s*(\d+)/)
        if (!m) return null
        return { booked: Number(m[1]), max: Number(m[2]) }
      }

      const dayAgg = {} // day -> am/pm booked & max
      apiData.forEach(apiItem => {
        const day = apiItem.day
        const isAm = String(apiItem.time || '').includes('上午')
        if (!dayAgg[day]) {
          dayAgg[day] = { am: { has: false, booked: 0, max: 0 }, pm: { has: false, booked: 0, max: 0 } }
        }

        const parsed = parseBookedMax(apiItem.note)
        if (parsed) {
          if (isAm) {
            dayAgg[day].am = { has: true, booked: parsed.booked, max: parsed.max }
          } else {
            dayAgg[day].pm = { has: true, booked: parsed.booked, max: parsed.max }
          }
        } else {
          // 兜底：后端 note 解析失败时仍标记有排班
          if (isAm) dayAgg[day].am.has = true
          else dayAgg[day].pm.has = true
        }
      })

      // 把统计结果写回 7 天网格
      schedule.value.forEach(cell => {
        const info = dayAgg[cell.day]
        if (!info) return

        const hasAm = info.am.has
        const hasPm = info.pm.has

        if (hasAm && hasPm) {
          const bookedTotal = info.am.booked + info.pm.booked
          const maxTotal = info.am.max + info.pm.max
          cell.value = `全天 已约:${bookedTotal}/${maxTotal}`
        } else if (hasAm) {
          cell.value = info.am.max > 0
            ? `上午 已约:${info.am.booked}/${info.am.max}`
            : '上午 ✓'
        } else if (hasPm) {
          cell.value = info.pm.max > 0
            ? `下午 已约:${info.pm.booked}/${info.pm.max}`
            : '下午 ✓'
        }
      })
    }
  } catch (error) {
    console.error('获取排班异常', error)
  }
}

// 处理确认/拒绝
const handleAppointment = async (appointId, action) => {
  const actionText = action === 1 ? '确认' : '拒绝'
  try {
    await ElMessageBox.confirm(`您确定要${actionText}该患者的预约吗？`, '操作提示', {
      type: action === 1 ? 'success' : 'warning',
    })

    const res = await request.post(`/api/doctor/appointments/handle?appointId=${appointId}&action=${action}`)
    
    if (res.data.code === 200) {
      ElMessage.success(res.data.data)
      fetchAppointments() // 重新刷表格
      fetchScheduleGrid() // 🌟 如果确认了，排班进度也会变，顺手也刷一下顶部！
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (cancel) {}
}

// 页面加载时自动去拿数据
onMounted(() => {
  fetchAppointments()
  fetchScheduleGrid()
})
</script>

<template>
  <div class="doctor-dashboard">
    <div class="doctor-topbar">
      <div class="page-title">患者预约大厅</div>
      <div class="page-date">{{ currentDate }}</div> 
    </div>

    <section class="schedule-card">
      <div class="section-title">本周排班</div>
      <div class="schedule-grid">
        <div v-for="item in schedule" :key="item.day" class="schedule-cell">
          <div class="cell-day">{{ item.day }}</div>
          <div :class="['cell-value', { inactive: !item.value }]">
            {{ item.value || '未排班' }}
          </div>
        </div>
      </div>
    </section>

    <section class="appointment-card">
      <div class="section-title">
        待处理预约 <span class="appointment-count">（{{ appointments.length }}）</span>
      </div>
      <div class="table-wrap">
      <el-table :data="appointments" stripe border style="width: 100%;" v-loading="loading">
        <el-table-column prop="name" label="患者姓名" width="120" />
        <el-table-column prop="time" label="预约时间" width="180" />
        <el-table-column prop="type" label="预约类型" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleAppointment(row.appointId, 1)">确认</el-button>
            <el-button size="small" type="danger" plain @click="handleAppointment(row.appointId, 2)">拒绝</el-button>
          </template>
        </el-table-column>
        
        <template #empty>
          <el-empty description="太棒了，所有预约都已处理完毕！" />
        </template>
      </el-table>
      </div>
    </section>

  </div>
</template>

<style scoped>
.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.doctor-dashboard {
  min-height: 100vh;
  padding-bottom: 24px;
}

.doctor-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #203449;
}

.page-date {
  font-size: 14px;
  color: #6f859d;
}

.schedule-card,
.appointment-card {
  background: linear-gradient(180deg, #ffffff 0%, #f6f9fd 100%);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #dce7f3;
  box-shadow: 0 8px 20px rgba(30, 63, 100, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #24384f;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #2994ea 0%, #57b4ff 100%);
}

.appointment-count {
  color: #ff4d4f;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 1px;
  background: #dfe9f3;
  border-radius: 8px;
  overflow: hidden;
}

.schedule-cell {
  min-height: 64px;
  padding: 12px 10px;
  background: #f9fcff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.cell-day {
  font-size: 13px;
  color: #7387a1;
  margin-bottom: 6px;
}

.cell-value {
  font-size: 13px;
  color: #248fe2;
  text-align: center;
}

.cell-value.inactive {
  color: #c0c4cc;
}

</style>
