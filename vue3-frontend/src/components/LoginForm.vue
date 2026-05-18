<template>
  <main v-if="!isForgotPassword" class="auth-card login-card">
    <h1 class="title">物料库服务登录系统</h1>
    <p class="sub">Material Library Service System</p>

    <form @submit.prevent="handleLogin">
      <div class="field">
        <input
            class="input"
            type="text"
            placeholder="请输入账号"
            v-model="loginForm.username"
        />
      </div>

      <div class="field">
        <input
            class="input"
            type="password"
            placeholder="请输入密码"
            v-model="loginForm.password"
        />
      </div>

      <div class="field">
        <select class="input" v-model="loginForm.roleCode">
          <option value="">请选择角色</option>
          <option value="MANAGER">管理人员</option>
          <option value="WORKER">施工人员</option>
          <option value="WAREHOUSE">仓库管理员</option>
        </select>
      </div>

      <div class="field captcha-field">
        <input
            class="input"
            type="text"
            maxlength="4"
            placeholder="请输入验证码"
            v-model="loginForm.captchaCode"
        />
        <img
            class="captcha-image"
            :src="captchaImage"
            alt="验证码"
            title="点击刷新验证码"
            @click="loadCaptcha"
        />
      </div>

      <div class="row">
        <label class="remember">
          <input
              type="checkbox"
              v-model="loginForm.rememberMe"
          />
          <span>记住我</span>
        </label>
        <a href="#" @click.prevent="handleForgotPassword">忘记密码？</a>
      </div>

      <button class="btn" type="submit">登 录</button>
    </form>

    <div class="footer">© 2026 物料库服务系统 版权所有</div>
  </main>

  <main v-else class="forgot-page">
    <header class="forgot-topbar">
      <div class="forgot-system-name">物料库服务系统</div>
      <div class="forgot-page-name">找回密码</div>
    </header>

    <section class="forgot-main">
      <div class="forgot-card">
        <template v-if="forgotStep === 'account'">
          <h1 class="forgot-title">找回密码</h1>

          <form @submit.prevent="handleForgotNext">
            <div class="forgot-label">请输入账号</div>
            <div class="field">
              <input
                  class="input"
                  type="text"
                  placeholder="请输入账号"
                  v-model="forgotForm.username"
              />
            </div>

            <button class="btn forgot-btn" type="submit">下 一 步</button>
          </form>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'verify'">
          <h1 class="forgot-title">身份验证</h1>
          <p class="verify-tip">为了保护你的账号安全，请通过邮箱验证身份</p>

          <div class="verify-options">
            <button class="verify-item" type="button" @click="chooseEmailRecovery">
              <div class="verify-item-title">邮箱验证</div>
              <div class="verify-item-desc">通过 {{ recoveryInfo.maskedEmail || '未绑定邮箱' }} 接收验证邮件</div>
            </button>
          </div>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToAccountStep">返回上一步</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'email-code'">
          <h1 class="forgot-title">邮箱验证与重置</h1>
          <p class="verify-tip">请输入验证码并设置新密码（验证码正确后才会重置）</p>
          <p class="verify-tip verify-mail">{{ recoveryInfo.maskedEmail || '未绑定邮箱' }}</p>

          <div class="field">
            <div class="code-row">
              <input
                  class="input"
                  :class="{ 'input-error': emailCodeError }"
                  type="text"
                  placeholder="请输入验证码"
                  v-model="emailVerifyCode"
                  @input="clearEmailCodeError"
              />
              <button
                  class="plain-btn resend-btn"
                  type="button"
                  :disabled="emailResendCountdown > 0"
                  @click="resendEmailCode"
              >
                {{ emailResendCountdown > 0 ? `${emailResendCountdown}s后重发` : '重新发送' }}
              </button>
            </div>
            <div v-if="emailCodeError" class="field-error">{{ emailCodeErrorMsg || '验证码输入错误' }}</div>
          </div>

          <div class="field">
            <input
                class="input"
                type="password"
                placeholder="请输入新密码"
                v-model="resetPasswordForm.newPassword"
            />
          </div>

          <div class="field">
            <input
                class="input"
                type="password"
                placeholder="请再次输入新密码"
                v-model="resetPasswordForm.confirmPassword"
            />
          </div>

          <button class="btn forgot-btn" type="button" @click="submitEmailCodeAndReset">验 证 并 重 置</button>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToVerifyStep">返回验证方式</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'email-input'">
          <h1 class="forgot-title">邮箱验证</h1>
          <p class="verify-tip">请输入账号绑定邮箱完成验证</p>
          <p class="verify-tip verify-mail">已绑定：{{ recoveryInfo.maskedEmail || '未绑定邮箱' }}</p>

          <div class="field">
            <input
                class="input"
                type="text"
                placeholder="请输入完整邮箱"
                v-model="emailInputValue"
            />
          </div>

          <button class="btn forgot-btn" type="button" @click="submitEmailMatch">验 证</button>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToVerifyStep">返回验证方式</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'phone-input'">
          <h1 class="forgot-title">手机验证</h1>
          <p class="verify-tip">请输入账号绑定手机号完成验证</p>
          <p class="verify-tip verify-mail">已绑定：{{ recoveryInfo.maskedPhone || '未绑定手机号' }}</p>

          <div class="field">
            <input
                class="input"
                type="text"
                placeholder="请输入完整手机号"
                v-model="phoneInputValue"
            />
          </div>

          <button class="btn forgot-btn" type="button" @click="submitPhoneMatch">验 证</button>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToVerifyStep">返回验证方式</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'phone-code'">
          <h1 class="forgot-title">手机验证</h1>
          <p class="verify-tip">请输入验证码（验证已发送至手机号）</p>
          <p class="verify-tip verify-mail">{{ recoveryInfo.maskedPhone || '未绑定手机号' }}</p>

          <div class="field">
            <input
                class="input"
                type="text"
                placeholder="请输入验证码"
                v-model="phoneVerifyCode"
            />
          </div>

          <button class="btn forgot-btn" type="button" @click="submitPhoneCode">验 证</button>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToVerifyStep">返回验证方式</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>

        <template v-else-if="forgotStep === 'reset-password'">
          <h1 class="forgot-title">重置密码</h1>
          <p class="verify-tip">身份验证通过，请输入并确认新密码</p>

          <div class="field">
            <input
                class="input"
                type="password"
                placeholder="请输入新密码（至少6位）"
                v-model="resetPasswordForm.newPassword"
            />
          </div>

          <div class="field">
            <input
                class="input"
                type="password"
                placeholder="请再次输入新密码"
                v-model="resetPasswordForm.confirmPassword"
            />
          </div>

          <button class="btn forgot-btn" type="button" @click="submitResetPassword">确 认 重 置</button>

          <div class="forgot-links">
            <a href="#" @click.prevent="backToVerifyStep">返回验证方式</a>
            <a href="#" @click.prevent="backToLogin">返回登录</a>
          </div>
        </template>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request' // 导入axios封装
import { saveAuthSession } from '@/utils/auth'
import { REGEXP_CODE_SIX, REGEXP_EMAIL, REGEXP_PHONE, REGEXP_PWD } from '@/utils/regexp'
import '@/assets/styles/login.css' // 导入样式

const props = defineProps({
  startForgot: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['login-success'])
const router = useRouter()

// 登录表单数据（匹配后端RoleCode）
const loginForm = ref({
  username: '',
  password: '',
  roleCode: '', // MANAGER/WORKER/WAREHOUSE
  captchaId: '',
  captchaCode: '',
  rememberMe: true
})

const captchaImage = ref('')

const isForgotPassword = ref(false)
const forgotStep = ref('account')
const forgotForm = ref({
  username: ''
})
const recoveryInfo = ref({
  hasPhone: false,
  hasEmail: false,
  userMail: '',
  maskedPhone: '',
  maskedEmail: ''
})
const emailVerifyCode = ref('')
const phoneVerifyCode = ref('')
const phoneInputValue = ref('')
const emailInputValue = ref('')
const emailCodeError = ref(false)
const emailCodeErrorMsg = ref('')
const emailResendCountdown = ref(0)
let emailResendTimer = null
const resetPasswordForm = ref({
  newPassword: '',
  confirmPassword: ''
})

// 登录处理函数
const handleLogin = async () => {
  // 前端表单校验
  if (!loginForm.value.username) {
    alert('请输入账号！')
    return
  }
  if (!loginForm.value.password) {
    alert('请输入密码！')
    return
  }
  if (!loginForm.value.roleCode) {
    alert('请选择角色！')
    return
  }
  if (!loginForm.value.captchaCode || loginForm.value.captchaCode.trim().length !== 4) {
    alert('请输入4位验证码！')
    return
  }

  try {
    // 发送登录请求到后端
    const payload = {
      username: loginForm.value.username,
      password: loginForm.value.password,
      roleCode: loginForm.value.roleCode,
      rememberMe: loginForm.value.rememberMe,
      captchaId: loginForm.value.captchaId,
      captchaCode: loginForm.value.captchaCode
    }
    const res = await request.post('/login', payload)
    if (res.code === 200) {
      // 登录成功：保存Token + 用户信息并进入系统
      const payload = res.data || {}
      const token = payload.accessToken || ''
      const user = payload.user || payload

      saveAuthSession({ token, user })
      emit('login-success', user)
    } else {
      // 登录失败：提示错误信息
      alert(res.msg)
      loginForm.value.captchaCode = ''
      await loadCaptcha()
    }
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg ? `登录失败：${backendMsg}` : '登录失败，请检查后端服务是否启动！')
    console.error('登录请求错误：', error)
    loginForm.value.captchaCode = ''
    await loadCaptcha()
  }
}

const loadCaptcha = async () => {
  try {
    const res = await request.get('/login/captcha')
    if (res.code === 200 && res.data) {
      loginForm.value.captchaId = res.data.captchaId || ''
      captchaImage.value = res.data.captchaImage || ''
      return
    }
    throw new Error(res?.msg || '验证码加载失败')
  } catch (error) {
    loginForm.value.captchaId = ''
    captchaImage.value = ''
    console.error('加载验证码失败：', error)
  }
}

// 忘记密码处理
const handleForgotPassword = () => {
  forgotForm.value.username = loginForm.value.username || ''
  forgotStep.value = 'account'
  isForgotPassword.value = true
  router.push('/forgot-password')
}

const backToLogin = () => {
  isForgotPassword.value = false
  forgotStep.value = 'account'
  emailVerifyCode.value = ''
  phoneVerifyCode.value = ''
  phoneInputValue.value = ''
  emailInputValue.value = ''
  emailCodeError.value = false
  emailCodeErrorMsg.value = ''
  emailResendCountdown.value = 0
  if (emailResendTimer) {
    clearInterval(emailResendTimer)
    emailResendTimer = null
  }
  resetPasswordForm.value = {
    newPassword: '',
    confirmPassword: ''
  }
  router.push('/login')
}

const handleForgotNext = async () => {
  if (!forgotForm.value.username) {
    alert('请输入账号！')
    return
  }

  try {
    const res = await request.get('/login/recovery/check-account', {
      params: { username: forgotForm.value.username }
    })

    if (res.code === 200 && res.data) {
      recoveryInfo.value = {
        hasPhone: !!res.data.hasPhone,
        hasEmail: !!res.data.hasEmail,
        userMail: res.data.userMail || '',
        maskedPhone: res.data.maskedPhone || '',
        maskedEmail: res.data.maskedEmail || ''
      }
      forgotStep.value = 'verify'
      return
    }

    alert(res.msg || '无法找到该账号')
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '无法找到该账号')
  }
}

const chooseEmailRecovery = () => {
  if (!recoveryInfo.value.hasEmail) {
    alert('该账号未绑定邮箱，无法通过邮箱找回')
    return
  }
  if (!REGEXP_EMAIL.test((recoveryInfo.value.userMail || '').trim())) {
    alert('该账号绑定邮箱格式不合法，请联系管理员维护邮箱信息')
    return
  }
  emailInputValue.value = ''
  forgotStep.value = 'email-input'
}

const backToAccountStep = () => {
  forgotStep.value = 'account'
}

const backToVerifyStep = () => {
  forgotStep.value = 'verify'
}

const startEmailResendCountdown = () => {
  emailResendCountdown.value = 60
  if (emailResendTimer) {
    clearInterval(emailResendTimer)
    emailResendTimer = null
  }
  emailResendTimer = setInterval(() => {
    if (emailResendCountdown.value <= 1) {
      emailResendCountdown.value = 0
      clearInterval(emailResendTimer)
      emailResendTimer = null
      return
    }
    emailResendCountdown.value -= 1
  }, 1000)
}

const resendEmailCode = async () => {
  if (emailResendCountdown.value > 0) {
    return
  }
  try {
    const res = await request.post('/login/recovery/send-email-code', null, {
      params: { username: forgotForm.value.username }
    })
    if (res.code === 200) {
      startEmailResendCountdown()
      return
    }
    alert(res.msg || '验证码发送失败')
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '验证码发送失败，请稍后重试')
  }
}

const submitEmailMatch = () => {
  const inputEmail = (emailInputValue.value || '').trim()
  const bindEmail = (recoveryInfo.value.userMail || '').trim()

  if (!inputEmail) {
    alert('请输入邮箱')
    return
  }
  if (!REGEXP_EMAIL.test(inputEmail)) {
    alert('邮箱格式不正确')
    return
  }
  if (!bindEmail || !REGEXP_EMAIL.test(bindEmail)) {
    alert('系统绑定邮箱异常，请联系管理员')
    return
  }
  if (inputEmail.toLowerCase() !== bindEmail.toLowerCase()) {
    alert('邮箱不匹配，请重新输入')
    return
  }

  request.post('/login/recovery/send-email-code', null, {
    params: { username: forgotForm.value.username }
  }).then((res) => {
    if (res.code === 200) {
      emailVerifyCode.value = ''
      emailCodeError.value = false
      emailCodeErrorMsg.value = ''
      startEmailResendCountdown()
      resetPasswordForm.value = {
        newPassword: '',
        confirmPassword: ''
      }
      forgotStep.value = 'email-code'
    } else {
      alert(res.msg || '验证码发送失败')
    }
  }).catch((error) => {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '验证码发送失败，请稍后重试')
  })
}

const clearEmailCodeError = () => {
  emailCodeError.value = false
  emailCodeErrorMsg.value = ''
}

const submitEmailCodeAndReset = async () => {
  const verifyCode = (emailVerifyCode.value || '').trim()
  const newPassword = (resetPasswordForm.value.newPassword || '').trim()
  const confirmPassword = (resetPasswordForm.value.confirmPassword || '').trim()

  if (!verifyCode) {
    emailCodeError.value = true
    emailCodeErrorMsg.value = '请输入验证码'
    return
  }
  if (!REGEXP_CODE_SIX.test(verifyCode)) {
    emailCodeError.value = true
    emailCodeErrorMsg.value = '请输入6位数字验证码'
    return
  }
  if (!newPassword) {
    alert('请输入新密码')
    return
  }
  if (!REGEXP_PWD.test(newPassword)) {
    alert('新密码须为6-18位，且需包含至少两类字符（数字/大小写字母/符号）')
    return
  }
  if (!confirmPassword) {
    alert('请再次输入新密码')
    return
  }
  if (newPassword !== confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }

  try {
    const verifyRes = await request.post('/login/recovery/verify-email-code', null, {
      params: {
        username: forgotForm.value.username,
        code: verifyCode
      }
    })

    if (verifyRes.code !== 200) {
      emailCodeError.value = true
      emailCodeErrorMsg.value = verifyRes.msg || '验证码输入错误'
      return
    }

    emailCodeError.value = false
    emailCodeErrorMsg.value = ''

    const resetRes = await request.post('/login/recovery/reset-password', null, {
      params: {
        username: forgotForm.value.username,
        newPassword
      }
    })

    if (resetRes.code === 200) {
      alert('密码重置成功，请使用新密码登录')
      loginForm.value.password = ''
      backToLogin()
      return
    }

    alert(resetRes.msg || '密码重置失败，请稍后重试')
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    if (backendMsg && (backendMsg.includes('验证码') || backendMsg.includes('过期'))) {
      emailCodeError.value = true
      emailCodeErrorMsg.value = backendMsg
      return
    }
    alert(backendMsg || '密码重置失败，请稍后重试')
  }
}

const submitEmailCode = async () => {
  // 兼容旧流程方法，已由 submitEmailCodeAndReset 替代
  await submitEmailCodeAndReset()
}

const submitPhoneMatch = () => {
  const inputPhone = (phoneInputValue.value || '').trim()
  const maskedPhone = (recoveryInfo.value.maskedPhone || '').trim()

  if (!inputPhone) {
    alert('请输入手机号')
    return
  }
  if (!REGEXP_PHONE.test(inputPhone)) {
    alert('手机号格式不正确')
    return
  }

  const maskedInput = inputPhone.substring(0, 3) + '****' + inputPhone.substring(inputPhone.length - 4)
  if (!maskedPhone || maskedInput !== maskedPhone) {
    alert('手机号不匹配，请重新输入')
    return
  }

  request.post('/login/recovery/send-phone-code', null, {
    params: { username: forgotForm.value.username }
  }).then((res) => {
    if (res.code === 200) {
      phoneVerifyCode.value = ''
      forgotStep.value = 'phone-code'
    } else {
      alert(res.msg || '验证码发送失败')
    }
  }).catch((error) => {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '验证码发送失败，请稍后重试')
  })
}

const submitPhoneCode = async () => {
  if (!phoneVerifyCode.value || !phoneVerifyCode.value.trim()) {
    alert('请输入验证码')
    return
  }
  if (!REGEXP_CODE_SIX.test(phoneVerifyCode.value.trim())) {
    alert('请输入6位数字验证码')
    return
  }

  try {
    const res = await request.post('/login/recovery/verify-phone-code', null, {
      params: {
        username: forgotForm.value.username,
        code: phoneVerifyCode.value.trim()
      }
    })

    if (res.code === 200) {
      resetPasswordForm.value = {
        newPassword: '',
        confirmPassword: ''
      }
      forgotStep.value = 'reset-password'
      return
    }
    alert(res.msg || '验证码错误或已过期')
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '验证码错误或已过期')
  }
}

const submitResetPassword = async () => {
  const newPassword = resetPasswordForm.value.newPassword?.trim()
  const confirmPassword = resetPasswordForm.value.confirmPassword?.trim()

  if (!newPassword) {
    alert('请输入新密码')
    return
  }
  if (!REGEXP_PWD.test(newPassword)) {
    alert('新密码须为6-18位，且需包含至少两类字符（数字/大小写字母/符号）')
    return
  }
  if (!confirmPassword) {
    alert('请再次输入新密码')
    return
  }
  if (newPassword !== confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }

  try {
    const res = await request.post('/login/recovery/reset-password', null, {
      params: {
        username: forgotForm.value.username,
        newPassword
      }
    })

    if (res.code === 200) {
      alert('密码重置成功，请使用新密码登录')
      loginForm.value.password = ''
      backToLogin()
      return
    }

    alert(res.msg || '密码重置失败，请稍后重试')
  } catch (error) {
    const backendMsg = error?.response?.data?.msg
    alert(backendMsg || '密码重置失败，请稍后重试')
  }
}

onUnmounted(() => {
  if (emailResendTimer) {
    clearInterval(emailResendTimer)
    emailResendTimer = null
  }
})

onMounted(() => {
  loadCaptcha()
})

if (props.startForgot) {
  isForgotPassword.value = true
  forgotStep.value = 'account'
  forgotForm.value.username = loginForm.value.username || ''
}
</script>