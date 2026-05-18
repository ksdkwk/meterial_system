<template>
  <div ref="pageContainerRef" class="ai-log-page" @scroll="onPageScroll">
    <div class="page-header">
      <h2 class="page-title">🧠 AI识别</h2>
      <span class="total-info">共 {{ total }} 条记录</span>
      <button v-if="canRecognize" class="primary-btn" @click="openRecognizeModal">+ 发起识别</button>
    </div>

    <div v-if="canManage" class="ai-config-check" :class="{ ok: aiConfigCheck.baiduApiKeyConfigured && aiConfigCheck.baiduSecretConfigured && aiConfigCheck.baiduMaterialUrlConfigured }">
      <span v-if="aiConfigLoading">AI配置自检中...</span>
      <template v-else>
        <span>BAIDU_AI_METERIAL_SYSTEM_API_KEY：{{ aiConfigCheck.baiduApiKeyConfigured ? '已读取' : '未读取' }}（{{ aiConfigCheck.baiduApiKeyMasked || '未读取到' }}）</span>
        <span>BAIDU_AI_METERIAL_SYSTEM_SECRET_KEY：{{ aiConfigCheck.baiduSecretConfigured ? '已读取' : '未读取' }}（{{ aiConfigCheck.baiduSecretMasked || '未读取到' }}）</span>
        <span>BAIDU_METERIAL_URL：{{ aiConfigCheck.baiduMaterialUrlConfigured ? '已读取' : '未读取' }}（{{ aiConfigCheck.baiduMaterialUrlMasked || '未读取到' }}）</span>
      </template>
    </div>

    <div v-if="canManage" class="settings-card">
      <div class="settings-head">
        <h3>识别参数配置</h3>
        <span class="settings-tip">支持在线调整识别策略，提高成功率与可信度</span>
      </div>
      <div class="settings-grid">
        <label>baike_num</label>
        <input v-model.number="settingsForm.baikeNum" type="number" min="0" max="5" />
        <label>最低置信度阈值(%)</label>
        <input v-model.number="settingsForm.minConfidenceThreshold" type="number" min="0" max="100" step="0.01" />
      </div>
      <div class="settings-actions">
        <button class="plain-btn" :disabled="settingsLoading" @click="loadRecognitionSettings">重载配置</button>
        <button class="primary-btn" :disabled="settingsSaving" @click="saveRecognitionSettings">{{ settingsSaving ? '保存中...' : '保存参数' }}</button>
      </div>
    </div>

    <div class="query-bar">
      <input
        v-model.trim="query.keyword"
        class="query-input"
        placeholder="按记录ID/用户/识别对象查询"
        @keyup.enter="fetchLogs(1)"
      />
      <button class="primary-btn" @click="fetchLogs(1)">查询</button>
      <button class="plain-btn" @click="resetQuery">重置</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error-box">
      <span>❌ {{ error }}</span>
      <button @click="fetchLogs(pageNum)">重试</button>
    </div>

    <div v-else class="table-region">
      <div class="table-wrap-shell">
        <div class="table-wrap">
          <table class="log-table">
            <thead>
              <tr>
                <th>记录ID</th>
                <th>发起用户</th>
                <th>识别对象（Top1）</th>
                <th>识别对象数</th>
                <th>置信度</th>
                <th>核实状态</th>
                <th>核实人</th>
                <th>识别时间</th>
                <th>原图</th>
                <th>AI结果JSON</th>
                <th v-if="canManage">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in logs" :key="item.recId">
                <td>{{ item.recId || '—' }}</td>
                <td>{{ item.userName || item.userId || '—' }}</td>
                <td>{{ item.recognizeMaterialName || '—' }}</td>
                <td>{{ item.recognizeQuantity ?? '—' }}</td>
                <td>{{ formatConfidence(item.confidenceScore) }}</td>
                <td>
                  <span class="status-tag" :class="Number(item.isVerified) === 1 ? 'active' : 'pending'">
                    {{ Number(item.isVerified) === 1 ? '已核实' : '未核实' }}
                  </span>
                </td>
                <td>{{ item.verifyUserName || item.verifyUserId || '—' }}</td>
                <td>{{ formatTime(item.createTime) }}</td>
                <td>
                  <button
                    v-if="item.originalImageUrl"
                    class="link-btn"
                    type="button"
                    @click="openOriginalImage(item.originalImageUrl)"
                  >
                    查看
                  </button>
                  <span v-else>—</span>
                </td>
                <td>
                  <button
                    v-if="item.aiResultJson"
                    class="tiny-btn"
                    @click="openJsonModal(item.aiResultJson)"
                  >
                    查看对象列表
                  </button>
                  <span v-else>—</span>
                </td>
                <td v-if="canManage">
                  <button
                    v-if="Number(item.isVerified) !== 1"
                    class="tiny-btn"
                    @click="openVerifyModal(item)"
                  >
                    核实
                  </button>
                  <span v-else>—</span>
                </td>
              </tr>
              <tr v-if="logs.length === 0">
                <td :colspan="canManage ? 11 : 10" class="empty">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

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

    <div v-if="showRecognizeModal" class="modal-mask" @click.self="showRecognizeModal = false">
      <div class="modal">
        <h3>发起AI物料图像识别</h3>
        <div class="form-grid">
          <label>本地图片*</label>
          <div class="upload-row">
            <input
              class="readonly-input"
              :value="recognizeForm.localFileName"
              placeholder="请先选择本地图片文件"
              readonly
              @click="triggerRecognizeFileSelect"
            />
            <button class="plain-btn" type="button" @click="triggerRecognizeFileSelect">选择文件</button>
            <input
              ref="recognizeFileInputRef"
              class="hidden-file-input"
              type="file"
              accept="image/*"
              @change="onRecognizeFileChange"
            />
          </div>
        </div>
        <div class="modal-actions">
          <button class="plain-btn" @click="showRecognizeModal = false">取消</button>
          <button class="primary-btn" :disabled="recognizeSubmitting" @click="submitRecognize">
            {{ recognizeSubmitting ? '识别中...' : '开始识别' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showVerifyModal" class="modal-mask" @click.self="showVerifyModal = false">
      <div class="modal">
        <h3>核实AI识别记录</h3>
        <div class="form-grid">
          <label>物料名称</label>
          <input v-model.trim="verifyForm.recognizeMaterialName" placeholder="核实后的物料名称" />
          <label>识别数量</label>
          <input v-model.number="verifyForm.recognizeQuantity" type="number" min="0" />
        </div>
        <div class="modal-actions">
          <button class="plain-btn" @click="showVerifyModal = false">取消</button>
          <button class="primary-btn" :disabled="verifySubmitting" @click="submitVerify">
            {{ verifySubmitting ? '提交中...' : '确认核实' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showJsonModal" class="modal-mask" @click.self="showJsonModal = false">
      <div class="modal">
        <h3>AI识别对象列表</h3>

        <div v-if="detailHasList" class="detail-overview">
          <div><b>Top1对象：</b>{{ detailMeta.topObject || '—' }}</div>
          <div><b>识别对象数：</b>{{ detailMeta.totalCount }}</div>
          <div><b>识别摘要：</b>{{ detailMeta.summary || '—' }}</div>
        </div>

        <div v-if="detailHasList" class="detail-table-wrap">
          <table class="detail-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>keyword</th>
                <th>root</th>
                <th>score</th>
                <th>命中物料词典</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in detailRows" :key="`${row.keyword}-${idx}`">
                <td>{{ idx + 1 }}</td>
                <td>{{ row.keyword }}</td>
                <td>{{ row.root }}</td>
                <td>{{ row.scoreText }}</td>
                <td>
                  <span :class="row.hit ? 'hit-tag yes' : 'hit-tag no'">
                    {{ row.hit ? `是（${row.hitNames}）` : '否' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <pre v-else class="json-content">{{ prettyJson }}</pre>
        <div class="modal-actions">
          <button class="plain-btn" @click="showJsonModal = false">关闭</button>
        </div>
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
const query = ref({ keyword: '' })
const showJsonModal = ref(false)
const currentJson = ref('')
const detailRows = ref([])
const detailMeta = ref({ topObject: '', totalCount: 0, summary: '' })
const showRecognizeModal = ref(false)
const recognizeSubmitting = ref(false)
const recognizeFileInputRef = ref(null)
const recognizeForm = ref({ localFile: null, localFileName: '' })
const showVerifyModal = ref(false)
const verifySubmitting = ref(false)
const verifyTargetRecId = ref('')
const verifyForm = ref({ recognizeMaterialName: '', recognizeQuantity: 0 })
const settingsLoading = ref(false)
const settingsSaving = ref(false)
const settingsForm = ref({ baikeNum: 3, minConfidenceThreshold: 55 })
const aiConfigLoading = ref(false)
const aiConfigCheck = ref({
  baiduApiKeyConfigured: false,
  baiduSecretConfigured: false,
  baiduMaterialUrlConfigured: false,
  baiduApiKeyMasked: '未读取到',
  baiduSecretMasked: '未读取到',
  baiduMaterialUrlMasked: '未读取到'
})

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
  } catch {
    return false
  }
})

const canRecognize = computed(() => {
  const raw = localStorage.getItem('currentUser') || localStorage.getItem('userInfo')
  if (!raw) return false
  try {
    const u = JSON.parse(raw)
    const roleCode = String(u?.role?.roleCode || u?.roleCode || u?.rolePermission || '').toUpperCase()
    const roleName = String(u?.role?.roleName || u?.roleName || '').trim()
    return canManage.value
      || ['WORKER', 'USER_WORKER'].includes(roleCode)
      || ['施工人员'].includes(roleName)
  } catch {
    return false
  }
})

const loadRecognitionSettings = async () => {
  if (!canManage.value) return
  settingsLoading.value = true
  try {
    const res = await request.get('/ai-recognition-logs/settings')
    if (res.code !== 200) {
      alert(res.msg || '加载识别参数失败')
      return
    }
    const d = res.data || {}
    settingsForm.value = {
      baikeNum: Number(d.baikeNum ?? 3),
      minConfidenceThreshold: Number(d.minConfidenceThreshold ?? 55)
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '加载识别参数失败')
  } finally {
    settingsLoading.value = false
  }
}

const loadAiConfigCheck = async () => {
  if (!canManage.value) return
  aiConfigLoading.value = true
  try {
    const res = await request.get('/ai-recognition-logs/config-check')
    if (res.code === 200 && res.data) {
      aiConfigCheck.value = {
        ...aiConfigCheck.value,
        ...res.data
      }
    }
  } catch {
    aiConfigCheck.value = {
      ...aiConfigCheck.value,
      baiduApiKeyConfigured: false,
      baiduSecretConfigured: false,
      baiduMaterialUrlConfigured: false,
      baiduApiKeyMasked: '未读取到',
      baiduSecretMasked: '未读取到',
      baiduMaterialUrlMasked: '未读取到'
    }
  } finally {
    aiConfigLoading.value = false
  }
}

const saveRecognitionSettings = async () => {
  const baikeNum = Number(settingsForm.value.baikeNum)
  const minThreshold = Number(settingsForm.value.minConfidenceThreshold)
  if (!Number.isInteger(baikeNum) || baikeNum < 0 || baikeNum > 5) {
    alert('baike_num范围必须在0-5')
    return
  }
  if (Number.isNaN(minThreshold) || minThreshold < 0 || minThreshold > 100) {
    alert('最低置信度阈值范围必须在0-100')
    return
  }

  settingsSaving.value = true
  try {
    const payload = {
      baikeNum,
      minConfidenceThreshold: Number(minThreshold.toFixed(2))
    }
    const res = await request.put('/ai-recognition-logs/settings', payload)
    if (res.code === 200) {
      alert('识别参数保存成功')
      loadRecognitionSettings()
    } else {
      alert(res.msg || '识别参数保存失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '识别参数保存失败')
  } finally {
    settingsSaving.value = false
  }
}

const fetchLogs = async (targetPage = pageNum.value) => {
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/ai-recognition-logs/list', {
      params: {
        keyword: query.value.keyword || undefined,
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
      error.value = res.msg || '获取AI识别记录失败'
    }
  } catch (e) {
    error.value = e?.response?.data?.msg || '网络请求失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value.keyword = ''
  fetchLogs(1)
}

const openJsonModal = (jsonText) => {
  currentJson.value = String(jsonText || '')
  detailRows.value = []
  detailMeta.value = { topObject: '', totalCount: 0, summary: '' }
  try {
    const parsed = JSON.parse(currentJson.value)
    const arr = Array.isArray(parsed?.detectedItems) ? parsed.detectedItems : []
    detailRows.value = arr.map((it) => {
      const scorePercent = it?.scorePercent != null
        ? Number(it.scorePercent)
        : Number(it?.score) * 100
      const materialMatches = Array.isArray(it?.materialMatches) ? it.materialMatches.filter(Boolean) : []
      return {
        keyword: String(it?.keyword || '—'),
        root: String(it?.root || '—'),
        scoreText: Number.isFinite(scorePercent) ? `${scorePercent.toFixed(2)}%` : '—',
        hit: materialMatches.length > 0,
        hitNames: materialMatches.join('、')
      }
    })
    detailMeta.value = {
      topObject: String(parsed?.recognizeMaterialName || ''),
      totalCount: Number(parsed?.recognizeQuantity ?? detailRows.value.length ?? 0),
      summary: String(parsed?.summary || '')
    }
  } catch {
    // 非JSON或旧数据时，保留原有JSON文本展示
  }
  showJsonModal.value = true
}

const detailHasList = computed(() => detailRows.value.length > 0)

const openRecognizeModal = () => {
  recognizeForm.value = { localFile: null, localFileName: '' }
  if (recognizeFileInputRef.value) {
    recognizeFileInputRef.value.value = ''
  }
  showRecognizeModal.value = true
}

const triggerRecognizeFileSelect = () => {
  recognizeFileInputRef.value?.click()
}

const onRecognizeFileChange = (e) => {
  const file = e?.target?.files?.[0]
  if (!file) return
  recognizeForm.value.localFile = file
  recognizeForm.value.localFileName = file.name
}

const submitRecognize = async () => {
  if (!recognizeForm.value.localFile) {
    alert('请先选择本地图片文件')
    return
  }
  recognizeSubmitting.value = true
  try {
    const formData = new FormData()
    formData.append('file', recognizeForm.value.localFile)

    const res = await request.post('/ai-recognition-logs/recognize/upload', formData, {
      timeout: 120000,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.code === 200) {
      alert('AI识别完成，记录已写入')
      showRecognizeModal.value = false
      fetchLogs(1)
    } else {
      alert(res.msg || 'AI识别失败')
    }
  } catch (e) {
    if (e?.code === 'ECONNABORTED') {
      alert('识别超时，请稍后重试')
    } else {
      alert(e?.response?.data?.msg || 'AI识别失败')
    }
  } finally {
    recognizeSubmitting.value = false
  }
}

const openVerifyModal = (item) => {
  verifyTargetRecId.value = item?.recId || ''
  verifyForm.value = {
    recognizeMaterialName: item?.recognizeMaterialName || '',
    recognizeQuantity: Number(item?.recognizeQuantity ?? 0)
  }
  showVerifyModal.value = true
}

const submitVerify = async () => {
  if (!verifyTargetRecId.value) {
    alert('记录ID不能为空')
    return
  }
  if (verifyForm.value.recognizeQuantity == null || Number(verifyForm.value.recognizeQuantity) < 0) {
    alert('识别数量不能小于0')
    return
  }
  verifySubmitting.value = true
  try {
    const res = await request.put(`/ai-recognition-logs/${verifyTargetRecId.value}/verify`, {
      recognizeMaterialName: verifyForm.value.recognizeMaterialName || null,
      recognizeQuantity: Number(verifyForm.value.recognizeQuantity)
    })
    if (res.code === 200) {
      alert('核实成功')
      showVerifyModal.value = false
      fetchLogs(pageNum.value)
    } else {
      alert(res.msg || '核实失败')
    }
  } catch (e) {
    alert(e?.response?.data?.msg || '核实失败')
  } finally {
    verifySubmitting.value = false
  }
}

const prettyJson = computed(() => {
  if (!currentJson.value) return '—'
  try {
    return JSON.stringify(JSON.parse(currentJson.value), null, 2)
  } catch {
    return currentJson.value
  }
})

const formatTime = (timeStr) => {
  if (!timeStr) return '—'
  return String(timeStr).replace('T', ' ').substring(0, 16)
}

const formatConfidence = (val) => {
  if (val === null || val === undefined || val === '') return '—'
  const n = Number(val)
  if (Number.isNaN(n)) return '—'
  return `${n.toFixed(2)}%`
}

const openOriginalImage = async (imageRef) => {
  const ref = String(imageRef || '').trim()
  if (!ref) {
    alert('原图地址为空')
    return
  }

  const isHttp = /^https?:\/\//i.test(ref)
  if (isHttp) {
    window.open(ref, '_blank', 'noopener,noreferrer')
    return
  }

  const localPrefix = 'local://'
  const legacyPrefix = 'upload://'
  let fileName = ''
  if (ref.startsWith(localPrefix)) {
    fileName = ref.slice(localPrefix.length)
  } else if (ref.startsWith(legacyPrefix)) {
    fileName = ref.slice(legacyPrefix.length)
  }

  if (!fileName) {
    alert('当前原图地址格式不支持预览')
    return
  }

  try {
    const blob = await request.get(`/ai-recognition-logs/uploads/${encodeURIComponent(fileName)}`, {
      responseType: 'blob',
      timeout: 30000
    })
    const blobUrl = URL.createObjectURL(blob)
    window.open(blobUrl, '_blank', 'noopener,noreferrer')
    setTimeout(() => URL.revokeObjectURL(blobUrl), 60000)
  } catch (e) {
    alert(e?.response?.data?.msg || '原图不存在或无权限访问')
  }
}

onMounted(() => {
  fetchLogs(1)
  if (canManage.value) {
    loadAiConfigCheck()
    loadRecognitionSettings()
  }
})
</script>

<style scoped>
.ai-log-page {
  padding: 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  min-height: 0;
  overflow: visible;
}

.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-title { font-size: 28px; font-weight: 700; color: #1f3a5f; margin: 0; }
.total-info { color: #888; font-size: 13px; }

.ai-config-check {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  border: 1px solid #ffd8bf;
  background: #fff7e6;
  color: #ad6800;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
}

.ai-config-check.ok {
  border-color: #b7eb8f;
  background: #f6ffed;
  color: #237804;
}

.settings-card {
  border: 1px solid #e7edf5;
  border-radius: 10px;
  background: #fafcff;
  padding: 14px;
  margin-bottom: 12px;
}

.settings-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.settings-head h3 {
  margin: 0;
  font-size: 16px;
  color: #1f3a5f;
}

.settings-tip {
  color: #7b8ba3;
  font-size: 12px;
}

.settings-grid {
  display: grid;
  grid-template-columns: 150px minmax(260px, 1fr);
  gap: 8px 12px;
  align-items: center;
}

.settings-grid label {
  color: #5a6a82;
  font-size: 13px;
}

.settings-grid input,
.settings-grid textarea {
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 8px 10px;
  box-sizing: border-box;
  font-size: 13px;
}

.settings-grid textarea {
  resize: vertical;
  min-height: 66px;
}

.settings-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.query-bar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.query-input, .query-select { height: 34px; border: 1px solid #d8e0ea; border-radius: 6px; padding: 0 10px; }
.query-input { width: 360px; }

.loading { text-align: center; padding: 48px; color: #aaa; font-size: 15px; }
.error-box { display: flex; align-items: center; gap: 12px; padding: 16px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; color: #cf1322; }
.error-box button { padding: 4px 12px; background: #ff4d4f; color: #fff; border: none; border-radius: 4px; cursor: pointer; }

.table-region {
  position: relative;
  min-height: 0;
}

.table-wrap-shell {
  position: relative;
}

.table-wrap {
  overflow-x: auto;
  overflow-y: visible;
  max-height: none;
}
.log-table { width: 100%; min-width: 1200px; border-collapse: collapse; font-size: 14px; }
.log-table thead tr { background: #f5f7fa; }
.log-table th { padding: 12px 14px; text-align: left; color: #5a6a82; font-weight: 600; border-bottom: 2px solid #e8edf3; white-space: nowrap; }
.log-table td { padding: 11px 14px; color: #333; border-bottom: 1px solid #f0f2f5; vertical-align: middle; white-space: nowrap; }
.log-table tbody tr:hover { background: #f9fbff; }

.status-tag { padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-tag.active { background: #f6ffed; color: #52c41a; }
.status-tag.pending { background: #fffbe6; color: #d48806; }

.empty { text-align: center; color: #bbb; padding: 40px 0; }
.pagination {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.primary-btn, .plain-btn { border-radius: 6px; border: 1px solid #d8e0ea; cursor: pointer; }
.primary-btn { background: #1677ff; color: #fff; border-color: #1677ff; padding: 0 12px; height: 34px; }
.plain-btn { background: #fff; color: #233a59; padding: 0 12px; height: 34px; }

.link {
  color: #1677ff;
  text-decoration: none;
}

.link-btn {
  border: none;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  padding: 0;
}

.link-btn:hover {
  text-decoration: underline;
}

.tiny-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #233a59;
  cursor: pointer;
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
  width: min(900px, calc(100vw - 32px));
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
  grid-template-columns: 92px 1fr;
  gap: 10px;
  align-items: center;
}

.form-grid label {
  color: #5a6a82;
  font-size: 13px;
}

.form-grid input {
  height: 34px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 0 10px;
  box-sizing: border-box;
}

.upload-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
}

.readonly-input {
  width: 100%;
}

.hidden-file-input {
  display: none;
}

.json-content {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: #0b1020;
  color: #dbe8ff;
  line-height: 1.5;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-overview {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
  color: #4d6279;
  font-size: 13px;
}

.detail-table-wrap {
  border: 1px solid #e2e9f2;
  border-radius: 8px;
  overflow: auto;
  max-height: min(52vh, 460px);
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
  font-size: 13px;
}

.detail-table thead tr {
  background: #f5f7fa;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f6;
  text-align: left;
  white-space: nowrap;
}

.hit-tag {
  display: inline-block;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
}

.hit-tag.yes {
  background: #e8f5e9;
  color: #2e7d32;
}

.hit-tag.no {
  background: #f3f5f7;
  color: #607080;
}

.modal-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

</style>
