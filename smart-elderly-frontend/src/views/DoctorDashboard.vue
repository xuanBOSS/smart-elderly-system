# DoctorDashboard.vue - 医生界面，预约管理界面组件

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const schedule = [
  { day: '周一', value: '上午 ✓' },
  { day: '周二', value: '全天 ✓' },
  { day: '周三', value: '' },
  { day: '周四', value: '下午 ✓' },
  { day: '周五', value: '上午 ✓' },
  { day: '周六', value: '' },
  { day: '周日', value: '' }
]

const appointments = [
  { name: '王大爷', time: '12/22 上午09:00', type: '门诊', status: '待确认' },
  { name: '李奶奶', time: '12/22 下午14:00', type: '上门', status: '待确认' },
  { name: '张大爷', time: '12/23 上午10:00', type: '门诊', status: '待确认' }
]

const confirmAppointment = () => {
  ElMessage.success('操作成功')
}


const goLogin = () => router.push('/login')

const rejectAppointment = () => {
  ElMessage.success('操作成功')
}
</script>

<template>
  <div class="doctor-dashboard">
    <div class="doctor-topbar">
      <div class="page-title">患者预约大厅</div>
      <div class="page-date">2024年12月20日 星期五</div>
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
        待处理预约 <span class="appointment-count">（3）</span>
      </div>
      <el-table :data="appointments" stripe border style="width: 100%;">
        <el-table-column prop="name" label="患者姓名" width="120" />
        <el-table-column prop="time" label="预约时间" width="180" />
        <el-table-column prop="type" label="预约类型" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="confirmAppointment">确认</el-button>
            <el-button size="small" type="danger" plain @click="rejectAppointment">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
  <el-button class="exit-button" type="text" @click="goLogin">退出</el-button>
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
