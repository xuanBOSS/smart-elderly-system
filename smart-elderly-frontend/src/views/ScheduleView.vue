# ScheduleView.vue - 医生界面，查看排班计划的组件
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const goBack = () => router.push('/doctor')

const scheduleItems = ref([])
const loading = ref(false)

const fetchSchedule = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/doctor/schedule')
    if (res.data.code === 200) {
      scheduleItems.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取排班失败')
    }
  } catch (error) {
    console.error('获取排班异常:', error)
    ElMessage.error('网络异常')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSchedule()
})
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

    <el-card class="schedule-card" shadow="hover" v-loading="loading">
      <el-empty v-if="scheduleItems.length === 0" description="您近期暂无排班安排" />
      
      <div class="schedule-row" v-for="(item, index) in scheduleItems" :key="index">
        <div class="schedule-day">{{ item.day }}</div>
        <div class="schedule-info">
          <div class="schedule-time">{{ item.time }}</div>
          <div class="schedule-note" :class="{'full-booked': item.note.includes('满')}">{{ item.note }}</div>
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
  background: transparent;
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
  color: #203449;
}

.page-header p {
  margin: 6px 0 0;
  color: #70839a;
}

.schedule-card {
  background: linear-gradient(180deg, #ffffff 0%, #f6f9fd 100%);
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #dce7f3;
  box-shadow: 0 8px 20px rgba(30, 63, 100, 0.08);
}

.schedule-row {
  display: flex;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #eaf0f6;
}

.schedule-row:last-child {
  border-bottom: none;
}

.schedule-day {
  width: 100px;
  font-size: 18px;
  font-weight: 700;
  color: #238ee1;
}

.schedule-info {
  flex: 1;
}

.schedule-time {
  font-size: 15px;
  font-weight: 500;
  color: #2a3f57;
}

.schedule-note {
  margin-top: 6px;
  font-size: 13px;
  color: #7f92a7;
}

/* 如果号源满了，可以扩展个标红样式 */
.full-booked {
  color: #ff4d4f;
  font-weight: bold;
}
</style>
