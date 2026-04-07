import { createRouter, createWebHistory } from 'vue-router'

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

export default router
