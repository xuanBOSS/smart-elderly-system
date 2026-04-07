# ScheduleView.vue - 医生界面，查看排班计划的组件

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const goBack = () => router.push('/doctor')

const scheduleItems = [
  { day: '周一', time: '上午 09:00 - 12:00', note: '门诊接诊' },
  { day: '周二', time: '全天', note: '预约手术' },
  { day: '周三', time: '无排班', note: '休息日' },
  { day: '周四', time: '下午 13:00 - 17:00', note: '上门随访' },
  { day: '周五', time: '上午 09:00 - 12:00', note: '门诊接诊' },
  { day: '周六', time: '无排班', note: '值班待命' },
  { day: '周日', time: '无排班', note: '休息日' }
]
</script>

<template>
  <div class="schedule-page">
    <div class="page-header">
      <div>
        <h1>我的排班计划</h1>
        <p>查看本周排班安排与任务提醒</p>
      </div>
      <el-button type="primary" @click="goBack">返回预约大厅</el-button>
    </div>

    <el-card class="schedule-card" shadow="hover">
      <div class="schedule-row" v-for="item in scheduleItems" :key="item.day">
        <div class="schedule-day">{{ item.day }}</div>
        <div>
          <div class="schedule-time">{{ item.time }}</div>
          <div class="schedule-note">{{ item.note }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.schedule-page {
  min-height: 100vh;
  max-width: 880px;
  margin: 0 auto;
  padding: 24px;
  background: #f0f2f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 6px 0 0;
  color: #606266;
}

.schedule-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.schedule-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f2f3f5;
}

.schedule-row:last-child {
  border-bottom: none;
}

.schedule-day {
  width: 100px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.schedule-time {
  font-size: 14px;
  color: #303133;
}

.schedule-note {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}
</style>
