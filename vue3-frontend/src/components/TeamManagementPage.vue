<template>
  <div class="team-management">
    <div class="page-header">
      <h2 class="page-title">🏗️ 施工班组信息管理</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="isManager" class="primary-btn" @click="openCreate">+ 新增施工班组信息</button>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按班组名称/班组长查询"
        @keyup.enter="fetchTeams(1)"
      />
      <select v-model="query.status" class="query-select">
        <option value="">全部状态</option>
        <option :value="1">正常运作</option>
        <option :value="0">已解散</option>
      </select>
      <button class="primary-btn" @click="fetchTeams(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchTeams(pageNum)">重试</button>
    </div>

    <div v-else class="table-wrap">
      <table class="team-table">
        <thead>
          <tr>
            <th>班组ID</th>
            <th>班组名称</th>
            <th>班组长</th>
            <th>联系电话</th>
            <th>班组成员</th>
            <th>成立时间</th>
            <th>状态</th>
            <th v-if="isManager">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in teams" :key="item.teamId">
            <td>{{ item.teamId }}</td>
            <td>{{ item.teamName || '—' }}</td>
            <td>{{ item.leaderName || '—' }}</td>
            <td>{{ item.leaderPhone || '—' }}</td>
            <td :title="item.teamMember || ''">{{ limitText(item.teamMember || '—') }}</td>
            <td>{{ formatTime(item.createTime) }}</td>
            <td>
              <span class="status-tag" :class="item.status === 1 ? 'active' : 'inactive'">
                {{ item.status === 1 ? '正常运作' : '已解散' }}
              </span>
            </td>
            <td v-if="isManager">
              <button class="tiny-btn" @click="openEdit(item)">编辑</button>
              <button class="tiny-btn danger" @click="submitDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="teams.length === 0">
            <td :colspan="isManager ? 8 : 7" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="plain-btn" :disabled="pageNum <= 1" @click="fetchTeams(pageNum - 1)">上一页</button>
        <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" class="query-select" @change="fetchTeams(1)">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
        </select>
        <button class="plain-btn" :disabled="pageNum >= totalPages" @click="fetchTeams(pageNum + 1)">下一页</button>
      </div>
    </div>


    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h3>新增施工班组信息</h3>
        <div class="form-grid">
          <label>班组ID</label>
          <input v-model.trim="createForm.teamId" placeholder="可不填，系统自动生成" />

          <label>班组名称*</label>
          <input v-model.trim="createForm.teamName" placeholder="请输入班组名称" />

          <label>班组长*</label>
          <input v-model.trim="createForm.leaderName" placeholder="请输入班组长姓名" />

          <label>联系电话</label>
          <input v-model.trim="createForm.leaderPhone" placeholder="请输入联系电话" />

          <label>班组成员</label>
          <textarea v-model.trim="createForm.teamMember" rows="3" placeholder="可选，输入成员说明"></textarea>

          <label>状态</label>
          <select v-model.number="createForm.status" class="query-select">
            <option :value="1">正常运作</option>
            <option :value="0">已解散</option>
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
        <h3>编辑施工班组信息</h3>
        <div class="form-grid">
          <label>班组ID</label>
          <input v-model.trim="editForm.teamId" disabled />

          <label>班组名称*</label>
          <input v-model.trim="editForm.teamName" placeholder="请输入班组名称" />

          <label>班组长*</label>
          <input v-model.trim="editForm.leaderName" placeholder="请输入班组长姓名" />

          <label>联系电话</label>
          <input v-model.trim="editForm.leaderPhone" placeholder="请输入联系电话" />

          <label>班组成员</label>
          <textarea v-model.trim="editForm.teamMember" rows="3" placeholder="可选，输入成员说明"></textarea>

          <label>状态</label>
          <select v-model.number="editForm.status" class="query-select">
            <option :value="1">正常运作</option>
            <option :value="0">已解散</option>
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

const teams = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')
const showCreate = ref(false)
const showEdit = ref(false)

const createForm = ref({
  teamId: '',
  teamName: '',
  leaderName: '',
  leaderPhone: '',
  teamMember: '',
  status: 1
})

const editForm = ref({
  teamId: '',
  teamName: '',
  leaderName: '',
  leaderPhone: '',
  teamMember: '',
  status: 1
})

const query = ref({ keyword: '', status: '' })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const isManager = computed(() => {
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

const fetchTeams = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const params = {
      keyword: query.value.keyword || undefined,
      status: query.value.status === '' ? undefined : Number(query.value.status),
      pageNum: targetPage,
      pageSize: pageSize.value
    }
    const res = await request.get('/teams/list', { params })
    if (res.code === 200) {
      const page = res.data || {}
      teams.value = Array.isArray(page.records) ? page.records : []
      total.value = Number(page.total || 0)
      pageNum.value = Number(page.pageNum || targetPage)
      pageSize.value = Number(page.pageSize || pageSize.value)
    } else {
      error.value = res.msg || '获取班组信息失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', status: '' }
  fetchTeams(1)
}

const openCreate = () => {
  createForm.value = {
    teamId: '',
    teamName: '',
    leaderName: '',
    leaderPhone: '',
    teamMember: '',
    status: 1
  }
  showCreate.value = true
}

const submitCreate = async () => {
  if (!isManager.value) {
    alert('仅管理人员可新增施工班组信息')
    return
  }
  if (!createForm.value.teamName) {
    alert('班组名称不能为空')
    return
  }
  if (!createForm.value.leaderName) {
    alert('班组长不能为空')
    return
  }

  try {
    const payload = {
      teamId: createForm.value.teamId || null,
      teamName: createForm.value.teamName,
      leaderName: createForm.value.leaderName,
      leaderPhone: createForm.value.leaderPhone || null,
      teamMember: createForm.value.teamMember || null,
      status: Number(createForm.value.status ?? 1)
    }
    const res = await request.post('/teams', payload)
    if (res.code === 200) {
      alert('新增施工班组成功')
      showCreate.value = false
      fetchTeams(1)
    } else {
      alert(res.msg || '新增施工班组失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '新增施工班组失败，请检查后端服务')
  }
}

const openEdit = (item) => {
  editForm.value = {
    teamId: item.teamId || '',
    teamName: item.teamName || '',
    leaderName: item.leaderName || '',
    leaderPhone: item.leaderPhone || '',
    teamMember: item.teamMember || '',
    status: Number(item.status ?? 1)
  }
  showEdit.value = true
}

const submitEdit = async () => {
  if (!isManager.value) {
    alert('仅管理人员可编辑施工班组信息')
    return
  }
  if (!editForm.value.teamId) {
    alert('班组ID不能为空')
    return
  }
  if (!editForm.value.teamName) {
    alert('班组名称不能为空')
    return
  }
  if (!editForm.value.leaderName) {
    alert('班组长不能为空')
    return
  }

  try {
    const payload = {
      teamName: editForm.value.teamName,
      leaderName: editForm.value.leaderName,
      leaderPhone: editForm.value.leaderPhone || null,
      teamMember: editForm.value.teamMember || null,
      status: Number(editForm.value.status ?? 1)
    }
    const res = await request.put(`/teams/${editForm.value.teamId}`, payload)
    if (res.code === 200) {
      alert('更新施工班组成功')
      showEdit.value = false
      fetchTeams(pageNum.value)
    } else {
      alert(res.msg || '更新施工班组失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '更新施工班组失败，请检查后端服务')
  }
}

const submitDelete = async (item) => {
  if (!isManager.value) {
    alert('仅管理人员可删除施工班组信息')
    return
  }
  if (!item?.teamId) {
    alert('班组ID不能为空')
    return
  }
  if (!confirm(`确定删除班组【${item.teamName || item.teamId}】吗？`)) {
    return
  }

  try {
    const res = await request.delete(`/teams/${item.teamId}`)
    if (res.code === 200) {
      alert('删除施工班组成功')
      fetchTeams(1)
    } else {
      alert(res.msg || '删除施工班组失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '删除施工班组失败，请检查后端服务')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const limitText = (text) => {
  const s = String(text || '')
  return s.length > 30 ? `${s.slice(0, 30)}…` : s
}

onMounted(() => {
  fetchTeams(1)
})
</script>

<style scoped>
.team-management {
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
.query-input { width: 280px; }

.loading { text-align: center; padding: 48px; color: #aaa; font-size: 15px; }
.error-box { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; }
.error-box button { padding: 4px 12px; background: #ff4d4f; color: #fff; border: none; border-radius: 4px; cursor: pointer; }

.table-wrap { overflow-x: auto; }
.team-table { width: 100%; min-width: 980px; border-collapse: collapse; font-size: 14px; }
.team-table thead tr { background: #f5f7fa; }
.team-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.team-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.team-table tbody tr:hover { background: #f9fbff; }

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
.form-grid select,
.form-grid textarea {
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 8px 10px;
}

.form-grid textarea {
  resize: vertical;
}

.modal-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
