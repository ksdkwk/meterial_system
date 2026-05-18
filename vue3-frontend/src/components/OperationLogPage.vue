<template>
  <div class="op-log-page">
    <div class="page-header">
      <h2 class="page-title">📋 系统操作日志</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button class="primary-btn" @click="openCreate">+ 新增日志</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按日志ID/操作人/模块/类型/IP查询"
        @keyup.enter="fetchLogs(1)"
      />
      <select v-model="query.operationType" class="query-select">
        <option value="">全部操作类型</option>
        <option value="ADD">ADD</option>
        <option value="EDIT">EDIT</option>
        <option value="DELETE">DELETE</option>
        <option value="AUDIT">AUDIT</option>
      </select>
      <button class="primary-btn" @click="fetchLogs(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchLogs(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="log-table">
        <thead>
          <tr>
            <th>日志ID</th>
            <th>操作人ID</th>
            <th>操作人</th>
            <th>操作模块</th>
            <th>操作类型</th>
            <th>操作描述</th>
            <th>请求IP</th>
            <th>操作时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.logId">
            <td>{{ item.logId || '—' }}</td>
            <td>{{ item.userId || '系统' }}</td>
            <td>{{ item.userName || '系统' }}</td>
            <td>{{ item.operationModule || '—' }}</td>
            <td>{{ item.operationType || '—' }}</td>
            <td :title="item.operationDesc || ''">{{ limitText(item.operationDesc) }}</td>
            <td>{{ item.requestIp || '—' }}</td>
            <td>{{ formatTime(item.operateTime) }}</td>
          </tr>
          <tr v-if="logs.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchLogs(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchLogs(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchLogs(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增系统操作日志</h3>
        <div class="form-grid">
          <label>日志ID</label>
          <input v-model.trim="createForm.logId" placeholder="可不填，系统自动生成" />

          <label>操作人ID</label>
          <select v-model="createForm.userId" class="query-select">
            <option value="">系统任务（空）</option>
            <option v-for="u in userOptions" :key="u.userId" :value="u.userId">
              {{ u.userId }} · {{ u.realName || u.username || '未命名用户' }}
            </option>
          </select>

          <label>操作模块*</label>
          <input v-model.trim="createForm.operationModule" placeholder="如：库存管理" />

          <label>操作类型*</label>
          <select v-model="createForm.operationType" class="query-select">
            <option value="ADD">ADD</option>
            <option value="EDIT">EDIT</option>
            <option value="DELETE">DELETE</option>
            <option value="AUDIT">AUDIT</option>
          </select>

          <label>操作描述</label>
          <input v-model.trim="createForm.operationDesc" placeholder="如：新增库存日志" />

          <label>请求IP</label>
          <input v-model.trim="createForm.requestIp" placeholder="如：127.0.0.1" />

          <label>操作时间</label>
          <input v-model="createForm.operateTime" type="datetime-local" />
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showCreate = false">取消</button>
          <button class="primary-btn" @click="submitCreate">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const query = ref({ keyword: '', operationType: '' })
const showCreate = ref(false)
const userOptions = ref([])

const createForm = ref({
  logId: '',
  userId: '',
  operationModule: '',
  operationType: 'ADD',
  operationDesc: '',
  requestIp: '',
  operateTime: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const fetchLogs = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/operation-logs/list', {
      params: {
        keyword: query.value.keyword || undefined,
        operationType: query.value.operationType || undefined,
        pageNum: targetPage,
        pageSize: pageSize.value
      }
    })
    if (res.code === 200) {
      const page = res.data || {}
      logs.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取系统操作日志失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', operationType: '' }
  fetchLogs(1)
}

const openCreate = () => {
  createForm.value = {
    logId: '',
    userId: '',
    operationModule: '',
    operationType: 'ADD',
    operationDesc: '',
    requestIp: '',
    operateTime: ''
  }
  if (userOptions.value.length === 1 && userOptions.value[0]?.userId) {
    createForm.value.userId = userOptions.value[0].userId
  }
  showCreate.value = true
}

const getCurrentUserOption = () => {
  const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  if (!raw) return null
  try {
    const u = JSON.parse(raw)
    const userId = u?.userId || ''
    if (!userId) return null
    return {
      userId,
      realName: u?.realName || u?.name || '',
      username: u?.username || ''
    }
  } catch {
    return null
  }
}

const loadUserOptions = async () => {
  const fallbackUser = getCurrentUserOption()
  try {
    const res = await request.get('/users/list', {
      params: {
        status: 1,
        pageNum: 1,
        pageSize: 500,
        sortBy: 'createTime',
        sortOrder: 'desc'
      }
    })
    if (res.code === 200) {
      const records = Array.isArray(res?.data?.records) ? res.data.records : []
      const map = new Map(records.map(u => [u.userId, u]))
      if (fallbackUser?.userId && !map.has(fallbackUser.userId)) map.set(fallbackUser.userId, fallbackUser)
      userOptions.value = Array.from(map.values())
      return
    }
  } catch {
    // ignore and fallback
  }
  userOptions.value = fallbackUser?.userId ? [fallbackUser] : []
}

const submitCreate = async () => {
  if (!createForm.value.operationModule) return alert('操作模块不能为空')
  if (!createForm.value.operationType) return alert('操作类型不能为空')
  try {
    const payload = {
      logId: createForm.value.logId || null,
      userId: createForm.value.userId || null,
      operationModule: createForm.value.operationModule,
      operationType: createForm.value.operationType,
      operationDesc: createForm.value.operationDesc || null,
      requestIp: createForm.value.requestIp || null,
      operateTime: createForm.value.operateTime || null
    }
    const res = await request.post('/operation-logs', payload)
    if (res.code === 200) {
      alert('新增系统操作日志成功')
      showCreate.value = false
      fetchLogs(1)
    } else {
      alert(res.msg || '新增系统操作日志失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增系统操作日志失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 19)
}

const limitText = (text) => {
  const s = String(text || '')
  return s.length > 40 ? `${s.slice(0, 40)}…` : (s || '—')
}

onMounted(async () => {
  await Promise.all([fetchLogs(1), loadUserOptions()])
})
</script>

<style scoped>
.op-log-page {
  padding: 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  height: 100%;
  overflow: auto;
}

.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 28px; font-weight: 700; color: #1f3a5f; margin: 0; }
.total-info { color: #888; font-size: 13px; }

.query-bar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.query-input, .query-select { height: 34px; border: 1px solid #d8e0ea; border-radius: 6px; padding: 0 10px; }
.query-input { width: 420px; }

.loading { text-align: center; padding: 48px; color: #aaa; font-size: 15px; }
.error-box { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; }
.error-box button { padding: 4px 12px; background: #ff4d4f; color: #fff; border: none; border-radius: 4px; cursor: pointer; }

.table-wrap { overflow-x: auto; }
.log-table { width: 100%; min-width: 1180px; border-collapse: collapse; font-size: 14px; }
.log-table thead tr { background: #f5f7fa; }
.log-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.log-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.log-table tbody tr:hover { background: #f9fbff; }

.empty { text-align: center; color: #bbb; padding: 40px 0; }
.pagination { margin-top: 12px; display: flex; align-items: center; gap: 12px; }

.primary-btn, .plain-btn { border-radius: 6px; border: 1px solid #d8e0ea; cursor: pointer; }
.primary-btn { background: #1677ff; color: #fff; border-color: #1677ff; padding: 0 12px; height: 34px; }
.plain-btn { background: #fff; color: #233a59; padding: 0 12px; height: 34px; }

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}

.modal {
  width: min(760px, calc(100vw - 32px));
  max-height: calc(100vh - 48px);
  overflow: auto;
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  box-sizing: border-box;
}

.modal h3 {
  margin: 0 0 14px;
  color: #1f3a5f;
}

.form-grid {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 10px 12px;
  align-items: center;
}

.form-grid input,
.form-grid select {
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 8px 10px;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
