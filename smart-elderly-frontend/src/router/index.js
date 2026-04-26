import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

/** 路由路径与登录角色（与后端 JWT role 一致：0老人1家属2医生3社区） */
const guardedRoutes = [
  { match: (p) => p === '/elderly', role: 0 },
  { match: (p) => p === '/family', role: 1 },
  { match: (p) => p === '/doctor' || p.startsWith('/doctor/'), role: 2 },
  { match: (p) => p === '/schedule' || p.startsWith('/schedule/'), role: 2 },
  { match: (p) => p === '/archives' || p.startsWith('/archives/'), role: 2 },
  { match: (p) => p === '/admin', role: 3 }
]

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/elderly', component: () => import('../views/ElderlyView.vue') },
  { path: '/family', component: () => import('../views/FamilyView.vue') },
  {
    path: '/doctor',
    component: () => import('../views/DoctorView.vue'),
    children: [
      { path: '', component: () => import('../views/DoctorDashboard.vue') }
    ]
  },
  {
    path: '/schedule',
    component: () => import('../views/DoctorView.vue'),
    children: [
      { path: '', component: () => import('../views/ScheduleView.vue') }
    ]
  },
  {
    path: '/archives',
    component: () => import('../views/DoctorView.vue'),
    children: [
      { path: '', component: () => import('../views/ArchivesView.vue') }
    ]
  },
  { path: '/admin', component: () => import('../views/AdminView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.path === '/login' || to.path === '/') {
    next()
    return
  }
  const rule = guardedRoutes.find((r) => r.match(to.path))
  if (!rule) {
    next()
    return
  }
  let userRole = null
  try {
    userRole = JSON.parse(localStorage.getItem('userInfo') || '{}').role
  } catch {
    userRole = null
  }
  const tokenKey = `token_${rule.role}`
  const token = localStorage.getItem(tokenKey) || localStorage.getItem('token')
  if (userRole !== rule.role || !token) {
    ElMessage.warning('当前登录身份无权进入该页面，请使用正确角色重新登录')
    localStorage.removeItem('token')
    for (let i = 0; i <= 3; i++) localStorage.removeItem(`token_${i}`)
    localStorage.removeItem('userInfo')
    next({ path: '/login' })
    return
  }
  next()
})

export default router
