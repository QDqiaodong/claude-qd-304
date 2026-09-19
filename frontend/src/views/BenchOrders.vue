<template>
  <div class="pane">
    <header class="pane-head">
      <h2>对照试验</h2>
      <span class="sub">一张开台单要同时挂住仪器和对照试剂、柜型配对也得过关，缺一样台都开不出去</span>
    </header>

    <div class="recorder" v-autofocus>
      <select v-model="form.instrumentId" class="pick">
        <option :value="null">选一台仪器</option>
        <option v-for="i in instruments" :key="i.id" :value="i.id" :disabled="!!instrumentBlock(i)">
          {{ i.instrumentCode }} {{ i.instrumentName }}{{ instrumentBlock(i) ? '（' + instrumentBlock(i) + '）' : '' }}
        </option>
      </select>
      <select v-model="form.reagentId" class="pick">
        <option :value="null">锁一瓶对照试剂</option>
        <option v-for="r in reagents" :key="r.id" :value="r.id" :disabled="!!reagentBlock(r)">
          {{ r.reagentCode }} {{ r.reagentName }}（{{ cabinetKind(r) }} · 余 {{ r.balance }}）{{ reagentBlock(r) ? ' · ' + reagentBlock(r) : '' }}
        </option>
      </select>
      <input v-model="form.operatorName" class="who" placeholder="开台人" @keyup.enter="open" />
      <button class="prime" :disabled="!canOpen" @click="open">开台</button>
    </div>
    <p v-if="busyWarning" class="warn-line">{{ busyWarning }}</p>
    <p v-else-if="pairWarning" class="warn-line">{{ pairWarning }}</p>

    <p class="sec-title">未收口（{{ openOrders.length }}）</p>
    <div class="feed">
      <div class="feed-head open-grid">
        <span>单号</span><span>仪器</span><span>对照试剂</span><span>开台人</span>
        <span class="r">预占</span><span>开台时间</span><span class="r">操作</span>
      </div>
      <div class="feed-row open-grid" v-for="o in openOrders" :key="o.id">
        <span class="mono">{{ o.orderNo }}</span>
        <span>{{ instrumentName(o) }}</span>
        <span>{{ reagentName(o) }}<small class="dim">（{{ reagentCabinet(o) }}）</small></span>
        <span>{{ o.operatorName }}</span>
        <span class="r">{{ o.quantity }}</span>
        <span class="dim">{{ fmt(o.openedAt) }}</span>
        <span class="r ops">
          <button class="mini ok" :disabled="!finishable(o)"
                  :title="finishable(o) ? '' : '仪器现在' + instrumentStatus(o) + '，这单只能作废'"
                  @click="finish(o)">完成</button>
          <button class="mini danger" @click="cancel(o)">作废</button>
        </span>
      </div>
      <p v-if="!openOrders.length" class="none">现在没有开着的台</p>
    </div>
    <p v-if="openOrders.some((o) => !finishable(o))" class="warn-line">
      有单挂的仪器已经不在「可用」状态了 —— 这些单只能整单作废，预占的库存会吐回去，不许点完成。
    </p>

    <p class="sec-title">历史</p>
    <div class="feed">
      <div class="feed-head his-grid">
        <span>单号</span><span>仪器</span><span>对照试剂</span><span>开台人</span>
        <span class="r">状态</span><span>开台时间</span><span>收口时间</span>
      </div>
      <div class="feed-row his-grid" v-for="o in history" :key="o.id">
        <span class="mono">{{ o.orderNo }}</span>
        <span>{{ instrumentName(o) }}</span>
        <span>{{ reagentName(o) }}</span>
        <span>{{ o.operatorName }}</span>
        <span class="r" :class="o.status === '已完成' ? 'done' : 'void'">{{ o.status }}</span>
        <span class="dim">{{ fmt(o.openedAt) }}</span>
        <span class="dim">{{ fmt(o.closedAt) }}</span>
      </div>
      <p v-if="!history.length" class="none">还没有收过口的单</p>
    </div>

    <p class="tip">小提示：开台预占只动试剂库存，不会写成领用或退回流水；作废的单会把预占吐回去，试剂回到还能再开的状态。</p>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import api from '../api'

const toast = inject('toast')
const today = inject('today')
const orders = ref([])
const instruments = ref([])
const reagents = ref([])
const cabinets = ref([])
const form = ref({ instrumentId: null, reagentId: null, operatorName: '' })

const instrumentOf = (id) => instruments.value.find((i) => i.id === id)
const reagentOf = (id) => reagents.value.find((r) => r.id === id)
const cabinetOf = (r) => r && cabinets.value.find((c) => c.id === r.cabinetId)

const openOrders = computed(() => orders.value.filter((o) => o.status === '进行中'))
const history = computed(() => orders.value.filter((o) => o.status !== '进行中'))

// 仪器挂不上台的原因；null 就是能选
function instrumentBlock(i) {
  if (i.instrumentStatus !== '可用') return i.instrumentStatus
  if (openOrders.value.some((o) => o.instrumentId === i.id)) return '有未收口的单'
  return null
}

// 试剂锁不上的原因；null 就是能选
function reagentBlock(r) {
  if (r.reagentStatus === '停用') return '停用'
  if (r.expireDate && r.expireDate < today) return '已过期'
  if (!r.balance || r.balance < 1) return '没库存了'
  return null
}

// 柜型 × 仪器配对红线，前端先亮出来，后端照样再拦一道
const pairWarning = computed(() => {
  const i = instrumentOf(form.value.instrumentId)
  const r = reagentOf(form.value.reagentId)
  const c = r && cabinetOf(r)
  if (!i || !c) return ''
  if (c.cabinetKind === '通风柜' && i.instrumentName.includes('分光光度计')) {
    return `「${c.cabinetName}」里的试剂会挥发，搅 ${i.instrumentName} 的光路，这个台开不了`
  }
  if (c.cabinetKind === '防爆柜' && i.instrumentName.includes('恒温水浴')) {
    return `「${c.cabinetName}」里的试剂上 ${i.instrumentName} 加热有引爆风险，这个台开不了`
  }
  return ''
})

// 第二个人选同一台仪器，当场看见这句
const busyWarning = computed(() => {
  const i = instrumentOf(form.value.instrumentId)
  if (i && openOrders.value.some((o) => o.instrumentId === i.id)) {
    return `「${i.instrumentName}」这台已经有未收口的对照试验，不能再开`
  }
  return ''
})

const canOpen = computed(() =>
  form.value.instrumentId && form.value.reagentId && form.value.operatorName.trim()
  && !pairWarning.value && !busyWarning.value)

function instrumentName(o) {
  const i = instrumentOf(o.instrumentId)
  return i ? i.instrumentName : '—'
}
function instrumentStatus(o) {
  const i = instrumentOf(o.instrumentId)
  return i ? i.instrumentStatus : '—'
}
function reagentName(o) {
  const r = reagentOf(o.reagentId)
  return r ? r.reagentName : '—'
}
function reagentCabinet(o) {
  const c = cabinetOf(reagentOf(o.reagentId))
  return c ? c.cabinetName : '未入柜'
}
function cabinetKind(r) {
  const c = cabinetOf(r)
  return c ? c.cabinetKind || c.cabinetName : '未入柜'
}
// 仪器被改成维修中 / 停用之后，这单就只能作废，完成按钮锁死
function finishable(o) {
  const i = instrumentOf(o.instrumentId)
  return !!i && i.instrumentStatus === '可用'
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  const [os, is, rs, cs] = await Promise.all([
    api.benchOrders.list(), api.instruments.list(), api.reagents.list(), api.cabinets.list()
  ])
  orders.value = os
  instruments.value = is
  reagents.value = rs
  cabinets.value = cs
}

async function open() {
  if (!canOpen.value) return
  try {
    await api.benchOrders.open({ ...form.value })
    await load()
    toast('台开好了，对照试剂已经预占')
    form.value = { instrumentId: null, reagentId: null, operatorName: form.value.operatorName }
  } catch (e) {
    await load()
    toast(e.message, false)
  }
}

async function finish(o) {
  try {
    await api.benchOrders.finish(o.id)
    await load()
    toast('这单收口了')
  } catch (e) {
    await load()
    toast(e.message, false)
  }
}

async function cancel(o) {
  try {
    await ElMessageBox.confirm(
      `作废后预占的 ${o.quantity} 份对照试剂会吐回库存，整单不能恢复。确定作废 ${o.orderNo} 吗？`,
      '整单作废',
      { confirmButtonText: '作废', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await api.benchOrders.cancel(o.id)
    await load()
    toast('已整单作废，预占吐回库存')
  } catch (e) {
    await load()
    toast(e.message, false)
  }
}

onMounted(load)
</script>

<style scoped>
.pane-head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.pane-head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #b3a09a; font-size: 12px; }
.recorder { display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid #f5d9cb;
  border-radius: 12px; padding: 14px 16px; flex-wrap: wrap; }
.pick, .who { border: 1px solid #eee0d9; border-radius: 8px; padding: 8px 10px; font-size: 13px; }
.pick { min-width: 240px; }
.who { width: 110px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 9px 22px; font-size: 13px; cursor: pointer; }
.prime:disabled { opacity: .45; cursor: not-allowed; }
.warn-line { margin: 8px 2px 0; font-size: 12px; color: #c0392b; }
.sec-title { margin: 18px 2px 8px; font-size: 12px; color: #b3a09a; }
.feed { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; overflow: hidden; }
.feed-head, .feed-row { display: grid; gap: 10px; align-items: center; padding: 10px 14px; font-size: 13px; }
.open-grid { grid-template-columns: 110px 1.1fr 1.5fr 76px 48px 140px 120px; }
.his-grid { grid-template-columns: 110px 1.1fr 1.5fr 76px 62px 140px 140px; }
.feed-head { background: #fdf7f4; color: #b3a09a; font-size: 12px; }
.feed-row { border-top: 1px solid #f9f1ed; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #b3a09a; font-size: 12px; }
.r { text-align: right; }
.ops { display: flex; gap: 6px; justify-content: flex-end; }
.mini { border-radius: 7px; padding: 4px 12px; font-size: 12px; cursor: pointer; }
.mini.ok { background: var(--el-color-primary); color: #fff; border: none; }
.mini.ok:disabled { opacity: .4; cursor: not-allowed; }
.mini.danger { background: #fff; border: 1px solid #e8b4ac; color: #c0392b; }
.done { color: #2e7d4f; }
.void { color: #a89891; }
.dim { color: #a89891; font-size: 12px; }
.none { color: #c9bab4; font-size: 12px; text-align: center; padding: 18px 0; margin: 0; }
.tip { margin-top: 14px; font-size: 12px; color: #c2b2ac; }
</style>
