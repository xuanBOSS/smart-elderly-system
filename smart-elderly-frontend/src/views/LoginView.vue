# LoginView.vue - 登录界面组件

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const username = ref('')
const password = ref('')
const role = ref('')

const enterSystem = () => {
  if (!role.value) {
    ElMessage.warning('请选择登录角色')
    return
  }
  router.push(`/${role.value}`)
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
