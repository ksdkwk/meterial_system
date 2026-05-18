<template>
  <div class="allocation-management">
    <div class="page-header">
      <h2 class="page-title">📦 物料方案分配信息</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button class="primary-btn" @click="openCreate">+ 新增物料方案分配</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按方案单号/需求单号/物料名称查询"
        @keyup.enter="fetchPlans(1)"
      />
      <select v-model="query.confirmStatus" class="query-select">
        <option value="">全部签收状态</option>
        <option :value="0">待确认</option>
        <option :value="1">已签收</option>
      </select>
      <button class="primary-btn" @click="fetchPlans(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchPlans(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="plan-table">
        <thead>
          <tr>
            <th>方案单号</th>
            <th>需求单号</th>
            <th>物料</th>
            <th>分配数量</th>
            <th>领取地点</th>
            <th>计划领取时间</th>
            <th>制定人</th>
            <th>签收状态</th>
            <th>签收人</th>
            <th>签收时间</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in plans" :key="item.planId">
            <td>{{ item.planId }}</td>
            <td>{{ item.demandId || '—' }}</td>
            <td>{{ item.materialName || item.materialId || '—' }}</td>
            <td>{{ item.allocQuantity ?? '—' }}</td>
            <td>{{ item.pickLocation || '—' }}</td>
            <td>{{ formatTime(item.pickTime) }}</td>
            <td>{{ item.allocatorName || item.allocatorId || '—' }}</td>
            <td>
              <span class="status-tag" :class="item.confirmStatus === 1 ? 'signed' : 'pending'">
                {{ item.confirmStatus === 1 ? '已签收' : '待确认' }}
              </span>
            </td>
            <td>{{ item.signUserName || item.signUserId || '—' }}</td>
            <td>{{ formatTime(item.signTime) }}</td>
            <td>{{ formatTime(item.createTime) }}</td>
          </tr>
          <tr v-if="plans.length === 0">
            <td colspan="11" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchPlans(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchPlans(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchPlans(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增物料方案分配</h3>
        <div class="form-grid">
          <label>需求单号*</label>
          <div class="field-col">
            <select v-model="createForm.demandId" class="query-select">
              <option value="">请选择需求单</option>
              <option v-if="demandOptions.length === 0" value="" disabled>
                暂无可分配需求单（需已审批通过且未分配）
              </option>
              <option v-for="d in demandOptions" :key="d.id" :value="d.id">
                {{ d.name }}
              </option>
            </select>
            <div class="manual-input-wrap">
              <input
                v-model.trim="createForm.demandIdManual"
                placeholder="或手动输入需求单号（优先）"
                @input="onDemandManualInput"
                @focus="onDemandManualInput"
                @blur="onDemandManualBlur"
              />
              <div v-if="showDemandSuggest && demandSuggestList.length > 0" class="suggest-panel">
                <div
                  v-for="d in demandSuggestList"
                  :key="d.id"
                  class="suggest-item"
                  @mousedown.prevent="pickDemandOption(d)"
                >
                  {{ d.name }}
                </div>
              </div>
            </div>
          </div>

          <label>分配数量*</label>
          <input v-model.number="createForm.allocQuantity" type="number" min="1" />

          <label>领取地点*</label>
          <input v-model.trim="createForm.pickLocation" placeholder="如：A区-发货口" />

          <label>计划领取时间*</label>
          <input v-model="createForm.pickTime" type="datetime-local" />

          <label>制定人ID*</label>
          <div class="field-col">
            <select v-model="createForm.allocatorId" class="query-select">
              <option value="">请选择制定人</option>
              <option v-for="a in allocatorOptions" :key="a.id" :value="a.id">
                {{ a.name }}（{{ a.id }}）
              </option>
            </select>
            <input v-model.trim="createForm.allocatorIdManual" placeholder="或手动输入制定人ID（优先）" />
          </div>

          <label>签收状态</label>
          <select v-model.number="createForm.confirmStatus" class="query-select">
            <option :value="0">待确认</option>
            <option :value="1">已签收</option>
          </select>

          <label>签收人ID</label>
          <input v-model.trim="createForm.signUserId" placeholder="可选" />

          <label>签收时间</label>
          <input v-model="createForm.signTime" type="datetime-local" />

          <label>签收照片URL</label>
          <input v-model.trim="createForm.signPhotoUrl" placeholder="可选" />
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

const plans = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const demandOptions = ref([])
const allocatorOptions = ref([])
const demandSuggestList = ref([])
const showDemandSuggest = ref(false)
const createForm = ref({
  demandId: '',
  demandIdManual: '',
  allocQuantity: 1,
  pickLocation: '',
  pickTime: '',
  allocatorId: '',
  allocatorIdManual: '',
  confirmStatus: 0,
  signUserId: '',
  signTime: '',
  signPhotoUrl: ''
})

const query = ref({
  keyword: '',
  confirmStatus: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const fetchPlans = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      confirmStatus: query.value.confirmStatus === '' ? undefined : Number(query.value.confirmStatus),
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/allocation-plans/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      plans.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取分配方案列表失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', confirmStatus: '' }
  fetchPlans(1)
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
    demandId: '',
    demandIdManual: '',
    allocQuantity: 1,
    pickLocation: '',
    pickTime: '',
    allocatorId: currentUserId,
    allocatorIdManual: '',
    confirmStatus: 0,
    signUserId: '',
    signTime: '',
    signPhotoUrl: ''
  }
  demandSuggestList.value = []
  showDemandSuggest.value = false
  fetchCreateOptions()
  showCreate.value = true
}

const pickDemandOption = (option) => {
  createForm.value.demandId = option.id
  createForm.value.demandIdManual = option.id
  showDemandSuggest.value = false
}

const onDemandManualInput = () => {
  const kw = (createForm.value.demandIdManual || '').trim().toLowerCase()
  if (!kw) {
    demandSuggestList.value = []
    showDemandSuggest.value = false
    return
  }

  const matched = demandOptions.value.filter((d) => {
    const id = String(d.id || '').toLowerCase()
    const name = String(d.name || '').toLowerCase()
    return id.includes(kw) || name.includes(kw)
  })

  demandSuggestList.value = matched.slice(0, 8)
  showDemandSuggest.value = demandSuggestList.value.length > 0

  const exact = matched.find((d) => String(d.id || '').toLowerCase() === kw)
  if (exact) {
    createForm.value.demandId = exact.id
  }
}

const onDemandManualBlur = () => {
  setTimeout(() => {
    showDemandSuggest.value = false
  }, 120)
}

const fetchCreateOptions = async () => {
  try {
    const [dRes, aRes] = await Promise.all([
      request.get('/allocation-plans/options/demands'),
      request.get('/allocation-plans/options/allocators')
    ])
    demandOptions.value = dRes?.code === 200 && Array.isArray(dRes?.data) ? dRes.data : []
    allocatorOptions.value = aRes?.code === 200 && Array.isArray(aRes?.data) ? aRes.data : []
  } catch (e) {
    demandOptions.value = []
    allocatorOptions.value = []
  }
}

const submitCreate = async () => {
  const manualDemandId = (createForm.value.demandIdManual || '').trim()
  let demandId = createForm.value.demandId

  if (manualDemandId) {
    const exact = demandOptions.value.find((d) => String(d.id || '') === manualDemandId)
    if (!exact) {
      alert('手动输入的需求单号无效：仅允许已审核成功且未分配的需求单')
      return
    }
    demandId = exact.id
    createForm.value.demandId = exact.id
  }

  const allocatorId = (createForm.value.allocatorIdManual || '').trim() || createForm.value.allocatorId

  if (!demandId) {
    alert('需求单号不能为空')
    return
  }
  if (!createForm.value.allocQuantity || createForm.value.allocQuantity <= 0) {
    alert('分配数量必须大于0')
    return
  }
  if (!createForm.value.pickLocation) {
    alert('领取地点不能为空')
    return
  }
  if (!createForm.value.pickTime) {
    alert('计划领取时间不能为空')
    return
  }
  if (!allocatorId) {
    alert('制定人ID不能为空')
    return
  }

  try {
    const payload = {
      demandId,
      allocQuantity: Number(createForm.value.allocQuantity),
      pickLocation: createForm.value.pickLocation,
      pickTime: createForm.value.pickTime,
      allocatorId,
      confirmStatus: Number(createForm.value.confirmStatus ?? 0),
      signUserId: createForm.value.signUserId || null,
      signTime: createForm.value.signTime || null,
      signPhotoUrl: createForm.value.signPhotoUrl || null
    }

    const res = await request.post('/allocation-plans', payload)
    if (res.code === 200) {
      alert('新增分配方案成功')
      showCreate.value = false
      fetchCreateOptions()
      fetchPlans(1)
    } else {
      alert(res.msg || '新增分配方案失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增分配方案失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  await Promise.all([fetchPlans(1), fetchCreateOptions()])
})
</script>

<style scoped>
.allocation-management {
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
  width: 320px;
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

.plan-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.plan-table thead tr {
  background: #f5f7fa;
}

.plan-table th {
  padding: 12px 14px;
  text-align: left;
  color: #5a6a82;
  font-weight: 600;
  border-bottom: 2px solid #e8edf3;
  white-space: nowrap;
}

.plan-table td {
  padding: 11px 14px;
  color: #333;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
}

.plan-table tbody tr:hover {
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

.status-tag.signed {
  background: #f6ffed;
  color: #52c41a;
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
.plain-btn {
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
  width: 880px;
  max-width: calc(100vw - 24px);
  background: #fff;
  border-radius: 10px;
  padding: 18px;
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

.form-grid .field-col {
  display: grid;
  gap: 8px;
}

.manual-input-wrap {
  position: relative;
}

.suggest-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 180px;
  overflow: auto;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 20;
}

.suggest-item {
  padding: 8px 10px;
  font-size: 13px;
  color: #22374d;
  cursor: pointer;
}

.suggest-item:hover {
  background: #f5f9ff;
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

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
