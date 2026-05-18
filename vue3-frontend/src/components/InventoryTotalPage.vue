<template>
  <div class="inventory-total">
    <div class="page-header">
      <h2 class="page-title">📦 库存总量</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="canManage" class="primary-btn" @click="openCreate">+ 新增库存</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按库存ID/物料ID/物料名/库位查询"
        @keyup.enter="fetchInventories(1)"
      />
      <button class="primary-btn" @click="fetchInventories(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchInventories(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="inventory-table">
        <thead>
          <tr>
            <th>库存ID</th>
            <th>物料ID</th>
            <th>物料名称</th>
            <th>存储位置</th>
            <th>库存数量</th>
            <th>最后盘点时间</th>
            <th>盘点人ID</th>
            <th>盘点人</th>
            <th>更新时间</th>
            <th v-if="canManage">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in inventories" :key="item.inventoryId">
            <td>{{ item.inventoryId }}</td>
            <td>{{ item.materialId || '—' }}</td>
            <td>{{ item.materialName || '—' }}</td>
            <td>{{ item.storageLocation || '—' }}</td>
            <td>{{ item.stockQuantity ?? 0 }}</td>
            <td>{{ formatTime(item.lastCheckTime) }}</td>
            <td>{{ item.lastCheckerId || '—' }}</td>
            <td>{{ item.lastCheckerName || '—' }}</td>
            <td>{{ formatTime(item.updateTime) }}</td>
            <td v-if="canManage">
              <button class="tiny-btn" @click="openEdit(item)">编辑</button>
              <button class="tiny-btn danger" @click="submitDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="inventories.length === 0">
            <td :colspan="canManage ? 10 : 9" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchInventories(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchInventories(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchInventories(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增库存</h3>
        <div class="form-grid">
          <label>库存ID</label>
          <input v-model.trim="createForm.inventoryId" placeholder="可不填，系统自动生成" />

          <label>物料ID*</label>
          <div class="material-picker" ref="materialPickerRef">
            <div class="material-input-group">
              <input
                v-model.trim="createForm.materialId"
                placeholder="请输入物料ID（可手动输入）"
                @focus="onMaterialFieldFocus('id')"
                @input="onMaterialFieldInput('id')"
              />
            </div>
            <div v-if="showMaterialDropdown && materialSearchKeyword.length > 0" class="material-dropdown-panel">
              <div
                v-for="m in filteredMaterialOptions"
                :key="m.materialId"
                class="material-option"
                @click="selectMaterialOption(m)"
              >
                {{ m.materialId }} · {{ m.materialName || '未命名物料' }}
              </div>
              <div v-if="filteredMaterialOptions.length === 0" class="material-empty">无匹配物料</div>
            </div>
          </div>

          <label>物料名称</label>
          <div class="material-picker" ref="materialNamePickerRef">
            <div class="material-input-group">
              <input
                v-model.trim="createForm.materialName"
                placeholder="非必填，输入后可联想匹配物料ID+名称"
                @focus="onMaterialFieldFocus('name')"
                @input="onMaterialFieldInput('name')"
              />
            </div>
            <div v-if="showMaterialDropdown && materialSearchKeyword.length > 0" class="material-dropdown-panel">
              <div
                v-for="m in filteredMaterialOptions"
                :key="`name-${m.materialId}`"
                class="material-option"
                @click="selectMaterialOption(m)"
              >
                {{ m.materialId }} · {{ m.materialName || '未命名物料' }}
              </div>
              <div v-if="filteredMaterialOptions.length === 0" class="material-empty">无匹配物料</div>
            </div>
          </div>

          <label>存储位置*</label>
          <input v-model.trim="createForm.storageLocation" placeholder="如：A区-01货架" />

          <label>库存数量*</label>
          <input v-model.number="createForm.stockQuantity" type="number" min="0" />

          <label>最后盘点时间</label>
          <input v-model="createForm.lastCheckTime" type="datetime-local" />

          <label>盘点人ID</label>
          <input v-model.trim="createForm.lastCheckerId" placeholder="可选" />
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showCreate = false">取消</button>
          <button class="primary-btn" @click="submitCreate">提交</button>
        </div>
      </div>
    </div>

    <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <div class="modal">
        <h3>编辑库存</h3>
        <div class="form-grid">
          <label>库存ID</label>
          <input v-model.trim="editForm.inventoryId" disabled />

          <label>物料ID*</label>
          <input v-model.trim="editForm.materialId" placeholder="请输入物料ID" />

          <label>存储位置*</label>
          <input v-model.trim="editForm.storageLocation" placeholder="如：A区-01货架" />

          <label>库存数量*</label>
          <input v-model.number="editForm.stockQuantity" type="number" min="0" />

          <label>最后盘点时间</label>
          <input v-model="editForm.lastCheckTime" type="datetime-local" />

          <label>盘点人ID</label>
          <input v-model.trim="editForm.lastCheckerId" placeholder="可选" />
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showEdit = false">取消</button>
          <button class="primary-btn" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showDuplicateModal" class="modal-mask" @click.self="cancelDuplicateCreate">
      <div class="modal duplicate-modal">
        <h3>重复增加提示</h3>
        <p class="duplicate-tip">检测到同物料ID与同存储位置已存在库存记录。点击“继续新增”后，将把本次数量累计写入已有库存。</p>

        <div class="duplicate-section">
          <div class="duplicate-title">当前新增</div>
          <div class="duplicate-current">
            <div>名称：{{ duplicateCreatePreview.materialName }}</div>
            <div>ID：{{ duplicateCreatePreview.materialId }}</div>
            <div>位置：{{ duplicateCreatePreview.storageLocation }}</div>
            <div>数量：{{ duplicateCreatePreview.stockQuantity }}</div>
          </div>
        </div>

        <div class="duplicate-section">
          <div class="duplicate-title">库存中已有</div>
          <div class="table-wrap">
            <table class="inventory-table duplicate-table">
              <thead>
                <tr>
                  <th>物料名称</th>
                  <th>物料ID</th>
                  <th>位置</th>
                  <th>数量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in duplicateRows" :key="`dup-${row.inventoryId || row.materialId}-${row.storageLocation}`">
                  <td>{{ row.materialName || '—' }}</td>
                  <td>{{ row.materialId || '—' }}</td>
                  <td>{{ row.storageLocation || '—' }}</td>
                  <td>{{ Number(row.stockQuantity ?? 0) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-actions">
          <button class="plain-btn" :disabled="duplicateSubmitting" @click="cancelDuplicateCreate">取消</button>
          <button class="primary-btn" :disabled="duplicateSubmitting" @click="confirmDuplicateCreate">
            {{ duplicateSubmitting ? '提交中...' : '继续新增' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import request from '@/utils/request'

const inventories = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const showEdit = ref(false)
const showDuplicateModal = ref(false)
const duplicateSubmitting = ref(false)
const materialOptions = ref([])
const showMaterialDropdown = ref(false)
const activeMaterialField = ref('id')
const materialPickerRef = ref(null)
const materialNamePickerRef = ref(null)
const duplicateRows = ref([])
const duplicateCreatePayload = ref(null)
const duplicateCreatePreview = ref({
  materialName: '—',
  materialId: '',
  storageLocation: '',
  stockQuantity: 0
})

const createForm = ref({
  inventoryId: '',
  materialId: '',
  materialName: '',
  storageLocation: '',
  stockQuantity: 0,
  lastCheckTime: '',
  lastCheckerId: ''
})

const editForm = ref({
  inventoryId: '',
  materialId: '',
  storageLocation: '',
  stockQuantity: 0,
  lastCheckTime: '',
  lastCheckerId: ''
})

const query = ref({ keyword: '' })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const materialSearchKeyword = computed(() => {
  const keyFromActive = activeMaterialField.value === 'name'
    ? normalizeText(createForm.value.materialName)
    : normalizeText(createForm.value.materialId)
  if (keyFromActive) return keyFromActive
  const keyFromName = normalizeText(createForm.value.materialName)
  if (keyFromName) return keyFromName
  return normalizeText(createForm.value.materialId)
})

const filteredMaterialOptions = computed(() => {
  const keyword = materialSearchKeyword.value.toLowerCase()
  if (!keyword) return materialOptions.value
  return materialOptions.value.filter((m) => {
    const id = String(m.materialId || '').toLowerCase()
    const name = String(m.materialName || '').toLowerCase()
    return id.includes(keyword) || name.includes(keyword)
  })
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

const fetchInventories = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/inventories/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      inventories.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取库存总量失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const fetchMaterialOptions = async () => {
  try {
    const res = await request.get('/materials/list', { params: { status: 1 } })
    if (res.code === 200) {
      materialOptions.value = Array.isArray(res.data) ? res.data : []
    } else {
      materialOptions.value = []
    }
  } catch {
    materialOptions.value = []
  }
}

const resetQuery = () => {
  query.value = { keyword: '' }
  fetchInventories(1)
}

const normalizeText = (v) => String(v || '').trim()

const resolveMaterialName = (materialId) => {
  const id = normalizeText(materialId)
  if (!id) return '—'
  const same = inventories.value.find((x) => normalizeText(x.materialId) === id)
  return same?.materialName || '—'
}

const findDuplicateInventories = (materialId, storageLocation) => {
  const mId = normalizeText(materialId)
  const loc = normalizeText(storageLocation)
  if (!mId || !loc) return []
  return inventories.value.filter((x) => {
    return normalizeText(x.materialId) === mId && normalizeText(x.storageLocation) === loc
  })
}

const cancelDuplicateCreate = () => {
  showDuplicateModal.value = false
  duplicateSubmitting.value = false
  duplicateRows.value = []
  duplicateCreatePayload.value = null
  duplicateCreatePreview.value = {
    materialName: '—',
    materialId: '',
    storageLocation: '',
    stockQuantity: 0
  }
}

const executeCreate = async (payload) => {
  try {
    const res = await request.post('/inventories', payload)
    if (res.code === 200) {
      alert('新增库存成功')
      showCreate.value = false
      cancelDuplicateCreate()
      fetchInventories(1)
    } else {
      alert(res.msg || '新增库存失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增库存失败，请检查后端服务')
  }
}

const confirmDuplicateCreate = async () => {
  if (!duplicateCreatePayload.value) {
    cancelDuplicateCreate()
    return
  }
  duplicateSubmitting.value = true
  try {
    const base = duplicateRows.value[0]
    if (!base?.inventoryId) {
      alert('未找到可合并的库存记录，请刷新后重试')
      return
    }

    const mergedPayload = {
      materialId: base.materialId || duplicateCreatePayload.value.materialId,
      storageLocation: base.storageLocation || duplicateCreatePayload.value.storageLocation,
      stockQuantity: Number(base.stockQuantity ?? 0) + Number(duplicateCreatePayload.value.stockQuantity ?? 0),
      lastCheckTime: duplicateCreatePayload.value.lastCheckTime || base.lastCheckTime || null,
      lastCheckerId: duplicateCreatePayload.value.lastCheckerId || base.lastCheckerId || null
    }

    const res = await request.put(`/inventories/${base.inventoryId}`, mergedPayload)
    if (res.code === 200) {
      alert('重复物料已合并入库（库存数量已累计）')
      showCreate.value = false
      cancelDuplicateCreate()
      fetchInventories(1)
    } else {
      alert(res.msg || '合并入库失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '合并入库失败，请检查后端服务')
  } finally {
    duplicateSubmitting.value = false
  }
}

const openCreate = () => {
  createForm.value = {
    inventoryId: '',
    materialId: '',
    materialName: '',
    storageLocation: '',
    stockQuantity: 0,
    lastCheckTime: '',
    lastCheckerId: ''
  }
  showMaterialDropdown.value = false
  showCreate.value = true
}

const onMaterialFieldFocus = (field) => {
  activeMaterialField.value = field
  showMaterialDropdown.value = true
}

const onMaterialFieldInput = (field) => {
  activeMaterialField.value = field
  showMaterialDropdown.value = true
}

const selectMaterialOption = (material) => {
  createForm.value.materialId = material?.materialId || ''
  createForm.value.materialName = material?.materialName || ''
  showMaterialDropdown.value = false
}

const handleGlobalClickForMaterialPicker = (event) => {
  if (!showMaterialDropdown.value) return
  const idRoot = materialPickerRef.value
  const nameRoot = materialNamePickerRef.value
  const inIdRoot = !!idRoot && idRoot.contains(event.target)
  const inNameRoot = !!nameRoot && nameRoot.contains(event.target)
  if (!inIdRoot && !inNameRoot) {
    showMaterialDropdown.value = false
  }
}

const submitCreate = async () => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可新增库存')
    return
  }
  if (!createForm.value.materialId) {
    alert('物料ID不能为空')
    return
  }
  if (!createForm.value.storageLocation) {
    alert('存储位置不能为空')
    return
  }
  if (createForm.value.stockQuantity == null || Number(createForm.value.stockQuantity) < 0) {
    alert('库存数量不能小于0')
    return
  }

  const payload = {
    inventoryId: createForm.value.inventoryId || null,
    materialId: createForm.value.materialId,
    storageLocation: createForm.value.storageLocation,
    stockQuantity: Number(createForm.value.stockQuantity),
    lastCheckTime: createForm.value.lastCheckTime || null,
    lastCheckerId: createForm.value.lastCheckerId || null
  }

  const duplicated = findDuplicateInventories(createForm.value.materialId, createForm.value.storageLocation)
  if (duplicated.length > 0) {
    duplicateCreatePayload.value = payload
    duplicateCreatePreview.value = {
      materialName: resolveMaterialName(createForm.value.materialId),
      materialId: normalizeText(createForm.value.materialId),
      storageLocation: normalizeText(createForm.value.storageLocation),
      stockQuantity: Number(createForm.value.stockQuantity)
    }
    duplicateRows.value = duplicated.map((x) => ({ ...x }))
    showDuplicateModal.value = true
    return
  }

  await executeCreate(payload)
}

const openEdit = (item) => {
  editForm.value = {
    inventoryId: item.inventoryId || '',
    materialId: item.materialId || '',
    storageLocation: item.storageLocation || '',
    stockQuantity: Number(item.stockQuantity ?? 0),
    lastCheckTime: toDateTimeLocal(item.lastCheckTime),
    lastCheckerId: item.lastCheckerId || ''
  }
  showEdit.value = true
}

const submitEdit = async () => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可编辑库存')
    return
  }
  if (!editForm.value.inventoryId) {
    alert('库存ID不能为空')
    return
  }
  if (!editForm.value.materialId) {
    alert('物料ID不能为空')
    return
  }
  if (!editForm.value.storageLocation) {
    alert('存储位置不能为空')
    return
  }
  if (editForm.value.stockQuantity == null || Number(editForm.value.stockQuantity) < 0) {
    alert('库存数量不能小于0')
    return
  }

  try {
    const payload = {
      materialId: editForm.value.materialId,
      storageLocation: editForm.value.storageLocation,
      stockQuantity: Number(editForm.value.stockQuantity),
      lastCheckTime: editForm.value.lastCheckTime || null,
      lastCheckerId: editForm.value.lastCheckerId || null
    }
    const res = await request.put(`/inventories/${editForm.value.inventoryId}`, payload)
    if (res.code === 200) {
      alert('更新库存成功')
      showEdit.value = false
      fetchInventories(pageNum.value)
    } else {
      alert(res.msg || '更新库存失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '更新库存失败，请检查后端服务')
  }
}

const submitDelete = async (item) => {
  if (!canManage.value) {
    alert('仅管理人员或仓库管理员可删除库存')
    return
  }
  if (!item?.inventoryId) {
    alert('库存ID不能为空')
    return
  }
  if (!confirm(`确定删除库存记录【${item.inventoryId}】吗？`)) {
    return
  }

  try {
    const res = await request.delete(`/inventories/${item.inventoryId}`)
    if (res.code === 200) {
      alert('删除库存成功')
      fetchInventories(1)
    } else {
      alert(res.msg || '删除库存失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '删除库存失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const toDateTimeLocal = (timeStr) => {
  if (!timeStr) return ''
  return String(timeStr).replace(' ', 'T').substring(0, 16)
}

onMounted(async () => {
  document.addEventListener('click', handleGlobalClickForMaterialPicker)
  await Promise.all([fetchInventories(1), fetchMaterialOptions()])
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClickForMaterialPicker)
})
</script>

<style scoped>
.inventory-total {
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
.query-input { width: 360px; }

.loading { text-align: center; padding: 48px; color: #aaa; font-size: 15px; }
.error-box { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; }
.error-box button { padding: 4px 12px; background: #ff4d4f; color: #fff; border: none; border-radius: 4px; cursor: pointer; }

.table-wrap { overflow-x: auto; }
.inventory-table { width: 100%; min-width: 1300px; border-collapse: collapse; font-size: 14px; }
.inventory-table thead tr { background: #f5f7fa; }
.inventory-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.inventory-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.inventory-table tbody tr:hover { background: #f9fbff; }

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

.material-picker {
  position: relative;
}

.material-input-group {
  display: flex;
  align-items: stretch;
}

.material-input-group input {
  width: 100%;
}

.material-dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow: auto;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  z-index: 20;
  box-shadow: 0 6px 18px rgba(35, 58, 89, 0.12);
}

.material-option {
  padding: 8px 10px;
  color: #2f445a;
  font-size: 13px;
  cursor: pointer;
}

.material-option:hover {
  background: #f2f7ff;
}

.material-empty {
  padding: 10px;
  color: #8b9bb0;
  font-size: 12px;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.duplicate-modal {
  width: min(860px, calc(100vw - 32px));
}

.duplicate-tip {
  margin: 0 0 10px;
  color: #4d6279;
  font-size: 13px;
}

.duplicate-section {
  border: 1px solid #e8edf3;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
}

.duplicate-title {
  color: #233a59;
  font-weight: 600;
  margin-bottom: 8px;
}

.duplicate-current {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 8px;
  color: #4d6279;
  font-size: 13px;
}

.duplicate-table {
  min-width: 620px;
}
</style>
