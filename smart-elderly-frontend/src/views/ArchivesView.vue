# ArchivesView.vue - 医生界面，查看患者健康档案的组件

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const goBack = () => router.push('/doctor')

const archives = [
  { name: '王大爷', age: 78, condition: '高血压、糖尿病', lastVisit: '12/18' },
  { name: '李奶奶', age: 82, condition: '心律不齐', lastVisit: '12/16' },
  { name: '张大爷', age: 80, condition: '慢阻肺', lastVisit: '12/15' }
]
</script>

<template>
  <div class="archives-page">
    <div class="page-header">
      <div>
        <h1>患者健康档案库</h1>
        <p>管理患者基本信息与健康状态</p>
      </div>
      <el-button type="primary" @click="goBack">返回预约大厅</el-button>
    </div>

    <el-card class="archives-card" shadow="hover">
      <div class="archive-row" v-for="item in archives" :key="item.name">
        <div class="archive-summary">
          <div class="archive-name">{{ item.name }}</div>
          <div class="archive-condition">状况：{{ item.condition }}</div>
        </div>
        <div class="archive-meta">
          <div>年龄 {{ item.age }} 岁</div>
          <div>最近就诊 {{ item.lastVisit }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.archives-page {
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

.archives-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.archive-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f2f3f5;
}

.archive-row:last-child {
  border-bottom: none;
}

.archive-summary {
  max-width: 60%;
}

.archive-name {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.archive-condition {
  margin-top: 6px;
  font-size: 14px;
  color: #606266;
}

.archive-meta {
  text-align: right;
  font-size: 13px;
  color: #606266;
}
</style>
