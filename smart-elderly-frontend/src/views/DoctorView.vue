# DoctorView.vue - 医生主界面组件，包含侧边导航和内容区域

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { List, Calendar, Document, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const isCollapsed = ref(false)

const navItems = [
  { label: '患者预约大厅', icon: List, path: '/doctor' },
  { label: '我的排班计划', icon: Calendar, path: '/schedule' },
  { label: '患者健康档案库', icon: Document, path: '/archives' }
]

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const navigate = (path) => {
  router.push(path)
}


const activePath = computed(() => route.path)
</script>

<template>
  <div class="doctor-layout">
    <aside class="doctor-sider" :class="{ collapsed: isCollapsed }">
      <div class="doctor-logo-section">
        <div class="doctor-logo" v-if="!isCollapsed">医务工作台</div>
        <div class="doctor-logo-icon" v-else>医</div>
        <button class="collapse-button" @click="toggleCollapse" type="button">
          <component :is="isCollapsed ? ArrowRight : ArrowLeft" class="toggle-icon" />
        </button>
      </div>

      <nav class="doctor-nav">
        <div
          v-for="item in navItems"
          :key="item.path"
          :class="['nav-item', { active: activePath === item.path, collapsed: isCollapsed }]"
          @click="navigate(item.path)"
        >
          <item.icon class="nav-icon" />
          <span v-if="!isCollapsed" class="nav-label">{{ item.label }}</span>
        </div>
      </nav>

      
    </aside>

    <main class="doctor-main" :style="{ marginLeft: isCollapsed ? '60px' : '220px' }">
      <router-view />
    </main>
  </div>

  
</template>

<style scoped>
.doctor-layout {
  display: flex;
  flex-direction: row;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #eef4fb 0%, #e6edf6 100%);
}

.doctor-sider {
  width: 220px;
  flex-shrink: 0;
  height: 100vh;
  background: linear-gradient(180deg, #10263f 0%, #163252 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 24px 20px 20px;
  position: fixed;
  top: 0;
  left: 0;
}

.doctor-sider.collapsed {
  width: 60px;
  padding: 20px 12px;
}

.doctor-logo-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.doctor-logo {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.doctor-logo-icon {
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 16px;
}

.collapse-button {
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
}

.toggle-icon {
  font-size: 16px;
}

.doctor-nav {
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 6px;
  border-left: 3px solid transparent;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.nav-item.collapsed {
  justify-content: center;
  padding: 14px 0;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(52, 146, 255, 0.25) 0%, rgba(52, 146, 255, 0.08) 100%);
  border-left-color: #56acff;
  box-shadow: inset 0 0 0 1px rgba(86, 172, 255, 0.3);
}

.nav-icon {
  margin-right: 10px;
  font-size: 16px;
}

.nav-item.collapsed .nav-icon {
  margin-right: 0;
}

.nav-label {
  white-space: nowrap;
}

.doctor-logout {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  cursor: pointer;
  padding: 12px 0;
}

.doctor-logout span {
  display: inline-block;
}

.logout-icon {
  margin-right: 6px;
}

.doctor-main {
  flex: 1;
  min-width: 800px;
  max-width: calc(100vw - 60px);
  background: linear-gradient(180deg, #eef4fb 0%, #e8eff8 100%);
  padding: 24px;
  overflow-y: auto;
  height: 100vh;
  transition: margin-left 0.3s ease;
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
