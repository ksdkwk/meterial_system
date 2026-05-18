<template>
  <div class="transport-management">
    <div class="page-header">
      <h2 class="page-title">🚚 物料运输信息</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="canManage" class="primary-btn" @click="openCreate">+ 新增物料运输信息</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按运输单号/方案单号/司机/车牌查询"
        @keyup.enter="fetchRecords(1)"
      />
      <select v-model="query.transportStatus" class="query-select">
        <option value="">全部运输状态</option>
        <option :value="0">未发车</option>
        <option :value="1">运输中</option>
        <option :value="2">已到达</option>
        <option :value="3">运输完成</option>
      </select>
      <button class="primary-btn" @click="fetchRecords(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchRecords(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="record-table">
        <thead>
          <tr>
            <th>运输单号</th>
            <th>方案单号</th>
            <th>需求单号</th>
            <th>物料</th>
            <th>车辆</th>
            <th>司机</th>
            <th>电话</th>
            <th>状态</th>
            <th>出发时间</th>
            <th>预计到达</th>
            <th>实际到达</th>
            <th>位置(纬度/经度)</th>
            <th>位置更新时间</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in records" :key="item.recordId">
            <td>{{ item.recordId }}</td>
            <td>{{ item.planId || '—' }}</td>
            <td>{{ item.demandId || '—' }}</td>
            <td>{{ item.materialName || '—' }}</td>
            <td>{{ item.plateNumber || item.vehicleId || '—' }}</td>
            <td>{{ item.driverName || '—' }}</td>
            <td>{{ item.driverPhone || '—' }}</td>
            <td>
              <span class="status-tag" :class="statusClass(item.transportStatus)">
                {{ statusText(item.transportStatus) }}
              </span>
            </td>
            <td>{{ formatTime(item.departTime) }}</td>
            <td>{{ formatTime(item.expectArriveTime) }}</td>
            <td>{{ formatTime(item.actualArriveTime) }}</td>
            <td>{{ formatCoord(item.currentLat, item.currentLng) }}</td>
            <td>{{ formatTime(item.lastUpdateTime) }}</td>
            <td>{{ formatTime(item.createTime) }}</td>
          </tr>
          <tr v-if="records.length === 0">
            <td colspan="14" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchRecords(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchRecords(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchRecords(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增物料运输信息</h3>
        <div class="form-grid">
          <label>方案单号*</label>
          <div class="searchable-select">
            <input
              v-model.trim="createForm.planId"
              placeholder="如：plan001（可输入或下拉选择）"
              @focus="planDropdownOpen = true"
              @input="planDropdownOpen = true"
              @keydown.esc="planDropdownOpen = false"
              @blur="onPlanInputBlur"
            />
            <div v-if="planDropdownOpen" class="searchable-select-dropdown">
              <div
                v-for="p in filteredPlanOptions"
                :key="`plan-${p.id}`"
                class="searchable-select-option"
                @mousedown.prevent="selectPlanOption(p)"
              >
                <span class="option-main">{{ p.id }}</span>
                <span class="option-sub">{{ p.name }}</span>
              </div>
              <div v-if="filteredPlanOptions.length === 0" class="searchable-select-empty">无匹配方案，可直接输入</div>
            </div>
          </div>

          <label>车辆ID/车牌号</label>
          <input
            v-model.trim="createForm.vehicleId"
            list="transport-vehicle-options"
            placeholder="可选：可输入或下拉选择"
            @change="handleVehicleChange"
          />

          <label>司机姓名</label>
          <input
            v-model.trim="createForm.driverName"
            list="transport-driver-options"
            placeholder="可选：可输入或下拉选择"
            @change="handleDriverChange"
          />

          <label>司机电话</label>
          <input v-model.trim="createForm.driverPhone" placeholder="可选（下拉司机可自动带出）" />

          <label>出发时间</label>
          <input v-model="createForm.departTime" type="datetime-local" />

          <label>预计到达</label>
          <input v-model="createForm.expectArriveTime" type="datetime-local" />

          <label>运输状态</label>
          <select v-model.number="createForm.transportStatus" class="query-select">
            <option :value="0">未发车</option>
            <option :value="1">运输中</option>
            <option :value="2">已到达</option>
            <option :value="3">运输完成</option>
          </select>
        </div>

        <datalist id="transport-vehicle-options">
          <option
            v-for="v in vehicleOptions"
            :key="`vehicle-${v.vehicleId}`"
            :value="v.plateNumber || v.vehicleId"
          >
            {{ `${v.plateNumber || '-'} / ${v.driverName || '-'} / ${v.driverPhone || '-'}` }}
          </option>
          <option
            v-for="v in vehicleOptions"
            :key="`vehicle-id-${v.vehicleId}`"
            :value="v.vehicleId"
          >
            {{ `${v.vehicleId} / ${v.plateNumber || '-'} / ${v.driverName || '-'}` }}
          </option>
        </datalist>

        <datalist id="transport-driver-options">
          <option v-for="d in driverOptions" :key="`driver-${d}`" :value="d" />
        </datalist>

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

const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const planDropdownOpen = ref(false)
const availablePlanOptions = ref([])
const vehicleOptions = ref([])

const createForm = ref({
  planId: '',
  vehicleId: '',
  driverName: '',
  driverPhone: '',
  departTime: '',
  expectArriveTime: '',
  transportStatus: 0
})

const query = ref({
  keyword: '',
  transportStatus: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const filteredPlanOptions = computed(() => {
  const keyword = norm(createForm.value.planId).toLowerCase()
  const src = Array.isArray(availablePlanOptions.value) ? availablePlanOptions.value : []
  if (!keyword) return src.slice(0, 20)
  return src
    .filter((p) => {
      const id = norm(p?.id).toLowerCase()
      const name = norm(p?.name).toLowerCase()
      return id.includes(keyword) || name.includes(keyword)
    })
    .slice(0, 20)
})
const driverOptions = computed(() => {
  const set = new Set()
  for (const v of vehicleOptions.value) {
    const name = String(v?.driverName || '').trim()
    if (name) set.add(name)
  }
  return Array.from(set)
})

const canManage = computed(() => {
  const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  if (!raw) return false
  try {
    const u = JSON.parse(raw)
    const roleCode = String(u?.role?.roleCode || u?.roleCode || u?.rolePermission || '').toUpperCase()
    const roleName = String(u?.role?.roleName || u?.roleName || '').trim()
    return ['MANAGER', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN', 'WAREHOUSE', 'WAREHOUSE_ADMIN', 'USER_WAREHOUSE'].includes(roleCode)
      || ['管理员', '系统管理员', '超级管理员', '仓库管理员'].includes(roleName)
  } catch (e) {
    return false
  }
})

const fetchRecords = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      transportStatus: query.value.transportStatus === '' ? undefined : Number(query.value.transportStatus),
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/transport-records/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      records.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取运输记录失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', transportStatus: '' }
  fetchRecords(1)
}

const openCreate = () => {
  createForm.value = {
    planId: '',
    vehicleId: '',
    driverName: '',
    driverPhone: '',
    departTime: '',
    expectArriveTime: '',
    transportStatus: 0
  }
  planDropdownOpen.value = false
  loadCreateOptions()
  showCreate.value = true
}

const norm = (v) => String(v == null ? '' : v).trim()

const findVehicleByVehicleInput = (value) => {
  const val = norm(value)
  if (!val) return null
  return vehicleOptions.value.find((v) => norm(v.vehicleId) === val || norm(v.plateNumber) === val) || null
}

const findVehicleByDriverName = (value) => {
  const val = norm(value)
  if (!val) return null
  return vehicleOptions.value.find((v) => norm(v.driverName) === val) || null
}

const handleVehicleChange = () => {
  const matched = findVehicleByVehicleInput(createForm.value.vehicleId)
  if (!matched) return
  createForm.value.vehicleId = norm(matched.plateNumber || matched.vehicleId)
  if (norm(matched.driverName)) createForm.value.driverName = norm(matched.driverName)
  if (norm(matched.driverPhone)) createForm.value.driverPhone = norm(matched.driverPhone)
}

const handleDriverChange = () => {
  const matched = findVehicleByDriverName(createForm.value.driverName)
  if (!matched) return
  if (norm(matched.plateNumber || matched.vehicleId)) {
    createForm.value.vehicleId = norm(matched.plateNumber || matched.vehicleId)
  }
  if (norm(matched.driverPhone)) createForm.value.driverPhone = norm(matched.driverPhone)
}

const selectPlanOption = (option) => {
  createForm.value.planId = norm(option?.id)
  planDropdownOpen.value = false
}

const onPlanInputBlur = () => {
  setTimeout(() => {
    planDropdownOpen.value = false
  }, 120)
}

const loadCreateOptions = async () => {
  try {
    const [planRes, vehicleRes] = await Promise.all([
      request.get('/transport-records/options/plans'),
      request.get('/transport-records/options/vehicles')
    ])
    availablePlanOptions.value = planRes?.code === 200 && Array.isArray(planRes?.data) ? planRes.data : []
    vehicleOptions.value = vehicleRes?.code === 200 && Array.isArray(vehicleRes?.data) ? vehicleRes.data : []
  } catch (e) {
    availablePlanOptions.value = []
    vehicleOptions.value = []
  }
}

const submitCreate = async () => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可新增运输记录')
    return
  }
  if (!createForm.value.planId) {
    alert('方案单号不能为空')
    return
  }

  try {
    const payload = {
      planId: createForm.value.planId,
      vehicleId: createForm.value.vehicleId || null,
      driverName: createForm.value.driverName || null,
      driverPhone: createForm.value.driverPhone || null,
      departTime: createForm.value.departTime || null,
      expectArriveTime: createForm.value.expectArriveTime || null,
      transportStatus: Number(createForm.value.transportStatus ?? 0)
    }
    const res = await request.post('/transport-records', payload)
    if (res.code === 200) {
      alert('新增运输记录成功')
      showCreate.value = false
      planDropdownOpen.value = false
      fetchRecords(1)
    } else {
      alert(res.msg || '新增运输记录失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增运输记录失败，请检查后端服务')
  }
}

const statusText = (s) => {
  const map = { 0: '未发车', 1: '运输中', 2: '已到达', 3: '运输完成' }
  return map[s] || '未知'
}

const statusClass = (s) => {
  if (s === 3) return 'done'
  if (s === 2) return 'arrived'
  if (s === 1) return 'onway'
  return 'pending'
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const formatCoord = (lat, lng) => {
  if (lat == null || lng == null) return '—'
  return `${lat} / ${lng}`
}

onMounted(async () => {
  await Promise.all([fetchRecords(1), loadCreateOptions()])
})
</script>

<style scoped>
.transport-management {
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

.record-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  min-width: 1500px;
}

.record-table thead tr {
  background: #f5f7fa;
}

.record-table th {
  padding: 12px 14px;
  text-align: left;
  color: #5a6a82;
  font-weight: 600;
  border-bottom: 2px solid #e8edf3;
  white-space: nowrap;
}

.record-table td {
  padding: 11px 14px;
  color: #333;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
}

.record-table tbody tr:hover {
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

.status-tag.onway {
  background: #e6f4ff;
  color: #1677ff;
}

.status-tag.arrived {
  background: #f0f5ff;
  color: #2f54eb;
}

.status-tag.done {
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

.searchable-select {
  position: relative;
  width: 100%;
}

.searchable-select > input {
  width: 100%;
  height: 34px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 0 10px;
  box-sizing: border-box;
}

.searchable-select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 34, 58, 0.12);
  z-index: 30;
}

.searchable-select-option {
  padding: 8px 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.searchable-select-option:hover {
  background: #f5f9ff;
}

.option-main {
  color: #1f3a5f;
  font-weight: 600;
  line-height: 1.2;
}

.option-sub {
  color: #7b8da8;
  font-size: 12px;
  line-height: 1.2;
}

.searchable-select-empty {
  padding: 10px;
  color: #8a9bb5;
  font-size: 12px;
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
