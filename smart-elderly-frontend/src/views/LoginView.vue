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
        localStorage.setItem('token', data.token)
      }

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
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <div class="login-title">智慧社区养老医疗服务平台</div>
      <div class="login-subtitle">请登录您的账号</div>
      <el-form label-position="top" class="login-form">
        <el-form-item label="账号">
          <el-input v-model="username" placeholder="请输入账号" prefix-icon="User">
            <template #prefix>
              <User />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" show-password>
            <template #prefix>
              <Lock />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="role" placeholder="请选择登录角色">
            <el-option label="老年用户" value="elderly" />
            <el-option label="家属用户" value="family" />
            <el-option label="医务人员" value="doctor" />
            <el-option label="社区管理" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-button" @click="enterSystem">
            进入系统
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">当前为演示模式，选择角色即可进入对应界面</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f0f2f5;
}

.login-card {
  width: 480px;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08);
  padding: 40px 40px 32px;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  text-align: center;
}

.login-subtitle {
  font-size: 14px;
  color: #909399;
  text-align: center;
  margin-top: 8px;
  margin-bottom: 28px;
}

.login-form {
  width: 100%;
}

.login-button {
  width: 100%;
}

.login-tip {
  margin-top: 16px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}
</style>
