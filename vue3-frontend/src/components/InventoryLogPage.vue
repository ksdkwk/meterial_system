<template>
  <div class="inventory-log-page">
    <div class="page-header">
      <h2 class="page-title">🧾 库存变动日志</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="canManage" class="primary-btn" @click="openCreate">+ 新增日志</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按日志ID/库存ID/物料/操作人查询"
        @keyup.enter="fetchLogs(1)"
      />
      <select v-model="query.changeType" class="query-select">
        <option value="">全部变动类型</option>
        <option :value="1">入库</option>
        <option :value="2">出库</option>
        <option :value="3">盘点调整</option>
        <option :value="4">退货入库</option>
      </select>
      <select v-model="query.action" class="query-select">
        <option value="">全部业务动作</option>
        <option v-for="a in LOG_ACTION_OPTIONS" :key="a.value" :value="a.value">{{ a.label }}</option>
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
            <th>库存ID</th>
            <th>物料ID</th>
            <th>物料名称</th>
            <th>变动类型</th>
            <th>变动数量</th>
            <th>变动前</th>
            <th>变动后</th>
            <th>操作人ID</th>
            <th>操作人</th>
            <th>操作时间</th>
            <th>备注</th>
            <th>日志变动原因</th>
            <th v-if="canManage">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.logId">
            <td>{{ item.logId }}</td>
            <td>{{ item.inventoryId || '—' }}</td>
            <td>{{ item.materialId || '—' }}</td>
            <td>{{ item.materialName || '—' }}</td>
            <td>
              <span class="status-tag" :class="typeClass(item.changeType)">{{ typeText(item.changeType) }}</span>
            </td>
            <td>{{ item.changeQuantity ?? 0 }}</td>
            <td>{{ item.beforeQuantity ?? 0 }}</td>
            <td>{{ item.afterQuantity ?? 0 }}</td>
            <td>{{ item.operatorId || '—' }}</td>
            <td>{{ item.operatorName || '—' }}</td>
            <td>{{ formatTime(item.operateTime) }}</td>
            <td :title="item.reason || ''">{{ limitText(item.reason || '—') }}</td>
            <td>{{ actionText(item.logChange) }}</td>
            <td v-if="canManage">
              <button class="tiny-btn danger" @click="submitDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="logs.length === 0">
            <td :colspan="canManage ? 14 : 13" class="empty">暂无数据</td>
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
        <h3>新增库存变动日志</h3>
        <div class="form-grid">
          <label>日志ID</label>
          <input v-model.trim="createForm.logId" placeholder="可不填，系统自动生成" />

          <label>库存ID*</label>
          <div class="id-picker" ref="createInventoryPickerRef">
            <input
              v-model.trim="createForm.inventoryId"
              placeholder="可手动输入，或输入关键词筛选库存"
              @focus="onCreateInventoryFocus"
              @input="onCreateInventoryInput"
            />
            <div v-if="showCreateInventoryDropdown && createInventoryKeyword.length > 0" class="id-dropdown-panel">
              <div
                v-for="inv in filteredCreateInventoryOptions"
                :key="inv.inventoryId"
                class="id-option"
                @click="chooseCreateInventory(inv)"
              >
                {{ inv.inventoryId }} · {{ inv.materialName || inv.materialId || '未知物料' }}
              </div>
              <div v-if="filteredCreateInventoryOptions.length === 0" class="id-empty">无匹配库存</div>
            </div>
          </div>

          <label>物料ID*</label>
          <div class="id-picker" ref="createMaterialPickerRef">
            <input
              v-model.trim="createForm.materialId"
              placeholder="可手动输入，或输入关键词筛选物料"
              @focus="onCreateMaterialFocus"
              @input="onCreateMaterialInput"
            />
            <div v-if="showCreateMaterialDropdown && createMaterialKeyword.length > 0" class="id-dropdown-panel">
              <div
                v-for="m in filteredCreateMaterialOptions"
                :key="m.materialId"
                class="id-option"
                @click="chooseCreateMaterial(m)"
              >
                {{ m.materialId }} · {{ m.materialName || '未命名物料' }}
              </div>
              <div v-if="filteredCreateMaterialOptions.length === 0" class="id-empty">无匹配物料</div>
            </div>
          </div>

          <label>变动类型*</label>
          <select v-model.number="createForm.changeType" class="query-select">
            <option :value="1">入库</option>
            <option :value="2">出库</option>
            <option :value="3">盘点调整</option>
            <option :value="4">退货入库</option>
          </select>

          <label>变动数量*</label>
          <div class="field-with-tip">
            <input
              v-model.number="createForm.changeQuantity"
              type="number"
              min="0"
              :class="{ 'invalid-field': isCreateChangeQuantityNegative }"
            />
            <div v-if="isCreateChangeQuantityNegative" class="field-error">变动数量 &lt; 0</div>
          </div>

          <label>变动前数量*</label>
          <input
            v-model.number="createForm.beforeQuantity"
            type="number"
            min="0"
            :readonly="!canDirectEditBeforeQuantity"
            :class="{ 'auto-field': !canDirectEditBeforeQuantity }"
            :title="canDirectEditBeforeQuantity ? '' : '自动读取库存数量，仅管理人员可直接输入'"
          />

          <label>变动后数量*</label>
          <div class="field-with-tip">
            <input
              :value="createForm.afterQuantity"
              type="number"
              readonly
              :class="['auto-field', { 'invalid-field': isCreateAfterQuantityNegative }]"
              title="系统根据变动前数量与变动数量自动计算"
            />
            <div v-if="isCreateAfterQuantityNegative" class="field-error">变动后数量 &lt; 0</div>
          </div>

          <label>备注</label>
          <input v-model.trim="createForm.reason" />

          <label>业务动作</label>
          <select v-model="createForm.logChange" class="query-select">
            <option v-for="a in LOG_ACTION_OPTIONS" :key="`create-${a.value}`" :value="a.value">{{ a.label }}</option>
          </select>

          <label>操作人ID*</label>
          <select v-model="createForm.operatorId" class="query-select">
            <option value="">请选择操作人</option>
            <option v-for="u in operatorOptions" :key="u.userId" :value="u.userId">
              {{ u.userId }} · {{ u.realName || u.username || '未命名用户' }}
            </option>
          </select>

          <label>操作时间</label>
          <input v-model="createForm.operateTime" type="datetime-local" />
        </div>

        <div class="modal-actions">
          <button class="plain-btn" @click="showCreate = false">取消</button>
          <button class="primary-btn" @click="submitCreate">提交</button>
        </div>
      </div>
    </div>

    <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
      <div class="modal">
        <h3>编辑库存变动日志</h3>
        <div class="form-grid">
          <label>日志ID</label>
          <input v-model.trim="editForm.logId" disabled />

          <label>库存ID*</label>
          <select v-model="editForm.inventoryId" class="query-select" @change="syncMaterialByInventory(editForm)">
            <option value="">请选择库存</option>
            <option v-for="inv in inventoryOptions" :key="inv.inventoryId" :value="inv.inventoryId">
              {{ inv.inventoryId }} · {{ inv.materialName || inv.materialId || '未知物料' }}
            </option>
          </select>

          <label>物料ID*</label>
          <select v-model="editForm.materialId" class="query-select" @change="syncInventoryByMaterial(editForm)">
            <option value="">请选择物料</option>
            <option v-for="m in materialOptions" :key="m.materialId" :value="m.materialId">
              {{ m.materialId }} · {{ m.materialName || '未命名物料' }}
            </option>
          </select>

          <label>变动类型*</label>
          <select v-model.number="editForm.changeType" class="query-select">
            <option :value="1">入库</option>
            <option :value="2">出库</option>
            <option :value="3">盘点调整</option>
            <option :value="4">退货入库</option>
          </select>

          <label>变动数量*</label>
          <div class="field-with-tip">
            <input
              v-model.number="editForm.changeQuantity"
              type="number"
              min="0"
              :class="{ 'invalid-field': isEditChangeQuantityNegative }"
            />
            <div v-if="isEditChangeQuantityNegative" class="field-error">变动数量 &lt; 0</div>
          </div>

          <label>变动前数量*</label>
          <input
            v-model.number="editForm.beforeQuantity"
            type="number"
            min="0"
            :readonly="!canDirectEditBeforeQuantity"
            :class="{ 'auto-field': !canDirectEditBeforeQuantity }"
            :title="canDirectEditBeforeQuantity ? '' : '自动读取库存数量，仅管理人员可直接输入'"
          />

          <label>变动后数量*</label>
          <div class="field-with-tip">
            <input
              :value="editForm.afterQuantity"
              type="number"
              readonly
              :class="['auto-field', { 'invalid-field': isEditAfterQuantityNegative }]"
              title="系统根据变动前数量与变动数量自动计算"
            />
            <div v-if="isEditAfterQuantityNegative" class="field-error">变动后数量 &lt; 0</div>
          </div>

          <label>备注</label>
          <input v-model.trim="editForm.reason" />

          <label>业务动作*</label>
          <div class="field-with-tip">
            <select v-model="editForm.logChange" class="query-select" :class="{ 'invalid-field': isEditLogChangeEmpty }">
              <option value="">请选择业务动作</option>
              <option v-for="a in LOG_ACTION_OPTIONS" :key="`edit-${a.value}`" :value="a.value">{{ a.label }}</option>
            </select>
            <div v-if="isEditLogChangeEmpty" class="field-error">请选择业务动作</div>
          </div>

          <label>操作人ID*</label>
          <select v-model="editForm.operatorId" class="query-select">
            <option value="">请选择操作人</option>
            <option v-for="u in operatorOptions" :key="u.userId" :value="u.userId">
              {{ u.userId }} · {{ u.realName || u.username || '未命名用户' }}
            </option>
          </select>

          <label>操作时间</label>
          <input v-model="editForm.operateTime" type="datetime-local" />
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import request from '@/utils/request'

const LOG_ACTION_OPTIONS = [
  { value: 'ADD', label: 'ADD（新增）' },
  { value: 'DELETE', label: 'DELETE（删除）' },
  { value: 'REVOKE_ADD', label: 'REVOKE_ADD（撤回新增）' },
  { value: 'ALLOCATE', label: 'ALLOCATE（分配物料）' },
  { value: 'ADJUST', label: 'ADJUST（调整）' }
]

const LOG_ACTION_LABEL_MAP = {
  ADD: 'ADD（新增）',
  DELETE: 'DELETE（删除）',
  REVOKE_ADD: 'REVOKE_ADD（撤回新增）',
  ALLOCATE: 'ALLOCATE（分配物料）',
  ADJUST: 'ADJUST（调整）'
}

const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const showEdit = ref(false)
const inventoryOptions = ref([])
const materialOptions = ref([])
const operatorOptions = ref([])
const showCreateInventoryDropdown = ref(false)
const showCreateMaterialDropdown = ref(false)
const createInventoryPickerRef = ref(null)
const createMaterialPickerRef = ref(null)

const createForm = ref({
  logId: '',
  inventoryId: '',
  materialId: '',
  changeType: 1,
  changeQuantity: 0,
  beforeQuantity: 0,
  afterQuantity: 0,
  reason: '',
  logChange: 'ADJUST',
  operatorId: '',
  operateTime: ''
})

const editForm = ref({
  logId: '',
  inventoryId: '',
  materialId: '',
  changeType: 1,
  changeQuantity: 0,
  beforeQuantity: 0,
  afterQuantity: 0,
  reason: '',
  logChange: 'ADJUST',
  operatorId: '',
  operateTime: ''
})

const query = ref({ keyword: '', changeType: '', action: '' })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const createInventoryKeyword = computed(() => String(createForm.value.inventoryId || '').trim().toLowerCase())
const createMaterialKeyword = computed(() => String(createForm.value.materialId || '').trim().toLowerCase())

const filteredCreateInventoryOptions = computed(() => {
  const keyword = createInventoryKeyword.value
  if (!keyword) return inventoryOptions.value
  return inventoryOptions.value.filter((inv) => {
    const inventoryId = String(inv.inventoryId || '').toLowerCase()
    const materialId = String(inv.materialId || '').toLowerCase()
    const materialName = String(inv.materialName || '').toLowerCase()
    const storageLocation = String(inv.storageLocation || '').toLowerCase()
    return inventoryId.includes(keyword)
      || materialId.includes(keyword)
      || materialName.includes(keyword)
      || storageLocation.includes(keyword)
  })
})

const filteredCreateMaterialOptions = computed(() => {
  const keyword = createMaterialKeyword.value
  if (!keyword) return materialOptions.value
  return materialOptions.value.filter((m) => {
    const materialId = String(m.materialId || '').toLowerCase()
    const materialName = String(m.materialName || '').toLowerCase()
    return materialId.includes(keyword) || materialName.includes(keyword)
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
  } catch {
    return false
  }
})

const canDirectEditBeforeQuantity = computed(() => {
  const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  if (!raw) return false
  try {
    const u = JSON.parse(raw)
    const roleCode = String(u?.role?.roleCode || u?.roleCode || u?.rolePermission || '').toUpperCase()
    const roleName = String(u?.role?.roleName || u?.roleName || '').trim()
    return ['MANAGER', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN'].includes(roleCode)
      || ['管理员', '系统管理员', '超级管理员'].includes(roleName)
  } catch {
    return false
  }
})

const normalizeNumber = (val, fallback = 0) => {
  const n = Number(val)
  return Number.isFinite(n) ? n : fallback
}

const isInboundLike = (changeType) => [1, 3, 4].includes(Number(changeType))

const calculateAfterQuantity = (beforeQuantity, changeQuantity, changeType) => {
  const before = normalizeNumber(beforeQuantity, 0)
  const change = normalizeNumber(changeQuantity, 0)
  return isInboundLike(changeType) ? before + change : before - change
}

const autoFillBeforeFromInventory = (formRef) => {
  if (canDirectEditBeforeQuantity.value) return
  const invId = formRef.value.inventoryId
  if (!invId) {
    formRef.value.beforeQuantity = 0
    return
  }
  const selected = inventoryOptions.value.find(v => v.inventoryId === invId)
  formRef.value.beforeQuantity = normalizeNumber(selected?.stockQuantity, 0)
}

const recalculateAfterQuantity = (formRef) => {
  formRef.value.afterQuantity = calculateAfterQuantity(
    formRef.value.beforeQuantity,
    formRef.value.changeQuantity,
    formRef.value.changeType
  )
}

const isNegative = (val) => normalizeNumber(val, 0) < 0

const isCreateChangeQuantityNegative = computed(() => isNegative(createForm.value.changeQuantity))
const isEditChangeQuantityNegative = computed(() => isNegative(editForm.value.changeQuantity))
const isCreateAfterQuantityNegative = computed(() => isNegative(createForm.value.afterQuantity))
const isEditAfterQuantityNegative = computed(() => isNegative(editForm.value.afterQuantity))
const isEditLogChangeEmpty = computed(() => !String(editForm.value.logChange || '').trim())

const fetchLogs = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      changeType: query.value.changeType === '' ? undefined : Number(query.value.changeType),
      action: query.value.action || undefined,
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/inventory-logs/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      logs.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取库存变动日志失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const loadInventoryOptions = async () => {
  try {
    const res = await request.get('/inventories/list', {
      params: { pageNum: 1, pageSize: 500 }
    })
    if (res.code === 200) {
      const records = Array.isArray(res?.data?.records) ? res.data.records : []
      inventoryOptions.value = records
    }
  } catch {
    inventoryOptions.value = []
  }
}

const loadMaterialOptions = async () => {
  try {
    const res = await request.get('/materials/list', {
      params: { status: 1 }
    })
    if (res.code === 200) {
      materialOptions.value = Array.isArray(res.data) ? res.data : []
    }
  } catch {
    materialOptions.value = []
  }
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

const loadOperatorOptions = async () => {
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
      operatorOptions.value = Array.from(map.values())
      return
    }
  } catch {
    // ignore and fallback
  }
  operatorOptions.value = fallbackUser?.userId ? [fallbackUser] : []
}

const loadFormOptions = async () => {
  await Promise.all([
    loadInventoryOptions(),
    loadMaterialOptions(),
    loadOperatorOptions()
  ])
}

const onCreateInventoryFocus = () => {
  showCreateInventoryDropdown.value = true
}

const onCreateInventoryInput = () => {
  showCreateInventoryDropdown.value = true
}

const onCreateMaterialFocus = () => {
  showCreateMaterialDropdown.value = true
}

const onCreateMaterialInput = () => {
  showCreateMaterialDropdown.value = true
}

const chooseCreateInventory = (inv) => {
  createForm.value.inventoryId = inv?.inventoryId || ''
  if (inv?.materialId) createForm.value.materialId = inv.materialId
  autoFillBeforeFromInventory(createForm)
  recalculateAfterQuantity(createForm)
  showCreateInventoryDropdown.value = false
}

const chooseCreateMaterial = (material) => {
  createForm.value.materialId = material?.materialId || ''
  const currentInv = inventoryOptions.value.find(v => v.inventoryId === createForm.value.inventoryId)
  if (currentInv?.materialId !== createForm.value.materialId) {
    const matchedInv = inventoryOptions.value.find(v => v.materialId === createForm.value.materialId)
    if (matchedInv?.inventoryId) createForm.value.inventoryId = matchedInv.inventoryId
  }
  autoFillBeforeFromInventory(createForm)
  recalculateAfterQuantity(createForm)
  showCreateMaterialDropdown.value = false
}

const handleCreatePickersOutsideClick = (event) => {
  if (showCreateInventoryDropdown.value) {
    const root = createInventoryPickerRef.value
    if (!root || !root.contains(event.target)) showCreateInventoryDropdown.value = false
  }
  if (showCreateMaterialDropdown.value) {
    const root = createMaterialPickerRef.value
    if (!root || !root.contains(event.target)) showCreateMaterialDropdown.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', changeType: '', action: '' }
  fetchLogs(1)
}

const openCreate = () => {
  createForm.value = {
    logId: '', inventoryId: '', materialId: '', changeType: 1,
    changeQuantity: 0, beforeQuantity: 0, afterQuantity: 0,
    reason: '', logChange: 'ADJUST', operatorId: '', operateTime: ''
  }
  showCreateInventoryDropdown.value = false
  showCreateMaterialDropdown.value = false
  if (operatorOptions.value.length === 1 && operatorOptions.value[0]?.userId) {
    createForm.value.operatorId = operatorOptions.value[0].userId
  }
  autoFillBeforeFromInventory(createForm)
  recalculateAfterQuantity(createForm)
  showCreate.value = true
}

const submitCreate = async () => {
  if (!canManage.value) return alert('仅管理人员或仓库管理员可新增日志')
  if (!createForm.value.inventoryId || !createForm.value.materialId || !createForm.value.operatorId) return alert('库存ID/物料ID/操作人ID不能为空')
  try {
    const payload = {
      logId: createForm.value.logId || null,
      inventoryId: createForm.value.inventoryId,
      materialId: createForm.value.materialId,
      changeType: Number(createForm.value.changeType),
      changeQuantity: Number(createForm.value.changeQuantity),
      beforeQuantity: Number(createForm.value.beforeQuantity),
      afterQuantity: Number(createForm.value.afterQuantity),
      reason: createForm.value.reason || null,
      logChange: createForm.value.logChange || 'ADJUST',
      operatorId: createForm.value.operatorId,
      operateTime: createForm.value.operateTime || null,
      logStatus: 1
    }
    const res = await request.post('/inventory-logs', payload)
    if (res.code === 200) {
      alert('新增日志成功')
      showCreate.value = false
      fetchLogs(1)
    } else alert(res.msg || '新增日志失败')
  } catch (e) {
    alert(e?.response?.data?.msg || '新增日志失败，请检查后端服务')
  }
}

const openEdit = (item) => {
  editForm.value = {
    logId: item.logId || '',
    inventoryId: item.inventoryId || '',
    materialId: item.materialId || '',
    changeType: Number(item.changeType ?? 1),
    changeQuantity: Number(item.changeQuantity ?? 0),
    beforeQuantity: Number(item.beforeQuantity ?? 0),
    afterQuantity: Number(item.afterQuantity ?? 0),
    reason: item.reason || '',
    logChange: item.logChange || 'ADJUST',
    operatorId: item.operatorId || '',
    operateTime: toDateTimeLocal(item.operateTime)
  }
  autoFillBeforeFromInventory(editForm)
  recalculateAfterQuantity(editForm)
  showEdit.value = true
}

const syncMaterialByInventory = (formRef) => {
  const invId = formRef.value.inventoryId
  if (!invId) return
  const selected = inventoryOptions.value.find(v => v.inventoryId === invId)
  if (selected?.materialId) formRef.value.materialId = selected.materialId
  autoFillBeforeFromInventory(formRef)
  recalculateAfterQuantity(formRef)
}

const syncInventoryByMaterial = (formRef) => {
  const matId = formRef.value.materialId
  if (!matId) return
  const currentInv = inventoryOptions.value.find(v => v.inventoryId === formRef.value.inventoryId)
  if (currentInv?.materialId === matId) {
    autoFillBeforeFromInventory(formRef)
    recalculateAfterQuantity(formRef)
    return
  }
  const selected = inventoryOptions.value.find(v => v.materialId === matId)
  if (selected?.inventoryId) formRef.value.inventoryId = selected.inventoryId
  autoFillBeforeFromInventory(formRef)
  recalculateAfterQuantity(formRef)
}

watch(
  () => [createForm.value.changeType, createForm.value.changeQuantity, createForm.value.beforeQuantity],
  () => recalculateAfterQuantity(createForm)
)

watch(
  () => [editForm.value.changeType, editForm.value.changeQuantity, editForm.value.beforeQuantity],
  () => recalculateAfterQuantity(editForm)
)

const submitEdit = async () => {
  if (!canManage.value) return alert('仅管理人员或仓库管理员可编辑日志')
  if (!editForm.value.logId || !editForm.value.inventoryId || !editForm.value.materialId || !editForm.value.operatorId) return alert('日志ID/库存ID/物料ID/操作人ID不能为空')
  if (isEditLogChangeEmpty.value) return alert('日志变动原因为必填：用于标记日志的撤销、修改等')
  try {
    const payload = {
      inventoryId: editForm.value.inventoryId,
      materialId: editForm.value.materialId,
      changeType: Number(editForm.value.changeType),
      changeQuantity: Number(editForm.value.changeQuantity),
      beforeQuantity: Number(editForm.value.beforeQuantity),
      afterQuantity: Number(editForm.value.afterQuantity),
      reason: editForm.value.reason || null,
      logChange: editForm.value.logChange || 'ADJUST',
      operatorId: editForm.value.operatorId,
      operateTime: editForm.value.operateTime || null
    }
    const res = await request.put(`/inventory-logs/${editForm.value.logId}`, payload)
    if (res.code === 200) {
      alert('更新日志成功')
      showEdit.value = false
      fetchLogs(pageNum.value)
    } else alert(res.msg || '更新日志失败')
  } catch (e) {
    alert(e?.response?.data?.msg || '更新日志失败，请检查后端服务')
  }
}

const submitDelete = async (item) => {
  if (!canManage.value) return alert('仅管理人员或仓库管理员可失效日志')
  if (!item?.logId) return alert('日志ID不能为空')
  if (!confirm(`确定将日志【${item.logId}】设为失效吗？`)) return
  try {
    const res = await request.delete(`/inventory-logs/${item.logId}`)
    if (res.code === 200) {
      alert('日志已失效（数据库保留）')
      fetchLogs(1)
    } else alert(res.msg || '日志失效处理失败')
  } catch (e) {
    alert(e?.response?.data?.msg || '日志失效处理失败，请检查后端服务')
  }
}

const typeText = (type) => {
  const map = { 1: '入库', 2: '出库', 3: '盘点调整', 4: '退货入库' }
  return map[type] || '未知'
}

const typeClass = (type) => {
  if (type === 1 || type === 4) return 'active'
  if (type === 2) return 'inactive'
  return 'pending'
}

const actionText = (action) => {
  const key = String(action || '').trim().toUpperCase()
  if (!key) return '—'
  return LOG_ACTION_LABEL_MAP[key] || action
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const limitText = (text) => {
  const s = String(text || '')
  return s.length > 24 ? `${s.slice(0, 24)}…` : s
}

const toDateTimeLocal = (timeStr) => {
  if (!timeStr) return ''
  return String(timeStr).replace(' ', 'T').substring(0, 16)
}

onMounted(async () => {
  document.addEventListener('click', handleCreatePickersOutsideClick)
  await Promise.all([fetchLogs(1), loadFormOptions()])
})

onUnmounted(() => {
  document.removeEventListener('click', handleCreatePickersOutsideClick)
})
</script>

<style scoped>
.inventory-log-page {
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
.log-table { width: 100%; min-width: 1450px; border-collapse: collapse; font-size: 14px; }
.log-table thead tr { background: #f5f7fa; }
.log-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.log-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.log-table tbody tr:hover { background: #f9fbff; }

.status-tag { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-tag.active { background: #f6ffed; color: #52c41a; }
.status-tag.inactive { background: #fff2f0; color: #ff4d4f; }
.status-tag.pending { background: #fffbe6; color: #d48806; }

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
  width: min(820px, calc(100vw - 32px));
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

.form-grid select {
  background: #fff;
}

.id-picker {
  position: relative;
}

.id-dropdown-panel {
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

.id-option {
  padding: 8px 10px;
  color: #2f445a;
  font-size: 13px;
  cursor: pointer;
}

.id-option:hover {
  background: #f2f7ff;
}

.id-empty {
  padding: 10px;
  color: #8b9bb0;
  font-size: 12px;
}

.field-with-tip {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auto-field {
  background: #f5f7fa;
  color: #5a6a82;
}

.invalid-field {
  border-color: #ff4d4f !important;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.12);
  color: #cf1322;
}

.field-error {
  margin-top: -4px;
  color: #cf1322;
  font-size: 12px;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
