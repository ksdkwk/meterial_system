<template>
  <div class="material-management">
    <div class="page-header">
      <h2 class="page-title">📦 物料基础信息管理</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button class="primary-btn" @click="openCreate">+ 新增物料</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按物料名称/类型查询"
        @keyup.enter="fetchMaterials"
      />
      <select v-model="query.status" class="query-select">
        <option value="">全部状态</option>
        <option :value="1">启用</option>
        <option :value="0">停用</option>
      </select>
      <button class="primary-btn" @click="fetchMaterials">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchMaterials">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="material-table">
        <thead>
          <tr>
            <th>物料ID</th>
            <th>物料名称</th>
            <th>规格型号</th>
            <th>物料类型</th>
            <th>单位</th>
            <th>单价(元)</th>
            <th>默认存储位置</th>
            <th>阈值(最小/最大)</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in materials" :key="item.materialId">
            <td>{{ item.materialId }}</td>
            <td>{{ item.materialName || '—' }}</td>
            <td>{{ item.specModel || '—' }}</td>
            <td>{{ item.materialType || '—' }}</td>
            <td>{{ item.unit || '—' }}</td>
            <td>{{ item.unitPrice ?? '—' }}</td>
            <td>{{ item.storageLocation || '—' }}</td>
            <td>{{ item.minThreshold ?? 0 }} / {{ item.maxThreshold ?? 0 }}</td>
            <td>
              <span class="status-tag" :class="item.status === 1 ? 'active' : 'inactive'">
                {{ item.status === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td>{{ formatTime(item.updateTime) }}</td>
            <td>
              <button class="link-btn" @click="openEdit(item)">编辑</button>
              <button
                v-if="item.status === 1"
                class="link-btn danger"
                @click="disableMaterial(item)"
              >
                停用
              </button>
              <span v-else class="disabled-text">已停用</span>
            </td>
          </tr>
          <tr v-if="materials.length === 0">
            <td colspan="11" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <h3>{{ isEdit ? '编辑物料' : '新增物料' }}</h3>
        <div class="form-grid">
          <label>物料名称*</label>
          <input v-model.trim="form.materialName" placeholder="请输入物料名称" />

          <label>规格型号</label>
          <input v-model.trim="form.specModel" placeholder="如：HRB400E Φ20" />

          <label>物料类型</label>
          <input v-model.trim="form.materialType" placeholder="如：钢筋、水泥" />

          <label>单位*</label>
          <input v-model.trim="form.unit" placeholder="吨/千块/米" />

          <label>单价(元)</label>
          <input v-model.number="form.unitPrice" type="number" min="0" step="0.01" />

          <label>供应商ID</label>
          <input v-model.trim="form.supplierId" placeholder="可选" />

          <label>默认存储位置</label>
          <input v-model.trim="form.storageLocation" placeholder="可选" />

          <label>最低阈值</label>
          <input v-model.number="form.minThreshold" type="number" min="0" />

          <label>最高阈值</label>
          <input v-model.number="form.maxThreshold" type="number" min="0" />

          <label>图片URL</label>
          <input v-model.trim="form.imageUrl" placeholder="可选" />

          <label>状态</label>
          <select v-model.number="form.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="closeModal">取消</button>
          <button class="primary-btn" @click="submitForm">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const materials = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

const query = ref({
  keyword: '',
  status: ''
})

const showModal = ref(false)
const isEdit = ref(false)
const form = ref(getDefaultForm())

function getDefaultForm() {
  return {
    materialId: '',
    materialName: '',
    specModel: '',
    materialType: '',
    unit: '',
    unitPrice: null,
    supplierId: '',
    storageLocation: '',
    minThreshold: 0,
    maxThreshold: 0,
    imageUrl: '',
    status: 1
  }
}

const fetchMaterials = async () => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      status: query.value.status === '' ? undefined : Number(query.value.status)
    }
    const res = await request.get('/materials/list', { params })
    if (res.code === 200) {
      materials.value = Array.isArray(res.data) ? res.data : []
      total.value = materials.value.length
    } else {
      error.value = res.msg || '获取物料列表失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', status: '' }
  fetchMaterials()
}

const openCreate = () => {
  isEdit.value = false
  form.value = getDefaultForm()
  showModal.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  form.value = {
    materialId: row.materialId,
    materialName: row.materialName || '',
    specModel: row.specModel || '',
    materialType: row.materialType || '',
    unit: row.unit || '',
    unitPrice: row.unitPrice ?? null,
    supplierId: row.supplierId || '',
    storageLocation: row.storageLocation || '',
    minThreshold: row.minThreshold ?? 0,
    maxThreshold: row.maxThreshold ?? 0,
    imageUrl: row.imageUrl || '',
    status: row.status ?? 1
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const validateForm = () => {
  if (!form.value.materialName?.trim()) return '物料名称不能为空'
  if (!form.value.unit?.trim()) return '单位不能为空'
  const min = Number(form.value.minThreshold ?? 0)
  const max = Number(form.value.maxThreshold ?? 0)
  if (min < 0 || max < 0) return '阈值不能为负数'
  if (min > max) return '最低阈值不能大于最高阈值'
  return null
}

const buildPayload = () => ({
  materialName: form.value.materialName?.trim(),
  specModel: form.value.specModel?.trim() || null,
  materialType: form.value.materialType?.trim() || null,
  unit: form.value.unit?.trim(),
  unitPrice: form.value.unitPrice === null || form.value.unitPrice === '' ? null : Number(form.value.unitPrice),
  supplierId: form.value.supplierId?.trim() || null,
  storageLocation: form.value.storageLocation?.trim() || null,
  minThreshold: Number(form.value.minThreshold ?? 0),
  maxThreshold: Number(form.value.maxThreshold ?? 0),
  imageUrl: form.value.imageUrl?.trim() || null,
  status: Number(form.value.status ?? 1)
})

const submitForm = async () => {
  const msg = validateForm()
  if (msg) {
    alert(msg)
    return
  }

  try {
    const payload = buildPayload()
    let res
    if (isEdit.value) {
      res = await request.put(`/materials/${form.value.materialId}`, payload)
    } else {
      res = await request.post('/materials', payload)
    }

    if (res.code === 200) {
      alert(isEdit.value ? '修改成功' : '新增成功')
      closeModal()
      fetchMaterials()
    } else {
      alert(res.msg || '操作失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '提交失败，请检查后端服务')
  }
}

const disableMaterial = async (row) => {
  if (!confirm(`确定停用物料 ${row.materialName} 吗？`)) return
  try {
    const res = await request.delete(`/materials/${row.materialId}`)
    if (res.code === 200) {
      alert('停用成功')
      fetchMaterials()
    } else {
      alert(res.msg || '停用失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '停用失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

onMounted(fetchMaterials)
</script>

<style scoped>
.material-management {
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
  margin-right: auto;
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
  width: 260px;
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

.material-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.material-table thead tr {
  background: #f5f7fa;
}

.material-table th {
  padding: 12px 14px;
  text-align: left;
  color: #5a6a82;
  font-weight: 600;
  border-bottom: 2px solid #e8edf3;
  white-space: nowrap;
}

.material-table td {
  padding: 11px 14px;
  color: #333;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
}

.material-table tbody tr:hover {
  background: #f9fbff;
}

.status-tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.active {
  background: #f6ffed;
  color: #52c41a;
}

.status-tag.inactive {
  background: #fff2f0;
  color: #ff4d4f;
}

.empty {
  text-align: center;
  color: #bbb;
  padding: 40px 0;
}

.primary-btn,
.plain-btn,
.link-btn {
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

.link-btn {
  border: none;
  background: transparent;
  padding: 0;
  margin-right: 10px;
  color: #1677ff;
  font-size: 13px;
}

.link-btn.danger {
  color: #ff4d4f;
}

.disabled-text {
  color: #bcbcbc;
  font-size: 12px;
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
  width: 780px;
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
  grid-template-columns: 120px 1fr 120px 1fr;
  gap: 10px 12px;
  align-items: center;
}

.form-grid input,
.form-grid select {
  height: 34px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 0 10px;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>