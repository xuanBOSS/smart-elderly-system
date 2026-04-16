import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router' // 引入路由，用于后续跳转

// 1. 创建一个自定义的 axios 实例
const request = axios.create({
  baseURL: 'http://localhost:8080', // 你的后端统一地址
  timeout: 60000 // 请求超时时间
})

const getTokenKeyByPath = () => {
  const path = window.location.pathname || ''
  // 路由约定：/elderly, /family, /doctor*, /admin
  if (path.startsWith('/doctor') || path.startsWith('/schedule') || path.startsWith('/archives')) return 'token_2'
  if (path.startsWith('/elderly')) return 'token_0'
  if (path.startsWith('/family')) return 'token_1'
  if (path.startsWith('/admin')) return 'token_3'
  return 'token' // 兼容旧逻辑
}

// 2. 请求拦截器：发请求前，自动去 localStorage 拿 token 塞进请求头
request.interceptors.request.use(
  config => {
    // 从本地缓存中获取刚刚登录存下来的 token
    const token = localStorage.getItem(getTokenKeyByPath()) || localStorage.getItem('token')
    if (token) {
      // 这里的 'Bearer ' 是后端 JWT 约定的前缀，注意 Bearer 后面有一个空格！
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 3. 响应拦截器：后端返回数据后，统一处理错误（比如 Token 过期）
request.interceptors.response.use(
  response => {
    // 如果 HTTP 状态码是 200，说明请求成功，直接把数据剥离出来返回
    return response
  },
  error => {
    // 如果后端保安拦截了，返回了 401（未登录或 Token 失效）
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      const tokenKey = getTokenKeyByPath()
      // 清理当前页面对应的 token，避免误踢其他角色
      localStorage.removeItem(tokenKey)
      localStorage.removeItem('token') // 兼容旧逻辑
      router.push('/login') // 自动踢回登录页
    } else {
      ElMessage.error(error.message || '系统异常')
    }
    return Promise.reject(error)
  }
)

export default request