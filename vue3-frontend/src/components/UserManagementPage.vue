<template>
  <div class="user-management">
    <div class="page-header">
      <h2 class="page-title">👥 用户管理</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button class="primary-btn" @click="openCreate">+ 新增用户</button>
      <button class="danger-btn" :disabled="selectedIds.length === 0" @click="batchRemove">批量删除</button>
    </div>

    <div class="query-bar">
      <input v-model="query.keyword" class="query-input" placeholder="按账号/姓名查询" @keyup.enter="fetchUsers(1)" />
      <select v-model="query.roleId" class="query-select">
        <option value="">全部角色</option>
        <option value="1">管理人员</option>
        <option value="2">施工人员</option>
        <option value="3">仓库管理员</option>
      </select>
      <select v-model="query.status" class="query-select">
        <option value="">全部状态</option>
        <option value="1">正常</option>
        <option value="0">禁用</option>
      </select>
      <button class="primary-btn" @click="fetchUsers(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchUsers(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="user-table">
        <thead>
          <tr>
            <th><input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" /></th>
            <th class="sortable" @click="changeSort('userId')">用户ID <span>{{ sortMark('userId') }}</span></th>
            <th class="sortable" @click="changeSort('username')">账号 <span>{{ sortMark('username') }}</span></th>
            <th class="sortable" @click="changeSort('realName')">真实姓名 <span>{{ sortMark('realName') }}</span></th>
            <th class="sortable" @click="changeSort('roleId')">角色ID <span>{{ sortMark('roleId') }}</span></th>
            <th class="col-role">角色</th>
            <th>权限</th>
            <th>手机号</th>
            <th class="col-status">状态</th>
            <th class="sortable" @click="changeSort('createTime')">创建时间 <span>{{ sortMark('createTime') }}</span></th>
            <th class="sortable" @click="changeSort('updateTime')">更新时间 <span>{{ sortMark('updateTime') }}</span></th>
            <th class="col-op">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId">
            <td><input type="checkbox" :value="user.userId" v-model="selectedIds" /></td>
            <td>{{ user.userId }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.realName || '—' }}</td>
            <td>{{ user.roleId ?? '—' }}</td>
            <td class="col-role">
              <span class="role-tag" :class="getRoleClass(user.role?.roleCode)" :title="user.role?.roleName || '未知角色'">
                {{ limitText(user.role?.roleName || '未知角色') }}
              </span>
            </td>
            <td>{{ user.rolePermission || '—' }}</td>
            <td>{{ user.phone || '—' }}</td>
            <td class="col-status">
              <span class="status-tag" :class="user.status === 1 ? 'active' : 'inactive'" :title="user.status === 1 ? '正常' : '禁用'">
                {{ limitText(user.status === 1 ? '正常' : '禁用') }}
              </span>
            </td>
            <td>{{ formatTime(user.createTime) }}</td>
            <td>{{ formatTime(user.updateTime) }}</td>
            <td class="col-op">
              <button class="link-btn" @click="openEdit(user)">编辑</button>
              <button class="link-btn danger" @click="removeUser(user)">删除</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="12" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchUsers(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchUsers(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchUsers(pageNum + 1)">下一页</button>
      </div>
    </div>

    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <h3>{{ isEdit ? '编辑用户' : '新增用户' }}</h3>
        <div class="form-grid">
          <label>用户ID</label>
          <input v-model="form.userId" :disabled="isEdit" placeholder="留空自动生成" />

          <label>账号</label>
          <input v-model="form.username" placeholder="请输入账号" />

          <label>密码</label>
          <input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'" />

          <label>真实姓名</label>
          <input v-model="form.realName" placeholder="请输入姓名" />

          <label>手机号</label>
          <input v-model="form.phone" placeholder="请输入11位手机号" />

          <label>角色</label>
          <select v-model.number="form.roleId">
            <option :value="1">管理人员</option>
            <option :value="2">施工人员</option>
            <option :value="3">仓库管理员</option>
          </select>

          <label>状态</label>
          <select v-model.number="form.status">
            <option :value="1">正常</option>
            <option :value="0">禁用</option>
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
import { computed, ref, onMounted } from 'vue'
import request from '@/utils/request'
import { REGEXP_PHONE } from '@/utils/regexp'

const users = ref([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const query = ref({ keyword: '', roleId: '', status: '' })
const sortBy = ref('createTime')
const sortOrder = ref('desc')

const selectedIds = ref([])
const showModal = ref(false)
const isEdit = ref(false)
const form = ref({
  userId: '',
  username: '',
  password: '',
  realName: '',
  phone: '',
  roleId: 2,
  status: 1
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const isAllSelected = computed(() => users.value.length > 0 && selectedIds.value.length === users.value.length)

const fetchUsers = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      roleId: query.value.roleId === '' ? undefined : Number(query.value.roleId),
      status: query.value.status === '' ? undefined : Number(query.value.status),
      pageNum: targetPage,
      pageSize: pageSize.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }

    const res = await request.get('/users/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      users.value = page.records || []
      total.value = page.total || 0
      pageNum.value = page.pageNum || targetPage
      pageSize.value = page.pageSize || pageSize.value
      selectedIds.value = []
    } else {
      error.value = res.msg || '获取用户列表失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', roleId: '', status: '' }
  fetchUsers(1)
}

const changeSort = (field) => {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortOrder.value = 'asc'
  }
  fetchUsers(1)
}

const sortMark = (field) => {
  if (sortBy.value !== field) return '↕'
  return sortOrder.value === 'asc' ? '↑' : '↓'
}

const toggleSelectAll = (e) => {
  if (e.target.checked) {
    selectedIds.value = users.value.map(u => u.userId)
  } else {
    selectedIds.value = []
  }
}

const validateForm = () => {
  if (!form.value.username?.trim()) return '账号不能为空'
  if (!isEdit.value && !form.value.password?.trim()) return '新增用户时密码不能为空'
  if (form.value.phone && !REGEXP_PHONE.test(form.value.phone)) return '手机号格式不正确'
  return null
}

const openCreate = () => {
  isEdit.value = false
  form.value = {
    userId: '', username: '', password: '', realName: '', phone: '', roleId: 2, status: 1
  }
  showModal.value = true
}

const openEdit = (user) => {
  isEdit.value = true
  form.value = {
    userId: user.userId,
    username: user.username,
    password: '',
    realName: user.realName || '',
    phone: user.phone || '',
    roleId: user.roleId || 2,
    status: user.status ?? 1
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const submitForm = async () => {
  const msg = validateForm()
  if (msg) {
    alert(msg)
    return
  }

  try {
    let res
    if (isEdit.value) {
      res = await request.put(`/users/${form.value.userId}`, form.value)
    } else {
      res = await request.post('/users', form.value)
    }

    if (res.code === 200) {
      alert(isEdit.value ? '修改成功' : '新增成功')
      closeModal()
      fetchUsers(pageNum.value)
    } else {
      alert(res.msg || '操作失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '提交失败，请检查后端服务')
  }
}

const removeUser = async (user) => {
  if (!confirm(`确定删除用户 ${user.username} 吗？`)) return
  try {
    const res = await request.delete(`/users/${user.userId}`)
    if (res.code === 200) {
      alert('删除成功')
      fetchUsers(pageNum.value)
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '删除失败，请检查后端服务')
  }
}

const batchRemove = async () => {
  if (selectedIds.value.length === 0) return
  if (!confirm(`确定批量删除 ${selectedIds.value.length} 个用户吗？`)) return
  try {
    const res = await request.post('/users/batch-delete', selectedIds.value)
    if (res.code === 200) {
      alert('批量删除成功')
      fetchUsers(pageNum.value)
    } else {
      alert(res.msg || '批量删除失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '批量删除失败，请检查后端服务')
  }
}

const getRoleClass = (roleCode) => {
  if (roleCode === 'MANAGER') return 'manager'
  if (roleCode === 'WORKER') return 'worker'
  if (roleCode === 'WAREHOUSE') return 'warehouse'
  return ''
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const limitText = (text) => {
  const str = String(text || '')
  return str.length > 10 ? `${str.slice(0, 10)}…` : str
}

onMounted(() => fetchUsers(1))
</script>

<style scoped>
.user-management {
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
  width: 220px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #233a59;
  margin: 0;
}

.total-info {
  color: #888;
  font-size: 13px;
  margin-right: auto;
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
  padding-bottom: 4px;
}

.user-table {
  width: 100%;
  min-width: 1480px;
  border-collapse: collapse;
  font-size: 14px;
}

.user-table thead tr {
  background: #f5f7fa;
}

.user-table th {
  padding: 12px 14px;
  text-align: left;
  color: #5a6a82;
  font-weight: 600;
  border-bottom: 2px solid #e8edf3;
  white-space: nowrap;
}

.user-table td {
  padding: 11px 14px;
  color: #333;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
  white-space: nowrap;
}

.user-table tbody tr:hover {
  background: #f9fbff;
}

.role-tag {
  display: inline-block;
  max-width: 10em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.role-tag.manager  { background: #e6f4ff; color: #1677ff; }
.role-tag.worker   { background: #f6ffed; color: #52c41a; }
.role-tag.warehouse{ background: #fff7e6; color: #fa8c16; }

.status-tag {
  display: inline-block;
  max-width: 10em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.col-role,
.col-status {
  min-width: 120px;
}

.col-op {
  min-width: 160px;
}
.status-tag.active   { background: #f6ffed; color: #52c41a; }
.status-tag.inactive { background: #fff2f0; color: #ff4d4f; }

.empty {
  text-align: center;
  color: #bbb;
  padding: 40px 0;
}

.sortable { cursor: pointer; user-select: none; }

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
  color: #fff;
  border-color: #ff4d4f;
  padding: 0 12px;
  height: 34px;
}

.danger-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.link-btn {
  background: #fff;
  color: #1677ff;
  margin-right: 8px;
  padding: 2px 8px;
  height: 28px;
}

.link-btn.danger {
  color: #ff4d4f;
}

.pagination {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: grid;
  place-items: center;
  z-index: 1000;
}

.modal {
  width: 520px;
  background: #fff;
  border-radius: 10px;
  padding: 16px;
}

.modal h3 {
  margin: 0 0 14px;
  color: #243547;
}

.form-grid {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 10px;
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
