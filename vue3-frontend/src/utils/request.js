import axios from 'axios'
import { clearAuthSession, getAccessToken } from '@/utils/auth'

// 创建axios实例
const request = axios.create({
    baseURL: 'http://localhost:8081', // 后端接口前缀
    timeout: 5000, // 请求超时时间
    headers: {
        'Content-Type': 'application/json;charset=utf-8'
    }
})

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const token = getAccessToken()
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        try {
            const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
            if (raw) {
                const user = JSON.parse(raw)
                const userId = String(user?.userId || '').trim()
                const roleCode = String(user?.role?.roleCode || user?.roleCode || user?.rolePermission || '').trim()
                if (userId) config.headers['X-User-Id'] = userId
                if (roleCode) config.headers['X-Role-Code'] = roleCode
            }
        } catch (e) {
            // ignore parse error
        }

        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        return response.data // 直接返回响应体
    },
    (error) => {
        const status = error?.response?.status
        if (status === 401) {
            clearAuthSession()
            if (window.location.pathname !== '/login' && window.location.pathname !== '/forgot-password') {
                window.location.href = '/login'
            }
        }
        console.error('请求错误：', error)
        return Promise.reject(error)
    }
)

export default request