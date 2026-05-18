const ACCESS_TOKEN_KEY = 'accessToken'
const USER_INFO_KEY = 'userInfo'
const LEGACY_USER_KEY = 'currentUser'

export const saveAuthSession = ({ token, user }) => {
  if (token) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
  }
  if (user) {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
  }
}

export const clearAuthSession = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
  localStorage.removeItem(LEGACY_USER_KEY)
}

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY) || ''

export const getStoredUser = () => {
  const raw = localStorage.getItem(USER_INFO_KEY) || localStorage.getItem(LEGACY_USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

export const isAuthenticated = () => !!getAccessToken() && !!getStoredUser()

// App 启动时恢复并清理不一致状态
export const restoreAuthOnAppInit = () => {
  const token = getAccessToken()
  const user = getStoredUser()

  if (token && user) {
    // 兼容历史键：仅存在 currentUser 时，自动补齐到 userInfo
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
    return
  }

  // token 与 user 不完整时视为无效状态
  clearAuthSession()
}
