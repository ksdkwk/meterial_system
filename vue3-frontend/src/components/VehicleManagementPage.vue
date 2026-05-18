<template>
  <div class="vehicle-management">
    <div class="page-header">
      <h2 class="page-title">🚗 工地运输车辆信息管理</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="canManage" class="primary-btn" @click="openCreate">+ 新增车辆信息</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按车牌号/车型/司机/电话查询"
        @keyup.enter="fetchVehicles(1)"
      />
      <select v-model="query.status" class="query-select">
        <option value="">全部状态</option>
        <option :value="1">可用</option>
        <option :value="0">维修/停用</option>
      </select>
      <button class="primary-btn" @click="fetchVehicles(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchVehicles(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="vehicle-table">
        <thead>
          <tr>
            <th>车辆ID</th>
            <th>车牌号码</th>
            <th>车型</th>
            <th>常用司机</th>
            <th>司机电话</th>
            <th>状态</th>
            <th>录入时间</th>
            <th v-if="canManage">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in vehicles" :key="item.vehicleId">
            <td>{{ item.vehicleId }}</td>
            <td>{{ item.plateNumber || '—' }}</td>
            <td>{{ item.vehicleType || '—' }}</td>
            <td>{{ item.driverName || '—' }}</td>
            <td>{{ item.driverPhone || '—' }}</td>
            <td>
              <span class="status-tag" :class="item.status === 1 ? 'active' : 'inactive'">
                {{ item.status === 1 ? '可用' : '维修/停用' }}
              </span>
            </td>
            <td>{{ formatTime(item.createTime) }}</td>
            <td v-if="canManage">
              <button class="tiny-btn" @click="openEdit(item)">编辑</button>
              <button class="tiny-btn danger" @click="submitDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="vehicles.length === 0">
            <td :colspan="canManage ? 8 : 7" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchVehicles(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchVehicles(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchVehicles(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增车辆信息</h3>
        <div class="form-grid">
          <label>车辆ID</label>
          <input v-model.trim="createForm.vehicleId" placeholder="可不填，系统自动生成" />

          <label>车牌号码*</label>
          <input v-model.trim="createForm.plateNumber" placeholder="请输入车牌号码" />

          <label>车型</label>
          <input v-model.trim="createForm.vehicleType" placeholder="如：平板车、罐车" />

          <label>常用司机</label>
          <input v-model.trim="createForm.driverName" placeholder="请输入司机姓名" />

          <label>司机电话</label>
          <input v-model.trim="createForm.driverPhone" placeholder="请输入司机联系电话" />

          <label>状态</label>
          <select v-model.number="createForm.status" class="query-select">
            <option :value="1">可用</option>
            <option :value="0">维修/停用</option>
          </select>
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showCreate = false">取消</button>
          <button class="primary-btn" @click="submitCreate">提交</button>
        </div>
      </div>
    </div>

    <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <div class="modal">
        <h3>编辑车辆信息</h3>
        <div class="form-grid">
          <label>车辆ID</label>
          <input v-model.trim="editForm.vehicleId" disabled />

          <label>车牌号码*</label>
          <input v-model.trim="editForm.plateNumber" placeholder="请输入车牌号码" />

          <label>车型</label>
          <input v-model.trim="editForm.vehicleType" placeholder="如：平板车、罐车" />

          <label>常用司机</label>
          <input v-model.trim="editForm.driverName" placeholder="请输入司机姓名" />

          <label>司机电话</label>
          <input v-model.trim="editForm.driverPhone" placeholder="请输入司机联系电话" />

          <label>状态</label>
          <select v-model.number="editForm.status" class="query-select">
            <option :value="1">可用</option>
            <option :value="0">维修/停用</option>
          </select>
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showEdit = false">取消</button>
          <button class="primary-btn" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const vehicles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const showEdit = ref(false)

const createForm = ref({
  vehicleId: '',
  plateNumber: '',
  vehicleType: '',
  driverName: '',
  driverPhone: '',
  status: 1
})

const editForm = ref({
  vehicleId: '',
  plateNumber: '',
  vehicleType: '',
  driverName: '',
  driverPhone: '',
  status: 1
})

const query = ref({ keyword: '', status: '' })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

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

const fetchVehicles = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      status: query.value.status === '' ? undefined : Number(query.value.status),
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/vehicles/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      vehicles.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取车辆信息失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', status: '' }
  fetchVehicles(1)
}

const openCreate = () => {
  createForm.value = {
    vehicleId: '',
    plateNumber: '',
    vehicleType: '',
    driverName: '',
    driverPhone: '',
    status: 1
  }
  showCreate.value = true
}

const submitCreate = async () => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可新增车辆')
    return
  }
  if (!createForm.value.plateNumber) {
    alert('车牌号码不能为空')
    return
  }

  try {
    const payload = {
      vehicleId: createForm.value.vehicleId || null,
      plateNumber: createForm.value.plateNumber,
      vehicleType: createForm.value.vehicleType || null,
      driverName: createForm.value.driverName || null,
      driverPhone: createForm.value.driverPhone || null,
      status: Number(createForm.value.status ?? 1)
    }
    const res = await request.post('/vehicles', payload)
    if (res.code === 200) {
      alert('新增车辆成功')
      showCreate.value = false
      fetchVehicles(1)
    } else {
      alert(res.msg || '新增车辆失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增车辆失败，请检查后端服务')
  }
}

const openEdit = (item) => {
  editForm.value = {
    vehicleId: item.vehicleId || '',
    plateNumber: item.plateNumber || '',
    vehicleType: item.vehicleType || '',
    driverName: item.driverName || '',
    driverPhone: item.driverPhone || '',
    status: Number(item.status ?? 1)
  }
  showEdit.value = true
}

const submitEdit = async () => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可编辑车辆')
    return
  }
  if (!editForm.value.vehicleId) {
    alert('车辆ID不能为空')
    return
  }
  if (!editForm.value.plateNumber) {
    alert('车牌号码不能为空')
    return
  }

  try {
    const payload = {
      plateNumber: editForm.value.plateNumber,
      vehicleType: editForm.value.vehicleType || null,
      driverName: editForm.value.driverName || null,
      driverPhone: editForm.value.driverPhone || null,
      status: Number(editForm.value.status ?? 1)
    }
    const res = await request.put(`/vehicles/${editForm.value.vehicleId}`, payload)
    if (res.code === 200) {
      alert('更新车辆成功')
      showEdit.value = false
      fetchVehicles(pageNum.value)
    } else {
      alert(res.msg || '更新车辆失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '更新车辆失败，请检查后端服务')
  }
}

const submitDelete = async (item) => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可删除车辆')
    return
  }
  if (!item?.vehicleId) {
    alert('车辆ID不能为空')
    return
  }
  if (!confirm(`确定删除车辆【${item.plateNumber || item.vehicleId}】吗？`)) {
    return
  }

  try {
    const res = await request.delete(`/vehicles/${item.vehicleId}`)
    if (res.code === 200) {
      alert('删除车辆成功')
      fetchVehicles(1)
    } else {
      alert(res.msg || '删除车辆失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '删除车辆失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

onMounted(() => fetchVehicles(1))
</script>

<style scoped>
.vehicle-management {
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
.query-input { width: 320px; }

.loading { text-align: center; padding: 48px; color: #aaa; font-size: 15px; }
.error-box { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; }
.error-box button { padding: 4px 12px; background: #ff4d4f; color: #fff; border: none; border-radius: 4px; cursor: pointer; }

.table-wrap { overflow-x: auto; }
.vehicle-table { width: 100%; min-width: 1000px; border-collapse: collapse; font-size: 14px; }
.vehicle-table thead tr { background: #f5f7fa; }
.vehicle-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.vehicle-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.vehicle-table tbody tr:hover { background: #f9fbff; }

.status-tag { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-tag.active { background: #f6ffed; color: #52c41a; }
.status-tag.inactive { background: #fff2f0; color: #ff4d4f; }

.empty { text-align: center; color: #bbb; padding: 40px 0; }
.pagination { margin-top: 12px; display: flex; align-items: center; gap: 12px; }

.primary-btn, .plain-btn { border-radius: 6px; border: 1px solid #d8e0ea; cursor: pointer; }
.primary-btn { background: #1677ff; color: #fff; border-color: #1677ff; padding: 0 12px; height: 34px; }
.plain-btn { background: #fff; color: #233a59; padding: 0 12px; height: 34px; }

.tiny-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #233a59;
  cursor: pointer;
  margin-right: 8px;
}

.tiny-btn.danger {
  border-color: #ffccc7;
  color: #cf1322;
}

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
  grid-template-columns: 120px minmax(0, 1fr);
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
