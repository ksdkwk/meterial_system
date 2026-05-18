<template>
  <div class="review-log-page">
    <div class="page-header">
      <h2 class="page-title">🧾 物料需求审批记录</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按审核ID/需求单号/申请人/审批人查询"
        @keyup.enter="fetchLogs(1)"
      />
      <select v-model="query.reviewState" class="query-select">
        <option value="">全部审核状态</option>
        <option value="待审核">待审核</option>
        <option value="审核中">审核中</option>
        <option value="已完成">已完成</option>
        <option value="已删除">已删除</option>
        <option value="已撤回">已撤回</option>
      </select>
      <select v-model="query.reviewResult" class="query-select">
        <option value="">全部审批结果</option>
        <option value="通过">通过</option>
        <option value="驳回">驳回</option>
        <option value="待审核">待审核</option>
        <option value="待处理">待处理</option>
        <option value="已撤回">已撤回</option>
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
            <th>审核ID</th>
            <th>需求单号</th>
            <th>申请人ID</th>
            <th>申请人姓名</th>
            <th>审批人ID</th>
            <th>审批人姓名</th>
            <th>审核状态</th>
            <th>审批时间</th>
            <th>审批结果</th>
            <th>审批理由</th>
            <th>其他</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.reviewId">
            <td>{{ item.reviewId || '—' }}</td>
            <td>{{ item.reviewDemandId || '—' }}</td>
            <td>{{ item.reviewApplyId || '—' }}</td>
            <td>{{ item.reviewApplyName || '—' }}</td>
            <td>{{ item.reviewApproveId || '系统' }}</td>
            <td>{{ item.reviewApproveName || '系统' }}</td>
            <td>
              <span class="status-tag" :class="stateClass(item.reviewState)">{{ item.reviewState || '—' }}</span>
            </td>
            <td>{{ formatTime(item.reviewTime) }}</td>
            <td>
              <span class="status-tag" :class="resultClass(item.reviewResult)">{{ item.reviewResult || '—' }}</span>
            </td>
            <td :title="item.reviewReason || ''">{{ limitText(item.reviewReason) }}</td>
            <td :title="item.reviewOther || ''">{{ limitText(item.reviewOther) }}</td>
          </tr>
          <tr v-if="logs.length === 0">
            <td colspan="11" class="empty">暂无数据</td>
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
const query = ref({ keyword: '', reviewState: '', reviewResult: '' })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const fetchLogs = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/review-logs/list', {
      params: {
        keyword: query.value.keyword || undefined,
        reviewState: query.value.reviewState || undefined,
        reviewResult: query.value.reviewResult || undefined,
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
      error.value = res.msg || '获取物料需求审批记录失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { keyword: '', reviewState: '', reviewResult: '' }
  fetchLogs(1)
}

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 19)
}

const limitText = (text) => {
  const s = String(text || '')
  return s.length > 24 ? `${s.slice(0, 24)}…` : (s || '—')
}

const stateClass = (state) => {
  const s = String(state || '').trim()
  if (s === '待审核') return 'pending'
  if (s === '审核中') return 'processing'
  if (s === '已完成') return 'done'
  if (s === '已删除' || s === '已撤回') return 'reject'
  return 'default'
}

const resultClass = (result) => {
  const s = String(result || '').trim()
  if (s === '通过') return 'pass'
  if (s === '驳回') return 'reject'
  if (s === '待审核' || s === '待处理') return 'pending'
  if (s === '已撤回') return 'reject'
  return 'default'
}

onMounted(() => fetchLogs(1))
</script>

<style scoped>
.review-log-page {
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
.log-table { width: 100%; min-width: 1580px; border-collapse: collapse; font-size: 14px; }
.log-table thead tr { background: #f5f7fa; }
.log-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.log-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.log-table tbody tr:hover { background: #f9fbff; }

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  height: 24px;
  border-radius: 12px;
  padding: 0 8px;
  font-size: 12px;
}

.status-tag.pending { background: #fff7e6; color: #d46b08; }
.status-tag.processing { background: #e6f4ff; color: #1677ff; }
.status-tag.done { background: #f6ffed; color: #389e0d; }
.status-tag.pass { background: #f6ffed; color: #389e0d; }
.status-tag.reject { background: #fff1f0; color: #cf1322; }
.status-tag.default { background: #f5f5f5; color: #666; }

.empty { text-align: center; color: #bbb; padding: 40px 0; }
.pagination { margin-top: 12px; display: flex; align-items: center; gap: 12px; }

.primary-btn, .plain-btn { border-radius: 6px; border: 1px solid #d8e0ea; cursor: pointer; }
.primary-btn { background: #1677ff; color: #fff; border-color: #1677ff; padding: 0 12px; height: 34px; }
.plain-btn { background: #fff; color: #233a59; padding: 0 12px; height: 34px; }
</style>
