# ElderlyView.vue - 老人界面组件

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Microphone, FirstAidKit, Calendar } from '@element-plus/icons-vue'

const router = useRouter()
const now = new Date()
const weekMap = ['日', '一', '二', '三', '四', '五', '六']
const currentDate = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`

const goLogin = () => router.push('/login')
const sendEmergency = () => {
  ElMessage.error('紧急求助信号已发送，请保持冷静')
}
</script>

<template>
  <div class="elderly-page">
    <div class="header-bar">
      <div class="header-date">{{ currentDate }}</div>
      <div class="header-greeting">王大爷，下午好</div>
      <div class="header-weather">多云 18°C 适宜出行</div>
      <el-button class="exit-button" type="text" @click="goLogin">退出</el-button>
    </div>

    <div class="voice-section">
      <div class="voice-circle">
        <Microphone class="voice-icon" />
      </div>
      <div class="voice-text">点击语音输入</div>
    </div>

    <div class="shortcut-row">
      <el-card class="shortcut-card" shadow="never">
        <div class="shortcut-icon"><FirstAidKit /></div>
        <div class="shortcut-label">我的今日用药</div>
      </el-card>
      <el-card class="shortcut-card" shadow="never">
        <div class="shortcut-icon"><Calendar /></div>
        <div class="shortcut-label">我的预约记录</div>
      </el-card>
    </div>

    <div class="bottom-spacer"></div>
    <div class="emergency-button-wrapper">
      <el-button type="primary" class="emergency-button" @click="sendEmergency">一键紧急求助</el-button>
    </div>
  </div>
</template>

<style scoped>
.elderly-page {
  min-height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  background: #f7f8fa;
  position: relative;
  padding-bottom: 100px;
}

.header-bar {
  background: #1890ff;
  color: #fff;
  padding: 20px 24px 120px;
  position: relative;
}

.header-date {
  font-size: 18px;
}

.header-greeting {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
}

.header-weather {
  margin-top: 4px;
  font-size: 16px;
  opacity: 0.85;
}

.exit-button {
  position: absolute;
  right: 20px;
  top: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
}

.voice-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: -80px auto 24px;
}

.voice-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #e6f7ff;
  border: 3px solid #1890ff;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: pulse 2s ease-in-out infinite;
}

.voice-icon {
  font-size: 48px;
  color: #1890ff;
}

.voice-text {
  margin-top: 12px;
  font-size: 20px;
  color: #1890ff;
}

.shortcut-row {
  display: flex;
  gap: 12px;
  margin: 0 16px;
}

.shortcut-card {
  flex: 1;
  min-height: 120px;
  border-radius: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background 0.2s ease;
}

.shortcut-card:hover {
  background: #f5f7fb;
}

.shortcut-icon {
  color: #52c41a;
  font-size: 32px;
  margin-bottom: 12px;
}

.shortcut-label {
  font-size: 18px;
  color: #303133;
}

.bottom-spacer {
  height: 80px;
}

.emergency-button-wrapper {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(480px, 100%);
  padding: 0 16px 16px;
}

.emergency-button {
  width: 100%;
  height: 64px;
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  border-radius: 0;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
</style>
