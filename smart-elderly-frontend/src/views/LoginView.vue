# LoginView.vue - 登录界面组件

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const username = ref('')
const password = ref('')
const role = ref('')

const getRoleNumber = (roleValue) => {
  const roleMap = {
    'elderly': 0,   // 老人
    'family': 1,    // 家属
    'doctor': 2,    // 医生
    'admin': 3      // 社区
  }
  return roleMap[roleValue] || 0
}

const enterSystem = async () => {
  if (!role.value) {
    ElMessage.warning('请选择登录角色')
    return
  }

  try {
    // 🌟加上完整的后端地址 8080，解决 404 报错
    const response = await axios.post('http://localhost:8080/api/user/login', {
      username: username.value,
      password: password.value,
      role: getRoleNumber(role.value)
    })
    
    const { code, message, data } = response.data || {}
    
    if (code === 200 && data && data.role !== undefined) {
      
      // 🌟把后端的 Token 存入本地缓存
      if (data.token) {
        // 按角色存储 token，避免切换角色后互相覆盖导致其他端被踢回登录
        localStorage.setItem(`token_${data.role}`, data.token)
        // 同时保留旧字段，保证兼容旧逻辑
        localStorage.setItem('token', data.token)
      }
      localStorage.setItem('userInfo', JSON.stringify({
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role
      }))

      // 登录成功：显示成功消息并跳转
      ElMessage.success('登录成功，正在跳转...')
      const roleMap = ['elderly', 'family', 'doctor', 'admin']
      
      //如果后端返回200但角色值不为0-3，则默认跳转到老人界面（保持程序健壮性）
      const roleName = roleMap[data.role] || 'elderly'
      router.push(`/${roleName}`)
    } else {
      // 登录失败：显示错误消息
      ElMessage.error(message || '登录失败，请检查账号密码和角色')
    }
  } catch (error) {
    // 网络错误或服务器错误
    const msg = error.response?.data?.message || error.message || '登录请求失败，请稍后重试'
    ElMessage.error(msg)
  }
}

const onForgotPassword = () => {
  ElMessage.info('请联系社区管理员重置密码')
}

const onRegisterRequest = () => {
  ElMessage.info('已记录注册申请，请等待审核')
}
</script>

<template>
  <div class="login-page">
    <div class="desk-card">
      <div class="intro-panel">
        <div class="intro-overlay">
          <h1 class="intro-title">智慧社区养老医疗服务平台</h1>
          <p class="intro-subtitle">Smart Community Elderly Care & Medical Service Platform</p>
          <p class="intro-text">连接社区资源，守护长者健康。</p>
        </div>
      </div>

      <el-card class="login-card" shadow="never">
        <div class="brand-top">
          <div class="brand-mark">✚</div>
          <div class="brand-text">智慧社区养老<br>医疗服务平台</div>
        </div>
        <div class="login-title">欢迎登录</div>
        <el-form label-position="top" class="login-form">
          <el-form-item label="账号 / 用户名">
            <el-input
              v-model.trim="username"
              placeholder="请输入账号或用户名"
              clearable
              @keyup.enter="enterSystem"
            >
              <template #prefix><User /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model.trim="password"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
              @keyup.enter="enterSystem"
            >
              <template #prefix><Lock /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="role" placeholder="请选择您的角色">
              <el-option label="老年用户" value="elderly" />
              <el-option label="家属用户" value="family" />
              <el-option label="医务人员" value="doctor" />
              <el-option label="社区管理" value="admin" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-button" @click="enterSystem">
              登录系统
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          <button class="footer-link-btn" type="button" @click="onForgotPassword">忘记密码？</button>
          <button class="footer-link-btn primary" type="button" @click="onRegisterRequest">新成员注册申请</button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #eef2f6;
  padding: 24px;
}

.desk-card {
  width: 980px;
  max-width: calc(100vw - 32px);
  min-height: 540px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 22px 60px rgba(14, 39, 65, 0.22);
  display: grid;
  grid-template-columns: 2fr 1fr;
}

.intro-panel {
  position: relative;
  background-color: #edf5fb;
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
  border-right: 1px solid #dbe6f2;
}

.intro-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 22%, #9fd08f 0 10%, transparent 16%),
    radial-gradient(circle at 86% 74%, #b6d8f8 0 8%, transparent 14%),
    linear-gradient(130deg, transparent 54%, rgba(66, 153, 225, 0.1) 55%, transparent 62%);
  opacity: 0.45;
}

.intro-overlay {
  position: relative;
  z-index: 2;
  padding: 84px 54px;
  background: linear-gradient(180deg, rgba(244, 250, 255, 0.4), rgba(244, 250, 255, 0.2));
}

.intro-title {
  margin: 0;
  font-size: 42px;
  line-height: 1.2;
  color: #23374d;
  font-weight: 700;
}

.intro-subtitle {
  margin: 16px 0 0;
  font-size: 18px;
  color: #3f5f7b;
}

.intro-text {
  margin-top: 26px;
  font-size: 26px;
  color: #2c4a63;
  font-weight: 500;
}

.login-card {
  border: none;
  border-radius: 0;
  box-shadow: none;
  padding: 34px 26px 22px;
  display: flex;
  flex-direction: column;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #e7f2ff;
  color: #2a84d5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.brand-text {
  font-size: 14px;
  color: #3d5a74;
  line-height: 1.25;
  font-weight: 600;
}

.login-title {
  margin-top: 18px;
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
}

.login-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
  margin-bottom: 16px;
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
  padding-bottom: 4px;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select__wrapper) {
  border-radius: 8px;
  min-height: 40px;
  box-shadow: 0 0 0 1px #d8e3f0 inset !important;
  background: #fbfdff;
}

.login-form :deep(.el-input__inner),
.login-form :deep(.el-select__selected-item) {
  color: #1e293b;
  font-weight: 500;
}

.login-form :deep(.el-input__wrapper.is-focus),
.login-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #5b9eff inset !important;
}

.login-button {
  width: 100%;
  height: 42px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(90deg, #1976d2 0%, #2b91ff 100%);
  border: none;
  box-shadow: 0 8px 16px rgba(34, 116, 201, 0.24);
}

.login-footer {
  margin-top: auto;
  padding-top: 6px;
  display: flex;
  justify-content: space-between;
}

.footer-link-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  padding: 0;
  cursor: pointer;
}

.footer-link-btn.primary {
  color: #1f8dd6;
  font-weight: 600;
}

@media (max-width: 900px) {
  .desk-card {
    grid-template-columns: 1fr;
  }

  .intro-panel {
    min-height: 180px;
  }

  .intro-overlay {
    padding: 28px 24px;
  }

  .intro-title {
    font-size: 28px;
  }

  .intro-subtitle {
    font-size: 14px;
  }

  .intro-text {
    margin-top: 12px;
    font-size: 18px;
  }
}
</style>
