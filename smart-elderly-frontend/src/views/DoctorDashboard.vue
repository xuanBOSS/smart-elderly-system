<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request' // 🌟 引入咱们的网络大总管

const router = useRouter()

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
      
      // 遍历后端数据，拼装“上午/下午/全天”
      apiData.forEach(apiItem => {
        // 找到我们格子里对应的那一天（比如找到 '周二'）
        const target = schedule.value.find(s => s.day === apiItem.day)
        if (target) {
          const isAm = apiItem.time.includes('上午')
          const str = isAm ? '上午 ✓' : '下午 ✓'
          
          if (target.value === '') {
            target.value = str // 如果是空的，直接填入
          } else if (target.value !== str) {
            target.value = '全天 ✓' // 如果原本有上午，新来个下午，就变成全天
          }
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

const goLogin = () => {
  localStorage.removeItem('token')
  router.push('/login')
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
      <div class="page-date">2026年04月11日 星期六</div> 
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
    </section>
  </div>
</template>

<style scoped>
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
  color: #303133;
}

.page-date {
  font-size: 14px;
  color: #909399;
}

.schedule-card,
.appointment-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
}

.appointment-count {
  color: #ff4d4f;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 1px;
  background: #ebeef5;
}

.schedule-cell {
  min-height: 64px;
  padding: 12px 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.cell-day {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.cell-value {
  font-size: 13px;
  color: #1890ff;
  text-align: center;
}

.cell-value.inactive {
  color: #c0c4cc;
}
</style>
