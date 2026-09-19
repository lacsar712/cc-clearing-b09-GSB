<template>
  <div class="page">
    <h2 class="page-title">重复检测</h2>
    <p class="page-desc">同一交割日、币种下收付方与金额完全相同的 OPEN 义务自动归组；处理完（取消重复笔或标记已复核）后才能执行对应轧差</p>

    <el-alert
      v-if="pendingCount > 0"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom:16px"
      :title="`有 ${pendingCount} 组疑似重复义务待处理，对应交割日 + 币种的轧差将被阻止`"
    />

    <div class="toolbar">
      <el-date-picker v-model="filters.settleDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日" />
      <el-select v-model="filters.currency" clearable placeholder="币种" style="width:120px">
        <el-option label="USD" value="USD" />
        <el-option label="CNY" value="CNY" />
        <el-option label="EUR" value="EUR" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态" style="width:140px">
        <el-option label="待处理" value="PENDING" />
        <el-option label="已复核" value="REVIEWED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="rows" v-loading="loading" row-key="groupId" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="group-detail">
              <el-table :data="row.obligations" size="small">
                <el-table-column label="义务 ID" min-width="220">
                  <template #default="{ row: o }"><span class="mono">{{ o.obligationId }}</span></template>
                </el-table-column>
                <el-table-column prop="amount" label="金额" width="160" />
                <el-table-column label="创建时间" min-width="170">
                  <template #default="{ row: o }">{{ formatTime(o.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row: o }"><el-tag size="small">{{ o.status }}</el-tag></template>
                </el-table-column>
                <el-table-column v-if="auth.isOperator" label="操作" width="160">
                  <template #default="{ row: o }">
                    <el-button
                      size="small"
                      type="danger"
                      :disabled="row.status !== 'PENDING'"
                      @click="openCancel(row, o)"
                    >标记重复并取消</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="settleDate" label="交割日" width="110" />
        <el-table-column prop="currency" label="币种" width="70" />
        <el-table-column label="付款方 → 收款方" min-width="200">
          <template #default="{ row }">{{ nameOf(row.payerMemberId) }} → {{ nameOf(row.payeeMemberId) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="150" />
        <el-table-column prop="obligationCount" label="笔数" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'danger' : 'success'">
              {{ row.status === 'PENDING' ? '待处理' : '已复核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核信息" min-width="170">
          <template #default="{ row }">
            <template v-if="row.reviewedAt">{{ formatTime(row.reviewedAt) }}<br />by {{ row.reviewedBy }}</template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="auth.isOperator" label="操作" width="130">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :disabled="row.status !== 'PENDING'"
              @click="markReviewed(row)"
            >标记已复核</el-button>
          </template>
        </el-table-column>
        <template #empty>当前筛选条件下没有疑似重复组</template>
      </el-table>
    </div>

    <el-dialog v-model="cancelDialog.visible" title="标记重复并取消义务" width="520px">
      <p style="margin-top:0">
        义务 <span class="mono">{{ cancelDialog.obligationId }}</span> 将被置为 CANCELLED，
        同组其余义务保持 OPEN；该组随即解除对轧差的阻止。
      </p>
      <el-input
        v-model="cancelDialog.reason"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="取消原因（必填），例如：重复录入，与另一笔义务相同"
      />
      <template #footer>
        <el-button @click="cancelDialog.visible = false">返回</el-button>
        <el-button
          type="danger"
          :disabled="!cancelDialog.reason.trim()"
          :loading="cancelDialog.saving"
          @click="confirmCancel"
        >确认取消该义务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const rows = ref([])
const members = ref([])
const loading = ref(false)

const filters = reactive({
  settleDate: '',
  currency: '',
  status: ''
})

const cancelDialog = reactive({
  visible: false,
  saving: false,
  obligationId: '',
  reason: ''
})

const memberMap = computed(() => Object.fromEntries(members.value.map((m) => [m.memberId, m.name])))
const pendingCount = computed(() => rows.value.filter((g) => g.status === 'PENDING').length)

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
    if (filters.status) params.status = filters.status
    const { data } = await api.get('/duplicate-groups', { params })
    rows.value = data
  } finally {
    loading.value = false
  }
}

function openCancel(group, obligation) {
  cancelDialog.obligationId = obligation.obligationId
  cancelDialog.reason = ''
  cancelDialog.visible = true
}

async function confirmCancel() {
  cancelDialog.saving = true
  try {
    await api.post(`/obligations/${cancelDialog.obligationId}/cancel`, {
      reason: cancelDialog.reason.trim()
    })
    ElMessage.success('义务已取消，该组不再阻止轧差')
    cancelDialog.visible = false
    await load()
  } finally {
    cancelDialog.saving = false
  }
}

async function markReviewed(row) {
  await ElMessageBox.confirm(
    `确认该组 ${row.obligationCount} 笔义务均为有效业务、并非重复录入？标记后该组不再阻止轧差；若之后再录入相同义务，该组会重新出现。`,
    '标记已复核',
    { confirmButtonText: '确认已复核', cancelButtonText: '返回', type: 'warning' }
  )
  await api.post('/duplicate-groups/review', {
    settleDate: row.settleDate,
    currency: row.currency,
    payerMemberId: row.payerMemberId,
    payeeMemberId: row.payeeMemberId,
    amount: row.amount
  })
  ElMessage.success('已标记复核')
  await load()
}

onMounted(async () => {
  const { data } = await api.get('/members')
  members.value = data
  await load()
})
</script>

<style scoped>
.group-detail {
  padding: 8px 24px 16px 48px;
  background: var(--el-fill-color-light);
}
</style>
