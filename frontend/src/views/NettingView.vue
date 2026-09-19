<template>
  <div class="page">
    <h2 class="page-title">轧差执行</h2>
    <p class="page-desc">指定交割日与币种执行单币种多边轧差，校验 Σnet = 0</p>

    <div class="card-panel">
      <div class="toolbar">
        <el-date-picker v-model="settleDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日" />
        <el-select v-model="currency" style="width:120px">
          <el-option label="USD" value="USD" />
          <el-option label="CNY" value="CNY" />
          <el-option label="EUR" value="EUR" />
        </el-select>
        <el-button type="primary" :disabled="!auth.isOperator || pendingDups.length > 0" :loading="running" @click="execute">执行轧差</el-button>
        <el-button @click="loadRuns">刷新批次</el-button>
      </div>
      <el-alert
        v-if="pendingDups.length"
        type="error"
        :closable="false"
        show-icon
        :title="`该交割日/币种存在 ${pendingDups.length} 组未处理的疑似重复 OPEN 义务，轧差已被阻止`"
      >
        <template #default>
          请先前往
          <router-link to="/duplicates" class="dup-link">重复检测</router-link>
          页取消重复义务或标记已复核，处理完毕后即可执行。
        </template>
      </el-alert>
    </div>

    <div v-if="result" class="card-panel" style="margin-top:16px">
      <div class="toolbar" style="justify-content:space-between">
        <div>
          <strong>本次结果</strong>
          <el-tag style="margin-left:8px" :type="result.run.status === 'COMPLETED' ? 'success' : 'danger'">
            {{ result.run.status }}
          </el-tag>
          <span style="margin-left:12px">ΣnetAmount = {{ result.sumNetAmount }}</span>
        </div>
        <el-button link type="primary" @click="$router.push(`/netting-runs/${result.run.runId}`)">查看详情</el-button>
      </div>
      <el-table :data="result.positions" stripe>
        <el-table-column prop="memberId" label="会员 ID" min-width="220">
          <template #default="{ row }">
            <span class="mono">{{ row.memberId }}</span>
            <div>{{ nameOf(row.memberId) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="90" />
        <el-table-column prop="netAmount" label="净头寸（正应收/负应付）" min-width="200" />
      </el-table>
    </div>

    <div class="card-panel" style="margin-top:16px">
      <strong>历史批次</strong>
      <el-table :data="runs" v-loading="loading" stripe style="margin-top:12px">
        <el-table-column prop="runId" label="Run ID" min-width="220">
          <template #default="{ row }">
            <router-link class="mono" :to="`/netting-runs/${row.runId}`">{{ row.runId }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="settleDate" label="交割日" width="120" />
        <el-table-column prop="currency" label="币种" width="90" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="failureReason" label="失败原因" min-width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const settleDate = ref(new Date().toISOString().slice(0, 10))
const currency = ref('USD')
const running = ref(false)
const loading = ref(false)
const result = ref(null)
const runs = ref([])
const memberMap = ref({})
const pendingDups = ref([])

function nameOf(id) {
  return memberMap.value[id] || ''
}

async function loadRuns() {
  loading.value = true
  try {
    const [r, m] = await Promise.all([api.get('/netting-runs'), api.get('/members')])
    runs.value = r.data
    memberMap.value = Object.fromEntries(m.data.map((x) => [x.memberId, x.name]))
  } finally {
    loading.value = false
  }
}

async function checkDuplicates() {
  if (!settleDate.value || !currency.value) {
    pendingDups.value = []
    return
  }
  try {
    const { data } = await api.get('/duplicates', {
      params: { settleDate: settleDate.value, currency: currency.value }
    })
    pendingDups.value = data.filter((g) => g.pending)
  } catch {
    pendingDups.value = []
  }
}

async function execute() {
  running.value = true
  try {
    const { data } = await api.post('/netting-runs', {
      settleDate: settleDate.value,
      currency: currency.value
    })
    result.value = data
    ElMessage.success('轧差完成，守恒校验通过')
    await loadRuns()
    await checkDuplicates()
  } catch (e) {
    result.value = null
    await loadRuns()
    await checkDuplicates()
  } finally {
    running.value = false
  }
}

watch([settleDate, currency], checkDuplicates)

onMounted(() => {
  loadRuns()
  checkDuplicates()
})
</script>

<style scoped>
.dup-link {
  color: #c45656;
  font-weight: 600;
  text-decoration: underline;
}
</style>
