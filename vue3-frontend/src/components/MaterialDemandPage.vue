<template>
  <div class="demand-management">
    <div class="page-header">
      <h2 class="page-title">📋 物料需求</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button class="primary-btn" @click="openCreate">+ 新增需求</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按需求单号/申请人/物料名称查询"
        @keyup.enter="fetchDemands"
      />
      <select v-model="query.demandStatus" class="query-select">
        <option value="">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已驳回</option>
        <option :value="3">已分配</option>
        <option :value="4">已完成</option>
      </select>
      <button class="primary-btn" @click="fetchDemands">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchDemands">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="demand-table">
        <thead>
          <tr>
            <th>需求单号</th>
            <th>申请人</th>
            <th>物料名称</th>
            <th>申请数量</th>
            <th>期望使用时间</th>
            <th>提交时间</th>
            <th>审批状态</th>
            <th>驳回原因</th>
            <th>审批人</th>
            <th>审批时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in demands" :key="item.demandId">
            <td>{{ item.demandId }}</td>
            <td>{{ formatApplicant(item) }}</td>
            <td>{{ item.materialName || item.materialId || '—' }}</td>
            <td>{{ item.demandQuantity ?? '—' }}</td>
            <td>{{ formatTime(item.useTime) }}</td>
            <td>{{ formatTime(item.submitTime) }}</td>
            <td>
              <span class="status-tag" :class="statusClass(item.demandStatus)">
                {{ statusText(item.demandStatus) }}
              </span>
            </td>
            <td>{{ item.rejectReason || '—' }}</td>
            <td>{{ item.auditorName || item.auditorId || '—' }}</td>
            <td>{{ formatTime(item.auditTime) }}</td>
            <td>
              <button class="link-btn" @click="openDetail(item)">查看详情</button>
              <button
                v-if="isManager && item.demandStatus === 0"
                class="link-btn"
                style="margin-left: 10px"
                @click="openAudit(item)"
              >
                进行审批
              </button>
            </td>
          </tr>
          <tr v-if="demands.length === 0">
            <td colspan="11" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchDemands(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchDemands(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchDemands(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showDetail" class="modal-mask" @click.self="showDetail = false">
      <div class="modal">
        <h3>物料需求详情</h3>
        <div class="detail-grid">
          <div class="k">需求单号</div><div class="v">{{ currentDetail.demandId || '—' }}</div>
          <div class="k">申请人</div><div class="v">{{ formatApplicant(currentDetail) }}</div>
          <div class="k">物料</div><div class="v">{{ currentDetail.materialName || currentDetail.materialId || '—' }}</div>
          <div class="k">申请数量</div><div class="v">{{ currentDetail.demandQuantity ?? '—' }}</div>
          <div class="k">期望使用时间</div><div class="v">{{ formatTime(currentDetail.useTime) }}</div>
          <div class="k">提交时间</div><div class="v">{{ formatTime(currentDetail.submitTime) }}</div>
          <div class="k">审批状态</div>
          <div class="v">
            <span class="status-tag" :class="statusClass(currentDetail.demandStatus)">
              {{ statusText(currentDetail.demandStatus) }}
            </span>
          </div>
          <div class="k">驳回原因</div><div class="v">{{ currentDetail.rejectReason || '—' }}</div>
          <div class="k">审批人</div><div class="v">{{ currentDetail.auditorName || currentDetail.auditorId || '—' }}</div>
          <div class="k">审批时间</div><div class="v">{{ formatTime(currentDetail.auditTime) }}</div>
          <div class="k">现场图片URL</div>
          <div class="v url">{{ currentDetail.sitePhotoUrl || '—' }}</div>
        </div>
        <div class="modal-actions">
          <button class="plain-btn" @click="showDetail = false">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal create-modal">
        <h3>新增物料需求</h3>
        <div class="form-grid">
          <label>申请人ID*</label>
          <div class="field-col">
            <select v-model="createForm.demandUserId" class="query-select">
              <option value="">请选择申请人</option>
              <option v-for="u in userOptions" :key="u.id" :value="u.id">
                {{ u.name }}（{{ u.id }}）
              </option>
            </select>
            <input v-model.trim="createForm.demandUserIdManual" placeholder="或手动输入申请人ID（优先）" />
          </div>

          <label>物料ID*</label>
          <div class="field-col">
            <select v-model="createForm.materialId" class="query-select">
              <option value="">请选择物料</option>
              <option v-for="m in materialOptions" :key="m.id" :value="m.id">
                {{ m.name }}（{{ m.id }}）
              </option>
            </select>
            <input v-model.trim="createForm.materialIdManual" placeholder="或手动输入物料ID（优先）" />
          </div>

          <label>申请数量*</label>
          <input v-model.number="createForm.demandQuantity" type="number" min="1" />

          <label>期望使用时间*</label>
          <input v-model="createForm.useTime" type="datetime-local" />

          <label>现场图片URL</label>
          <input v-model.trim="createForm.sitePhotoUrl" placeholder="可选" />
        </div>
        <div class="modal-actions">
          <button class="plain-btn" @click="showCreate = false">取消</button>
          <button class="primary-btn" @click="submitCreate">提交</button>
        </div>
      </div>
    </div>

    <div v-if="showAudit" class="modal-mask" @click.self="closeAudit">
      <div class="modal">
        <h3>物料需求审批</h3>
        <div class="detail-grid">
          <div class="k">需求单号</div><div class="v">{{ auditTarget.demandId || '—' }}</div>
          <div class="k">申请人</div><div class="v">{{ formatApplicant(auditTarget) }}</div>
          <div class="k">物料</div><div class="v">{{ auditTarget.materialName || auditTarget.materialId || '—' }}</div>
          <div class="k">申请数量</div><div class="v">{{ auditTarget.demandQuantity ?? '—' }}</div>
          <div class="k">期望使用时间</div><div class="v">{{ formatTime(auditTarget.useTime) }}</div>
          <div class="k">提交时间</div><div class="v">{{ formatTime(auditTarget.submitTime) }}</div>
          <div class="k">当前状态</div>
          <div class="v">
            <span class="status-tag" :class="statusClass(auditTarget.demandStatus)">
              {{ statusText(auditTarget.demandStatus) }}
            </span>
          </div>
        </div>

        <div v-if="rejectMode" class="reject-box">
          <label>驳回理由*</label>
          <textarea
            v-model.trim="rejectReason"
            maxlength="255"
            placeholder="请输入驳回理由"
          />
        </div>

        <div class="modal-actions">
          <button class="plain-btn" :disabled="auditing" @click="closeAudit">取消</button>
          <button v-if="!rejectMode" class="danger-btn" :disabled="auditing" @click="startReject">驳回</button>
          <button v-if="!rejectMode" class="primary-btn" :disabled="auditing" @click="submitApprove">审批通过</button>
          <button v-if="rejectMode" class="plain-btn" :disabled="auditing" @click="rejectMode = false">返回</button>
          <button v-if="rejectMode" class="danger-btn" :disabled="auditing" @click="submitReject">确认驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const demands = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

const showDetail = ref(false)
const currentDetail = ref({})
const showCreate = ref(false)
const showAudit = ref(false)
const auditTarget = ref({})
const rejectMode = ref(false)
const rejectReason = ref('')
const auditing = ref(false)
const userOptions = ref([])
const materialOptions = ref([])

const createForm = ref({
  demandUserId: '',
  demandUserIdManual: '',
  materialId: '',
  materialIdManual: '',
  demandQuantity: 1,
  useTime: '',
  sitePhotoUrl: ''
})

const query = ref({
  keyword: '',
  demandStatus: ''
})

const currentUser = computed(() => {
  const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
})

const isManager = computed(() => {
  const roleCode = String(
    currentUser.value?.role?.roleCode || currentUser.value?.roleCode || currentUser.value?.rolePermission || ''
  ).toUpperCase()
  return ['MANAGER', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN', 'USER_ADMIN'].includes(roleCode)
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const fetchDemands = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const normalizedPage = Number.isFinite(Number(targetPage)) ? Number(targetPage) : pageNum.value
    const params = {
      keyword: query.value.keyword || undefined,
      demandStatus: query.value.demandStatus === '' ? undefined : Number(query.value.demandStatus),
      pageNum: normalizedPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/material-demands/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      demands.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || normalizedPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取物料需求列表失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', demandStatus: '' }
  fetchDemands(1)
}

const openDetail = (row) => {
  currentDetail.value = { ...row }
  showDetail.value = true
}

const openCreate = () => {
  const currentUserRaw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  let currentUserId = ''
  if (currentUserRaw) {
    try {
      const u = JSON.parse(currentUserRaw)
      currentUserId = u?.userId || ''
    } catch (e) {
      currentUserId = ''
    }
  }

  createForm.value = {
    demandUserId: currentUserId,
    demandUserIdManual: '',
    materialId: '',
    materialIdManual: '',
    demandQuantity: 1,
    useTime: '',
    sitePhotoUrl: ''
  }
  showCreate.value = true
}

const openAudit = (row) => {
  auditTarget.value = { ...row }
  rejectMode.value = false
  rejectReason.value = ''
  showAudit.value = true
}

const closeAudit = () => {
  if (auditing.value) return
  showAudit.value = false
  rejectMode.value = false
  rejectReason.value = ''
  auditTarget.value = {}
}

const startReject = () => {
  rejectMode.value = true
}

const doAudit = async (action) => {
  if (!auditTarget.value?.demandId) return
  auditing.value = true
  try {
    const payload = {
      demandId: auditTarget.value.demandId,
      action,
      rejectReason: action === 'REJECT' ? rejectReason.value : null
    }
    const res = await request.post('/material-demands/audit', payload)
    if (res.code === 200) {
      alert('审批操作成功')
      closeAudit()
      fetchDemands(pageNum.value)
    } else {
      alert(res.msg || '审批操作失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '审批操作失败，请检查后端服务')
  } finally {
    auditing.value = false
  }
}

const submitApprove = async () => {
  if (!window.confirm('确认审批通过该需求吗？')) return
  await doAudit('APPROVE')
}

const submitReject = async () => {
  if (!rejectReason.value) {
    alert('请输入驳回理由')
    return
  }
  await doAudit('REJECT')
}

const fetchCreateOptions = async () => {
  try {
    const [uRes, mRes] = await Promise.all([
      request.get('/material-demands/options/users'),
      request.get('/material-demands/options/materials')
    ])
    userOptions.value = uRes?.code === 200 && Array.isArray(uRes?.data) ? uRes.data : []
    materialOptions.value = mRes?.code === 200 && Array.isArray(mRes?.data) ? mRes.data : []
  } catch (e) {
    userOptions.value = []
    materialOptions.value = []
  }
}

const submitCreate = async () => {
  const demandUserId = (createForm.value.demandUserIdManual || '').trim() || createForm.value.demandUserId
  const materialId = (createForm.value.materialIdManual || '').trim() || createForm.value.materialId

  if (!demandUserId) {
    alert('申请人ID不能为空')
    return
  }
  if (!materialId) {
    alert('物料ID不能为空')
    return
  }
  if (!createForm.value.demandQuantity || createForm.value.demandQuantity <= 0) {
    alert('申请数量必须大于0')
    return
  }
  if (!createForm.value.useTime) {
    alert('期望使用时间不能为空')
    return
  }

  try {
    const payload = {
      demandUserId,
      materialId,
      demandQuantity: Number(createForm.value.demandQuantity),
      useTime: createForm.value.useTime,
      sitePhotoUrl: createForm.value.sitePhotoUrl || null
    }
    const res = await request.post('/material-demands', payload)
    if (res.code === 200) {
      alert('新增需求成功')
      showCreate.value = false
      fetchDemands(1)
    } else {
      alert(res.msg || '新增需求失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增需求失败，请检查后端服务')
  }
}

const statusText = (status) => {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已驳回',
    3: '已分配',
    4: '已完成'
  }
  return map[status] || '未知'
}

const statusClass = (status) => {
  if (status === 4) return 'done'
  if (status === 3) return 'allocated'
  if (status === 2) return 'rejected'
  if (status === 1) return 'approved'
  return 'pending'
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const formatApplicant = (row) => {
  if (!row) return '—'
  const account = row.demandUserAccount || row.demandUserName || ''
  const userId = row.demandUserId || ''
  if (account && userId) return `${account}（${userId}）`
  return account || userId || '—'
}

onMounted(async () => {
  await Promise.all([fetchDemands(), fetchCreateOptions()])
})
</script>

<style scoped>
.demand-management {
  padding: 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  height: 100%;
  overflow: auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f3a5f;
  margin: 0;
}

.total-info {
  color: #888;
  font-size: 13px;
}

.query-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.query-input,
.query-select {
  height: 34px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 0 10px;
}

.query-input {
  width: 280px;
}

.loading {
  text-align: center;
  padding: 48px;
  color: #aaa;
  font-size: 15px;
}

.error-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  color: #cf1322;
}

.error-box button {
  padding: 4px 12px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.table-wrap {
  overflow-x: auto;
}

.demand-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.demand-table thead tr {
  background: #f5f7fa;
}

.demand-table th {
  padding: 12px 14px;
  text-align: left;
  color: #5a6a82;
  font-weight: 600;
  border-bottom: 2px solid #e8edf3;
  white-space: nowrap;
}

.demand-table td {
  padding: 11px 14px;
  color: #333;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
}

.demand-table tbody tr:hover {
  background: #f9fbff;
}

.status-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-tag.approved {
  background: #f6ffed;
  color: #52c41a;
}

.status-tag.rejected {
  background: #fff2f0;
  color: #ff4d4f;
}

.status-tag.allocated {
  background: #e6f4ff;
  color: #1677ff;
}

.status-tag.done {
  background: #f0f5ff;
  color: #2f54eb;
}

.empty {
  text-align: center;
  color: #bbb;
  padding: 40px 0;
}

.pagination {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-btn,
.plain-btn,
.link-btn,
.danger-btn {
  border-radius: 6px;
  border: 1px solid #d8e0ea;
  cursor: pointer;
}

.primary-btn {
  background: #1677ff;
  color: #fff;
  border-color: #1677ff;
  padding: 0 12px;
  height: 34px;
}

.plain-btn {
  background: #fff;
  color: #233a59;
  padding: 0 12px;
  height: 34px;
}

.danger-btn {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
  padding: 0 12px;
  height: 34px;
}

.link-btn {
  border: none;
  background: transparent;
  color: #1677ff;
  padding: 0;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal {
  width: 760px;
  max-width: calc(100vw - 24px);
  background: #fff;
  border-radius: 10px;
  padding: 18px;
}

.create-modal {
  width: 980px;
  max-width: calc(100vw - 32px);
}

.modal h3 {
  margin: 0 0 14px;
  color: #233a59;
}

.form-grid {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 120px minmax(0, 1fr);
  gap: 10px 12px;
  align-items: center;
}

.form-grid > * {
  min-width: 0;
}

.form-grid input,
.form-grid .query-select {
  width: 100%;
  height: 34px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 0 10px;
  box-sizing: border-box;
}

.form-grid .field-col {
  display: grid;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 120px 1fr 120px 1fr;
  gap: 10px 12px;
  align-items: center;
}

.detail-grid .k {
  color: #66788a;
  font-size: 13px;
}

.detail-grid .v {
  color: #22374d;
  font-size: 13px;
  min-height: 20px;
}

.detail-grid .url {
  word-break: break-all;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reject-box {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.reject-box label {
  color: #66788a;
  font-size: 13px;
}

.reject-box textarea {
  width: 100%;
  min-height: 88px;
  resize: vertical;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 8px 10px;
  box-sizing: border-box;
}
</style>
