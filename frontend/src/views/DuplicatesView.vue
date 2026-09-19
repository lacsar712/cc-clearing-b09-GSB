<template>
  <div class="page">
    <h2 class="page-title">疑似重复检测</h2>
    <p class="page-desc">同一交割日、币种下收付方与金额完全相同的 OPEN 义务自动归组；存在未处理组时对应交割日/币种的轧差将被阻止</p>

    <div class="toolbar">
      <el-date-picker v-model="filters.settleDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日（全部）" clearable />
      <el-select v-model="filters.currency" clearable placeholder="币种（全部）" style="width:140px">
        <el-option label="USD" value="USD" />
        <el-option label="CNY" value="CNY" />
        <el-option label="EUR" value="EUR" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-alert
      v-if="pendingGroups.length"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
      :title="`存在 ${pendingGroups.length} 组未处理的疑似重复 OPEN 义务，对应交割日/币种的轧差已被阻止`"
      description="请在下方取消重复义务（原因必填），或将整组标记为已复核无问题。"
    />

    <div v-loading="loading">
      <div v-if="!pendingGroups.length && !resolvedGroups.length" class="card-panel empty">
        当前没有疑似重复组
      </div>

      <div v-for="g in pendingGroups" :key="g.groupKey" class="card-panel group-card pending">
        <div class="group-head">
          <div class="group-title">
            <el-tag type="danger" effect="dark">待处理</el-tag>
            <strong>{{ g.settleDate }}</strong>
            <el-tag>{{ g.currency }}</el-tag>
            <span>{{ nameOf(g.payerMemberId) }} → {{ nameOf(g.payeeMemberId) }}</span>
            <span class="mono">金额 {{ g.amount }}</span>
            <el-tag type="warning" effect="plain">{{ g.obligations.length }} 笔撞单</el-tag>
          </div>
          <el-button
            v-if="auth.isOperator"
            type="success"
            plain
            size="small"
            @click="openReview(g)"
          >整组已复核无问题</el-button>
        </div>
        <el-table :data="g.obligations" stripe>
          <el-table-column prop="obligationId" label="义务 ID" min-width="230">
            <template #default="{ row }"><span class="mono">{{ row.obligationId }}</span></template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="160" />
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column v-if="auth.isOperator" label="操作" width="180">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="openCancel(g, row)">标记重复并取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template v-if="resolvedGroups.length">
        <h3 class="section-title">已处理（不再阻止轧差；新录入撞单会重新出现）</h3>
        <div v-for="g in resolvedGroups" :key="g.groupKey" class="card-panel group-card">
          <div class="group-head">
            <div class="group-title">
              <el-tag :type="g.resolutionType === 'REVIEWED' ? 'success' : 'info'" effect="dark">
                {{ g.resolutionType === 'REVIEWED' ? '已复核' : '已取消处理' }}
              </el-tag>
              <strong>{{ g.settleDate }}</strong>
              <el-tag>{{ g.currency }}</el-tag>
              <span>{{ nameOf(g.payerMemberId) }} → {{ nameOf(g.payeeMemberId) }}</span>
              <span class="mono">金额 {{ g.amount }}</span>
            </div>
          </div>
          <div class="resolution-line">
            处理人：{{ g.resolvedBy || '-' }}　时间：{{ formatTime(g.resolvedAt) }}　{{ g.resolutionType === 'REVIEWED' ? '备注' : '取消原因' }}：{{ g.resolutionReason || '-' }}
          </div>
          <el-table :data="g.obligations" stripe>
            <el-table-column prop="obligationId" label="义务 ID" min-width="230">
              <template #default="{ row }"><span class="mono">{{ row.obligationId }}</span></template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="160" />
            <el-table-column label="创建时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>

    <el-dialog v-model="cancelDialog.visible" title="标记重复并取消" width="480px">
      <p class="dialog-tip">
        将把义务 <span class="mono">{{ cancelDialog.obligationId }}</span> 置为 CANCELLED，组内其余义务保持 OPEN。
      </p>
      <el-form label-width="90px">
        <el-form-item label="取消原因" required>
          <el-input
            v-model="cancelDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="必填，例如：与 xxx 重复录入"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialog.visible = false">返回</el-button>
        <el-button type="danger" :loading="cancelDialog.saving" @click="confirmCancel">确认取消该笔</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialog.visible" title="整组标记已复核" width="480px">
      <p class="dialog-tip">确认该组义务均为有效业务、非重复录入。标记后本组不再阻止轧差。</p>
      <el-form label-width="90px">
        <el-form-item label="备注">
          <el-input v-model="reviewDialog.note" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">返回</el-button>
        <el-button type="success" :loading="reviewDialog.saving" @click="confirmReview">确认已复核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const groups = ref([])
const members = ref([])

const filters = reactive({
  settleDate: '',
  currency: ''
})

const cancelDialog = reactive({
  visible: false,
  saving: false,
  groupKey: '',
  obligationId: '',
  reason: ''
})

const reviewDialog = reactive({
  visible: false,
  saving: false,
  groupKey: '',
  note: ''
})

const memberMap = computed(() => Object.fromEntries(members.value.map((m) => [m.memberId, m.name])))
const pendingGroups = computed(() => groups.value.filter((g) => g.pending))
const resolvedGroups = computed(() => groups.value.filter((g) => !g.pending))

function nameOf(id) {
  return memberMap.value[id] || id
}

function formatTime(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString()
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.settleDate) params.settleDate = filters.settleDate
    if (filters.currency) params.currency = filters.currency
    const { data } = await api.get('/duplicates', { params })
    groups.value = data
  } finally {
    loading.value = false
  }
}

function openCancel(group, obligation) {
  cancelDialog.groupKey = group.groupKey
  cancelDialog.obligationId = obligation.obligationId
  cancelDialog.reason = ''
  cancelDialog.visible = true
}

function openReview(group) {
  reviewDialog.groupKey = group.groupKey
  reviewDialog.note = ''
  reviewDialog.visible = true
}

async function confirmCancel() {
  if (!cancelDialog.reason.trim()) {
    ElMessage.warning('取消原因必填')
    return
  }
  cancelDialog.saving = true
  try {
    await api.post('/duplicates/cancel', {
      groupKey: cancelDialog.groupKey,
      obligationId: cancelDialog.obligationId,
      reason: cancelDialog.reason.trim()
    })
    ElMessage.success('已取消该笔重复义务')
    cancelDialog.visible = false
    await load()
  } finally {
    cancelDialog.saving = false
  }
}

async function confirmReview() {
  reviewDialog.saving = true
  try {
    await api.post('/duplicates/review', {
      groupKey: reviewDialog.groupKey,
      note: reviewDialog.note.trim() || null
    })
    ElMessage.success('整组已标记复核')
    reviewDialog.visible = false
    await load()
  } finally {
    reviewDialog.saving = false
  }
}

onMounted(async () => {
  try {
    const { data } = await api.get('/members')
    members.value = data
  } finally {
    await load()
  }
})
</script>

<style scoped>
.empty {
  color: var(--muted);
  text-align: center;
  padding: 32px 16px;
}
.group-card {
  margin-bottom: 16px;
}
.group-card.pending {
  border-color: #f3c2c2;
}
.group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.resolution-line {
  color: var(--muted);
  font-size: 13px;
  margin: -4px 0 12px;
}
.section-title {
  margin: 20px 0 12px;
  font-size: 15px;
  color: var(--muted);
}
.dialog-tip {
  margin: 0 0 12px;
  color: var(--muted);
  font-size: 13px;
}
</style>
